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
 * $Id: StatusTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.status;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Date;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
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
import de.dal33t.powerfolder.event.OverallFolderStatListener;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.FileDropTransferHandler;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.model.NoticesModel;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.notices.AskForFriendshipEventNotice;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.NoticeSeverity;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.UIUtil;

/**
 * Class for the Status tab in the main tab area of the UI.
 */
public class StatusTab extends PFUIComponent {

    private JPanel uiComponent;

    private JLabel synchronizationStatusLabel;
    private JLabel synchronizationDateLabel;
    private StatusTabLine numberOfFoldersLine;
    private StatusTabLine sizeOfFoldersLine;
    private StatusTabLine filesAvailableLine;
    private StatusTabLine newNoticesLine;
    private StatusTabLine downloadsLine;
    private StatusTabLine uploadsLine;
    private final ValueModel downloadsCountVM;
    private final ValueModel uploadsCountVM;
    private final MyFolderListener folderListener;
    private ServerClient client;
    private ActionLabel onlineStorageAccountLabel;
    private OnlineStorageSection onlineStorageSection;
    private LicenseInfoSection licenseInfoSection;
    private LinkLabel buyNowLabel;
    private ActionLabel tellFriendLabel;

    private NoticesModel noticeModel;

    /**
     * Constructor
     * 
     * @param controller
     */
    public StatusTab(Controller controller) {
        super(controller);
        downloadsCountVM = getApplicationModel().getTransferManagerModel()
            .getAllDownloadsCountVM();
        uploadsCountVM = getApplicationModel().getTransferManagerModel()
            .getAllUploadsCountVM();
        folderListener = new MyFolderListener();
        client = getApplicationModel().getServerClientModel().getClient();
        noticeModel = getApplicationModel().getNoticesModel();
    }

