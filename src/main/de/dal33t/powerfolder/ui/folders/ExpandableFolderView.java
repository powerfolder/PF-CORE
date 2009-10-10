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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.TimerTask;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.event.ExpansionEvent;
import de.dal33t.powerfolder.event.ExpansionListener;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.ExpandableView;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.FolderRemovePanel;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilter;
import de.dal33t.powerfolder.ui.information.folder.settings.SettingsTab;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;
import de.dal33t.powerfolder.util.ui.SyncIconButtonMini;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableFolderView extends PFUIComponent implements
    ExpandableView
{

    private final FolderInfo folderInfo;
    private Folder folder;
    private boolean local;
    private boolean online;

    private JLabel nameLabel;
    private JButtonMini openSettingsInformationButton;
    private JButtonMini openFilesInformationButton;
    private JButtonMini inviteButton;
    private JButtonMini problemButton;
    private SyncIconButtonMini syncFolderButton;
    private ActionLabel membersLabel;

    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;

    private JLabel filesLabel;
    private ActionLabel transferModeLabel;
    private JLabel syncPercentLabel;
    private ActionLabel syncDateLabel;
    private JLabel localSizeLabel;
    private JLabel totalSizeLabel;
    private ActionLabel filesAvailableLabel;
    private JPanel upperPanel;
    private JButtonMini primaryButton;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;

    private MyServerClientListener myServerClientListener;
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
    private FolderRemoveAction removeFolderAction;
    private BackupOnlineStorageAction backupOnlineStorageAction;
    private StopOnlineStorageAction stopOnlineStorageAction;

    private DelayedUpdater syncUpdater;
    private DelayedUpdater folderUpdater;
    private DelayedUpdater folderDetailsUpdater;

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
     * @param folderArg
     */
    public void configure(Folder folderArg, boolean localArg, boolean onlineArg)
    {
        boolean changed = false;
        if (folderArg != null && folder == null) {
            changed = true;
        } else if (folderArg == null && folder != null) {
            changed = true;
        } else if (folderArg != null && !folder.equals(folderArg)) {
            changed = true;
        } else if (local ^ localArg) {
            changed = true;
        } else if (online ^ onlineArg) {
            changed = true;
        }

        if (!changed) {
            return;
        }

        // Something changed - change details.

        unregisterFolderListeners();

        folder = folderArg;
        local = localArg;
        online = onlineArg;
        osComponent.setFolder(folderArg);

        updateStatsDetails();
        updateNumberOfFiles();
        updateTransferMode();
        updateFolderMembershipDetails();
        updateIconAndOS();
        updateButtons();
        updateProblems();
        updateNewFiles();

        registerFolderListeners();
    }

    /**
     * Expand this view if collapsed.
     */
    public void expand() {
        if (folder == null) {
            // Don't expand for ONLINE folder only.
            return;
        }
        expanded.set(true);
        upperPanel.setToolTipText(Translation
            .getTranslation("exp_folder_view.collapse"));
        lowerOuterPanel.setVisible(true);
        listenerSupport.collapseAllButSource(new ExpansionEvent(this));
    }

    /**
     * Collapse this view if expanded.
     */
    public void collapse() {
        expanded.set(false);
        upperPanel.setToolTipText(Translation
            .getTranslation("exp_folder_view.expand"));
        lowerOuterPanel.setVisible(false);
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
        // icon name space # files probs sync / join
        FormLayout upperLayout = new FormLayout(
            "pref, 3dlu, pref, pref:grow, 3dlu, pref, 3dlu, pref", "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();
        primaryButton = new JButtonMini(Icons.getIconById(Icons.BLANK), "");
        updateIconAndOS();

        upperBuilder.add(primaryButton, cc.xy(1, 1));
        nameLabel = new JLabel();
        upperBuilder.add(nameLabel, cc.xy(3, 1));
        upperBuilder.add(filesAvailableLabel.getUIComponent(), cc.xy(6, 1));

        upperBuilder.add(problemButton, cc.xy(8, 1));

        upperPanel = upperBuilder.getPanel();
        upperPanel.setOpaque(false);
        upperPanel.setToolTipText(Translation
            .getTranslation("exp_folder_view.expand"));
        upperPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter ma = new MyMouseAdapter();
        upperPanel.addMouseListener(ma);

        primaryButton.addActionListener(new PrimaryButtonActionListener());

        // Build lower detials with line border.
        FormLayout lowerLayout;
        if (getController().isBackupOnly()) {
            // Skip computers stuff
            lowerLayout = new FormLayout(
                "3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref");
        } else {
            lowerLayout = new FormLayout(
                "3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref");
        }
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        int row = 1;

        lowerBuilder.addSeparator(null, cc.xywh(1, row, 6, 1));

        row += 2;

        lowerBuilder.add(syncDateLabel.getUIComponent(), cc.xy(2, row));
        lowerBuilder.add(syncFolderButton, cc.xy(5, row));

        row += 2;

        lowerBuilder.add(syncPercentLabel, cc.xy(2, row));
        lowerBuilder.add(openFilesInformationButton, cc.xy(5, row));

        row += 2;

        lowerBuilder.add(filesLabel, cc.xy(2, row));

        row += 2;

        lowerBuilder.add(localSizeLabel, cc.xy(2, row));

        row += 2;

        lowerBuilder.add(totalSizeLabel, cc.xy(2, row));

        row += 2;

        lowerBuilder.addSeparator(null, cc.xywh(2, row, 4, 1));

        row += 2;

        // No computers stuff if backup mode.
        if (getController().isBackupOnly()) {
            lowerBuilder.add(transferModeLabel.getUIComponent(), cc.xy(2, row));
            lowerBuilder.add(openSettingsInformationButton, cc.xy(5, row));

        } else {
            lowerBuilder.add(membersLabel.getUIComponent(), cc.xy(2, row));
            lowerBuilder.add(inviteButton, cc.xy(5, row));

            row += 2;

            lowerBuilder.addSeparator(null, cc.xywh(2, row, 4, 1));

            row += 2;

            lowerBuilder.add(transferModeLabel.getUIComponent(), cc.xy(2, row));
            lowerBuilder.add(openSettingsInformationButton, cc.xy(5, row));
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
        JPanel borderPanel = borderBuilder.getPanel();
        borderPanel.setOpaque(false);
        borderPanel.setBorder(BorderFactory.createEtchedBorder());

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
        openSettingsInformationAction = new MyOpenSettingsInformationAction(
            getController());
        openMembersInformationAction = new MyOpenMembersInformationAction(
            getController());
        mostRecentChangesAction = new MyMostRecentChangesAction(getController());
        clearCompletedDownloadsAction = new MyClearCompletedDownloadsAction(
            getController());
        openExplorerAction = new MyOpenExplorerAction(getController());
        removeFolderAction = new FolderRemoveAction(getController());
        backupOnlineStorageAction = new BackupOnlineStorageAction(
            getController());
        stopOnlineStorageAction = new StopOnlineStorageAction(getController());

        MyProblemAction myProblemAction = new MyProblemAction(getController());
        syncFolderAction = new MySyncFolderAction(getController());

        expanded = new AtomicBoolean();

        osComponent = new OnlineStorageComponent(getController(), folder);

        openSettingsInformationButton = new JButtonMini(
            openSettingsInformationAction, true);

        openFilesInformationButton = new JButtonMini(
            openFilesInformationAction, true);

        inviteButton = new JButtonMini(inviteAction, true);
        problemButton = new JButtonMini(myProblemAction, true);
        syncFolderButton = new SyncIconButtonMini(getController());
        syncFolderButton.addActionListener(syncFolderAction);
        filesLabel = new JLabel();
        transferModeLabel = new ActionLabel(getController(),
            openSettingsInformationAction);
        syncPercentLabel = new JLabel();
        syncDateLabel = new ActionLabel(getController(),
            mostRecentChangesAction);
        localSizeLabel = new JLabel();
        totalSizeLabel = new JLabel();
        membersLabel = new ActionLabel(getController(),
            openMembersInformationAction);
        filesAvailableLabel = new ActionLabel(getController(),
            new MyFilesAvailableAction());

        updateNumberOfFiles();
        updateStatsDetails();
        updateFolderMembershipDetails();
        updateTransferMode();
        updateButtons();
        updateProblems();

        registerListeners();
    }

    private void updateButtons() {
        boolean enabled = folder != null;

        openSettingsInformationButton.setEnabled(enabled);
        transferModeLabel.setEnabled(enabled);
        openSettingsInformationAction.setEnabled(enabled);

        openFilesInformationButton.setEnabled(enabled);
        openFilesInformationAction.setEnabled(enabled);

        inviteButton.setEnabled(enabled);
        inviteAction.setEnabled(enabled);

        syncDateLabel.setEnabled(enabled);
        mostRecentChangesAction.setEnabled(enabled);

        membersLabel.setEnabled(enabled);
        openMembersInformationAction.setEnabled(enabled);

        openExplorerAction.setEnabled(enabled && Desktop.isDesktopSupported());

        // Always.
        removeFolderAction.setEnabled(true);
        updateSyncButton();
    }

    private void updateSyncButton() {
        if (folder == null) {
            syncFolderButton.spin(false);
            return;
        }
        syncUpdater.schedule(new Runnable() {
            public void run() {
                if (folder == null) {
                    syncFolderButton.spin(false);
                } else {
                    syncFolderButton.spin(folder.isSyncing());
                }
            }
        });
    }

    private void registerListeners() {
        myServerClientListener = new MyServerClientListener();
        getController().getOSClient().addListener(myServerClientListener);

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
        if (myServerClientListener != null) {
            getController().getOSClient().addListener(myServerClientListener);
            myServerClientListener = null;
        }
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
     * Gets the name of the associated folder.
     * 
     * @return
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
        if (folder == null) {

            syncPercentText = Translation.getTranslation(
                "exp_folder_view.synchronized", "?");
            syncDateText = Translation.getTranslation(
                "exp_folder_view.last_synchronized", "?");
            localSizeString = "?";
            totalSizeString = "?";
            filesAvailableLabelText = "";
        } else {

            Date lastSyncDate = folder.getLastSyncDate();

            if (lastSyncDate == null) {
                syncDateText = Translation
                    .getTranslation("exp_folder_view.never_synchronized");
            } else {
                String formattedDate = Format.getInstance(getController())
                    .formatDate(lastSyncDate);
                syncDateText = Translation.getTranslation(
                    "exp_folder_view.last_synchronized", formattedDate);
            }

            ScanResult.ResultState state = folder.getLastScanResultState();
            if (state == null) {
                syncPercentText = Translation
                    .getTranslation("exp_folder_view.not_yet_scanned");
                localSizeString = "?";
                totalSizeString = "?";
                filesAvailableLabelText = "";
            } else {
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
                        String formattedDate = Format.getInstance(
                            getController()).formatDate(date);
                        syncDateText = Translation.getTranslation(
                            "exp_folder_view.estimated_synchronized",
                            formattedDate);
                    }
                }

                if (lastSyncDate == null
                    && (Double.compare(sync, 100.0) == 0 || Double.compare(
                        sync, UNKNOWN_SYNC_STATUS) == 0))
                {
                    // Never synced with others.
                    syncPercentText = Translation
                        .getTranslation("exp_folder_view.unsynchronized");
                } else {
                    if (Double.compare(sync, UNKNOWN_SYNC_STATUS) == 0) {
                        syncPercentText = Translation
                            .getTranslation("exp_folder_view.unsynchronized");
                        syncPercentTip = Translation
                            .getTranslation("exp_folder_view.unsynchronized.tip");
                    } else {
                        syncPercentText = Translation.getTranslation(
                            "exp_folder_view.synchronized", Format
                                .formatNumber(sync));
                    }
                }

                if (lastSyncDate != null && Double.compare(sync, 100.0) == 0) {
                    // 100% sync - remove any sync problem.
                    folder.processUnsyncFolder();
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
                        "exp_folder_view.files_available", String
                            .valueOf(count));
                }
            }
        }

        syncPercentLabel.setText(syncPercentText);
        syncPercentLabel.setToolTipText(syncPercentTip);
        syncDateLabel.setText(syncDateText);
        localSizeLabel.setText(Translation.getTranslation(
            "exp_folder_view.local", localSizeString));
        totalSizeLabel.setText(Translation.getTranslation(
            "exp_folder_view.total", totalSizeString));
        filesAvailableLabel.setText(filesAvailableLabelText);
        if (filesAvailableLabelText.length() == 0) {
            filesAvailableLabel.setToolTipText(null);
        } else {
            filesAvailableLabel.setToolTipText(Translation
                .getTranslation("exp_folder_view.files_available_tip"));
        }
    }

    /**
     * Updates the number of files details of the folder.
     */
    private void updateNumberOfFiles() {
        String filesText;
        if (folder == null) {
            filesText = Translation
                .getTranslation("exp_folder_view.files", "?");
        } else {
            // FIXME: Returns # of files + # of directories
            filesText = Translation.getTranslation("exp_folder_view.files",
                String.valueOf(folder.getStatistic().getLocalFilesCount()));
        }
        filesLabel.setText(filesText);
    }

    /**
     * Updates transfer mode of the folder.
     */
    private void updateTransferMode() {
        String transferMode;
        if (folder == null) {
            transferMode = Translation.getTranslation(
                "exp_folder_view.transfer_mode", "?");
        } else {
            transferMode = Translation.getTranslation(
                "exp_folder_view.transfer_mode", folder.getSyncProfile()
                    .getName());
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
        if (folder == null) {
            countText = "?";
            connectedCountText = "?";
        } else {
            countText = String.valueOf(folder.getMembersCount());
            connectedCountText = String.valueOf(folder
                .getConnectedMembersCount());
        }
        membersLabel.setText(Translation.getTranslation(
            "exp_folder_view.members", countText, connectedCountText));
    }

    private void updateIconAndOS() {

        if (folder == null) {
            primaryButton.setIcon(Icons.getIconById(Icons.ONLINE_FOLDER));
            primaryButton.setToolTipText(Translation
                .getTranslation("exp_folder_view.folder_online_text"));
            osComponent.getUIComponent().setVisible(
                PreferencesEntry.USE_ONLINE_STORAGE
                    .getValueBoolean(getController()));
        } else {
            boolean preview = folder.isPreviewOnly();
            if (preview) {
                primaryButton.setIcon(Icons.getIconById(Icons.PREVIEW_FOLDER));
                primaryButton.setToolTipText(Translation
                    .getTranslation("exp_folder_view.folder_preview_text"));
            } else if (online) {
                primaryButton.setIcon(Icons
                    .getIconById(Icons.LOCAL_AND_ONLINE_FOLDER));
                primaryButton
                    .setToolTipText(Translation
                        .getTranslation("exp_folder_view.folder_local_online_text"));
                osComponent.getUIComponent().setVisible(
                    PreferencesEntry.USE_ONLINE_STORAGE
                        .getValueBoolean(getController()));
            } else {
                primaryButton.setIcon(Icons.getIconById(Icons.LOCAL_FOLDER));
                primaryButton.setToolTipText(Translation
                    .getTranslation("exp_folder_view.folder_local_text"));
            }
        }

        if (folder != null && folder.isPreviewOnly()) {
            osComponent.getUIComponent().setVisible(false);
        } else {
            Boolean osComponentVisible = PreferencesEntry.USE_ONLINE_STORAGE
                .getValueBoolean(getController());
            osComponent.getUIComponent().setVisible(osComponentVisible);
            if (osComponentVisible) {
                Member server = serverClient.getServer();
                double sync = 0;
                if (folder != null && server != null) {
                    sync = folder.getStatistic().getSyncPercentage(server);
                }
                boolean warned = serverClient.getAccountDetails().getAccount()
                    .getOSSubscription().isDisabledUsage();
                boolean joined = folder != null
                    && serverClient.hasJoined(folder);
                osComponent.setSyncPercentage(sync, warned, joined);
            }
        }
    }

    public void addExpansionListener(ExpansionListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeExpansionListener(ExpansionListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
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
        if (online && !local) {
            // Online-only folder popup
            contextMenu.add(removeFolderAction);
        } else {
            // Local folder popup
            contextMenu.add(openExplorerAction);
            contextMenu.addSeparator();
            contextMenu.add(syncFolderAction);
            contextMenu.add(openFilesInformationAction);
            contextMenu.add(mostRecentChangesAction);
            contextMenu.add(clearCompletedDownloadsAction);
            contextMenu.addSeparator();
            contextMenu.add(inviteAction);
            contextMenu.add(openMembersInformationAction);
            contextMenu.addSeparator();
            contextMenu.add(openSettingsInformationAction);
            contextMenu.add(removeFolderAction);
            if (folder != null && serverClient.isConnected()
                && serverClient.isLoggedIn())
            {
                boolean osConfigured = serverClient.hasJoined(folder);
                if (osConfigured) {
                    contextMenu.add(stopOnlineStorageAction);
                } else {
                    contextMenu.add(backupOnlineStorageAction);
                }
            }
        }
        return contextMenu;
    }

    private void openExplorer() {
        try {
            FileUtils.openFile(folder.getLocalBase());
        } catch (IOException ioe) {
            logSevere("IOException", ioe);
        }
    }

    /**
     * This is called when a Problem has been added / removed for this folder.
     * If there are problems for this folder, show icon.
     */
    public void updateProblems() {
        problemButton.setVisible(folder != null && folder.countProblems() > 0);
    }

    /**
     * Downloads added or removed for this folder. Recalculate new files status.
     */
    public void updateNewFiles() {

        int newCount = 0;
        boolean newFiles = false;
        String newCountString = "";

        if (folder != null) {
            newCount = getController().getTransferManager()
                .countCompletedDownloads(folder);
            newFiles = newCount > 0;
            if (newFiles) {
                newCountString = " - "
                    + Translation.getTranslation(
                        "exp_folder_view.new_files_text", String
                            .valueOf(newCount));
            }
        }
        
        if (folderInfo.name.length() > 25) {
            nameLabel.setText(folderInfo.name.substring(0, 25) + "..."
                + newCountString);
        } else {
            nameLabel.setText(folderInfo.name + newCountString);
        }
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), newFiles
            ? Font.BOLD
            : Font.PLAIN, nameLabel.getFont().getSize()));
        clearCompletedDownloadsAction.setEnabled(newFiles);
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyNodeManagerListener extends NodeManagerAdapter {

        private void updateIfRequired(NodeManagerEvent e) {
            if (folder != null && folder.hasMember(e.getNode())) {
                updateFolderMembershipDetails();
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

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Class to respond to folder events.
     */
    private class MyFolderListener implements FolderListener {

        private void doFolderChanges(FolderEvent folderEvent) {
            if (folder == null || folder.equals(folderEvent.getFolder())) {
                folderUpdater.schedule(new Runnable() {
                    public void run() {
                        updateNumberOfFiles();
                        updateStatsDetails();
                        updateIconAndOS();
                        updateButtons();
                    }
                });
            }
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            doFolderChanges(folderEvent);
        }

        public void fileChanged(FolderEvent folderEvent) {
            doFolderChanges(folderEvent);
        }

        public void filesDeleted(FolderEvent folderEvent) {
            doFolderChanges(folderEvent);
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            doFolderChanges(folderEvent);
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            doFolderChanges(folderEvent);
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            doFolderChanges(folderEvent);
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
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
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

    private class MyTransferManagerListener extends TransferAdapter {

        private void updateIfRequired(TransferManagerEvent event) {
            if (folder == null
                || !folderInfo.equals(event.getFile().getFolderInfo()))
            {
                return;
            }
            updateSyncButton();
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

    /**
     * Class to respond to expand / collapse events.
     */
    private class MyMouseAdapter extends MouseAdapter {

        private volatile boolean mouseOver;

        // Auto expand if user hovers for two seconds.
        public void mouseEntered(MouseEvent e) {
            if (PreferencesEntry.AUTO_EXPAND.getValueBoolean(getController())) {
                mouseOver = true;
                if (!expanded.get()) {
                    getController().schedule(new TimerTask() {
                        public void run() {
                            if (mouseOver) {
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
        }

        public void mouseExited(MouseEvent e) {
            mouseOver = false;
        }

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
            createPopupMenu().show(evt.getComponent(), evt.getX(), evt.getY());
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (expanded.get()) {
                    collapse();
                } else {
                    expand();
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

    private class MyOpenFilesInformationAction extends BaseAction {

        MyOpenFilesInformationAction(Controller controller) {
            super("action_open_files_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformation(folderInfo);
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

    private class MyServerClientListener implements ServerClientListener {

        public void login(ServerClientEvent event) {
            // updateIconAndOS();
        }

        public void accountUpdated(ServerClientEvent event) {
            // updateIconAndOS();
        }

        public void serverConnected(ServerClientEvent event) {
            // updateIconAndOS();
        }

        public void serverDisconnected(ServerClientEvent event) {
            // updateIconAndOS();
        }

        public boolean fireInEventDispatchThread() {
            return true;
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
                        PreviewToJoinPanel panel = new PreviewToJoinPanel(
                            getController(), folder);
                        panel.open();
                    }
                });
            } else {
                getController().getUIController().syncFolder(folder);
            }
        }
    }

    private class FolderRemoveAction extends BaseAction {

        private FolderRemoveAction(Controller controller) {
            super("action_remove_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FolderRemovePanel panel = new FolderRemovePanel(getController(),
                folderInfo);
            panel.open();
        }
    }

    private class MyProblemAction extends BaseAction {

        private MyProblemAction(Controller controller) {
            super("action_folder_problem", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openProblemsInformation(
                folderInfo);
        }
    }

    private class MyClearCompletedDownloadsAction extends BaseAction {

        private MyClearCompletedDownloadsAction(Controller controller) {
            super("action_clear_completed_downloads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            TransferManager transferManager = getController()
                .getTransferManager();
            List<FileInfo> infoList = folder.getDirectory()
                .getFileInfosRecursive();
            for (FileInfo fileInfo : infoList) {
                if (transferManager.isCompletedDownload(fileInfo)) {
                    transferManager.clearCompletedDownload(fileInfo);
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

    private class MyFilesAvailableAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformationIncoming(
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

    private class BackupOnlineStorageAction extends BaseAction {
        private BackupOnlineStorageAction(Controller controller) {
            super("action_backup_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // FolderOnlineStoragePanel knows if folder already joined :-)
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }

    private class StopOnlineStorageAction extends BaseAction {
        private StopOnlineStorageAction(Controller controller) {
            super("action_stop_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // FolderOnlineStoragePanel knows if folder already joined :-)
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }

    private class PrimaryButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (folder == null && folderInfo != null) {
                PFWizard.openSingletonOnlineStorageJoinWizard(getController(),
                    Collections.singletonList(folderInfo));
            } else if (folder != null && folder.isPreviewOnly()) {
                // Preview
                SettingsTab.doPreviewChange(getController(), folder);
            } else {
                // Local
                if (Desktop.isDesktopSupported()) {
                    openExplorer();
                }
            }
        }
    }
}
