package de.dal33t.powerfolder.ui.model;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelListener;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * UI-Model for the nodemanager. Prepare data from the nodemanager in a
 * "swing-compatible" way. E.g. as <code>TreeNode</code> or
 * <code>TableModel</code>.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @version $Revision: 1.5 $
 */
public class NodeManagerModel extends PFUIComponent {
    private NavTreeModel navTreeModel;
    private ChatModel chatModel;
    private TreeNodeList friendsTreeNode;
    private TreeNodeList connectedTreeNode;
    private TreeNodeList notInFriendsTreeNodes;
    private FriendsNodeTableModel friendsTableModel;
    private ValueModel hideOfflineUsersModel;

    private FindFriendAction findFriendsAction;
    private boolean expandedFriends;

    public NodeManagerModel(Controller controller,
        NavTreeModel theNavTreeModel, ChatModel theChatModel)
    {
        super(controller);
        navTreeModel = theNavTreeModel;
        chatModel = theChatModel;
        initalize();
    }

    /**
     * Initalize all nessesary ui models
     */
    private synchronized void initalize() {
        friendsTableModel = new FriendsNodeTableModel(getController());

        hideOfflineUsersModel = PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS
            .getModel(getController());
        hideOfflineUsersModel
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    rebuildFriendslist();
                }
            });

        TreeNode rootNode = navTreeModel.getRootNode();

        // Init friends treenodes
        friendsTreeNode = new TreeNodeList(rootNode);
        friendsTreeNode.sortBy(MemberComparator.IN_GUI);

        Member[] friends = getController().getNodeManager().getFriends();
        for (Member friend : friends) {
            friendsTreeNode.addChild(friend);
        }

        notInFriendsTreeNodes = new TreeNodeList(rootNode);
        notInFriendsTreeNodes.sortBy(MemberComparator.IN_GUI);

        if (getController().isVerbose()) {
            // Initalize online nodestree
            connectedTreeNode = new TreeNodeList(rootNode);
            connectedTreeNode.sortBy(MemberComparator.IN_GUI);

            // Get all connected nodes
            Collection<Member> nodes = getController().getNodeManager()
                .getConnectedNodes();
            for (Member node : nodes) {
                if (!connectedTreeNode.contains(node)
                    && node.isCompleteyConnected())
                {
                    connectedTreeNode.addChild(node);
                }
            }
        }

        // Register listener on nodemanager
        NodeManager nodeManager = getController().getNodeManager();
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());

        // And on chatmanager
        chatModel.addChatModelListener(new MyChatModelListener());

        // update based on prefs
        rebuildFriendslist();

        // Expand when UI gets opened
        Runnable expander = new Runnable() {
            public void run() {
                expandFriendList();
            }
        };
        getUIController().invokeLater(expander);
    }

    // Exposing ***************************************************************

    /**
     * @return if offline friends should be shown.
     */
    public ValueModel getHideOfflineUsersModel() {
        return hideOfflineUsersModel;
    }

    /**
     * @return the tablemodel containing the friends
     */
    public FriendsNodeTableModel getFriendsTableModel() {
        return friendsTableModel;
    }

    /**
     * @return the tree node containing all friends
     */
    public TreeNodeList getFriendsTreeNode() {
        return friendsTreeNode;
    }

    /**
     * @return the tree node containing all connected nodes
     */
    public TreeNodeList getConnectedTreeNode() {
        if (!getController().isVerbose()) {
            throw new IllegalStateException("only when verbose...");
        }
        return connectedTreeNode;
    }

    /**
     * @return the tree node containing all non-friend members in chat.
     */
    public TreeNodeList getNotInFriendsTreeNodes() {
        return notInFriendsTreeNodes;
    }

    public boolean hasMemberNode(Member node) {
        return friendsTreeNode.indexOf(node) >= 0
            || notInFriendsTreeNodes.indexOf(node) >= 0;
    }

    public void addChatMember(Member node) {
        if (!notInFriendsTreeNodes.contains(node) && !node.isMySelf()) {
            notInFriendsTreeNodes.addChild(node);
            fireTreeNodeStructureChangeEvent(notInFriendsTreeNodes);
        }
    }

    // Logic ******************************************************************

    private boolean isHideOfflineFriends() {
        return (Boolean) hideOfflineUsersModel.getValue();
    }

    private void rebuildFriendslist() {
        friendsTableModel.setHideOffline(isHideOfflineFriends());
        // setting changed
        Member[] friends = getController().getNodeManager().getFriends();
        // remove all:
        friendsTreeNode.removeAllChildren();
        boolean hideOffline = isHideOfflineFriends();
        for (Member friend : friends) {
            // add friends to treenode
            if (hideOffline) {
                if (friend.isCompleteyConnected()
                    || friend.isConnectedToNetwork())
                {
                    friendsTreeNode.addChild(friend);
                }
            } else {
                friendsTreeNode.addChild(friend);
            }
        }
        fireTreeNodeStructureChangeEvent(friendsTreeNode);
    }

    /**
     * Fires tree structure change events on the navigation tree node.
     */
    private void fireTreeNodeStructureChangeEvent(TreeNodeList treeNode) {
        if (!getController().isUIOpen()) {
            return;
        }
        if (treeNode == null) {
            return;
        }
        // Update connected nodes
        ControlQuarter controlQuarter = getController().getUIController()
            .getControlQuarter();
        JTree tree = controlQuarter.getTree();

        TreePath selectionPath = tree.getSelectionPath();
        Object selected = null;
        if (selectionPath != null) {
            selected = selectionPath.getLastPathComponent();
        }

        treeNode.sort();
        TreeModelEvent treeNodeEvent = new TreeModelEvent(this, treeNode
            .getPathTo());
        navTreeModel.fireTreeStructureChanged(treeNodeEvent);

        // Expand treenodes
        expandFriendList();
        expandNotInFriendsList();

        // Restore selection
        if (selected != null && selected instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selected;
            Object userObject = node.getUserObject();
            if (userObject instanceof Member) {
                getController().getUIController().getControlQuarter()
                    .setSelected((Member) userObject);
            }
        }
    }

    // Helper methods *********************************************************

    /**
     * Expands the friends treenode
     */
    private void expandFriendList() {
        if (!getUIController().isStarted()) {
            return;
        }
        if (expandedFriends) {
            return;
        }
        if (getController().getUIController().getNodeManagerModel()
            .getFriendsTreeNode().getChildCount() > 0)
        {
            log().verbose("Expanding friendlist");
            Runnable runner = new Runnable() {
                public void run() {
                    getController().getUIController().getControlQuarter()
                        .getUITree().expandPath(friendsTreeNode.getPathTo());
                    expandedFriends = true;
                }
            };
            if (EventQueue.isDispatchThread()) {
                runner.run();
            } else {
                EventQueue.invokeLater(runner);
            }
        }
    }

    /**
     * Expands the not in friendslist treenode. #376
     */
    private void expandNotInFriendsList() {
        if (!getUIController().isStarted()) {
            return;
        }
        if (notInFriendsTreeNodes.getChildCount() == 1) {
            log().verbose("Expanding not friendlist");
            Runnable runner = new Runnable() {
                public void run() {
                    getController().getUIController().getControlQuarter()
                        .getUITree().expandPath(
                            notInFriendsTreeNodes.getPathTo());
                }
            };
            if (EventQueue.isDispatchThread()) {
                runner.run();
            } else {
                EventQueue.invokeLater(runner);
            }
        }
    }

    /**
     * Listens for changes in the nodemanager
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        // Nodemanager events
        public void friendAdded(NodeManagerEvent e) {
            Member node = e.getNode();
            PreferencesEntry hideOffline = PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS;
            if (hideOffline.getValueBoolean(getController())) {
                if (node.isCompleteyConnected()) {
                    if (!friendsTreeNode.contains(node)) {
                        friendsTreeNode.addChild(node);
                    }
                    fireTreeNodeStructureChangeEvent(friendsTreeNode);
                }
            } else {
                if (!friendsTreeNode.contains(node)) {
                    friendsTreeNode.addChild(node);
                }
                fireTreeNodeStructureChangeEvent(friendsTreeNode);
            }
            if (notInFriendsTreeNodes.removeChild(node)) {
                fireTreeNodeStructureChangeEvent(notInFriendsTreeNodes);
            }
        }

        public void friendRemoved(NodeManagerEvent e) {
            Member node = e.getNode();

            // Treenode
            if (friendsTreeNode.removeChild(node)) {
                fireTreeNodeStructureChangeEvent(friendsTreeNode);
            }

            if (!notInFriendsTreeNodes.contains(node)) {
                notInFriendsTreeNodes.addChild(node);
                fireTreeNodeStructureChangeEvent(notInFriendsTreeNodes);
            }
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            Member node = e.getNode();
            if (connectedTreeNode != null && !connectedTreeNode.contains(node))
            {
                connectedTreeNode.addChild(node);
                fireTreeNodeStructureChangeEvent(connectedTreeNode);
            }
            if (node.isFriend()) {
                if (!friendsTreeNode.contains(node)) {
                    friendsTreeNode.addChild(node);
                }
                fireTreeNodeStructureChangeEvent(friendsTreeNode);
            } else if (node.isOnLAN()) {
                if (!notInFriendsTreeNodes.contains(node)) {
                    notInFriendsTreeNodes.addChild(node);
                }
                fireTreeNodeStructureChangeEvent(notInFriendsTreeNodes);
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            Member node = e.getNode();
            if (connectedTreeNode != null) {
                connectedTreeNode.removeChild(e.getNode());
                fireTreeNodeStructureChangeEvent(connectedTreeNode);
            }
            PreferencesEntry hideOffline = PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS;
            if (hideOffline.getValueBoolean(getController()) && node.isFriend())
            {
                // friendsTableModel.remove(node);
                friendsTreeNode.removeChild(node);
                fireTreeNodeStructureChangeEvent(friendsTreeNode);
            } else if (notInFriendsTreeNodes.removeChild(node)) {
                fireTreeNodeStructureChangeEvent(notInFriendsTreeNodes);
            }
        }

        public void nodeRemoved(NodeManagerEvent e) {
            if (friendsTreeNode.removeChild(e.getNode())) {
                fireTreeNodeStructureChangeEvent(friendsTreeNode);
            }
            if (notInFriendsTreeNodes.removeChild(e.getNode())) {
                fireTreeNodeStructureChangeEvent(notInFriendsTreeNodes);
            }
            if (connectedTreeNode != null) {
                if (connectedTreeNode.removeChild(e.getNode())) {
                    fireTreeNodeStructureChangeEvent(connectedTreeNode);
                }
            }
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyChatModelListener implements ChatModelListener {
        public void chatChanged(ChatModelEvent event) {
            if (!event.isStatus() && (event.getSource() instanceof Member)
                && !hasMemberNode((Member) event.getSource()))
            {
                getUIController().getNodeManagerModel().addChatMember(
                    (Member) event.getSource());
            }
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    // Actions ****************************************************************

    /** Switches to the find friends panel */
    private class FindFriendAction extends BaseAction {
        public FindFriendAction(Controller controller) {
            super("findfriends", controller);
        }

        public void actionPerformed(ActionEvent e) {
            findFriends();
        }
    }

    /** called if button removeFriend clicked or if selected in popupmenu */
    private void findFriends() {
        // TODO Uarg, this is ugly (tm)
        getUIController().getControlQuarter().setSelected(
            getNotInFriendsTreeNodes());
    }

    public FindFriendAction getFindFriendAction() {
        if (findFriendsAction == null) {
            findFriendsAction = new FindFriendAction(getController());
        }
        return findFriendsAction;
    }

}
