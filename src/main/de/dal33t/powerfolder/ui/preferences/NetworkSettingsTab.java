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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.panel.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.UDTSocket;

public class NetworkSettingsTab extends PFComponent implements PreferenceTab {

    private JPanel panel;
    private JComboBox<String> networkingModeCombo;
    private JCheckBox relayedConnectionCB;
    private JCheckBox udtConnectionCB;
    private LineSpeedSelectionPanel wanSpeed;
    private LineSpeedSelectionPanel lanSpeed;
    private JButton httpProxyButton;
    private JCheckBox randomPortCB;
    private JTextField advPortTF;
    private JCheckBox openPortCB;
    private JComboBox bindAddressCombo;
    private LANList lanList;

    private boolean needsRestart;

    public NetworkSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.get("exp.preferences.network.title");
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
            .get("general.network_mode.private");
        options[NetworkingMode.LANONLYMODE.ordinal()] = Translation
            .get("general.network_mode.lan_only");
        options[NetworkingMode.SERVERONLYMODE.ordinal()] = Translation
            .get("general.network_mode.server_only");
        networkingModeCombo = new JComboBox<>(options);

        NetworkingMode currentNetworkingMode = getController()
            .getNetworkingMode();
        String tooltip = getTooltip(currentNetworkingMode);
        networkingModeCombo.setSelectedIndex(currentNetworkingMode.ordinal());
        networkingModeCombo.setToolTipText(tooltip);
        networkingModeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NetworkingMode selectedNetworkingMode = NetworkingMode.values()[networkingModeCombo
                    .getSelectedIndex()];
                networkingModeCombo.setToolTipText(getTooltip(selectedNetworkingMode));
                enableDisableComponents(NetworkingMode.LANONLYMODE == selectedNetworkingMode);
            }
        });

        String port = ConfigurationEntry.NET_BIND_PORT.getValue(getController());
        if (port == null) {
            port = Integer.toString(ConnectionListener.DEFAULT_PORT);
        }
        advPortTF = new JTextField(port) {
            protected Document createDefaultModel() {
                return new NumberAndCommaDocument();
            }
        };
        advPortTF.setToolTipText(Translation.get("exp.preferences.network.adv_port_tooltip"));
        randomPortCB = SimpleComponentFactory.createCheckBox(Translation
            .get("exp.preferences.network.random_port"));
        randomPortCB.setToolTipText(Translation
            .get("exp.preferences.network.random_port_tooltip"));
        randomPortCB.setSelected(ConfigurationEntry.NET_BIND_RANDOM_PORT
            .getValueBoolean(getController()));

        advPortTF.setEnabled(!randomPortCB.isSelected());

        randomPortCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                advPortTF.setEnabled(!randomPortCB.isSelected());
            }
        });

        relayedConnectionCB = SimpleComponentFactory
            .createCheckBox(Translation
                .get("exp.preferences.network.use_relayed_connections"));
        relayedConnectionCB
            .setSelected(ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED
                .getValueBoolean(getController()));

        udtConnectionCB = SimpleComponentFactory.createCheckBox(Translation
            .get("exp.preferences.network.use_udt_connection"));
        udtConnectionCB.setSelected(ConfigurationEntry.UDT_CONNECTIONS_ENABLED
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

        String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS
            .getValue(getController());
        bindAddressCombo = new JComboBox();
        bindAddressCombo.addItem(Translation
            .get("exp.preferences.network.bind_any"));
        // Fill in all known InetAddresses of this machine
        try {
            Enumeration<NetworkInterface> e = NetworkInterface
                .getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                Enumeration<InetAddress> ie = ni.getInetAddresses();
                while (ie.hasMoreElements()) {
                    InetAddress addr = ie.nextElement();
                    if (!(addr instanceof Inet4Address)) {
                        continue;
                    }
                    bindAddressCombo.addItem(new InterfaceChoice(ni, addr));
                    if (!StringUtils.isEmpty(cfgBind)) {
                        if (addr.getHostAddress().equals(cfgBind)) {
                            bindAddressCombo.setSelectedIndex(bindAddressCombo
                                .getItemCount() - 1);
                        }
                    }
                }
            }
        } catch (SocketException e1) {
            logWarning("SocketException. " + e1);
        } catch (Error e1) {
            logWarning("Error. " + e1);
        }


        lanList = new LANList(getController());
        lanList.load();
    }

    private void enableDisableComponents(boolean lanOnly) {
        relayedConnectionCB.setEnabled(!lanOnly);
        udtConnectionCB.setEnabled(!lanOnly && UDTSocket.isSupported());
        udtConnectionCB.setVisible(UDTSocket.isSupported());
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
                // Extra pref for useOnlineStorageCB.
                layout = new FormLayout(
                        "right:pref, 3dlu, 140dlu, pref:grow",
                        "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
            
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.addLabel(Translation
                .get("exp.preferences.network_mode_name"), cc.xy(
                1, row));
            builder.add(networkingModeCombo, cc.xy(3, row));

            row += 2;
            builder.add(relayedConnectionCB, cc.xy(3, row));

            row += 2;
            builder.add(udtConnectionCB, cc.xy(3, row));

            row += 2;
            builder.add(
                ButtonBarFactory.buildLeftAlignedBar(httpProxyButton),
                cc.xy(3, row));

            row += 2;
            builder.addLabel(
                Translation.get("exp.preferences.network.line_settings"),
                cc.xywh(1, row, 1, 1, "default, top"));
            builder.add(wanSpeed.getUiComponent(), cc.xyw(3, row, 2));

            row += 2;
            builder.addLabel(Translation.get("exp.preferences.network.lan_line_settings"),
                    cc.xywh(1, row, 1, 1, "default, top"));
            builder.add(lanSpeed.getUiComponent(), cc.xyw(3, row, 2));

            row += 2;
            builder.add(randomPortCB, cc.xy(3, row));

            row += 2;
            builder.addLabel(
                Translation.get("exp.preferences.network.bind"),
                cc.xy(1, row)).setToolTipText(
                Translation.get("exp.preferences.network.bind_tooltip"));
            builder.add(bindAddressCombo, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation
                .get("exp.preferences.network.ip_lan_list"), cc.xywh(1,
                row, 1, 1, "default, top"));
            builder.add(lanList.getUIPanel(), cc.xy(3, row));

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

        // Check for correctly entered port values
        try {
            // Check if it's a comma-separated list of parseable numbers
            String port = advPortTF.getText();
            StringTokenizer st = new StringTokenizer(port, ",");
            while (st.hasMoreTokens()) {
                int p = Integer.parseInt(st.nextToken());
                if (p < 0 || p > 65535) {
                    throw new NumberFormatException(
                        "Port out of range [0,65535]");
                }
            }

            // Check if only one port was given which is the default port
            if (ConfigurationEntry.NET_BIND_PORT.getValue(getController()) == null)
            {
                try {
                    int portnum = Integer.parseInt(port);
                    if (portnum != ConnectionListener.DEFAULT_PORT) {
                        needsRestart = true;
                    }
                } catch (NumberFormatException e) {
                }
            }
            // Only compare with old value if the things above don't match
            if (!needsRestart) {
                // Check if the value actually changed
                if (!port.equals(ConfigurationEntry.NET_BIND_PORT.getValue(getController()))) {
                    needsRestart = true;
                }
            }

            ConfigurationEntry.NET_BIND_PORT.setValue(getController(), port);
        } catch (NumberFormatException e) {
            logWarning("Unparsable port number");
        }

        boolean current = ConfigurationEntry.NET_BIND_RANDOM_PORT
            .getValueBoolean(getController());
        if (current != randomPortCB.isSelected()) {
            ConfigurationEntry.NET_BIND_RANDOM_PORT.setValue(getController(),
                String.valueOf(randomPortCB.isSelected()));
            needsRestart = true;
        }


        NetworkingMode netMode = NetworkingMode.values()[networkingModeCombo
            .getSelectedIndex()];
        getController().setNetworkingMode(netMode);
        TransferManager tm = getController().getTransferManager();
        ConfigurationEntry.TRANSFER_LIMIT_AUTODETECT.setValue(getController(),
            wanSpeed.isAutomatic());
        if (wanSpeed.isAutomatic()) {
            //Update the automatic rates.
            getController().getIOProvider().startIO(
                    tm.getRecalculateAutomaticRate());
        } else {
            tm.setUploadCPSForWAN(wanSpeed.getUploadSpeedKBPS());
            tm.setDownloadCPSForWAN(wanSpeed.getDownloadSpeedKBPS());
        }

        tm.setUploadCPSForLAN(lanSpeed.getUploadSpeedKBPS());
        tm.setDownloadCPSForLAN(lanSpeed.getDownloadSpeedKBPS());

        ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED.setValue(
            getController(), String.valueOf(relayedConnectionCB.isSelected()));
        ConfigurationEntry.UDT_CONNECTIONS_ENABLED.setValue(getController(),
            String.valueOf(udtConnectionCB.isSelected()));

        String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS.getValue(getController());
        Object bindObj = bindAddressCombo.getSelectedItem();
        if (bindObj instanceof String) { // Selected ANY
            if (!StringUtils.isEmpty(cfgBind)) {
                ConfigurationEntry.NET_BIND_ADDRESS.removeValue(getController());
                needsRestart = true;
            }
        } else {
            InetAddress addr = ((InterfaceChoice) bindObj).getAddress();
            if (!addr.getHostAddress().equals(cfgBind)) {
                ConfigurationEntry.NET_BIND_ADDRESS.setValue(getController(),
                    addr.getHostAddress());
                needsRestart = true;
            }
        }

        // LAN list
        needsRestart |= lanList.save();
    }

    private static String getTooltip(NetworkingMode nm) {
        if (nm == NetworkingMode.LANONLYMODE) {
            return Translation
                .get("exp.preferences.network.lan_only_tooltip");
        } else if (nm == NetworkingMode.PRIVATEMODE) {
            return Translation
                .get("exp.preferences.network.private_tooltip");
        } else if (nm == NetworkingMode.SERVERONLYMODE) {
            return Translation
                .get("exp.preferences.network.server_only_tooltip");
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

    /**
     * Accepts oly digits and commas
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private static class NumberAndCommaDocument extends PlainDocument {
        public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException
        {

            if (str == null) {
                return;
            }
            StringBuilder b = new StringBuilder();
            char[] chars = str.toCharArray();
            for (char aChar : chars) {
                if (Character.isDigit(aChar) || aChar == ',') {
                    b.append(aChar);
                }
            }
            super.insertString(offs, b.toString(), a);
        }
    }

    private static class InterfaceChoice {
        private InetAddress address;
        private String showString;

        private InterfaceChoice(NetworkInterface netInterface,
            InetAddress address)
        {
            this.address = address;

            StringBuilder sb = new StringBuilder();
            if (address.getAddress() != null) {
                for (int i = 0; i < address.getAddress().length; i++) {
                    if (i > 0) {
                        sb.append('.');
                    }
                    sb.append(address.getAddress()[i] & 0xff);
                }
            }
            sb.append(" / ");
            if (StringUtils.isNotBlank(netInterface.getDisplayName())) {
                sb.append(netInterface.getDisplayName().trim());
            }
            showString = sb.toString();
        }

        public String toString() {
            return showString;
        }

        public InetAddress getAddress() {
            return address;
        }
    }
}