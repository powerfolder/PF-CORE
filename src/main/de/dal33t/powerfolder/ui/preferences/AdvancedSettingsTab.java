package de.dal33t.powerfolder.ui.preferences;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class AdvancedSettingsTab extends PFComponent implements PreferenceTab {
    private JPanel panel;
    private JTextField advPort;
    private JComboBox bindAddress;
    private JTextArea ifDescr;
    private JCheckBox showPreviewPanelBox;

    private static final String SHOW_PREVIEW_PANEL = "show_preview_panel";

    boolean needsRestart = false;

    public AdvancedSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.advanced.title");
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
        String port = getController().getConfig().getProperty("port");
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

        String cfgBind = StringUtils.trim(getController().getConfig()
            .getProperty("net.bindaddress"));
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
            log().error(e1);
        }

        ifDescr = new JTextArea(3, 20);
        ifDescr.setLineWrap(true);
        ifDescr.setWrapStyleWord(true);
        ifDescr.setEditable(false);
        ifDescr.setOpaque(false);
        
        updateIFDescr();

        bindAddress.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                if (arg0.getStateChange() == ItemEvent.SELECTED) {
                    updateIFDescr();
                }
            }

        });

        showPreviewPanelBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.dialog.showpreviewpanel"));
        showPreviewPanelBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.showpreviewpanel.tooltip"));
        showPreviewPanelBox.setSelected("true".equals(getController()
            .getConfig().get(SHOW_PREVIEW_PANEL)));

    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "3dlu, right:pref, 3dlu, pref, 3dlu",
                "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, top:pref:grow, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            int row = 2;
            builder.addLabel(
                Translation.getTranslation("preferences.dialog.advPort"),
                cc.xy(2, row)).setToolTipText(
                Translation
                    .getTranslation("preferences.dialog.advPort.tooltip"));
            builder.add(advPort, cc.xy(4, row));
            row += 2;
            builder.addLabel(
                Translation.getTranslation("preferences.dialog.bind"),
                cc.xy(2, row)).setToolTipText(
                Translation.getTranslation("preferences.dialog.bind.tooltip"));
            builder.add(bindAddress, cc.xy(4, row));

            row += 2;
            ifDescr.setBorder(new TitledBorder(Translation
                .getTranslation("preferences.dialog.bindDescr")));
            builder.add(ifDescr, cc.xy(4, row));

            row += 2;
            builder.add(showPreviewPanelBox, cc.xy(4, row));
            panel = builder.getPanel();
        }
        return panel;
    }

    private void updateIFDescr() {
        Object selection = bindAddress.getSelectedItem();
        if (selection instanceof InterfaceChoice) {
            ifDescr.setText(((InterfaceChoice) selection).getNetInterface()
                .getDisplayName());
        } else {
            ifDescr.setText("");
        }
    }

    /**
     * Saves the advanced settings.
     */
    public void save() {
        Properties config = getController().getConfig();
        // Check for correctly entered port values
        try {
            // Check if it's a commaseperated list of parseable numbers
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
            if (config.getProperty("port") == null) {
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
                if (!port.equals(config.getProperty("port"))) {
                    needsRestart = true;
                }
            }

            config.setProperty("port", port);
        } catch (NumberFormatException e) {
            log().warn("Unparsable port number");
        }
        String cfgBind = StringUtils.trim(getController().getConfig()
            .getProperty("net.bindaddress"));
        Object bindObj = bindAddress.getSelectedItem();
        if (bindObj instanceof String) { // Selected ANY
            if (!StringUtils.isEmpty(cfgBind)) {
                config.setProperty("net.bindaddress", "");
                needsRestart = true;
            }
        } else {
            InetAddress addr = ((InterfaceChoice) bindObj).getAddress();
            if (!addr.getHostAddress().equals(cfgBind)) {
                config.setProperty("net.bindaddress", addr.getHostAddress());
                needsRestart = true;
            }
        }
        // image previewer
        boolean current = "true".equals(config.getProperty(SHOW_PREVIEW_PANEL));
        if (current != showPreviewPanelBox.isSelected()) {
            config.setProperty(SHOW_PREVIEW_PANEL, showPreviewPanelBox
                .isSelected()
                + "");
            needsRestart = true;
        }
    }

    private class InterfaceChoice {
        private NetworkInterface netInterface;
        private InetAddress address;
        private String showString;

        public InterfaceChoice(NetworkInterface netInterface,
            InetAddress address)
        {
            this.netInterface = netInterface;
            this.address = address;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < address.getAddress().length; i++) {
                if (i > 0)
                    sb.append('.');
                sb.append(address.getAddress()[i] & 0xff);
            }
            // sb.append("
            // (").append(netInterface.getDisplayName().trim()).append(')');
            showString = sb.toString();
        }

        public String toString() {
            return showString;
        }

        public InetAddress getAddress() {
            return address;
        }

        public NetworkInterface getNetInterface() {
            return netInterface;
        }
    }

    /**
     * Accepts oly digits and commatas
     * 
     * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
     */
    private class NumberAndCommaDocument extends PlainDocument {
        public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException
        {

            if (str == null) {
                return;
            }
            StringBuilder b = new StringBuilder();
            char[] chars = str.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (Character.isDigit(chars[i]) || chars[i] == ',')
                    b.append(chars[i]);
            }
            super.insertString(offs, b.toString(), a);
        }
    }

}
