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
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.LockingEvent;
import de.dal33t.powerfolder.event.LockingListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.OverallFolderStatListener;
import de.dal33t.powerfolder.event.PausedModeEvent;
import de.dal33t.powerfolder.event.PausedModeListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileListRequest;
import de.dal33t.powerfolder.security.AdminPermission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.ActionModel;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.dialog.SyncFolderDialog;
import de.dal33t.powerfolder.ui.event.SyncStatusEvent;
import de.dal33t.powerfolder.ui.event.SyncStatusListener;
import de.dal33t.powerfolder.ui.notices.NoticeSeverity;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.notification.NotificationHandlerBase;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.wizard.DesktopSyncSetupPanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;

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
        getController().getFolderRepository().getLocking()
            .addListener(new MyLockingListener());
        getApplicationModel().getFolderRepositoryModel().addOverallFolderStatListener(
                new MyOverallFolderStatListener());
        getNoticesModel().getUnreadNoticesCountVM().addValueChangeListener(new MyNoticesModelPropertyChangeListener());
        getNoticesModel().getAllNoticesCountVM().addValueChangeListener(new MyNoticesModelPropertyChangeListener());
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
        PointerInfo pInfo = MouseInfo.getPointerInfo();
        if (pInfo == null) {
            return true;
        }
        Point nowMouseLocation = pInfo.getLocation();
        if (nowMouseLocation == null) {
            return true;
        }
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
                if ((storageSize + used) > 0 && used >= storageSize * 9 / 10
                    && used < storageSize)
                {
                    // More than 90% used. Notify.
                    WarningNotice notice = new WarningNotice(
                        Translation.get("warning_notice.title"),
                        Translation
                            .get("warning_notice.cloud_full_summary"),
                        Translation
                            .get("warning_notice.cloud_full_message"));
                    noticesModel.handleNotice(notice);
                }
            }
        }
    }

    /**
     * Controls the movement of a folder directory.
     */
    public void moveLocalFolder(Folder folder) {

        // Lock out the 'new folder' scanner.
        // Else it's _just_ possible the scanner might see the renamed folder
        // and autocreate it during the file move.
        getController().getFolderRepository().setSuspendNewFolderSearch(true);

        try {

            Path originalDirectory = folder.getCommitOrLocalDir();

            // Select the new folder.
            List<Path> files = DialogFactory.chooseDirectory(getController()
                    .getUIController(), originalDirectory, false);
            if (!files.isEmpty()) {
                Path newDirectory = files.get(0);
                if (!folder.checkIfDeviceDisconnected() &&
                        PathUtils.isSubdirectory(originalDirectory, newDirectory)) {
                    // Can't move a folder to one of its subdirectories.
                    DialogFactory.genericDialog(getController(),
                            Translation.get("general.directory"),
                            Translation.get("general.subdirectory_error.text"),
                            GenericDialogType.ERROR);
                    return;
                }

                Path foldersBaseDir = getController().getFolderRepository().getFoldersBasedir();
                if (newDirectory.equals(foldersBaseDir)) {
                    // Can't move a folder to the base directory.
                    DialogFactory.genericDialog(getController(),
                            Translation.get("general.directory"),
                            Translation.get("general.basedir_error.text"),
                            GenericDialogType.ERROR);
                    return;
                }

                if (ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY
                    .getValueBoolean(getController()))
                {
                    if (!newDirectory.getParent().equals(
                        getController().getFolderRepository()
                            .getFoldersBasedir()))
                    {
                        // Can't move a folder outside the base directory.
                        DialogFactory.genericDialog(getController(),
                            Translation.get("general.directory"),
                            Translation.get(
                                "general.outside_basedir_error.text",
                                getController().getFolderRepository()
                                    .getFoldersBasedirString()),
                            GenericDialogType.ERROR);
                        return;
                    }
                }

                // Find out if the user wants to move the content of the
                // current folder
                // to the new one.
                int moveContent = shouldMoveContent();

                if (moveContent == 2) {
                    // Cancel
                    return;
                }

                moveDirectory(originalDirectory, newDirectory,
                        moveContent == 0, folder);
            }
        } finally {
            try {
                // Unlock the 'new folder' scanner.
                getController().getFolderRepository()
                        .setSuspendNewFolderSearch(false);
            } catch (Exception e) {
                logSevere(e);
            }
        }
    }

    /**
     * Should the content of the existing folder be moved to the new location?
     *
     * @return true if should move.
     */
    private int shouldMoveContent() {
        return DialogFactory.genericDialog(
            getController(),
            Translation.get("settings_tab.move_content.title"),
            Translation.get("settings_tab.move_content"),
            new String[]{
                Translation.get("settings_tab.move_content.move"),
                Translation.get("settings_tab.move_content.dont"),
                Translation.get("general.cancel"),}, 0,
            GenericDialogType.INFO);
    }

    /**
     * Move the directory.
     */
    private void moveDirectory(Path originalDirectory, Path newDirectory,
        boolean moveContent, Folder folder)
    {
        if (!newDirectory.equals(originalDirectory)) {

            // Check for any problems with the new folder.
            if (checkNewLocalFolder(newDirectory)) {

                // Confirm move.
                if (shouldMoveLocal(newDirectory, folder)) {
                    try {
                        // Move contentes selected
                        ActivityVisualizationWorker worker = new FolderMoveWorker(
                            moveContent, originalDirectory, newDirectory, folder);
                        worker.start();
                    } catch (Exception e) {
                        // Probably failed to create temp directory.
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .get("settings_tab.move_error.title"),
                                Translation
                                    .get("settings_tab.move_error.temp"),
                                getController().isVerbose(), e);
                    }
                }
            }
        }
    }

    /**
     * Confirm that the user really does want to go ahead with the move.
     *
     * @param newDirectory
     * @return true if the user wishes to move.
     */
    private boolean shouldMoveLocal(Path newDirectory, Folder folder) {
        String title = Translation
            .get("settings_tab.confirm_local_folder_move.title");
        String message = Translation.get(
            "settings_tab.confirm_local_folder_move.text", folder
                .getCommitOrLocalDir().toAbsolutePath().toString(), newDirectory
                .toAbsolutePath().toString());

        return DialogFactory.genericDialog(getController(), title, message,
            new String[]{Translation.get("general.continue"),
                Translation.get("general.cancel")}, 0,
            GenericDialogType.INFO) == 0;
    }

    /**
     * Do some basic validation. Warn if moving to a folder that has files /
     * directories in it.
     *
     * @param newDirectory
     * @return
     */
    private boolean checkNewLocalFolder(Path newDirectory) {

        // Warn if target directory is not empty.
        if (newDirectory != null && Files.exists(newDirectory)
            && PathUtils.getNumberOfSiblings(newDirectory) > 0)
        {
            int result = DialogFactory.genericDialog(getController(),
                Translation
                    .get("exp.settings_tab.folder_not_empty.title"),
                Translation.get("exp.settings_tab.folder_not_empty",
                    newDirectory.toAbsolutePath().toString()),
                new String[]{Translation.get("general.continue"),
                    Translation.get("general.cancel")}, 1,
                GenericDialogType.WARN); // Default is cancel.
            if (result != 0) {
                // User does not want to move to new folder.
                return false;
            }
        }

        // All good.
        return true;
    }

    /**
     * Displays an error if the folder move failed.
     *
     * @param e
     *            the error
     */
    private void displayError(Exception e) {
        DialogFactory.genericDialog(
            getController(),
            Translation.get("settings_tab.move_error.title"),
            Translation.get("settings_tab.move_error.other",
                e.getMessage()), GenericDialogType.WARN);
    }

    /**
     * Moves the contents of a folder to another via a temporary directory.
     *
     * @param moveContent
     * @param originalDirectory
     * @param newDirectory
     * @return
     */
    private Object transferFolder(boolean moveContent, Path originalDirectory,
        Path newDirectory, Folder folder)
    {
        try {
            newDirectory = PathUtils.removeInvalidFilenameChars(newDirectory);

            // Copy the files to the new local base
            if (Files.notExists(newDirectory)) {
                try {
                    Files.createDirectories(newDirectory);
                } catch (IOException ioe) {
                    throw new IOException("Failed to create directory: "
                        + newDirectory + ". " + ioe);
                }
            }

            // Remove the old folder from the repository.
            FolderRepository repository = getController().getFolderRepository();
            repository.removeFolder(folder, false);

            // Move it.
            if (moveContent) {
                PathUtils.recursiveMove(originalDirectory, newDirectory);
            }

            Path commitDir = null;
            boolean hasCommitDir = folder.getCommitDir() != null;
            if (hasCommitDir) {
                commitDir = newDirectory;
                newDirectory = newDirectory.resolve(
                    Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR);
                PathUtils.setAttributesOnWindows(newDirectory, true, true);
            }

            // Remember patterns if content not moving.
            List<String> patterns = null;
            if (!moveContent) {
                patterns = folder.getDiskItemFilter().getPatterns();
            }

            // Create the new Folder in the repository.
            FolderInfo fi = new FolderInfo(folder);
            FolderSettings fs = new FolderSettings(newDirectory,
                folder.getSyncProfile(), folder.getDownloadScript(), folder
                    .getFileArchiver().getVersionsPerFile(),
                folder.isSyncPatterns(), commitDir, folder.getSyncWarnSeconds());
            folder = repository.createFolder(fi, fs);

            // Restore patterns if content not moved.
            if (!moveContent && patterns != null) {
                for (String pattern : patterns) {
                    folder.addPattern(pattern);
                }
            }
        } catch (Exception e) {
            return e;
        }
        return null;
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
                    Translation.get("warning_notice.title"),
                    Translation.get("warning_notice.admin_login.summary"),
                    Translation.get("warning_notice.admin_login.message"));
                noticesModel.handleNotice(notice);
            }

            if (DesktopSyncSetupPanel.offerOption(getController())
                && serverClientModel.getClient().isAllowedToCreateFolders())
            {
                PFWizard.openDesktopSyncWizard(getController(),
                    serverClientModel.getClient());
            }
        }

        public void accountUpdated(ServerClientEvent event) {
            handleSyncStatusChange();
            checkCloudSpace();
        }

        public void serverConnected(ServerClientEvent event) {
            handleSyncStatusChange();
            ServerClient client = event.getClient();
            if (client.isPasswordRequired() && !client.isLoggedIn()
                && !PFWizard.isWizardOpen())
            {
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
        boolean loggingIn = client.isLoggingIn();
        boolean loggedIn = client.isLoggedIn();
        boolean noticeAvailable = getNoticesModel().getHighestUnreadSeverity() != null
            || repository.getFolderProblemsCount() > 0;

        SyncStatusEvent status = SyncStatusEvent.SYNC_INCOMPLETE;
        if (getController().isPaused() && !noticeAvailable) {
            status = SyncStatusEvent.PAUSED;
        } else if (!getController().getNodeManager().isStarted()) {
            status = SyncStatusEvent.NOT_STARTED;
        } else if (!connected) {
            status = SyncStatusEvent.NOT_CONNECTED;
        } else if (loggingIn) {
            status = SyncStatusEvent.LOGGING_IN;
        } else if (!loggedIn) {
            status = SyncStatusEvent.NOT_LOGGED_IN;
        } else if (repository.getFoldersCount() == 0 && !noticeAvailable) {
            status = SyncStatusEvent.NO_FOLDERS;
        } else if (folderRepositoryModel.isSyncing()) {
            status = SyncStatusEvent.SYNCING;
        } else if (repository.areAllFoldersInSync() && !noticeAvailable) {
            status = SyncStatusEvent.SYNCHRONIZED;
        } else if (getNoticesModel().getHighestUnreadSeverity() == NoticeSeverity.WARINING
            || repository.getFolderProblemsCount() > 0)
        {
            status = SyncStatusEvent.WARNING;
        } else if (getNoticesModel().getHighestUnreadSeverity() == NoticeSeverity.INFORMATION)
        {
            status = SyncStatusEvent.INFORMATION;
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

    private class MyNoticesModelPropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            handleSyncStatusChange();

        }

    }

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

    private class MyLockingListener implements LockingListener {
        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public void locked(LockingEvent event) {
            // Don't care
        }

        @Override
        public void unlocked(LockingEvent event) {
            // Don't care
        }

        @Override
        public void autoLockForbidden(LockingEvent event) {
            FileInfo fInfo = event.getFileInfo();
            Lock lock = fInfo.getLock(getController());

            final String displayName = lock.getAccountInfo().getDisplayName();
            final String name = fInfo.getFilenameOnly();
            final String date = new SimpleDateFormat("dd MMM yyyy HH:mm")
                .format(lock.getCreated());

            String memberTemp = Translation
                .get("context_menu.unlock.message.web");
            if (lock.getMemberInfo() != null) {
                memberTemp = lock.getMemberInfo().getNick();
            }
            final String memberName = memberTemp;

            new LockOverwriteNoticeHandler(getController(), name, displayName,
                date, memberName).show();
        }
    }

    private class LockOverwriteNoticeHandler extends NotificationHandlerBase {
        LockOverwriteNoticeHandler(final Controller controller,
            final String name, final String displayName, final String date,
            final String memberName)
        {
            super(controller);

            setTitle(Translation.get("context_menu.unlock.title"));
            setMessageText(Translation.get(
                "context_menu.unlock.notice", name, displayName, date,
                memberName));

            setCancelOptionLabel(Translation.get("general.ok"));
            setCancelAction(new AbstractAction() {
                private static final long serialVersionUID = 100L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    sliderClose();
                }
            });
        }
    }

    /**
     * Visualisation worker for folder move.
     */
    private class FolderMoveWorker extends ActivityVisualizationWorker {

        private final boolean moveContent;
        private final Path originalDirectory;
        private final Path newDirectory;
        private final Folder folder;

        FolderMoveWorker(boolean moveContent,
            Path originalDirectory, Path newDirectory, Folder folder)
        {
            super(getController().getUIController());
            this.moveContent = moveContent;
            this.originalDirectory = originalDirectory;
            this.newDirectory = newDirectory;
            this.folder = folder;
        }

        @Override
        public Object construct() {
            return transferFolder(moveContent, originalDirectory, newDirectory, folder);
        }

        @Override
        protected String getTitle() {
            return Translation.get("settings_tab.working.title");
        }

        @Override
        protected String getWorkingText() {
            return Translation
                .get("settings_tab.working.description");
        }

        @Override
        public void finished() {
            if (get() != null) {
                displayError((Exception) get());
            }
        }
    }
}
