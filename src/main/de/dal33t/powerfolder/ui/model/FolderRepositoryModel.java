/* $Id$
 */
package de.dal33t.powerfolder.ui.model;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.ScanResult.ResultState;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Prepares core data as (swing) ui models. e.g. <code>TreeModel</code>
 * <p>
 * TODO Move table models for public and my folders in here.
 * 
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderRepositoryModel extends PFUIComponent {
    private NavTreeModel navTreeModel;
    // a list containing all joined folder
    private TreeNodeList myFoldersTreeNode;
    private MyFoldersTableModel myFoldersTableModel;

    private MyFolderListener myFolderListener;
    private boolean expandedMyFolders;

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
        myFoldersTableModel = new MyFoldersTableModel(getController()
            .getFolderRepository());

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
            myFoldersTreeNode.addChild(folder.getTreeNode());
            folder.addFolderListener(myFolderListener);
            folder.addMembershipListener(myFolderListener);
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
    public MyFoldersTableModel getMyFoldersTableModel() {
        return myFoldersTableModel;
    }

    // Helper methods *********************************************************

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

            folder.removeFolderListener(myFolderListener);
            folder.removeMembershipListener(myFolderListener);

            myFoldersTreeNode.remove(folder.getTreeNode());

            // Fire tree model event
            TreeModelEvent te = new TreeModelEvent(e.getSource(),
                myFoldersTreeNode.getPathTo());
            navTreeModel.fireTreeStructureChanged(te);

            // Select my folders
            getUIController().getControlQuarter()
                .setSelected(myFoldersTreeNode);
        }

        public void folderCreated(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            if (myFoldersTreeNode.contains(folder.getTreeNode())) {
                return;
            }

            folder.addFolderListener(myFolderListener);
            folder.addMembershipListener(myFolderListener);

            myFoldersTreeNode.addChild(folder.getTreeNode());
            // Fire tree model event
            TreeModelEvent te = new TreeModelEvent(e.getSource(),
                myFoldersTreeNode.getPathTo());
            navTreeModel.fireTreeStructureChanged(te);

            expandFolderRepository();

            // Select folder
            getUIController().getControlQuarter().setSelected(folder);
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
            if (folder.isTransferring() || folder.isScanning()) {
                getUIController().getBlinkManager().addBlinking(folder,
                    Icons.FOLDER);
            } else {
                getUIController().getBlinkManager().removeBlinking(folder);
            }
        }
    }

    private class MyFolderListener implements FolderListener,
        FolderMembershipListener
    {
        // FolderListener
        public void remoteContentsChanged(FolderEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void folderChanged(FolderEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
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
                // Refresh root directory
                folderEvent.getFolder().refreshRootDirectory();
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
            Object[] path = new Object[]{navTreeModel.getRoot(), list,
                folder.getTreeNode()};
            TreeModelEvent te = new TreeModelEvent(this, path);
            navTreeModel.fireTreeStructureChanged(te);
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
