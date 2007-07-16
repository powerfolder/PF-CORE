package de.dal33t.powerfolder.ui.preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.binding.adapter.BasicComponentFactory;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.Win32.FirewallUtil;
import de.dal33t.powerfolder.util.ui.LANList;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class AdvancedSettingsTab extends PFComponent implements PreferenceTab {
    private JPanel panel;
    private JTextField advPort;
    private JComboBox bindAddress;
    private JTextArea ifDescr;
    private JCheckBox showPreviewPanelBox;
    private JCheckBox useZipOnLanCheckBox;
    private LANList	lanList;
    private JCheckBox randomPort;
    private JCheckBox openport;
    private JCheckBox verboseBox;
    private boolean originalVerbose;

    boolean needsRestart = false;

    // The triggers the writing into core
    private Trigger writeTrigger;


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
        writeTrigger = new Trigger();
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
        showPreviewPanelBox.setSelected(PreferencesEntry.SHOW_PREVIEW_PANEL.getValueBoolean(getController()));
        useZipOnLanCheckBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.dialog.useziponlan"));
        useZipOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.useziponlan.tooltip"));
        useZipOnLanCheckBox.setSelected(ConfigurationEntry.USE_ZIP_ON_LAN
            .getValueBoolean(getController()).booleanValue());
        
        lanList = new LANList(getController());
        lanList.load();    
    
        randomPort = SimpleComponentFactory.createCheckBox(
        		Translation.getTranslation("preferences.dialog.randomPort"));
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
	        openport = SimpleComponentFactory.createCheckBox(
	        		Translation.getTranslation("preferences.dialog.openport"));
	        openport.setToolTipText(Translation
	        		.getTranslation("preferences.dialog.openport.tooltip"));
	        openport.setSelected(ConfigurationEntry.NET_FIREWALL_OPENPORT
	        		.getValueBoolean(getController()));
        }

        ValueModel verboseModel = new ValueHolder(ConfigurationEntry.VERBOSE.
                        getValueBoolean(getController()));
        originalVerbose = (Boolean) verboseModel.getValue();
        verboseBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(verboseModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.verbose"));
    }


    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
        	String rows = "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, top:pref, 3dlu, pref, 3dlu, pref, 3dlu";
        	if (FirewallUtil.isFirewallAccessible()) {
        		rows = "pref, 3dlu, " + rows;
        	}
        		
            FormLayout layout = new FormLayout(
                "right:100dlu, 3dlu, pref, 3dlu",
                rows);
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 0dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.addLabel(
                Translation.getTranslation("preferences.dialog.advPort"),
                cc.xy(1, row)).setToolTipText(
                Translation
                    .getTranslation("preferences.dialog.advPort.tooltip"));
            builder.add(advPort, cc.xy(3, row));

            row += 2;
            builder.add(randomPort, cc.xy(3, row));

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
            ifDescr.setBorder(new TitledBorder(Translation
                .getTranslation("preferences.dialog.bindDescr")));
            builder.add(ifDescr, cc.xy(3, row));

            row += 2;
            builder.add(useZipOnLanCheckBox, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.iplanlist"), cc.xy(1, row));
            builder.add(lanList.getUIPanel(), cc.xy(3, row));
            
            row += 2;
            builder.add(showPreviewPanelBox, cc.xy(3, row));

            row += 2;
            builder.add(verboseBox, cc.xy(3, row));

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

        // Write properties into core
        writeTrigger.triggerCommit();

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
            log().warn("Unparsable port number");
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
        // image previewer
        boolean current = PreferencesEntry.SHOW_PREVIEW_PANEL.getValueBoolean(getController());
        if (current != showPreviewPanelBox.isSelected()) {
        	  PreferencesEntry.SHOW_PREVIEW_PANEL.setValue(getController(), 
        			  showPreviewPanelBox.isSelected());
            needsRestart = true;
        }

        // zip on lan?
        current = ConfigurationEntry.USE_ZIP_ON_LAN.getValueBoolean(
            getController()).booleanValue();
        if (current != useZipOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_ZIP_ON_LAN.setValue(getController(),
                useZipOnLanCheckBox.isSelected() + "");
        }
        
        current = ConfigurationEntry.NET_BIND_RANDOM_PORT.getValueBoolean(
                getController()).booleanValue();
        if (current != randomPort.isSelected()) {
            ConfigurationEntry.NET_BIND_RANDOM_PORT.setValue(getController(),
                randomPort.isSelected() + "");
            needsRestart = true;
        }

        if (FirewallUtil.isFirewallAccessible()) {
	        current = ConfigurationEntry.NET_FIREWALL_OPENPORT.getValueBoolean(
	        		getController());
	        if (current != openport.isSelected()) {
	        	ConfigurationEntry.NET_FIREWALL_OPENPORT.setValue(getController(),
	        			openport.isSelected() + "");
	        	needsRestart = true;
	        }
        }
        
        // Verbose logging
        if (originalVerbose ^ verboseBox.isSelected()) {
            // Verbose setting changed.
            needsRestart = true;
        }
        ConfigurationEntry.VERBOSE.setValue(getController(),
                Boolean.toString(verboseBox.isSelected()));

        // LAN list
        needsRestart |= lanList.save();
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
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
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
