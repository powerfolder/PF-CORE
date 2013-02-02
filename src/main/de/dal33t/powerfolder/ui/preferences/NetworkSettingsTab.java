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
package de.dal33t.powerfolder.ui.preferences;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.panel.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.UDTSocket;

public class NetworkSettingsTab extends PFComponent implements PreferenceTab {
    private JPanel panel;
    private JComboBox networkingMode;
    private JCheckBox relayedConnectionBox;
    private JCheckBox udtConnectionBox;
    private LineSpeedSelectionPanel wanSpeed;
    private LineSpeedSelectionPanel lanSpeed;
    private boolean needsRestart;
    private JButton httpProxyButton;
    private JComboBox serverDisconnectBehaviorBox;

    public NetworkSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.network.title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        return true;
    }

    private void initComponents() {
        String[] options = new String[NetworkingMode.values().length];
        options[NetworkingMode.PRIVATEMODE.ordinal()] = Translation
            .getTranslation("general.network_mode.private");
        options[NetworkingMode.LANONLYMODE.ordinal()] = Translation
            .getTranslation("general.network_mode.lan_only");
        options[NetworkingMode.SERVERONLYMODE.ordinal()] = Translation
            .getTranslation("general.network_mode.server_only");
        networkingMode = new JComboBox(options);

        NetworkingMode currentNetworkingMode = getController()
            .getNetworkingMode();
        String tooltip = getTooltip(currentNetworkingMode);
        networkingMode.setSelectedIndex(currentNetworkingMode.ordinal());
        networkingMode.setToolTipText(tooltip);
        networkingMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NetworkingMode selectedNetworkingMode = NetworkingMode.values()[networkingMode
                    .getSelectedIndex()];
                networkingMode.setToolTipText(getTooltip(selectedNetworkingMode));
                enableDisableComponents(NetworkingMode.LANONLYMODE == selectedNetworkingMode);
            }
        });

        relayedConnectionBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.dialog.use.relayed.connections"));
        relayedConnectionBox
            .setSelected(ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED
                .getValueBoolean(getController()));

        udtConnectionBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.dialog.use.udt.connections"));
        udtConnectionBox.setSelected(ConfigurationEntry.UDT_CONNECTIONS_ENABLED
            .getValueBoolean(getController()));

        HttpProxyAction action  = new HttpProxyAction(getController());
        httpProxyButton = new JButton(action);


        wanSpeed = new LineSpeedSelectionPanel(getController(), true, true);

        lanSpeed = new LineSpeedSelectionPanel(getController(), false, true);

        enableDisableComponents(getController().isLanOnly());
        

        TransferManager tm = getController().getTransferManager();

        wanSpeed.setSpeedKBPS(
                ConfigurationEntry.TRANSFER_LIMIT_AUTODETECT.getValueBoolean(
                        getController()), tm.getUploadCPSForWAN() / 1024,
            tm.getDownloadCPSForWAN() / 1024);

        lanSpeed.setSpeedKBPS(false, tm.getUploadCPSForLAN() / 1024,
            tm.getDownloadCPSForLAN() / 1024);

        options = new String[]{
            Translation
                .getTranslation("preferences.dialog.server_disconnect.sync"),
            Translation
                .getTranslation("preferences.dialog.server_disconnect.nosync")};
        serverDisconnectBehaviorBox = new JComboBox(options);
        int selected = ConfigurationEntry.SERVER_DISCONNECT_SYNC_ANYWAYS
            .getValueBoolean(getController()) ? 0 : 1;
        serverDisconnectBehaviorBox.setSelectedIndex(selected);
        serverDisconnectBehaviorBox.setToolTipText(String
            .valueOf(serverDisconnectBehaviorBox.getSelectedItem()));
        serverDisconnectBehaviorBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                serverDisconnectBehaviorBox.setToolTipText(String
                    .valueOf(serverDisconnectBehaviorBox.getSelectedItem()));
            }
        });
    }

    private void enableDisableComponents(boolean lanOnly) {
        relayedConnectionBox.setEnabled(!lanOnly);
        udtConnectionBox.setEnabled(!lanOnly && UDTSocket.isSupported());
        udtConnectionBox.setVisible(UDTSocket.isSupported());
        wanSpeed.setEnabled(!lanOnly);
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout;
            if (getController().isBackupOnly()) {
                layout = new FormLayout(
                        "right:pref, 3dlu, 140dlu, pref:grow",
                        "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 6dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
            } else {
                // Extra pref for useOnlineStorageCB.
                layout = new FormLayout(
                        "right:pref, 3dlu, 140dlu, pref:grow",
                        "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 6dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
            }
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.network_mode.name"), cc.xy(
                1, row));
            builder.add(networkingMode, cc.xy(3, row));

            row += 2;
            builder.add(relayedConnectionBox, cc.xy(3, row));

            row += 2;
            builder.add(udtConnectionBox, cc.xy(3, row));

            row += 2;
            builder.add(
                ButtonBarFactory.buildLeftAlignedBar(httpProxyButton),
                cc.xy(3, row));

            row += 2;
            builder.addLabel(
                Translation.getTranslation("preferences.dialog.line_settings"),
                cc.xywh(1, row, 1, 1, "default, top"));
            builder.add(wanSpeed.getUiComponent(), cc.xyw(3, row, 2));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.lan_line_settings"), cc
                .xywh(1, row, 1, 1, "default, top"));
            builder.add(lanSpeed.getUiComponent(), cc.xyw(3, row, 2));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.server_disconnect"), cc.xy(
                1, row));
            builder.add(serverDisconnectBehaviorBox, cc.xy(3, row));

            panel = builder.getPanel();
        }
        return panel;
    }

    private static Component pairPanel(JComponent left, JComponent right) {
        FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(left, cc.xy(1, 1));
        builder.add(right, cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Saves the network settings.
     */
    public void save() {

        NetworkingMode netMode = NetworkingMode.values()[networkingMode
            .getSelectedIndex()];
        getController().setNetworkingMode(netMode);
        TransferManager tm = getController().getTransferManager();
        ConfigurationEntry.TRANSFER_LIMIT_AUTODETECT.setValue(getController(),
            wanSpeed.isAutomatic());
        if (wanSpeed.isAutomatic()) {
            //Update the automatic rates.
            getController().getThreadPool().execute(
                    tm.getRecalculateAutomaticRate());
        } else {
            tm.setUploadCPSForWAN(wanSpeed.getUploadSpeedKBPS());
            tm.setDownloadCPSForWAN(wanSpeed.getDownloadSpeedKBPS());
        }

        tm.setUploadCPSForLAN(lanSpeed.getUploadSpeedKBPS());
        tm.setDownloadCPSForLAN(lanSpeed.getDownloadSpeedKBPS());

        ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED.setValue(
            getController(), String.valueOf(relayedConnectionBox.isSelected()));
        ConfigurationEntry.UDT_CONNECTIONS_ENABLED.setValue(getController(),
            String.valueOf(udtConnectionBox.isSelected()));
        boolean syncAnyways = serverDisconnectBehaviorBox.getSelectedIndex() == 0;
        ConfigurationEntry.SERVER_DISCONNECT_SYNC_ANYWAYS.setValue(
            getController(), String.valueOf(syncAnyways));
    }

    private static String getTooltip(NetworkingMode nm) {
        if (nm == NetworkingMode.LANONLYMODE) {
            return Translation
                .getTranslation("preferences.dialog.network_mode.lan_only.tooltip");
        } else if (nm == NetworkingMode.PRIVATEMODE) {
            return Translation
                .getTranslation("preferences.dialog.network_mode.private.tooltip");
        } else if (nm == NetworkingMode.SERVERONLYMODE) {
            return Translation
                .getTranslation("preferences.dialog.network_mode.server_only.tooltip");
        }
        return null;
    }

    private static class HttpProxyAction extends BaseAction {
        private HttpProxyAction(Controller controller) {
            super("action_proxy", controller);

        }

        public void actionPerformed(ActionEvent e) {
            new HTTPProxySettingsDialog(getController()).open();
        }
    }

}
