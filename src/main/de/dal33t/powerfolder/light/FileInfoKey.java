/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: FileInfo.java 13684 2010-09-06 12:19:57Z harry $
 */
package de.dal33t.powerfolder.light;

import java.util.Map;

import de.dal33t.powerfolder.util.Reject;

/**
 * Helper class to construct a key for a {@link FileInfo} which can be used in
 * {@link Map}s using a additional fields to compare them.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfoKey {
    private FileInfo fileInfo;
    private Type type;

    public FileInfoKey(FileInfo fileInfo, Type type) {
        super();
        Reject.ifNull(fileInfo, "FileInfo is null");
        Reject.ifNull(type, "Type is null");
        this.fileInfo = fileInfo;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileInfoKey)) {
            return false;
        }
        FileInfoKey other = (FileInfoKey) obj;
        if (other.type != type) {
            return false;
        }
        FileInfo otherInfo = other.fileInfo;

        switch (type) {
            case VERSION :
                return fileInfo.getVersion() == otherInfo.getVersion()
                    && fileInfo.equals(other);
            case VERSION_DATE_SIZE :
                return fileInfo.isVersionDateAndSizeIdentical(otherInfo);
        }

        // Default case:
        return fileInfo.equals(otherInfo);
    }

    @Override
    public int hashCode() {
        switch (type) {
            case VERSION :
                return fileInfo.hashCode() + fileInfo.getVersion();
            case VERSION_DATE_SIZE :
                return fileInfo.hashCode() + fileInfo.getVersion()
                    + fileInfo.getModifiedDate().hashCode()
                    + Long.valueOf(fileInfo.getSize()).hashCode();
        }

        // Default case:
        return fileInfo.hashCode();
    }

    public enum Type {
        DEFAULT, VERSION, VERSION_DATE_SIZE
    }
}
