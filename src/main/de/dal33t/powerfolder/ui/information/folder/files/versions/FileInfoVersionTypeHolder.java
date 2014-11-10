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
 */
package de.dal33t.powerfolder.ui.information.folder.files.versions;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Class holing a fileInfo and whether it is an online version.
 */
public class FileInfoVersionTypeHolder {

    private final FileInfo fileInfo;
    private final boolean online;

    public FileInfoVersionTypeHolder(FileInfo fileInfo, boolean online) {
        this.fileInfo = fileInfo;
        this.online = online;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isOnline() {
        return online;
    }
}
