package de.dal33t.powerfolder.ui.render;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.model.FolderModel;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.MemberChatPanel;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelListener;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.ui.TreeNodeList;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Manages the Blinking icon in the tree and in the Systray. First add a member
 * to this manager. Then automaticaly the Tree Node will be updated every second
 * with the default or the icon given. use getIconFor(member, defaultIcon) in
 * the renderer.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.7 $
 */
public class BlinkManager extends PFUIComponent {
    /** one second blink time * */
    private static final long ICON_BLINK_TIME = 300;

    private String trayBlinkIcon;

    /** key = member, value = icon */
    private Map<Member, Icon> blinkingMembers = new ConcurrentHashMap<Member, Icon>();

    /** key = folder, value = icon */
    private Map<Folder, Icon> blinkingFolders = new ConcurrentHashMap<Folder, Icon>();

    private MyTimerTask task;

    /**
     * create a BlinkManager
     * 
     * @param controller
     *            the controller
     * @param chatModel
     *            the chat model to act upon
     */
    public BlinkManager(Controller controller, ChatModel chatModel) {
        super(controller);
        task = new MyTimerTask();
        getController().scheduleAndRepeat(task, ICON_BLINK_TIME);
        chatModel.addChatModelListener(new MyChatModelListener());
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    update();
                }
            });

        }
    }

    /**
     * set the member thats needs blinking
     * 
     * @param member
     *            The member that should have a blinking icon
     * @param icon
     *            The icon to use (in combination with the default icon)
     */
    public void addBlinking(Member member, Icon icon) {
        if (!member.isMySelf()) {
            blinkingMembers.put(member, icon);
            updateTrayBlinkingIcon();
        }
    }

    /**
     * stop blinking a member
     * 
     * @param member
     *            The member that should not blink anymore
     */
    public void removeBlinkMember(Member member) {
        update();
        if (blinkingMembers.containsKey(member)) {
            blinkingMembers.remove(member);
            update();
        }
    }

    public boolean isMemberBlinking() {
        return blinkingMembers.size() > 0;
    }

    public Member getABlinkingMember() {
        return blinkingMembers.keySet().iterator().next();
    }

    /**
     * @param member
     *            the member to check if it is blinking
     * @return true if this member is in blinking state
     */
    public boolean isBlinking(Member member) {
        return blinkingMembers.containsKey(member);
    }

    /**
     * set the folder thats needs blinking
     * 
     * @param folder
     *            The folder that should have a blinking icon
     * @param icon
     *            The icon to use (in combination with the default icon)
     */
    public void addBlinking(Folder folder, Icon icon) {
        blinkingFolders.put(folder, icon);
        updateTrayBlinkingIcon();
    }

    /**
     * stop blinking a folder
     * 
     * @param folder
     *            The folder that should not blink anymore
     */
    public void removeBlinking(Folder folder) {
        update();
        if (blinkingFolders.containsKey(folder)) {
            blinkingFolders.remove(folder);
            update();
        }
    }

    /**
     * @param folder
     *            the folder to check if it is blinking
     * @return true if this folder is in blinking state
     */
    public boolean isBlinking(Folder folder) {
        return blinkingFolders.containsKey(folder);
    }

    /**
     * @param member
     *            get for this member the blinking icon
     * @param defaultIcon
     * @return The Icon, either the defaultIcon or the "blink" icon (Set when
     *         adding a member) switches every second. If member is not in
     *         blinking state the default Icon is returned.
     */
    public Icon getIconFor(Member member, Icon defaultIcon) {
        boolean blink = ((new GregorianCalendar()).get(Calendar.SECOND) % 2) == 1;
        Icon blinkIcon = blinkingMembers.get(member);
        if (blink && blinkIcon != null) {
            return blinkIcon;
        }
        return defaultIcon;
    }

    /**
     * set the icon that should blink in the systray, set to null if blinking
     * should stop.
     * 
     * @param icon
     */
    public void setBlinkingTrayIcon(String icon) {
        this.trayBlinkIcon = icon;
    }

    /**
     * Updates the tray blink icon by checking the number of blinking folder and
     * nodes.
     */
    private void updateTrayBlinkingIcon() {
        if (blinkingMembers.isEmpty() && blinkingFolders.isEmpty()) {
            trayBlinkIcon = null;
        } else {
            trayBlinkIcon = Icons.ST_CHAT;
        }
    }

    /**
     * called every second, checks all members and fires the treeModel event if
     * the correct nodes are found
     */
    private void update() {
        UIController uiController = getController().getUIController();
        ControlQuarter controlQuarter = uiController.getControlQuarter();
        if (controlQuarter == null) {
            return;
        }
        NavTreeModel treeModel = getUIController().getApplicationModel()
            .getNavTreeModel();
        if (treeModel == null) {
            return;
        }

        boolean blink = ((System.currentTimeMillis() / 1000) % 2) == 1;
        if (blink && !blinkingMembers.isEmpty()) {
            uiController.setTrayIcon(trayBlinkIcon);
        } else {
            uiController.setTrayIcon(null); // means the default
        }

        updateNodeBlinking(treeModel);
        updateFolderBlinking(treeModel);
    }

    /**
     * @param treeModel
     *            the model where the blinking occours
     */
    private void updateNodeBlinking(NavTreeModel treeModel) {
        if (blinkingMembers.isEmpty()) {
            return;
        }
        for (Member member : blinkingMembers.keySet()) {
            if (member.isCompleteyConnected()) {
                fireUpdate(treeModel, member);
            } else {
                blinkingMembers.remove(member);
            }
        }
    }

    /**
     * @param treeModel
     *            the model where the blinking occours
     */
    private void updateFolderBlinking(NavTreeModel treeModel) {
        if (blinkingFolders.isEmpty()) {
            return;
        }
        for (Folder folder : blinkingFolders.keySet()) {
            if (getController().getFolderRepository().hasJoinedFolder(
                folder.getInfo()))
            {
                fireUpdate(treeModel, folder);
            } else {
                blinkingFolders.remove(folder);
            }
        }
    }

    private void fireUpdate(NavTreeModel treeModel, Member member) {
        TreeNodeList nodeList = getController().getUIController()
            .getNodeManagerModel().getFriendsTreeNode();
        TreeNode nodeTreeNode = nodeList.getChildTreeNode(member);
        if (nodeTreeNode != null) {
            TreeModelEvent te = new TreeModelEvent(this, UIUtil
                .getPathTo(nodeTreeNode));
            treeModel.fireTreeNodesChangedEvent(te);
        }

        nodeList = getController().getUIController().getNodeManagerModel()
            .getNotInFriendsTreeNodes();
        nodeTreeNode = nodeList.getChildTreeNode(member);
        if (nodeTreeNode != null) {
            TreeModelEvent te = new TreeModelEvent(this, UIUtil
                .getPathTo(nodeTreeNode));
            treeModel.fireTreeNodesChangedEvent(te);
        }
    }

    private void fireUpdate(NavTreeModel treeModel, Folder folder) {
        FolderModel folderModel = getController().getUIController()
                .getFolderRepositoryModel().locateFolderModel(folder);
        TreeModelEvent te = new TreeModelEvent(this, UIUtil.getPathTo(folderModel
            .getTreeNode()));
        treeModel.fireTreeNodesChangedEvent(te);
    }

    // Internal classes ********************************************************

    private class MyChatModelListener implements ChatModelListener {
        public void chatChanged(ChatModelEvent event) {
            if (event.isStatus()) {
                // Ignore status updates
                return;
            }

            if (event.getSource() instanceof Member) {
                Member chatMessageMember = (Member) event.getSource();
                Member currentChatPartner = null;
                if (getUIController().getInformationQuarter()
                    .getDisplayTarget() instanceof MemberChatPanel)
                {
                    currentChatPartner = getUIController()
                        .getInformationQuarter().getMemberChatPanel()
                        .getChatPartner();
                }
                if (!chatMessageMember.equals(currentChatPartner)
                    && !chatMessageMember.isMySelf())
                {
                    addBlinking(chatMessageMember, Icons.CHAT);
                }
            } else if (event.getSource() instanceof Folder) {
                Folder chatFolder = (Folder) event.getSource();
                Folder currentChatFolder = null;
                if (getUIController().getInformationQuarter()
                    .getDisplayTarget() instanceof Folder) {
                    if (chatFolder.isPreviewOnly()) {
                        currentChatFolder = getUIController()
                            .getInformationQuarter().getPreviewFolderPanel()
                            .getChatPanel().getChatFolder();
                    } else {
                        currentChatFolder = getUIController()
                            .getInformationQuarter().getMyFolderPanel()
                            .getChatPanel().getChatFolder();
                    }
                }
                if (!chatFolder.equals(currentChatFolder)) {
                    getUIController().getBlinkManager().addBlinking(chatFolder,
                        Icons.CHAT);
                }
            }
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
