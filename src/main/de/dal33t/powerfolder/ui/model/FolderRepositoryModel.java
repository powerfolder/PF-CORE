/* $Id$
 */
package de.dal33t.powerfolder.ui.model;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.ScanResult.ResultState;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 * Prepares core data as (swing) ui models. e.g. <code>TreeModel</code>
 * <p>
 *
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderRepositoryModel extends PFUIComponent {
    private NavTreeModel navTreeModel;
    // a list containing all joined folder
    private TreeNodeList myFoldersTreeNode;
    private FoldersTableModel myFoldersTableModel;

    private MyFolderListener myFolderListener;
    private boolean expandedMyFolders;

    private Map<Folder, FolderModel> folderModelMap =
            Collections.synchronizedMap(new HashMap<Folder, FolderModel>());

    public FolderRepositoryModel(Controller controller,
        NavTreeModel aNavTreeModel)
    {
        super(controller);
        Reject.ifNull(aNavTreeModel, "Nav tree model is null");
        navTreeModel = aNavTreeModel;

        TreeNode rootNode = navTreeModel.getRootNode();
        myFoldersTreeNode = new TreeNodeList(rootNode);
        myFoldersTreeNode.sortBy(new FolderComparator());

        // Table model initalization
        myFoldersTableModel = new FoldersTableModel(getController()
            .getFolderRepository(), getController());

        // UI Updating code for single folders
        myFolderListener = new MyFolderListener();
    }

    // Inizalization **********************************************************

    /**
     * Initalizes the listener on the core components and sets an inital state
     * of the repo model. Adds all folder models as childs
     */
    public void initalize() {
        // Register listener
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        // Add inital model state
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            if (!hideFolder(folder)) {
                FolderModel folderModel = new FolderModel(getController(), folder);
                folderModelMap.put(folder, folderModel);
                myFoldersTreeNode.addChild(folderModel.getTreeNode());
                folder.addFolderListener(myFolderListener);
                folder.addMembershipListener(myFolderListener);
            }
        }

        Runnable runner = new Runnable() {
            public void run() {
                expandFolderRepository();
            }
        };
        getUIController().invokeLater(runner);
    }

    // Exposing ***************************************************************

    public TreeNodeList getMyFoldersTreeNode() {
        return myFoldersTreeNode;
    }

    /**
     * @return a tablemodel containing my folders
     */
    public FoldersTableModel getMyFoldersTableModel() {
        return myFoldersTableModel;
    }

    // Helper methods *********************************************************

    /**
     * Gets a folder model fro a folder from the map.
     *
     * @param folder
     * @return
     */
    public FolderModel locateFolderModel(Folder folder) {
        Reject.ifNull(folder, "Folder is required");
        return folderModelMap.get(folder);
    }

    /**
     * Expands the folder repository, only done once
     */
    private void expandFolderRepository() {
        if (myFoldersTreeNode.getChildCount() > 0 && !expandedMyFolders) {
            log().verbose("Expanding foined folders on navtree");
            // Expand joined folders
            getController().getUIController().getControlQuarter().getUITree()
                .expandPath(myFoldersTreeNode.getPathTo());
            expandedMyFolders = true;
        }
    }

    /**
     * Synchronizes the currently selected folder in the nav tree.
     */
    public void scanSelectedFolder() {
        Object selectedItem = getController().getUIController()
            .getControlQuarter().getSelectionModel().getSelection();
        if (!(selectedItem instanceof Folder)) {
            return;
        }
        Folder folder = (Folder) selectedItem;

        // Let other nodes scan now!
        folder.broadcastScanCommand();

        // Ask for more sync options on that folder if on project sync
        if (SyncProfile.PROJECT_WORK.equals(folder.getSyncProfile())) {
            new SyncFolderPanel(getController(), folder).open();
        } else {
            // Recommend scan on this
            folder.recommendScanOnNextMaintenance();
        }

        log().debug("Disable silent mode");
        getController().setSilentMode(false);

        // Now trigger the scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requesting (trigger all folders, doesn't matter)
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting(folder.getInfo());
    }

    /**
     * Add or remove folder models from the tree depending on the folder
     * preview state.
     */
    public void folderStructureChanged() {
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            FolderModel folderModel = locateFolderModel(folder);
            if (hideFolder(folder) && myFoldersTreeNode
                    .contains(folderModel.getTreeNode())) {
                removeFolder(folder, this);
            } else if (!hideFolder(folder) && !myFoldersTreeNode
                    .contains(folderModel.getTreeNode())) {
                addFolder(folder, this, false);
            }
        }
    }

    // Internal code **********************************************************

    /**
     * Listens for changes in the folder repository. Changes the prepared model
     * and fires the appropiate events on the swing models.
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderRemoved(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            FolderModel folderModel = locateFolderModel(folder);
            if (!myFoldersTreeNode.contains(folderModel.getTreeNode())) {
                return;
            }

            removeFolder(folder, e.getSource());
        }

        public void folderCreated(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            FolderModel folderModel = locateFolderModel(folder);
            if (folderModel != null && 
                    myFoldersTreeNode.contains(folderModel.getTreeNode())) {
                return;
            }

            if (!hideFolder(folder)) {
                addFolder(folder, e.getSource(), true);
            }
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateFolderTreeNode(e);
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateFolderTreeNode(e);
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

        private void updateFolderTreeNode(FolderRepositoryEvent event) {
            Folder folder = event.getFolder();
            if (folder == null) {
                return;
            }
            FolderModel folderModel = locateFolderModel(folder);
            if (!myFoldersTreeNode.contains(folderModel.getTreeNode())) {
                return;
            }
            if (folder.isTransferring() || folder.isScanning()) {
                getUIController().getBlinkManager().addBlinking(folder,
                    Icons.FOLDER);
            } else {
                getUIController().getBlinkManager().removeBlinking(folder);
            }
        }
    }

    private class MyFolderListener extends FolderAdapter implements
        FolderMembershipListener
    {
        // FolderListener
        public void remoteContentsChanged(FolderEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void fileChanged(FolderEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void filesDeleted(FolderEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            ScanResult sr = folderEvent.getScanResult();
            if (!sr.getResultState().equals(ResultState.SCANNED)) {
                return;
            }
            boolean changed = !sr.getNewFiles().isEmpty()
                || !sr.getChangedFiles().isEmpty()
                || !sr.getDeletedFiles().isEmpty()
                || !sr.getMovedFiles().isEmpty()
                || !sr.getRestoredFiles().isEmpty();
            if (changed) {
                updateFolderTreeNode((Folder) folderEvent.getSource());
            }
        }

        // FolderMembershipListener
        public void memberJoined(FolderMembershipEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        /** update Folder treenode for a folder */
        private void updateFolderTreeNode(Folder folder) {
            // Update tree on that folder
            if (logVerbose) {
                log().verbose("Updating files of folder " + folder);
            }
            FolderRepositoryModel folderRepositoryModel = getUIController()
                .getFolderRepositoryModel();
            TreeNodeList list = folderRepositoryModel.getMyFoldersTreeNode();
            FolderModel folderModel = locateFolderModel(folder);
            Object[] path = new Object[]{navTreeModel.getRoot(), list,
                folderModel.getTreeNode()};
            TreeModelEvent te = new TreeModelEvent(this, path);
            navTreeModel.fireTreeStructureChanged(te);
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    /**
     * Only show folders if not preview or show preview config is true.
     *
     * @param folder
     * @return
     */
    private boolean hideFolder(Folder folder) {
        return folder.isPreviewOnly() &&
                ConfigurationEntry.HIDE_PREVIEW_FOLDERS
                        .getValueBoolean(getController());
    }

    private void removeFolder(Folder folder, Object eventSource) {
        folder.removeFolderListener(myFolderListener);
        folder.removeMembershipListener(myFolderListener);
        TreeNodeList tnl = myFoldersTreeNode;
        FolderModel folderModel = locateFolderModel(folder);
        myFoldersTreeNode.remove(folderModel.getTreeNode());

        // Fire tree model event
        TreeModelEvent te = new TreeModelEvent(eventSource, tnl
            .getPathTo());
        navTreeModel.fireTreeStructureChanged(te);

        // Select my folders
        getUIController().getControlQuarter().setSelected(tnl);
    }

    private void addFolder(Folder folder, Object eventSource, boolean select) {
        folder.addFolderListener(myFolderListener);
        folder.addMembershipListener(myFolderListener);

        FolderModel folderModel = new FolderModel(getController(), folder);
        folderModelMap.put(folder, folderModel);
        myFoldersTreeNode.addChild(folderModel.getTreeNode());
        // Fire tree model event
        TreeModelEvent te = new TreeModelEvent(eventSource,
                myFoldersTreeNode.getPathTo());
        navTreeModel.fireTreeStructureChanged(te);

        expandFolderRepository();

        if (select) {

            // Select folder
            getUIController().getControlQuarter().setSelected(folder);
        }
    }


}
