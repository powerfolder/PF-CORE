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
package de.dal33t.powerfolder.ui.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.security.AdminPermission;
import de.dal33t.powerfolder.ui.action.ActionModel;
import de.dal33t.powerfolder.ui.chat.ChatAdviceEvent;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;

/**
 * Contains all core models for the application.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ApplicationModel extends PFUIComponent {

    private ActionModel actionModel;
    private ChatModel chatModel;
    private FolderRepositoryModel folderRepositoryModel;
    private NodeManagerModel nodeManagerModel;
    private TransferManagerModel transferManagerModel;
    private ServerClientModel serverClientModel;
    private ValueModel chatNotificationsValueModel;
    private ValueModel systemNotificationsValueModel;
    private ValueModel useOSModel;
    private LicenseModel licenseModel;
    private NoticesModel noticesModel;
    private final ValueModel dialogActiveModel; // <Boolean>
    private final DialogMonitorBean dialogMonitorBean;

    /**
     * Constructs a non-initialized application model. Before the model can be
     * used call {@link #initialize()}
     * 
     * @param controller
     * @see #initialize()
     */
    public ApplicationModel(final Controller controller) {
        super(controller);
        actionModel = new ActionModel(getController());
        chatModel = new ChatModel(getController());
        chatModel.addChatModelListener(new ChatNotificationManager());
        folderRepositoryModel = new FolderRepositoryModel(getController());
        nodeManagerModel = new NodeManagerModel(getController());
        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager());
        serverClientModel = new ServerClientModel(getController(),
            getController().getOSClient());

        chatNotificationsValueModel = new ValueHolder(
            PreferencesEntry.SHOW_CHAT_NOTIFICATIONS
                .getValueBoolean(controller));
        chatNotificationsValueModel
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    PreferencesEntry.SHOW_CHAT_NOTIFICATIONS.setValue(
                        controller, (Boolean) evt.getNewValue());
                    controller.saveConfig();
                }
            });
        systemNotificationsValueModel = new ValueHolder(
            PreferencesEntry.SHOW_SYSTEM_NOTIFICATIONS
                .getValueBoolean(controller));
        systemNotificationsValueModel
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    PreferencesEntry.SHOW_SYSTEM_NOTIFICATIONS.setValue(
                        controller, (Boolean) evt.getNewValue());
                    controller.saveConfig();
                }
            });

        useOSModel = PreferencesEntry.USE_ONLINE_STORAGE
            .getModel(getController());
        licenseModel = new LicenseModel();
        noticesModel = new NoticesModel(getController());
        dialogActiveModel = new ValueHolder(Boolean.FALSE);
        dialogMonitorBean = new DialogMonitorBean();
    }

    /**
     * Are there any dialogs (BaseDialog or Wizard) open?
     * @return ValueModel&lt;Boolean&gt;
     */
    public ValueModel getDialogActiveModel() {
        return dialogActiveModel;
    }

    /**
     * Initializes this and all submodels
     */
    public void initialize() {
        transferManagerModel.initialize();
        getController().getOSClient().addListener(new MyServerClientListener());
    }

    // Logic ******************************************************************

    public void syncFolder(Folder folder) {
        // Want to be aware when the scan completes.
        folderRepositoryModel.addInterestedFolderInfo(folder.getInfo());

        if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder.getSyncProfile()))
        {
            // Ask for more sync options on that folder if on project sync
            new SyncFolderPanel(getController(), folder).open();
        } else {

            getController().setSilentMode(false);

            // Let other nodes scan now!
            folder.broadcastScanCommand();

            // Recommend scan on this. User request, so recommend with true.
            folder.recommendScanOnNextMaintenance(true);

            // Now trigger the scan
            getController().getFolderRepository().triggerMaintenance();

            // Trigger file requesting.
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folder.getInfo());
        }

        Folder metaFolder = getController().getFolderRepository()
            .getMetaFolderForParent(folder.getInfo());
        metaFolder.scanLocalFiles(true);
        metaFolder.syncRemoteDeletedFiles(true);
        metaFolder.getStatistic().scheduleCalculate();

        folder.getStatistic().scheduleCalculate();
    }

    // Exposing ***************************************************************

    public ActionModel getActionModel() {
        return actionModel;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public FolderRepositoryModel getFolderRepositoryModel() {
        return folderRepositoryModel;
    }

    public NodeManagerModel getNodeManagerModel() {
        return nodeManagerModel;
    }

    public TransferManagerModel getTransferManagerModel() {
        return transferManagerModel;
    }

    public ServerClientModel getServerClientModel() {
        return serverClientModel;
    }

    public ValueModel getChatNotificationsValueModel() {
        return chatNotificationsValueModel;
    }

    public ValueModel getSystemNotificationsValueModel() {
        return systemNotificationsValueModel;
    }

    public ValueModel getUseOSModel() {
        return useOSModel;
    }

    public LicenseModel getLicenseModel() {
        return licenseModel;
    }

    private class ChatNotificationManager implements ChatModelListener {

        public void chatChanged(ChatModelEvent event) {
            if (event.isStatusFlag() || event.isCreatedLocally()) {
                // Ignore status updates and own messages
                return;
            }
            getController().getUIController().showChatNotification(
                event.getMemberInfo(),
                Translation.getTranslation("chat.notification.title_long",
                    event.getMemberInfo().getNick()), event.getMessage());
        }

        public void chatAdvice(ChatAdviceEvent event) {
            // Don't care
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void login(ServerClientEvent event) {
            if (event.getAccountDetails().getAccount()
                .hasPermission(AdminPermission.INSTANCE))
            {
                WarningNotice notice = new WarningNotice(
                    Translation.getTranslation("warning_notice.title"),
                    Translation
                        .getTranslation("warning_notice.admin_login.summary"),
                    Translation
                        .getTranslation("warning_notice.admin_login.message"));
                noticesModel.handleNotice(notice);
            }
        }

        public void accountUpdated(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
            ServerClient client = event.getClient();
            if (client.isPasswordEmpty() && !client.isLoggedIn()) {
                PFWizard.openLoginWizard(getController(), client);
            }
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
        }

    }

    public NoticesModel getNoticesModel() {
        return noticesModel;
    }

    /**
     * A Wizard has been opened or closed, let the monitor know.
     * @param wizardOpen
     */
    public void setWizardOpen(boolean wizardOpen) {
        dialogMonitorBean.setWizardOpen(wizardOpen);
    }

    /**
     * A BaseDialog has been opened or closed, let the monitor know.
     * @param baseDialogOpen
     */
    public void setBaseDialogOpen(boolean baseDialogOpen) {
        dialogMonitorBean.setBaseDialogOpen(baseDialogOpen);
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    /**
     * This class is used in monitoring the state of active BaseDialogs or
     * Wizards. If one is open, we should not be spamming users with more
     * dialogs (and potentially other events).
     */
    private class DialogMonitorBean {

        /**
         * Prevent concurent events from screwing the pooch.
         */
        private final Object changeLock = ".";

        /**
         * Are any Wizards open?
         */
        private final AtomicBoolean wizardOpen = new AtomicBoolean();

        /**
         * Are any BaseDialogs open?
         */
        private final AtomicBoolean baseDialogOpen = new AtomicBoolean();

        /**
         * PFWizard sets this when a Wizard opens or closes.
         *
         * @param wizardOpen
         */
        public void setWizardOpen(boolean wizardOpen) {
            synchronized (changeLock) {
                if (this.wizardOpen.compareAndSet(!wizardOpen, wizardOpen)) {
                    // Only advise if there has been a state change.
                    advise();
                }
            }
        }

        /**
         * BaseDialog sets this when a BaseDialog opens or closes.
         *
         * @param baseDialogOpen
         */
        public void setBaseDialogOpen(boolean baseDialogOpen) {
            synchronized (changeLock) {
                if (this.baseDialogOpen.compareAndSet(!baseDialogOpen,
                        baseDialogOpen)) {
                    // Only advise if there has been a state change.
                    advise();
                }
            }
        }

        /**
         * Is anything open?
         *
         * @return
         */
        private boolean isDialogOpen() {
            return wizardOpen.get() || baseDialogOpen.get();
        }

        /**
         * Let the value model's listeners know.
         */
        private void advise() {
            System.out.println("hghg wizards " + wizardOpen + ", dialogs " + baseDialogOpen);
            dialogActiveModel.setValue(isDialogOpen());
        }
    }
}
