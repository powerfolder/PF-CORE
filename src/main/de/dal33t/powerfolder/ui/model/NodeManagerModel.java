package de.dal33t.powerfolder.ui.model;

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
                    update();
                }
            });

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
        update();
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
        if (notInFriendsTreeNodes != null
            && !notInFriendsTreeNodes.contains(node) && !node.isMySelf())
        {
            notInFriendsTreeNodes.addChild(node);
        }
        fireTreeNodeStructureChangeEvent();
    }

    // Logic ******************************************************************

    private boolean isHideOfflineFriends() {
        return (Boolean) hideOfflineUsersModel.getValue();
    }

    private void update() {
        friendsTableModel.setHideOffline(isHideOfflineFriends());
        // setting changed
        Member[] friends = getController().getNodeManager().getFriends();
        // remove all:
        friendsTreeNode.removeAllChildren();
        boolean hideOffline = isHideOfflineFriends();
        for (Member friend : friends) {
            // add friends to treenode
            if (hideOffline) {
                if (friend.isCompleteyConnected()) {
                    friendsTreeNode.addChild(friend);
                }
            } else {
                friendsTreeNode.addChild(friend);
            }
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

                    if (notInFriendsTreeNodes.getChildCount() == 1) { // Ticket
                        // #376
                        getController().getUIController().getControlQuarter()
                            .getNavigationTreeModel().expandLANList();
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

        // Expand treenodes
        navTreeModel.expandFriendList();
        navTreeModel.expandFolderRepository();

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
                if (node.isCompleteyConnected()) {
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
            if (connectedTreeNode != null) {
                connectedTreeNode.removeChild(e.getNode());
            }
            fireTreeNodeStructureChangeEvent();
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
        getUIController().getControlQuarter().setSelected(getNotInFriendsTreeNodes());
    }
    
    public FindFriendAction getFindFriendAction(Controller controller) {
        if (findFriendsAction == null) {
            findFriendsAction = new FindFriendAction(controller);
        }
        return findFriendsAction;
    }

}
