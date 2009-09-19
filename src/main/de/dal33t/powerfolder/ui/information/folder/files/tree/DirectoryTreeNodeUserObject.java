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

/**
 * User object for the directory tree.
 */
public class DirectoryTreeNodeUserObject {

    /**
     * The display name for the node. May not be a true path or file name.
     */
    private final String displayName;

    /** The relative name of the directory from the root, like /bob/test/sub */
    private final String relativeName;

    private final boolean newFiles;

    public DirectoryTreeNodeUserObject(String displayName, String relativeName, boolean newFiles) {
        this.displayName = displayName;
        this.relativeName = relativeName;
        this.newFiles = newFiles;
    }

    public String getRelativeName() {
        return relativeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasNewFiles() {
        return newFiles;
    }

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
        if (!relativeName.equals(that.relativeName)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = relativeName.hashCode();
        result = 31 * result + (newFiles ? 1 : 0);
        return result;
    }
}
