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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.action.ActionModel;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;

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
    private ReceivedInvitationsModel receivedInvitationsModel;
    private ReceivedAskedForFriendshipModel receivedAskedForFriendshipModel;
    private ReceivedSingleFileOffersModel receivedSingleFileOffersModel;
    private WarningModel warningsModel;
    private ValueModel chatNotificationsValueModel;
    private ValueModel systemNotificationsValueModel;
    private ValueModel useOSModel;
    private LicenseModel licenseModel;

    /**
     * Constructs a non-initialized application model. Before the model can used
     * call {@link #initialize()}
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
        receivedInvitationsModel = new ReceivedInvitationsModel(getController());
        receivedInvitationsModel.getReceivedInvitationsCountVM()
            .addValueChangeListener(new MyReceivedInvitationListener());
        receivedAskedForFriendshipModel = new ReceivedAskedForFriendshipModel(
            getController());
        receivedAskedForFriendshipModel.getReceivedAskForFriendshipCountVM()
            .addValueChangeListener(new MyReceivedFrendshipListener());
        receivedSingleFileOffersModel = new ReceivedSingleFileOffersModel(
            getController());
        warningsModel = new WarningModel(getController());

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
    }

    /**
     * Initializes this and all submodels
     */
    public void initialize() {
        transferManagerModel.initialize();
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

    public ReceivedInvitationsModel getReceivedInvitationsModel() {
        return receivedInvitationsModel;
    }

    public ReceivedAskedForFriendshipModel getReceivedAskedForFriendshipModel()
    {
        return receivedAskedForFriendshipModel;
    }

    public ReceivedSingleFileOffersModel getReceivedSingleFileOffersModel() {
        return receivedSingleFileOffersModel;
    }

    public WarningModel getWarningsModel() {
        return warningsModel;
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
            getController().getUIController().notifyMessage(
                Translation.getTranslation("chat.notification.title"),
                Translation.getTranslation("chat.notification.message"), true);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyReceivedInvitationListener implements
        PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt) {
            Integer newValue = (Integer) evt.getNewValue();
            Integer oldValue = (Integer) evt.getOldValue();
            if (newValue != null && oldValue != null && newValue > oldValue) {
                getController().getUIController()
                    .notifyMessage(
                        Translation
                            .getTranslation("invitation.notification.title"),
                        Translation
                            .getTranslation("invitation.notification.message"),
                        false);
            }
        }
    }

    private class MyReceivedFrendshipListener implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt) {
            Integer newValue = (Integer) evt.getNewValue();
            Integer oldValue = (Integer) evt.getOldValue();
            if (newValue != null && oldValue != null && newValue > oldValue) {
                getController().getUIController()
                    .notifyMessage(
                        Translation
                            .getTranslation("friendship.notification.title"),
                        Translation
                            .getTranslation("friendship.notification.message"),
                        false);
            }
        }
    }

}
