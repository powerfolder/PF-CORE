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
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.net.NodeManager;
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
    private TreeNodeList friendsTreeNode;
    private TreeNodeList connectedTreeNode;
    private TreeNodeList notInFriendsTreeNodes;
    private NodeTableModel friendsTableModel;
    private NodeTableModel connectedFriendsTableModel;
    private boolean hideOfflineFriends = false;

    public NodeManagerModel(Controller controller, NavTreeModel theNavTreeModel)
    {
        super(controller);
        navTreeModel = theNavTreeModel;
        friendsTableModel = new NodeTableModel(getController());
        connectedFriendsTableModel = new NodeTableModel(getController());

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
    }

    public void setHideOfflineFriends(boolean hideOfflineFriends) {       
        boolean old = this.hideOfflineFriends;       
        this.hideOfflineFriends = hideOfflineFriends;
        if (old != hideOfflineFriends) {
            
            // setting changed
            Member[] friends = getController().getNodeManager().getFriends();
            // remove all:
            friendsTreeNode.removeAllChildren();
            for (Member friend : friends) {
                // add friends to treenode
                if (hideOfflineFriends) {
                    if (friend.isConnected()) {
                        friendsTreeNode.addChild(friend);
                    }
                } else {
                    friendsTreeNode.addChild(friend);
                }
            }
            fireTreeNodeStructureChangeEvent();
        }
    }

    /**
     * @return the tablemodel containing the friends
     */
    public NodeTableModel getFriendsTableModel() {
        return friendsTableModel;
    }

    /**
     * @return the tablemodel containing the connected friends
     */
    public NodeTableModel getConnectedFriendsTableModel() {
        return connectedFriendsTableModel;
    }

    /**
     * Returns the tree node containing all friends
     * 
     * @return
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
            friendsTableModel.add(node);
            if (hideOfflineFriends) {
                if (node.isConnected()) {
                    friendsTreeNode.addChild(node);
                }
            } else {
                friendsTreeNode.addChild(node);
            }

            if (node.isConnected()) {
                connectedFriendsTableModel.add(node);
            }
            notInFriendsTreeNodes.removeChild(node);
            fireTreeNodeStructureChangeEvent();
        }

        public void friendRemoved(NodeManagerEvent e) {
            Member node = e.getNode();
            friendsTableModel.remove(node);
            if (node.isConnected()) {
                connectedFriendsTableModel.remove(node);
            }

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
                friendsTreeNode.addChild(node);
                connectedFriendsTableModel.add(node);                
            }
            fireTreeNodeStructureChangeEvent();
            updateNotOnFriendList(node);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            Member node = e.getNode();
            if (connectedTreeNode != null) {
                connectedTreeNode.removeChild(e.getNode());
                fireTreeNodeStructureChangeEvent();
            }
            if (node.isFriend()) {
                connectedFriendsTableModel.remove(node);
            }
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
}
