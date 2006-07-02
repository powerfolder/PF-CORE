/* $Id: NetworkFolderList.java,v 1.6 2005/11/20 03:14:14 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Message used to broadcast/answer the folders known on the network.
 * <p>
 * This message has nothing todo with the list of joined folders from the remote
 * user. <code>FolderList</code>
 * <p>
 * This list may also be only a subset of all those folders available on the
 * network
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class NetworkFolderList extends Message {
    private static final long serialVersionUID = 100L;

    public FolderDetails[] folderDetails;

    /**
     * Initializes NetworkFolderList with the given FolderDetails
     */
    private NetworkFolderList(List<FolderDetails> folderDetails) {
        super();
        this.folderDetails = folderDetails.toArray(new FolderDetails[0]);
    }

    /**
     * Creates NetworkFolderlists containing all known network folders as
     * FolderDetails.
     * 
     * @param repo
     * @param folders
     * @return
     */
    public static Message[] createNetworkFolderLists(
        FolderRepository repo)
    {
        List<FolderDetails> foDetails = repo.getNetworkFoldersAsList();
        return createNetworkFolderLists(foDetails);
    }

    /**
     * Creates NetworkFolderlists as answer to a FolderList. Contains
     * FolderDetails of Folders contained in the folderlist
     * 
     * @param repo
     * @param folders
     * @return
     */
    public static Message[] createNetworkFolderLists(
        FolderRepository repo, FolderInfo[] folders)
    {
        List<FolderDetails> foDetails = getFolderDetails(repo, folders);
        return createNetworkFolderLists(foDetails);
    }

    /**
     * Creats the list of network folder messages.
     * 
     * @param nFoldersPerList
     * @return
     */
    private static NetworkFolderList[] createNetworkFolderLists(
        List<FolderDetails> folderDetails)
    {
        Reject.ifNull(folderDetails, "FolderDetails is null");

        if (Constants.NETWORK_FOLDER_LIST_MAX_FOLDERS >= folderDetails.size()) {
            return new NetworkFolderList[]{new NetworkFolderList(folderDetails)};
        }
        // Split list
        int nFoldersPerList = Constants.NETWORK_FOLDER_LIST_MAX_FOLDERS;
        int nLists = folderDetails.size() / nFoldersPerList;
        int lastListSize = folderDetails.size() - nFoldersPerList * nLists;
        int arrSize = nLists;
        if (lastListSize > 0) {
            arrSize++;
        }
        NetworkFolderList[] netLists = new NetworkFolderList[arrSize];
        for (int i = 0; i < nLists; i++) {
            List<FolderDetails> subList = folderDetails.subList(i
                * nFoldersPerList, i * nFoldersPerList + nFoldersPerList);
            netLists[i] = new NetworkFolderList(subList);
        }

        // Add last list
        if (lastListSize > 0) {
            List<FolderDetails> subList = folderDetails.subList(nLists
                * nFoldersPerList, nLists * nFoldersPerList + lastListSize);
            netLists[nLists] = new NetworkFolderList(subList);
        }

        return netLists;
    }

    /**
     * Returns the list FolderDetails for the given folders only.
     * 
     * @param repo
     *            FolderRepository
     * @param folders
     *            the folders to return as Detail
     * @return the list FolderDetails for the given folders only.
     */
    private static List<FolderDetails> getFolderDetails(FolderRepository repo,
        FolderInfo[] folders)
    {
        Reject.ifNull(folders, "Folder list is null");
        List<FolderDetails> folderDetails = new ArrayList<FolderDetails>(
            folders.length);
        if (folders.length == 0) {
            return folderDetails;
        }
        for (int i = 0; i < folders.length; i++) {
            if (repo.hasFolderDetails(folders[i])) {
                folderDetails.add(repo.getFolderDetails(folders[i]));
            }
        }
        return folderDetails;
    }

    /**
     * Answers if this network folder list is empty
     * 
     * @return
     */
    public boolean isEmpty() {
        return folderDetails == null || folderDetails.length == 0;
    }

    public String toString() {
        return "NetworkFolderList with " + folderDetails.length + " folders";
    }
}