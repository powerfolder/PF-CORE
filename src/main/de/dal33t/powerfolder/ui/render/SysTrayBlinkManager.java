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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;
import de.dal33t.powerfolder.ui.chat.ChatAdviceEvent;

/**
 * Manages the blinking icon in the Systray. Flash the sys tray every second if
 * UI iconified and (message, invitation, friendship or warning detected).
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class SysTrayBlinkManager extends PFUIComponent {

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
        uiController.getApplicationModel().getChatModel()
            .addChatModelListener(new MyChatModelListener());
        uiController.getApplicationModel().getNoticesModel()
            .getUnreadNoticesCountVM()
            .addValueChangeListener(new MyNoticesCountListener());
        uiController.getMainFrame().getUIComponent()
            .addWindowListener(new MyWindowListener());
        getController().getFolderRepository().addProblemListenerToAllFolders(
            new MyProblemListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /**
     * Sets the icon blinking.
     *
     * @param blink
     */
    private void blinkTrayIcon(boolean blink) {
        uiController.blinkTrayIcon(blink);
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
            if (event.isStatusFlag()
                || !uiController.getMainFrame().isIconifiedOrHidden())
            {
                return;
            }
            blinkTrayIcon(true);
        }

        public void chatAdvice(ChatAdviceEvent event) {
            // Don't care.
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Listen for deiconification to stop flashing icon.
     */
    private class MyWindowListener extends WindowAdapter {

        public void windowDeiconified(WindowEvent e) {
            blinkTrayIcon(false);
        }

        /**
         * Catch cases where UI gets un-hidden - may not also de-iconify.
         * 
         * @param e
         */
        public void windowActivated(WindowEvent e) {
            if (!uiController.getMainFrame().isIconifiedOrHidden()) {
                blinkTrayIcon(false);
            }
        }
    }

    /**
     * Listen for incoming invitations.
     */
    private class MyNoticesCountListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {

            Integer count = (Integer) evt.getNewValue();

            if (count == null || count == 0
                || !uiController.getMainFrame().isIconifiedOrHidden())
            {
                return;
            }

            blinkTrayIcon(true);
        }
    }

    private class MyProblemListener implements ProblemListener {

        public void problemAdded(Problem problem) {
            if (!uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }
            blinkTrayIcon(true);
        }

        public void problemRemoved(Problem problem) {
            // Do nothing
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {

        public void startStop(NodeManagerEvent e) {
            blinkTrayIcon(!getController().getNodeManager().isStarted());
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }
}
