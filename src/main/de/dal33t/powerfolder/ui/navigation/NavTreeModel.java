/* $Id: NavTreeModel.java,v 1.22 2006/03/29 20:53:25 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.navigation;

import java.awt.EventQueue;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * The model for the navigation tree
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class NavTreeModel extends PFComponent implements TreeModel {
    private Set<TreeModelListener> listeners;
    private RootNode rootNode;
    private PublicFoldersTreeNode publicFoldersTreeNode;
    private boolean expandedFriends;
    private boolean expandedJoinedFolders;
    private MyFolderListener myFolderListener;

    public NavTreeModel(Controller controller) {
        super(controller);
        listeners = new HashSet<TreeModelListener>();
        FolderRepository repository = getController().getFolderRepository();
        // UI Updating for the repository
        repository
            .addFolderRepositoryListener(new MyFolderRepositoryListener());

        // UI Updating code for single folders
        myFolderListener = new MyFolderListener();
        addListenerToExsistingFolders();
        // repository.addFolderEventListenerOnAllFolders(myFolderListener);
        // repository
        // .addFolderMembershipEventListenerOnAllFolders(myFolderListener);

        // Listen for folder lists
        controller.getNodeManager().addMessageListenerToAllNodes(
            new NodesMessageListener());

        // Listen on transfer manager
        controller.getTransferManager().addListener(
            new MyTransferManagerListener());

        // Listen on Recycle Bin
        controller.getRecycleBin().addRecycleBinListener(
            new MyRecycleBinListener());
    }

    private void addListenerToExsistingFolders() {
        FolderRepository repo = getController().getFolderRepository();
        Folder[] folders = repo.getFolders();
        for (int i = 0; i < folders.length; i++) {
            folders[i].addFolderListener(myFolderListener);
            folders[i].addMembershipListener(myFolderListener);
        }
    }

    /**
     * Listens to folder
     * <p>
     * TODO: Add correct javadoc!
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    public class MyFolderListener implements FolderListener,
        FolderMembershipListener
    {
        // FolderListener
        public void remoteContentsChanged(FolderEvent folderEvent) {
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void folderChanged(FolderEvent folderEvent) {
            // log().debug(folderEvent);
            updateFolderTreeNode((Folder) folderEvent.getSource());
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
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
            Object[] path = new Object[]{getRoot(), getJoinedFoldersTreeNode(),
                folder.getTreeNode()};
            TreeModelEvent te = new TreeModelEvent(this, path);
            fireTreeStructureChanged(te);
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    // Component listener classes *********************************************

    /**
     * Listens on transfermanager and fires change events on tree
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener implements TransferManagerListener {
        public void downloadRequested(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateDownloadsTreeNode();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        private void updateDownloadsTreeNode() {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                getRoot(), getRootNode().DOWNLOADS_NODE});
            fireTreeNodesChangedEvent(te);
        }

        private void updateUploadsTreeNode() {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                getRoot(), getRootNode().UPLOADS_NODE});
            fireTreeNodesChangedEvent(te);
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * Listener on folder repository
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
            TreeModelEvent te = new TreeModelEvent(this,
                getPublicFoldersTreeNode().getPathTo());
            // log().warn("Updating public folders");
            fireTreeNodesChangedEvent(te);
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            updateJoinedFolders(e.getSource());
            Folder folder = e.getFolder();
            folder.removeFolderListener(myFolderListener);
            folder.removeMembershipListener(myFolderListener);

            // Select "My Folders"
            getController().getUIController().getControlQuarter().setSelected(
                getJoinedFoldersTreeNode());
        }

        public void folderCreated(FolderRepositoryEvent e) {
            updateJoinedFolders(e.getSource());
            Folder folder = e.getFolder();
            folder.addFolderListener(myFolderListener);
            folder.addMembershipListener(myFolderListener);

            // Remove from public folders treenode
            getPublicFoldersTreeNode().removeChild(
                new FolderDetails(e.getFolder().getInfo()));

            // Select folder
            getController().getUIController().getControlQuarter().setSelected(
                folder);
        }

        private void updateJoinedFolders(Object source) {
            // Fire not on unjoined folder adding
            Object[] path = new Object[]{getRoot(), getJoinedFoldersTreeNode()};
            TreeModelEvent te = new TreeModelEvent(this, path);
            fireTreeStructureChanged(te);

            if (!expandedJoinedFolders) {
                expandFolderRepository();
            }
        }

        public void scansStarted(FolderRepositoryEvent e) {
        }

        public void scansFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * Processing a users filelist. Updates directory treenode under folder
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class NodesMessageListener implements MessageListener {
        public void handleMessage(Member source, Message message) {
            Folder folder = null;
            if (message instanceof FileList) {
                FileList list = (FileList) message;
                folder = getController().getFolderRepository().getFolder(
                    list.folder);
            } else if (message instanceof FolderFilesChanged) {
                FolderFilesChanged changes = (FolderFilesChanged) message;
                folder = getController().getFolderRepository().getFolder(
                    changes.folder);
            }
            if (folder != null) {
                // Update tree on that folder
                if (logVerbose) {
                    log().verbose("Updating files of folder " + folder);
                }
                TreeModelEvent te = new TreeModelEvent(this,
                    new Object[]{getRoot(), getJoinedFoldersTreeNode(),
                        folder.getTreeNode()});
                fireTreeStructureChanged(te);
            }
        }
    }

    private class MyRecycleBinListener implements RecycleBinListener {

        public void fileAdded(RecycleBinEvent e) {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                getRoot(), getRootNode().RECYCLEBIN_NODE});
            fireTreeNodesChangedEvent(te);
        }

        public void fileRemoved(RecycleBinEvent e) {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                getRoot(), getRootNode().RECYCLEBIN_NODE});
            fireTreeNodesChangedEvent(te);
        }

    }

    /**
     * @return the tree node of the join folders
     */
    public TreeNode getJoinedFoldersTreeNode() {
        // TODO Move the join folder tree node into here or a
        // <code>UIModel</code>
        return getController().getFolderRepository().getJoinedFoldersTreeNode();
    }

    /**
     * Returns the unjoined folder preview tree node
     * 
     * @return
     */
    public PublicFoldersTreeNode getPublicFoldersTreeNode() {
        if (publicFoldersTreeNode == null) {
            synchronized (this) {
                if (publicFoldersTreeNode == null) {
                    publicFoldersTreeNode = new PublicFoldersTreeNode(this);
                }
            }
        }
        return publicFoldersTreeNode;
    }

    public RootNode getRootNode() {
        if (rootNode == null) {
            // Init lazily
            synchronized (this) {
                if (rootNode == null) {
                    rootNode = new RootNode(getController(), this);
                    // rootNode.initalizeChildren();
                }
            }
        }
        return rootNode;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public Object getChild(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return ((TreeNode) parent).getIndex((TreeNode) child);
    }

    public Object getRoot() {
        return getRootNode();
    }

    public int getChildCount(Object parent) {
        return ((TreeNode) parent).getChildCount();
    }

    public boolean isLeaf(Object node) {
        if (node == this) {
            return true;
        }
        TreeNode treeNode = (TreeNode) node;
        return treeNode.getChildCount() == 0;
    }

    // Listener handling code *************************************************

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    /**
     * Fires a treeStructureChanged on the nav ui tree
     * 
     * @param e
     */
    public void fireTreeStructureChanged(final TreeModelEvent e) {
        // TODO not nice to do it this way
        ControlQuarter controlQuarter = getController().getUIController()
            .getControlQuarter();
        if (controlQuarter != null) {
            final JTree tree = controlQuarter.getTree();
            if (tree != null) {
                Runnable runner = new Runnable() {
                    public void run() {
                        synchronized (this) {
                            final TreePath path = e.getTreePath();
                            boolean expandedTmp = false;
                            boolean selectionExpandedTmp = false;
                            boolean selectionVisibleTmp = false;
                            if (path != null) {
                                expandedTmp = tree.isExpanded(path);
                            }
                            final TreePath selectedPath = tree
                                .getSelectionPath();
                            if (selectedPath != null) {
                                selectionExpandedTmp = tree
                                    .isExpanded(selectedPath);
                                selectionVisibleTmp = tree
                                    .isVisible(selectedPath);
                            }
                            final boolean expandedFinal = expandedTmp;
                            final boolean selectionExpandedFinal = selectionExpandedTmp;
                            final boolean selectionVisibleFinal = selectionVisibleTmp;

                            // Object[] pathtmp = e.getPath();
                            // int count = 0;
                            // for (Object tmp : pathtmp) {
                            // log().debug(count++ + " " + tmp);
                            // }
                            for (TreeModelListener listener : listeners) {
                                listener.treeStructureChanged(e);
                            }

                            if (expandedFinal) {
                                tree.expandPath(path);
                            }
                            if (selectionExpandedFinal) {
                                tree.expandPath(selectedPath);
                            }

                            if (selectionVisibleFinal) {
                                tree.makeVisible(selectedPath);
                            }
                            TreePath lastExpanded = getController()
                                .getUIController().getControlQuarter()
                                .getLastExpandedPath();
                            if (lastExpanded != null) {
                                if (!tree.isExpanded(lastExpanded)) {
                                    tree.expandPath(lastExpanded);
                                }
                            }
                        }
                    }
                };
                if (EventQueue.isDispatchThread()) {
                    runner.run();

                } else {
                    EventQueue.invokeLater(runner);
                }
            }
        }

    }

    /**
     * Fires a changeevent on the nav ui tree
     * 
     * @param e
     */
    public void fireTreeNodesChangedEvent(final TreeModelEvent e) {
        ControlQuarter controlQuarter = getController().getUIController()
            .getControlQuarter();
        if (controlQuarter != null) {
            final JTree tree = controlQuarter.getTree();
            if (tree != null) {
                Runnable runner = new Runnable() {
                    public void run() {
                        synchronized (this) {
                            final TreePath path = e.getTreePath();
                            boolean expandedTmp = false;
                            boolean selectionExpandedTmp = false;
                            boolean selectionVisibleTmp = false;
                            if (path != null) {
                                expandedTmp = tree.isExpanded(path);
                            }
                            final TreePath selectedPath = tree
                                .getSelectionPath();
                            if (selectedPath != null) {
                                selectionExpandedTmp = tree
                                    .isExpanded(selectedPath);
                                selectionVisibleTmp = tree
                                    .isVisible(selectedPath);
                            }
                            final boolean expandedFinal = expandedTmp;
                            final boolean selectionExpandedFinal = selectionExpandedTmp;
                            final boolean selectionVisibleFinal = selectionVisibleTmp;

                            for (TreeModelListener listener : listeners) {
                                listener.treeNodesChanged(e);
                            }

                            if (expandedFinal) {
                                tree.expandPath(path);
                            }
                            if (selectionExpandedFinal) {
                                tree.expandPath(selectedPath);
                            }
                            if (selectionVisibleFinal) {
                                tree.makeVisible(selectedPath);
                            }
                        }
                    }
                };
                if (EventQueue.isDispatchThread()) {
                    runner.run();

                } else {
                    EventQueue.invokeLater(runner);
                }
            }
        }
    }

    /**
     * Fires a changeevent on the nav ui tree
     * 
     * @param e
     */
    public void fireTreeNodesRemovedEvent(final TreeModelEvent e) {
        ControlQuarter controlQuarter = getController().getUIController()
            .getControlQuarter();
        if (controlQuarter != null) {
            final JTree tree = controlQuarter.getTree();
            if (tree != null) {
                Runnable runner = new Runnable() {
                    public void run() {
                        synchronized (this) {
                            final TreePath path = e.getTreePath();
                            boolean expandedTmp = false;
                            boolean selectionExpandedTmp = false;
                            boolean selectionVisibleTmp = false;
                            if (path != null) {
                                expandedTmp = tree.isExpanded(path);
                            }
                            final TreePath selectedPath = tree
                                .getSelectionPath();
                            if (selectedPath != null) {
                                selectionExpandedTmp = tree
                                    .isExpanded(selectedPath);
                                selectionVisibleTmp = tree
                                    .isVisible(selectedPath);
                            }
                            final boolean expandedFinal = expandedTmp;
                            final boolean selectionExpandedFinal = selectionExpandedTmp;
                            final boolean selectionVisibleFinal = selectionVisibleTmp;

                            for (TreeModelListener listener : listeners) {
                                listener.treeNodesRemoved(e);
                            }

                            if (expandedFinal) {
                                tree.expandPath(path);
                            }
                            if (selectionExpandedFinal) {
                                tree.expandPath(selectedPath);
                            }
                            if (selectionVisibleFinal) {
                                tree.makeVisible(selectedPath);
                            }
                        }
                    }
                };
                if (EventQueue.isDispatchThread()) {
                    runner.run();

                } else {
                    EventQueue.invokeLater(runner);
                }
            }
        }
    }

    /**
     * Fires a changeevent on the nav ui tree
     * 
     * @param e
     */
    public void fireTreeNodesInsertedEvent(final TreeModelEvent e) {
        ControlQuarter controlQuarter = getController().getUIController()
            .getControlQuarter();
        if (controlQuarter != null) {
            final JTree tree = controlQuarter.getTree();
            if (tree != null) {
                Runnable runner = new Runnable() {
                    public void run() {
                        synchronized (this) {
                            final TreePath path = e.getTreePath();
                            boolean expandedTmp = false;
                            boolean selectionExpandedTmp = false;
                            boolean selectionVisibleTmp = false;
                            if (path != null) {
                                expandedTmp = tree.isExpanded(path);
                            }
                            final TreePath selectedPath = tree
                                .getSelectionPath();
                            if (selectedPath != null) {
                                selectionExpandedTmp = tree
                                    .isExpanded(selectedPath);
                                selectionVisibleTmp = tree
                                    .isVisible(selectedPath);
                            }
                            final boolean expandedFinal = expandedTmp;
                            final boolean selectionExpandedFinal = selectionExpandedTmp;
                            final boolean selectionVisibleFinal = selectionVisibleTmp;

                            for (TreeModelListener listener : listeners) {
                                listener.treeNodesInserted(e);
                            }

                            if (expandedFinal) {
                                tree.expandPath(path);
                            }
                            if (selectionExpandedFinal) {
                                tree.expandPath(selectedPath);
                            }
                            if (selectionVisibleFinal) {
                                tree.makeVisible(selectedPath);
                            }
                        }
                    }
                };
                if (EventQueue.isDispatchThread()) {
                    runner.run();

                } else {
                    EventQueue.invokeLater(runner);
                }
            }
        }
    }

    /*
     * Helper code ************************************************************
     */

    /**
     * Expands the friends treenode
     */
    public void expandFriendList() {
        if (!expandedFriends) {
            if (getController().getUIController().getNodeManagerModel()
                .getFriendsTreeNode().getChildCount() > 0)
            {
                log().verbose("Expanding friendlist");

                Runnable runner = new Runnable() {
                    public void run() {
                        synchronized (this) {
                            TreePath path = new TreePath(
                                new Object[]{
                                    getRoot(),
                                    getController().getUIController()
                                        .getNodeManagerModel()
                                        .getFriendsTreeNode()});
                            getController().getUIController()
                                .getControlQuarter().getUITree().expandPath(
                                    path);
                            expandedFriends = true;
                        }
                    }
                };
                if (EventQueue.isDispatchThread()) {
                    runner.run();
                } else {
                    EventQueue.invokeLater(runner);
                }
            }
        }
    }

    public void fireChatNodeUpdatedAndExpand() {
        TreeNodeList chatNodes = getController().getUIController()
            .getNodeManagerModel().getNotInFriendsTreeNodes();
        final Object[] path = new Object[2];
        path[0] = getRoot();
        path[1] = chatNodes;
        fireTreeStructureChanged(new TreeModelEvent(this, path));

        Runnable runner = new Runnable() {
            public void run() {
                synchronized (this) {
                    TreePath treePath = new TreePath(path);
                    JTree tree = getController().getUIController()
                        .getControlQuarter().getUITree();
                    tree.expandPath(treePath);
                }
            }
        };
        if (EventQueue.isDispatchThread()) {
            runner.run();
        } else {
            EventQueue.invokeLater(runner);
        }
    }

    /**
     * Expands the folder repository, only done once
     */
    private void expandFolderRepository() {
        if (getJoinedFoldersTreeNode().getChildCount() > 0
            && !expandedJoinedFolders)
        {
            log().verbose("Expanding foined folders on navtree");

            // Expand joined folders
            TreePath joinedFolders = new TreePath(new Object[]{getRoot(),
                getJoinedFoldersTreeNode()});
            log().warn(
                "expandFolderRepository idDispatch?"
                    + EventQueue.isDispatchThread());
            getController().getUIController().getControlQuarter().getUITree()
                .expandPath(joinedFolders);

            expandedJoinedFolders = true;
        }
    }
}