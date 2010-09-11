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

    private final String displayName;
    private final String relativeName;
    private final boolean newFilesDeep;

    public DirectoryTreeNodeUserObject(String displayName, String relativeName,
                                       boolean newFilesDeep) {
        this.displayName = displayName;
        this.relativeName = relativeName;
        this.newFilesDeep = newFilesDeep;
    }

    public boolean hasNewFilesDeep() {
        return newFilesDeep;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRelativeName() {
        return relativeName;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DirectoryTreeNodeUserObject that = (DirectoryTreeNodeUserObject) obj;

        if (newFilesDeep != that.newFilesDeep) {
            return false;
        }
        if (!displayName.equals(that.displayName)) {
            return false;
        }
        if (!relativeName.equals(that.relativeName)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = displayName.hashCode();
        result = 31 * result + relativeName.hashCode();
        result = 31 * result + (newFilesDeep ? 1 : 0);
        return result;
    }
}
