/*
 * Copyright 2004 - 2026 Christian Sprajc. All rights reserved.
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

import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.Account;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * Manages versioned archives of file changes within a folder.
 * <p>
 * Each time a file is modified or deleted, the previous version can be
 * archived for later retrieval. Implementations control where and how
 * archived versions are stored, how many versions are retained per file,
 * and how old versions are cleaned up.
 *
 * @see FileArchiverImpl
 * @see SubFolderFileArchiverProxy
 */
public interface FileArchiver {

    /**
     * Archives a version of a file.
     *
     * @param fileInfo the metadata of the file version to archive
     * @param source the path to the actual file content on disk
     * @param forceKeepSource if {@code true}, the source file is copied
     *     rather than moved
     * @throws IOException if an I/O error occurs during archiving
     */
    void archive(FileInfo fileInfo, Path source, boolean forceKeepSource)
            throws IOException;

    /**
     * @param fileInfo the file to check
     * @return {@code true} if at least one archived version exists
     */
    boolean hasArchivedFileInfo(FileInfo fileInfo);

    /**
     * Returns all archived versions of a file in no particular order.
     *
     * @param fileInfo the file to look up
     * @return list of archived versions, empty if none exist
     */
    List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo);

    /**
     * Returns all archived versions of a file sorted by version
     * (newest first).
     *
     * @param fileInfo the file to look up
     * @return sorted list of archived versions, empty if none exist
     */
    List<FileInfo> getSortedArchivedFilesInfos(FileInfo fileInfo);

    /**
     * Resolves the on-disk path of an archived file version.
     *
     * @param fileInfo the archived version to locate
     * @return the path to the archived file, or {@code null} if not
     *     found
     */
    Path getArchivedFile(FileInfo fileInfo);

    /**
     * Restores an archived version to the given target path.
     *
     * @param versionInfo the archived version to restore
     * @param target the destination path
     * @return {@code true} if the file was successfully restored
     * @throws IOException if an I/O error occurs during restore
     */
    boolean restore(FileInfo versionInfo, Path target)
        throws IOException;

    /**
     * Restores an archived version, replacing the current file.
     *
     * @param versionInfo the archived version to restore
     * @param currentFile the current file being replaced, may be
     *     {@code null}
     * @param target the destination path
     * @return {@code true} if the file was successfully restored
     * @throws IOException if an I/O error occurs during restore
     */
    boolean restore(FileInfo versionInfo, FileInfo currentFile,
        Path target) throws IOException;

    /**
     * @return the maximum number of versions kept per file
     */
    int getVersionsPerFile();

    /**
     * Sets the maximum number of versions to keep per file.
     *
     * @param versionsPerFile the new version limit
     */
    void setVersionsPerFile(int versionsPerFile);

    /**
     * @return the total size in bytes of all archived files managed
     *     by this archiver
     */
    long getSize();

    /**
     * Purges all archived versions in this folder.
     * The folder's archiver must be this instance.
     *
     * @param folder the folder whose archive is being purged
     * @param account the account performing the purge
     * @throws IOException if an I/O error occurs during deletion
     * @throws IllegalArgumentException if folder's archiver is not
     *     this instance
     */
    void purge(Folder folder, Account account) throws IOException;

    /**
     * Purges all archived versions of a specific file or directory.
     * The folder's archiver must be this instance.
     *
     * @param fileInfo the file or directory to purge
     * @param folder the folder containing the file
     * @param account the account performing the purge
     * @throws IOException if an I/O error occurs during deletion
     * @throws IllegalArgumentException if folder's archiver is not
     *     this instance
     */
    void purge(FileInfo fileInfo, Folder folder, Account account)
        throws IOException;

    /**
     * Performs periodic maintenance: removes versions older than the
     * cleanup date and trims excess versions beyond the configured
     * limit.
     *
     * @param cleanupDate versions older than this date are removed
     * @param dao the file info DAO for metadata lookups
     * @param folderInfo the folder being maintained
     * @param myAccount the local account info
     * @return list of FileInfos that were cleaned up
     */
    List<FileInfo> maintainAndCleanup(Date cleanupDate,
        FileInfoDAO dao, FolderInfo folderInfo,
        AccountInfo myAccount);
}
