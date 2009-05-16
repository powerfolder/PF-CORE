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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.ScanResult;
import static de.dal33t.powerfolder.disk.FolderStatistic.UNKNOWN_SYNC_STATUS;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.ExpandableView;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilter;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.FileUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableFolderView extends PFUIComponent implements ExpandableView {

    private final FolderInfo folderInfo;
    private Folder folder;
    private boolean local;
    private boolean online;

    private JButtonMini openSettingsInformationButton;
    private JButtonMini openFilesInformationButton;
    private JButtonMini inviteButton;
    private JButtonMini syncFolderButton;
    private JButtonMini joinOnlineStorageButton;
    private ActionLabel membersLabel;

    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;

    private JLabel filesLabel;
    private JLabel transferModeLabel;
    private JLabel syncPercentLabel;
    private ActionLabel syncDateLabel;
    private JLabel localSizeLabel;
    private JLabel totalSizeLabel;
    private JLabel recycleLabel;
    private ActionLabel filesAvailableLabel;
    private JPanel upperPanel;
    private JLabel jLabel;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;
    private MyServerClientListener myServerClientListener;

    private ExpansionListener listenerSupport;

    private OnlineStorageComponent osComponent;
    private ServerClient serverClient;

    private MyOpenFilesInformationAction openFilesInformationAction;
    private MyOpenSettingsInformationAction openSettingsInformationAction;
    private MyInviteAction inviteAction;
    private MyOpenMembersInformationAction openMembersInformationAction;
    private MyMostRecentChangesAction mostRecentChangesAction;
    private MyOpenExplorerAction openExplorerAction;

    private JPopupMenu collapsedContextMenu;
    private JPopupMenu expandedContextMenu;

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

        updateStatsDetails();
        updateNumberOfFiles();
        updateTransferMode();
        updateFolderMembershipDetails();
        updateIconAndOS();
        updateButtons();

        registerFolderListeners();
    }

    /**
     * Expand this view if collapsed.
     */
    public void expand() {
        expanded.set(true);
        upperPanel.setToolTipText(
                Translation.getTranslation("exp_folder_view.collapse"));
        lowerOuterPanel.setVisible(true);
        listenerSupport.collapseAllButSource(new ExpansionEvent(this));
    }

    /**
     * Collapse this view if expanded.
     */
    public void collapse() {
        expanded.set(false);
        upperPanel.setToolTipText(
                Translation.getTranslation("exp_folder_view.expand"));
        lowerOuterPanel.setVisible(false);
    }

    /**
     * Gets the ui component, building if required.
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
                                            //  icon        name   space            # files     sync / join
        FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, pref:grow, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();
        jLabel = new JLabel();
        updateIconAndOS();

        upperBuilder.add(jLabel, cc.xy(1, 1));
        upperBuilder.add(new JLabel(folderInfo.name), cc.xy(3, 1));
        upperBuilder.add(filesAvailableLabel.getUIComponent(), cc.xy(6, 1));

        // syncFolderButton and joinOnlineStorageButton share same slot.
        upperBuilder.add(syncFolderButton, cc.xy(8, 1));
        upperBuilder.add(joinOnlineStorageButton, cc.xy(8, 1));

        upperPanel = upperBuilder.getPanel();
        upperPanel.setToolTipText(
                Translation.getTranslation("exp_folder_view.expand"));
        upperPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter ma = new MyMouseAdapter();
        upperPanel.addMouseListener(ma);
        jLabel.addMouseListener(ma);

        // Build lower detials with line border.
        FormLayout lowerLayout = new FormLayout("3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref");
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        lowerBuilder.addSeparator(null, cc.xywh(1, 1, 6, 1));

        lowerBuilder.add(syncDateLabel.getUIComponent(), cc.xy(2, 3));
        lowerBuilder.add(openFilesInformationButton, cc.xy(5, 3));

        lowerBuilder.add(syncPercentLabel, cc.xy(2, 5));

        lowerBuilder.add(filesLabel, cc.xy(2, 7));

        lowerBuilder.add(localSizeLabel, cc.xy(2, 9));

        lowerBuilder.add(totalSizeLabel, cc.xy(2, 11));

        lowerBuilder.add(recycleLabel, cc.xy(2, 13));

        lowerBuilder.addSeparator(null, cc.xywh(2, 15, 4, 1));

        lowerBuilder.add(membersLabel.getUIComponent(), cc.xy(2, 17));
        lowerBuilder.add(inviteButton, cc.xy(5, 17));

        lowerBuilder.addSeparator(null, cc.xywh(2, 19, 4, 1));

        lowerBuilder.add(transferModeLabel, cc.xy(2, 21));
        lowerBuilder.add(openSettingsInformationButton, cc.xy(5, 21));

        lowerBuilder.add(osComponent.getUIComponent(), cc.xywh(2, 22, 4, 1));

        JPanel lowerPanel = lowerBuilder.getPanel();

        // Build spacer then lower outer with lower panel
        FormLayout lowerOuterLayout = new FormLayout("pref:grow",
            "3dlu, pref");
        PanelBuilder lowerOuterBuilder = new PanelBuilder(lowerOuterLayout);
        lowerOuterPanel = lowerOuterBuilder.getPanel();
        lowerOuterPanel.setVisible(false);
        lowerOuterBuilder.add(lowerPanel, cc.xy(1, 2));

        // Build border around upper and lower
        FormLayout borderLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, pref, 3dlu");
        PanelBuilder borderBuilder = new PanelBuilder(borderLayout);
        borderBuilder.add(upperPanel, cc.xy(2, 2));
        borderBuilder.add(lowerOuterBuilder.getPanel(), cc.xy(2, 3));
        JPanel borderPanel = borderBuilder.getPanel();
        borderPanel.setBorder(BorderFactory.createEtchedBorder());

        // Build ui with vertical space before the next one
        FormLayout outerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 3dlu");
        PanelBuilder outerBuilder = new PanelBuilder(outerLayout);
        outerBuilder.add(borderPanel, cc.xy(2, 1));

        uiComponent = outerBuilder.getPanel();
    }

    /**
     * Initializes the components.
     */
    private void initComponent() {

        openFilesInformationAction =
                new MyOpenFilesInformationAction(getController());
        inviteAction = new MyInviteAction(getController());
        openSettingsInformationAction =
                new MyOpenSettingsInformationAction(getController());
        openMembersInformationAction =
                new MyOpenMembersInformationAction(getController());
        mostRecentChangesAction = new MyMostRecentChangesAction(getController());
        openExplorerAction = new MyOpenExplorerAction(getController());

        MySyncFolderAction mySyncFolderAction =
                new MySyncFolderAction(getController());
        MyJoinOnlineStorageAction myJoinOnlineStorageAction =
                new MyJoinOnlineStorageAction(getController());

        expanded = new AtomicBoolean();

        osComponent = new OnlineStorageComponent(getController());

        openSettingsInformationButton = new JButtonMini(
                openSettingsInformationAction, true);

        openFilesInformationButton = new JButtonMini(openFilesInformationAction,
                true);

        inviteButton = new JButtonMini(inviteAction, true);
        syncFolderButton = new JButtonMini(mySyncFolderAction, true);
        joinOnlineStorageButton = new JButtonMini(myJoinOnlineStorageAction, true);
        filesLabel = new JLabel();
        transferModeLabel = new JLabel();
        syncPercentLabel = new JLabel();
        syncDateLabel = new ActionLabel(getController(), mostRecentChangesAction);
        localSizeLabel = new JLabel();
        totalSizeLabel = new JLabel();
        recycleLabel = new JLabel();
        membersLabel = new ActionLabel(getController(),
                openMembersInformationAction);
        filesAvailableLabel = new ActionLabel(getController(),
                new MyFilesAvailableAction());

        updateNumberOfFiles();
        updateStatsDetails();
        updateFolderMembershipDetails();
        updateTransferMode();
        updateButtons();

        myServerClientListener = new MyServerClientListener();
        getController().getOSClient().addListener(myServerClientListener);
    }

    private void updateButtons() {
        boolean enabled = folder != null;

        openSettingsInformationButton.setEnabled(enabled);
        openSettingsInformationAction.setEnabled(enabled);

        openFilesInformationButton.setEnabled(enabled);
        openFilesInformationAction.setEnabled(enabled);

        inviteButton.setEnabled(enabled);
        inviteAction.setEnabled(enabled);

        syncDateLabel.setEnabled(enabled);
        mostRecentChangesAction.setEnabled(enabled);

        membersLabel.setEnabled(enabled);
        openMembersInformationAction.setEnabled(enabled);

        syncFolderButton.setVisible(enabled);
        joinOnlineStorageButton.setVisible(!enabled);
        openExplorerAction.setEnabled(enabled && Desktop.isDesktopSupported());
    }

    /**
     * Call if this object is being discarded, so that listeners are not orphaned.
     */
    public void unregisterListeners() {
        if (myServerClientListener != null) {
            getController().getOSClient().addListener(myServerClientListener);
            myServerClientListener = null;
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
        String syncDateText;
        String localSizeString;
        String totalSizeString;
        String recycleLabelText;
        String filesAvailableLabelText;
        if (folder == null) {

            syncPercentText = Translation.getTranslation(
                    "exp_folder_view.synchronized", "?");
            syncDateText = Translation.getTranslation(
                    "exp_folder_view.last_synchronized", "?");
            localSizeString = "?";
            totalSizeString = "?";
            recycleLabelText = Translation.getTranslation("exp_folder_view.recycled",
                    "?", "?");
            filesAvailableLabelText = "";
        } else {

            Date lastSyncDate = folder.getLastSyncDate();

            if (lastSyncDate == null) {
                syncDateText = Translation.getTranslation(
                        "exp_folder_view.never_synchronized");
            } else {
                String formattedDate = Format.formatDate(lastSyncDate);
                syncDateText = Translation.getTranslation(
                        "exp_folder_view.last_synchronized",
                        formattedDate);
            }

            ScanResult.ResultState state = folder.getLastScanResultState();
            if (state == null) {
                syncPercentText = Translation.getTranslation(
                        "exp_folder_view.not_yet_scanned");
                localSizeString = "?";
                totalSizeString = "?";
                recycleLabelText = Translation.getTranslation(
                        "exp_folder_view.recycled", "?", "?");
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
                if (Double.compare(sync, 100.0) < 0  && Double.compare(sync,
                        UNKNOWN_SYNC_STATUS) > 0)  {
                    Date date = folder.getStatistic().getEstimatedSyncDate();
                    if (date != null) {
                        String formattedDate = Format.formatDate(date);
                        syncDateText = Translation.getTranslation(
                                "exp_folder_view.estimated_synchronized",
                                formattedDate);
                    }
                }

                if (lastSyncDate == null && (Double.compare(sync, 100.0) == 0
                        || Double.compare(sync, UNKNOWN_SYNC_STATUS) == 0)) {
                    // Never synced with others.
                    syncPercentText = Translation.getTranslation(
                            "exp_folder_view.synchronized", "?");
                } else {
                    syncPercentText = Translation.getTranslation(
                            "exp_folder_view.synchronized",
                            Double.compare(sync, UNKNOWN_SYNC_STATUS) == 0 ? "?"
                                    : sync);
                }

                long localSize = statistic.getLocalSize();
                localSizeString = Format.formatBytesShort(localSize);

                long totalSize = statistic.getTotalSize();
                totalSizeString = Format.formatBytesShort(totalSize);

                if (folder.isUseRecycleBin()) {
                    RecycleBin recycleBin = getController().getRecycleBin();
                    int recycledCount = recycleBin.countRecycledFiles(folderInfo);
                    long recycledSize = recycleBin.recycledFilesSize(folderInfo);
                    String recycledSizeString = Format.formatBytesShort(recycledSize);
                    recycleLabelText = Translation.getTranslation("exp_folder_view.recycled",
                            recycledCount, recycledSizeString);
                } else {
                    recycleLabelText = Translation.getTranslation("exp_folder_view.no_recycled");
                }

                int count = statistic.getIncomingFilesCount();
                if (count == 0) {
                    filesAvailableLabelText = "";
                } else {
                    filesAvailableLabelText = Translation.getTranslation(
                            "exp_folder_view.files_available", count);
                }
            }
        }

        syncPercentLabel.setText(syncPercentText);
        syncDateLabel.setText(syncDateText);
        localSizeLabel.setText(Translation.getTranslation("exp_folder_view.local",
                localSizeString));
        totalSizeLabel.setText(Translation.getTranslation("exp_folder_view.total",
                totalSizeString));
        recycleLabel.setText(recycleLabelText);
        filesAvailableLabel.setText(filesAvailableLabelText);
        if (filesAvailableLabelText.length() == 0) {
            filesAvailableLabel.setToolTipText(null);
        } else {
            filesAvailableLabel.setToolTipText(
                    Translation.getTranslation("exp_folder_view.files_available_tip"));
        }
    }

    /**
     * Updates the number of files details of the folder.
     */
    private void updateNumberOfFiles() {
        String filesText;
        if (folder == null) {
            filesText = Translation.getTranslation("exp_folder_view.files",
                    "?");
        } else {
            filesText = Translation.getTranslation("exp_folder_view.files",
                    folder.getKnownFilesCount());
        }
        filesLabel.setText(filesText);
    }

    /**
     * Updates transfer mode of the folder.
     */
    private void updateTransferMode() {
        String transferMode;
        if (folder == null) {
            transferMode = Translation.getTranslation("exp_folder_view.transfer_mode",
                    "?");
        } else {
            transferMode = Translation.getTranslation("exp_folder_view.transfer_mode",
                    folder.getSyncProfile().getProfileName());
        }
        transferModeLabel.setText(transferMode);
    }

    /**
     * Updates the folder member details.
     */
    private void updateFolderMembershipDetails() {
        String countText;
        if (folder == null) {
            countText = "?";
        } else {
            countText = String.valueOf(folder.getMembersCount());
        }
        membersLabel.setText(Translation.getTranslation(
                "exp_folder_view.members", countText));
    }

    private void updateIconAndOS() {

        if (folder == null) {
            jLabel.setIcon(Icons.getIconById(Icons.ONLINE_FOLDER));
            jLabel.setToolTipText(Translation.getTranslation(
                    "exp_folder_view.folder_online_text"));
            osComponent.getUIComponent().setVisible(false);
        } else {
            boolean preview = folder.isPreviewOnly();
            if (preview) {
                jLabel.setIcon(Icons.getIconById(Icons.PREVIEW_FOLDER));
                jLabel.setToolTipText(Translation.getTranslation(
                        "exp_folder_view.folder_preview_text"));
                osComponent.getUIComponent().setVisible(false);
            } else if (online) {
                jLabel.setIcon(Icons.getIconById(Icons.LOCAL_AND_ONLINE_FOLDER));
                jLabel.setToolTipText(Translation.getTranslation(
                        "exp_folder_view.folder_local_online_text"));
                osComponent.getUIComponent().setVisible(true);
                Member server = serverClient.getServer();
                double sync = folder.getStatistic().getSyncPercentage(server);
                boolean warned = serverClient.getAccountDetails().getAccount()
                        .getOSSubscription().isWarnedUsage();
                osComponent.setSyncPercentage(sync, warned);
            } else {
                jLabel.setIcon(Icons.getIconById(Icons.LOCAL_FOLDER));
                jLabel.setToolTipText(Translation.getTranslation(
                        "exp_folder_view.folder_local_text"));
                osComponent.getUIComponent().setVisible(false);
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

    public JPopupMenu createCollapsedPopupMenu() {
        if (collapsedContextMenu == null) {
            collapsedContextMenu = new JPopupMenu();
            collapsedContextMenu.add(openExplorerAction);
            collapsedContextMenu.addSeparator();
            collapsedContextMenu.add(openFilesInformationAction);
            collapsedContextMenu.add(mostRecentChangesAction);
            collapsedContextMenu.add(inviteAction);
            collapsedContextMenu.add(openMembersInformationAction);
            collapsedContextMenu.add(openSettingsInformationAction);
        }
        return collapsedContextMenu;
    }

    private void openExplorer() {
        try {
            FileUtils.openFile(folder.getLocalBase());
        } catch (IOException ioe) {
            logSevere("IOException", ioe);
        }
    }

    public JPopupMenu createExpandedPopupMenu() {
        if (expandedContextMenu == null) {
            expandedContextMenu = new JPopupMenu();
            expandedContextMenu.add(openExplorerAction);
        }
        return expandedContextMenu;
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Class to respond to folder events.
     */
    private class MyFolderListener implements FolderListener {

        private void doFolderChanges(FolderEvent folderEvent) {
            if (folder == null || folder.equals(folderEvent.getFolder())) {
                updateStatsDetails();
                updateIconAndOS();
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
    private class MyFolderMembershipListener implements FolderMembershipListener {

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

    /**
     * Class to respond to expand / collapse events.
     */
    private class MyMouseAdapter extends MouseAdapter {

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
            if (expanded.get()) {
                createExpandedPopupMenu().show(evt.getComponent(), evt.getX(),
                        evt.getY());
            } else {
                createCollapsedPopupMenu().show(evt.getComponent(), evt.getX(),
                        evt.getY());
            }
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
            getController().getUIController().openSettingsInformation(folderInfo);
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
            getController().getUIController().openMembersInformation(folderInfo);
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public void login(ServerClientEvent event) {
            updateIconAndOS();
        }

        public void accountUpdated(ServerClientEvent event) {
            updateIconAndOS();
        }

        public void serverConnected(ServerClientEvent event) {
            updateIconAndOS();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateIconAndOS();
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
            getController().getUIController().syncFolder(folderInfo);
        }
    }

    private class MyJoinOnlineStorageAction extends BaseAction {

        private MyJoinOnlineStorageAction(Controller controller) {
            super("action_join_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            List<FolderInfo> folderInfoList = new ArrayList<FolderInfo>();
            folderInfoList.add(folderInfo);
            PFWizard.openSingletonOnlineStorageJoinWizard(getController(),
                    folderInfoList);
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
            getController().getUIController().openFilesInformation(folderInfo,
                    DirectoryFilter.MODE_INCOMING_ONLY);
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
}
