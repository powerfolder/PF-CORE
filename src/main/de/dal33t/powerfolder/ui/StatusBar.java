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
import java.awt.Dimension;
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
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.WarningEvent;
import de.dal33t.powerfolder.net.*;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.*;

/**
 * The status bar on the lower side of the main window.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class StatusBar extends PFUIComponent implements UIPanel {

    private static final int UNKNOWN = -1;
    private static final int DISABLED = 0;
    private static final int CONNECTED = 1;
    private static final int DISCONNECTED = 2;

    private Component comp;
    private final JLabel onlineStateInfo = new JLabel();
    private JButton sleepButton;
    private JLabel limitedConnectivityLabel;
    private JLabel upStats;
    private JLabel downStats;
    private JLabel portLabel;
    private JButton openAboutBoxButton;
    private JButton openPreferencesButton;
    private JButton openDebugButton;
    private JButton pendingMessagesButton;
    private boolean shownQualityWarningToday;

    /** Connection state */
    private final AtomicInteger state = new AtomicInteger(UNKNOWN);

    protected StatusBar(Controller controller) {
        super(controller);
    }

    public Component getUIComponent() {

        if (comp == null) {

            boolean showPort = ConfigurationEntry.NET_BIND_RANDOM_PORT
                .getValueBoolean(getController())
                && getController().getConnectionListener().getPort() != ConnectionListener.DEFAULT_PORT;
            initComponents();

            CellConstraints cc = new CellConstraints();

            // Upper section

            String showPortArea = "";
            if (showPort) {
                showPortArea = "pref, 3dlu, pref, 3dlu, ";
            }

            FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, fill:pref:grow, " + showPortArea + " pref, 3dlu, pref",
                "pref");
            DefaultFormBuilder upperBuilder = new DefaultFormBuilder(upperLayout);
            int col = 1;
            upperBuilder.add(sleepButton, cc.xy(col, 1));
            col +=2;
            upperBuilder.add(pendingMessagesButton, cc.xy(col, 1));
            col +=2;
            if (showPort) {
                upperBuilder.add(portLabel, cc.xy(col, 1));
                col += 2;

                JSeparator sep0 = new JSeparator(SwingConstants.VERTICAL);
                sep0.setPreferredSize(new Dimension(2, 12));

                upperBuilder.add(sep0, cc.xy(col, 1));
                col += 2;
            }

            upperBuilder.add(openPreferencesButton, cc.xy(col, 1));
            col += 2;
            upperBuilder.add(openAboutBoxButton, cc.xy(col, 1));

            // Lower section

            String showDebugArea = "";
            if (ConfigurationEntry.VERBOSE.getValueBoolean(getController())) {
                showDebugArea = "pref, 3dlu, ";
            }

            FormLayout lowerLayout = new FormLayout(
            // debug online limit sep down sep up
                showDebugArea
                    + "pref, 3dlu, pref, fill:pref:grow, pref, 3dlu, pref, 3dlu, pref", "pref");
            DefaultFormBuilder lowerBuilder = new DefaultFormBuilder(
                lowerLayout);

            col = 1;

            if (ConfigurationEntry.VERBOSE.getValueBoolean(getController())) {
                lowerBuilder.add(openDebugButton, cc.xy(col, 1));
                col += 2;
            }

            lowerBuilder.add(onlineStateInfo, cc.xy(col, 1));
            col += 2;

            lowerBuilder.add(limitedConnectivityLabel, cc.xy(col, 1));
            col += 2;

            JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
            sep1.setPreferredSize(new Dimension(2, 12));

            lowerBuilder.add(downStats, cc.xy(col, 1));
            col += 2;
            lowerBuilder.add(sep1, cc.xy(col, 1));
            col += 2;
            lowerBuilder.add(upStats, cc.xy(col, 1));

            // Main section

            FormLayout mainLayout = new FormLayout(
                "1dlu, fill:pref:grow, 1dlu", "pref, 3dlu, pref, 1dlu");
            DefaultFormBuilder mainBuilder = new DefaultFormBuilder(mainLayout);
            mainBuilder.add(upperBuilder.getPanel(), cc.xy(2, 1));
            mainBuilder.add(lowerBuilder.getPanel(), cc.xy(2, 3));
            comp = mainBuilder.getPanel();
        }
        return comp;
    }

    public void showPendingMessages(boolean show) {
        pendingMessagesButton.setVisible(show);
    }

    private void initComponents() {

        configureConnectionLabels();

        // Add behavior
        onlineStateInfo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // open connect dialog
                if (getController().getNodeManager().isStarted()) {
                    getApplicationModel().getActionModel().getConnectAction()
                        .actionPerformed(null);
                } else if (!Util.isRunningProVersion()) {
                    // Smells like hack(tm).
                    new FreeLimitationDialog(getController()).open();
                }
            }
        });

        sleepButton = new JButtonMini(Icons.getIconById(Icons.SLEEP),
            Translation.getTranslation("status_bar.sleep.tips"));
        MyActionListener listener = new MyActionListener();
        sleepButton.addActionListener(listener);

        getController().addPropertyChangeListener(
            Controller.PROPERTY_SILENT_MODE, new MyValueChangeListener());

        upStats = createTransferCounterLabel(getController(), Icons
            .getIconById(Icons.UPLOAD), Translation
            .getTranslation("status.upload"), getController()
            .getTransferManager().getUploadCounter(), Translation
            .getTranslation("status.upload.text"));
        upStats.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                getUIController().openUploadsInformation();
            }
        });

        downStats = createTransferCounterLabel(getController(), Icons
            .getIconById(Icons.DOWNLOAD), Translation
            .getTranslation("status.download"), getController()
            .getTransferManager().getDownloadCounter(), Translation
            .getTranslation("status.download.text"));
        downStats.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                getUIController().openDownloadsInformation();
            }
        });

        limitedConnectivityLabel = new JLabel();
        limitedConnectivityLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (getController().isLimitedConnectivity()) {
                    getController().getThreadPool().execute(
                        new LimitedConnectivityChecker.CheckTask(
                            getController(), false));
                    // Directly show dialog, not after check! may take up to 30
                    // seconds.poü
                    LimitedConnectivityChecker
                        .showConnectivityWarning(getController());
                }
            }
        });

        // Behaviour when the limited connecvitiy gets checked
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

        portLabel = new JLabel(Translation.getTranslation("status.port.text",
                getController().getConnectionListener().getPort()));
        portLabel
            .setToolTipText(Translation.getTranslation("status.port.tip"));

        openPreferencesButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenPreferencesAction());
        openAboutBoxButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenAboutBoxAction());
        openDebugButton = new JButtonMini(getApplicationModel()
            .getActionModel().getOpenDebugInformationAction());

        pendingMessagesButton = new JButtonMini(Icons.getIconById(Icons.CHAT_PENDING),
                Translation.getTranslation("status.chat_pending"));
        pendingMessagesButton.addActionListener(listener);
        showPendingMessages(false);
    }

    private void configureConnectionLabels() {
        NodeManagerListener nodeListener = new NodeManagerListener() {
            public void friendAdded(NodeManagerEvent e) {
            }

            public void friendRemoved(NodeManagerEvent e) {
            }

            public void nodeAdded(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public void nodeConnected(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public void nodeDisconnected(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public void nodeRemoved(NodeManagerEvent e) {
                updateConnectionLabels();
            }

            public void settingsChanged(NodeManagerEvent e) {
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
        if (getController().isLimitedConnectivity()) {
            limitedConnectivityLabel.setToolTipText(Translation
                .getTranslation("limited_connection.title"));
            limitedConnectivityLabel.setIcon(Icons.getIconById(Icons.WARNING));
        } else {
            limitedConnectivityLabel.setText("");
            limitedConnectivityLabel.setIcon(null);
        }
    }

    private void updateConnectionLabels() {
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
                            connectionQualityIcon = Icons.getIconById(Icons.CONNECTION_GOOD);
                            connectionQualityText = Translation
                                .getTranslation("connection_quality_good.text");
                            break;
                        case MEDIUM :
                            connectionQualityIcon = Icons.getIconById(Icons.CONNECTION_MEDIUM);
                            connectionQualityText = Translation
                                .getTranslation("connection_quality_medium.text");
                            break;
                        case POOR :
                            connectionQualityIcon = Icons.getIconById(Icons.CONNECTION_POOR);
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
                    text += " (" + Translation.getTranslation("general.lan_only")
                        + ')';
                } else if (controller.getNetworkingMode() == NetworkingMode.SERVERONLYMODE)
                {
                    text += " ("
                        + Translation.getTranslation("general.server_only") + ')';
                }
                onlineStateInfo.setToolTipText(text);
                onlineStateInfo.setIcon(Icons.getIconById(Icons.CONNECTED));
            } else {
                onlineStateInfo.setIcon(connectionQualityIcon);
                onlineStateInfo.setToolTipText(connectionQualityText);
            }
            newState = CONNECTED;
        } else {
            // Connecting
            String text = Translation.getTranslation("online_label.connecting");
            if (controller.isLanOnly()) {
                text += " (" + Translation.getTranslation("general.lan_only")
                    + ')';
            } else if (controller.getNetworkingMode() == NetworkingMode.SERVERONLYMODE)
            {
                text += " ("
                    + Translation.getTranslation("general.server_only") + ')';
            }
            onlineStateInfo.setToolTipText(text);
            onlineStateInfo.setIcon(Icons.getIconById(Icons.DISCONNECTED));
            newState = DISCONNECTED;
        }

        if (!ConfigurationEntry.SHOW_SYSTEM_NOTIFICATIONS
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
                    getUIController().notifyMessage(title, notificationText,
                        false);
                } else if (newState == CONNECTED) {
                    notificationText = Translation
                        .getTranslation("status_bar.status_change.connected");
                    getUIController().notifyMessage(title, notificationText,
                        false);
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
                    NeverAskAgainResponse response = DialogFactory
                        .genericDialog(
                            controller,
                            Translation
                                .getTranslation("status_bar.poor_quality_warning.title"),
                            Translation
                                .getTranslation("status_bar.poor_quality_warning.text"),
                            new String[]{Translation
                                .getTranslation("general.ok")}, 0,
                            GenericDialogType.INFO, Translation
                                .getTranslation("general.neverAskAgain"));
                    if (response.isNeverAskAgain()) {
                        PreferencesEntry.WARN_POOR_QUALITY.setValue(controller,
                            false);
                    }
                }
            };
            WarningEvent warningEvent = new WarningEvent(runnable);
            controller.getUIController().getApplicationModel()
                .getWarningsModel().pushWarning(warningEvent);
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

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == sleepButton) {
                getController().setSilentMode(!getController().isSilentMode());
            } else if (e.getSource() == pendingMessagesButton) {
                getController().getUIController().openChat(null);
            }
        }
    }

    private class MyValueChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            // Move into EDT. Property change event might be called from
            // anywhere, not just from EDT.
            UIUtil.invokeLaterInEDT(new Runnable() {
                public void run() {
                    if (getController().isSilentMode()) {
                        sleepButton.setIcon(Icons.getIconById(Icons.WAKE_UP));
                        sleepButton.setToolTipText(Translation
                            .getTranslation("status_bar.no_sleep.tips"));
                    } else {
                        sleepButton.setIcon(Icons.getIconById(Icons.SLEEP));
                        sleepButton.setToolTipText(Translation
                            .getTranslation("status_bar.sleep.tips"));
                    }
                }
            });
        }
    }
}
