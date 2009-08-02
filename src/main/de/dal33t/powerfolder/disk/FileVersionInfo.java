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
 * $Id: FileVersionInfo.java 5857 2008-11-24 02:30:02Z tot $
 */
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.light.FileInfo;

import java.util.Date;

/**
 * Class holding information about a file version.
 */
public class FileVersionInfo implements Comparable<FileVersionInfo> {

    /** This is the FileInfo that the version is for. */
    private final FileInfo baseFileInfo;

    /** File version number. */
    private final int version;

    /** The size of the version file. */
    private final long size;

    /** The date the version was created. */
    private final Date created;

    public FileVersionInfo(FileInfo baseFileInfo, int version, long size,
                           Date created) {
        this.baseFileInfo = baseFileInfo;
        this.version = version;
        this.size = size;
        this.created = created;
    }

    public FileInfo getBaseFileInfo() {
        return baseFileInfo;
    }

    public int getVersion() {
        return version;
    }

    public long getSize() {
        return size;
    }

    public Date getCreated() {
        return created;
    }

    /**
     * This assumes that we are comparing versions with the same base file
     * info ;-)
     *
     * @param o
     * @return
     */
    public int compareTo(FileVersionInfo o) {
        return version - o.version;
    }

    /**
     * Only care about base file info and version for equals (like compare).
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        FileVersionInfo that = (FileVersionInfo) obj;

        if (!baseFileInfo.equals(that.baseFileInfo)) {
            return false;
        }
        if (version != that.version) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = baseFileInfo.hashCode();
        result = 31 * result + version;
        return result;
    }
}
