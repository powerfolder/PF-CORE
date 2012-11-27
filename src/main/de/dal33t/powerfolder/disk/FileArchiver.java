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
import java.io.IOException;
import java.util.List;
import java.util.Date;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ArchiveMode;

/**
 * This class represents an archive for Files. Subclasses can store file as they
 * wish and even use seperate means etc. for different FileInfos.
 * 
 * @author Dennis Waldherr
 */
public interface FileArchiver {

    /**
     * Archives the given file under the given FileInfo. On return the file will
     * most likely have been removed.
     * 
     * @param fileInfo
     *            the local file info for this file.
     * @param source
     *            the actual file to be archived.
     * @param forceKeepSource
     *            if this is true the archiver <b>must</b> not remove the file
     *            given in the source parameter. Otherwise it <b>may</b> keep or
     *            delete the file.
     * @throws IOException
     *             if the archiving failed
     */
    void archive(FileInfo fileInfo, File source, boolean forceKeepSource)
        throws IOException;

    ArchiveMode getArchiveMode();

    /**
     * Retrieves a List of existing FileInfos for an archived file.
     *
     * NOTE - implementors should ensure this list is read-only
     * 
     * @param fileInfo
     *            fileInfo of the file to get archived versions for.
     * @return list of archived {@link FileInfo}.
     */
    List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo);

    /**
     * @param fileInfo
     * @return true if archived {@link FileInfo} exists.
     */
    boolean hasArchivedFileInfo(FileInfo fileInfo);

    /**
     * Restores/Copies a file version from the archive to the target location.
     * Does NOT deleted the file in the archive. Does NOT scan the related
     * folder!
     * <p>
     * TODO Handle existing target file!
     * 
     * @param versionInfo
     *            the FileInfo of the archived file.
     * @param target
     * @return true if the file was actually restored. false if not in archive.
     * @throws IOException
     *             problem restoring the file.
     */
    boolean restore(FileInfo versionInfo, File target) throws IOException;

    void setVersionsPerFile(int versionsPerFile);

    int getVersionsPerFile();

    /**
     * @return the total size in bytes occupied by this archive.
     */
    long getSize();

    /**
     * Do any required maintenance on the versions kept
     * 
     * @return true if succeed. false if not
     */
    boolean maintain();

    void purge() throws IOException;

    /**
     * Delete archives older that a specified number of days.
     *
     * @param cleanupDate Age in days of archive files to delete.
     */
    void cleanupOldArchiveFiles(Date cleanupDate);
}
