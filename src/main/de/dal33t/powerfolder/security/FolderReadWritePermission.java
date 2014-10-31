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
 * $Id: FolderWritePermission.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * Permission that allows the user to write into the folder.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderReadWritePermission extends FolderPermission {
    private static final long serialVersionUID = 100L;

    /**
     * Construct externally with {@link FolderPermission#readWrite(FolderInfo)}
     *
     * @param foInfo
     */
    FolderReadWritePermission(FolderInfo foInfo) {
        super(foInfo);
    }

    public String getName() {
        return Translation.getTranslation("permissions.folder.read_write");
    }

    @Override
    public AccessMode getMode() {
        return AccessMode.READ_WRITE;
    }

    public boolean implies(Permission impliedPermision) {
        if (impliedPermision instanceof FolderReadPermission) {
            FolderReadPermission rp = (FolderReadPermission) impliedPermision;
            return rp.getFolder().equals(getFolder());
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FolderReadWritePermission))
            return false;
        final FolderReadWritePermission other = (FolderReadWritePermission) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }
}
