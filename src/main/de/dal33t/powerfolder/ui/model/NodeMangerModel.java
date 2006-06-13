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
    private TreeNodeList chatTreeNodes;

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

        chatTreeNodes = new TreeNodeList(rootNode);
        chatTreeNodes.sortBy(MemberComparator.IN_GUI);

        // Get all connected nodes
        List<Member> nodes = getController().getNodeManager()
            .getConnectedNodes();

        // Initalize online nodestree
        onlineTreeNodes = new TreeNodeList(rootNode);
        for (Member node : nodes) {

            if (!onlineTreeNodes.contains(node) && node.isCompleteyConnected())
            {
                onlineTreeNodes.addChild(node);
            }
        }
        onlineTreeNodes.sortBy(MemberComparator.IN_GUI);

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
    public TreeNodeList getChatTreeNodes() {
        if (!uiModelsInitalized) {
            initalizeUIModels();
        }
        return chatTreeNodes;
    }

    public boolean hasMemberNode(Member node) {
        return friendsTreeNode.indexOf(node) >= 0
            || chatTreeNodes.indexOf(node) >= 0;

    }

    public void addChatMember(Member node) {
        if (chatTreeNodes != null && !chatTreeNodes.contains(node)
            && !node.isMySelf())
        {
            chatTreeNodes.addChild(node);
        }
        updateTreeNode();
    }

    public void removeChatMember(Member member) {
        if (chatTreeNodes != null) {
            chatTreeNodes.removeChild(member);
        }
        updateTreeNode();
    }

    private void updateFriendStatus(Member member) {
        if (friendsTreeNode != null) {
            if (member.isFriend()) {
                friendsTreeNode.addChild(member);
                chatTreeNodes.removeChild(member);
            } else {
                friendsTreeNode.removeChild(member);
                chatTreeNodes.addChild(member);
            }
        }
        updateTreeNode();
    }

    private void updateTreeNode() {

        updateFriendsAndOnlineTreeNodes();
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
        updateTreeNode();
    }

    /** add online nodes on LAN to the "not on friends list" */
    private void updateNotOnFriendList(Member member) {
        if (chatTreeNodes != null && member.isOnLAN()) {
            boolean inchatNodesList = chatTreeNodes.indexOf(member) >= 0;
            if (member.isCompleteyConnected()) {
                if (!inchatNodesList) {
                    // Add if not already in list
                    chatTreeNodes.addChild(member);
                }
            } else {
                if (inchatNodesList) {
                    // Remove from list
                    chatTreeNodes.removeChild(member);
                }
            }
        }
    }

    // Nodemanager events
    public void friendAdded(NodeManagerEvent e) {
        updateFriendStatus(e.getNode());
    }

    public void friendRemoved(NodeManagerEvent e) {
        updateFriendStatus(e.getNode());
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
        if (chatTreeNodes != null) {
            chatTreeNodes.removeChild(e.getNode());
        }
        updateTreeNode();
    }

    public void settingsChanged(NodeManagerEvent e) {
    }

    public boolean fireInEventDispathThread() {
        return false;
    }

    /**
     * updatets both the Friends and Online tree Nodes. <BR>
     * TODO Move this code into <code>NodeManagerModel</code>
     */
    public void updateFriendsAndOnlineTreeNodes() {
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
                    // TreeNode nodeInConnectedList = connectedNodes
                    // .getChildTreeNode(node);

                    // Resort
                    onlineTreeNodes.sort();
                    Object[] path1 = new Object[]{rootNode, onlineTreeNodes};

                    TreeModelEvent conTreeNodeEvent = new TreeModelEvent(this,
                        path1);

                    // Update friend node
                    TreeNodeList friends = getController().getUIController()
                        .getNodeManagerModel().getFriendsTreeNode();
                    // TreeNode nodeInFriendList =
                    // friends.getChildTreeNode(node);

                    // Resort
                    friends.sort();
                    Object[] path2 = new Object[]{rootNode, friends};

                    TreeModelEvent friendTreeNodeEvent = new TreeModelEvent(
                        this, path2);

                    // log().warn(
                    // "Updating " + node.getNick() + ", update in fl ? "
                    // + (friendTreeNodeEvent != null) + ", update in
                    // connodes ?
                    // "
                    // + (conTreeNodeEvent != null));

                    // Now fire events
                    navTreeModel.fireTreeStructureChanged(conTreeNodeEvent);
                    navTreeModel.fireTreeStructureChanged(friendTreeNodeEvent);

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
