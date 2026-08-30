/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.protocol.GroupInfoProto;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import org.hibernate.annotations.*;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * A group of accounts.
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @version $Revision: 1.5 $
 */
@Entity(name = "AGroup")
@Table(indexes = {
    @Index(name = "IDX_GROUP_NAME", columnList = Group.PROPERTYNAME_GROUPNAME),
    @Index(name = "IDX_GROUP_LDAPDN", columnList = Group.PROPERTYNAME_LDAPDN),
    @Index(name = "IDX_GRP_ORG_ID", columnList = Group.PROPERTYNAME_ORGANIZATION_ID)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Group implements Serializable, D2DObject, Auditable {

    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_GROUPNAME = "name";
    public static final String PROPERTYNAME_NOTES = "notes";
    public static final String PROPERTYNAME_PERMISSIONS = "permissions";
    public static final String PROPERTYNAME_ORGANIZATION_ID = "organizationOID";
    public static final String PROPERTYNAME_LDAPDN = "ldapDN";

    private static final long serialVersionUID = 100L;

    private static final Logger LOG = Logger.getLogger(Group.class.getName());

    @Id
    private String oid;
    @Column(nullable = false)
    private String name;

    @Column(length = 512)
    private String ldapDN;

    @Column(length = 2048)
    private String notes;

    @Column(nullable = true, unique = false)
    private String organizationOID;

    /* Both collections are loaded eagerly, so a query over many groups loads them for every row.
     * Without a batch size that is one SELECT per group and collection - a list of a few thousand
     * groups spent seconds on nothing else. Batched, the same load takes a handful of statements.
     * The size matches the one Account and Organization use for their collections. */
    @javax.persistence.ElementCollection
    @javax.persistence.CollectionTable(name = "AGroup_permissions", joinColumns = @JoinColumn(name = "AGroup_oid"))
    @Type(type = "permissionType")
    @Column(name = "element")
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.NONE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Permission> permissions;

    @ManyToMany
    @JoinTable(name = "AGroup_Parents", joinColumns = @JoinColumn(name = "child_oid"), inverseJoinColumns = @JoinColumn(name = "parent_oid"))
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Group> parents;

    @Embedded
    @Fetch(FetchMode.JOIN)
    public AuditFields auditFields = new AuditFields();

    /**
     * Serialization constructor
     */
    public Group() {
    }

    public Group(String name) {
        this(IdGenerator.makeId(), name);
    }

    public Group(String oid, String name) {
        Reject.ifBlank(oid, "OID");
        this.oid = oid;
        this.name = name;
        this.permissions = new CopyOnWriteArrayList<>();
        this.parents = new CopyOnWriteArraySet<>();
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public Group(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public void grant(Permission... newPermissions) {
        Reject.ifNull(newPermissions, "Permission is null");
        for (Permission p : newPermissions) {
            if (isInvalidGrant(p)) {
                continue;
            }
            if (isAlreadyGranted(p)) {
                continue;
            }
            if (p instanceof FolderPermission) {
                revokeAllFolderPermissions(((FolderPermission) p).getFolder());
            }
            permissions.add(p);
        }
    }

    private boolean isInvalidGrant(Permission p) {
        Reject.ifNull(p, "Permission is null");
        if (p instanceof FolderPermission) {
            FolderInfo foInfo = ((FolderPermission) p).getFolder();
            if (foInfo.isMetaFolder()) {
                LOG.warning(this + ": Not allowed to grant permissions on meta " + foInfo);
                return true;
            }
            if (p instanceof FolderOwnerPermission) {
                // A group never OWNS a folder: ownership carries the quota and the charging, and both
                // belong to an account. Every ownership path on the server grants to an Account only
                // (ServerSecurityService), and Account.getFoldersCharged() consequently looks at the
                // account's own permissions. Rejected for top folders as well as for subfolders,
                // inheriting or interrupted (PFC-3543).
                LOG.warning(this + ": Not allowed to grant owner permission on " + foInfo
                    + " - a group never owns a folder");
                return true;
            }
        }
        return false;
    }

    private boolean isAlreadyGranted(Permission permission) {
        if (permissions == null) {
            return false;
        }
        if (permission instanceof FolderPermission) {
            FolderInfo folder = ((FolderPermission) permission).getFolder();
            for (Permission p : permissions) {
                if (p instanceof FolderPermission) {
                    FolderPermission fp = (FolderPermission) p;
                    if (fp.getFolder().equals(folder) && fp.implies(permission)) {
                        return true;
                    }
                }
            }
            return false;
        }
        for (Permission p : permissions) {
            if (p != null && p.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    public void revoke(Permission... revokePermission) {
        Reject.ifNull(revokePermission, "Permission is null");
        for (Permission p : revokePermission) {
            if (permissions.remove(p)) {
                LOG.fine("Revoked permission from " + this + ": " + p);
            }
        }
    }

    /**
     * Revokes every folder permission of this group on the given folder.
     * <p>
     * Owner is included although a group must never hold it (see {@code isInvalidGrant}): a legacy row
     * or an import from before that guard would otherwise stay, and since {@link #grant} calls this to
     * replace the previous mode - and {@link #getAllowedAccess(FolderInfo)} checks owner first - any
     * downgrade would silently have no effect. This way a grant cleans such a leftover up.
     *
     * @param foInfo the folder to revoke all permissions on
     */
    public void revokeAllFolderPermissions(FolderInfo foInfo) {
        revoke(FolderPermission.read(foInfo), FolderPermission.readWrite(foInfo),
            FolderPermission.admin(foInfo), FolderPermission.owner(foInfo));
    }

    public void revokeAllGroupAdminPermissions() {
        for (Permission p : permissions) {
            if (p instanceof GroupAdminPermission) {
                permissions.remove(p);
            }
        }
    }

    public void revokeAllOrgAdminPermissions() {
        for (Permission p : permissions) {
            if (p instanceof OrganizationAdminPermission) {
                permissions.remove(p);
            }
        }
    }

    public boolean hasPermission(Permission permission) {
        Reject.ifNull(permission, "Permission is null");
        return hasPermission(permission, new HashSet<>());
    }

    private boolean hasPermission(Permission permission, Set<String> visited) {
        if (oid != null && !visited.add(oid)) {
            return false;
        }
        if (permissions == null) {
            LOG.severe("Illegal group " + name + ", permissions is null");
            return false;
        }
        for (Permission p : permissions) {
            if (p == null) {
                LOG.severe("Got null permission on " + this);
                continue;
            }
            if (p.equals(permission)) {
                return true;
            }
            if (p.implies(permission)) {
                return true;
            }
        }
        if (parents != null) {
            for (Group parent : parents) {
                if (parent != null && parent.hasPermission(permission, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    public Collection<Group> getParents() {
        if (parents == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(parents);
    }

    public void addParent(Group parent) {
        Reject.ifNull(parent, "Parent is null");
        Reject.ifTrue(this.equals(parent), "A group cannot be its own parent");
        if (parents == null) {
            parents = new CopyOnWriteArraySet<>();
        }
        parents.add(parent);
    }

    public void removeParent(Group parent) {
        Reject.ifNull(parent, "Parent is null");
        if (parents != null) {
            parents.remove(parent);
        }
    }

    public boolean wouldCreateCycle(Group candidateChild) {
        Reject.ifNull(candidateChild, "Candidate child is null");
        if (this.equals(candidateChild)) {
            return true;
        }
        return isReachableViaParents(this, candidateChild, new HashSet<>());
    }

    private static boolean isReachableViaParents(Group from, Group target,
                                                 Set<String> visited) {
        if (from == null) {
            return false;
        }
        if (from.equals(target)) {
            return true;
        }
        if (from.oid != null && !visited.add(from.oid)) {
            return false;
        }
        if (from.parents == null || from.parents.isEmpty()) {
            return false;
        }
        for (Group p : from.parents) {
            if (isReachableViaParents(p, target, visited)) {
                return true;
            }
        }
        return false;
    }

    public Collection<FolderInfo> getAllFolders() {
        Set<FolderInfo> result = new HashSet<>();
        collectAllFolders(result, new HashSet<>());
        return result;
    }

    private void collectAllFolders(Set<FolderInfo> sink, Set<String> visited) {
        if (oid != null && !visited.add(oid)) {
            return;
        }
        if (permissions != null) {
            for (Permission p : permissions) {
                if (p instanceof FolderPermission) {
                    FolderInfo f = ((FolderPermission) p).getFolder();
                    if (f != null) {
                        sink.add(f);
                    }
                }
            }
        }
        if (parents != null) {
            for (Group parent : parents) {
                if (parent != null) {
                    parent.collectAllFolders(sink, visited);
                }
            }
        }
    }

    public boolean hasAnyFolderAdmin() {
        return hasAnyFolderAdmin(new HashSet<>());
    }

    private boolean hasAnyFolderAdmin(Set<String> visited) {
        if (oid != null && !visited.add(oid)) {
            return false;
        }
        if (permissions != null) {
            for (Permission p : permissions) {
                if (p instanceof FolderPermission) {
                    AccessMode mode = ((FolderPermission) p).getMode();
                    if (AccessMode.ADMIN.equals(mode)
                        || AccessMode.OWNER.equals(mode)) {
                        return true;
                    }
                }
            }
        }
        if (parents != null) {
            for (Group parent : parents) {
                if (parent != null && parent.hasAnyFolderAdmin(visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The {@link FolderPermission} granted directly on the given folder.
     * <p>
     * CAUTION - this does NOT mirror {@link Account#getPermissionFor(FolderInfo)}, which answers the
     * EFFECTIVE permission (own plus groups plus inherited from the top folder). This one has always
     * answered the DIRECT permission, and it keeps doing so, because its callers select their candidates
     * via {@code findWithFolderPermission(folderInfo)} and want exactly that. On a top folder the
     * difference is invisible, on a subfolder it is not - so the two meanings now have their own names
     * ({@link #getDirectPermissionFor(FolderInfo)} / {@link #getAllowedAccess(FolderInfo)}) and the
     * callers get moved over separately, as that changes what an ACL view displays.
     *
     * @param foInfo the folder to get the direct permission on
     * @return the granted FolderPermission, or {@code null} if none is granted on that folder itself
     */
    public FolderPermission getPermissionFor(FolderInfo foInfo) {
        return getDirectPermissionFor(foInfo);
    }

    /**
     * PFS-5510: The {@link FolderPermission} granted DIRECTLY on the given folder - this group's own
     * permissions only, matched on exactly that folder. Parent groups do not count and nothing is
     * inherited from the top folder, so an ACL view can tell an explicit grant apart from an inherited
     * one. Mirrors {@link Account#getDirectPermissionFor(FolderInfo)}.
     *
     * @param foInfo the folder to get the direct permission on
     * @return the granted FolderPermission, or {@code null} if none is granted on that folder itself
     */
    public FolderPermission getDirectPermissionFor(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        for (Permission perm : permissions) {
            if (perm instanceof FolderPermission) {
                FolderPermission foPerm = (FolderPermission) perm;
                if (foInfo.equals(foPerm.getFolder())) {
                    return foPerm;
                }
            }
        }
        return null;
    }

    /**
     * The folders this group holds a permission on ITSELF. Parent groups are not followed - use
     * {@link #getAllFolders()} for that. Callers that show or edit the group's own folder assignment
     * want this one; callers that resolve access want {@link #getAllFolders()}.
     *
     * @return the folders of this group's own permissions
     */
    public Collection<FolderInfo> getFolders() {
        Collection<FolderInfo> folder = new ArrayList<FolderInfo>(
            permissions.size());

        for (Permission p : permissions) {
            if (p instanceof FolderPermission) {
                FolderPermission fp = (FolderPermission) p;

                folder.add(fp.getFolder());
            }
        }

        return folder;
    }

    /**
     * @param folder
     * @return the permission on the given folder. AccessMode.NO_ACCESS for no
     * access.
     */
    public AccessMode getAllowedAccess(FolderInfo folder) {
        if (hasPermission(FolderPermission.owner(folder))) {
            return FolderPermission.owner(folder).getMode();
        } else if (hasPermission(FolderPermission.admin(folder))) {
            return FolderPermission.admin(folder).getMode();
        } else if (hasPermission(FolderPermission.readWrite(folder))) {
            return FolderPermission.readWrite(folder).getMode();
        } else if (hasPermission(FolderPermission.read(folder))) {
            return FolderPermission.read(folder).getMode();
        }
        return AccessMode.NO_ACCESS;
    }

    /**
     * PFS-5510: The EFFECTIVE access of this group (including permissions inherited from parent
     * groups) on the addressed location: resolves the innermost subfolder the group holds a
     * permission on that encloses ({@code folder} + {@code relativeName}) and evaluates the access
     * there. Purely structure-based (the permission lists carry the subfolder FolderInfos incl. their
     * location), so it works without a mounted folder. For an INHERITING subfolder the result is
     * never lower than the plain {@link #getAllowedAccess(FolderInfo)}, since top-folder permissions
     * inherit downward. Accepts a top folder or a subfolder as {@code folder}.
     * <p>
     * Interrupted permission inheritance (PFC-3543) is honored by the resolution itself: an interrupted
     * subfolder is a barrier it does not look past, also when the group holds no permission on it. See
     * {@link Account#getAllowedAccess(FolderInfo, String)}.
     *
     * @param folder       the addressed folder (top folder or shared subfolder)
     * @param relativeName the addressed path relative to {@code folder}, may be blank
     * @return the effective access mode, {@link AccessMode#NO_ACCESS} for none
     */
    public AccessMode getAllowedAccess(FolderInfo folder, String relativeName) {
        FolderInfo governing = FolderInfo.findEnclosingSubFolder(getAllFolders(), folder, relativeName);
        return getAllowedAccess(governing != null ? governing : folder);
    }

    /**
     * PFS-5510: Answers if the group is allowed to write at the addressed location - the EFFECTIVE
     * access resolves the enclosing shared subfolder (incl. permissions inherited from parent
     * groups), see {@link #getAllowedAccess(FolderInfo, String)}.
     *
     * @param foInfo     the addressed folder (top folder or shared subfolder)
     * @param subDirPath the addressed path relative to {@code foInfo}, may be blank
     * @return true if the group is allowed to write at the addressed location.
     */
    public boolean hasWritePermissions(FolderInfo foInfo, String subDirPath) {
        return getAllowedAccess(foInfo, subDirPath).compareTo(AccessMode.READ_WRITE) >= 0;
    }

    /**
     * PFS-5510: Answers if the group is allowed to read at the addressed location - the EFFECTIVE
     * access resolves the enclosing shared subfolder (incl. permissions inherited from parent
     * groups), see {@link #getAllowedAccess(FolderInfo, String)}.
     *
     * @param foInfo     the addressed folder (top folder or shared subfolder)
     * @param subDirPath the addressed path relative to {@code foInfo}, may be blank
     * @return true if the group is allowed to read at the addressed location.
     */
    public boolean hasReadPermissions(FolderInfo foInfo, String subDirPath) {
        return getAllowedAccess(foInfo, subDirPath).compareTo(AccessMode.READ) >= 0;
    }

    /**
     * PFS-5510: Like {@link #hasWritePermissions(FolderInfo, String)}, addressed by the file itself.
     *
     * @param fileInfo the addressed file/directory
     * @return true if the group is allowed to write at the file's location.
     */
    public boolean hasWritePermissions(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        return hasWritePermissions(fileInfo.getFolderInfo(), fileInfo.getRelativeName());
    }

    /**
     * PFS-5510: Like {@link #hasReadPermissions(FolderInfo, String)}, addressed by the file itself.
     *
     * @param fileInfo the addressed file/directory
     * @return true if the group is allowed to read at the file's location.
     */
    public boolean hasReadPermissions(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        return hasReadPermissions(fileInfo.getFolderInfo(), fileInfo.getRelativeName());
    }
    
    /**
     * @return An unmodifiable collection of all
     *         {@link OrganizationAdminPermission OrganizationAdminPermissions}.
     *         If the user does not have any OrganizationAdminPermission, an
     *         empty collection will be returned.
     */
    public Collection<OrganizationAdminPermission> getOrgAdminPermissions() {
        Collection<OrganizationAdminPermission> orgAdmins = new ArrayList<>();
        for (Permission perm : permissions) {
            if (perm instanceof OrganizationAdminPermission) {
                orgAdmins.add((OrganizationAdminPermission) perm);
            }
        }
        return Collections.unmodifiableCollection(orgAdmins);
    }

    public String getOID() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getDisplayName() {
        return name;
    }

    public void setLdapDN(String newLdapDN) {
        ldapDN = newLdapDN;
    }

    public String getLdapDN() {
        return ldapDN;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = StringUtils.cutNotes(notes);
    }
    
    /**
     * Adds a line of info with the current date to the notes of that account.
     *
     * @param infoText
     */
    public void addNotesWithDate(String infoText) {
        if (StringUtils.isBlank(infoText)) {
            return;
        }
        String infoLine = Format.formatDateCanonical(new Date());
        infoLine += ": ";
        infoLine += infoText;
        String newNotes;
        if (StringUtils.isBlank(notes)) {
            newNotes = infoLine;
        } else {
            newNotes = notes + "\n" + infoLine;
        }
        setNotes(newNotes);
    }

    public String getOrganizationOID() {
        return organizationOID;
    }

    public void setOrganizationOID(String organizationOID) {
        this.organizationOID = organizationOID;
    }

    public GroupInfo createInfo() {
        return new GroupInfo(oid, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Group)) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        return (this.oid.equals(((Group) obj).oid));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oid == null) ? 0 : oid.hashCode());
        return result;
    }

    public synchronized void convertCollections() {
        if (!(permissions instanceof CopyOnWriteArrayList<?>)) {
            Collection<Permission> newPermissions = new CopyOnWriteArrayList<Permission>(
                permissions);
            permissions = newPermissions;
        }
        if (!(parents instanceof CopyOnWriteArraySet<?>)) {
            parents = new CopyOnWriteArraySet<>(parents);
        }
    }

    @Override
    public String toString() {
        return "Group [name=" + name + ", organizationOID=" + organizationOID
            + "]";
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof GroupInfoProto.GroupInfo) {
            GroupInfoProto.GroupInfo proto  = (GroupInfoProto.GroupInfo)mesg;
            this.oid                = proto.getId();
            this.name               = proto.getName();
            this.organizationOID    = proto.getOrganizationId();
            this.permissions        = new CopyOnWriteArrayList<Permission>();
            for (PermissionInfoProto.PermissionInfo permissionInfoProto: proto.getPermissionInfosList()) {
                switch(permissionInfoProto.getPermissionType()) {
                    case ADMIN :
                        this.permissions.add(new AdminPermission(permissionInfoProto));
                        break;
                    case CHANGE_PREFERENCES :
                        this.permissions.add(new ChangePreferencesPermission(permissionInfoProto));
                        break;
                    case CHANGE_TRANSFER_MODE :
                        this.permissions.add(new ChangeTransferModePermission(permissionInfoProto));
                        break;
                    case COMPUTERS_APP :
                        this.permissions.add(new ComputersAppPermission(permissionInfoProto));
                        break;
                    case CONFIG_APP :
                        this.permissions.add(new ConfigAppPermission(permissionInfoProto));
                        break;
                    case FOLDER_ADMIN :
                        this.permissions.add(new FolderAdminPermission(permissionInfoProto));
                        break;
                    case FOLDER_CREATE :
                        this.permissions.add(new FolderCreatePermission(permissionInfoProto));
                        break;
                    case FOLDER_OWNER :
                        // A group never owns a folder (see isInvalidGrant). This import path writes to
                        // the permission list directly, so the invariant has to be enforced here too -
                        // otherwise a peer or client could push an ownership in through the back door.
                        LOG.warning(this + ": Dropping owner permission from D2D import on "
                            + new FolderOwnerPermission(permissionInfoProto).getFolder()
                            + " - a group never owns a folder");
                        break;
                    case FOLDER_READ :
                        this.permissions.add(new FolderReadPermission(permissionInfoProto));
                        break;
                    case FOLDER_READ_WRITE :
                        this.permissions.add(new FolderReadWritePermission(permissionInfoProto));
                        break;
                    case FOLDER_REMOVE :
                        this.permissions.add(new FolderRemovePermission(permissionInfoProto));
                        break;
                    case GROUP_ADMIN :
                        this.permissions.add(new GroupAdminPermission(permissionInfoProto));
                        break;
                    case ORGANIZATION_ADMIN :
                        this.permissions.add(new OrganizationAdminPermission(permissionInfoProto));
                        break;
                    case ORGANIZATION_CREATE :
                        this.permissions.add(new OrganizationCreatePermission(permissionInfoProto));
                        break;
                    case SYSTEM_SETTINGS :
                        this.permissions.add(new SystemSettingsPermission(permissionInfoProto));
                        break;
                    case UNRECOGNIZED :
                        break;
                    default :
                        break;
                }
            }
        }
    }
    
    /**
     * Convert to D2D message
     *
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        GroupInfoProto.GroupInfo.Builder builder = GroupInfoProto.GroupInfo.newBuilder();
        builder.setClazzName("GroupInfo");
        if (this.oid != null) builder.setId(this.oid);
        if (this.getDisplayName() != null) builder.setName(this.getDisplayName());
        if (this.organizationOID != null) builder.setOrganizationId(this.organizationOID);
        for (Permission permission: this.permissions) {
            // Since the different permission classes do not have one common superclass we have to decide for each class separately
            if (permission instanceof FolderPermission) {
                builder.addPermissionInfos((PermissionInfoProto.PermissionInfo)((FolderPermission)permission).toD2D());
            }
            else if (permission instanceof GroupAdminPermission) {
                builder.addPermissionInfos((PermissionInfoProto.PermissionInfo)((GroupAdminPermission)permission).toD2D());
            }
            else if (permission instanceof OrganizationAdminPermission) {
                builder.addPermissionInfos((PermissionInfoProto.PermissionInfo)((OrganizationAdminPermission)permission).toD2D());
            }
            else if (permission instanceof SingletonPermission) {
                builder.addPermissionInfos((PermissionInfoProto.PermissionInfo)((SingletonPermission)permission).toD2D());
            }
        }
        return builder.build();
    }

    @Override
    public void setCreatedNowBy(Account caller) {
        this.auditFields().setCreatedNowBy(caller);
    }

    @Override
    public void setModifiedNowBy(Account caller) {
        this.auditFields().setModifiedNowBy(caller);
    }

    private AuditFields auditFields() {
        if (this.auditFields == null) {
            this.auditFields = new AuditFields();
        }
        return auditFields;
    }
}
