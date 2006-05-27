/* $Id: PreferencesPanel.java,v 1.63 2006/04/23 18:21:18 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.adapter.PreferencesAdapter;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.theme.ThemeSupport;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.util.ui.LinkLabel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * The preferences panel getUsername()
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.63 $
 */
public class PreferencesPanel extends BaseDialog {
    // disposition constants for status messages
    final static int DISP_INFO = 0; // just informing message
    final static int DISP_WARNING = 1; // warning
    final static int DISP_ERROR = 2; // error

    public static String password;
    public static String username;
    public static String newDyndns;
    public static String dyndnsSystem;

    // UI Components
    private JTextField nickField;
    private JCheckBox createDesktopShortcutsBox;
    private JCheckBox privateNetwokringBox;
    private JCheckBox onStartUpdateBox;
    private JLabel myDnsLabel;
    private JTextField myDnsField;
    private JLabel dyndnsHost;
//    private JTextField ulLimitField;
    private LineSpeedSelectionPanel wanSpeed;
    private LineSpeedSelectionPanel lanSpeed;
    private JComboBox languageChooser;
    private JTextField dyndnsUserField;
    private JPasswordField dyndnsPasswordField;
    private JLabel currentIPField;
    private JLabel updatedIPField;
    private JTabbedPane tabbedPane;
    private JComboBox colorThemeChooser;
    private JComboBox xBehaviorChooser;
    private JComponent localBaseSelectField;
// Advanced settings tab
    private JTextField advPort;
    private JComboBox bindAddress;
    private JTextArea ifDescr;
    
    private JButton okButton;
    private JButton cancelButton;
    private JButton updateButton;

    // Models
    private ValueModel mydnsndsModel;
    private ValueModel localBaseHolder;
    // The triggers the writing into core
    private Trigger writeTrigger;

    // The original theme
    private PlasticTheme oldTheme;

    /**
     * @param controller
     */
    public PreferencesPanel(Controller controller) {        
        super(controller, true, false);
    }

    // Application logic ******************************************************

    /**
     * Validates the settings before saving them persistantly
     * 
     * @return the ui component which validation has failed (giving a chance to
     *         the caller to give it the input focus) or null if all is good.
     */
    private JComponent validateSettings() {

        if (!getController().getDynDnsManager().validateDynDns(
            myDnsField.getText()))
        {

            return myDnsField;
        }

        // all validations have passed
        return null;
    }

    /**
     * Saves the changes
     */
    private void saveSettings() {
        Properties config = getController().getConfig();
        // If we should request a restart after save
        boolean requestRestart = false;

        log().info("Saving config");

        // Write properties into core
        writeTrigger.triggerCommit();

        // Set locale
        if (languageChooser.getSelectedItem() instanceof Locale) {
            Locale locale = (Locale) languageChooser.getSelectedItem();
            // Check if we need to restart
            // log().warn(
            // "Active locale: " + Translation.getActiveLocale()
            // + ", choosen: " + locale);
            requestRestart = !Util.equals(locale.getLanguage(), Translation
                .getActiveLocale().getLanguage());
            // Save settings
            Translation.saveLocalSetting(locale);
        } else {
            // Remove setting
            Translation.saveLocalSetting(null);
        }

        // Set folder base
        config.setProperty("foldersbase", (String) localBaseHolder.getValue());

        // Store networking mode
        getController().setPublicNetworking(!privateNetwokringBox.isSelected());
        getController().getNodeManager().disconnectUninterestingNodes();
        
        // Store ui theme
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            PlasticTheme theme = PlasticXPLookAndFeel.getMyCurrentTheme();
            config.put("uitheme", theme.getClass().getName());
            if (!Util.equals(theme, oldTheme)) {
                // FIXME: Themechange does not repaint SimpleInternalFrames.
                // thus restart required
                requestRestart = true;
            }
        }

