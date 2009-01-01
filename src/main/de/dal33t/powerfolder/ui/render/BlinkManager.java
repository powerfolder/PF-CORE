/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui.render;

import java.awt.Image;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;

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

    private Image trayBlinkIcon;

    /** key = member, value = icon */
    private Map<Member, Icon> blinkingMembers = new ConcurrentHashMap<Member, Icon>();

    /** key = folder, value = icon */
    private Map<Folder, Icon> blinkingFolders = new ConcurrentHashMap<Folder, Icon>();

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
        MyTimerTask task = new MyTimerTask();
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
    public void addChatBlinking(Member member, Icon icon) {
        if (!member.isMySelf()) {
            blinkingMembers.put(member, icon);
            trayBlinkIcon = Icons.SYSTRAY_CHAT_ICON;
        }
    }

    /**
     * stop blinking a member
     * 
     * @param member
     *            The member that should not blink anymore
     */
    public void removeBlinking(Member member) {
        update();
        if (blinkingMembers.containsKey(member)) {
            blinkingMembers.remove(member);
            if (blinkingFolders.isEmpty() && blinkingMembers.isEmpty()) {
                trayBlinkIcon = null;
            }
            update();
        }
    }

    public boolean isMemberBlinking() {
        return !blinkingMembers.isEmpty();
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
    public void addChatBlinking(Folder folder, Icon icon) {
        blinkingFolders.put(folder, icon);
        trayBlinkIcon = Icons.SYSTRAY_CHAT_ICON;
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
            if (blinkingFolders.isEmpty() && blinkingMembers.isEmpty()) {
                trayBlinkIcon = null;
            }
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
        boolean blink = new GregorianCalendar().get(Calendar.SECOND) % 2 != 0;
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
    public void setBlinkingTrayIcon(Image icon) {
        trayBlinkIcon = icon;
    }

    /**
     * called every second, checks all members and fires the treeModel event if
     * the correct nodes are found
     */
    private void update() {
        UIController uiController = getController().getUIController();

        boolean blink = (System.currentTimeMillis() / 1000) % 2 != 0;
        if (blink && !blinkingMembers.isEmpty()) {
            uiController.setTrayIcon(trayBlinkIcon);
        } else {
            uiController.setTrayIcon(null); // means the default
        }

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
                if (!chatMessageMember.equals(currentChatPartner)
                    && !chatMessageMember.isMySelf())
                {
                    addChatBlinking(chatMessageMember, Icons.CHAT);
                }
            } else if (event.getSource() instanceof Folder) {
                Folder chatFolder = (Folder) event.getSource();
                Folder currentChatFolder = null;
                if (!chatFolder.equals(currentChatFolder)) {
                    getUIController().getBlinkManager().addChatBlinking(chatFolder,
                        Icons.CHAT);
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
