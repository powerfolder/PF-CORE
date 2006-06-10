package de.dal33t.powerfolder.ui.model;

import javax.swing.tree.TreeNode;

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
 * "swing-compatible" way. E.g. as <code>TreeNode</code>.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@riege.com">Jan van Oosterom</a>
 * @version $Revision: 1.5 $
 */
public class NodeMangerModel extends PFUIComponent implements
    NodeManagerListener
{
    // UI element
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

        // Get all nodes
        Member[] nodes = getController().getNodeManager().getNodes();

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

        // Initalize online nodestree
        onlineTreeNodes = new TreeNodeList(rootNode);
        for (int i = 0; i < nodes.length; i++) {
            if (!onlineTreeNodes.contains(nodes[i])
                && nodes[i].isCompleteyConnected())
            {
                onlineTreeNodes.addChild(nodes[i]);
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
        getUIController().getControlQuarter().getNavigationTreeModel()
            .updateFriendsAndOnlineTreeNodes();
    }

    private void updateOnlineStatus(Member member) {
        // UI Stuff
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

    public void friendAdded(NodeManagerEvent e) {
        updateFriendStatus(e.getNode());
    }

    public void friendRemoved(NodeManagerEvent e) {
        updateFriendStatus(e.getNode());
    }

    public void nodeAdded(NodeManagerEvent e) {
    }

    public void nodeConnected(NodeManagerEvent e) {
        updateOnlineStatus(e.getNode());
    }

    public void nodeDisconnected(NodeManagerEvent e) {
        updateOnlineStatus(e.getNode());
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
}
