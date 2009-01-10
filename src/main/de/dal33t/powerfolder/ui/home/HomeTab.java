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
 * $Id: HomeTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.home;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.AskForFriendshipReceivedEvent;
import de.dal33t.powerfolder.event.AskForFriendshipReceivedListener;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedListener;
import de.dal33t.powerfolder.event.NodeManagerModelEvent;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.event.SynchronizationStatsEvent;
import de.dal33t.powerfolder.event.SynchronizationStatsListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Class for the Home tab in the main tab area of the UI.
 */
public class HomeTab extends PFUIComponent {

    private JPanel uiComponent;

    private JLabel synchronizationStatusLabel;
    private JLabel synchronizationDateLabel;
    private HomeTabLine numberOfFoldersLine;
    private HomeTabLine sizeOfFoldersLine;
    private HomeTabLine filesAvailableLine;
    private HomeTabLine newInvitationsLine;
    private HomeTabLine newFriendRequestLine;
    private HomeTabLine computersLine;
    private HomeTabLine downloadsLine;
    private HomeTabLine uploadsLine;
    private final ValueModel downloadsCountVM;
    private final ValueModel uploadsCountVM;
    private final MyFolderListener folderListener;
    private ServerClient client;
    private ActionLabel onlineStorageAccountLabel;
    private OnlineStorageSection onlineStorageSection;
    private final ValueModel newFriendRequestCountVM;
    private final ValueModel newInvitationsCountVM;

    /**
     * Constructor
     *
     * @param controller
     */
    public HomeTab(Controller controller) {
        super(controller);
        downloadsCountVM = getApplicationModel()
                .getTransferManagerModel().getCompletedDownloadsCountVM();
        uploadsCountVM = getApplicationModel()
                .getTransferManagerModel().getCompletedUploadsCountVM();
        folderListener = new MyFolderListener();
        client = getApplicationModel().getServerClientModel().getClient();
        getUIController().getApplicationModel().getReceivedAskedForFriendshipModel()
                .addListener(new MyAskForFriendshipReceivedListener());
        getUIController().getApplicationModel().getReceivedInvitationModel()
                .addInvitationReceivedListener(new MyInvitationReceivedListener());
        newFriendRequestCountVM = getUIController().getApplicationModel()
                .getReceivedAskedForFriendshipModel().getReceivedAskForFriendshipCountVM();
        newInvitationsCountVM = getUIController().getApplicationModel()
                .getReceivedInvitationModel().getReceivedInvitationsCountVM();
        controller.getFolderRepository().addSynchronizationStatsListener(
                new MySynchronizationStatsListener());

    }

    /**
     * Returns the UI component after optionally building it.
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * One-off build of UI component.
     */
    private void buildUI() {
        initComponents();

        FormLayout layout = new FormLayout("3dlu, pref:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Toolbar
        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 2));

