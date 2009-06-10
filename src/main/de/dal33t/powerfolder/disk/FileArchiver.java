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
package de.dal33t.powerfolder.disk;

import java.io.File;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * This class represents an archive for Files. Subclasses can store file as they
 * wish and even use seperate means etc. for different FileInfos.
 * 
 * @author Dennis Waldherr
 */
public interface FileArchiver {
    /**
     * Archives the given file under the given FileInfo. On return the file will
     * most likely have been removed. TODO: For Versioning the FileInfo should
     * be replaced by VersionedFile, also additional info like file delta should
     * be provided.
     * 
     * @param fileInfo
     * @param source
     * @param forcekeepSource
     *            if this is true the archiver <b>must</b> not remove the file
     *            given in the source parameter. Otherwise it <b>may</b> keep or
     *            delete the file.
     */
    void archive(FileInfo fileInfo, File source, boolean forcekeepSource);
}