        // Nickname
        if (!StringUtils.isBlank(nickField.getText())) {
            getController().changeNick(nickField.getText(), false);
        }
        getController().getTransferManager().setAllowedUploadCPSForWAN(wanSpeed.getUploadSpeedKBPS());
        getController().getTransferManager().setAllowedUploadCPSForLAN(lanSpeed.getUploadSpeedKBPS());
//       log().warn(wanSpeed.getUploadSpeedKBPS() + " " + getController().getTransferManager().getAllowedUploadCPS());

        // Save dyndns settings
        saveDynDnsSettings();
        requestRestart |= saveAdvancedSettings();

        // Store settings
        getController().saveConfig();

        if (requestRestart) {
            handleRestartRequest();
        }
    }

    /**
     * Saves the dyndns settings
     */
    private void saveDynDnsSettings() {
        String dyndnsHost = (String) mydnsndsModel.getValue();
        if (!StringUtils.isBlank(dyndnsHost)) {
            getController().getConfig().put("mydyndns", dyndnsHost);
        } else {
            getController().getConfig().remove("mydyndns");
        }

        if (!StringUtils.isBlank(dyndnsHost)) {
            if (!StringUtils.isBlank(dyndnsUserField.getText())) {
                getController().getConfig().put("dyndnsUserName",
                    dyndnsUserField.getText());
            } else {
                getController().getConfig().remove("dyndnsUserName");
            }

            String password = new String(dyndnsPasswordField.getPassword());
            if (!StringUtils.isBlank(password)) {
                getController().getConfig().put("dyndnsPassword", password);
            } else {
                getController().getConfig().remove("dyndnsPassword");
            }
        }

        boolean b = onStartUpdateBox.isSelected();

        if (b) {
            getController().getConfig().put("onStartUpdate", "true");
        } else {
            getController().getConfig().remove("onStartUpdate");
        }
    }

    /**
     * Saves the advanced settings. 
     * @return true if some changed settings require PowerFolder to be restarted
     */
    private boolean saveAdvancedSettings() {
        boolean requestRestart = false;

        // Check for correctly entered port values
        try {
            // Check if it's a commaseperated list of parseable numbers
            String port = advPort.getText();
            StringTokenizer st = new StringTokenizer(port, ",");
            while (st.hasMoreTokens()) {
                int p = Integer.parseInt(st.nextToken());
                if (p < 0 || p > 65535)
                    throw new NumberFormatException("Port out of range [0,65535]");
            }
            
            // Check if only one port was given which is the default port
            if (getController().getConfig().getProperty("port") == null) {
                try {
                    int portnum = Integer.parseInt(port);
                    if (portnum != ConnectionListener.DEFAULT_PORT)
                        requestRestart = true;                    
                } catch (NumberFormatException e) {
                }
            } 
            // Only compare with old value if the things above don't match
            if (!requestRestart) {
                // Check if the value actually changed
                if (!port.equals(getController().getConfig().getProperty("port")))
                        requestRestart = true;
            }
            
            getController().getConfig().setProperty("port", port);
        } catch (NumberFormatException e) {
            log().warn("Unparsable port number");
        }
        
        String cfgBind = StringUtils.trim(getController().getConfig().getProperty("net.bindaddress"));
        Object bindObj = bindAddress.getSelectedItem();
        if (bindObj instanceof String) { // Selected ANY
            if (!StringUtils.isEmpty(cfgBind)) { 
                getController().getConfig().setProperty("net.bindaddress", "");
                requestRestart = true;
            }
        } else {
            InetAddress addr = ((InterfaceChoice) bindObj).getAddress();
            if (!addr.getHostAddress().equals(cfgBind)) {
                getController().getConfig().setProperty("net.bindaddress", 
                    addr.getHostAddress());
                requestRestart = true;
            }
        }
        
        return requestRestart;
    }
    
    private boolean isUpdateSelected() {
        return Util.getBooleanProperty(getController().getConfig(),
            "onStartUpdate", false);
    }

    // UI Methods *************************************************************

    /**
     * Initalizes all needed ui components
     */
    private void initComponents() {

        writeTrigger = new Trigger();

        nickField = new JTextField(getController().getMySelf().getNick());

        ValueModel csModel = new PreferencesAdapter(getController()
            .getPreferences(), "createdesktopshortcuts", Boolean.TRUE);
        createDesktopShortcutsBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(csModel, writeTrigger), "");
        // Only available on windows systems
        createDesktopShortcutsBox.setEnabled(Util.isWindowsSystem());

        // Public networking option
        privateNetwokringBox = SimpleComponentFactory.createCheckBox();
        privateNetwokringBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.privatenetworking.tooltip"));
        privateNetwokringBox.setSelected(!getController().isPublicNetworking());

        // DynDns
        myDnsLabel = new LinkLabel(Translation
            .getTranslation("preferences.dialog.dyndns"), Translation
            .getTranslation("preferences.dialog.dyndns.link"));

        mydnsndsModel = new ValueHolder(getController().getConfig()
            .getProperty("mydyndns"));
        mydnsndsModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String dyndns = (String) evt.getNewValue();
                // Enable tab when dyndns host ist set
                tabbedPane.setEnabledAt(1, !StringUtils.isBlank(dyndns));
            }
        });
        myDnsField = SimpleComponentFactory
            .createTextField(mydnsndsModel, true);


        wanSpeed = new LineSpeedSelectionPanel();
        wanSpeed.loadWANSelection();
        wanSpeed.setUploadSpeedKBPS(getController().getTransferManager().getAllowedUploadCPSForWAN() / 1024);
        
        lanSpeed = new LineSpeedSelectionPanel();
        lanSpeed.loadLANSelection();
        lanSpeed.setUploadSpeedKBPS(getController().getTransferManager()
            .getAllowedUploadCPSForLAN() / 1024);

        // Language selector
        languageChooser = createLanguageChooser();

        // Build color theme chooser
        colorThemeChooser = createThemeChooser();
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            colorThemeChooser.setEnabled(true);
            oldTheme = PlasticXPLookAndFeel.getMyCurrentTheme();
        } else {
            // Only available if PlasicXPLookAndFeel is enabled
            colorThemeChooser.setEnabled(false);
        }

        // Create xBehaviorchooser
        ValueModel xBehaviorModel = new PreferencesAdapter(getController()
            .getPreferences(), "quitonx", Boolean.FALSE);
        // Build behavior chooser
        xBehaviorChooser = createXBehaviorChooser(new BufferedValueModel(
            xBehaviorModel, writeTrigger));
        // Only available on systems with system tray
        xBehaviorChooser.setEnabled(Util.isSystraySupported());
        if (!xBehaviorChooser.isEnabled()) {
            // Display exit on x if not enabled
            xBehaviorModel.setValue(Boolean.TRUE);
        }

        // Local base selection
        localBaseHolder = new ValueHolder(getController().getFolderRepository()
            .getFoldersBasedir());
        localBaseSelectField = ComplexComponentFactory
            .createDirectorySelectionField(Translation
                .getTranslation("preferences.dialog.basedir.title"),
                localBaseHolder, null);

        // Init the dyndns components
        initDynDnsSettingsComponents();
        
        initAdvancedSettingsComponents();

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //
                // OK button event handler
                //

                // since we are performing a validation
                // that could take some time we need to warn the user about it.
                // However updating the gui while the task is progressing,
                // requires
                // us to run the validation in a new thread that will
                // give the chance of the swing thread to update the GUI
                new Thread("Preferences saver/validator") {
                    public void run() {
                        // disable the OK button while we are in the
                        // working thread (i.e. prohibit re-entrance)
                        okButton.setEnabled(false);

                        // validate the user input and check the result
                        JComponent failedComponent = validateSettings();

                        if (failedComponent == null) {
                            // all is good then just save the settings and
                            // close the window                            
                            close();
                        } else {
                            // otherwise bring the focus to the failed
                            // component
                            failedComponent.grabFocus();
                            if (failedComponent instanceof JTextComponent) {
                                // if it is a text component then also
                                // select all the text in it.
                                JTextComponent comp = (JTextComponent) failedComponent;
                                comp.selectAll();
                            }
                        }

                        // Save settings
                        saveSettings();

                        close();
                        // // restore the enabled state of the ok button
                        okButton.setEnabled(true);
                    } // end run
                }.start(); // start the working thread
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
                PlasticTheme activeTheme = ThemeSupport.getActivePlasticTheme();
                // Reset to old look and feel
                if (!Util.equals(oldTheme, activeTheme)) {
                    ThemeSupport.setPlasticTheme(oldTheme, getUIController()
                        .getMainFrame().getUIComponent(), getUIComponent());
                }
            }
        });
    }

    private void initDynDnsSettingsComponents() {
        dyndnsHost = BasicComponentFactory.createLabel(mydnsndsModel);        

        if (getController().getConfig().getProperty("dyndnsUserName") == null) {
            dyndnsUserField = new JTextField("");
        } else {
            dyndnsUserField = new JTextField(getController().getConfig()
                .getProperty("dyndnsUserName"));
        }

        if (getController().getConfig().getProperty("dyndnsPassword") == null) {
            dyndnsPasswordField = new JPasswordField("");
        } else {
            dyndnsPasswordField = new JPasswordField(getController()
                .getConfig().getProperty("dyndnsPassword"));
        }

        currentIPField = new JLabel(getController().getDynDnsManager()
            .getDyndnsViaHTTP());

        if (!isUpdateSelected()) {
            updatedIPField = new JLabel(getController().getDynDnsManager()
                .getHostIP(getController().getConfig().getProperty("mydyndns")));
        } else {
            updatedIPField = new JLabel(getController().getConfig()
                .getProperty("lastUpdatedIP"));
        }

        onStartUpdateBox = SimpleComponentFactory.createCheckBox();
        onStartUpdateBox.setSelected(isUpdateSelected());

        updateButton = createUpdateButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        updateButton.setEnabled(false);

                        username = dyndnsUserField.getText();
                        password = new String(dyndnsPasswordField.getPassword());
                        newDyndns = myDnsField.getText();

                        if (dyndnsUserField.getText().equals("")) {
                            dyndnsUserField.grabFocus();
                        } else if (new String(dyndnsPasswordField.getPassword())
                            .equals(""))
                        {
                            dyndnsPasswordField.grabFocus();
                        }

                        if (!myDnsField.getText().equals("")
                            && !dyndnsUserField.getText().equals("")
                            && !new String(dyndnsPasswordField.getPassword())
                                .equals(""))
                        {
                            // update
                            getController().getDynDnsManager().forceUpdate();
                            updatedIPField.setText(getController().getConfig()
                                .getProperty("lastUpdatedIP"));
                        } else {
                            updateButton.setEnabled(false);
                            getController().getDynDnsManager()
                                .showPanelErrorMessage();
                        }
                        updateButton.setEnabled(true);
                    }
                }.start();
            }
        });
    }

    protected void initAdvancedSettingsComponents() {
        String port = getController().getConfig().getProperty("port");
        if (port == null)
            port = Integer.toString(ConnectionListener.DEFAULT_PORT);
        advPort = new JTextField(port) {
            protected Document createDefaultModel() {
                return new NumberAndCommaDocument();
            }
        };
        advPort.setToolTipText(Translation
            .getTranslation("preferences.dialog.advPort.tooltip"));
        
        String cfgBind = StringUtils.trim(getController().getConfig().getProperty("net.bindaddress"));
        bindAddress = new JComboBox();
        bindAddress.addItem(Translation
            .getTranslation("preferences.dialog.bind.any"));
        // Fill in all known InetAddresses of this machine
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                Enumeration<InetAddress> ie = ni.getInetAddresses();
                while (ie.hasMoreElements()) {
                    InetAddress addr = ie.nextElement();
                    bindAddress.addItem(new InterfaceChoice(ni, addr));
                    if (!StringUtils.isEmpty(cfgBind)) {
                       if (addr.getHostAddress().equals(cfgBind))
                           bindAddress.setSelectedIndex(bindAddress.getItemCount() - 1);
                    }
                }
            }
        } catch (SocketException e1) {
            log().error(e1);
        }
        
        ifDescr = new JTextArea();
        ifDescr.setLineWrap(true);
        ifDescr.setWrapStyleWord(true);
        ifDescr.setEditable(false);
        ifDescr.setOpaque(false);
        updateIFDescr();
        
        bindAddress.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                if (arg0.getStateChange() == ItemEvent.SELECTED)
                    updateIFDescr();
            }
            
        });
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
     * Accepts oly digits and commatas
     * 
     * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
     */
    private class NumberAndCommaDocument extends PlainDocument {
        public void insertString(int offs, String str, AttributeSet a) 
            throws BadLocationException {

            if (str == null) {
                return;
            }
            StringBuilder b = new StringBuilder();
            char[] chars = str.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (Character.isDigit(chars[i]) ||
                    chars[i] == ',')
                    b.append(chars[i]);
            }
            super.insertString(offs, b.toString(), a);
        }
    }
    
    /*
     * 
     */
    protected JCheckBox createCheckBoxListener(ItemListener listener) {
        onStartUpdateBox.addItemListener(listener);
        return onStartUpdateBox;
    }

    protected JButton createUpdateButton(ActionListener listener) {
        updateButton = new JButton(Translation
            .getTranslation("preferences.dialog.dyndnsUpdateButton"));
        updateButton.addActionListener(listener);
        return updateButton;
    }

    /**
     * Creates a language chooser, which contains the supported locales
     * 
     * @return
     */
    private JComboBox createLanguageChooser() {
        // Create combobox
        JComboBox chooser = new JComboBox();
        Locale[] locales = Translation.getSupportedLocales();
        for (int i = 0; i < locales.length; i++) {
            chooser.addItem(locales[i]);
        }
        // Set current locale as selected
        chooser.setSelectedItem(Translation.getResourceBundle().getLocale());

        // Add renderer
        chooser.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
                if (value instanceof Locale) {
                    Locale locale = (Locale) value;
                    setText(locale.getDisplayName(locale));
                } else {
                    setText("- unknown -");
                }
                return this;
            }
        });

        return chooser;
    }

    /**
     * Build the ui theme chooser combobox
     */
    private JComboBox createThemeChooser() {
        JComboBox chooser = new JComboBox();
        final PlasticTheme[] availableThemes = ThemeSupport
            .getAvailableThemes();
        for (int i = 0; i < availableThemes.length; i++) {
            chooser.addItem(availableThemes[i].getName());
            if (availableThemes[i].getClass().getName().equals(
                getUIController().getUIThemeConfig()))
            {
                chooser.setSelectedIndex(i);
            }
        }
        chooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                PlasticTheme theme = availableThemes[colorThemeChooser
                    .getSelectedIndex()];
                ThemeSupport.setPlasticTheme(theme, getUIController()
                    .getMainFrame().getUIComponent(), getUIComponent());
                getUIComponent().pack();
            }
        });
        return chooser;
    }

    /**
     * Creates a X behavior chooser, writes settings into model
     * 
     * @param xBehaviorModel
     *            the behavior model, writes true if should exit program, false
     *            if minimize to system is choosen
     * @return the combobox
     */
    private JComboBox createXBehaviorChooser(final ValueModel xBehaviorModel) {
        // Build combobox model
        ComboBoxAdapter model = new ComboBoxAdapter(new Object[]{Boolean.FALSE,
            Boolean.TRUE}, xBehaviorModel);

        // Create combobox
        JComboBox chooser = new JComboBox(model);

        // Add renderer
        chooser.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
                if (((Boolean) value).booleanValue()) {
                    setText(Translation
                        .getTranslation("preferences.dialog.xbehavior.exit"));
                } else {
                    setText(Translation
                        .getTranslation("preferences.dialog.xbehavior.minimize"));
                }
                return this;
            }
        });

        return chooser;
    }

    // Dialogs ****************************************************************

    /**
     * Asks user about restart and executes that if requested
     */
    private void handleRestartRequest() {
        int result = getUIController().showOKCancelDialog(null,
            Translation.getTranslation("preferences.dialog.restarttitle"),
            Translation.getTranslation("preferences.dialog.restarttext"));

        if (result == JOptionPane.OK_OPTION) {
            getController().shutdownAndRequestRestart();
        }
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("preferences.dialog.title");
    }

    protected Icon getIcon() {
        return Icons.PREFERENCES;
    }

    protected Component getContent() {
        // Initalize components
        initComponents();

        JPanel generalPanel = createGeneralPanel();
        JPanel dyndnsPanel = createDynDnsSettingsPanel();
        JPanel advancedPanel = createAdvancedSettingsPanel();

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(Translation
            .getTranslation("preferences.dialog.generalTabbedPane"), null,
            generalPanel, null);
        tabbedPane.addTab(Translation
            .getTranslation("preferences.dialog.dyndnsSettingsTabbedPane"),
            null, dyndnsPanel, null);
        tabbedPane.addTab(Translation
            .getTranslation("preferences.dialog.advancedSettingsTabbedPane"),
            null, advancedPanel, null);

        tabbedPane.setSelectedIndex(0);
        // Enable only if dyndns host ist set
        tabbedPane.setEnabledAt(1, !StringUtils.isBlank((String) mydnsndsModel
            .getValue()));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        // Dimension tabDims = new Dimension(Sizes.dialogUnitXAsPixel(300,
        // tabbedPane), Sizes.dialogUnitXAsPixel(220, tabbedPane));
        // tabbedPane.setPreferredSize(tabDims);

        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        builder.add(tabbedPane, cc.xy(1, 1));

        return builder.getPanel();
    }

    /**
     * Builds general ui panel
     */
    private JPanel createGeneralPanel() {
        FormLayout layout = new FormLayout(
            "right:pref, 7dlu, 30dlu, 3dlu, 15dlu, 10dlu, 30dlu, 30dlu, pref:g",
            // "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu,
            // pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 7dlu, pref, 7dlu");
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, top:pref, 3dlu, top:pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 7dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("0, 0, 0, 7dlu"));
        CellConstraints cc = new CellConstraints();
        int row = 1;

        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.nickname")), cc.xy(1, row));
        builder.add(nickField, cc.xywh(3, row, 7, 1));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.createdesktopshortcuts")), cc
            .xy(1, row));
        builder.add(createDesktopShortcutsBox, cc.xy(3, row));

        row += 2;
        JLabel pnLabel = builder.addLabel(Translation
            .getTranslation("preferences.dialog.privatenetworking"), cc.xy(1,
            row));
        pnLabel.setToolTipText(Translation
            .getTranslation("preferences.dialog.privatenetworking.tooltip"));
        builder.add(privateNetwokringBox, cc.xy(3, row));

        row += 2;
        builder.add(myDnsLabel, cc.xy(1, row));
        builder.add(myDnsField, cc.xywh(3, row, 7, 1));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.linesettings")), cc.xy(1, row));
