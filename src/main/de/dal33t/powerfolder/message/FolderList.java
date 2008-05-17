/* $Id: FolderList.java,v 1.9 2005/11/04 14:00:35 schaatser Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * List of available folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderList extends Message {
    private static final long serialVersionUID = 101L;

    /** List of public folders. LEFT for backward compatibility */
    public FolderInfo[] folders = new FolderInfo[0];

    /** Secret folders, Folder IDs are encrypted with magic Id */
    public FolderInfo[] secretFolders;

    public FolderList() {
        // Serialisation constructor
    }

    /**
     * Constructor which splits up public and secret folder into own array.
     * Folder Ids of secret folders will be encrypted with magic Id sent by
     * remote node
     * 
     * @param allFolders
     * @param remoteMagicId
     *            the magic id which was sent by the remote side
     */
    public FolderList(FolderInfo[] allFolders, String remoteMagicId) {
        // Split folderlist into secret and public list
        // Encrypt secret folder ids with magic id
        List<FolderInfo> publicFos = new ArrayList<FolderInfo>(
            allFolders.length);
        List<FolderInfo> secretFos = new ArrayList<FolderInfo>(
            allFolders.length);

        for (int i = 0; i < allFolders.length; i++) {
            if (!StringUtils.isBlank(remoteMagicId)) {
                // Send secret folder infos if magic id is not empty

                // Clone folderinfo
                FolderInfo secretFolder = (FolderInfo) allFolders[i].clone();

                // Set Id to secure Id
                secretFolder.id = secretFolder.calculateSecureId(remoteMagicId);

                // Secret folder, encrypt folder id with magic id
                secretFos.add(secretFolder);
            }
        }

        this.secretFolders = new FolderInfo[secretFos.size()];
        secretFos.toArray(secretFolders);
    }

    public String toString() {
        return "FolderList: " + secretFolders.length + " folders";
    }
}