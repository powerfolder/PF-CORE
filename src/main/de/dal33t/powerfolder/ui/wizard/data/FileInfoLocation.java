/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: FileInfoLocation.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard.data;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Class that holds a FileInfo and location (local / online) information.
 */
public class FileInfoLocation {

    private final FileInfo fileInfo;
    private final boolean local;
    private boolean online;

    public FileInfoLocation(FileInfo fileInfo, boolean local, boolean online) {
        this.fileInfo = fileInfo;
        this.local = local;
        this.online = online;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isOnline() {
        return online;
    }

    /**
     * Helper method for comparing locations.
     *
     * @return
     */
    public int getLocation() {
        return (local ? 1 : 0) + (online ? 2 : 0);
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
