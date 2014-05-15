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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.disk.dao;

import java.util.Collection;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Describes a Folder database access object. Offers basic CRUD and advanced
 * querying for <code>FileInfo</code> objects related to ONE <code>Folder</code>
 * . Database is separated into different "Domains". One collection of
 * {@link FileInfo} objects is hold per domain. A domain represents the
 * collection of {@link FileInfo} objects related to one {@link Member}. The
 * domain with identified by <code>null</code> represents the {@link FileInfo}s
 * of myself. A domain is in fact the Member's id field.
 *
 * @author sprajc
 * @see FileInfo
 * @see Folder
 */
public interface FileInfoDAO {

    /**
     * Stops the DAO and releases all resources
     */
    void stop();

    /**
     * Puts the file into the index, replacs existing FileInfo that has same
     * fileName, folderId and domain
     *
     * @param domain
     * @param fInfos
     */
    void store(String domain, FileInfo... fInfos);

    /**
     * Adds or updates the file in the DAO, replaces existing FileInfo that has
     * same fileName, folderId and domain
     *
     * @param domain
     * @param fInfos
     *            the <code>FileInfo</code>s to store.
     */
    void store(String domain, Collection<FileInfo> fInfos);

    /**
     * Finds the newest version of this file in the given domains.
     *
     * @param fInfo
     * @param domains
     * @return the newest FileInfo
     */
    FileInfo findNewestVersion(FileInfo fInfo, String... domains);

    /**
     * Finds the {@link FileInfo} in the given domain
     *
     * @param fInfo
     * @param domain
     * @return the matching {@link FileInfo} or null if not found
     */
    FileInfo find(FileInfo fInfo, String domain);

    /**
     * Deletes the FileInfo from the DAO.
     *
     * @param fInfo
     * @param domain
     */
    void delete(String domain, FileInfo fInfo);

    /**
     * Clears the whole domain by deleting all FileInfo within that domain.
     *
     * @param domain
     */
    void deleteDomain(String domain, int newInitialSize);

    /**
     * Finds all {@link FileInfo} objects of the given domain.
     * <P>
     * TODO: Think about the usage of this method. 2. Change return value to
     * Iterator<FileInfo>
     *
     * @param domain
     * @return all {@link FileInfo} objects of the given domain.
     * @deprecated Try to implement a more intelligent method to obtain FileInfo
     *             database information. Activation/Loading all FileInfos is a
     *             BAD idea generally.
     */
    @Deprecated
    Collection<FileInfo> findAllFiles(String domain);

    /**
     * All directories in the given base directory. optionally adds ALL
     * subdirectories recursively.
     *
     * @param domain
     * @return the collection of available directories
     */
    Collection<DirectoryInfo> findAllDirectories(String domain);

    /**
     * Finds all files in the given (sub) directory and domain only.
     * <p>
     * Does NOT included FileInfos that are excluded by a {@link DiskItemFilter}
     *
     * @param criteria
     *            the selection criteria.
     * @return the collection of matching directories.
     */
    Collection<FileInfo> findFiles(FileInfoCriteria criteria);

    /**
     * Finds all files in the given (sub) directory and domain only.
     * <p>
     * Does NOT included FileInfos that are excluded by a {@link DiskItemFilter}
     *
     * @param domain
     *            the domain to check.
     * @param path
     *            the path/relative name of the sub directory.
     * @param recursive
     *            true to recursively include all files from subdirectory too.
     * @return the collection of matching directories.
     * @deprecated use {@link #findFiles(FileInfoCriteria)}
     */
    Collection<FileInfo> findInDirectory(String domain, String path,
        boolean recursive);

    /**
     * Finds all files in the given (sub) directory and domain only.
     * <p>
     * Does NOT included FileInfos that are excluded by a {@link DiskItemFilter}
     *
     * @param domain
     *            the domain to check.
     * @param directoryInfo
     * @param recursive
     *            true to recursively include all files from subdirectory too.
     * @return the collection of matching directories.
     * @deprecated use {@link #findFiles(FileInfoCriteria)}
     */
    Collection<FileInfo> findInDirectory(String domain,
        DirectoryInfo directoryInfo, boolean recursive);

    /**
     * @param fileInfo
     *            the <code>FileInfo</code> to retrieve the file history for.
     * @return the FileHistory for the given FileInfo.
     */
    FileHistory getFileHistory(FileInfo fileInfo);

    /**
     * Counts all {@link FileInfo} objects of the given domain.
     *
     * @param domain
     * @param includeDirs
     *            if directories should be included in counting or not * @param
     * @param excludeIgnored
     *            If files should be counted that are ignored by
     *            {@link DiskItemFilter}
     * @return the number of FileInfos in this domain
     */
    int count(String domain, boolean includeDirs, boolean excludeIgnored);

    /**
     * Counts all {@link FileInfo} objects of the given domain that are in sync.
     *
     * @param domain
     * @param includeDirs
     *            if directories should be included in counting or not
     * @param excludeIgnored
     *            If files should be counted that are ignored by
     *            {@link DiskItemFilter}
     * @return the number of FileInfos in this domain that are in sync
     */
    int countInSync(String domain, boolean includeDirs, boolean excludeIgnored);

    /**
     * Counts the total size of all {@link FileInfo} objects of the given domain
     * that are in sync. Ignored files won't be included in the result.
     *
     * @param domain
     * @return the total number of bytes in this domain that are in sync
     */
    long bytesInSync(String domain);
}