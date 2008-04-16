package de.dal33t.powerfolder.ui.model;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.ui.transfer.DownloadsTableModel;
import de.dal33t.powerfolder.ui.transfer.UploadsTableModel;
import de.dal33t.powerfolder.util.Reject;

public class TransferManagerModel extends PFUIComponent {
    private TransferManager transferManager;
    private DownloadsTableModel downloadsTableModel;
    private ValueModel downloadsAutoCleanupModel;
    private ValueModel uploadsAutoCleanupModel;
    private UploadsTableModel uploadsTableModel;

    private NavTreeModel navTree;
    private final DefaultMutableTreeNode DOWNLOADS_NODE = new DefaultMutableTreeNode(
        RootNode.DOWNLOADS_NODE_LABEL);
    private final DefaultMutableTreeNode UPLOADS_NODE = new DefaultMutableTreeNode(
        RootNode.UPLOADS_NODE_LABEL);

    public TransferManagerModel(TransferManager transferManager,
        NavTreeModel theNavTreeModel)
    {
        super(transferManager.getController());
        Reject.ifNull(theNavTreeModel, "Nav tree model is null");
        this.navTree = theNavTreeModel;
        this.transferManager = transferManager;
        downloadsAutoCleanupModel = new ValueHolder();
        downloadsAutoCleanupModel
            .setValue(ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
                .getValueBoolean(getController()));
        downloadsTableModel = new DownloadsTableModel(this);
        uploadsAutoCleanupModel = new ValueHolder();
        uploadsAutoCleanupModel
            .setValue(ConfigurationEntry.UPLOADS_AUTO_CLEANUP
                .getValueBoolean(getController()));
        uploadsTableModel = new UploadsTableModel(this, true);
    }

    /**
     * Initializes the listeners into the <code>TransferManager</code>.
     */
    public void initialize() {
        // Listen on transfer manager
        transferManager.addListener(new MyTransferManagerListener());
    }

    // Exposing ***************************************************************

    public TransferManager getTransferManager() {
        return getController().getTransferManager();
    }

    public DownloadsTableModel getDownloadsTableModel() {
        return downloadsTableModel;
    }

    public ValueModel getDownloadsAutoCleanupModel() {
        return downloadsAutoCleanupModel;
    }

    public ValueModel getUploadsAutoCleanupModel() {
        return uploadsAutoCleanupModel;
    }

    public UploadsTableModel getUploadsTableModel() {
        return uploadsTableModel;
    }

    public TreeNode getUploadsTreeNode() {
        return UPLOADS_NODE;
    }

    public TreeNode getDownloadsTreeNode() {
        return DOWNLOADS_NODE;
    }

    // Inner classes **********************************************************

    /**
     * Listens on transfermanager and fires change events on tree
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener implements TransferManagerListener {
        public void downloadRequested(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateDownloadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateUploadsTreeNode();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateUploadsTreeNode();
            updateFolderTreeNode(event);
        }

        private void updateDownloadsTreeNode() {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                navTree.getRoot(), DOWNLOADS_NODE});
            navTree.fireTreeNodesChangedEvent(te);
        }

        private void updateUploadsTreeNode() {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                navTree.getRoot(), UPLOADS_NODE});
            navTree.fireTreeNodesChangedEvent(te);
        }

        private void updateFolderTreeNode(TransferManagerEvent event) {
            Folder folder = event.getFile().getFolder(
                getController().getFolderRepository());
            if (folder == null) {
                return;
            }
            if (folder.isTransferring()) {
                getUIController().getBlinkManager().addBlinking(folder,
                    Icons.FOLDER);
            } else {
                getUIController().getBlinkManager().removeBlinking(folder);
            }
        }
    }
}
