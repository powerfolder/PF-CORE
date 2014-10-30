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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.net.ConnectionHandlerFactory;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.net.IOProvider;
import de.dal33t.powerfolder.ui.model.NoticesModel;
import de.dal33t.powerfolder.ui.notices.AskForFriendshipEventNotice;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.NoticeSeverity;
import de.dal33t.powerfolder.ui.notices.RunnableNotice;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.*;
import de.dal33t.powerfolder.ui.util.LimitedConnectivityChecker.CheckTask;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;

/**
 * The status bar on the lower side of the main window.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class StatusBar extends PFUIComponent {

    private JComponent comp;
    private JButton compactModeButton;
    private SyncIconButtonMini syncButton;
    private JLabel portLabel;
    private JLabel networkModeLabel;
    private JButton newNoticesButton;
    //private JButton openAboutBoxButton;
    private JButton openPreferencesButton;
    //private JButton openStatsChartButton;
    private JButton openDebugButton;
    private JButton pendingMessagesButton;
    private boolean shownQualityWarningToday;
    private boolean shownLimitedConnectivityToday;

    private DelayedUpdater syncUpdater;
    private DelayedUpdater checkQualityUpdater;
    private NoticesModel noticeModel;

    protected StatusBar(Controller controller) {
        super(controller);
        noticeModel = getApplicationModel().getNoticesModel();
        noticeModel.getAllNoticesCountVM().addValueChangeListener(
            new MyNoticesListener());
        noticeModel.getUnreadNoticesCountVM().addValueChangeListener(
            new MyNoticesListener());
    }

    public Component getUIComponent() {

        if (comp == null) {

            boolean showPort = getController().getConnectionListener()
                .getPort() != ConnectionListener.DEFAULT_PORT;
            initComponents();

            String debugArea = "";
            if (getController().isVerbose()) {
                debugArea = "pref, 3dlu, ";
            }

            String portArea = "";
            if (showPort) {
                portArea = "pref, 3dlu, ";
            }

            FormLayout mainLayout = new FormLayout(
                "1dlu, pref, 3dlu, pref, center:pref:grow, pref, 3dlu, "
                    + portArea
                    + debugArea
                    + " pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu", "pref");
            DefaultFormBuilder mainBuilder = new DefaultFormBuilder(mainLayout);
            mainBuilder.setBorder(Borders.createEmptyBorder("3dlu, 0, 0, 0"));
            CellConstraints cc = new CellConstraints();

            int col = 2;
            mainBuilder.add(syncButton, cc.xy(col, 1));
            col += 2;
            mainBuilder.add(newNoticesButton, cc.xy(col, 1));
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
//            mainBuilder.add(openStatsChartButton, cc.xy(col, 1));
//            col += 2;
            mainBuilder.add(openPreferencesButton, cc.xy(col, 1));
//            col += 2;
//            mainBuilder.add(openAboutBoxButton, cc.xy(col, 1));
            if (Feature.COMPACT_MODE.isEnabled()) {
                col += 2;
                mainBuilder.add(compactModeButton, cc.xy(col, 1));
            }

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
        checkQualityUpdater = new DelayedUpdater(getController());

        MyActionListener listener = new MyActionListener();

        compactModeButton = new JButtonMini(Icons.getIconById(Icons.COMPACT),
                Translation.getTranslation("status_bar.compact.tips"));
        compactModeButton.addActionListener(listener);


        configureConnectionLabels();

        // Behavior when the limited connecvitiy gets checked
        getController().addLimitedConnectivityListener(
                new MyLimitedConnectivityListener());

        syncButton = new SyncIconButtonMini(getController());
        syncButton.addActionListener(listener);
        updateSyncButton();
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());

        portLabel = new JLabel(Translation.getTranslation("status.port.text",
            String.valueOf(getController().getConnectionListener().getPort())));
        portLabel.setToolTipText(Translation.getTranslation("status.port.tip"));

        newNoticesButton = new JButtonMini(getApplicationModel()
            .getActionModel().getViewNoticesAction());
        newNoticesButton.setText(null);

        networkModeLabel = new JLabel("nwm");

//        openStatsChartButton = new JButtonMini(getApplicationModel()
//            .getActionModel().getOpenStatsChartsAction());
//        openStatsChartButton.setVisible(getController().isVerbose());
        openPreferencesButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenPreferencesAction());
//        openAboutBoxButton = new JButtonMini(getApplicationModel()
//            .getActionModel().getOpenAboutBoxAction());
        openDebugButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenDebugInformationAction());

        pendingMessagesButton = new JButtonMini(
            Icons.getIconById(Icons.CHAT_PENDING),
            Translation.getTranslation("status.chat_pending"));
        pendingMessagesButton.addActionListener(listener);
        showPendingMessages(false);
        updateNewNoticesText();
    }

    private void updateNewNoticesText() {

        int unread = (Integer) noticeModel.getUnreadNoticesCountVM().getValue();

        newNoticesButton.setVisible(unread != 0);

        if (unread != 0) {
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
            newNoticesButton.setIcon(noticesIcon);

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
            newNoticesButton.setToolTipText(noticesText);
        }
    }

    private void configureConnectionLabels() {
        // Add behavior
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeListener());
        getController().getOSClient().addListener(new MyServerClientListener());

        checkQuality();
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
            RunnableNotice notice = new RunnableNotice(
                Translation.getTranslation("warning_notice.title"),
                Translation
                    .getTranslation("warning_notice.limited_connectivity"),
                runnable, NoticeSeverity.WARINING);
            controller.getUIController().getApplicationModel()
                .getNoticesModel().handleNotice(notice);
        }
    }

    private void checkQuality() {
        checkQualityUpdater.schedule(new Runnable() {
            public void run() {
                checkQuality0();
            }
        });
    }

    private void checkQuality0() {
        Controller controller = getController();
        IOProvider ioProvider = controller.getIOProvider();
        if (ioProvider != null) {
            ConnectionHandlerFactory factory = ioProvider
                .getConnectionHandlerFactory();
            if (factory != null) {
                ConnectionQuality quality = factory.getConnectionQuality();
                if (quality != null) {
                    if (quality == ConnectionQuality.POOR) {
                        // Only show warning once!
                        if (!shownQualityWarningToday) {
                            shownQualityWarningToday = true;
                            showQualityWarning(controller);
                        }
                    }
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

            RunnableNotice notice = new RunnableNotice(
                Translation.getTranslation("warning_notice.title"),
                Translation.getTranslation("warning_notice.poor_quality"),
                runnable, NoticeSeverity.WARINING);
            controller.getUIController().getApplicationModel()
                .getNoticesModel().handleNotice(notice);
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
                        label.setText(String.format(format,
                            tc.calculateCurrentKBS()));
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
            && !getController().isBackupOnly())
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
            }
        });
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyNodeListener extends NodeManagerAdapter {
        public void nodeConnected(NodeManagerEvent e) {
            checkQuality();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            checkQuality();
        }

        public void startStop(NodeManagerEvent e) {
            checkQuality();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyServerClientListener implements ServerClientListener {
        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
            checkQuality();
        }

        public void accountUpdated(ServerClientEvent event) {
            checkQuality();
        }
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
            if (e.getSource() == pendingMessagesButton) {
                getUIController().openChat(null);
            } else if (e.getSource() == syncButton) {
                syncAllFolders();
            } else if (e.getSource() == compactModeButton) {
                getUIController().reconfigureForCompactMode(true);
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
            getApplicationModel().syncFolder(folder);
        }
    }

    private class MyNoticesListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            updateNewNoticesText();
        }
    }


    private class MyLimitedConnectivityListener implements LimitedConnectivityListener {
        public void setLimitedConnectivity(LimitedConnectivityEvent event) {
            updateLimitedConnectivityLabel();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}