        // Main panel in scroll pane
        JPanel mainPanel = buildMainPanel();
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xyw(1, 6, 2));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
        synchronizationStatusLabel = new JLabel();
        synchronizationDateLabel = new JLabel();
        numberOfFoldersLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.folders"),
                Translation.getTranslation("home_tab.no_folders"),
                false, true);
        sizeOfFoldersLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.total", "KB"),
                null, true, false);
        filesAvailableLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.files_available"), null,
                true, true);
        newInvitationsLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.new_invitations"), null,
                true, true, getApplicationModel().getActionModel()
                .getOpenInvitationReceivedWizardAction());
        newFriendRequestLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.new_friend_requests"), null,
                true, true, getApplicationModel().getActionModel()
                .getAskForFriendshipAction());
        downloadsLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.files_downloaded"), null,
                false, true, getApplicationModel().getActionModel()
                .getOpenDownloadsInformationAction());
        uploadsLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.files_uploaded"), null,
                false, true, getApplicationModel().getActionModel()
                .getOpenUploadsInformationAction());
        computersLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.computers"), Translation
            .getTranslation("home_tab.no_computers"), false, true);
        onlineStorageAccountLabel = new ActionLabel(getController(),
                new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                PFWizard.openLoginWebServiceWizard(getController(),
                    getController().getOSClient());
            }
        });
        onlineStorageSection = new OnlineStorageSection(getController());
        updateTransferText();
        updateFoldersText();
        recalculateFilesAvailable();
        updateComputers();
        updateOnlineStorageDetails();
        updateNewInvitationsText();
        updateNewComputersText();
        initialSyncStats();
        registerListeners();
    }

    private void initialSyncStats() {
        boolean synchronizing = getController().getFolderRepository().isSynchronizing();
        Date syncDate = getController().getFolderRepository().getSynchronizationDate();
        displaySyncStats(syncDate, synchronizing);
    }

    /**
     * Register any listeners.
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        getUIController().getApplicationModel().getNodeManagerModel()
                .addNodeManagerModelListener(new MyNodeManagerModelListener());
        client.addListener(new MyServerClientListener());
    }

    /**
     * Build the main panel with all the detail lines.
     * @return
     */
    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout("3dlu, 100dlu, pref:grow, 3dlu",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref, pref, pref, pref, pref, 3dlu, pref, pref, pref, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref:grow");
        //   sync-stat   sync-date   sep         you-have    files comps invs  down  upl   sep         #fol  szfo  comp  sep         os-acc      osSec
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(synchronizationStatusLabel, cc.xywh(2, row, 3, 1));
        row += 2;

        builder.add(synchronizationDateLabel, cc.xywh(2, row, 3, 1));
        row += 2;

        builder.addSeparator(null, cc.xywh(2, row, 2, 1));
        row +=2;

        JLabel youHaveLabel = new JLabel(Translation.getTranslation("home_tab.you_have"));
        Font f = youHaveLabel.getFont();
        youHaveLabel.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        builder.add(youHaveLabel, cc.xywh(2, row, 2, 1));
        row +=2;

        builder.add(filesAvailableLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.add(newInvitationsLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.add(newFriendRequestLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.add(downloadsLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.add(uploadsLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.addSeparator(null, cc.xywh(2, row, 2, 1));
        row +=2;

        builder.add(numberOfFoldersLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.add(sizeOfFoldersLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.add(computersLine.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        builder.addSeparator(null, cc.xywh(2, row, 2, 1));
        row +=2;

        builder.add(onlineStorageAccountLabel.getUIComponent(), cc.xywh(2, row, 2, 1));
        row += 2;

        builder.add(onlineStorageSection.getUIComponent(), cc.xywh(2, row, 2, 1));
        row++;

        return builder.getPanel();
    }

    /**
     * Updates the text for the number and size of the folders.
     */
    private void updateFoldersText() {
        Folder[] folders = getController().getFolderRepository().getFolders();
        int numberOfFolder = folders.length;
        numberOfFoldersLine.setValue(numberOfFolder);
        long totalSize = 0;
        for (Folder folder : folders) {
            totalSize += folder.getStatistic().getTotalSize();
        }
        // Copied from Format.formatBytesShort()
        totalSize /= 1024;
        String suffix = "KB";
        if (totalSize > 800) {
            totalSize /= 1024;
            suffix = "MB";
        }
        if (totalSize > 800) {
            totalSize /= 1024;
            suffix = "GB";
        }
        sizeOfFoldersLine.setValue(totalSize);
        sizeOfFoldersLine.setNormalLabelText(Translation.getTranslation(
            "home_tab.total", suffix));
    }

    /**
     * Updates the upload / download text.
     */
    private void updateTransferText() {
        downloadsLine.setValue((Integer) downloadsCountVM.getValue());
        uploadsLine.setValue((Integer) uploadsCountVM.getValue());
    }

    private void updateNewInvitationsText() {
        Integer integer = (Integer) newInvitationsCountVM.getValue();
        newInvitationsLine.setValue(integer);
    }

    private void updateNewComputersText() {
        Integer integer = (Integer) newFriendRequestCountVM.getValue();
        newFriendRequestLine.setValue(integer);
    }

    /**
     * Cretes the toolbar.
     *
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton newFolderButton = new JButton(getApplicationModel().getActionModel()
                .getNewFolderAction());
        JButton searchComputerButton = new JButton(getApplicationModel().getActionModel()
                .getFindComputersAction());

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(newFolderButton);
        bar.addRelatedGap();
        bar.addGridded(searchComputerButton);
        return bar.getPanel();
    }

    /**
     * Sums the number of incoming files in all folders.
     */
    private void recalculateFilesAvailable() {
        Folder[] folders = getController().getFolderRepository().getFolders();
        long count = 0;
        for (Folder folder : folders) {
            count += folder.getStatistic().getIncomingFilesCount();
            logFine("Folder: " + folder.getName() + ", incoming: " +
                    folder.getStatistic().getIncomingFilesCount());
        }
        filesAvailableLine.setValue(count);
    }

    /**
     * Updates the information about the number of computers.
     * This is affected by the type selection in the computers tab.
     */
    private void updateComputers() {
        int nodeCount = getUIController().getApplicationModel()
                .getNodeManagerModel().getNodes().size();
        computersLine.setValue(nodeCount);
    }

    /**
     * Updates the Online Storage details.
     */
    private void updateOnlineStorageDetails() {
        boolean active = false;
        if (client.getUsername() == null
            || client.getUsername().trim().length() == 0) {
                onlineStorageAccountLabel.setText(Translation
                    .getTranslation("home_tab.online_storage.not_setup"));
                onlineStorageAccountLabel.setToolTipText(Translation
                    .getTranslation("home_tab.online_storage.not_setup.tips"));
        } else if (client.getPassword() == null ||
                client.getPassword().trim().length() == 0) {
            onlineStorageAccountLabel.setText(Translation
                .getTranslation("home_tab.online_storage.no_password"));
            onlineStorageAccountLabel.setToolTipText(Translation
                .getTranslation("home_tab.online_storage.no_password.tips"));
        } else if (client.isConnected()) {
            if (!client.isLastLoginOK()) {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account_not_logged_in", client
                        .getUsername()));
                onlineStorageAccountLabel.setToolTipText(Translation.getTranslation(
                    "home_tab.online_storage.account_not_logged_in.tips"));
            } else if (client.getAccount().getOSSubscription().isDisabled()) {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account_disabled", client
                        .getUsername()));
                onlineStorageAccountLabel.setToolTipText(Translation.getTranslation(
                    "home_tab.online_storage.account_disabled.tips"));
            } else {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account", client.getUsername()));
                onlineStorageAccountLabel.setToolTipText(Translation.getTranslation(
                    "home_tab.online_storage.account.tips"));
                active = true;
            }
        } else {
            onlineStorageAccountLabel.setText(Translation.getTranslation(
                "home_tab.online_storage.account_connecting", client
                    .getUsername()));
            onlineStorageAccountLabel.setToolTipText(Translation.getTranslation(
                "home_tab.online_storage.account_connecting.tips"));
        }
        if (active) {
            OnlineStorageSubscriptionType storageSubscriptionType = client.getAccount().getOSSubscription().getType();
            long totalStorage = storageSubscriptionType.getStorageSize();
            long spaceUsed = client.getAccountDetails().getSpaceUsed();

            onlineStorageSection.getUIComponent().setVisible(true);
            boolean trial = storageSubscriptionType.isTrial();
            int daysLeft = client.getAccount().getOSSubscription().getDaysLeft();
            onlineStorageSection.setInfo(totalStorage, spaceUsed,
                    trial, daysLeft);
        } else {
            onlineStorageSection.getUIComponent().setVisible(false);
        }
    }

    private void displaySyncStats(Date syncDate, boolean syncing) {

        String syncStatsText = syncing
                ? Translation.getTranslation("home_tab.synchronizing")
                : Translation.getTranslation("home_tab.in_sync");
        synchronizationStatusLabel.setText(syncStatsText);

        String date = Format.formatDate(syncDate);
        String syncDateText = syncing
                ? Translation.getTranslation("home_tab.sync_eta", date)
                : Translation.getTranslation("home_tab.last_synced", date);
        synchronizationDateLabel.setText(syncDateText);
    }

    /**
     * Listener for folder events.
     */
    private class MyFolderListener implements FolderListener {

        public void fileChanged(FolderEvent folderEvent) {
        }

        public void filesDeleted(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            recalculateFilesAvailable();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    /**
     * Listener for folder repo events.
     */
    private class MyFolderRepositoryListener
            implements FolderRepositoryListener {
        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void folderCreated(FolderRepositoryEvent e) {
            e.getFolder().addFolderListener(folderListener);
            updateFoldersText();
            logFine("Added to folder listeners: " + e.getFolder().getName());
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            e.getFolder().removeFolderListener(folderListener);
            updateFoldersText();
            logFine("Removed from folder listeners: " + e.getFolder().getName());
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateFoldersText();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateFoldersText();
        }
    }

    /**
     * Listener for transfer events.
     */
    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateTransferText();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateTransferText();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateTransferText();
        }

    }

    private class MyNodeManagerModelListener implements NodeManagerModelListener {

        public void nodeRemoved(NodeManagerModelEvent e) {
            updateComputers();
        }

        public void nodeAdded(NodeManagerModelEvent e) {
            updateComputers();
        }

        public void rebuilt(NodeManagerModelEvent e) {
            updateComputers();
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void login(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverConnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }
    }

    private class MyAskForFriendshipReceivedListener
            implements AskForFriendshipReceivedListener {

        public void notificationReceived(AskForFriendshipReceivedEvent
                askForFriendshipReceivedEvent) {
            updateNewComputersText();
        }
    }

    private class MyInvitationReceivedListener
            implements InvitationReceivedListener {

        public void invitationReceived(InvitationReceivedEvent
                invitationReceivedEvent) {
            updateNewInvitationsText();
        }
    }

    /**
     * Class to listen for SynchronizationStatsEvents, affects the label text. 
     */
    private class MySynchronizationStatsListener implements
            SynchronizationStatsListener {
        public void synchronizationStatsChanged(SynchronizationStatsEvent event) {
            displaySyncStats(event.getSynchronizationDate(),
                    event.isSynchronizing());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
