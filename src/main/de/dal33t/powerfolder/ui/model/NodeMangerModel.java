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
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.util.MemberComparator;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * UI-Model for the nodemanager. Prepare data from the nodemanager in a
 * "swing-compatible" way. E.g. as <code>TreeNode</code>.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @version $Revision: 1.5 $
 */
public class NodeMangerModel extends PFUIComponent implements
    NodeManagerListener
{
    private boolean uiModelsInitalized;
    private TreeNodeList friendsTreeNode;
    private TreeNodeList onlineTreeNodes;
    private TreeNodeList notInFriendsTreeNodes;

    public NodeMangerModel(Controller controller) {
        super(controller);
        uiModelsInitalized = false;
        registerListeners();
    }

    private void registerListeners() {
        NodeManager nodeManager = getController().getNodeManager();
        nodeManager.addNodeManagerListener(this);
    }

    /**
     * Initalize all nessesary ui models
     */
    private synchronized void initalizeUIModels() {
        if (uiModelsInitalized) {
            // Already initalized not again
            return;
        }

        ControlQuarter controllQuarter = getController().getUIController()
            .getControlQuarter();
        if (controllQuarter == null) {
            // happends during startup if incomming connection is there before
            // the UI is started
            return;
        }
        NavTreeModel navTreeModel = controllQuarter.getNavigationTreeModel();
        TreeNode rootNode = navTreeModel.getRootNode();
        Member[] friends = getController().getNodeManager().getFriends();
        // Init friends treenodes
        friendsTreeNode = new TreeNodeList(rootNode);
        friendsTreeNode.sortBy(MemberComparator.IN_GUI);

        for (Member friend : friends) {
            friendsTreeNode.addChild(friend);
        }
        friendsTreeNode.sort();

        notInFriendsTreeNodes = new TreeNodeList(rootNode);
        notInFriendsTreeNodes.sortBy(MemberComparator.IN_GUI);

        // Get all connected nodes
        List<Member> nodes = getController().getNodeManager()
            .getConnectedNodes();
        if (getController().isVerbose()) {
            // Initalize online nodestree
            onlineTreeNodes = new TreeNodeList(rootNode);
            for (Member node : nodes) {

                if (!onlineTreeNodes.contains(node)
                    && node.isCompleteyConnected())
                {
                    onlineTreeNodes.addChild(node);
                }
            }
            onlineTreeNodes.sortBy(MemberComparator.IN_GUI);
        }
        uiModelsInitalized = true;

    }

    /**
     * Returns the tree node containing all friends
     * 
     * @return
     */
    public TreeNodeList getFriendsTreeNode() {
        if (!uiModelsInitalized) {
            initalizeUIModels();
        }
        return friendsTreeNode;
    }

    /**
     * Returns the tree node containing all friends
     * 
     * @return
     */
    public TreeNodeList getOnlineTreeNode() {
        if (!getController().isVerbose()) {
            throw new IllegalStateException("only when verbose...");
        }
        if (!uiModelsInitalized) {
            initalizeUIModels();
        }
        return onlineTreeNodes;
    }

    /**
     * Returns the tree node containing all non-friend members in chat.
     * 
     * @return
     */
    public TreeNodeList getNotInFriendsTreeNodes() {
        if (!uiModelsInitalized) {
            initalizeUIModels();
        }
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
        updateTreeNodes();
    }

    public void removeChatMember(Member member) {
        if (notInFriendsTreeNodes != null) {
            notInFriendsTreeNodes.removeChild(member);
        }
        updateTreeNodes();
    }

    private void updateFriendStatus(Member member) {
        if (friendsTreeNode != null) {
            if (member.isFriend()) {
                friendsTreeNode.addChild(member);
                notInFriendsTreeNodes.removeChild(member);
            } else {
                friendsTreeNode.removeChild(member);
                notInFriendsTreeNodes.addChild(member);
            }
        }
        updateTreeNodes();
    }

    private void updateOnlineStatus(Member member) {
        if (onlineTreeNodes != null) {
            boolean inOnlineList = onlineTreeNodes.indexOf(member) >= 0;

            if (member.isCompleteyConnected()) {
                if (!inOnlineList) {
                    // Add if not already in list
                    onlineTreeNodes.addChild(member);
                }
            } else {
                if (inOnlineList) {
                    // Remove from list
                    onlineTreeNodes.removeChild(member);
                }
            }
        }
        updateTreeNodes();
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
        updateTreeNodes();
    }

    // Nodemanager events
    public void friendAdded(NodeManagerEvent e) {
        Member node = e.getNode();
        updateFriendStatus(node);
        updateNotOnFriendList(node);
    }

    public void friendRemoved(NodeManagerEvent e) {
        Member node = e.getNode();
        updateFriendStatus(node);
        updateNotOnFriendList(node);
    }

    public void nodeAdded(NodeManagerEvent e) {
    }

    public void nodeConnected(NodeManagerEvent e) {
        Member node = e.getNode();
        updateOnlineStatus(node);
        updateNotOnFriendList(node);
    }

    public void nodeDisconnected(NodeManagerEvent e) {
        Member node = e.getNode();
        updateOnlineStatus(node);
        updateNotOnFriendList(node);
    }

    public void nodeRemoved(NodeManagerEvent e) {
        if (friendsTreeNode != null) {
            friendsTreeNode.removeChild(e.getNode());
        }
        if (notInFriendsTreeNodes != null) {
            notInFriendsTreeNodes.removeChild(e.getNode());
        }
        updateTreeNodes();
    }

    public void settingsChanged(NodeManagerEvent e) {
    }

    public boolean fireInEventDispathThread() {
        return false;
    }

    /**
     * updates the Friends and not On FriendList and Online tree Nodes. <BR>
     */
    public void updateTreeNodes() {
        // Update connected nodes

        ControlQuarter controlQuarter = getController().getUIController()
            .getControlQuarter();
        NavTreeModel navTreeModel = controlQuarter.getNavigationTreeModel();
        RootNode rootNode = navTreeModel.getRootNode();
        if (controlQuarter != null) {
            JTree tree = controlQuarter.getTree();
            if (tree != null) {
                synchronized (this) {
                    TreePath selectionPath = tree.getSelectionPath();
                    Object selected = null;
                    if (selectionPath != null) {
                        selected = selectionPath.getLastPathComponent();
                    }
                    if (getController().isVerbose()) {
                        getOnlineTreeNode().sort();
                        Object[] path1 = new Object[]{rootNode, getOnlineTreeNode()};

                        TreeModelEvent conTreeNodeEvent = new TreeModelEvent(
                            this, path1);
                        navTreeModel.fireTreeStructureChanged(conTreeNodeEvent);
                    }

                    // Update friend node
                    TreeNodeList friends = getFriendsTreeNode();
                    friends.sort();
                    Object[] path2 = new Object[]{rootNode, friends};
                    TreeModelEvent friendTreeNodeEvent = new TreeModelEvent(
                        this, path2);
                    navTreeModel.fireTreeStructureChanged(friendTreeNodeEvent);

                    // Update Not On Friend list node
                    TreeNodeList notOnFriends = getNotInFriendsTreeNodes();
                    friends.sort();
                    Object[] path3 = new Object[]{rootNode, notOnFriends};
                    TreeModelEvent notOnFriendTreeNodeEvent = new TreeModelEvent(
                        this, path3);
                    navTreeModel
                        .fireTreeStructureChanged(notOnFriendTreeNodeEvent);

                    // Expand friendlist
                    navTreeModel.expandFriendList();

                    if (selected != null
                        && selected instanceof DefaultMutableTreeNode)
                    {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selected;
                        Object userObject = node.getUserObject();
                        if (userObject instanceof Member) {
                            getController().getUIController()
                                .getControlQuarter().setSelected(
                                    (Member) userObject);
                        }
                    }
                }
            }
        }
    }
}
