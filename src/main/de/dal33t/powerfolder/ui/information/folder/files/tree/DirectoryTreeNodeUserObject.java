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

import java.io.File;

/**
 * User object for the directory tree.
 */
public class DirectoryTreeNodeUserObject {

    private final String displayName;
    private final File file;
    private final boolean newFiles;

    public DirectoryTreeNodeUserObject(String displayName, File file,
                                       boolean newFiles) {
        this.displayName = displayName;
        this.file = file;
        this.newFiles = newFiles;
    }

    public String getDisplayName() {
        return displayName;
    }

    public File getFile() {
        return file;
    }

    public boolean hasNewFiles() {
        return newFiles;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DirectoryTreeNodeUserObject that = (DirectoryTreeNodeUserObject) o;

        if (newFiles != that.newFiles) {
            return false;
        }
        if (!displayName.equals(that.displayName)) {
            return false;
        }
        if (!file.equals(that.file)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = displayName.hashCode();
        result = 31 * result + file.hashCode();
        result = 31 * result + (newFiles ? 1 : 0);
        return result;
    }
}
