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
 * $Id: ExpandableFolderView.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.folders;

import static de.dal33t.powerfolder.disk.FolderStatistic.UNKNOWN_SYNC_STATUS;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.FolderRemovePermission;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.ExpandableView;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.FolderRemoveDialog;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinDialog;
import de.dal33t.powerfolder.ui.event.ExpansionEvent;
import de.dal33t.powerfolder.ui.event.ExpansionListener;
import de.dal33t.powerfolder.ui.information.folder.settings.SettingsTab;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SyncIconButtonMini;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.widget.ResizingJLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableFolderView extends PFUIComponent implements
    ExpandableView
{

    private final FolderInfo folderInfo;
    private Folder folder;
    private ExpandableFolderModel.Type type;
    private boolean online;
    private final AtomicBoolean focus = new AtomicBoolean();

    private final AtomicBoolean showing100Sync = new AtomicBoolean();

    private ResizingJLabel nameLabel;
    private JButtonMini openSettingsInformationButton;
    private JButtonMini openFilesInformationButton;
    private JButtonMini inviteButton;
    private ActionLabel membersLabel;
    private ActionLabel upperSyncPercentageLabel;

    private JPanel uiComponent;
    private JPanel borderPanel;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;
    private AtomicBoolean mouseOver;

    private ActionLabel filesLabel;
    private ActionLabel deletedFilesLabel;
    private ActionLabel transferModeLabel;
    private ActionLabel localDirectoryLabel;
    private JLabel syncPercentLabel;
    private ActionLabel syncDateLabel;
    private JLabel localSizeLabel;
    private JLabel totalSizeLabel;
//    private ActionLabel filesAvailableLabel;
    private JPanel upperPanel;
    private JButtonMini primaryButton;
    private SyncIconButtonMini upperSyncFolderButton;
    private JButtonMini lowerSyncFolderButton;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;

    // private MyServerClientListener myServerClientListener;
    private MyTransferManagerListener myTransferManagerListener;
    private MyFolderRepositoryListener myFolderRepositoryListener;
    private MyNodeManagerListener myNodeManagerListener;

    private ExpansionListener listenerSupport;

    private OnlineStorageComponent osComponent;
    private ServerClient serverClient;

    private MySyncFolderAction syncFolderAction;
    private MyOpenFilesInformationAction openFilesInformationAction;
    private MyOpenSettingsInformationAction openSettingsInformationAction;
    private MyInviteAction inviteAction;
    private MyOpenMembersInformationAction openMembersInformationAction;
    private MyMostRecentChangesAction mostRecentChangesAction;
    private MyClearCompletedDownloadsAction clearCompletedDownloadsAction;
    private MyOpenExplorerAction openExplorerAction;
    private FolderRemoveAction removeFolderLocalAction;
    private FolderRemoveAction removeFolderOnlineAction;
    private BackupOnlineStorageAction backupOnlineStorageAction;
    private StopOnlineStorageAction stopOnlineStorageAction;
    private WebdavAction webdavAction;
    private WebViewAction webViewAction;

    // Performance stuff
    private DelayedUpdater syncUpdater;
    private DelayedUpdater folderUpdater;
    private DelayedUpdater folderDetailsUpdater;

    private String webDAVURL;

    /**
     * Constructor
     * 
     * @param controller
     * @param folderInfo
     */
    public ExpandableFolderView(Controller controller, FolderInfo folderInfo) {
        super(controller);
        serverClient = controller.getOSClient();
        this.folderInfo = folderInfo;
        listenerSupport = ListenerSupportFactory
            .createListenerSupport(ExpansionListener.class);
        initComponent();
        buildUI();
    }

    /**
     * Set the folder for this view. May be null if online storage only, so
     * update visual components if null --> folder or folder --> null
     * 
     * @param folderModel
     */
    public void configure(ExpandableFolderModel folderModel) {
        boolean changed = false;
        Folder beanFolder = folderModel.getFolder();
        ExpandableFolderModel.Type beanType = folderModel.getType();
        boolean beanOnline = folderModel.isOnline();
        if (beanFolder != null && folder == null) {
            changed = true;
        } else if (beanFolder == null && folder != null) {
            changed = true;
        } else if (beanFolder != null && !folder.equals(beanFolder)) {
            changed = true;
        } else if (beanType != type) {
            changed = true;
        } else if (beanOnline ^ online) {
            changed = true;
        }

        if (!changed) {
            return;
        }

        // Something changed - change details.
        unregisterFolderListeners();

        type = beanType;
        folder = beanFolder;
        online = beanOnline;
        osComponent.setFolder(beanFolder);

        updateStatsDetails();
        updateNumberOfFiles();
        updateTransferMode();
        updateFolderMembershipDetails();
        updateIconAndOS();
        updateLocalButtons();
        updateNameLabel();
        updatePermissions();
        updateDeletedFiles();

        registerFolderListeners();
    }

    /**
     * Expand this view if collapsed.
     */
    public void expand() {
        // Only actually expand local folders in advanced mode,
        // but we still need to fire the reset to clear others' focus.
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())
            && type == ExpandableFolderModel.Type.Local)
        {
            expanded.set(true);
            updateWebDAVURL();
            upperPanel.setToolTipText(Translation
                .getTranslation("exp_folder_view.collapse"));
            updateNameLabel();
            lowerOuterPanel.setVisible(true);
        }
        listenerSupport.resetAllButSource(new ExpansionEvent(this));
    }

    /**
     * Collapse this view if expanded.
     */
    public void collapse() {
        expanded.set(false);
        updateWebDAVURL();
        upperPanel.setToolTipText(Translation
            .getTranslation("exp_folder_view.expand"));
        updateNameLabel();
        lowerOuterPanel.setVisible(false);
    }

    public void setFocus(boolean focus) {
        this.focus.set(focus);
        updateBorderPanel();
    }

    public boolean hasFocus() {
        return focus.get();
    }

    private void updateBorderPanel() {
        if (focus.get()) {
            borderPanel.setBorder(BorderFactory.createEtchedBorder());
        } else {
            borderPanel.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void updateWebDAVURL() {
        SwingWorker worker = new SwingWorker() {
            protected Object doInBackground() throws Exception {
                createWebDAVURL();
                return null;
            }
        };
        worker.execute();
    }

    private synchronized void createWebDAVURL() {
        if (!serverClient.isConnected() || !serverClient.isLoggedIn()) {
            return;
        }
        if (webDAVURL == null) {
            webDAVURL = serverClient.getFolderService()
                .getWebDAVURL(folderInfo);
            if (webDAVURL == null) {
                // Don't fetch again. It's simply not available.
                webDAVURL = "";
            }
        }

    }

    /**
     * Gets the ui component, building if required.
     * 
     * @return
     */
    public JPanel getUIComponent() {
        return uiComponent;
    }

    /**
     * Builds the ui component.
     */
    private void buildUI() {

        // Build ui
        // icon name #-files webdav open
        FormLayout upperLayout = new FormLayout(
//                "pref, 3dlu, pref:grow, 3dlu, pref, 3dlu, pref, 3dlu", "pref");
            "pref, 3dlu, pref:grow, 3dlu, pref, 3dlu", "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();
        updateIconAndOS();

        // Primary and upperSyncFolder buttons share the same slot.
        upperBuilder.add(primaryButton, cc.xy(1, 1));
        upperBuilder.add(upperSyncFolderButton, cc.xy(1, 1));

        MouseAdapter mca = new MyMouseClickAdapter();
        MouseAdapter moa = new MyMouseOverAdapter();
        nameLabel = new ResizingJLabel();
        upperBuilder.add(nameLabel, cc.xy(3, 1));
        nameLabel.addMouseListener(moa);
        nameLabel.addMouseListener(mca); // Because this is the biggest blank
                                         // area where the user might click.
        upperBuilder.add(upperSyncPercentageLabel.getUIComponent(),
                cc.xy(5, 1));
//        upperBuilder.add(filesAvailableLabel.getUIComponent(), cc.xy(7, 1));
//        filesAvailableLabel.getUIComponent().addMouseListener(moa);

        upperPanel = upperBuilder.getPanel();
        upperPanel.setOpaque(false);
        if (type == ExpandableFolderModel.Type.Local) {
            upperPanel.setToolTipText(Translation
                .getTranslation("exp_folder_view.expand"));
        }
        CursorUtils.setHandCursor(upperPanel);
        upperPanel.addMouseListener(moa);
        upperPanel.addMouseListener(mca);

        // Build lower detials with line border.
        FormLayout lowerLayout;
        if (getController().isBackupOnly()) {
            // Skip computers stuff
            lowerLayout = new FormLayout(
                "3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref");
        } else {
            lowerLayout = new FormLayout(
                "3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref");
        }
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        int row = 1;

        lowerBuilder.addSeparator(null, cc.xywh(1, row, 6, 1));

        row += 2;

        lowerBuilder.add(syncDateLabel.getUIComponent(), cc.xy(2, row));
        lowerBuilder.add(lowerSyncFolderButton, cc.xy(5, row));

        row += 2;

        lowerBuilder.add(syncPercentLabel, cc.xy(2, row));
        lowerBuilder.add(openFilesInformationButton, cc.xy(5, row));

        row += 2;

        lowerBuilder.add(filesLabel.getUIComponent(), cc.xy(2, row));

        row += 2;

        lowerBuilder.add(localSizeLabel, cc.xy(2, row));

        row += 2;

        lowerBuilder.add(totalSizeLabel, cc.xy(2, row));

        row += 2;

        lowerBuilder.add(deletedFilesLabel.getUIComponent(), cc.xy(2, row));

        row += 2;

        lowerBuilder.addSeparator(null, cc.xywh(2, row, 4, 1));

        row += 2;

        // No computers stuff if backup mode.
        if (getController().isBackupOnly()) {
            lowerBuilder.add(transferModeLabel.getUIComponent(), cc.xy(2, row));
            lowerBuilder.add(openSettingsInformationButton, cc.xy(5, row));

            row += 2;

            lowerBuilder.add(localDirectoryLabel.getUIComponent(),
                cc.xy(2, row));

        } else {
            lowerBuilder.add(membersLabel.getUIComponent(), cc.xy(2, row));
            lowerBuilder.add(inviteButton, cc.xy(5, row));

            row += 2;

            lowerBuilder.addSeparator(null, cc.xywh(2, row, 4, 1));

            row += 2;

            lowerBuilder.add(transferModeLabel.getUIComponent(), cc.xy(2, row));
            lowerBuilder.add(openSettingsInformationButton, cc.xy(5, row));

            row += 2;

            lowerBuilder.add(localDirectoryLabel.getUIComponent(),
                cc.xy(2, row));

        }

        row++; // Just add one.

        lowerBuilder.add(osComponent.getUIComponent(), cc.xywh(2, row, 4, 1));

        JPanel lowerPanel = lowerBuilder.getPanel();
        lowerPanel.setOpaque(false);

        // Build spacer then lower outer with lower panel
        FormLayout lowerOuterLayout = new FormLayout("pref:grow", "3dlu, pref");
        PanelBuilder lowerOuterBuilder = new PanelBuilder(lowerOuterLayout);
        lowerOuterPanel = lowerOuterBuilder.getPanel();
        lowerOuterPanel.setVisible(false);
        lowerOuterBuilder.add(lowerPanel, cc.xy(1, 2));

        // Build border around upper and lower
        FormLayout borderLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, pref, 3dlu");
        PanelBuilder borderBuilder = new PanelBuilder(borderLayout);
        borderBuilder.add(upperPanel, cc.xy(2, 2));
        JPanel panel = lowerOuterBuilder.getPanel();
        panel.setOpaque(false);
        borderBuilder.add(panel, cc.xy(2, 3));
        borderPanel = borderBuilder.getPanel();
        borderPanel.setOpaque(false);

        // Build ui with vertical space before the next one
        FormLayout outerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 3dlu");
        PanelBuilder outerBuilder = new PanelBuilder(outerLayout);
        outerBuilder.add(borderPanel, cc.xy(2, 1));

        uiComponent = outerBuilder.getPanel();
        uiComponent.setOpaque(false);
    }

    /**
     * Initializes the components.
     */
    private void initComponent() {

        syncUpdater = new DelayedUpdater(getController(), 1000L);
        folderDetailsUpdater = new DelayedUpdater(getController());
        folderUpdater = new DelayedUpdater(getController());

        openFilesInformationAction = new MyOpenFilesInformationAction(
            getController());
        inviteAction = new MyInviteAction(getController());
        MyOpenFilesUnsyncedAction openFilesUnsyncedAction =
                new MyOpenFilesUnsyncedAction(getController());
        openSettingsInformationAction = new MyOpenSettingsInformationAction(
            getController());
        openSettingsInformationAction.setEnabled(!getController()
            .isBackupOnly());
        MyMoveLocalFolderAction moveLocalFolderAction =
                new MyMoveLocalFolderAction(getController());
        moveLocalFolderAction.setEnabled(!getController().isBackupOnly());
        openMembersInformationAction = new MyOpenMembersInformationAction(
            getController());
        mostRecentChangesAction = new MyMostRecentChangesAction(getController());
        clearCompletedDownloadsAction = new MyClearCompletedDownloadsAction(
            getController());
        openExplorerAction = new MyOpenExplorerAction(getController());
        
        // Allow to stop local sync even if no folder remove permissions was given.
        removeFolderLocalAction = new FolderRemoveAction(getController());
        
        // Don't allow to choose action at all if online folder only.
        removeFolderOnlineAction = new FolderRemoveAction(getController());
        removeFolderOnlineAction.allowWith(FolderRemovePermission.INSTANCE);

        backupOnlineStorageAction = new BackupOnlineStorageAction(
            getController());
        stopOnlineStorageAction = new StopOnlineStorageAction(getController());

        syncFolderAction = new MySyncFolderAction(getController());

        webdavAction = new WebdavAction(getController());
        webViewAction = new WebViewAction(getController());

        expanded = new AtomicBoolean();
        mouseOver = new AtomicBoolean();

        osComponent = new OnlineStorageComponent(getController(), folder);

        primaryButton = new JButtonMini(Icons.getIconById(Icons.BLANK), "");
        primaryButton.addActionListener(new PrimaryButtonActionListener());
        openSettingsInformationButton = new JButtonMini(
            openSettingsInformationAction);

        upperSyncPercentageLabel = new ActionLabel(getController(),
                new MyOpenFilesUnsyncedAction(getController()));
        openFilesInformationButton = new JButtonMini(openFilesInformationAction);

        inviteButton = new JButtonMini(inviteAction);

        upperSyncFolderButton = new SyncIconButtonMini(getController());
        upperSyncFolderButton
            .addActionListener(new PrimaryButtonActionListener());
        upperSyncFolderButton.setVisible(false);

        Icon pIcon = Icons.getIconById(Icons.SYNC_COMPLETE);
        Icon sIcon = Icons.getIconById(Icons.SYNC_ANIMATION[0]);
        if (pIcon.getIconHeight() > sIcon.getIconHeight()) {
            // HACK(tm) when mixing 16x16 sync icon with 24x24 icons
            upperSyncFolderButton.setBorder(Borders
                .createEmptyBorder("6, 6, 6, 6"));
        }

        lowerSyncFolderButton = new JButtonMini(syncFolderAction);

        filesLabel = new ActionLabel(getController(),
            openFilesInformationAction);
        transferModeLabel = new ActionLabel(getController(),
            openSettingsInformationAction);
        localDirectoryLabel = new ActionLabel(getController(),
            moveLocalFolderAction);
        syncPercentLabel = new JLabel();
        syncDateLabel = new ActionLabel(getController(),
            mostRecentChangesAction);
        localSizeLabel = new JLabel();
        totalSizeLabel = new JLabel();
        membersLabel = new ActionLabel(getController(),
            openMembersInformationAction);
//        filesAvailableLabel = new ActionLabel(getController(),
//            new MyFilesAvailableAction());
        deletedFilesLabel = new ActionLabel(getController(),
            new MyDeletedFilesAction());
        updateNumberOfFiles();
        updateStatsDetails();
        updateFolderMembershipDetails();
        updateTransferMode();
        updateLocalButtons();
        updateIconAndOS();
        updatePermissions();

        registerListeners();
    }

    private void updatePermissions() {
        if (getController().isBackupOnly()) {
            backupOnlineStorageAction.setEnabled(false);
            stopOnlineStorageAction.setEnabled(false);
            inviteAction.setEnabled(false);
            return;
        }
        // Update permissions
        Permission folderAdmin = FolderPermission.admin(folderInfo);
        backupOnlineStorageAction.allowWith(folderAdmin);
        stopOnlineStorageAction.allowWith(folderAdmin);
        inviteAction.allowWith(folderAdmin);
    }

    private void updateLocalButtons() {
        boolean enabled = type == ExpandableFolderModel.Type.Local;

        openSettingsInformationButton.setEnabled(enabled
            && !getController().isBackupOnly());
        transferModeLabel
            .setEnabled(enabled && !getController().isBackupOnly());
        localDirectoryLabel.setEnabled(enabled
            && !getController().isBackupOnly());
        openSettingsInformationAction.setEnabled(enabled
            && !getController().isBackupOnly());

        openFilesInformationButton.setEnabled(enabled);
        openFilesInformationAction.setEnabled(enabled);

        inviteButton.setEnabled(enabled && !getController().isBackupOnly());
        inviteAction.setEnabled(enabled && !getController().isBackupOnly());

        syncDateLabel.setEnabled(enabled);
        mostRecentChangesAction.setEnabled(enabled);

        membersLabel.setEnabled(enabled);
        openMembersInformationAction.setEnabled(enabled
            && !getController().isBackupOnly());

        openExplorerAction.setEnabled(enabled && Desktop.isDesktopSupported());

        // Controlled by permission system.
        // removeFolderAction.setEnabled(true);

        updateSyncButton();
    }

    private void updateSyncButton() {
        if (type != ExpandableFolderModel.Type.Local) {
            upperSyncFolderButton.setVisible(false);
            upperSyncFolderButton.spin(false);
            primaryButton.setVisible(true);
            return;
        }

        // Do Local updates later.
        syncUpdater.schedule(new Runnable() {
            public void run() {
                if (folder == null) {
                    return;
                }
                if (folder.isSyncing()) {
                    primaryButton.setVisible(false);
                    upperSyncFolderButton.setVisible(true);
                    upperSyncFolderButton.spin(true);
                } else {
                    primaryButton.setVisible(true);
                    upperSyncFolderButton.setVisible(false);
                    upperSyncFolderButton.spin(false);
                }
            }
        });
    }

    private void registerListeners() {
        // myServerClientListener = new MyServerClientListener();
        // getController().getOSClient().addListener(myServerClientListener);
        //
        myNodeManagerListener = new MyNodeManagerListener();
        getController().getNodeManager().addNodeManagerListener(
            myNodeManagerListener);

        myTransferManagerListener = new MyTransferManagerListener();
        getController().getTransferManager().addListener(
            myTransferManagerListener);

        myFolderRepositoryListener = new MyFolderRepositoryListener();
        getController().getFolderRepository().addFolderRepositoryListener(
            myFolderRepositoryListener);
    }

    /**
     * Call if this object is being discarded, so that listeners are not
     * orphaned.
     */
    public void unregisterListeners() {
        if (myNodeManagerListener != null) {
            getController().getNodeManager().removeNodeManagerListener(
                myNodeManagerListener);
            myNodeManagerListener = null;
        }
        if (myTransferManagerListener != null) {
            getController().getTransferManager().removeListener(
                myTransferManagerListener);
            myTransferManagerListener = null;
        }
        if (myFolderRepositoryListener != null) {
            getController().getFolderRepository()
                .removeFolderRepositoryListener(myFolderRepositoryListener);
            myFolderRepositoryListener = null;
        }
        unregisterFolderListeners();
    }

    /**
     * Register listeners of the folder.
     */
    private void registerFolderListeners() {
        if (folder != null) {
            myFolderListener = new MyFolderListener();
            folder.addFolderListener(myFolderListener);

            myFolderMembershipListener = new MyFolderMembershipListener();
            folder.addMembershipListener(myFolderMembershipListener);
        }
    }

    /**
     * Unregister listeners of the folder.
     */
    private void unregisterFolderListeners() {
        if (folder != null) {
            if (myFolderListener != null) {
                folder.removeFolderListener(myFolderListener);
                myFolderListener = null;
            }
            if (myFolderMembershipListener != null) {
                folder.removeMembershipListener(myFolderMembershipListener);
                myFolderMembershipListener = null;
            }
        }
    }

    /**
     * @return the Info of the associated folder.
     */
    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    /**
     * Updates the statistics details of the folder.
     */
    private void updateStatsDetails() {

        String syncPercentText;
        String syncPercentTip = null;
        String syncDateText;
        String localSizeString;
        String totalSizeString;
        String filesAvailableLabelText;
        if (type == ExpandableFolderModel.Type.Local) {

            Date lastSyncDate = folder.getLastSyncDate();

            if (lastSyncDate == null) {
                syncDateText = Translation
                    .getTranslation("exp_folder_view.never_synchronized");
            } else {
                String formattedDate = Format.formatDateShort(lastSyncDate);
                syncDateText = Translation.getTranslation(
                    "exp_folder_view.last_synchronized", formattedDate);
            }

            if (folder.hasOwnDatabase()) {
                FolderStatistic statistic = folder.getStatistic();
                double sync = statistic.getHarmonizedSyncPercentage();
                if (sync < UNKNOWN_SYNC_STATUS) {
                    sync = UNKNOWN_SYNC_STATUS;
                }
                if (sync > 100) {
                    sync = 100;
                }

                // Sync in progress? Rewrite date as estimate.
                if (Double.compare(sync, 100.0) < 0
                    && Double.compare(sync, UNKNOWN_SYNC_STATUS) > 0)
                {
                    Date date = folder.getStatistic().getEstimatedSyncDate();
                    if (date != null) {
                        //If ETA sync > 2 days show text: "Estimated sync: Unknown"
                        //If ETA sync > 20 hours show text: "Estimated sync: in X days"
                        //If ETA sync > 45 minutes show text: "Estimated sync: in X hours"
                        //If ETA sync < 45 minutes show text: "Estimated sync: in X minutes"
                        if (DateUtil.isDateMoreThanNDaysInFuture(date, 2)) {
                            syncDateText = Translation.getTranslation(
                                    "main_frame.sync_eta_unknown");
                        } else if (DateUtil.isDateMoreThanNHoursInFuture(date,
                                20)) {
                            int days = DateUtil.getDaysInFuture(date);
                            if (days <= 1) {
                                syncDateText = Translation.getTranslation(
                                        "exp_folder_view.sync_eta_one_day");
                            } else {
                                syncDateText = Translation.getTranslation(
                                        "exp_folder_view.sync_eta_days",
                                        String.valueOf(days));
                            }
                        } else if (DateUtil.isDateMoreThanNMinutesInFuture(date,
                                45)) {
                            int hours = DateUtil.getDaysInFuture(date);
                            if (hours <= 1) {
                            syncDateText = Translation.getTranslation(
                                    "exp_folder_view.sync_eta_one_hour");
                            } else {
                                syncDateText = Translation.getTranslation(
                                        "exp_folder_view.sync_eta_hours",
                                        String.valueOf(hours));
                            }
                        } else {
                            int minutes = DateUtil.getHoursInFuture(date);
                            if (minutes <= 1) {
                                syncDateText = Translation.getTranslation(
                                        "exp_folder_view.sync_eta_one_minute");
                            } else {
                                syncDateText = Translation.getTranslation(
                                        "exp_folder_view.sync_eta_minutes",
                                        String.valueOf(minutes));

                            }
                        }
                    }
                }

                if (lastSyncDate == null
                    && (Double.compare(sync, 100.0) == 0 || Double.compare(
                        sync, UNKNOWN_SYNC_STATUS) == 0))
                {
                    // Never synced with others.
                    syncPercentText = Translation
                        .getTranslation("exp_folder_view.unsynchronized");
                    showing100Sync.set(false);
                } else {
                    showing100Sync.set(Double.compare(sync, 100) == 0);
                    if (Double.compare(sync, UNKNOWN_SYNC_STATUS) == 0) {
                        if (folder.getConnectedMembersCount() >= 1) {
                            syncPercentText = Translation
                                .getTranslation("exp_folder_view.unsynchronized");
                            syncPercentTip = Translation
                                .getTranslation("exp_folder_view.unsynchronized.tip");
                        } else {
                            syncPercentText = "";
                            syncPercentTip = "";
                        }
                    } else {
                        syncPercentText = Translation.getTranslation(
                            "exp_folder_view.synchronized",
                            Format.formatDecimal(sync));
                    }
                }

                if (lastSyncDate != null && Double.compare(sync, 100.0) == 0) {
                    // 100% sync - remove any sync problem.
                    folder.checkSync();
                }

                long localSize = statistic.getLocalSize();
                localSizeString = Format.formatBytesShort(localSize);

                long totalSize = statistic.getTotalSize();
                totalSizeString = Format.formatBytesShort(totalSize);

                int count = statistic.getIncomingFilesCount();
                if (count == 0) {
                    filesAvailableLabelText = "";
                } else {
                    filesAvailableLabelText = Translation.getTranslation(
                        "exp_folder_view.files_available",
                        String.valueOf(count));
                }
                if (sync >= 0 && sync < 100) {
                    upperSyncPercentageLabel
                        .setText(Format.formatDecimal(sync) + '%');
                } else {
                    upperSyncPercentageLabel.setText("");
                }
            } else {
                upperSyncPercentageLabel.setText("");
                showing100Sync.set(false);
                syncPercentText = Translation
                    .getTranslation("exp_folder_view.not_yet_scanned");
                localSizeString = "?";
                totalSizeString = "?";
                filesAvailableLabelText = "";
            }
        } else {
            upperSyncPercentageLabel.setText("");
            syncPercentText = Translation.getTranslation(
                "exp_folder_view.synchronized", "?");
            syncDateText = Translation.getTranslation(
                "exp_folder_view.last_synchronized", "?");
            localSizeString = "?";
            totalSizeString = "?";
            filesAvailableLabelText = "";
        }

        syncPercentLabel.setText(syncPercentText);
        syncPercentLabel.setToolTipText(syncPercentTip);
        syncDateLabel.setText(syncDateText);
        localSizeLabel.setText(Translation.getTranslation(
            "exp_folder_view.local", localSizeString));
        totalSizeLabel.setText(Translation.getTranslation(
            "exp_folder_view.total", totalSizeString));
//        filesAvailableLabel.setText(filesAvailableLabelText);
//        if (filesAvailableLabelText.length() == 0) {
//            filesAvailableLabel.setToolTipText(null);
//        } else {
//            filesAvailableLabel.setToolTipText(Translation
//                .getTranslation("exp_folder_view.files_available_tip"));
//        }
        // Maybe change visibility of upperSyncLink.
        updateWebDAVURL();
    }

    /**
     * Updates the number of files details of the folder.
     */
    private void updateNumberOfFiles() {
        String filesText;
        if (type == ExpandableFolderModel.Type.Local) {
            // FIXME: Returns # of files + # of directories
            filesText = Translation.getTranslation("exp_folder_view.files",
                String.valueOf(folder.getStatistic().getLocalFilesCount()));
        } else {
            filesText = Translation
                .getTranslation("exp_folder_view.files", "?");
        }
        filesLabel.setText(filesText);
    }

    private void updateDeletedFiles() {
        String deletedFileText;
        if (type == ExpandableFolderModel.Type.Local) {
            Collection<FileInfo> allFiles = folder.getDAO().findAllFiles(
                getController().getMySelf().getId());
            int deletedCount = 0;
            for (FileInfo file : allFiles) {
                if (file.isDeleted()) {
                    deletedCount++;
                }
            }
            deletedFileText = Translation.getTranslation(
                "exp_folder_view.deleted_files", String.valueOf(deletedCount));
        } else {
            deletedFileText = Translation.getTranslation(
                "exp_folder_view.deleted_files", "?");
        }
        deletedFilesLabel.setText(deletedFileText);
    }

    /**
     * Updates transfer mode of the folder.
     */
    private void updateTransferMode() {
        String transferMode;
        if (type == ExpandableFolderModel.Type.Local) {
            transferMode = Translation.getTranslation(
                "exp_folder_view.transfer_mode", folder.getSyncProfile()
                    .getName());
            String path = folder.getCommitOrLocalDir().getAbsolutePath();
            if (path.length() >= 35) {
                path = path.substring(0, 15) + "..."
                    + path.substring(path.length() - 15, path.length());
            }
            localDirectoryLabel.setVisible(true);
            localDirectoryLabel.setText(path);
        } else {
            transferMode = Translation.getTranslation(
                "exp_folder_view.transfer_mode", "?");
            localDirectoryLabel.setVisible(false);
        }
        transferModeLabel.setText(transferMode);
    }

    /**
     * Updates the folder member details.
     */
    private void updateFolderMembershipDetails() {
        folderDetailsUpdater.schedule(new Runnable() {
            public void run() {
                updateFolderMembershipDetails0();
            }
        });
    }

    /**
     * Updates the folder member details.
     */
    private void updateFolderMembershipDetails0() {
        String countText;
        String connectedCountText;
        if (type == ExpandableFolderModel.Type.Local) {
            countText = String.valueOf(folder.getMembersCount());
            // And me!
            connectedCountText = String.valueOf(folder
                .getConnectedMembersCount() + 1);
        } else {
            countText = "?";
            connectedCountText = "?";
        }
        membersLabel.setText(Translation.getTranslation(
            "exp_folder_view.members", countText, connectedCountText));
    }

    /**
     * Gets called externally to update the display of problems.
     */
    public void updateIconAndOS() {
        boolean osComponentVisible = getController().getOSClient()
            .isBackupByDefault() && !getController().isBackupOnly();
        if (type == ExpandableFolderModel.Type.Local) {

            double sync = folder.getStatistic().getHarmonizedSyncPercentage();
            if (folder != null && folder.countProblems() > 0) {
                // Got a problem.
                primaryButton.setIcon(Icons.getIconById(Icons.PROBLEMS));
                primaryButton.setToolTipText(Translation
                    .getTranslation("exp_folder_view.folder_problem_text"));
            } else if (folder != null && folder.isPreviewOnly()) {
                // It's a preview.
                primaryButton.setIcon(Icons.getIconById(Icons.PREVIEW_FOLDER));
                primaryButton.setToolTipText(Translation
                    .getTranslation("exp_folder_view.folder_preview_text"));
            } else if (getController().isPaused() && sync < 100.0d
                && sync != FolderStatistic.UNKNOWN_SYNC_STATUS)
            {
                // Sync is in pause
                primaryButton.setIcon(Icons.getIconById(Icons.PAUSE));
                primaryButton.setToolTipText(Translation
                    .getTranslation("exp_folder_view.folder_sync_paused"));
            } else {
                // We are in sync.
                primaryButton.setIcon(Icons.getIconById(Icons.SYNC_COMPLETE));
                primaryButton.setToolTipText(Translation
                    .getTranslation("exp_folder_view.folder_sync_complete"));
            }
            // } else if (online) {
            // // We are local and online.
            // primaryButton.setIcon(Icons
            // .getIconById(Icons.LOCAL_AND_ONLINE_FOLDER));
            // primaryButton
            // .setToolTipText(Translation
            // .getTranslation("exp_folder_view.folder_local_online_text"));
            // osComponent.getUIComponent().setVisible(osComponentVisible);
            // } else {
            // // Just a local folder.
            // primaryButton.setIcon(Icons.getIconById(Icons.LOCAL_FOLDER));
            // primaryButton.setToolTipText(Translation
            // .getTranslation("exp_folder_view.folder_local_text"));
            // }
        } else if (type == ExpandableFolderModel.Type.Typical) {
            primaryButton.setIcon(Icons.getIconById(Icons.TYPICAL_FOLDER));
            primaryButton.setToolTipText(Translation
                .getTranslation("exp_folder_view.folder_typical_text"));
            osComponent.getUIComponent().setVisible(false);
        } else { // CloudOnly
            primaryButton.setIcon(Icons.getIconById(Icons.ONLINE_FOLDER));
            primaryButton.setToolTipText(Translation
                .getTranslation("exp_folder_view.folder_online_text"));
            osComponent.getUIComponent().setVisible(osComponentVisible);
        }

        if (folder != null && folder.isPreviewOnly()) {
            osComponent.getUIComponent().setVisible(false);
        } else {
            osComponent.getUIComponent().setVisible(osComponentVisible);
            if (osComponentVisible) {
                double sync = 0;
                if (folder != null) {
                    sync = folder.getStatistic().getServerSyncPercentage();
                }
                boolean warned = serverClient.getAccountDetails().getAccount()
                    .getOSSubscription().isDisabledUsage();
                boolean joined = folder != null
                    && serverClient.joinedByCloud(folder);
                osComponent.setSyncPercentage(sync, warned, joined);
            }
        }
    }

    public void addExpansionListener(ExpansionListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeExpansionListener(ExpansionListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    /**
     * Is the view expanded?
     * 
     * @return
     */
    public boolean isExpanded() {
        return expanded.get();
    }

    public JPopupMenu createPopupMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        if (type == ExpandableFolderModel.Type.CloudOnly) {
            // Cloud-only folder popup
            createWebDAVURL();
            if (StringUtils.isNotBlank(webDAVURL)) {
                if (serverClient.supportsWebDAV() && OSUtil.isWindowsSystem()) {
                    contextMenu.add(webdavAction).setIcon(null);
                }
                if (serverClient.supportsWebLogin()) {
                    contextMenu.add(webViewAction).setIcon(null);
                }
            }
            contextMenu.add(removeFolderOnlineAction).setIcon(null);
        } else {
            // Local folder popup
            contextMenu.add(openExplorerAction).setIcon(null);
            contextMenu.addSeparator();
            boolean expert = PreferencesEntry.EXPERT_MODE
                .getValueBoolean(getController());
            if (expert) {
                contextMenu.add(syncFolderAction).setIcon(null);
                contextMenu.add(openFilesInformationAction).setIcon(null);
                contextMenu.add(mostRecentChangesAction).setIcon(null);
                contextMenu.add(clearCompletedDownloadsAction).setIcon(null);
            }
            if (!getController().isBackupOnly()) {
                contextMenu.addSeparator();
                contextMenu.add(inviteAction).setIcon(null);
                if (expert) {
                    contextMenu.add(openMembersInformationAction).setIcon(null);
                }
            }
            contextMenu.addSeparator();
            contextMenu.add(openSettingsInformationAction).setIcon(null);
            contextMenu.add(removeFolderLocalAction).setIcon(null);
            if (expert && serverClient.isConnected()
                && serverClient.isLoggedIn())
            {
                boolean osConfigured = serverClient.joinedByCloud(folder);
                if (osConfigured) {
                    contextMenu.add(stopOnlineStorageAction).setIcon(null);
                } else {
                    contextMenu.add(backupOnlineStorageAction).setIcon(null);
                }
            }
        }
        return contextMenu;
    }

    private void openExplorer() {
        FileUtils.openFile(folder.getCommitOrLocalDir());
    }

    /**
     * Downloads added or removed for this folder. Recalculate new files status.
     * Or if expanded / collapsed - might need to change tool tip.
     */
    public void updateNameLabel() {

        boolean newFiles = false;
        String newCountString = "";

        if (folder != null) {
            int newCount = getController().getTransferManager()
                .countCompletedDownloads(folder);
            newFiles = newCount > 0;
            if (newFiles) {
                newCountString = " (" + newCount + ')';
                nameLabel.setToolTipText(Translation.getTranslation(
                    "exp_folder_view.new_files_tip_text",
                    String.valueOf(newCount)));
            }
        }

        if (!newFiles && folder != null) {
            if (expanded.get()) {
                nameLabel.setToolTipText(Translation
                    .getTranslation("exp_folder_view.collapse"));
            } else {
                nameLabel.setToolTipText(Translation
                    .getTranslation("exp_folder_view.expand"));
            }
        }

        nameLabel.setText(folderInfo.name + newCountString);
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), newFiles
            ? Font.BOLD
            : Font.PLAIN, nameLabel.getFont().getSize()));
        clearCompletedDownloadsAction.setEnabled(newFiles);
    }

    /**
     * Create a WebDAV connection to this folder. Should be something like 'net
     * use * "https://access.powerfolder.com/node/os004/webdav/afolder"
     * /User:bob@powerfolder.com pazzword'
     */
    private void createWebdavConnection() {
        ActivityVisualizationWorker worker = new ActivityVisualizationWorker(
            getUIController())
        {
            protected String getTitle() {
                return Translation
                    .getTranslation("exp_folder_view.webdav_title");
            }

            protected String getWorkingText() {
                return Translation
                    .getTranslation("exp_folder_view.webdav_working_text");
            }

            public Object construct() throws Throwable {
                try {
                    createWebDAVURL();
                    Process process = Runtime.getRuntime().exec(
                        "net use * \"" + webDAVURL + "\" /User:"
                            + serverClient.getUsername() + ' '
                            + serverClient.getPasswordClearText());
                    byte[] out = StreamUtils.readIntoByteArray(process
                        .getInputStream());
                    String output = new String(out);
                    byte[] err = StreamUtils.readIntoByteArray(process
                        .getErrorStream());
                    String error = new String(err);
                    if (StringUtils.isEmpty(error)) {
                        if (!StringUtils.isEmpty(output)) {
                            // Looks like the link succeeded :-)
                            return 'Y' + output;
                        }
                    } else {
                        // Looks like the link failed :-(
                        return 'N' + error;
                    }
                } catch (Exception e) {
                    // Looks like the link failed, badly :-(
                    logSevere(e.getMessage(), e);
                    return 'N' + e.getMessage();
                }
                // Huh?
                return null;
            }

            public void finished() {

                // See what happened.
                String result = (String) get();
                if (result != null) {
                    if (result.startsWith("Y")) {
                        String[] parts = result.substring(1).split("\\s");
                        for (final String part : parts) {
                            if (part.length() == 2 && part.charAt(1) == ':') {
                                // Probably the new drive name, so open it.
                                getController().getIOProvider().startIO(
                                    new Runnable() {
                                        public void run() {
                                            FileUtils.openFile(new File(part));
                                        }
                                    });

                                break;
                            }
                        }
                    } else if (result.startsWith("N")) {
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("exp_folder_view.webdav_failure_title"),
                                Translation.getTranslation(
                                    "exp_folder_view.webdav_failure_text",
                                    result.substring(1)),
                                GenericDialogType.ERROR);
                    }
                }
            }
        };
        worker.start();
    }

    /**
     * See if user wants to create this typical folder.
     */
    private void askToCreateFolder() {
        if (type != ExpandableFolderModel.Type.Typical) {
            logSevere("Folder " + folderInfo.getName() + " is not Typical");
            return;
        }
        PFWizard.openTypicalFolderJoinWizard(getController(), folderInfo);
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyNodeManagerListener extends NodeManagerAdapter {
        private void updateIfRequired(NodeManagerEvent e) {
            if (folder != null && folder.hasMember(e.getNode())) {
                updateFolderMembershipDetails();
                doFolderChanges(folder);
            }
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateIfRequired(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateIfRequired(e);
        }

        public void friendAdded(NodeManagerEvent e) {
            updateIfRequired(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateIfRequired(e);
        }

        @Override
        public void settingsChanged(NodeManagerEvent e) {
            updateIfRequired(e);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private void doFolderChanges(Folder eventFolder) {
        if (folder == null || folder.equals(eventFolder)) {
            folderUpdater.schedule(new Runnable() {
                public void run() {
                    updateNumberOfFiles();
                    updateDeletedFiles();
                    updateStatsDetails();
                    updateIconAndOS();
                    updateLocalButtons();
                    updateTransferMode();
                    updatePermissions();
                }
            });
        }
    }

    /**
     * Class to respond to folder events.
     */
    private class MyFolderListener implements FolderListener {

        public void statisticsCalculated(FolderEvent folderEvent) {
            doFolderChanges(folderEvent.getFolder());
        }

        public void fileChanged(FolderEvent folderEvent) {
            doFolderChanges(folderEvent.getFolder());
        }

        public void filesDeleted(FolderEvent folderEvent) {
            doFolderChanges(folderEvent.getFolder());
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            if (folderEvent.getMember().hasCompleteFileListFor(
                folderEvent.getFolder().getInfo()))
            {
                doFolderChanges(folderEvent.getFolder());
            }
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            doFolderChanges(folderEvent.getFolder());
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            doFolderChanges(folderEvent.getFolder());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Class to respond to folder membership events.
     */
    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
            doFolderChanges(folder);
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
            doFolderChanges(folder);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        private void updateIfRequired(FolderRepositoryEvent e) {
            if (folder == null || !folder.equals(e.getFolder())) {
                return;
            }
            updateSyncButton();
            updateIconAndOS();
        }

        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateIfRequired(e);
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateIfRequired(e);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MyTransferManagerListener extends TransferManagerAdapter {

        private void updateIfRequired(TransferManagerEvent event) {
            if (folder == null
                || !folderInfo.equals(event.getFile().getFolderInfo()))
            {
                return;
            }
            updateSyncButton();
            updateIconAndOS();
        }

        @Override
        public void downloadAborted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void downloadBroken(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void downloadCompleted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void downloadQueued(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void downloadRequested(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void downloadStarted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void uploadAborted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void uploadBroken(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void uploadCompleted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void uploadRequested(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        @Override
        public void uploadStarted(TransferManagerEvent event) {
            updateIfRequired(event);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    /** Hover over any component in the upper panel should expand / collapse. */
    private class MyMouseOverAdapter extends MouseAdapter {

        // Auto expand if user hovers for two seconds.
        public void mouseEntered(MouseEvent e) {
            mouseOver.set(true);
            if (PreferencesEntry.AUTO_EXPAND.getValueBoolean(getController())) {
                if (!expanded.get()) {
                    getController().schedule(new TimerTask() {
                        public void run() {
                            if (mouseOver.get()) {
                                if (!expanded.get()) {
                                    expand();
                                    PreferencesEntry.AUTO_EXPAND.setValue(
                                        getController(), Boolean.FALSE);
                                }
                            }
                        }
                    }, 2000);
                }
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateWebDAVURL();
                }
            });
        }

        public void mouseExited(MouseEvent e) {
            mouseOver.set(false);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateWebDAVURL();
                }
            });
        }
    }

    /** Click on the upper panel should expand or display context menu */
    private class MyMouseClickAdapter extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            Cursor c = CursorUtils.setWaitCursor(upperPanel);
            try {
                createPopupMenu().show(evt.getComponent(), evt.getX(),
                    evt.getY());
            } finally {
                CursorUtils.returnToOriginal(upperPanel, c);
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                setFocus(true);
                if (expanded.get()) {
                    collapse();
                } else {
                    expand();
                    if (type == ExpandableFolderModel.Type.Local) {
                        getController().getUIController().openFilesInformation(
                            folderInfo);
                    }
                    if (type == ExpandableFolderModel.Type.CloudOnly
                        && folderInfo != null)
                    {
                        PFWizard.openOnlineStorageJoinWizard(getController(),
                            Collections.singletonList(folderInfo));
                    }
                    if (type == ExpandableFolderModel.Type.Typical) {
                        askToCreateFolder();
                    }
                }
            }
        }
    }

    // Action to invite friend.
    private class MyInviteAction extends BaseAction {

        private MyInviteAction(Controller controller) {
            super("action_invite_friend", controller);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard.openSendInvitationWizard(getController(), folderInfo);
        }
    }

    private class MyOpenSettingsInformationAction extends BaseAction {
        private MyOpenSettingsInformationAction(Controller controller) {
            super("action_open_settings_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openSettingsInformation(
                folderInfo);
        }
    }

    private class MyMoveLocalFolderAction extends BaseAction {
        private MyMoveLocalFolderAction(Controller controller) {
            super("action_move_local_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().moveLocalFolder(folderInfo);
        }
    }

    private class MyOpenFilesInformationAction extends BaseAction {

        MyOpenFilesInformationAction(Controller controller) {
            super("action_open_files_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformation(folderInfo);
        }
    }

    private class MyOpenFilesUnsyncedAction extends BaseAction {

        MyOpenFilesUnsyncedAction(Controller controller) {
            super("action_open_files_unsynced", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformationUnsynced(
                    folderInfo);
        }
    }

    private class MyOpenMembersInformationAction extends BaseAction {

        MyOpenMembersInformationAction(Controller controller) {
            super("action_open_members_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController()
                .openMembersInformation(folderInfo);
        }
    }

    private class MySyncFolderAction extends BaseAction {

        private MySyncFolderAction(Controller controller) {
            super("action_sync_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (folder.isPreviewOnly()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        PreviewToJoinDialog panel = new PreviewToJoinDialog(
                            getController(), folder);
                        panel.open();
                    }
                });
            } else {
                getApplicationModel().syncFolder(folder);
            }
        }
    }

    private class FolderRemoveAction extends BaseAction {

        private FolderRemoveAction(Controller controller) {
            super("action_remove_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FolderRemoveDialog panel = new FolderRemoveDialog(getController(),
                folderInfo);
            panel.open();
        }
    }

    // private class MyProblemAction extends BaseAction {
    //
    // private MyProblemAction(Controller controller) {
    // super("action_folder_problem", controller);
    // }
    //
    // public void actionPerformed(ActionEvent e) {
    // getController().getUIController().openProblemsInformation(
    // folderInfo);
    // }
    // }
    //
    private class MyClearCompletedDownloadsAction extends BaseAction {

        private MyClearCompletedDownloadsAction(Controller controller) {
            super("action_clear_completed_downloads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            TransferManager transferManager = getController()
                .getTransferManager();
            for (DownloadManager dlMan : transferManager
                .getCompletedDownloadsCollection())
            {
                if (dlMan.getFileInfo().getFolderInfo()
                    .equals(folder.getInfo()))
                {
                    transferManager.clearCompletedDownload(dlMan);
                }
            }
        }
    }

    private class MyMostRecentChangesAction extends BaseAction {

        private MyMostRecentChangesAction(Controller controller) {
            super("action_most_recent_changes", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformationLatest(
                folderInfo);
        }
    }

//    private class MyFilesAvailableAction extends AbstractAction {
//
//        public void actionPerformed(ActionEvent e) {
//            getController().getUIController().openFilesInformationIncoming(
//                folderInfo);
//        }
//    }

    private class MyDeletedFilesAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformationDeleted(
                folderInfo);
        }
    }

    private class MyOpenExplorerAction extends BaseAction {

        private MyOpenExplorerAction(Controller controller) {
            super("action_open_explorer", controller);
        }

        public void actionPerformed(ActionEvent e) {
            openExplorer();
        }
    }

    @SuppressWarnings("serial")
    private class BackupOnlineStorageAction extends BaseAction {
        private BackupOnlineStorageAction(Controller controller) {
            super("action_backup_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // FolderOnlineStoragePanel knows if folder already joined :-)
            getUIController().getApplicationModel().getServerClientModel()
                .checkAndSetupAccount();
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }

    @SuppressWarnings("serial")
    private class StopOnlineStorageAction extends BaseAction {
        private StopOnlineStorageAction(Controller controller) {
            super("action_stop_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // FolderOnlineStoragePanel knows if folder already joined :-)
            getUIController().getApplicationModel().getServerClientModel()
                .checkAndSetupAccount();
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }

    private class PrimaryButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (type == ExpandableFolderModel.Type.Local && folder != null
                && folder.countProblems() > 0)
            {
                // Display the problem.
                getController().getUIController().openProblemsInformation(
                    folderInfo);
            } else if (type == ExpandableFolderModel.Type.CloudOnly) {
                // Join the folder locally.
                PFWizard.openOnlineStorageJoinWizard(getController(),
                    Collections.singletonList(folderInfo));
            } else if (type == ExpandableFolderModel.Type.Local
                && folder != null && folder.isPreviewOnly())
            {
                // Local Preview - want to change?
                SettingsTab.doPreviewChange(getController(), folder);
            } else if (type == ExpandableFolderModel.Type.Local) {
                // Local - open it
                if (Desktop.isDesktopSupported()) {
                    openExplorer();
                }
            } else {
                // Typical - ask to create.
                askToCreateFolder();
            }
        }
    }

    @SuppressWarnings("serial")
    private class WebdavAction extends BaseAction {
        private WebdavAction(Controller controller) {
            super("action_webdav", controller);
        }

        public void actionPerformed(ActionEvent e) {
            createWebdavConnection();
        }
    }

    @SuppressWarnings("serial")
    private class WebViewAction extends BaseAction {

        private WebViewAction(Controller controller) {
            super("action_webview", controller);
        }

        public void actionPerformed(ActionEvent e) {
            ServerClient client = getController().getOSClient();
            if (client.supportsWebLogin()) {
                try {
                    String folderURL = client
                        .getFolderURLWithCredentials(folderInfo);
                    BrowserLauncher.openURL(folderURL);
                } catch (IOException e1) {
                    logSevere(e1);
                }
            }
        }
    }

    void dispose() {
        removeFolderLocalAction.dispose();
        removeFolderOnlineAction.dispose();
        backupOnlineStorageAction.dispose();
        stopOnlineStorageAction.dispose();
        inviteAction.dispose();
    }

}
