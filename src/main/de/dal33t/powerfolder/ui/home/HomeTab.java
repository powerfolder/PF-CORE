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

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Font;

/**
 * Class for the Home tab in the main tab area of the UI.
 */
public class HomeTab extends PFUIComponent {

    private static final int ONE_K = 1024;
    private static final int ONE_M = 1024 * ONE_K;
    private static final int ONE_G = 1024 * ONE_M;

    private JPanel uiComponent;

    private JLabel synchronizationStatusLabel;
    private HomeTabLine numberOfFoldersLine;
    private HomeTabLine sizeOfFoldersLine;
    private HomeTabLine filesAvailableLine;
    private HomeTabLine computersLine;
    private HomeTabLine downloadsLine;
    private HomeTabLine uploadsLine;
    private final ValueModel downloadsCountVM;
    private final ValueModel uploadsCountVM;
    private final MyFolderListener folderListener;
    private ServerClient client;
    private JLabel onlineStorageAccountLabel;
    private OnlineStorageSection onlineStorageSection;

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

        FormLayout layout = new FormLayout("pref:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Toolbar
        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 2));
        builder.addSeparator(null, cc.xy(1, 4));

        // Main panel in scroll pane
        JPanel mainPanel = buildMainPanel();
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xy(1, 6));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
        synchronizationStatusLabel = new JLabel();
        numberOfFoldersLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.folders"),
                Translation.getTranslation("home_tab.no_folders"),
                false, true);
        sizeOfFoldersLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.total_bytes"),
                null, true, true);
        filesAvailableLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.files_available"), null,
                true, true);
        downloadsLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.files_downloaded"), null,
                false, true, getApplicationModel().getActionModel()
                .getOpenDownloadsInformationAction());
        uploadsLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.files_uploaded"), null,
                false, true, getApplicationModel().getActionModel()
                .getOpenUploadsInformationAction());
        computersLine = new HomeTabLine(getController(),
                Translation.getTranslation("home_tab.computers"),
                Translation.getTranslation("home_tab.no_computers"),
                false, true);
        onlineStorageAccountLabel = new JLabel();
        onlineStorageSection = new OnlineStorageSection(getController());
        updateTransferText();
        updateFoldersText();
        recalculateFilesAvailable();
        updateComputers();
        updateOnlineStorageDetails();
        registerListeners();
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
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref, pref, pref, 3dlu, pref, pref, pref, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref:grow");
        //   sync        sep         you-have    files down  upl   sep         #fol  szfo  comp  sep         os-acc      osSec
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(synchronizationStatusLabel, cc.xywh(2, row, 3, 1));
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

        builder.add(onlineStorageAccountLabel, cc.xywh(2, row, 2, 1));
        row += 2;

        builder.add(onlineStorageSection.getUIComponent(), cc.xywh(2, row, 2, 1));
        row += 2;

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
        String descriptionKey;
        double num;
        if (totalSize >= ONE_G) {
            descriptionKey = "home_tab.total_gigabytes";
            num = (double) totalSize / (double) ONE_G;
        } else if (totalSize >= ONE_M) {
            descriptionKey = "home_tab.total_megabytes";
            num = (double) totalSize / (double) ONE_M;
        } else if (totalSize >= ONE_K) {
            descriptionKey = "home_tab.total_kilobytes";
            num = (double) totalSize / (double) ONE_K;
        } else {
            descriptionKey = "home_tab.total_bytes";
            num = totalSize;
        }

        sizeOfFoldersLine.setValue(num);
        sizeOfFoldersLine.setNormalLabelText(
                Translation.getTranslation(descriptionKey));
    }

    /**
     * Updates the upload / download text.
     */
    private void updateTransferText() {
        synchronizationStatusLabel.setText(getSyncText());
        downloadsLine.setValue((Integer) downloadsCountVM.getValue());
        uploadsLine.setValue((Integer) uploadsCountVM.getValue());
    }

    /**
     * Updates the synchronization text.
     * @return
     */
    private String getSyncText() {
        return getController().getFolderRepository()
                .isAnyFolderTransferring() ?
                Translation.getTranslation("home_tab.synchronizing") :
                Translation.getTranslation("home_tab.in_sync");
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

        FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref, pref:grow",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(newFolderButton, cc.xy(2, 1));
        builder.add(searchComputerButton, cc.xy(4, 1));

        return builder.getPanel();
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
        if (client == null || client.getUsername() == null ||
                client.getUsername().trim().length() == 0) {
            onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.not_setup"));
        } else if (client.isConnected()) {
            if (client.getAccount().getOSSubscription().isDisabled()) {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                        "home_tab.online_storage.account_disabled",
                        client.getUsername()));
            } else {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                        "home_tab.online_storage.account", client.getUsername()));
                active = true;
            }
        } else {
            onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account_connecting",
                    client.getUsername()));
        }
        if (active) {
            OnlineStorageSubscriptionType storageSubscriptionType =
                    client.getAccount().getOSSubscription().getType();
            long totalStorage = storageSubscriptionType.getStorageSize();
            long spaceUsed = client.getAccountDetails().getSpaceUsed();
            double spacedUsedPercentage = 100.0d * (double) spaceUsed /
                    (double) totalStorage;
            if (spacedUsedPercentage < 0.0d) {
                spacedUsedPercentage = 0.0d;
            }
            if (spacedUsedPercentage > 100.0d) {
                spacedUsedPercentage = 100.0d;
            }

            onlineStorageSection.getUIComponent().setVisible(true);
            boolean trial = storageSubscriptionType.isTrial();
            int daysLeft = client.getAccount().getOSSubscription().getDaysLeft();
            onlineStorageSection.setInfo(spacedUsedPercentage,
                    trial,
                    daysLeft);
        } else {
            onlineStorageSection.getUIComponent().setVisible(false);
        }
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
}
