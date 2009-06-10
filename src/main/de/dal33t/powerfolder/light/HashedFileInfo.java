/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.util.Arrays;

import de.dal33t.powerfolder.util.Reject;

public class HashedFileInfo implements Serializable {
    private static final long serialVersionUID = -3233726429455214290L;

    private final FileInfo fileInfo;
    private final byte[] md5;

    private int hashCode;

    public HashedFileInfo(FileInfo fileInfo, byte[] md5) {
        super();
        Reject.ifNull(fileInfo, "FileInfo is null!");
        Reject.ifTrue(fileInfo.isTemplate(), "FileInfo must not be template!");
        Reject.ifNull(md5, "MD5 is null!");
        Reject.ifTrue(md5.length != 16, "Invalid MD5 of length: " + md5.length);
        this.fileInfo = fileInfo;
        this.md5 = Arrays.copyOf(md5, md5.length);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public byte[] getMD5() {
        return Arrays.copyOf(md5, md5.length);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            final int prime = 31;
            int result = 1;
            result = prime * result
                + ((fileInfo == null) ? 0 : fileInfo.hashCode());
            result = prime * result + Arrays.hashCode(md5);
            hashCode = result != 0 ? result : -1;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HashedFileInfo other = (HashedFileInfo) obj;
        if (!fileInfo.isVersionDateAndSizeIdentical(other.fileInfo))
            return false;
        if (!Arrays.equals(md5, other.md5))
            return false;
        return true;
    }
}
