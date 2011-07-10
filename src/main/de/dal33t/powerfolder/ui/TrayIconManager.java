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
 * $Id: TrayIconManager.java 15105 2011-05-11 09:26:16Z harry $
 */
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.chat.ChatAdviceEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.notices.SimpleNotificationNotice;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

/**
 * Encapsualtes tray icon functionality. Anything to do with the tray icon
 * should be done *HERE*. This keeps all tray functionality encapsulated.
 * <p/>
 * Blink has the highest priority. If blink is true, the 'P' icon will blink
 * and the blinkText will be the tool tip, explaining why it is blinking.
 * <p/>
 * Sync has second highest priority. If syncing, the icon will rotate
 * and tool tip will say 'syncing'.
 * <p/>
 * Otherwise, normal or warning will display.
 */
public class TrayIconManager extends PFComponent {

    private static final long ROTATION_STEP_DELAY = 200L;

    private enum TrayIconState {
        NORMAL,
        NOT_CONNECTED,
        NOT_LOGGED_IN
    }

    private final UIController uiController;
    private TrayIcon trayIcon;
    private volatile int angle = 0;
    private volatile TrayIconState state;
    private volatile boolean blink;
    private volatile String blinkText;
    private volatile String downText = "";
    private volatile String upText = "";
    private volatile boolean connected;

    public TrayIconManager(UIController uiController) {
        super(uiController.getController());
        this.uiController = uiController;

        Image image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
        if (image == null) {
            logSevere("Unable to retrieve default system tray icon. "
                    + "System tray disabled");
            OSUtil.disableSystray();
            return;
        }
        trayIcon = new TrayIcon(image);
        trayIcon.setImageAutoSize(true);
        updateConnectionStatus();
        updateIcon();
        Controller controller = getController();
        controller.scheduleAndRepeat(new MyUpdateTask(),
                ROTATION_STEP_DELAY);
        controller.getOSClient().addListener(new MyServerClientListener());
        controller.getFolderRepository().addProblemListenerToAllFolders(
                new MyProblemListener());
        uiController.getMainFrame().getUIComponent().addWindowListener(
                new MyWindowListener());
        ApplicationModel applicationModel = uiController.getApplicationModel();
        applicationModel.getNoticesModel().getUnreadNoticesCountVM()
                .addValueChangeListener(new MyNoticesCountListener());
        applicationModel.getChatModel().addChatModelListener(
                new MyChatModelListener());
    }

    /**
     * Only use this to actually display the TrayIcon. Any modifiers should be
     * done through this.
     *
     * @return
     */
    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    private void updateConnectionStatus() {
        state = TrayIconManager.TrayIconState.NORMAL;
        ServerClient client = getController().getOSClient();
        boolean myConnected = client.isConnected();
        if (!myConnected) {
            state = TrayIconManager.TrayIconState.NOT_CONNECTED;
        } else if (!client.isLoggedIn()) {
            state = TrayIconManager.TrayIconState.NOT_LOGGED_IN;
        }

        // Do a notification if moved between connected and not connected.
        if (!PreferencesEntry.SHOW_SYSTEM_NOTIFICATIONS
            .getValueBoolean(getController()))
        {
            return;
        }

        if (myConnected ^ connected) {
            connected = myConnected;
            // State changed, notify ui.
            String notificationText;
            String title = Translation.getTranslation(
                    "tray_icon_manager.status_change.title");
            if (connected) {
                notificationText = Translation.getTranslation(
                        "tray_icon_manager.status_change.connected");
            } else {
                notificationText = Translation.getTranslation(
                        "tray_icon_manager.status_change.disabled");
            }
            uiController.getApplicationModel().getNoticesModel().handleNotice(
                    new SimpleNotificationNotice(title, notificationText));
        }
    }

