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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
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
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
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
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.FolderRemovePanel;
import de.dal33t.powerfolder.ui.dialog.PreviewToJoinPanel;
import de.dal33t.powerfolder.ui.information.folder.settings.SettingsTab;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.widget.ResizingJLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SyncIconButtonMini;

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

    private final AtomicBoolean showing100Sync = new AtomicBoolean();

    private ActionLabel upperSyncLink;
    private JButtonMini upperOpenFilesButton;
    private JButtonMini upperMountWebDavButton;
    private JButtonMini upperInviteButton;

    private ResizingJLabel nameLabel;
    private JButtonMini openSettingsInformationButton;
    private JButtonMini openFilesInformationButton;
    private JButtonMini inviteButton;
    private JButtonMini problemButton;
    private ActionLabel membersLabel;

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
    private ActionLabel filesAvailableLabel;
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
    private FolderRemoveAction removeFolderAction;
    private BackupOnlineStorageAction backupOnlineStorageAction;
    private StopOnlineStorageAction stopOnlineStorageAction;
    private WebdavAction webdavAction;

    // Performance stuff
    private DelayedUpdater syncUpdater;
    private DelayedUpdater folderUpdater;
    private DelayedUpdater folderDetailsUpdater;
    
    private boolean folderInCloud;
    private Date lastFetch;

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
        updateProblems();
        updateNameLabel();
        updatePermissions();
        updateDeletedFiles();

        registerFolderListeners();
    }

    /**
     * Expand this view if collapsed.
     */
    public void expand() {
        if (type != ExpandableFolderModel.Type.Local) {
            // Only expand for Local folder only.
            return;
        }
        expanded.set(true);
        updateUpperComponents();
        upperPanel.setToolTipText(Translation
            .getTranslation("exp_folder_view.collapse"));
        updateNameLabel();
        lowerOuterPanel.setVisible(true);
        listenerSupport.collapseAllButSource(new ExpansionEvent(this));
        borderPanel.setBorder(Borders.createEmptyBorder("0, 0, 10dlu, 0"));
    }

    /**
     * Collapse this view if expanded.
     */
    public void collapse() {
        expanded.set(false);
        updateUpperComponents();
        upperPanel.setToolTipText(Translation
            .getTranslation("exp_folder_view.expand"));
        updateNameLabel();
        lowerOuterPanel.setVisible(false);
        borderPanel.setBorder(null);
    }

    /**
     * Show the upper links if mouse over.
     */
    private void updateUpperComponents() {
        upperSyncLink.getUIComponent().setVisible(
            type == ExpandableFolderModel.Type.Local
                && (mouseOver.get() || !showing100Sync.get()));
        boolean showLocalButtons = mouseOver.get()
            && type == ExpandableFolderModel.Type.Local;
        upperInviteButton.setVisible(showLocalButtons);
        upperOpenFilesButton.setVisible(showLocalButtons);

        final boolean showCloudOnlyButtons = mouseOver.get()
            && type == ExpandableFolderModel.Type.CloudOnly;
        SwingWorker worker = new SwingWorker() {
            protected Object doInBackground() throws Exception {
                if (OSUtil.isWindows7System() || OSUtil.isWindowsVistaSystem())
                {
                    if (serverClient.isConnected() && folderInCloud()) {
                        upperMountWebDavButton.setVisible(showCloudOnlyButtons);
                    } else {
                        upperMountWebDavButton.setVisible(false);
                    }
                } else {
                    upperMountWebDavButton.setVisible(false);
                }
                return null;
            }
        };
        worker.execute();
    }

    private synchronized boolean folderInCloud() {
        // Cache 60s.
        if (lastFetch == null
            || lastFetch.before(new Date(
                System.currentTimeMillis() - 1000L * 60 * 5)))
        {
            folderInCloud = serverClient.getFolderService().hasJoined(
                folderInfo);
            lastFetch = new Date();
        }
        return folderInCloud;
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
            "pref, 3dlu, pref:grow, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();
        updateIconAndOS();

        upperBuilder.add(primaryButton, cc.xy(1, 1));
        upperBuilder.add(upperSyncFolderButton, cc.xy(1, 1));

        MouseAdapter mca = new MyMouseClickAdapter();
        MouseAdapter moa = new MyMouseOverAdapter();
        nameLabel = new ResizingJLabel();
        upperBuilder.add(nameLabel, cc.xy(3, 1));
        nameLabel.addMouseListener(moa);
        nameLabel.addMouseListener(mca); // Because this is the biggest blank
                                         // area where the user might click.
        upperBuilder.add(filesAvailableLabel.getUIComponent(), cc.xy(5, 1));
        filesAvailableLabel.getUIComponent().addMouseListener(moa);

        upperBuilder.add(upperSyncLink.getUIComponent(), cc.xy(7, 1));
        upperBuilder.add(upperInviteButton, cc.xy(9, 1));
        upperBuilder.add(upperOpenFilesButton, cc.xy(11, 1));
        upperBuilder.add(upperMountWebDavButton, cc.xy(11, 1));
        upperBuilder.add(problemButton, cc.xy(13, 1));

        upperPanel = upperBuilder.getPanel();
        upperPanel.setOpaque(false);
        if (type == ExpandableFolderModel.Type.Local) {
            upperPanel.setToolTipText(Translation
                .getTranslation("exp_folder_view.expand"));
        }
        upperPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        upperPanel.addMouseListener(moa);
        upperPanel.addMouseListener(mca);
        upperSyncLink.getUIComponent().addMouseListener(moa);
        upperInviteButton.addMouseListener(moa);
        upperOpenFilesButton.addMouseListener(moa);
        upperMountWebDavButton.addMouseListener(moa);

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
                cc.xy(5, row));

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
        openSettingsInformationAction = new MyOpenSettingsInformationAction(
            getController());
        MyMoveLocalFolderAction moveLocalFolderAction = new MyMoveLocalFolderAction(
            getController());
        openMembersInformationAction = new MyOpenMembersInformationAction(
            getController());
        mostRecentChangesAction = new MyMostRecentChangesAction(getController());
        clearCompletedDownloadsAction = new MyClearCompletedDownloadsAction(
            getController());
        openExplorerAction = new MyOpenExplorerAction(getController());
        removeFolderAction = new FolderRemoveAction(getController());
        removeFolderAction.allowWith(FolderRemovePermission.INSTANCE);

        backupOnlineStorageAction = new BackupOnlineStorageAction(
            getController());
        stopOnlineStorageAction = new StopOnlineStorageAction(getController());

        MyProblemAction myProblemAction = new MyProblemAction(getController());
        syncFolderAction = new MySyncFolderAction(getController());

        webdavAction = new WebdavAction(getController());

        expanded = new AtomicBoolean();
        mouseOver = new AtomicBoolean();

        osComponent = new OnlineStorageComponent(getController(), folder);

        primaryButton = new JButtonMini(Icons.getIconById(Icons.BLANK), "");
        primaryButton.addActionListener(new PrimaryButtonActionListener());
        openSettingsInformationButton = new JButtonMini(
            openSettingsInformationAction);

        openFilesInformationButton = new JButtonMini(openFilesInformationAction);
        upperOpenFilesButton = new JButtonMini(openFilesInformationAction);
        upperMountWebDavButton = new JButtonMini(webdavAction);

        inviteButton = new JButtonMini(inviteAction);
        upperInviteButton = new JButtonMini(inviteAction);

        problemButton = new JButtonMini(myProblemAction);
        upperSyncFolderButton = new SyncIconButtonMini(getController());
        upperSyncFolderButton
            .addActionListener(new PrimaryButtonActionListener());
        upperSyncFolderButton.setVisible(false);

        Icon pIcon = Icons.getIconById(Icons.LOCAL_FOLDER);
        Icon sIcon = Icons.getIconById(Icons.SYNC_ANIMATION[0]);
        if (pIcon.getIconHeight() > sIcon.getIconHeight()) {
            // HACK(tm) when mixing 16x16 sync icon with 24x24 icons
            upperSyncFolderButton.setBorder(Borders
                .createEmptyBorder("6, 6, 6, 6"));
        }

        lowerSyncFolderButton = new JButtonMini(syncFolderAction);
        upperSyncLink = new ActionLabel(getController(), syncFolderAction);
        upperSyncLink.setText("");

        upperSyncLink.getUIComponent().setVisible(!showing100Sync.get());
        upperInviteButton.setVisible(false);
        upperOpenFilesButton.setVisible(false);
        upperMountWebDavButton.setVisible(false);

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
        filesAvailableLabel = new ActionLabel(getController(),
            new MyFilesAvailableAction());
        deletedFilesLabel = new ActionLabel(getController(),
            new MyDeletedFilesAction());
        updateNumberOfFiles();
        updateStatsDetails();
        updateFolderMembershipDetails();
        updateTransferMode();
        updateLocalButtons();
        updateProblems();
        updatePermissions();

        registerListeners();
    }

    private void updatePermissions() {
        // Update permissions
        Permission folderAdmin = FolderPermission.admin(folderInfo);
        backupOnlineStorageAction.allowWith(folderAdmin);
        stopOnlineStorageAction.allowWith(folderAdmin);
        inviteAction.allowWith(folderAdmin);
    }

    private void updateLocalButtons() {
        boolean enabled = type == ExpandableFolderModel.Type.Local;

        openSettingsInformationButton.setEnabled(enabled);
        transferModeLabel.setEnabled(enabled);
        localDirectoryLabel.setEnabled(enabled);
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
        syncUpdater.schedule(new Runnable() {
            public void run() {
                if (type == ExpandableFolderModel.Type.Local) {
                    if (folder.isSyncing()) {
                        primaryButton.setVisible(false);
                        upperSyncFolderButton.setVisible(true);
                        upperSyncFolderButton.spin(folder.isSyncing());
                    } else {
                        upperSyncFolderButton.setVisible(false);
                        upperSyncFolderButton.spin(false);
                        primaryButton.setVisible(true);
                    }
                } else {
                    upperSyncFolderButton.setVisible(false);
                    upperSyncFolderButton.spin(false);
                    primaryButton.setVisible(true);
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
        String upperSyncPercent;
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
                        if (DateUtil.isDateMoreThanNDaysInFuture(date, 2)) {
                            syncDateText = Translation
                                .getTranslation("exp_folder_view.estimated_unknown");
                        } else {
                            String formattedDate = Format.formatDateShort(date);
                            syncDateText = Translation.getTranslation(
                                "exp_folder_view.estimated_synchronized",
                                formattedDate);
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
                    upperSyncPercent = syncPercentText;
                    showing100Sync.set(false);
                } else {
                    showing100Sync.set(Double.compare(sync, 100) == 0);
                    if (Double.compare(sync, UNKNOWN_SYNC_STATUS) == 0) {
                        if (folder.getConnectedMembersCount() >= 1) {
                            syncPercentText = Translation
                                .getTranslation("exp_folder_view.unsynchronized");
                            upperSyncPercent = syncPercentText;
                            syncPercentTip = Translation
                                .getTranslation("exp_folder_view.unsynchronized.tip");
                        } else {
                            syncPercentText = "";
                            upperSyncPercent = "";
                            syncPercentTip = "";
                        }
                    } else {
                        syncPercentText = Translation.getTranslation(
                            "exp_folder_view.synchronized",
                            Format.formatDecimal(sync));
                        upperSyncPercent = Format.formatDecimal(sync) + '%';
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
            } else {
                showing100Sync.set(false);
                syncPercentText = Translation
                    .getTranslation("exp_folder_view.not_yet_scanned");
                upperSyncPercent = "?";
                localSizeString = "?";
                totalSizeString = "?";
                filesAvailableLabelText = "";
            }
        } else {

            syncPercentText = Translation.getTranslation(
                "exp_folder_view.synchronized", "?");
            upperSyncPercent = "?";
            syncDateText = Translation.getTranslation(
                "exp_folder_view.last_synchronized", "?");
            localSizeString = "?";
            totalSizeString = "?";
            filesAvailableLabelText = "";
        }

        syncPercentLabel.setText(syncPercentText);
        syncPercentLabel.setToolTipText(syncPercentTip);
        upperSyncLink.setText(upperSyncPercent);
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
        // Maybe change visibility of upperSyncLink.
        updateUpperComponents();
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

    private void updateIconAndOS() {

        if (type == ExpandableFolderModel.Type.Local) {
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
        } else if (type == ExpandableFolderModel.Type.Typical) {
            primaryButton.setIcon(Icons.getIconById(Icons.SETTINGS));
            primaryButton.setToolTipText(Translation
                .getTranslation("exp_folder_view.folder_typical_text"));
            osComponent.getUIComponent().setVisible(false);
        } else { // CloudOnly
            primaryButton.setIcon(Icons.getIconById(Icons.ONLINE_FOLDER));
            primaryButton.setToolTipText(Translation
                .getTranslation("exp_folder_view.folder_online_text"));
            osComponent.getUIComponent().setVisible(
                PreferencesEntry.USE_ONLINE_STORAGE
                    .getValueBoolean(getController()));
        }

        if (folder != null && folder.isPreviewOnly()) {
            osComponent.getUIComponent().setVisible(false);
        } else {
            Boolean osComponentVisible = PreferencesEntry.USE_ONLINE_STORAGE
                .getValueBoolean(getController());
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
            contextMenu.add(removeFolderAction);
        } else {
            // Local folder popup
            contextMenu.add(openExplorerAction);
            contextMenu.addSeparator();
            contextMenu.add(syncFolderAction);
            contextMenu.add(openFilesInformationAction);
            contextMenu.add(mostRecentChangesAction);
            contextMenu.add(clearCompletedDownloadsAction);
            if (!getController().isBackupOnly()) {
                contextMenu.addSeparator();
                contextMenu.add(inviteAction);
                contextMenu.add(openMembersInformationAction);
            }
            contextMenu.addSeparator();
            contextMenu.add(openSettingsInformationAction);
            contextMenu.add(removeFolderAction);
            if (serverClient.isConnected() && serverClient.isLoggedIn()) {
                boolean osConfigured = serverClient.joinedByCloud(folder);
                if (osConfigured) {
                    contextMenu.add(stopOnlineStorageAction);
                } else {
                    contextMenu.add(backupOnlineStorageAction);
                }
            }
        }
        if (type == ExpandableFolderModel.Type.CloudOnly) {
            if (OSUtil.isWindows7System() || OSUtil.isWindowsVistaSystem()) {
                if (serverClient.isConnected() && folderInCloud()) {
                    contextMenu.add(webdavAction);
                }
            }
        }
        return contextMenu;
    }

    private void openExplorer() {
        FileUtils.openFile(folder.getCommitOrLocalDir());
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
                    Process process = Runtime.getRuntime().exec(
                        "net use * \"" + serverClient.getWebURL() + "/webdav/"
                            + folderInfo.getName() + "\" /User:"
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
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("exp_folder_view.webdav_success_title"),
                                Translation.getTranslation(
                                    "exp_folder_view.webdav_success_text",
                                    result.substring(1)),
                                GenericDialogType.INFO);
                        String[] parts = result.substring(1).split("\\s");
                        for (String part : parts) {
                            if (part.length() == 2 && part.charAt(1) == ':') {
                                // Probably the new drive name, so open it.
                                FileUtils.openFile(new File(part));
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
            doFolderChanges(folderEvent.getFolder());
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
                    updateUpperComponents();
                }
            });
        }

        public void mouseExited(MouseEvent e) {
            mouseOver.set(false);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateUpperComponents();
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
            Cursor c = upperPanel.getCursor();
            try {
                upperPanel.setCursor(Cursor
                    .getPredefinedCursor(Cursor.WAIT_CURSOR));
                createPopupMenu().show(evt.getComponent(), evt.getX(),
                    evt.getY());
            } finally {
                upperPanel.setCursor(c);
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (expanded.get()) {
                    collapse();
                } else {
                    expand();
                    if (type == ExpandableFolderModel.Type.Local
                        && getController().getUIController().isShowingFolder())
                    {
                        getController().getUIController().openFilesInformation(
                            folderInfo);
                    }
                    if (type == ExpandableFolderModel.Type.CloudOnly
                        && folderInfo != null)
                    {
                        PFWizard.openSingletonOnlineStorageJoinWizard(
                            getController(),
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

    private class MyOpenMembersInformationAction extends BaseAction {

        MyOpenMembersInformationAction(Controller controller) {
            super("action_open_members_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController()
                .openMembersInformation(folderInfo);
        }
    }

    // private class MyServerClientListener implements ServerClientListener {
    //
    // public void login(ServerClientEvent event) {
    // // updateIconAndOS();
    // }
    //
    // public void accountUpdated(ServerClientEvent event) {
    // // updateIconAndOS();
    // }
    //
    // public void serverConnected(ServerClientEvent event) {
    // // updateIconAndOS();
    // }
    //
    // public void serverDisconnected(ServerClientEvent event) {
    // // updateIconAndOS();
    // }
    //
    // public boolean fireInEventDispatchThread() {
    // return true;
    // }
    // }
    //
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

    private class MyFilesAvailableAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openFilesInformationIncoming(
                folderInfo);
        }
    }

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
            if (type == ExpandableFolderModel.Type.CloudOnly) {
                PFWizard.openSingletonOnlineStorageJoinWizard(getController(),
                    Collections.singletonList(folderInfo));
            } else if (type == ExpandableFolderModel.Type.Local
                && folder.isPreviewOnly())
            {
                // Local Preview
                SettingsTab.doPreviewChange(getController(), folder);
            } else if (type == ExpandableFolderModel.Type.Local) {
                // Local
                if (Desktop.isDesktopSupported()) {
                    openExplorer();
                }
            } else { // Typical
                askToCreateFolder();
            }
        }
    }

    private class WebdavAction extends BaseAction {
        private WebdavAction(Controller controller) {
            super("action_webdav", controller);
        }

        public void actionPerformed(ActionEvent e) {
            createWebdavConnection();
        }
    }

}
