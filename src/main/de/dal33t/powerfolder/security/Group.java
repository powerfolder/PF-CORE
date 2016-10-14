/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: Account.java 18110 2012-02-13 02:41:13Z tot $
 */
package de.dal33t.powerfolder.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Type;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.protocol.GroupProto;
import de.dal33t.powerfolder.protocol.PermissionProto;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * A group of accounts.
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @version $Revision: 1.5 $
 */
@Entity(name = "AGroup")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Group implements Serializable, D2DObject {

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
    @Index(name = "IDX_GROUP_NAME")
    @Column(nullable = false)
    private String name;

    @Index(name = "IDX_GROUP_LDAPDN")
    @Column(length = 512)
    private String ldapDN;

    @Column(length = 2048)
    private String notes;

    @Index(name = "IDX_GRP_ORG_ID")
    @Column(nullable = true, unique = false)
    private String organizationOID;

    @CollectionOfElements
    @Type(type = "permissionType")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Permission> permissions;

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
        this.permissions = new CopyOnWriteArrayList<Permission>();
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
            if (hasPermission(p)) {
                // Skip
                continue;
            } else {
                permissions.add(p);
            }
        }
    }

    public void revoke(Permission... revokePermission) {
        Reject.ifNull(revokePermission, "Permission is null");
        for (Permission p : revokePermission) {
            if (permissions.remove(p)) {
                LOG.fine("Revoked permission from " + this + ": " + p);
            }
        }
    }

    public void revokeAllFolderPermissions(FolderInfo foInfo) {
        revoke(FolderPermission.read(foInfo),
            FolderPermission.readWrite(foInfo), FolderPermission.admin(foInfo));
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

        return false;
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

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

    synchronized void convertCollections() {
        if (!(permissions instanceof CopyOnWriteArrayList<?>)) {
            Collection<Permission> newPermissions = new CopyOnWriteArrayList<Permission>(
                permissions);
            permissions = newPermissions;
        }
    }

    @Override
    public String toString() {
        return "Group [name=" + name + ", organizationOID=" + organizationOID
            + "]";
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof GroupProto.Group) {
            GroupProto.Group proto  = (GroupProto.Group)mesg;
            this.name               = proto.getName();
            this.ldapDN             = proto.getLdapDn();
            this.notes              = proto.getNotes();
            this.organizationOID    = proto.getOrganizationOid();
            this.permissions        = new CopyOnWriteArrayList<Permission>();
            for (PermissionProto.Permission permissionProto: proto.getPermissionsList()) {
                switch(permissionProto.getPermissionType()) {
                    case ADMIN :
                        this.permissions.add(new AdminPermission(permissionProto));
                        break;
                    case CHANGE_PREFERENCES :
                        this.permissions.add(new ChangePreferencesPermission(permissionProto));
                        break;
                    case CHANGE_TRANSFER_MODE :
                        this.permissions.add(new ChangeTransferModePermission(permissionProto));
                        break;
                    case COMPUTERS_APP :
                        this.permissions.add(new ComputersAppPermission(permissionProto));
                        break;
                    case CONFIG_APP :
                        this.permissions.add(new ConfigAppPermission(permissionProto));
                        break;
                    case FOLDER_ADMIN :
                        this.permissions.add(new FolderAdminPermission(permissionProto));
                        break;
                    case FOLDER_CREATE :
                        this.permissions.add(new FolderCreatePermission(permissionProto));
                        break;
                    case FOLDER_OWNER :
                        this.permissions.add(new FolderOwnerPermission(permissionProto));
                        break;
                    case FOLDER_READ :
                        this.permissions.add(new FolderReadPermission(permissionProto));
                        break;
                    case FOLDER_READ_WRITE :
                        this.permissions.add(new FolderReadWritePermission(permissionProto));
                        break;
                    case FOLDER_REMOVE :
                        this.permissions.add(new FolderRemovePermission(permissionProto));
                        break;
                    case GROUP_ADMIN :
                        this.permissions.add(new GroupAdminPermission(permissionProto));
                        break;
                    case ORGANIZATION_ADMIN :
                        this.permissions.add(new OrganizationAdminPermission(permissionProto));
                        break;
                    case ORGANIZATION_CREATE :
                        this.permissions.add(new OrganizationCreatePermission(permissionProto));
                        break;
                    case SYSTEM_SETTINGS :
                        this.permissions.add(new SystemSettingsPermission(permissionProto));
                        break;
                    case UNRECOGNIZED :
                        break;
                    default :
                        break;
                }
            }
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        GroupProto.Group.Builder builder = GroupProto.Group.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.name != null) builder.setName(this.name);
        if (this.ldapDN != null) builder.setLdapDn(this.ldapDN);
        if (this.notes != null) builder.setNotes(this.notes);
        if (this.organizationOID != null) builder.setOrganizationOid(this.organizationOID);
        for (Permission permission: this.permissions) {
            // Since the different permission classes do not have one common superclass we have to decide for each class separately
            if (permission instanceof FolderPermission) {
                builder.addPermissions((PermissionProto.Permission)((FolderPermission)permission).toD2D());
            }
            else if (permission instanceof GroupAdminPermission) {
                builder.addPermissions((PermissionProto.Permission)((GroupAdminPermission)permission).toD2D());
            }
            else if (permission instanceof OrganizationAdminPermission) {
                builder.addPermissions((PermissionProto.Permission)((OrganizationAdminPermission)permission).toD2D());
            }
            else if (permission instanceof SingletonPermission) {
                builder.addPermissions((PermissionProto.Permission)((SingletonPermission)permission).toD2D());
            }
        }
        return builder.build();
    }

}
