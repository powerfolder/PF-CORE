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
 * $Id$
 */
package de.dal33t.powerfolder.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Administration permission on one folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderAdminPermission extends FolderPermission {

    private static final long serialVersionUID = 100L;

    @Deprecated
    private FolderInfo folder;

    /**
     * Construct externally with {@link FolderPermission#admin(FolderInfo)}
     * 
     * @param foInfo
     */
    FolderAdminPermission(FolderInfo foInfo) {
        super(foInfo);
        Reject.ifNull(foInfo, "Folderinfo is null");
        folder = foInfo;
    }

    public String getName() {
        return Translation.getTranslation("permissions.folder.admin");
    }

    @Override
    public AccessMode getMode() {
        return AccessMode.ADMIN;
    }

    public boolean migrate() {
        if (super.folder == null) {
            super.folder = folder;
            folder = null;
            return true;
        }
        return false;
    }

    public boolean implies(Permission impliedPermision) {
        if (impliedPermision instanceof FolderReadPermission) {
            FolderReadPermission rp = (FolderReadPermission) impliedPermision;
            return rp.getFolder().equals(getFolder());
        } else if (impliedPermision instanceof FolderReadWritePermission) {
            FolderReadWritePermission rwp = (FolderReadWritePermission) impliedPermision;
            return rwp.getFolder().equals(getFolder());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof FolderAdminPermission))
            return false;
        FolderAdminPermission other = (FolderAdminPermission) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }

    // Serialization compatibility ********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        if (folder != null) {
            super.folder = folder;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        if (super.folder != null) {
            folder = super.folder;
        }
        out.defaultWriteObject();
    }
}
