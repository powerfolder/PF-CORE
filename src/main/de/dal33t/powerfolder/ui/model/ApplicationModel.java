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

import java.awt.MouseInfo;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.message.FileListRequest;
import de.dal33t.powerfolder.security.AdminPermission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.ActionModel;
import de.dal33t.powerfolder.ui.dialog.SyncFolderDialog;
import de.dal33t.powerfolder.ui.event.SyncStatusEvent;
import de.dal33t.powerfolder.ui.event.SyncStatusListener;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;

import static de.dal33t.powerfolder.ui.event.SyncStatusEvent.*;

/**
 * Contains all core models for the application.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ApplicationModel extends PFUIComponent {

    private ActionModel actionModel;
    private FolderRepositoryModel folderRepositoryModel;
    private NodeManagerModel nodeManagerModel;
    private TransferManagerModel transferManagerModel;
    private ServerClientModel serverClientModel;
    private ValueModel systemNotificationsValueModel;
    private LicenseModel licenseModel;
    private NoticesModel noticesModel;
    private Date lastMouseAction;
    private Point lastMouseLocation;
    private final List<SyncStatusListener> syncStatusListeners;
    private volatile SyncStatusEvent syncStatus;

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
        folderRepositoryModel = new FolderRepositoryModel(getController());
        nodeManagerModel = new NodeManagerModel(getController());
        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager());
        serverClientModel = new ServerClientModel(getController(),
            getController().getOSClient());

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

        licenseModel = new LicenseModel();
        noticesModel = new NoticesModel(getController());
        syncStatusListeners = new CopyOnWriteArrayList<SyncStatusListener>();
    }

    /**
     * Initializes this and all submodels
     */
    public void initialize() {
        transferManagerModel.initialize();
        getController().getOSClient().addListener(new MyServerClientListener());
        getController().addPausedModeListener(new MyPausedModeListener());
        getController().getFolderRepository().addFolderRepositoryListener(new MyFolderRepositoryListener());
        getController().getNodeManager().addNodeManagerListener(new MyNodeManagerListener());
        getApplicationModel().getFolderRepositoryModel().addOverallFolderStatListener(
                new MyOverallFolderStatListener());
    }

    // Logic ******************************************************************

    public void syncFolder(Folder folder) {
        // Want to be aware when the scan completes.
        folderRepositoryModel.addInterestedFolderInfo(folder.getInfo());

        if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder.getSyncProfile()))
        {
            // Ask for more sync options on that folder if on project sync
            new SyncFolderDialog(getController(), folder).open();
        } else {

            getController().setPaused(false);

            // Let other nodes scan now!
            folder.broadcastScanCommand();

            folder.broadcastMessages(new FileListRequest(folder.getInfo()));

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

    public boolean isUserActive() {
        Point nowMouseLocation = MouseInfo.getPointerInfo().getLocation();
        if (lastMouseLocation == null) {
            // Init
            lastMouseLocation = nowMouseLocation;
            lastMouseAction = new Date();
            return true;
        }
        // Mouse was moved.
        if (!nowMouseLocation.equals(lastMouseLocation)) {
            lastMouseLocation = nowMouseLocation;
            lastMouseAction = new Date();
            return true;
        }
        // Mouse was not moved.
        long forSeconds = (System.currentTimeMillis() - lastMouseAction
            .getTime()) / 1000;
        return forSeconds <= 10;
    }
    
    private void checkCloudSpace() {
        if (!PreferencesEntry.WARN_FULL_CLOUD.getValueBoolean(getController()))
        {
            return;
        }
        ServerClient client = getServerClientModel().getClient();
        if (client != null && client.isLoggedIn()) {
            if (!client.getAccount().getOSSubscription().isDisabled()) {
                long storageSize = client.getAccount().getOSSubscription()
                    .getStorageSize();
                long used = client.getAccountDetails().getSpaceUsed();
                if (used >= storageSize * 9 / 10) {
                    // More than 90% used. Notify.
                    WarningNotice notice = new WarningNotice(
                        Translation.getTranslation("warning_notice.title"),
                        Translation
                            .getTranslation("warning_notice.cloud_full_summary"),
                        Translation
                            .getTranslation("warning_notice.cloud_full_message"));
                    noticesModel.handleNotice(notice);
                }
            }
        }
    }

    // Exposing ***************************************************************

    public ActionModel getActionModel() {
        return actionModel;
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

    public ValueModel getSystemNotificationsValueModel() {
        return systemNotificationsValueModel;
    }

    public LicenseModel getLicenseModel() {
        return licenseModel;
    }

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void login(ServerClientEvent event) {
            handleSyncStatusChange();
            checkCloudSpace();
            
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
            handleSyncStatusChange();
            checkCloudSpace();
        }

        public void serverConnected(ServerClientEvent event) {
            handleSyncStatusChange();
            ServerClient client = event.getClient();
            if (client.isPasswordEmpty() && !client.isLoggedIn()) {
                PFWizard.openLoginWizard(getController(), client);
            }
        }

        public void serverDisconnected(ServerClientEvent event) {
            handleSyncStatusChange();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            handleSyncStatusChange();
        }

    }

    public NoticesModel getNoticesModel() {
        return noticesModel;
    }

    public void addSyncStatusListener(SyncStatusListener l) {
        syncStatusListeners.add(l);
    }

    public void removeSyncStatusListener(SyncStatusListener l) {
        syncStatusListeners.remove(l);
    }

    private void handleSyncStatusChange() {
        FolderRepository repository = getController().getFolderRepository();
        ServerClient client = getController().getOSClient();
        boolean connected = client.isConnected();
        boolean loggedIn = client.isLoggedIn();

        SyncStatusEvent status = SYNC_INCOMPLETE;
        if (getController().isPaused()) {
            status = PAUSED;
        } else if (!getController().getNodeManager().isStarted()) {
            status = NOT_STARTED;
        } else if (!connected) {
            status = NOT_CONNECTED;
        } else if (!loggedIn) {
            status = NOT_LOGGED_IN;
        } else if (repository.getFoldersCount() == 0) {
            status = NO_FOLDERS;
        } else if (folderRepositoryModel.isSyncing()) {
            status = SYNCING;
        } else if (repository.areAllFoldersInSync()) {
            status = SYNCHRONIZED;
        }
        triggerSyncStatusChange(status);
    }

    private void triggerSyncStatusChange(SyncStatusEvent event) {
        if (isFiner()) {            
            logFiner(event.toString());
        }
        for (SyncStatusListener listener : syncStatusListeners) {
            listener.syncStatusChanged(event);
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyPausedModeListener implements PausedModeListener {

        public void setPausedMode(PausedModeEvent event) {
            handleSyncStatusChange();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyOverallFolderStatListener implements OverallFolderStatListener {
        public void statCalculated() {
            handleSyncStatusChange();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {

        public void startStop(NodeManagerEvent e) {
            handleSyncStatusChange();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyFolderRepositoryListener implements FolderRepositoryListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            handleSyncStatusChange();
        }

        public void folderCreated(FolderRepositoryEvent e) {
            handleSyncStatusChange();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            // Don't care
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            // Don't care
        }
    }
}
