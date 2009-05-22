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
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Manages the blinking icon in the Systray. Flash the sys tray every second if
 * UI iconified and (message, invitation, friendship or warning detected).
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class SysTrayBlinkManager extends PFUIComponent {

    private final AtomicBoolean flashSysTray = new AtomicBoolean();

    private final UIController uiController;

    /**
     * Create a Blink Manager for the system tray
     * 
     * @param uiController
     *            the UIController
     */
    public SysTrayBlinkManager(UIController uiController) {
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
        uiController.getMainFrame().getUIComponent().addWindowListener(
            new MyWindowListener());
        uiController.getController().getFolderRepository().addProblemListenerToAllFolders(
            new MyProblemListener());
    }

    /**
     * Update the sys tray icon with the image required.
     */
    private void update() {
        boolean blink = System.currentTimeMillis() / 1000 % 2 != 0;
        if (blink && flashSysTray.get()) {
            uiController.setTrayIcon(Icons.getImageById(Icons.BLANK));
        } else {
            uiController.setTrayIcon(null); // the default
        }
    }

    /**
     * Sets the icon flashing.
     *
     * @param flash
     */
    private void flashTrayIcon(boolean flash) {
        flashSysTray.set(flash);
    }

    /* ------------- */
    /* Inner Classes */
    /* ------------- */

    /**
     * Listens for chat messages.
     */
    private class MyChatModelListener implements ChatModelListener {

        public void chatChanged(ChatModelEvent event) {

            // Ignore status updates or if ui not iconified
            if (event.isStatus() ||
                    !uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }
            flashTrayIcon(true);
            update();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Timer task, always running, updates the sys tray icon.
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
     * Listen for deiconification to stop flashing icon.
     */
    private class MyWindowListener extends WindowAdapter {

        public void windowDeiconified(WindowEvent e) {
            flashTrayIcon(false);
            update();
        }

        /**
         * Catch cases where UI gets un-hidden - may not also de-iconify.
         * 
         * @param e
         */
        public void windowActivated(WindowEvent e) {
            if (!uiController.getMainFrame().isIconifiedOrHidden()) {
                flashTrayIcon(false);
                update();
            }
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
                    !uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }

            flashTrayIcon(true);
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
                    !uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }

            flashTrayIcon(true);
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
                    !uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }

            flashTrayIcon(true);
            update();
        }
    }

    private class MyProblemListener implements ProblemListener {

        public void problemAdded(Problem problem) {
            if (!uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }

            flashTrayIcon(true);
            update();
        }

        public void problemRemoved(Problem problem) {
            // Do nothing
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
