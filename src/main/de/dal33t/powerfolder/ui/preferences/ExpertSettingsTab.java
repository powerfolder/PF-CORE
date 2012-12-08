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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.Win32.FirewallUtil;

public class ExpertSettingsTab extends PFComponent implements PreferenceTab {

    private JPanel panel;
    private JTextField advPort;
    private JComboBox bindAddress;
    private JCheckBox useZipOnInternetCheckBox;
    private JCheckBox useZipOnLanCheckBox;
    private JCheckBox useDeltaSyncOnLanCheckBox;
    private LANList lanList;
    private JCheckBox randomPort;
    private JCheckBox openport;
    private JCheckBox verboseBox;
    private boolean originalVerbose;
    private JCheckBox useDeltaSyncOnInternetCheckBox;
    private JCheckBox useSwarmingOnLanCheckBox;
    private JCheckBox useSwarmingOnInternetCheckBox;

    private JTextField locationTF;
    private ValueModel locationModel;
    private JComponent locationField;

    private boolean needsRestart;

    public ExpertSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.expert.title");
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
        String port = ConfigurationEntry.NET_BIND_PORT
            .getValue(getController());
        if (port == null) {
            port = Integer.toString(ConnectionListener.DEFAULT_PORT);
        }
        advPort = new JTextField(port) {
            protected Document createDefaultModel() {
                return new NumberAndCommaDocument();
            }
        };
        advPort.setToolTipText(Translation
            .getTranslation("preferences.dialog.advPort.tooltip"));

        // Local base selection
        locationModel = new ValueHolder(getController().getFolderRepository()
            .getFoldersBasedirString());

        // Behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocationComponents();
            }
        });

        locationField = createLocationField();

        String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS
            .getValue(getController());
        bindAddress = new JComboBox();
        bindAddress.addItem(Translation
            .getTranslation("preferences.dialog.bind.any"));
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
                    bindAddress.addItem(new InterfaceChoice(ni, addr));
                    if (!StringUtils.isEmpty(cfgBind)) {
                        if (addr.getHostAddress().equals(cfgBind)) {
                            bindAddress.setSelectedIndex(bindAddress
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
        
        useZipOnLanCheckBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.dialog.use_zip_on_lan"));
        useZipOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.use_zip_on_lan.tooltip"));
        useZipOnLanCheckBox.setSelected(ConfigurationEntry.USE_ZIP_ON_LAN
            .getValueBoolean(getController()));

        // Always uses compression on internet
        useZipOnInternetCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.dialog.use_zip_on_internet"));
        useZipOnInternetCheckBox.setSelected(true);
        useZipOnInternetCheckBox.setEnabled(false);

        useDeltaSyncOnLanCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.dialog.use_delta_on_lan"));
        useDeltaSyncOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.use_delta_on_lan.tooltip"));
        useDeltaSyncOnLanCheckBox
            .setSelected(ConfigurationEntry.USE_DELTA_ON_LAN
                .getValueBoolean(getController()));

        useDeltaSyncOnInternetCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.dialog.use_delta_on_internet"));
        useDeltaSyncOnInternetCheckBox
            .setToolTipText(Translation
                .getTranslation("preferences.dialog.use_delta_on_internet.tooltip"));
        useDeltaSyncOnInternetCheckBox
            .setSelected(ConfigurationEntry.USE_DELTA_ON_INTERNET
                .getValueBoolean(getController()));

        useSwarmingOnLanCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.dialog.swarming.lan"));
        useSwarmingOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.swarming.lan.tooltip"));
        useSwarmingOnLanCheckBox
            .setSelected(ConfigurationEntry.USE_SWARMING_ON_LAN
                .getValueBoolean(getController()));

        useSwarmingOnInternetCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.dialog.swarming.internet"));
        useSwarmingOnInternetCheckBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.swarming.internet.tooltip"));
        useSwarmingOnInternetCheckBox
            .setSelected(ConfigurationEntry.USE_SWARMING_ON_INTERNET
                .getValueBoolean(getController()));

        lanList = new LANList(getController());
        lanList.load();

        randomPort = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.dialog.randomPort"));
        randomPort.setToolTipText(Translation
            .getTranslation("preferences.dialog.randomPort.tooltip"));
        randomPort.setSelected(ConfigurationEntry.NET_BIND_RANDOM_PORT
            .getValueBoolean(getController()));

        advPort.setEnabled(!randomPort.isSelected());

        randomPort.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                advPort.setEnabled(!randomPort.isSelected());
            }
        });

        if (FirewallUtil.isFirewallAccessible()) {
            openport = SimpleComponentFactory.createCheckBox(Translation
                .getTranslation("preferences.dialog.open_port"));
            openport.setToolTipText(Translation
                .getTranslation("preferences.dialog.open_port.tooltip"));
            openport.setSelected(ConfigurationEntry.NET_FIREWALL_OPENPORT
                .getValueBoolean(getController()));
        }

        originalVerbose = ConfigurationEntry.VERBOSE
            .getValueBoolean(getController());
        verboseBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.dialog.verbose"));
        verboseBox.setSelected(ConfigurationEntry.VERBOSE
            .getValueBoolean(getController()));
    }

    /**
     * Called when the location model changes value. Sets the location text
     * field value and enables the location button.
     */
    private void updateLocationComponents() {
        String value = (String) locationModel.getValue();
        locationTF.setText(value);
    }

    /**
     * Creates a pair of location text field and button.
     * 
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButtonMini(Icons
            .getIconById(Icons.DIRECTORY), Translation
            .getTranslation("folder_create.dialog.select_directory.text"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            String rows = "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref,  3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref";
            if (FirewallUtil.isFirewallAccessible()) {
                rows = "pref, 3dlu, " + rows;
            }

            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow", rows);
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.base_dir")), cc.xy(1, row));
            builder.add(locationField, cc.xyw(3, row, 2));

            row += 2;
            builder.add(randomPort, cc.xy(3, row));

            row += 2;
            builder.addLabel(
                Translation.getTranslation("preferences.dialog.advPort"),
                cc.xy(1, row)).setToolTipText(
                Translation
                    .getTranslation("preferences.dialog.advPort.tooltip"));
            builder.add(advPort, cc.xy(3, row));

            if (FirewallUtil.isFirewallAccessible()) {
                row += 2;
                builder.add(openport, cc.xy(3, row));
            }

            row += 2;
            builder.addLabel(
                Translation.getTranslation("preferences.dialog.bind"),
                cc.xy(1, row)).setToolTipText(
                Translation.getTranslation("preferences.dialog.bind.tooltip"));
            builder.add(bindAddress, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.ip_lan_list"), cc.xywh(1,
                row, 1, 1, "default, top"));
            builder.add(lanList.getUIPanel(), cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.zip_compression"), cc.xy(1,
                row));
            ButtonBarBuilder zipBar = ButtonBarBuilder
                .createLeftToRightBuilder();
            zipBar.addGridded(useZipOnInternetCheckBox);
            zipBar.addRelatedGap();
            zipBar.addGridded(useZipOnLanCheckBox);
            builder.add(zipBar.getPanel(), cc.xyw(3, row, 2));
            row += 2;

            builder
                .addLabel(Translation
                    .getTranslation("preferences.dialog.delta_sync"), cc.xy(1,
                    row));
            ButtonBarBuilder deltaBar = ButtonBarBuilder
                .createLeftToRightBuilder();
            deltaBar.addGridded(useDeltaSyncOnInternetCheckBox);
            deltaBar.addRelatedGap();
            deltaBar.addGridded(useDeltaSyncOnLanCheckBox);
            builder.add(deltaBar.getPanel(), cc.xyw(3, row, 2));
            row += 2;

            builder.addLabel(Translation
                .getTranslation("preferences.dialog.swarming"), cc.xy(1, row));
            ButtonBarBuilder swarmingBar = ButtonBarBuilder
                .createLeftToRightBuilder();
            swarmingBar.addGridded(useSwarmingOnInternetCheckBox);
            swarmingBar.addRelatedGap();
            swarmingBar.addGridded(useSwarmingOnLanCheckBox);
            builder.add(swarmingBar.getPanel(), cc.xyw(3, row, 2));

            row += 2;
            builder.add(verboseBox, cc.xyw(3, row, 2));

            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the advanced settings.
     */
    public void save() {

        // Check for correctly entered port values
        try {
            // Check if it's a comma-seperated list of parseable numbers
            String port = advPort.getText();
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
                if (!port.equals(ConfigurationEntry.NET_BIND_PORT
                    .getValue(getController())))
                {
                    needsRestart = true;
                }
            }

            ConfigurationEntry.NET_BIND_PORT.setValue(getController(), port);
        } catch (NumberFormatException e) {
            logWarning("Unparsable port number");
        }
        String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS
            .getValue(getController());
        Object bindObj = bindAddress.getSelectedItem();
        if (bindObj instanceof String) { // Selected ANY
            if (!StringUtils.isEmpty(cfgBind)) {
                ConfigurationEntry.NET_BIND_ADDRESS
                    .removeValue(getController());
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

        // Set folder base
        String oldFolderBaseString = getController().getFolderRepository()
            .getFoldersBasedirString();
        String newFolderBaseString = (String) locationModel.getValue();
        getController().getFolderRepository().setFoldersBasedir(newFolderBaseString);
        if (!StringUtils.isEqual(oldFolderBaseString, newFolderBaseString)) {
            getController().getUIController().configureDesktopShortcut(true);
        }

        // zip on lan?
        boolean current = ConfigurationEntry.USE_ZIP_ON_LAN
            .getValueBoolean(getController());
        if (current != useZipOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_ZIP_ON_LAN.setValue(getController(), String
                .valueOf(useZipOnLanCheckBox.isSelected()));
        }

        // delta on lan?
        current = ConfigurationEntry.USE_DELTA_ON_LAN
            .getValueBoolean(getController());
        if (current != useDeltaSyncOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_DELTA_ON_LAN.setValue(getController(),
                String.valueOf(useDeltaSyncOnLanCheckBox.isSelected()));
            needsRestart = true;
        }

        current = ConfigurationEntry.USE_DELTA_ON_INTERNET
            .getValueBoolean(getController());
        if (current != useDeltaSyncOnInternetCheckBox.isSelected()) {
            ConfigurationEntry.USE_DELTA_ON_INTERNET.setValue(getController(),
                Boolean.toString(useDeltaSyncOnInternetCheckBox.isSelected()));
            needsRestart = true;
        }

        // Swarming
        current = ConfigurationEntry.USE_SWARMING_ON_LAN
            .getValueBoolean(getController());
        if (current != useSwarmingOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_SWARMING_ON_LAN.setValue(getController(),
                String.valueOf(useSwarmingOnLanCheckBox.isSelected()));
            needsRestart = true;
        }

        current = ConfigurationEntry.USE_SWARMING_ON_INTERNET
            .getValueBoolean(getController());
        if (current != useSwarmingOnInternetCheckBox.isSelected()) {
            ConfigurationEntry.USE_SWARMING_ON_INTERNET.setValue(
                getController(), Boolean.toString(useSwarmingOnInternetCheckBox
                    .isSelected()));
            needsRestart = true;
        }

        current = ConfigurationEntry.NET_BIND_RANDOM_PORT
            .getValueBoolean(getController());
        if (current != randomPort.isSelected()) {
            ConfigurationEntry.NET_BIND_RANDOM_PORT.setValue(getController(),
                String.valueOf(randomPort.isSelected()));
            needsRestart = true;
        }

        if (FirewallUtil.isFirewallAccessible()) {
            current = ConfigurationEntry.NET_FIREWALL_OPENPORT
                .getValueBoolean(getController());
            if (current != openport.isSelected()) {
                ConfigurationEntry.NET_FIREWALL_OPENPORT.setValue(
                    getController(), String.valueOf(openport.isSelected()));
                needsRestart = true;
            }
        }

        // Verbose logging
        if (originalVerbose ^ verboseBox.isSelected()) {
            // Verbose setting changed.
            needsRestart = true;
        }
        ConfigurationEntry.VERBOSE.setValue(getController(), Boolean
            .toString(verboseBox.isSelected()));

        ConfigurationEntry.VERBOSE.setValue(getController(), Boolean
            .toString(verboseBox.isSelected()));

        // LAN list
        needsRestart |= lanList.save();
    }

    private static class InterfaceChoice {
        private NetworkInterface netInterface;
        private InetAddress address;
        private String showString;

        private InterfaceChoice(NetworkInterface netInterface,
            InetAddress address)
        {
            this.netInterface = netInterface;
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

    /**
     * Accepts oly digits and commatas
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

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) locationModel.getValue();
            List<File> files = DialogFactory.chooseDirectory(getController()
                .getUIController(), initial, false);
            if (!files.isEmpty()) {
                File newLocation = files.get(0);
                // Make sure that the user is not setting this to the base dir
                // of an existing folder.
                for (Folder folder : getController().getFolderRepository()
                    .getFolders(true))
                {
                    if (folder.getLocalBase().equals(newLocation)) {
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("preferences.dialog.duplicate_localbase.title"),
                                Translation
                                    .getTranslation(
                                        "preferences.dialog.duplicate_localbase.message",
                                        folder.getName()),
                                GenericDialogType.ERROR);
                        return;
                    }
                }
                locationModel.setValue(newLocation.getAbsolutePath());
            }
        }
    }

}
