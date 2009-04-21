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

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.MainTabbedPane;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Manages the blinking of tab icons in main tabs. Flash every second if
 * tab not showing and (message, invitation, friendship or warning detected).
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class MainFrameBlinkManager extends PFUIComponent {

    private final AtomicBoolean flashHomeTab = new AtomicBoolean();
    private final AtomicBoolean flashMemberTab = new AtomicBoolean();
    private final AtomicInteger selectedMainTab = new AtomicInteger();

    private final UIController uiController;

    /**
     * Create a Blink Manager for the mainframe tabs
     *
     * @param uiController
     *            the UIController
     */
    public MainFrameBlinkManager(UIController uiController) {
        super(uiController.getController());
        this.uiController = uiController;
        MyTimerTask task = new MyTimerTask();
        getController().scheduleAndRepeat(task, 1000);
        uiController.getApplicationModel().getChatModel()
                .addChatModelListener(new MyChatModelListener());
        uiController.getApplicationModel().getWarningsModel()
                .getWarningsCountVM().addValueChangeListener(
                new MyWarningsCountListener());
        uiController.getApplicationModel().getReceivedInvitationsModel()
                .getReceivedInvitationsCountVM().addValueChangeListener(
                new MyInvitationsCountListener());
        uiController.getApplicationModel().getReceivedAskedForFriendshipModel()
                .getReceivedAskForFriendshipCountVM().addValueChangeListener(
                new MyFriendshipCountListener());
        uiController.getMainFrame().addTabbedPaneChangeListener(
                new MyMainTabChangeListener());
    }

    /**
     * Update the tabs with the image required.
     */
    private void update() {

        boolean blink = (System.currentTimeMillis() / 1000) % 2 != 0;

        if (blink && flashHomeTab.get()) {
            uiController.getMainFrame().setHomeTabIcon(Icons.getIconById(Icons.BLANK));
        } else {
            uiController.getMainFrame().setHomeTabIcon(Icons.getIconById(Icons.HOME));
        }

        if (blink && flashMemberTab.get()) {
            uiController.getMainFrame().setComputersTabIcon(Icons.getIconById(Icons.BLANK));
        } else {
            uiController.getMainFrame().setComputersTabIcon(Icons.getIconById(Icons.COMPUTER));
        }

        // Folder tab seems to lose its icon when we do this. So set it anyway.
        uiController.getMainFrame().setFoldersTabIcon(Icons.getIconById(Icons.FOLDER));
    }

    /**
     * Sets the home icon flashing.
     *
     * @param flash
     */
    private void flashHomeTabIcon(boolean flash) {
        flashHomeTab.set(flash);
    }

    /**
     * Sets the home icon flashing.
     *
     * @param flash
     */
    private void flashMemberTabIcon(boolean flash) {
        flashMemberTab.set(flash);
    }

    /* ------------- */
    /* Inner Classes */
    /* ------------- */

    /**
     * Listens for chat messages.
     */
    private class MyChatModelListener implements ChatModelListener {

        public void chatChanged(ChatModelEvent event) {

            // Ignore status updates or if member tab selected
            // or if chat frame visible.
            if (event.isStatus() || uiController.chatFrameVisible() ||
                    selectedMainTab.get() == MainTabbedPane.COMPUTERS_INDEX) {
                return;
            }
            flashMemberTabIcon(true);
            update();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Timer task, always running, updates the tab icons.
     */
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
     * Listen for incoming warnings.
     */
    private class MyWarningsCountListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {

            Integer count = (Integer) uiController.getApplicationModel()
                    .getWarningsModel().getWarningsCountVM().getValue();

            if (count == null || count == 0 ||
                    selectedMainTab.get() == MainTabbedPane.HOME_INDEX) {
                return;
            }

            flashHomeTabIcon(true);
            update();
        }
    }

    /**
     * Listen for incoming invitations.
     */
    private class MyInvitationsCountListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {

            Integer count = (Integer) uiController.getApplicationModel()
                    .getReceivedInvitationsModel()
                    .getReceivedInvitationsCountVM().getValue();

            if (count == null || count == 0 ||
                    selectedMainTab.get() == MainTabbedPane.HOME_INDEX) {
                return;
            }

            flashHomeTabIcon(true);
            update();
        }
    }

    /**
     * Listen for friendship messages.
     */
    private class MyFriendshipCountListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {

            Integer count = (Integer) uiController.getApplicationModel()
                    .getReceivedAskedForFriendshipModel()
                    .getReceivedAskForFriendshipCountVM().getValue();

            if (count == null || count == 0 ||
                    selectedMainTab.get() == MainTabbedPane.HOME_INDEX) {
                return;
            }

            flashHomeTabIcon(true);
            update();
        }
    }

    /**
     * Listen to main tab selection changes.
     */
    private class MyMainTabChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {

            selectedMainTab.set(uiController.getMainFrame()
                    .getSelectedMainTabIndex());

            if (selectedMainTab.get() == MainTabbedPane.HOME_INDEX) {
                flashHomeTabIcon(false);
                update();
            } else if (selectedMainTab.get() == MainTabbedPane.COMPUTERS_INDEX)
            {
                flashMemberTabIcon(false);
                update();
            }
        }
    }
}