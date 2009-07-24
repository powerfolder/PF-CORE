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

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Security settings per folder. Contains general security header data.
 * <P>
 * TRAC #1046
 * <P>
 * TODO Seal this message with server key.
 * 
 * @author sprajc
 */
public class FolderSecuritySettings implements Serializable {
    public static final String PROPERTYNAME_FOLDER = "folder";
    public static final String PROPERTYNAME_DEFAULT_PERMISSION = "defaultPermission";

    private static final long serialVersionUID = 100L;

    private final FolderInfo folder;

    /**
     * The permissions a computer/account inherits if no permission is found for
     * the given computer/account. null = no access permission.
     */
    private FolderPermission defaultPermission;

    public FolderSecuritySettings(FolderInfo folder)
    {
        this(folder, null);
    }
    public FolderSecuritySettings(FolderInfo folder,
        FolderPermission defaultPermission)
    {
        super();
        Reject.ifNull(folder, "Folder is null");
        this.folder = folder;
        this.defaultPermission = defaultPermission;
    }

    public FolderInfo getFolder() {
        return folder;
    }

    public FolderPermission getDefaultPermission() {
        return defaultPermission;
    }

    public void setDefaultPermission(FolderPermission defaultPermission) {
        this.defaultPermission = defaultPermission;
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
