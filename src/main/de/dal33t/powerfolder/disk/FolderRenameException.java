/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.disk;

import java.nio.file.Path;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class FolderRenameException extends Exception {

    private static final long serialVersionUID = 100L;

    private final Path file;
    private final FolderInfo fi;

    FolderRenameException(Path file, FolderInfo fi) {
        this.file = file;
        this.fi = fi;
    }

    FolderRenameException(Path file, FolderInfo fi, Throwable cause) {
        super(cause);
        this.file = file;
        this.fi = fi;
    }

    public Path getPath() {
        return file;
    }

    public FolderInfo getFolderInfo() {
        return fi;
    }

    @Override
    public String getMessage() {
        return "FolderInfo: " + (fi != null ? fi.toString() : "n/a") + " on file " + file;
    }
}