    private void updateIcon() {

        if (trayIcon == null) {
            // Tray icon not supported?
            return;
        }
        StringBuilder tooltip = new StringBuilder();

        Image image;
        if (blink) {
            tooltip.append(blinkText).append(' ');
            if (blink && System.currentTimeMillis() / 1000 % 2 != 0) {
                // Blank for blink.
                image = Icons.getImageById(Icons.BLANK);
            } else {
                image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
            }
        } else {

            // Always increment the angle.
            // If it is zero, we also update the tooltip up / down rates.
            angle++;
            if (angle >= Icons.SYNC_ANIMATION.length) {
                angle = 0;
            }

            tooltip.append(Translation.getTranslation(
                    "general.application.name"));
            tooltip.append(' ');

            if (getController().getFolderRepository().isSyncing()) {
                image = Icons.getImageById(Icons.SYNC_ANIMATION[angle]);
                if (trayIcon != null) {
                    trayIcon.setImage(image);
                }
                tooltip.append(Translation.getTranslation(
                        "systray.tooltip.syncing"));
            } else if (state == TrayIconState.NORMAL) {
                image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
                tooltip.append(Translation.getTranslation(
                        "systray.tooltip.in_sync"));
            } else if (state == TrayIconState.NOT_CONNECTED) {
                image = Icons.getImageById(Icons.WARNING);
                tooltip.append(Translation.getTranslation(
                        "systray.tooltip.not_connected"));
            } else if (state == TrayIconState.NOT_LOGGED_IN) {
                image = Icons.getImageById(Icons.WARNING);
                tooltip.append(Translation.getTranslation(
                        "systray.tooltip.not_logged_in"));
            } else {
                logSevere("Indeterminate TrayIcon state " + state);
                image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
            }

            // Only update the up/down rates intermittantly.
            if (angle == 0) {
                TransferManager transferManager =
                        getController().getTransferManager();
                double totalCPSdownKB = transferManager
                        .getDownloadCounter().calculateCurrentKBS();
                double totalCPSupKB = transferManager
                        .getUploadCounter().calculateCurrentKBS();

                if (totalCPSdownKB > 1024) {
                    downText = " - " + Translation.getTranslation(
                            "systray.tooltip.down.mb",
                            Format.formatDecimal(totalCPSdownKB / 1024));
                } else {
                    downText = " - " + Translation.getTranslation(
                            "systray.tooltip.down",
                            Format.formatDecimal(totalCPSdownKB));
                }

                if (totalCPSupKB > 1024) {
                    upText = " - " + Translation.getTranslation(
                            "systray.tooltip.up.mb",
                            Format.formatDecimal(totalCPSupKB / 1024));
                } else {
                    upText = " - " + Translation.getTranslation(
                            "systray.tooltip.up",
                            Format.formatDecimal(totalCPSupKB));
                }
            }
            tooltip.append(upText + downText);
        }

        trayIcon.setImage(image);
        trayIcon.setToolTip(tooltip.toString());
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    /**
     * Timer to rotate the icon.
     */
    private class MyUpdateTask extends TimerTask {
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    updateIcon();
                }
            });
        }
    }

    private class MyServerClientListener implements ServerClientListener {
        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            updateConnectionStatus();
        }

        public void login(ServerClientEvent event) {
            updateConnectionStatus();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            updateConnectionStatus();
        }

        public void serverConnected(ServerClientEvent event) {
            updateConnectionStatus();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateConnectionStatus();
        }
    }

    private class MyProblemListener implements ProblemListener {

        public void problemAdded(Problem problem) {
            if (!uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }
            blink = true;
            blinkText = Translation.getTranslation("systray.tooltip.new_problem");
        }

        public void problemRemoved(Problem problem) {
            // Do nothing
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
            blink = false;
        }

        /**
         * Catch cases where UI gets un-hidden - may not also de-iconify.
         *
         * @param e
         */
        public void windowActivated(WindowEvent e) {
            if (!uiController.getMainFrame().isIconifiedOrHidden()) {
                blink = false;
            }
        }
    }

    /**
     * Listen for incoming invitations, etc.
     */
    private class MyNoticesCountListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {

            Integer count = (Integer) evt.getNewValue();

            if (count == null || count == 0
                    || !uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }

            blink = true;
            blinkText = Translation.getTranslation("systray.tooltip.new_notice");
        }
    }

    /**
     * Listens for chat messages.
     */
    private class MyChatModelListener implements ChatModelListener {

        public void chatChanged(ChatModelEvent event) {

            // Ignore status updates or if ui not iconified
            if (event.isStatusFlag()
                    || !uiController.getMainFrame().isIconifiedOrHidden()) {
                return;
            }
            blink = true;
            blinkText = Translation.getTranslation(
                    "systray.tooltip.new_chat_message");
        }

        public void chatAdvice(ChatAdviceEvent event) {
            // Don't care.
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}
