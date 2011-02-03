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
package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.net.ConnectionHandlerFactory;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.net.IOProvider;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.notices.SimpleNotificationNotice;
import de.dal33t.powerfolder.ui.notices.RunnableNotice;
import de.dal33t.powerfolder.ui.notices.NoticeSeverity;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.LimitedConnectivityChecker;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.SyncIconButtonMini;
import de.dal33t.powerfolder.util.ui.SyncIconHelper;
import de.dal33t.powerfolder.util.ui.UIPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.util.ui.LimitedConnectivityChecker.CheckTask;

/**
 * The status bar on the lower side of the main window.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class StatusBar extends PFUIComponent implements UIPanel {

    private static final int UNKNOWN = -1;
    private static final int DISABLED = 0;
    private static final int CONNECTED = 1;
    private static final int DISCONNECTED = 2;

    private JComponent comp;
    private JButton onlineStateInfo;
    private JButton sleepButton;
    private SyncIconButtonMini syncButton;
    private JLabel portLabel;
    private JLabel networkModeLabel;
    private JButton openAboutBoxButton;
    private JButton openPreferencesButton;
    private JButton openStatsChartButton;
    private JButton openDebugButton;
    private JButton pendingMessagesButton;
    private boolean shownQualityWarningToday;
    private boolean shownLimitedConnectivityToday;

    /** Connection state */
    private final AtomicInteger state = new AtomicInteger(UNKNOWN);

    private DelayedUpdater syncUpdater;
    private DelayedUpdater connectLabelUpdater;
    /** TODO: Find a better place for systray icon setter */
    private SyncIconHelper syncHelper;

    protected StatusBar(Controller controller) {
        super(controller);
    }

    public Component getUIComponent() {

        if (comp == null) {

            boolean showPort = getController().getConnectionListener()
                .getPort() != ConnectionListener.DEFAULT_PORT;
            initComponents();

            String debugArea = "";
            if (ConfigurationEntry.VERBOSE.getValueBoolean(getController())) {
                debugArea = "pref, 3dlu, ";
            }

            String portArea = "";
            if (showPort) {
                portArea = "pref, 3dlu, ";
            }

            FormLayout mainLayout = new FormLayout(
                "1dlu, pref, 3dlu, pref, 3dlu, pref, center:pref:grow, pref, 3dlu, "
                    + portArea + debugArea + " pref, 3dlu, pref, 3dlu, pref, 1dlu",
                    "pref");
            DefaultFormBuilder mainBuilder = new DefaultFormBuilder(mainLayout);
            mainBuilder.setBorder(Borders.createEmptyBorder("3dlu, 0, 0, 0"));
            CellConstraints cc = new CellConstraints();

            int col = 2;
            mainBuilder.add(onlineStateInfo, cc.xy(col, 1));
            col += 2;
            mainBuilder.add(sleepButton, cc.xy(col, 1));
            col += 2;
            mainBuilder.add(syncButton, cc.xy(col, 1));
            col += 1;
            mainBuilder.add(networkModeLabel, cc.xy(col, 1));
            col += 1;
            mainBuilder.add(pendingMessagesButton, cc.xy(col, 1));
            col += 2;
            if (portArea.length() > 0) {
                mainBuilder.add(portLabel, cc.xy(col, 1));
                col += 2;
            }
            if (debugArea.length() > 0) {
                mainBuilder.add(openDebugButton, cc.xy(col, 1));
                col += 2;
            }
            mainBuilder.add(openStatsChartButton, cc.xy(col, 1));
            col += 2;
            mainBuilder.add(openPreferencesButton, cc.xy(col, 1));
            col += 2;
            mainBuilder.add(openAboutBoxButton, cc.xy(col, 1));

            comp = mainBuilder.getPanel();
            comp.setOpaque(false);
        }
        return comp;
    }

    public void showPendingMessages(boolean show) {
        pendingMessagesButton.setVisible(show);
    }

    private void initComponents() {
        syncUpdater = new DelayedUpdater(getController(), 1000L);
        connectLabelUpdater = new DelayedUpdater(getController());

        onlineStateInfo = new JButtonMini(Icons.getIconById(Icons.BLANK), "");

        configureConnectionLabels();

        // Add behavior
        onlineStateInfo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // open connect dialog
                if (getController().getNodeManager().isStarted()) {
                    getApplicationModel().getActionModel().getConnectAction()
                        .actionPerformed(
                            new ActionEvent(onlineStateInfo,
                                ActionEvent.ACTION_PERFORMED, "clicked"));
                } else if (!ProUtil.isRunningProVersion()) {
                    // Smells like hack(tm).
                    new FreeLimitationDialog(getController()).open();
                } else if (getApplicationModel().getLicenseModel()
                    .getActivationAction() != null)
                {
                    getApplicationModel().getLicenseModel()
                        .getActivationAction().actionPerformed(
                            new ActionEvent(onlineStateInfo,
                                ActionEvent.ACTION_PERFORMED, "clicked"));
                }
            }
        });

        sleepButton = new JButtonMini(Icons.getIconById(Icons.PAUSE),
            Translation.getTranslation("status_bar.sleep.tips"));
        MyActionListener listener = new MyActionListener();
        sleepButton.addActionListener(listener);

        getController().addPropertyChangeListener(
            Controller.PROPERTY_SILENT_MODE, new MyValueChangeListener());
        updateSilentMode();

        // Behavior when the limited connecvitiy gets checked
        getController().addPropertyChangeListener(
            Controller.PROPERTY_LIMITED_CONNECTIVITY,
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    // EventQueue because property change not fired in EDT.
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            updateLimitedConnectivityLabel();
                        }
                    });
                }
            });

        syncButton = new SyncIconButtonMini(getController());
        syncButton.addActionListener(listener);
        updateSyncButton();
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());

        // FIND a better place
        syncHelper = new SyncIconHelper(getController());

        portLabel = new JLabel(Translation.getTranslation("status.port.text",
            String.valueOf(getController().getConnectionListener().getPort())));
        portLabel.setToolTipText(Translation.getTranslation("status.port.tip"));

        networkModeLabel = new JLabel("nwm");

        openStatsChartButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenStatsChartsAction());
        openPreferencesButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenPreferencesAction());
        openAboutBoxButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenAboutBoxAction());
        openDebugButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenDebugInformationAction());

        pendingMessagesButton = new JButtonMini(Icons
            .getIconById(Icons.CHAT_PENDING), Translation
            .getTranslation("status.chat_pending"));
        pendingMessagesButton.addActionListener(listener);
        showPendingMessages(false);
    }

    private void configureConnectionLabels() {
        NodeManagerListener nodeListener = new NodeManagerAdapter() {
            public void nodeConnected(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public void nodeDisconnected(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public void startStop(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public boolean fireInEventDispatchThread() {
                return true;
            }
        };

        // Add behavior
        getController().getNodeManager().addNodeManagerListener(nodeListener);

        updateConnectionLabels();
    }

    private void updateLimitedConnectivityLabel() {
        if (getController().isLimitedConnectivity()
            && !shownLimitedConnectivityToday)
        {
            shownLimitedConnectivityToday = true;
            showLimitedConnectivityWarning(getController());
        }
    }

    private static void showLimitedConnectivityWarning(
        final Controller controller)
    {
        Boolean warn = PreferencesEntry.TEST_CONNECTIVITY
            .getValueBoolean(controller);
        if (warn) {
            // Advise user of limited connectivity.
            Runnable runnable = new Runnable() {
                public void run() {
                    controller.getThreadPool().execute(
                        new CheckTask(controller));
                    LimitedConnectivityChecker
                        .showConnectivityWarning(controller);
                }
            };
            RunnableNotice notice = new RunnableNotice(Translation
                .getTranslation("warning_notice.title"), Translation
                .getTranslation("warning_notice.limited_connectivity"),
                runnable, NoticeSeverity.WARINING);
            controller.getUIController().getApplicationModel()
                .getNoticesModel().addNotice(notice);
        }
    }

    private void updateConnectionLabels() {
        connectLabelUpdater.schedule(new Runnable() {
            public void run() {
                updateConnectionLabels0();
            }
        });
    }

    private void updateConnectionLabels0() {
        Controller controller = getController();
        IOProvider ioProvider = controller.getIOProvider();
        Icon connectionQualityIcon = null;
        String connectionQualityText = null;
        if (ioProvider != null) {
            ConnectionHandlerFactory factory = ioProvider
                .getConnectionHandlerFactory();
            if (factory != null) {
                ConnectionQuality quality = factory.getConnectionQuality();
                if (quality != null) {
                    switch (quality) {
                        case GOOD :
                            connectionQualityIcon = Icons
                                .getIconById(Icons.CONNECTION_GOOD);
                            connectionQualityText = Translation
                                .getTranslation("connection_quality_good.text");
                            break;
                        case MEDIUM :
                            connectionQualityIcon = Icons
                                .getIconById(Icons.CONNECTION_MEDIUM);
                            connectionQualityText = Translation
                                .getTranslation("connection_quality_medium.text");
                            break;
                        case POOR :
                            connectionQualityIcon = Icons
                                .getIconById(Icons.CONNECTION_POOR);
                            connectionQualityText = Translation
                                .getTranslation("connection_quality_poor.text");

                            // Only show warning once!
                            if (!shownQualityWarningToday) {
                                shownQualityWarningToday = true;
                                showQualityWarning(controller);
                            }
                            break;
                    }
                }
            }
        }
        if (connectionQualityIcon == null) {
            connectionQualityIcon = Icons.getIconById(Icons.BLANK);
        }

        // Get connected node count
        int nOnlineUser = controller.getNodeManager().countConnectedNodes();

        int newState;

        if (!controller.getNodeManager().isStarted()) {
            // Disabled
            onlineStateInfo.setToolTipText(Translation
                .getTranslation("online_label.disabled"));
            onlineStateInfo.setIcon(Icons.getIconById(Icons.WARNING));
            newState = DISABLED;
        } else if (nOnlineUser > 0) {
            if (connectionQualityText == null) {
                // No connection quality indication yet - just show connected.
                String text = Translation.getTranslation("online_label.online");
                if (controller.isLanOnly()) {
                    text += " ("
                        + Translation
                            .getTranslation("general.network_mode.lan_only")
                        + ')';
                } else if (controller.getNetworkingMode() == NetworkingMode.SERVERONLYMODE
                    && !ConfigurationEntry.BACKUP_ONLY_CLIENT
                        .getValueBoolean(getController()))
                {
                    text += " ("
                        + Translation
                            .getTranslation("general.network_mode.server_only")
                        + ')';
                }
                onlineStateInfo.setToolTipText(text);
                onlineStateInfo.setIcon(Icons.getIconById(Icons.DISCONNECTED));
            } else {
                onlineStateInfo.setIcon(connectionQualityIcon);
                onlineStateInfo.setToolTipText(connectionQualityText);
            }
            newState = CONNECTED;
        } else {
            // Connecting
            String text = Translation.getTranslation("online_label.connecting");
            if (controller.isLanOnly()) {
                text += " ("
                    + Translation
                        .getTranslation("general.network_mode.lan_only") + ')';
            } else if (controller.getNetworkingMode() == NetworkingMode.SERVERONLYMODE
                && !ConfigurationEntry.BACKUP_ONLY_CLIENT
                    .getValueBoolean(getController()))
            {
                text += " ("
                    + Translation
                        .getTranslation("general.network_mode.server_only")
                    + ')';
            }
            onlineStateInfo.setToolTipText(text);
            onlineStateInfo.setIcon(Icons.getIconById(Icons.DISCONNECTED));
            newState = DISCONNECTED;
        }

        if (!PreferencesEntry.SHOW_SYSTEM_NOTIFICATIONS
            .getValueBoolean(getController()))
        {
            return;
        }

        synchronized (state) {

            int oldState = state.getAndSet(newState);
            if (oldState != newState) {
                // State changed, notify ui.
                String notificationText;
                String title = Translation
                    .getTranslation("status_bar.status_change.title");
                if (newState == DISABLED) {
                    notificationText = Translation
                        .getTranslation("status_bar.status_change.disabled");
                    getApplicationModel().getNoticesModel().handleNotice(
                        new SimpleNotificationNotice(title, notificationText));
                } else if (newState == CONNECTED) {
                    notificationText = Translation
                        .getTranslation("status_bar.status_change.connected");
                    getApplicationModel().getNoticesModel().handleNotice(
                        new SimpleNotificationNotice(title, notificationText));
                } else {
                    // Disconnected
                }
            }
        }

    }

    private static void showQualityWarning(final Controller controller) {
        Boolean warn = PreferencesEntry.WARN_POOR_QUALITY
            .getValueBoolean(controller);
        if (warn) {
            // Advise user of quality issue.
            Runnable runnable = new Runnable() {
                public void run() {
                    String wikiLink = Help.getWikiArticleURL(controller,
                        WikiLinks.LIMITED_CONNECTIVITY);
                    String infoText = Translation.getTranslation(
                        "status_bar.poor_quality_warning.text", wikiLink);
                    NeverAskAgainResponse response = DialogFactory
                        .genericDialog(
                            controller,
                            Translation
                                .getTranslation("status_bar.poor_quality_warning.title"),
                            infoText, new String[]{Translation
                                .getTranslation("general.ok")}, 0,
                            GenericDialogType.INFO, Translation
                                .getTranslation("general.neverAskAgain"));
                    if (response.isNeverAskAgain()) {
                        PreferencesEntry.WARN_POOR_QUALITY.setValue(controller,
                            false);
                    }
                }
            };

            RunnableNotice notice = new RunnableNotice(Translation
                .getTranslation("warning_notice.title"), Translation
                .getTranslation("warning_notice.poor_quality"), runnable,
                    NoticeSeverity.WARINING);
            controller.getUIController().getApplicationModel()
                .getNoticesModel().addNotice(notice);
        }
    }

    public static JLabel createTransferCounterLabel(Controller controller,
        final Icon icon, final String format, final TransferCounter tc,
        final String toolTip)
    {
        final JLabel label = new JLabel();
        // Create task which updates the counter each second
        controller.scheduleAndRepeat(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        label.setIcon(icon);
                        label.setText(String.format(format, tc
                            .calculateCurrentKBS()));
                        label.setToolTipText(toolTip);
                    }
                });
            }
        }, 0, 1000);
        return label;
    }

    public void setNetworkingModeStatus(NetworkingMode networkingMode) {
        if (networkingMode == NetworkingMode.LANONLYMODE) {
            networkModeLabel.setText(Translation
                .getTranslation("general.network_mode.lan_only"));
        } else if (networkingMode == NetworkingMode.SERVERONLYMODE
            && !ConfigurationEntry.BACKUP_ONLY_CLIENT
                .getValueBoolean(getController()))
        {
            networkModeLabel.setText(Translation
                .getTranslation("general.network_mode.server_only"));
        } else {
            networkModeLabel.setText("");
        }
    }

    private void updateSyncButton() {
        syncUpdater.schedule(new Runnable() {
            public void run() {
                boolean anySynchronizing = false;
                for (Folder folder : getController().getFolderRepository()
                    .getFolders(true))
                {
                    if (folder.isSyncing()) {
                        anySynchronizing = true;
                        break;
                    }
                }
                syncButton.setVisible(anySynchronizing);
                syncButton.spin(anySynchronizing);
                syncHelper.setVisible(anySynchronizing);
            }
        });
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateSyncButton();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateSyncButton();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MyTransferManagerListener extends TransferManagerAdapter {

        private void updateIfRequired(TransferManagerEvent event) {
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

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == sleepButton) {
                getController().setSilentMode(!getController().isSilentMode());
            } else if (e.getSource() == pendingMessagesButton) {
                getController().getUIController().openChat(null);
            } else if (e.getSource() == syncButton) {
                syncAllFolders();
            }
        }
    }

    private void syncAllFolders() {
        for (Folder folder : getController().getFolderRepository().getFolders(
            true))
        {
            if (folder.isPreviewOnly()) {
                continue;
            }
            getController().getUIController().syncFolder(folder);
        }
    }

    private class MyValueChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            // Move into EDT. Property change event might be called from
            // anywhere, not just from EDT.
            UIUtil.invokeLaterInEDT(new Runnable() {
                public void run() {
                    updateSilentMode();
                }
            });
        }
    }

    private void updateSilentMode() {
        if (getController().isSilentMode()) {
            sleepButton.setIcon(Icons.getIconById(Icons.RUN));
            sleepButton.setToolTipText(Translation
                .getTranslation("status_bar.no_sleep.tips"));
        } else {
            sleepButton.setIcon(Icons.getIconById(Icons.PAUSE));
            sleepButton.setToolTipText(Translation
                .getTranslation("status_bar.sleep.tips"));
        }
    }
}
