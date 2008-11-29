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
package de.dal33t.powerfolder.ui.information.folder.files;

import java.io.File;

/**
 * User object for the directory tree.
 */
public class DirectoryTreeNodeUserObject {

    private String displayName;
    private File file;
    private boolean newFiles;

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
}
