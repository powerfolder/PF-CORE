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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.net.UDTSocket;
import de.dal33t.powerfolder.util.ui.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class NetworkSettingsTab extends PFComponent implements PreferenceTab {
    private JPanel panel;
    private JComboBox networkingMode;
    private JCheckBox relayedConnectionBox;
    private JCheckBox udtConnectionBox;
    private LineSpeedSelectionPanel wanSpeed;
    private LineSpeedSelectionPanel lanSpeed;
    private JSlider silentModeThrottle;
    private boolean needsRestart = false;
    private JLabel silentThrottleLabel;
    private JButton httpProxyButton;
    private ServerSelectorPanel severSelector;
    private ValueModel serverModel;

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
            .getTranslation("preferences.dialog.networkmode.private");
        options[NetworkingMode.LANONLYMODE.ordinal()] = Translation
            .getTranslation("preferences.dialog.networkmode.lanonly");
        options[NetworkingMode.SERVERONLYMODE.ordinal()] = Translation
            .getTranslation("preferences.dialog.networkmode.serveronly");
        networkingMode = new JComboBox(options);

        NetworkingMode currentNetworkingMode = getController()
            .getNetworkingMode();
        String tooltip = getTooltip(currentNetworkingMode);
        networkingMode.setSelectedIndex(currentNetworkingMode.ordinal());
        networkingMode.setToolTipText(tooltip);
        networkingMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NetworkingMode selNm = NetworkingMode.values()[networkingMode
                    .getSelectedIndex()];
                networkingMode.setToolTipText(getTooltip(selNm));
                enableDisableComponents(NetworkingMode.LANONLYMODE
                    .equals(selNm));
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

        wanSpeed = new LineSpeedSelectionPanel(true);
        wanSpeed.loadWANSelection();
        TransferManager tm = getController().getTransferManager();
        wanSpeed.setSpeedKBPS(tm.getAllowedUploadCPSForWAN() / 1024, tm
            .getAllowedDownloadCPSForWAN() / 1024);

        lanSpeed = new LineSpeedSelectionPanel(true);
        lanSpeed.loadLANSelection();
        lanSpeed.setSpeedKBPS(tm.getAllowedUploadCPSForLAN() / 1024, tm
            .getAllowedDownloadCPSForLAN() / 1024);

        silentThrottleLabel = new JLabel(Translation
            .getTranslation("preferences.dialog.silentthrottle"));
        silentThrottleLabel.setToolTipText(Translation
            .getTranslation("preferences.dialog.silentthrottle.tooltip"));

        silentModeThrottle = new JSlider();
        silentModeThrottle.setMinimum(10);
        silentModeThrottle.setMajorTickSpacing(25);
        silentModeThrottle.setMinorTickSpacing(5);

        silentModeThrottle.setPaintTicks(true);
        silentModeThrottle.setPaintLabels(true);
        Dictionary<Integer, JLabel> smtT = new Hashtable<Integer, JLabel>();
        for (int i = 0; i <= 100; i += silentModeThrottle.getMajorTickSpacing())
        {
            smtT.put(i, new JLabel(Integer.toString(i) + "%"));
        }
        smtT.put(silentModeThrottle.getMinimum(), new JLabel(silentModeThrottle
            .getMinimum()
            + "%"));
        smtT.put(silentModeThrottle.getMaximum(), new JLabel(silentModeThrottle
            .getMaximum()
            + "%"));
        silentModeThrottle.setLabelTable(smtT);

        int smt = 25;
        try {
            smt = Math.min(100, Math.max(10, Integer
                .parseInt(ConfigurationEntry.UPLOADLIMIT_SILENTMODE_THROTTLE
                    .getValue(getController()))));
        } catch (NumberFormatException e) {
            logWarning("silentmodethrottle" + e);
        }
        silentModeThrottle.setValue(smt);

        httpProxyButton = new JButton(Translation
            .getTranslation("general.proxy_settings"));
        httpProxyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new HTTPProxySettingsDialog(getController()).open();
            }
        });

        Member server = getController().getOSClient().getServer();
        serverModel = new ValueHolder(server, true);
        severSelector = new ServerSelectorPanel(getController(), serverModel);

        enableDisableComponents(getController().isLanOnly());
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
            FormLayout layout = new FormLayout(
                "right:100dlu, 3dlu, 30dlu, 3dlu, 15dlu, 10dlu, 30dlu, 30dlu, pref, 0:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 7dlu, top:pref, 7dlu, top:pref, 7dlu, pref, 7dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 0dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.networkmode.name"), cc.xy(
                1, row));
            builder.add(networkingMode, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(relayedConnectionBox, cc.xywh(3, row, 8, 1));

            row += 2;
            builder.add(udtConnectionBox, cc.xywh(3, row, 8, 1));

            row += 2;
            builder.add(httpProxyButton, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.linesettings"), cc.xy(1,
                row));
            builder.add(wanSpeed, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.lanlinesettings"), cc.xy(1,
                row));
            builder.add(lanSpeed, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(silentThrottleLabel, cc.xy(1, row));
            builder.add(silentModeThrottle, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.server"), cc.xy(1, row));
            builder.add(severSelector.getUIComponent(), cc.xywh(3, row, 7, 1));
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the network settings.
     */
    public void save() {
        NetworkingMode netMode = NetworkingMode.values()[networkingMode
            .getSelectedIndex()];
        getController().setNetworkingMode(netMode);
        TransferManager tm = getController().getTransferManager();
        tm.setAllowedUploadCPSForWAN(wanSpeed.getUploadSpeedKBPS());
        tm.setAllowedDownloadCPSForWAN(wanSpeed.getDownloadSpeedKBPS());
        tm.setAllowedUploadCPSForLAN(lanSpeed.getUploadSpeedKBPS());
        tm.setAllowedDownloadCPSForLAN(lanSpeed.getDownloadSpeedKBPS());
        ConfigurationEntry.UPLOADLIMIT_SILENTMODE_THROTTLE.setValue(
            getController(), Integer.toString(silentModeThrottle.getValue()));
        ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED.setValue(
            getController(), "" + relayedConnectionBox.isSelected());
        ConfigurationEntry.UDT_CONNECTIONS_ENABLED.setValue(getController(), ""
            + udtConnectionBox.isSelected());
        Member oldServer = getController().getOSClient().getServer();
        Member newServer = (Member) serverModel.getValue();
        if (newServer == null) {
            ConfigurationEntry.SERVER_NAME.removeValue(getController());
            ConfigurationEntry.SERVER_HOST.removeValue(getController());
            ConfigurationEntry.SERVER_NODEID.removeValue(getController());
            ConfigurationEntry.SERVER_WEB_URL.removeValue(getController());
        } else if (!newServer.equals(oldServer)) {
            getController().getOSClient()
                .setServerInConfig(newServer.getInfo());
        }
        needsRestart = !Util.equals(oldServer, newServer);
    }

    private String getTooltip(NetworkingMode nm) {
        if (nm.equals(NetworkingMode.LANONLYMODE)) {
            return Translation
                .getTranslation("preferences.dialog.networkmode.lanonly.tooltip");
        } else if (nm.equals(NetworkingMode.PRIVATEMODE)) {
            return Translation
                .getTranslation("preferences.dialog.networkmode.private.tooltip");
        } else if (nm.equals(NetworkingMode.SERVERONLYMODE)) {
            return Translation
                .getTranslation("preferences.dialog.networkmode.serveronly.tooltip");
        }
        return null;
    }
}