//        builder.add(ulLimitField, cc.xy(3, row));
//        builder.addLabel("KB/s", cc.xy(5, row));
        builder.add(wanSpeed, cc.xywh(3, row, 7, 1));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.lanlinesettings")), cc.xy(1, row));
        builder.add(lanSpeed, cc.xywh(3, row, 7, 1));
        
        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.language")), cc.xy(1, row));
        builder.add(languageChooser, cc.xywh(3, row, 7, 1));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.xbehavior")), cc.xy(1, row));
        builder.add(xBehaviorChooser, cc.xywh(3, row, 7, 1));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.colortheme")), cc.xy(1, row));
        builder.add(colorThemeChooser, cc.xywh(3, row, 7, 1));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("preferences.dialog.basedir")), cc.xy(1, row));
        builder.add(localBaseSelectField, cc.xywh(3, row, 7, 1));

        // Add info for non-windows systems
        if (!Util.isWindowsSystem()) {
            builder.appendRow(new RowSpec("pref"));
            builder.appendRow(new RowSpec("7dlu"));
            // Add info for non-windows system
            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.nonwindowsinfo")), cc.xywh(
                1, row, 7, 1));
        }
        builder.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
        JPanel panel = builder.getPanel();
        return panel;
    }

    /*
     * Builds DynDns UI panel
     */
    private JPanel createDynDnsSettingsPanel() {
        FormLayout layout = new FormLayout(
            "right:120dlu:g, 7dlu, 80dlu, 3dlu, left:40dlu:g",
            "pref, 3dlu, pref, 7dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, "
                + "3dlu, pref, 7dlu, pref, 7dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("2dlu, 0, 0, 7dlu"));
        CellConstraints cc = new CellConstraints();
        int row = 1;

        builder.addLabel(Translation
            .getTranslation("preferences.dialog.dyndnsLoginPanel"), cc.xy(1,
            row));
        row += 2;

        builder
            .addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsHostname"), cc.xy(1,
                row));
        builder.add(dyndnsHost, cc.xywh(3, row, 3, 1));

        row += 2;
        builder
            .addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsUserName"), cc.xy(1,
                row));
        builder.add(dyndnsUserField, cc.xywh(3, row, 3, 1));

        row += 2;
        dyndnsPasswordField.setEchoChar('*');
        builder
            .addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsPassword"), cc.xy(1,
                row));
        builder.add(dyndnsPasswordField, cc.xywh(3, row, 3, 1));

        row += 4;
        builder.addLabel(Translation
            .getTranslation("preferences.dialog.dyndnsCurrentIP"), cc
            .xy(1, row));
        builder.add(currentIPField, cc.xywh(3, row, 3, 1));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("preferences.dialog.dyndnsUpdatedIP"), cc
            .xy(1, row));
        builder.add(updatedIPField, cc.xywh(3, row, 3, 1));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("preferences.dialog.dyndnsUpdateOnStart"), cc.xy(1,
            row));
        builder.add(onStartUpdateBox, cc.xywh(3, row, 3, 1));

        row += 2;
        builder.add(updateButton, cc.xy(3, row));

        return builder.getPanel();
    }

    /**
     * Creates the JPanel for advanced settings
     * @return the created panel
     */
    protected JPanel createAdvancedSettingsPanel() {
        FormLayout layout = new FormLayout("3dlu, right:pref, 3dlu, pref:grow, 3dlu", 
            "3dlu, pref, 3dlu, pref, 3dlu, top:pref:grow, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        
        int row = 2;
        builder.addLabel(Translation
            .getTranslation("preferences.dialog.advPort"), cc.xy(2, row))
            .setToolTipText(Translation
            .getTranslation("preferences.dialog.advPort.tooltip"));
        builder.add(advPort, cc.xy(4, row));
        row += 2;
        builder.addLabel(Translation
            .getTranslation("preferences.dialog.bind"), cc.xy(2, row))
            .setToolTipText(Translation
                .getTranslation("preferences.dialog.bind.tooltip"));
        builder.add(bindAddress, cc.xy(4, row));
        
        row += 2;
        ifDescr.setBorder(new TitledBorder(Translation
            .getTranslation("preferences.dialog.bindDescr")));
        builder.add(ifDescr, cc.xy(4, row));
        return builder.getPanel();
    }
    
    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }
    
    private class InterfaceChoice {
        private NetworkInterface netInterface;
        private InetAddress address;
        private String showString;
        
        public InterfaceChoice(NetworkInterface netInterface, InetAddress address) {
            this.netInterface = netInterface;
            this.address = address;
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < address.getAddress().length; i++) {
                if (i > 0)
                    sb.append('.');
                sb.append(address.getAddress()[i] & 0xff);
            }
//            sb.append(" (").append(netInterface.getDisplayName().trim()).append(')');
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
}
