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
* $Id: DirectoryTreeNodeUserObject.java 5766 2008-11-07 22:52:58Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.tree;

import de.dal33t.powerfolder.light.DirectoryInfo;

/**
 * User object for the directory tree.
 */
public class DirectoryTreeNodeUserObject {

    private final DirectoryInfo directoryInfo;
    private final String displayName;
    private final boolean newFiles;

    public DirectoryTreeNodeUserObject(DirectoryInfo directoryInfo,
                                       String displayName, boolean newFiles) {
        this.directoryInfo = directoryInfo;
        this.displayName = displayName;
        this.newFiles = newFiles;
    }

    public DirectoryInfo getDirectoryInfo() {
        return directoryInfo;
    }

    public boolean hasNewFiles() {
        return newFiles;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DirectoryTreeNodeUserObject that = (DirectoryTreeNodeUserObject) obj;

        if (newFiles != that.newFiles) {
            return false;
        }
        if (directoryInfo != null ? !directoryInfo.equals(that.directoryInfo) : that.directoryInfo != null) {
            return false;
        }
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = directoryInfo != null ? directoryInfo.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (newFiles ? 1 : 0);
        return result;
    }
}
