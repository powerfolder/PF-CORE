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

/**
 * Administration permission on one folder.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderAdminPermission extends FolderPermission {
    private static final long serialVersionUID = 100L;

    @Deprecated
    private FolderInfo folder;

    public FolderAdminPermission(FolderInfo foInfo) {
        super(foInfo);
        Reject.ifNull(foInfo, "Folderinfo is null");
        folder = foInfo;
    }

    public boolean migrate() {
        if (super.folderInfo == null) {
            super.folderInfo = folder;
            folder = null;
            return true;
        }
        return false;
    }

    // Serialization compatibility ********************************************

//    private void readObject(ObjectInputStream in) throws IOException,
//        ClassNotFoundException
//    {
//        in.defaultReadObject();
//        if (folder != null) {
//            super.folderInfo = folder;
//        }
//    }
//
//    private void writeObject(ObjectOutputStream out) throws IOException {
//
//        if (super.folderInfo != null) {
//            folder = super.folderInfo;
//        }
//        out.defaultWriteObject();
//    }
}