    /**
     * @return the UI component after optionally building it.
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        uiComponent.setTransferHandler(new FileDropTransferHandler(
            getController()));
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

        uiComponent = builder.getPanel();
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
        synchronizationStatusLabel = new JLabel();
        synchronizationDateLabel = new JLabel();
        numberOfFoldersLine = new StatusTabLine(getController(),
            Translation.getTranslation("status_tab.folders"),
            Translation.getTranslation("status_tab.no_folders"), false, true,
            null, null);
        sizeOfFoldersLine = new StatusTabLine(getController(),
            Translation.getTranslation("status_tab.total", "kB"), null, true,
            false, null, null);
        filesAvailableLine = new StatusTabLine(getController(),
            Translation.getTranslation("status_tab.files_available"), null,
            true, true, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    getUIController().getMainFrame().showFoldersTab();
                }
            }, null);
        newNoticesLine = new StatusTabLine(getController(),
            Translation.getTranslation("status_tab.unread_notices"), null,
            false, true, getApplicationModel().getActionModel()
                .getViewNoticesAction(), Icons.getIconById(Icons.WARNING));
        downloadsLine = new StatusTabLine(getController(),
            Translation.getTranslation("status_tab.files_downloads"), null,
            false, true, getApplicationModel().getActionModel()
                .getOpenDownloadsInformationAction(), null);
        uploadsLine = new StatusTabLine(getController(),
            Translation.getTranslation("status_tab.files_uploads"), null,
            false, true, getApplicationModel().getActionModel()
                .getOpenUploadsInformationAction(), null);
        onlineStorageAccountLabel = new ActionLabel(getController(),
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    boolean changeLoginAllowed = ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED
                        .getValueBoolean(getController());
                    if (changeLoginAllowed) {
                        PFWizard.openLoginWizard(getController(),
                            getController().getOSClient());
                    }
                }
            });
        onlineStorageAccountLabel.getUIComponent().setBorder(
            Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        onlineStorageSection = new OnlineStorageSection(getController());
        onlineStorageSection.getUIComponent().setBorder(
            Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        licenseInfoSection = new LicenseInfoSection(getController());
        buyNowLabel = new LinkLabel(getController(), "", "");
        buyNowLabel.convertToBigLabel();
        UIUtil.setFontStyle(buyNowLabel.getUIComponent(), Font.BOLD);
        buyNowLabel.getUIComponent().setVisible(false);
        buyNowLabel.getUIComponent().setBorder(
            Borders.createEmptyBorder("20dlu, 0, 0, 0"));
        if (!ProUtil.isRunningProVersion() && Feature.BETA.isDisabled()) {
            updateBuyNowLink(
                Translation
                    .getTranslation("pro.status_tab.upgrade_powerfolder"),
                true);
        }
        tellFriendLabel = SimpleComponentFactory
            .createTellAFriendLabel(getController());

        updateTransferText();
        updateFoldersText();
        recalculateFilesAvailable();
        updateOnlineStorageDetails();
        updateLicenseDetails();
        updateNewNoticesText();
        updateSyncStats();
        registerListeners();

        // Start periodical updates
        getController().scheduleAndRepeat(new MyTimerTask(), 5000, 5000);
    }

    private void updateSyncStats() {
        FolderRepositoryModel folderRepositoryModel =
                getUIController().getApplicationModel()
                        .getFolderRepositoryModel();
        boolean syncing = folderRepositoryModel.isSyncing();
        Date syncDate;
        if (syncing) {
            syncDate = folderRepositoryModel.getEstimatedSyncDate();
        } else {
            syncDate = folderRepositoryModel.getLastSyncDate();
        }
        double overallSyncPercentage =
                folderRepositoryModel.getOverallSyncPercentage();

        if (isFiner()) {
            logFiner("Sync status: syncing? " + syncing + ", date: " + syncDate);
        }
        if (synchronizationStatusLabel != null) {
            String syncStatsText;
            if (!getController().getNodeManager().isStarted()) {
                // Not started
                syncStatsText = Translation
                    .getTranslation("status_tab.not_running");
            } else if (getController().getFolderRepository().getFoldersCount() == 0)
            {
                // No folders
                syncStatsText = Translation
                    .getTranslation("status_tab.no_folders");
            } else if (syncDate == null && !syncing) { // Never synced
                syncStatsText = Translation
                    .getTranslation("status_tab.never_synced");
            } else {
                if (syncing) {
                    syncStatsText = Translation
                    .getTranslation("status_tab.syncing",
                            Format.formatDecimal(overallSyncPercentage));
                } else {
                    syncStatsText = Translation
                        .getTranslation("status_tab.in_sync");
                }
            }
            synchronizationStatusLabel.setText(syncStatsText);
        }

        if (synchronizationDateLabel != null) {
            if (syncDate == null) {
                synchronizationDateLabel.setVisible(false);
            } else {
                String syncDateText;
                if (DateUtil.isDateMoreThanNDaysInFuture(syncDate, 2)) {
                    syncDateText = Translation
                        .getTranslation("status_tab.sync_unknown");
                } else {
                    String date = Format.formatDateShort(syncDate);
                    syncDateText = syncing ? Translation.getTranslation(
                        "status_tab.sync_eta", date) : Translation
                        .getTranslation("status_tab.last_synced", date);
                }
                synchronizationDateLabel.setVisible(true);
                synchronizationDateLabel.setText(syncDateText);
            }
        }
    }

    /**
     * Register any listeners.
     */
    private void registerListeners() {
        noticeModel.getAllNoticesCountVM().addValueChangeListener(
            new MyNoticesListener());
        noticeModel.getUnreadNoticesCountVM().addValueChangeListener(
            new MyNoticesListener());
        getApplicationModel().getFolderRepositoryModel()
            .addOverallFolderStatListener(new MyOverallFolderStatListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        getApplicationModel().getUseOSModel().addValueChangeListener(
            new UseOSModelListener());
        getApplicationModel().getLicenseModel().getDaysValidModel()
            .addValueChangeListener(new MyDaysValidListener());
        getApplicationModel().getLicenseModel().getLicenseKeyModel()
            .addValueChangeListener(new MyLicenseKeyListener());

        downloadsCountVM.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateTransferText();
            }
        });
        uploadsCountVM.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateTransferText();
            }
        });
        client.addListener(new MyServerClientListener());
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            folder.addFolderListener(folderListener);
        }
    }

    /**
     * Build the main panel with all the detail lines.
     * 
     * @return
     */
    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 12dlu, pref, 3dlu, pref, pref, "
                + "pref, pref, 9dlu, pref, 3dlu, pref, "
                + "pref, pref, pref, 0:grow, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        // Bottom border
        builder.setBorder(Borders.createEmptyBorder("1dlu, 3dlu, 2dlu, 3dlu"));
        CellConstraints cc = new CellConstraints();

        int row = 1;

        builder.addSeparator(Translation.getTranslation("status_tab.status"),
            cc.xy(1, row));
        row += 2;
        builder.add(synchronizationStatusLabel, cc.xy(1, row));
        row += 2;
        builder.add(synchronizationDateLabel, cc.xy(1, row));
        row += 2;

        builder.addSeparator(Translation.getTranslation("status_tab.you_have"),
            cc.xy(1, row));
        row += 2;
        builder.add(newNoticesLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(filesAvailableLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(downloadsLine.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(uploadsLine.getUIComponent(), cc.xy(1, row));
        row += 2;

        builder.addSeparator(
            Translation.getTranslation("status_tab.online_storage.title"),
            cc.xy(1, row));
        row += 2;
        builder.add(onlineStorageAccountLabel.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(onlineStorageSection.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(licenseInfoSection.getUIComponent(), cc.xy(1, row));
        row++;
        builder.add(buyNowLabel.getUIComponent(), cc.xy(1, row));
        if (PreferencesEntry.SHOW_TELL_A_FRIEND
            .getValueBoolean(getController()))
        {
            row += 2;
            builder.add(tellFriendLabel.getUIComponent(), cc.xy(1, row));
        }

        return builder.getPanel();
    }

    /**
     * Updates the text for the number and size of the folders.
     */
    private void updateFoldersText() {
        // #2002: Hide
        numberOfFoldersLine.setValue(0);
        sizeOfFoldersLine.setValue(0);
    }

    /**
     * Updates the upload / download text.
     */
    private void updateTransferText() {
        downloadsLine.setValue((Integer) downloadsCountVM.getValue());
        uploadsLine.setValue((Integer) uploadsCountVM.getValue());
    }

    private void updateNewNoticesText() {

        int unread = (Integer) noticeModel.getUnreadNoticesCountVM().getValue();
        int all = (Integer) noticeModel.getAllNoticesCountVM().getValue();

        // See if they are all one type
        Class clazz = null;
        boolean variety = false;
        for (Notice notice : noticeModel.getAllNotices()) {
            if (notice.isRead()) {
                // Skip alread read notices.
                continue;
            }
            if (clazz == null) {
                clazz = notice.getClass();
            } else if (clazz != notice.getClass()) {
                variety = true;
                break;
            }
        }

        // Adjust status text if they are all one variety.
        String noticesText;
        if (clazz != null && !variety) {
            if (clazz == AskForFriendshipEventNotice.class) {
                noticesText = Translation
                    .getTranslation("status_tab.new_friendship_notices");
            } else if (clazz == WarningNotice.class) {
                noticesText = Translation
                    .getTranslation("status_tab.new_warning_notices");
            } else if (clazz == InvitationNotice.class) {
                noticesText = Translation
                    .getTranslation("status_tab.new_invitation_notices");
            } else {
                // Default
                noticesText = Translation
                    .getTranslation("status_tab.unread_notices");
            }
        } else {
            noticesText = Translation
                .getTranslation("status_tab.unread_notices");
        }
        newNoticesLine.setNormalLabelText(noticesText);
        newNoticesLine.setValue(unread);
        newNoticesLine.getUIComponent().setVisible(all != 0);

        // If there are any warnings, set icon as warning, else information.
        Icon noticesIcon = Icons.getIconById(Icons.INFORMATION);
        for (Notice notice : getUIController().getApplicationModel()
            .getNoticesModel().getAllNotices())
        {
            if (notice.getNoticeSeverity() == NoticeSeverity.WARINING) {
                noticesIcon = Icons.getIconById(Icons.WARNING);
                break;
            }
        }
        if (unread > 0) {
            newNoticesLine.setNzIcon(noticesIcon);
        } else {
            newNoticesLine.setNzIcon(null);
        }
    }

    private void updateBuyNowLink(String text, boolean visible) {
        if (StringUtils.isBlank(ProUtil.getBuyNowURL(getController()))) {
            visible = false;
        }
        buyNowLabel.setTextAndURL(text, ProUtil.getBuyNowURL(getController()));
        buyNowLabel.getUIComponent().setVisible(visible);
    }

    /**
     * Cretes the toolbar.
     * 
     * @return the toolbar
     */
    private JPanel createToolBar() {

        Boolean expertMode = PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController());
        FormLayout layout;
        if (expertMode) {
            layout = new FormLayout("pref, 3dlu, pref, 3dlu:grow", "pref");
        } else {
            layout = new FormLayout("pref, 3dlu:grow", "pref");
        }
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Set the links to the height of a checkbox,
        // to make the layout similar to Computers and Folders tabs.
        JCheckBox dummyCB = new JCheckBox("x");
        int dummyHeight = (int) dummyCB.getPreferredSize().getHeight();

        if (expertMode) {
            ActionLabel newFolderLink = new ActionLabel(getController(),
                getApplicationModel().getActionModel().getFolderWizardAction());
            newFolderLink.convertToBigLabel();
            JComponent newFolderLinkComponent = newFolderLink.getUIComponent();

            newFolderLinkComponent.setMinimumSize(new Dimension(
                (int) newFolderLinkComponent.getMinimumSize().getWidth(),
                dummyHeight));
            newFolderLinkComponent.setMaximumSize(new Dimension(
                (int) newFolderLinkComponent.getMaximumSize().getWidth(),
                dummyHeight));
            newFolderLinkComponent.setPreferredSize(new Dimension(
                (int) newFolderLinkComponent.getPreferredSize().getWidth(),
                dummyHeight));
            builder.add(newFolderLinkComponent, cc.xy(1, 1));
        }
        if (!getController().isBackupOnly()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            ActionLabel searchComputerLink = new ActionLabel(getController(),
                getApplicationModel().getActionModel().getFindComputersAction());
            JComponent searchComputerLinkComponent = searchComputerLink
                .getUIComponent();
            searchComputerLink.convertToBigLabel();
            searchComputerLinkComponent.setMinimumSize(new Dimension(
                (int) searchComputerLinkComponent.getMinimumSize().getWidth(),
                dummyHeight));
            searchComputerLinkComponent.setMaximumSize(new Dimension(
                (int) searchComputerLinkComponent.getMaximumSize().getWidth(),
                dummyHeight));
            searchComputerLinkComponent
                .setPreferredSize(new Dimension(
                    (int) searchComputerLinkComponent.getPreferredSize()
                        .getWidth(), dummyHeight));
            builder.add(searchComputerLinkComponent,
                cc.xy(expertMode ? 3 : 1, 1));
        }

        return builder.getPanel();
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
            if (isFiner()) {
                logFiner("Folder: " + folder.getName() + ", incoming: "
                    + folder.getStatistic().getIncomingFilesCount());
            }
        }
        filesAvailableLine.setValue(count);
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

        if (!ProUtil.isRunningProVersion()) {
            updateBuyNowLink(
                Translation
                    .getTranslation("pro.status_tab.upgrade_powerfolder"),
                true);
            return;
        }

        // Display buynow link: If is trial or about to expire or not allowed to
        // run
        boolean trial = ProUtil.isTrial(getController());
        boolean allowed = ProUtil.isAllowedToRun(getController());
        boolean aboutToExpire = daysValid != null && daysValid != -1
            && daysValid < 30;
        if (trial || !allowed) {
            updateBuyNowLink(
                Translation
                    .getTranslation("pro.status_tab.upgrade_powerfolder"),
                true);
        } else if (aboutToExpire) {
            updateBuyNowLink(
                Translation.getTranslation("pro.status_tab.renew_license"),
                true);
        }
    }

    private void updateOnlineStorageDetails() {
        updateOnlineStorageDetails(true);
    }

    /**
     * Updates the Online Storage details.
     */
    private void updateOnlineStorageDetails(boolean loginSuccess) {
        onlineStorageAccountLabel.setIcon(null);
        AccountDetails ad = client.getAccountDetails();
        boolean active = false;
        boolean showBuyNow = ProUtil.isTrial(getController());
        String username = client.getUsername();
        if (username == null || username.trim().length() == 0) {
            onlineStorageAccountLabel.setText(Translation
                .getTranslation("status_tab.online_storage.not_setup"));
            onlineStorageAccountLabel.setToolTipText(Translation
                .getTranslation("status_tab.online_storage.not_setup.tips"));
        } else {
            if (client.isPasswordEmpty()) {
                onlineStorageAccountLabel.setText(Translation
                    .getTranslation("status_tab.online_storage.no_password"));
                onlineStorageAccountLabel
                    .setToolTipText(Translation
                        .getTranslation("status_tab.online_storage.no_password.tips"));
            } else if (client.isConnected()) {
                if (client.isLoggedIn()) {
                    OnlineStorageSubscription storageSubscription = client
                        .getAccount().getOSSubscription();
                    if (storageSubscription.isDisabled()) {
                        Date expirationDate = storageSubscription
                            .getDisabledExpirationDate();
                        if (storageSubscription.isDisabledExpiration()
                            && expirationDate != null)
                        {
                            onlineStorageAccountLabel
                                .setText(Translation
                                    .getTranslation(
                                        "status_tab.online_storage.account_disabled_expiration",
                                        username,
                                        Format
                                            .formatDateCanonical(expirationDate)));
                        } else if (storageSubscription.isDisabledUsage()) {
                            onlineStorageAccountLabel
                                .setText(Translation
                                    .getTranslation(
                                        "status_tab.online_storage.account_disabled_usage",
                                        username));
                        } else {
                            onlineStorageAccountLabel
                                .setText(Translation
                                    .getTranslation(
                                        "status_tab.online_storage.account_disabled",
                                        username));
                        }
                        onlineStorageAccountLabel
                            .setToolTipText(Translation
                                .getTranslation("status_tab.online_storage.account_disabled.tips"));
                        showBuyNow = true;
                    } else {
                        if (ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED
                            .getValueBoolean(getController()))
                        {
                            onlineStorageAccountLabel.setText(Translation
                                .getTranslation(
                                    "status_tab.online_storage.account",
                                    username));
                            onlineStorageAccountLabel
                                .setToolTipText(Translation
                                    .getTranslation("status_tab.online_storage.account.tips"));
                        } else {
                            onlineStorageAccountLabel.setText(username);
                            onlineStorageAccountLabel.setToolTipText("");
                        }
                        active = true;
                        showBuyNow = !ad.getAccount().isProUser();
                    }
                } else if (loginSuccess) {
                    onlineStorageAccountLabel.setText(Translation
                        .getTranslation(
                            "status_tab.online_storage.account_connecting",
                            username));
                    onlineStorageAccountLabel
                        .setToolTipText(Translation
                            .getTranslation("status_tab.online_storage.account_connecting.tips"));
                } else {
                    onlineStorageAccountLabel.setIcon(Icons
                        .getIconById(Icons.WARNING));
                    onlineStorageAccountLabel.setText(Translation
                        .getTranslation(
                            "status_tab.online_storage.account_login_failed",
                            username));
                    onlineStorageAccountLabel
                        .setToolTipText(Translation
                            .getTranslation("status_tab.online_storage.account_login_failed.tips"));
                }
            } else {
                onlineStorageAccountLabel.setText(Translation.getTranslation(
                    "status_tab.online_storage.account_connecting", username));
                onlineStorageAccountLabel
                    .setToolTipText(Translation
                        .getTranslation("status_tab.online_storage.account_connecting.tips"));
            }
        }

        // Don't show if PowerFolder is disabled.
        onlineStorageAccountLabel.getUIComponent().setVisible(
            getController().getNodeManager().isStarted());

        if (active) {
            OnlineStorageSubscription storageSubscription = ad.getAccount()
                .getOSSubscription();
            long totalStorage = storageSubscription.getStorageSize();
            long spaceUsed = ad.getSpaceUsed();
            if (spaceUsed > (double) totalStorage * 0.8) {
                showBuyNow = true;
            }
            onlineStorageSection.getUIComponent().setVisible(true);
            onlineStorageSection.setInfo(totalStorage, spaceUsed);
        } else {
            onlineStorageSection.getUIComponent().setVisible(false);
        }

        // Show Buy now link if: Disabled OR >80% OR isTrial
        updateBuyNowLink(
            Translation.getTranslation("pro.status_tab.upgrade_powerfolder"),
            showBuyNow);
        // Make sure to display buy now if license is about to expire.
        updateLicenseDetails();
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
            updateFoldersText();
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
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            e.getFolder().removeFolderListener(folderListener);
            updateFoldersText();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateFoldersText();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateFoldersText();
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
            updateOnlineStorageDetails(event.isLoginSuccess());
        }

        public void serverConnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }
    }

    private class UseOSModelListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateOnlineStorageDetails();
        }
    }

    private class MyLicenseKeyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            updateLicenseDetails();
        }
    }

    private class MyDaysValidListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            updateLicenseDetails();
        }
    }

    private class MyNoticesListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateNewNoticesText();
        }
    }

    private class MyOverallFolderStatListener implements
            OverallFolderStatListener {
        public void statCalculated() {
            updateSyncStats();
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
            updateSyncStats();
            updateLicenseDetails();
        }
    }

    /**
     * Class to update the up/download rates.
     * <P>
     * And update the sync text.
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
            // Update general sync stats
            updateSyncStats();

            double d = uploadCounter.calculateCurrentKBS();
            if (getController().getTransferManager().countActiveUploads() == 0)
            {
                // Hide KB/s when not active uploads
                d = 0;
            }
            if (Double.compare(d, 0) == 0) {
                uploadsLine.setNormalLabelText(Translation
                    .getTranslation("status_tab.files_uploads"));
            } else {
                String s = Format.formatDecimal(d);
                uploadsLine.setNormalLabelText(Translation.getTranslation(
                    "status_tab.files_uploads_active", s));
            }
            d = downloadCounter.calculateCurrentKBS();
            if (getController().getTransferManager().countActiveDownloads() == 0)
            {
                // Hide KB/s when no active downloads
                d = 0;
            }
            if (Double.compare(d, 0) == 0) {
                downloadsLine.setNormalLabelText(Translation
                    .getTranslation("status_tab.files_downloads"));
            } else {
                String s = Format.formatDecimal(d);
                downloadsLine.setNormalLabelText(Translation.getTranslation(
                    "status_tab.files_downloads_active", s));
            }
        }
    }
}
