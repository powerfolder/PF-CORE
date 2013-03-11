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
 * $Id$
 */
package de.dal33t.powerfolder.clientserver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderStatisticInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;

/**
 * Access/Control over folders of a server.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface FolderService {

    /**
     * Creates a new folder to be mirrored by the server. Default Sync
     * 
     * @param foInfo
     * @param profile
     *            the transfer mode to use on the server or null if default mode
     *            of server should be used.
     * @see SyncProfile#getDefault(de.dal33t.powerfolder.Controller)
     */
    void createFolder(FolderInfo foInfo, SyncProfile profile);
    
    void createFolder(FolderInfo foInfo, SyncProfile profile,
        File targetDir);

    /**
     * Removes a folder from the account. Required owner permission if
     * deletedFiles is true.
     * 
     * @param foInfo
     * @param deleteFiles
     *            true to delete all file contained in the folder. Requires
     *            ownership.
     * @deprecated legacy support. remove after major 4.0 distribution
     */
    void removeFolder(FolderInfo foInfo, boolean deleteFiles);
    

    /**
     * Removes a folder from the account. Required owner permission if
     * deletedFiles is true.
     * 
     * @param foInfo
     * @param deleteFiles
     *            true to delete all file contained in the folder. Requires
     *            ownership.
     * @param removePermission
     *            if the permission to this folder should also be removed.
     */
    void removeFolder(FolderInfo foInfo, boolean deleteFiles,
        boolean removePermission);

    /**
     * #854
     * 
     * @param foInfo
     *            the folder to change the name for!
     * @param newName
     *            the new name of the folder.
     * @return the new folder info object.
     */
    FolderInfo renameFolder(FolderInfo foInfo, String newName);

    /**
     * Invites a user to a folder. The invited user gains read/write
     * permissions.
     * 
     * @param user
     *            the name of the user to be invited
     * @param invitation
     *            the folder to be invited to
     * @deprecated Use {@link SendInvitationEmail} instead
     */
    @Deprecated
    void inviteUser(Invitation invitation, String user);

    /**
     * @param request
     */
    void sendInvitationEmail(SendInvitationEmail request);

    /**
     * Changes the sync profile on the remote server for this folder.
     * 
     * @param foInfo
     * @param profile
     */
    void setSyncProfile(FolderInfo foInfo, SyncProfile profile);

    /**
     * TRAC #991
     * <p>
     * To get the default synchronized folder use
     * <code>Account.getDefaultSynchronizedFolder()</code>.
     * 
     * @param foInfo
     *            the folder that should be used as default synchronized folder
     *            for the current account.
     */
    void setDefaultSynchronizedFolder(FolderInfo foInfo);

    /**
     * @param foInfos
     *            the list of folders to retrieve the hosted servers for.
     * @return the list of servers the folders are hosted on.
     */
    Collection<MemberInfo> getHostingServers(FolderInfo... foInfos);

    // Server archive calls ***************************************************

    /**
     * Retrieves a List of existing FileInfos for an archived file.
     * 
     * @param fileInfo
     *            fileInfo of the file to get archived versions for.
     * @return list of archived {@link FileInfo}.
     */
    List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo);

    /**
     * Restores/Copies a file version from the archive to a new File within the
     * folder. Does NOT deleted the file in the archive. Does scan the related
     * folder and returns the new FileInfo of the restored file.
     * 
     * @param versionInfo
     *            the FileInfo of the archived file.
     * @param sameLocation
     *            if the file should be restored under the same location and
     *            name. otherwise server stores file under a different name.
     *            Check returned FileInfo at any case.
     * @param target
     * @return the fileInfo of the restored file. Can be used for automatic
     *         downloading this file from the server after restoring.
     * @throws IOException
     *             problem restoring the file.
     */
    @Deprecated
    FileInfo restore(FileInfo versionInfo, boolean sameLocation)
        throws IOException;
    
    /**
     * Restores/Copies a file version from the archive to a new File within the
     * folder. Does NOT deleted the file in the archive. Does scan the related
     * folder and returns the new FileInfo of the restored file.
     * 
     * @param versionInfo
     *            the FileInfo of the archived file.
     * @param newRelativeName
     *            the new relative name. Leave null for same location
     * @param target
     * @return the fileInfo of the restored file. Can be used for automatic
     *         downloading this file from the server after restoring.
     * @throws IOException
     *             problem restoring the file.
     */
    FileInfo restore(FileInfo versionInfo, String newRelativeName)
        throws IOException;

    /**
     * Controls the archive configuration on the server.
     * 
     * @param foInfo
     * @param versionsPerFile
     */
    void setArchiveMode(FolderInfo foInfo, int versionsPerFile);

    /**
     * To empty/purge the online stored archive.
     * 
     * @param foInfo
     * @return if succeeded
     */
    boolean purgeArchive(FolderInfo foInfo);

    int getVersionsPerFile(FolderInfo foInfo);

    // Information ************************************************************

    /**
     * @param foInfo
     * @return true if this folder is joined by the remote side.
     */
    boolean hasJoined(FolderInfo foInfo);

    /**
     * The web DAV URL of a folder.
     *
     * @param foInfo
     * @return
     */
    String getWebDAVURL(FolderInfo foInfo);

    /**
     * Create a file link.
     * 
     * @param fInfo
     * @param folder
     */
    String getFileLink(FileInfo fInfo);

    /**
     * Create a download link.
     * 
     * @param fInfo
     * @param folder
     */
    String getDownloadLink(FileInfo fInfo);

    /**
     * Bulk get of archive and local folders size.
     * 
     * @param foInfos
     * @return [0] = the local size occupied by the given folders.
     *         <p>
     *         [1] = the archive size occupied by the given folders.
     */
    long[] calculateSizes(Collection<FolderInfo> foInfos);

    /**
     * Returns stats for all folders which are available in the cluster.
     * 
     * @param foInfos
     * @return the {@link FolderStatisticInfo} for the given {@link FolderInfo}
     *         s.
     */
    Map<FolderInfo, FolderStatisticInfo> getCloudStatisticInfo(
        Collection<FolderInfo> foInfos);

    /**
     * Returns stats only for the locally synced folders.
     * 
     * @param foInfos
     * @return the {@link FolderStatisticInfo} for the given {@link FolderInfo}
     *         s.
     */
    Map<FolderInfo, FolderStatisticInfo> getLocalStatisticInfo(
        Collection<FolderInfo> foInfos);
    
    

}
