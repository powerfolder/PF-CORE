/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: FolderAdminPermission.java 6582 2009-01-26 17:02:32Z tot $
 */
package de.dal33t.powerfolder.security;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Security settings per folder. Contains general security header data.
 * <P>
 * TRAC #1046
 *
 * @author sprajc
 */
@Deprecated
public class FolderSecuritySettings implements Serializable {
    public static final String PROPERTYNAME_FOLDER = "folder";
    public static final String PROPERTYNAME_DEFAULT_PERMISSION = "defaultPermission";

    private static final long serialVersionUID = 100L;

    /**
     * The date of the last modification.
     */
    private Date modifiedDate;

    @ManyToOne
    @JoinColumn(name = "folderInfo_id")
    private FolderInfo folder;

    /**
     * The permissions a computer/account inherits if no permission is found for
     * the given computer/account. null = no access permission.
     */
    @Type(type = "permissionType")
    private FolderPermission defaultPermission;

    /**
     * The permissions a web user inherits. Basically public access. null = no
     * access (default)
     */
    @Type(type = "permissionType")
    private FolderPermission webPermission;

    @SuppressWarnings("unused")
    private FolderSecuritySettings() {
        // NOP - for hibernate
    }

    public FolderSecuritySettings(FolderInfo folder) {
        this(folder, null);
    }

    public FolderSecuritySettings(FolderInfo folder,
        FolderPermission defaultPermission)
    {
        super();
        Reject.ifNull(folder, "Folder is null");
        this.folder = folder.intern();
        this.defaultPermission = defaultPermission;
        touch();
    }

    public FolderInfo getFolder() {
        return folder;
    }

    public FolderPermission getDefaultPermission() {
        return defaultPermission;
    }

    public void setDefaultPermission(FolderPermission defaultPermission) {
        this.defaultPermission = defaultPermission;
        touch();
    }

    public FolderPermission getWebPermission() {
        return webPermission;
    }

    public void setWebPermission(FolderPermission webPermission) {
        this.webPermission = webPermission;
        touch();
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Sets the last modified date to now.
     */
    public void touch() {
        this.modifiedDate = new Date();
    }

    public void internFolderInfos() {
        folder = folder.intern();
        if (defaultPermission != null) {
            defaultPermission.folder = folder;
        }
    }

    // General ****************************************************************

    @Override
    public String toString() {
        return "FolderSecSettings '" + folder.name + "' defPerm: "
            + defaultPermission;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((defaultPermission == null) ? 0 : defaultPermission.hashCode());
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FolderSecuritySettings other = (FolderSecuritySettings) obj;
        if (defaultPermission == null) {
            if (other.defaultPermission != null)
                return false;
        } else if (!defaultPermission.equals(other.defaultPermission))
            return false;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }
}
