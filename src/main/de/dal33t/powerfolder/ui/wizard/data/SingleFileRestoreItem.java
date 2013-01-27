/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
 * $Id: SingleFileRestoreItem.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard.data;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * FileInfo and a boolean to indicate whether we have a local copy.
 */
public class SingleFileRestoreItem {

    private FileInfo fileInfo;
    private boolean local;

    public SingleFileRestoreItem(FileInfo fileInfo, boolean local) {
        this.fileInfo = fileInfo;
        this.local = local;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isLocal() {
        return local;
    }
}
