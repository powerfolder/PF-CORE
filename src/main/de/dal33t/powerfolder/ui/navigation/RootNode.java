package de.dal33t.powerfolder.ui.navigation;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.ConfigurationEntry;
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
    public final static String WEBSERVICE_NODE_LABEL = "WEBSERVICE_NODE";
    public final static String DEBUG_NODE_LABEL = "DEBUG_NODE";

    final DefaultMutableTreeNode DOWNLOADS_NODE = new DefaultMutableTreeNode(
        DOWNLOADS_NODE_LABEL);
    final DefaultMutableTreeNode UPLOADS_NODE = new DefaultMutableTreeNode(
        UPLOADS_NODE_LABEL);
    final DefaultMutableTreeNode RECYCLEBIN_NODE = new DefaultMutableTreeNode(
        RECYCLEBIN_NODE_LABEL);
    final DefaultMutableTreeNode WEBSERVICE_NODE = new DefaultMutableTreeNode(
        WEBSERVICE_NODE_LABEL);
    final DefaultMutableTreeNode DEBUG_NODE = new DefaultMutableTreeNode(
        DEBUG_NODE_LABEL);

    private Controller controller;
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
        initalized = false;
    }

    /**
     * @return the controller
     */
    private Controller getController() {
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
        addChild(controller.getUIController().getFolderRepositoryModel()
            .getMyFoldersTreeNode());

        // Only show the WEBSERVICE_NODE if internet access available.
        if (!controller.getNetworkingMode().equals(NetworkingMode.LANONLYMODE)) {
            addChild(WEBSERVICE_NODE);
        }
        addChild(RECYCLEBIN_NODE);
        addChild(DOWNLOADS_NODE);
        addChild(UPLOADS_NODE);
        addChild(controller.getUIController().getNodeManagerModel()
            .getFriendsTreeNode());
        addChild(controller.getUIController().getNodeManagerModel()
            .getNotInFriendsTreeNodes());

        // Only add if verbose mode.
        if (controller.isVerbose()) {

            // Only show the connected user node if debug reports is true.
            if (ConfigurationEntry.DEBUG_REPORTS.getValueBoolean(controller)) {
                addChild(controller.getUIController().getNodeManagerModel()
                 .getConnectedTreeNode());
            }

            // Add debug node
            addChild(DEBUG_NODE);
        }
    }
}