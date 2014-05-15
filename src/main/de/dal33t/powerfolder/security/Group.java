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

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;

/**
 * A group of accounts.
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @version $Revision: 1.5 $
 */
@Entity(name = "AGroup")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Group implements Serializable {

    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_GROUPNAME = "name";
    public static final String PROPERTYNAME_NOTES = "notes";
    public static final String PROPERTYNAME_PERMISSIONS = "permissions";
    public static final String PROPERTYNAME_ORGANIZATION_ID = "organizationOID";

    private static final long serialVersionUID = 100L;

    private static final Logger LOG = Logger.getLogger(Group.class.getName());

    @Id
    private String oid;
    @Index(name = "IDX_GROUP_NAME")
    @Column(nullable = false)
    private String name;

    @Column(length = 1024)
    private String notes;

    @Index(name = "IDX_GRP_ORG_ID")
    @Column(nullable = true, unique = false)
    private String organizationOID;

    @CollectionOfElements
    @Type(type = "permissionType")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Permission> permissions;

    protected Group() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String newNotes) {
        notes = newNotes;
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
}
