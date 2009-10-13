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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.event.OverallFolderStatEvent;
import de.dal33t.powerfolder.event.OverallFolderStatListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TellFriendPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.TransferCounter;
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
    private HomeTabLine newWarningsLine;
    private HomeTabLine newInvitationsLine;
    private HomeTabLine newFriendRequestsLine;
    private HomeTabLine newSingleFileOffersLine;
    private HomeTabLine computersLine;
    private HomeTabLine downloadsLine;
    private HomeTabLine uploadsLine;
    private final ValueModel downloadsCountVM;
    private final ValueModel uploadsCountVM;
    private final MyFolderListener folderListener;
    private ServerClient client;
    private ActionLabel onlineStorageAccountLabel;
    private OnlineStorageSection onlineStorageSection;
    private LicenseInfoSection licenseInfoSection;
    private LinkLabel buyNowLabel;
    private ActionLabel tellFriendLabel;

    private final ValueModel newWarningsCountVM;
    private final ValueModel newFriendRequestsCountVM;
    private final ValueModel newInvitationsCountVM;
    private final ValueModel newSingleFileOffersCountVM;

    /**
     * Constructor
     * 
     * @param controller
     */
    public HomeTab(Controller controller) {
        super(controller);
        downloadsCountVM = getApplicationModel().getTransferManagerModel()
            .getAllDownloadsCountVM();
        uploadsCountVM = getApplicationModel().getTransferManagerModel()
            .getAllUploadsCountVM();
        folderListener = new MyFolderListener();
        client = getApplicationModel().getServerClientModel().getClient();
        newFriendRequestsCountVM = getUIController().getApplicationModel()
            .getReceivedAskedForFriendshipModel()
            .getReceivedAskForFriendshipCountVM();
        newFriendRequestsCountVM
            .addValueChangeListener(new MyFriendRequestListener());
        newInvitationsCountVM = getUIController().getApplicationModel()
            .getReceivedInvitationsModel().getReceivedInvitationsCountVM();
        newInvitationsCountVM
            .addValueChangeListener(new MyInvitationListener());
        newSingleFileOffersCountVM = getUIController().getApplicationModel()
            .getReceivedSingleFileOffersModel()
            .getReceivedSingleFileOfferCountVM();
        newSingleFileOffersCountVM
            .addValueChangeListener(new MyOfferPropertyListener());
        newWarningsCountVM = getUIController().getApplicationModel()
            .getWarningsModel().getWarningsCountVM();
        newWarningsCountVM.addValueChangeListener(new MyWarningsListener());
        controller.getFolderRepository().addOverallFolderStatListener(
            new MyOverallFolderStatListener());
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        getApplicationModel().getUseOSModel().addValueChangeListener(
            new UseOSModelListener());
        getApplicationModel().getLicenseModel().getDaysValidModel()
            .addValueChangeListener(new MyDaysValidListener());
    }

    /**
     * @return the UI component after optionally building it.
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        uiComponent.setTransferHandler(new MyTransferHandler());
        return uiComponent;
    }

    /**
     * One-off build of UI component.
     */
    private void buildUI() {
        initComponents();

        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Toolbar
        JPanel toolbar = createToolBar();
        toolbar.setOpaque(false);
        builder.add(toolbar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 2));

        // Main panel in scroll pane
        JPanel mainPanel = buildMainPanel();
        mainPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xyw(1, 6, 2));

        uiComponent = GradientPanel.create(builder.getPanel());
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
        synchronizationStatusLabel = new JLabel();
        synchronizationDateLabel = new JLabel();
        numberOfFoldersLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.folders"), null, false, true, null, null);
        sizeOfFoldersLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.total", "KB"), null, true, false, null,
            null);
        filesAvailableLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.files_available"), null, true, true,
            null, null);
        newWarningsLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.new_warnings"), null, true, true,
            getApplicationModel().getActionModel().getActivateWarningAction(),
            Icons.getIconById(Icons.WARNING));
        newInvitationsLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.new_invitations"), null, true, true,
            getApplicationModel().getActionModel()
                .getOpenInvitationReceivedWizardAction(), Icons
                .getIconById(Icons.INFORMATION));
        newFriendRequestsLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.new_friend_requests"), null, true, true,
            getApplicationModel().getActionModel().getAskForFriendshipAction(),
            Icons.getIconById(Icons.INFORMATION));
        newSingleFileOffersLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.new_single_file_offers"), null, true,
            true, getApplicationModel().getActionModel()
                .getSingleFileTransferOfferAction(), null);
        downloadsLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.files_downloads"), null, false, true,
            getApplicationModel().getActionModel()
                .getOpenDownloadsInformationAction(), null);
        uploadsLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.files_uploads"), null, false, true,
            getApplicationModel().getActionModel()
                .getOpenUploadsInformationAction(), null);
        computersLine = new HomeTabLine(getController(), Translation
            .getTranslation("home_tab.computers"), Translation
            .getTranslation("home_tab.no_computers"), false, true, null, null);
        onlineStorageAccountLabel = new ActionLabel(getController(),
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    PFWizard.openLoginWizard(getController(), getController()
                        .getOSClient());
                }
            });
        onlineStorageAccountLabel.getUIComponent().setBorder(
            Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        onlineStorageSection = new OnlineStorageSection(getController());
        onlineStorageSection.getUIComponent().setBorder(
            Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        licenseInfoSection = new LicenseInfoSection(getController());
        buyNowLabel = new LinkLabel(getController(), "", "");
        UIUtil.convertToBigLabel((JLabel) buyNowLabel.getUIComponent());
        buyNowLabel.getUIComponent().setVisible(false);
        buyNowLabel.getUIComponent().setBorder(
            Borders.createEmptyBorder("20dlu, 0, 0, 0"));
        if (!ProUtil.isRunningProVersion() && Feature.BETA.isDisabled()) {
            showBuyNowLink(Translation
                .getTranslation("pro.home_tab.upgrade_powerfolder"));
        }
        tellFriendLabel = new ActionLabel(getController(), new AbstractAction()
        {
            public void actionPerformed(ActionEvent e) {
                PFWizard wizard = new PFWizard(getController());
                wizard.open(new TellFriendPanel(getController()));
            }
        });
        tellFriendLabel.setText(Translation
            .getTranslation("home_tab.tell_friend.text"));
        tellFriendLabel.setToolTipText(Translation
            .getTranslation("home_tab.tell_friend.tip"));

        updateTransferText();
        updateFoldersText();
        recalculateFilesAvailable();
        updateComputers();
        updateOnlineStorageDetails();
        updateLicenseDetails();
        updateNewInvitationsText();
        updateNewWarningsText();
        updateNewComputersText();
        updateNewSingleFileOffersText();
        initialSyncStats();
        registerListeners();

        // Start monitoring the up/download rate
        getController().scheduleAndRepeat(new MyTimerTask(), 1000, 1000);

    }

    private void initialSyncStats() {
        boolean synced = getController().getFolderRepository().isSynchronized();
        Date syncDate = getController().getFolderRepository()
            .getSynchronizationDate();
        displaySyncStats(syncDate, synced, !getController().getNodeManager()
            .isStarted());
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
     * 
     * @return
     */
    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 12dlu, " + // Sync section
                "pref, 3dlu, pref, pref, pref, pref, pref, pref, pref, 9dlu, " + // You
                // have
                // section
                "pref, 3dlu, pref, pref, pref, 9dlu, " + // Local section
                "pref, 3dlu, pref, pref, pref, pref, 3dlu, " + // Online
                // Storage
                // section + License key section
                "pref:grow, pref");
        // sep, sync-stat sync-date sep warn, files invs comps singl
        // down upl sep #fol szfo comp sep os-acc osSec tell friend

        PanelBuilder builder = new PanelBuilder(layout);
        // Bottom border
        builder.setBorder(Borders.createEmptyBorder("1dlu, 3dlu, 2dlu, 3dlu"));
        CellConstraints cc = new CellConstraints();

        int row = 1;

        builder.addSeparator(Translation
            .getTranslation("home_tab.synchronization"), cc.xy(1, row));
        row += 2;
        builder.add(synchronizationStatusLabel, cc.xy(1, row));
        row += 2;
        builder.add(synchronizationDateLabel, cc.xy(1, row));
        row += 2;

        builder.addSeparator(Translation.getTranslation("home_tab.you_have"),
            cc.xy(1, row));
        row += 2;
        builder.add(newWarningsLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(filesAvailableLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(newInvitationsLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(newFriendRequestsLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(newSingleFileOffersLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(downloadsLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(uploadsLine.getUIComponent(), cc.xy(1, row));
        row += 2;

        builder.addSeparator(Translation.getTranslation("home_tab.local"), cc
            .xy(1, row));
        row += 2;
        builder.add(numberOfFoldersLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(sizeOfFoldersLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(computersLine.getUIComponent(), cc.xy(1, row));
        row += 2;

        builder.addSeparator(Translation
            .getTranslation("home_tab.online_storage.title"), cc.xy(1, row));
        row += 2;
        builder.add(onlineStorageAccountLabel.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(onlineStorageSection.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(licenseInfoSection.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(buyNowLabel.getUIComponent(), cc.xy(1, row));
        row += 3;
        builder.add(tellFriendLabel.getUIComponent(), cc.xy(1, row));

        return builder.getPanel();
    }

    /**
     * Updates the text for the number and size of the folders.
     */
    private void updateFoldersText() {
        Collection<Folder> folders = getController().getFolderRepository()
            .getFolders();
        int numberOfFolder = folders.size();
        numberOfFoldersLine.setValue(numberOfFolder);
        long totalSize = 0;
        for (Folder folder : folders) {
            totalSize += folder.getStatistic().getTotalSize();
        }
        // Copied from Format.formatBytesShort()
        totalSize /= 1024;
        String suffix = "KB";
        if (totalSize >= 1024) {
            totalSize /= 1024;
            suffix = "MB";
        }
        if (totalSize >= 1024) {
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

    private void updateNewWarningsText() {
        Integer integer = (Integer) newWarningsCountVM.getValue();
        newWarningsLine.setValue(integer);
    }

    private void updateNewInvitationsText() {
        Integer integer = (Integer) newInvitationsCountVM.getValue();
        newInvitationsLine.setValue(integer);
    }

    private void updateNewComputersText() {
        Integer integer = (Integer) newFriendRequestsCountVM.getValue();
        newFriendRequestsLine.setValue(integer);
    }

    private void updateNewSingleFileOffersText() {
        Integer integer = (Integer) newSingleFileOffersCountVM.getValue();
        newSingleFileOffersLine.setValue(integer);
    }

    private void showBuyNowLink(String text) {
        buyNowLabel.setTextAndURL(text, ConfigurationEntry.PROVIDER_BUY_URL
            .getValue(getController()));
        buyNowLabel.getUIComponent().setVisible(true);
    }

    /**
     * Cretes the toolbar.
     * 
     * @return the toolbar
     */
    private JPanel createToolBar() {

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        JButton newFolderButton = new JButton(getApplicationModel()
            .getActionModel().getNewFolderAction());
        bar.addGridded(newFolderButton);
        if (!getController().isBackupOnly()) {
            JButton searchComputerButton = new JButton(getApplicationModel()
                .getActionModel().getFindComputersAction());
            bar.addRelatedGap();
            bar.addGridded(searchComputerButton);
        }

        return bar.getPanel();
    }

    /**
     * Sums the number of incoming files in all folders.
     */
    private void recalculateFilesAvailable() {
        Collection<Folder> folders = getController().getFolderRepository()
            .getFolders();
        long count = 0;
        for (Folder folder : folders) {
            count += folder.getStatistic().getIncomingFilesCount();
            if (isFine()) {
                logFine("Folder: " + folder.getName() + ", incoming: "
                    + folder.getStatistic().getIncomingFilesCount());
            }
        }
        filesAvailableLine.setValue(count);
    }

    /**
     * Updates the information about the number of computers. This is affected
     * by the type selection in the computers tab.
     */
    private void updateComputers() {
        // FIXME This calculation depends on FILTER setting.
        int nodeCount = getUIController().getApplicationModel()
            .getNodeManagerModel().getNodes().size();
        computersLine.setValue(nodeCount);
    }

    private void updateLicenseDetails() {
        licenseInfoSection.getUIComponent();
        Integer daysValid = (Integer) getApplicationModel().getLicenseModel()
            .getDaysValidModel().getValue();
        if (daysValid != null) {
            licenseInfoSection.setDaysValid(daysValid);
        } else {
            licenseInfoSection.setDaysValid(-1);
        }

        // Display buynow link: If is trial or about to expire or not allowed to
        // run
        boolean trial = ProUtil.isTrial(getController());
        boolean allowed = ProUtil.isAllowedToRun(getController());
        boolean aboutToExpire = daysValid != null && daysValid != -1
            && daysValid < 30;
        if (trial || !allowed) {
            showBuyNowLink(Translation
                .getTranslation("pro.home_tab.upgrade_powerfolder"));
        } else if (aboutToExpire) {
            showBuyNowLink(Translation
                .getTranslation("pro.home_tab.renew_license"));
        }
    }

    /**
     * Updates the Online Storage details.
     */
    private void updateOnlineStorageDetails() {
        boolean active = false;
        boolean showBuyNow = false;
        if (client.getUsername() == null
            || client.getUsername().trim().length() == 0)
        {
            onlineStorageAccountLabel.setText(Translation
                .getTranslation("home_tab.online_storage.not_setup"));
            onlineStorageAccountLabel.setToolTipText(Translation
                .getTranslation("home_tab.online_storage.not_setup.tips"));
        } else if (client.getPassword() == null
            || client.getPassword().trim().length() == 0)
        {
            onlineStorageAccountLabel.setText(Translation
                .getTranslation("home_tab.online_storage.no_password"));
            onlineStorageAccountLabel.setToolTipText(Translation
                .getTranslation("home_tab.online_storage.no_password.tips"));
        } else if (client.isConnected()) {
            if (!client.isLoggedIn()) {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account_not_logged_in", client
                        .getUsername()));
                onlineStorageAccountLabel
                    .setToolTipText(Translation
                        .getTranslation("home_tab.online_storage.account_not_logged_in.tips"));
            } else if (client.getAccount().getOSSubscription().isDisabled()) {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account_disabled", client
                        .getUsername()));
                onlineStorageAccountLabel
                    .setToolTipText(Translation
                        .getTranslation("home_tab.online_storage.account_disabled.tips"));
                showBuyNow = true;
            } else {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "home_tab.online_storage.account", client.getUsername()));
                onlineStorageAccountLabel.setToolTipText(Translation
                    .getTranslation("home_tab.online_storage.account.tips"));
                active = true;
            }
        } else {
            onlineStorageAccountLabel.setText(Translation.getTranslation(
                "home_tab.online_storage.account_connecting", client
                    .getUsername()));
            onlineStorageAccountLabel
                .setToolTipText(Translation
                    .getTranslation("home_tab.online_storage.account_connecting.tips"));
        }

        // Don't show if PowerFolder is disabled.
        onlineStorageAccountLabel.getUIComponent().setVisible(
            getController().getNodeManager().isStarted());

        if (active) {
            OnlineStorageSubscription storageSubscription = client.getAccount()
                .getOSSubscription();
            long totalStorage = storageSubscription.getStorageSize();
            long spaceUsed = client.getAccountDetails().getSpaceUsed();
            if (spaceUsed > (double) totalStorage * 0.8) {
                showBuyNow = true;
            }
            onlineStorageSection.getUIComponent().setVisible(true);
            onlineStorageSection.setInfo(totalStorage, spaceUsed);
        } else {
            onlineStorageSection.getUIComponent().setVisible(false);
        }

        // Show Buy now link if: Disabled OR >80%
        if (showBuyNow) {
            showBuyNowLink(Translation
                .getTranslation("pro.home_tab.upgrade_powerfolder"));
        }

    }

    private void displaySyncStats(Date syncDate, boolean synced,
        boolean disabled)
    {
        if (synchronizationStatusLabel != null) {
            String syncStatsText;
            if (disabled) {
                // Not running
                syncStatsText = Translation
                    .getTranslation("home_tab.not_running");
            } else if (getController().getFolderRepository().getFoldersCount() == 0)
            {
                // No folders
                syncStatsText = Translation
                    .getTranslation("home_tab.no_folders");
            } else if (syncDate == null && synced) { // Never synced
                syncStatsText = Translation
                    .getTranslation("home_tab.never_synced");
            } else {
                syncStatsText = synced ? Translation
                    .getTranslation("home_tab.in_sync") : Translation
                    .getTranslation("home_tab.synchronizing");
            }
            synchronizationStatusLabel.setText(syncStatsText);
        }

        if (synchronizationDateLabel != null) {
            if (syncDate == null) {
                synchronizationDateLabel.setVisible(false);
            } else {
                String date = Format.formatDateShort(syncDate);
                String syncDateText = synced ? Translation.getTranslation(
                    "home_tab.last_synced", date) : Translation.getTranslation(
                    "home_tab.sync_eta", date);
                synchronizationDateLabel.setVisible(true);
                synchronizationDateLabel.setText(syncDateText);
            }
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
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
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

    private class MyNodeManagerModelListener implements
        NodeManagerModelListener
    {

        public void changed() {
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

    /**
     * Handler to accept folder drops, opening folder wizard.
     */
    private class MyTransferHandler extends TransferHandler {

        /**
         * Whether this drop can be imported; must be file list flavor.
         * 
         * @param support
         * @return
         */
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        /**
         * Import the file. Only import if it is a single directory.
         * 
         * @param support
         * @return
         */
        public boolean importData(TransferSupport support) {

            if (!support.isDrop()) {
                return false;
            }

            final File file = getFileList(support);
            if (file == null) {
                return false;
            }

            // Run later, so do not tie up OS drag and drop process.
            Runnable runner = new Runnable() {
                public void run() {
                    if (file.isDirectory()) {
                        PFWizard.openExistingDirectoryWizard(getController(),
                            file);
                    } else if (file.getName().endsWith(".invitation")) {
                        Invitation invitation = InvitationUtil.load(file);
                        PFWizard.openInvitationReceivedWizard(getController(),
                            invitation);
                    }
                }
            };
            SwingUtilities.invokeLater(runner);

            return true;
        }

        /**
         * Get the directory to import. The transfer is a list of files; need to
         * check the list has one directory, else return null.
         * 
         * @param support
         * @return
         */
        private File getFileList(TransferSupport support) {
            Transferable t = support.getTransferable();
            try {
                List list = (List) t
                    .getTransferData(DataFlavor.javaFileListFlavor);
                if (list.size() == 1) {
                    for (Object o : list) {
                        if (o instanceof File) {
                            File file = (File) o;
                            if (file.isDirectory()) {
                                return file;
                            } else if (file.getName().endsWith(".invitation")) {
                                return file;
                            }
                        }
                    }
                }
            } catch (UnsupportedFlavorException e) {
                logSevere(e);
            } catch (IOException e) {
                logSevere(e);
            }
            return null;
        }
    }

    private class UseOSModelListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateOnlineStorageDetails();
        }
    }

    private final class MyDaysValidListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            updateLicenseDetails();
        }
    }

    private class MyOfferPropertyListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateNewSingleFileOffersText();
        }
    }

    private class MyFriendRequestListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateNewComputersText();
        }
    }

    private class MyInvitationListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateNewInvitationsText();
        }
    }

    private class MyWarningsListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateNewWarningsText();
        }
    }

    private class MyOverallFolderStatListener implements
        OverallFolderStatListener
    {
        public void statCalculated(OverallFolderStatEvent e) {
            displaySyncStats(e.getSyncDate(), e.isAllInSync(), !getController()
                .getNodeManager().isStarted());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        @Override
        public void startStop(NodeManagerEvent e) {
            initialSyncStats();
            updateLicenseDetails();
        }
    }

    /**
     * Class to update the up/download rates.
     */
    private class MyTimerTask extends TimerTask {

        private final TransferCounter uploadCounter;
        private final TransferCounter downloadCounter;

        private MyTimerTask() {
            TransferManager transferManager = getController()
                .getTransferManager();
            uploadCounter = transferManager.getUploadCounter();
            downloadCounter = transferManager.getDownloadCounter();
        }

        public void run() {
            double d = uploadCounter.calculateCurrentKBS();
            if (Double.compare(d, 0) == 0) {
                uploadsLine.setNormalLabelText(Translation
                    .getTranslation("home_tab.files_uploads"));
            } else {
                String s = Format.formatDecimal(d);
                uploadsLine.setNormalLabelText(Translation.getTranslation(
                    "home_tab.files_uploads_active", s));
            }
            d = downloadCounter.calculateCurrentKBS();
            if (Double.compare(d, 0) == 0) {
                downloadsLine.setNormalLabelText(Translation
                    .getTranslation("home_tab.files_downloads"));
            } else {
                String s = Format.formatDecimal(d);
                downloadsLine.setNormalLabelText(Translation.getTranslation(
                    "home_tab.files_downloads_active", s));
            }
        }
    }
}
