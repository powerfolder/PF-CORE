/* $Id: NetworkFolderList.java,v 1.6 2005/11/20 03:14:14 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Message used to broadcast/answer the folders known on the network.
 * <p>
 * This message has nothing todo with the list of joined folders from the remote
 * user. <code>FolderList</code>
 * <p>
 * This list may also be only a subset of all those folders available on the
 * network
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class NetworkFolderList extends Message {
    private static final long serialVersionUID = 100L;

    public FolderDetails[] folderDetails;

    /**
     * Initializes NetworkFolderList with the given FolderDetails
     */
    public NetworkFolderList(FolderDetails[] folderDetails) {
        super();
        this.folderDetails = folderDetails;
    }

    /**
     * Initalizes with all folder from folderepository
     * 
     * @param repo
     */
    public NetworkFolderList(FolderRepository repo) {
        this.folderDetails = repo.getNetworkFolders();
    }

    /**
     * Creates an NetworkFolderlist as answer to a FolderFist. Contains
     * FolderDetails of Folders contained in the folderlist
     * 
     * @param repo
     * @param folderList
     */
    public NetworkFolderList(FolderRepository repo, FolderList folderList) {
        // Fill network folder list from repo
        fillFromRepository(repo, folderList.folders);
    }

    /**
     * Creates the correct network folder list for the request
     * 
     * @param repo
     * @param request
     */
    public NetworkFolderList(FolderRepository repo,
        RequestNetworkFolderList request)
    {
        if (request.completeList()) {
            // Complete list
            this.folderDetails = repo.getNetworkFolders();
        } else {
            // Filter list
            fillFromRepository(repo, request.folders);
        }
    }

    /**
     * Splits up the folder list into smaller ones. This makes it possible to
     * parital transfer the list
     * 
     * @param nFoldersPerList
     * @return
     */
    public NetworkFolderList[] split(int nFoldersPerList) {
        if (nFoldersPerList <= 0) {
            throw new IllegalArgumentException(
                "Need at least one folder per list");
        }
        if (isEmpty() || nFoldersPerList >= folderDetails.length) {
            return new NetworkFolderList[]{this};
        }
        // Split list
        int nLists = folderDetails.length / nFoldersPerList;
        int lastListSize = this.folderDetails.length - nFoldersPerList * nLists;
        int arrSize = nLists;
        if (lastListSize > 0) {
            arrSize++;
        }
        NetworkFolderList[] netLists = new NetworkFolderList[arrSize];
        for (int i = 0; i < nLists; i++) {
            FolderDetails[] foDetails = new FolderDetails[nFoldersPerList];
            System.arraycopy(this.folderDetails, i * nFoldersPerList,
                foDetails, 0, foDetails.length);
            netLists[i] = new NetworkFolderList(foDetails);
        }

        // Add last list
        if (lastListSize > 0) {
            FolderDetails[] foDetails = new FolderDetails[lastListSize];
            System.arraycopy(this.folderDetails, nFoldersPerList * nLists,
                foDetails, 0, foDetails.length);
            netLists[nLists] = new NetworkFolderList(foDetails);
        }

        return netLists;
    }

    /**
     * Filss the list of Network Folders that contains FolderDetails for the
     * given folders only. This method should only be used in the constructors
     * 
     * @param repo
     *            FolderRepository
     * @param folders
     *            the folders to add as Detail
     */
    private void fillFromRepository(FolderRepository repo, FolderInfo[] folders)
    {
        if (folders == null || folders.length == 0) {
            return;
        }
        this.folderDetails = new FolderDetails[folders.length];
        for (int i = 0; i < folders.length; i++) {
            if (folders[i] != null) {
                folderDetails[i] = repo.getFolderDetails(folders[i]);
            }
        }

        List<FolderDetails> folderDetailsList = new ArrayList<FolderDetails>(
            folders.length);
        for (int i = 0; i < folders.length; i++) {
            if (repo.hasFolderDetails(folders[i])) {
                folderDetailsList.add(repo.getFolderDetails(folders[i]));
            }
        }
        if (folderDetailsList.isEmpty()) {
            // We do not have infos for him
            return;
        }
        this.folderDetails = new FolderDetails[folderDetailsList.size()];
        folderDetailsList.toArray(this.folderDetails);
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