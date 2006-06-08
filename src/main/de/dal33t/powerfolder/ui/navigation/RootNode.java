package de.dal33t.powerfolder.ui.navigation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * The root node of the navigation tree
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class RootNode extends TreeNodeList {
    // All nodes un
    public final static String DOWNLOADS_NODE_LABEL = "DOWNLOADS_NODE";
    public final static String UPLOADS_NODE_LABEL = "UPLOADS_NODE";
    public final static String RECYCLEBIN_NODE_LABEL = "RECYCLEBIN_NODE";
    public final static String DEBUG_NODE_LABEL = "DEBUG_NODE";

    private final static int PUBLIC_FOLDERS_NODE_INDEX = 1;
    final DefaultMutableTreeNode DOWNLOADS_NODE = new DefaultMutableTreeNode(
        DOWNLOADS_NODE_LABEL);
    final DefaultMutableTreeNode UPLOADS_NODE = new DefaultMutableTreeNode(
        UPLOADS_NODE_LABEL);
    final DefaultMutableTreeNode RECYCLEBIN_NODE = new DefaultMutableTreeNode(
        RECYCLEBIN_NODE_LABEL);
    final DefaultMutableTreeNode DEBUG_NODE = new DefaultMutableTreeNode(
        DEBUG_NODE_LABEL);

    private Controller controller;
    private NavTreeModel navTreeModel;
    private boolean initalized;

    public RootNode(Controller controller, NavTreeModel navTreeModel) {
        super(null, null);
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        if (navTreeModel == null) {
            throw new NullPointerException("Navtreemodel is null");
        }
        this.controller = controller;
        this.navTreeModel = navTreeModel;
        this.initalized = false;
    }

    /**
     * Enables/Disables public folders view based on the networking mode
     */
    private void updatePublicFoldersVisibility() {
        boolean currentlyVisible = indexOf(navTreeModel
            .getPublicFoldersTreeNode()) >= 0;

        if (currentlyVisible && !getController().isPublicNetworking()) {
            removeChild(navTreeModel.getPublicFoldersTreeNode());
            firePublicFolderTreeNodeRemoved();
        }

        if (getController().isPublicNetworking() && !currentlyVisible) {
            addChildAt(navTreeModel.getPublicFoldersTreeNode(),
                PUBLIC_FOLDERS_NODE_INDEX);
            firePublicFolderTreeNodeAdded();
        }
    }

    /**
     * Returns the controller
     * 
     * @return
     */
    public Controller getController() {
        return controller;
    }

    public int getChildCount() {
        initalizeChildren();
        return super.getChildCount();
    }

    public Enumeration children() {
        initalizeChildren();
        return super.children();
    }

    public TreeNode getChildAt(int childIndex) {
        initalizeChildren();
        return super.getChildAt(childIndex);
    }

    public int getIndex(TreeNode node) {
        initalizeChildren();
        return super.getIndex(node);
    }

    /**
     * Initalized the root node. done lazy
     */
    private void initalizeChildren() {
        if (initalized) {
            return;
        }
        log().verbose("Initalizing Children");
        initalized = true;
        addChild(navTreeModel.getJoinedFoldersTreeNode());
        if (getController().isPublicNetworking()) {
            addChild(navTreeModel.getPublicFoldersTreeNode());
        }
        addChild(RECYCLEBIN_NODE);
        addChild(DOWNLOADS_NODE);
        addChild(UPLOADS_NODE);
        addChild(getController().getUIController().getMemberUI()
            .getFriendsTreeNode());
        addChild(getController().getUIController().getMemberUI()
            .getChatTreeNodes());
        if (getController().isVerbose()) {
            addChild(getController().getUIController().getMemberUI()
                .getOnlineTreeNode());
            addChild(DEBUG_NODE);
        }

        // Listen on controller for changes in networking mode
        getController().addPropertyChangeListener(
            Controller.NETWORKING_MODE_PROPERTY, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    log().debug("network prop change");
                    updatePublicFoldersVisibility();
                }
            });
    }

    /**
     * Fires a change of this treenode
     */
    private void firePublicFolderTreeNodeAdded() {
        int[] childIndices = new int[]{PUBLIC_FOLDERS_NODE_INDEX};
        Object[] childs = new Object[]{navTreeModel.getPublicFoldersTreeNode()};
        TreeModelEvent te = new TreeModelEvent(this, getPathTo(), childIndices,
            childs);
        navTreeModel.fireTreeNodesInsertedEvent(te);
    }

    /**
     * Fires a change of this treenode
     */
    private void firePublicFolderTreeNodeRemoved() {
        int[] childIndices = new int[]{PUBLIC_FOLDERS_NODE_INDEX};
        Object[] childs = new Object[]{navTreeModel.getPublicFoldersTreeNode()};
        TreeModelEvent te = new TreeModelEvent(this, getPathTo(), childIndices,
            childs);
        navTreeModel.fireTreeNodesRemovedEvent(te);
    }
}