package de.dal33t.powerfolder.ui.render;

import java.util.*;

import javax.swing.Icon;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Manages the Blinking icon in the tree and in the Systray. First add a member
 * to this manager. Then automaticaly the Tree Node will be updated every second
 * with the default or the icon given. use getIconFor(member, defaultIcon) in
 * the renderer.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.7 $
 */
public class BlinkManager extends PFComponent {
    /** one second blink time * */
    private static final long ICON_BLINK_TIME = 1000;
        
    private String trayBlinkIcon;

    /** key = member, value = icon */
    private Map<Member, Icon> blinkingMembers = Collections
        .synchronizedMap(new HashMap());

    private MyTimerTask task;

    /** create a BlinkManager */
    public BlinkManager(Controller controller) {
        super(controller);
        task = new MyTimerTask();
        getController().scheduleAndRepeat(task, ICON_BLINK_TIME);
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            update();
        }
    }

    /**
     * set the member thats needs blinking
     * 
     * @param member
     *            The member that should have a blinking icon
     * @param icon
     *            The icon to use (in combination with the default icon)
     * @see #getIconFor(member, defaultIcon)
     */
    public void addBlinkMember(Member member, Icon icon) {
        if (!member.isMySelf()) {
            blinkingMembers.put(member, icon);
            trayBlinkIcon = Icons.ST_CHAT;
        }
    }

    /**
     * stop blinking a member
     * 
     * @param member
     *            The member that should not blink anymore
     */
    public void removeBlinkMember(Member member) {
        if (blinkingMembers.containsKey(member)) {
            blinkingMembers.remove(member);
        } else {
            throw new IllegalStateException("Not a blinking member: " + member);
        }
        if (blinkingMembers.size() == 0) {
            trayBlinkIcon = null;
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
    public boolean isBlinkingMember(Member member) {
        return blinkingMembers.containsKey(member);
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
        if (blink && blinkingMembers.containsKey(member)) {
            return blinkingMembers.get(member);
        }
        return defaultIcon;
    }

    /**
     * set the icon that should blink in the systray, set to null if blinking
     * should stop.
     */
    public void setBlinkingTrayIcon(String icon) {
        this.trayBlinkIcon = icon;
    }

    /**
     * called every second, checks all members and fires the treeModel event if
     * the correct nodes are found
     */
    private void update() {
        UIController uiController = getController().getUIController();
        ControlQuarter controlQuarter = uiController.getControlQuarter();
        if (controlQuarter != null) {
            NavTreeModel treeModel = controlQuarter.getNavigationTreeModel();
            if (treeModel != null) {
                boolean blink = ((new GregorianCalendar()).get(Calendar.SECOND) % 2) == 1;
                if (blink) {
                    uiController.setTrayIcon(trayBlinkIcon);
                } else {
                    uiController.setTrayIcon(null); // means the default
                }

                Collection<Member> toRemove = null; // add if a member not is
                // connected
                synchronized (blinkingMembers) {
                    Iterator membersIterator = blinkingMembers.keySet()
                        .iterator();
                    while (membersIterator.hasNext()) {
                        Member member = (Member) membersIterator.next();
                        if (member.isConnected()) {
                            fireUpdate(treeModel, member);
                        } else {
                            // Lazy creation to avoid creating a vector every
                            // second
                            if (toRemove == null)
                                toRemove = new Vector<Member>();
                            toRemove.add(member);
                        }
                    }
                }

                if (toRemove != null) {
                    for (Member m : toRemove) {
                        removeBlinkMember(m);
                        fireUpdate(treeModel, m);
                    }
                }
            }
        }
    }

    private void fireUpdate(NavTreeModel treeModel, Member member) {
        TreeNodeList nodeList = getController().getUIController().getNodeManagerModel()
            .getFriendsTreeNode();
        synchronized (nodeList) {
            for (int i = 0; i < nodeList.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeList
                    .getChildAt(i);
                if (node.getUserObject() == member) {
                    Object[] path = new Object[]{treeModel.getRoot(), nodeList,
                        node};
                    TreeModelEvent te = new TreeModelEvent(this, path);
                    treeModel.fireTreeNodesChangedEvent(te);
                }
            }
        }

        nodeList = getController().getUIController().getNodeManagerModel()
            .getNotInFriendsTreeNodes();
        synchronized (nodeList) {
            for (int i = 0; i < nodeList.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeList
                    .getChildAt(i);
                if (node.getUserObject() == member) {
                    Object[] path = new Object[]{treeModel.getRoot(), nodeList,
                        node};
                    TreeModelEvent te = new TreeModelEvent(this, path);
                    treeModel.fireTreeNodesChangedEvent(te);
                }
            }
        }
    }
}
