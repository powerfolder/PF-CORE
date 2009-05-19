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

import java.util.Collection;

import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;

/**
 * Access/Control over folders of a server.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface FolderService {

    /**
     * Creates a new folder to be mirrored by the server. Default Sync
     * 
     * @param foInfo
     * @param profile
     * @throws FolderException
     */
    void createFolder(FolderInfo foInfo, SyncProfile profile)
        throws FolderException;

    /**
     * Removes a folder from the server. Required admin permission on the
     * folder. Also removes the permission to this folder afterwards.
     * 
     * @param foInfo
     * @param deleteFiles
     *            true to delete all file contained in the folder.
     */
    void removeFolder(FolderInfo foInfo, boolean deleteFiles);

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

    // /**
    // * Grants the currently logged in user access to folder. the folder is NOT
    // * setup on the remote server.
    // *
    // * @param foInfos
    // * @see #createFolder(FolderInfo, SyncProfile)
    // */
    // void grantAdmin(FolderInfo... foInfos);

    /**
     * Revokes the currently logged in user access to folder. the folder is NOT
     * setup on the remote server.
     * 
     * @param foInfos
     * @see #removeFolder(FolderInfo, boolean)
     */
    void revokeAdmin(FolderInfo... foInfos);

    /**
     * @param foInfos
     *            the list of folders to retrieve the hosted servers for.
     * @return the list of servers the folders are hosted on.
     */
    Collection<MemberInfo> getHostingServers(FolderInfo... foInfos);
}
