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

public class VersionedFile implements Serializable {
    private static final long serialVersionUID = -3233726429455214290L;

    private final FileInfo fileInfo;
    private final byte[] id;

    private boolean hashed = false;
    private int hashCode;

    public VersionedFile(FileInfo fileInfo, byte[] id) {
        super();
        Reject.ifNull(fileInfo, "FileInfo is null!");
        Reject.ifTrue(fileInfo.isTemplate(), "FileInfo must not be template!");
        Reject.ifNull(id, "id is null!");
        this.fileInfo = fileInfo;
        this.id = Arrays.copyOf(id, id.length);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public byte[] getId() {
        return Arrays.copyOf(id, id.length);
    }

    @Override
    public int hashCode() {
        if (!hashed) {
            hashed = true;
            final int prime = 31;
            int result = 1;
            result = prime * result
                + ((fileInfo == null) ? 0 : fileInfo.hashCode());
            result = prime * result + Arrays.hashCode(id);
            hashCode = result;
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
        VersionedFile other = (VersionedFile) obj;
        if (!fileInfo.isCompletelyIdentical(other.fileInfo))
            return false;
        if (!Arrays.equals(id, other.id))
            return false;
        return true;
    }
}
