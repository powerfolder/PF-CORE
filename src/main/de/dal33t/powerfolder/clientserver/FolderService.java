package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;

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
     * Invites a user to a folder. The invited user gains read/write
     * permissions.
     * 
     * @param user
     *            the name of the user to be invited
     * @param foInfo
     *            the folder to be invited to
     * @throws FolderException 
     */
    void inviteUser(FolderInfo foInfo, String user) throws FolderException;

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
     * Grants the currently logged in user access to folder. the folder is NOT
     * setup on the remote server.
     * 
     * @param foInfos
     * @see #createFolder(FolderInfo, SyncProfile)
     */
    void grantAdmin(FolderInfo... foInfos);

    /**
     * Revokes the currently logged in user access to folder. the folder is NOT
     * setup on the remote server.
     * 
     * @param foInfos
     * @see #removeFolder(FolderInfo, boolean)
     */
    void revokeAdmin(FolderInfo... foInfos);
}
