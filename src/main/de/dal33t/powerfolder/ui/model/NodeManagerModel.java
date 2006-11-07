package de.dal33t.powerfolder.ui.model;

import java.util.List;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelListener;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.MemberComparator;
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

    public NodeManagerModel(Controller controller,
        NavTreeModel theNavTreeModel, ChatModel theChatModel)
    {
        super(controller);
        navTreeModel = theNavTreeModel;
        chatModel = theChatModel;
        friendsTableModel = new FriendsNodeTableModel(getController());
        initalize();
    }

    /**
     * Initalize all nessesary ui models
     */
    private synchronized void initalize() {
        TreeNode rootNode = navTreeModel.getRootNode();

        // Init friends treenodes
        friendsTreeNode = new TreeNodeList(rootNode);
        friendsTreeNode.sortBy(MemberComparator.IN_GUI);

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
            List<Member> nodes = getController().getNodeManager()
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
        update();
    }

    public boolean hideOfflineFriends() {
        return PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS
            .getValueBoolean(getController());
    }

    public void setHideOfflineFriends(boolean hideOfflineFriends) {
        PreferencesEntry hideOffline = PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS;
        boolean old = hideOffline.getValueBoolean(getController());
        if (old != hideOfflineFriends) {
            hideOffline.setValue(getController(), hideOfflineFriends);
            update();
        }
    }

    private void update() {
        friendsTableModel.setHideOffline(hideOfflineFriends());
        // setting changed
        Member[] friends = getController().getNodeManager().getFriends();
        // remove all:
        friendsTreeNode.removeAllChildren();
        boolean hideOffline = hideOfflineFriends();
        for (Member friend : friends) {
            // add friends to treenode
            if (hideOffline) {
                if (friend.isConnected()) {
                    friendsTreeNode.addChild(friend);
                }
            } else {
                friendsTreeNode.addChild(friend);
            }
        }
        fireTreeNodeStructureChangeEvent();
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
        if (notInFriendsTreeNodes != null
            && !notInFriendsTreeNodes.contains(node) && !node.isMySelf())
        {
            notInFriendsTreeNodes.addChild(node);
        }
        fireTreeNodeStructureChangeEvent();
    }

    /** add online nodes on LAN to the "not on friends list" */
    private void updateNotOnFriendList(Member member) {
        boolean inFriendsTreeNode = friendsTreeNode.indexOf(member) >= 0;

        if (notInFriendsTreeNodes != null && member.isOnLAN()
            && !inFriendsTreeNode)
        {
            boolean inNotInFriendNodesList = notInFriendsTreeNodes
                .indexOf(member) >= 0;
            if (member.isCompleteyConnected()) {
                if (!inNotInFriendNodesList) {
                    // Add if not already in list
                    notInFriendsTreeNodes.addChild(member);

                    if (notInFriendsTreeNodes.getChildCount() == 1) { // Ticket #376
                    	getController().getUIController().getControlQuarter()
                    		.getUITree().expandPath(notInFriendsTreeNodes.getPathTo());
                    }
                }
            } else {
                if (inNotInFriendNodesList) {
                    // Remove from list
                    notInFriendsTreeNodes.removeChild(member);
                }
            }
        }
    }

    /**
     * Fires tree structure change events on the navigation tree
     */
    private void fireTreeNodeStructureChangeEvent() {
        if (!getController().isUIOpen()) {
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

        if (connectedTreeNode != null) {
            // Resort
            connectedTreeNode.sort();
            TreeModelEvent conTreeNodeEvent = new TreeModelEvent(this,
                connectedTreeNode.getPathTo());
            navTreeModel.fireTreeStructureChanged(conTreeNodeEvent);
        }

        // Update friend node
        friendsTreeNode.sort();
        TreeModelEvent friendTreeNodeEvent = new TreeModelEvent(this,
            friendsTreeNode.getPathTo());
        // Fire event
        navTreeModel.fireTreeStructureChanged(friendTreeNodeEvent);

        // Update friend node
        notInFriendsTreeNodes.sort();
        TreeModelEvent notInFriendsTreeNodeEvent = new TreeModelEvent(this,
            notInFriendsTreeNodes.getPathTo());
        // Fire event
        navTreeModel.fireTreeStructureChanged(notInFriendsTreeNodeEvent);

        // Expand friendlist
        navTreeModel.expandFriendList();

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

    /**
     * Listens for changes in the nodemanager
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        // Nodemanager events
        public void friendAdded(NodeManagerEvent e) {
            Member node = e.getNode();
            PreferencesEntry hideOffline = PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS;
            if (hideOffline.getValueBoolean(getController())) {
                if (node.isConnected()) {
                    if (!friendsTreeNode.contains(node)) {
                        friendsTreeNode.addChild(node);
                    }
                }
            } else {
                if (!friendsTreeNode.contains(node)) {
                    friendsTreeNode.addChild(node);
                }
            }

            notInFriendsTreeNodes.removeChild(node);
            fireTreeNodeStructureChangeEvent();
        }

        public void friendRemoved(NodeManagerEvent e) {
            Member node = e.getNode();

            // Treenode
            friendsTreeNode.removeChild(node);
            notInFriendsTreeNodes.addChild(node);
            fireTreeNodeStructureChangeEvent();
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            Member node = e.getNode();
            if (connectedTreeNode != null) {
                connectedTreeNode.addChild(e.getNode());
            }
            if (node.isFriend()) {
                if (!friendsTreeNode.contains(node)) {
                    friendsTreeNode.addChild(node);
                }
            }
            fireTreeNodeStructureChangeEvent();
            updateNotOnFriendList(node);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            Member node = e.getNode();
            if (connectedTreeNode != null) {
                connectedTreeNode.removeChild(e.getNode());
            }
            PreferencesEntry hideOffline = PreferencesEntry.NODEMANAGERMODEL_HIDEOFFLINEFRIENDS;
            if (hideOffline.getValueBoolean(getController()) && node.isFriend())
            {
                // friendsTableModel.remove(node);
                friendsTreeNode.removeChild(node);
            }
            fireTreeNodeStructureChangeEvent();
            updateNotOnFriendList(node);
        }

        public void nodeRemoved(NodeManagerEvent e) {
            friendsTreeNode.removeChild(e.getNode());
            notInFriendsTreeNodes.removeChild(e.getNode());
            connectedTreeNode.removeChild(e.getNode());
            fireTreeNodeStructureChangeEvent();
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
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
    }
}
