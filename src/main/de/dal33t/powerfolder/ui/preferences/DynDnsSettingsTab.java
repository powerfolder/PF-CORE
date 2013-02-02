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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.Security;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;

public class DynDnsSettingsTab extends PFComponent implements PreferenceTab {

    // disposition constants for status messages
    static final int DISP_INFO = 0; // just informing message
    static final int DISP_WARNING = 1; // warning
    static final int DISP_ERROR = 2; // error

    private static String password;
    private static String username;
    private static String newDyndns;
    private static String dyndnsSystem;

    private JTextField myDnsField;
    private JLabel myDnsLabel;

    private JPanel panel;
    private JTextField dyndnsUserField;
    private JPasswordField dyndnsPasswordField;
    private JLabel currentIPField;
    private JLabel updatedIPField;
    private JCheckBox cbAutoUpdate;
    private ValueModel mydnsndsModel;
    private JButton updateButton;

    public DynDnsSettingsTab(Controller controller, ValueModel mydnsndsModel) {
        super(controller);
        this.mydnsndsModel = mydnsndsModel;
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dyn_dns.title");
    }

    public boolean needsRestart() {
        return false;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        if (mydnsndsModel.getValue() == null
            || ((String) mydnsndsModel.getValue()).trim().length() == 0)
        {
            return true;
        }
        if (!getController().getDynDnsManager().validateDynDns(
            mydnsndsModel.getValue().toString()))
        {
            // myDnsField.grabFocus();
            // myDnsField.selectAll();
            return false;
        }

        // all validations have passed
        return true;
    }

    /**
     * Saves the dyndns settings
     */
    public void save() {
        String theDyndnsHost = (String) mydnsndsModel.getValue();
        if (StringUtils.isBlank(theDyndnsHost)) {
            ConfigurationEntry.HOSTNAME.removeValue(getController());
        } else {
            ConfigurationEntry.HOSTNAME
                .setValue(getController(), theDyndnsHost);
        }

        if (!StringUtils.isBlank(theDyndnsHost)) {
            if (StringUtils.isBlank(dyndnsUserField.getText())) {
                ConfigurationEntry.DYNDNS_USERNAME.removeValue(getController());
            } else {
                ConfigurationEntry.DYNDNS_USERNAME.setValue(getController(),
                        dyndnsUserField.getText());
            }

            String thePassword = new String(dyndnsPasswordField.getPassword());
            if (StringUtils.isBlank(thePassword)) {
                ConfigurationEntry.DYNDNS_PASSWORD.removeValue(getController());
            } else {
                ConfigurationEntry.DYNDNS_PASSWORD.setValue(getController(),
                        thePassword);
            }
        }

        boolean b = cbAutoUpdate.isSelected();
        ConfigurationEntry.DYNDNS_AUTO_UPDATE.setValue(getController(), Boolean
            .valueOf(b).toString());

        // Let the DynDns manager check if he needs to do something.
        getController().getDynDnsManager().updateIfNessesary();
    }

    /*
     * Builds DynDns UI panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, "
                    + "3dlu, pref, 3dlu, pref, 3dlu");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.add(myDnsLabel, cc.xy(1, row));
            builder.add(myDnsField, cc.xy(3, row));
            
            row += 2;
            builder.add(cbAutoUpdate, cc.xy(3, row));

            row += 2;
            builder.addTitle(Translation
                .getTranslation("preferences.dyn_dns.login_panel"), cc.xy(
                1, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dyn_dns.user_name"), cc.xy(1,
                row));
            builder.add(dyndnsUserField, cc.xy(3, row));

            row += 2;
            dyndnsPasswordField.setEchoChar('*');
            builder.addLabel(Translation
                .getTranslation("preferences.dyn_dns.password"), cc.xy(1,
                row));
            builder.add(dyndnsPasswordField, cc.xy(3, row));

            row += 4;
            builder.addLabel(Translation
                .getTranslation("preferences.dyn_dns.current_ip"), cc.xy(1,
                row));
            builder.add(currentIPField, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dyn_dns.updated_ip"), cc.xy(1,
                row));
            builder.add(updatedIPField, cc.xy(3, row));

            row += 2;
            builder.add(updateButton, cc.xy(3, row));

            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        // DynDns
        myDnsField = BasicComponentFactory
            .createTextField(mydnsndsModel, false);
        myDnsField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
            }

            public void focusLost(FocusEvent e) {
                updateDynDnsInfo();
            }
        });
        myDnsLabel = new JLabel(Translation.getTranslation("preferences.dyn_dns_host_name"));

        if (ConfigurationEntry.DYNDNS_USERNAME.getValue(getController()) == null)
        {
            dyndnsUserField = new JTextField("");
        } else {
            dyndnsUserField = new JTextField(ConfigurationEntry.DYNDNS_USERNAME
                .getValue(getController()));
        }

        if (ConfigurationEntry.DYNDNS_PASSWORD.getValue(getController()) == null)
        {
            dyndnsPasswordField = new JPasswordField("");
        } else {
            dyndnsPasswordField = new JPasswordField(
                ConfigurationEntry.DYNDNS_PASSWORD.getValue(getController()));
        }

        currentIPField = new JLabel();
        updatedIPField = new JLabel();

        cbAutoUpdate = SimpleComponentFactory.createCheckBox(Translation.
                getTranslation("preferences.dyn_dns.auto_update"));
        cbAutoUpdate.setSelected(isUpdateSelected());

        updateButton = createUpdateButton(new UpdateDynDnsAction());

        mydnsndsModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                enableDisableComponents();
            }
        });
        cbAutoUpdate.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });
        enableDisableComponents();
    }

    /**
     * Starts a worker which gathers the current dyndns stuff. e.g. own ip.
     */
    protected void updateDynDnsInfo() {
        logFine(
            "Gathering dyndns infos. Cache: "
                + Security.getProperty("networkaddress.cache.ttl"));
        SwingWorker worker = new SwingWorker() {
            private String ownIP;
            private String dyndnsIP;

            @Override
            public Object construct() {
                ownIP = getController().getDynDnsManager()
                    .getIPviaHTTPCheckIP();
                // if (!isUpdateSelected()) {
                dyndnsIP = getController().getDynDnsManager().getHostIP(
                    (String) mydnsndsModel.getValue());
                // } else {
                // dyndnsIP = ConfigurationEntry.DYNDNS_LAST_UPDATED_IP
                // .getValue(getController());
                // }

                return null;
            }

            @Override
            public void finished() {
                currentIPField.setText(ownIP);
                updatedIPField.setText(dyndnsIP);
            }
        };
        worker.start();
    }

    private boolean isUpdateSelected() {
        return ConfigurationEntry.DYNDNS_AUTO_UPDATE.getValueBoolean(
                getController());
    }

    private JButton createUpdateButton(ActionListener listener) {
        updateButton = new JButton(
            Translation
                .getTranslation("preferences.dyn_dns.update_button"));
        updateButton.addActionListener(listener);
        return updateButton;
    }

    private void enableDisableComponents() {
        boolean enable = !StringUtils
            .isBlank((String) mydnsndsModel.getValue())
            && cbAutoUpdate.isSelected();
        updateButton.setEnabled(enable);
        dyndnsUserField.setEditable(enable);
        dyndnsPasswordField.setEditable(enable);
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        DynDnsSettingsTab.password = password;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        DynDnsSettingsTab.username = username;
    }

    public static String getNewDyndns() {
        return newDyndns;
    }

    public static void setNewDyndns(String newDyndns) {
        DynDnsSettingsTab.newDyndns = newDyndns;
    }

    public static String getDyndnsSystem() {
        return dyndnsSystem;
    }

    public static void setDyndnsSystem(String dyndnsSystem) {
        DynDnsSettingsTab.dyndnsSystem = dyndnsSystem;
    }

    private class UpdateDynDnsAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    updateButton.setEnabled(false);

                    setUsername(dyndnsUserField.getText());
                    setPassword(new String(dyndnsPasswordField.getPassword()));
                    setNewDyndns((String) mydnsndsModel.getValue());
                    if (dyndnsUserField.getText().length() == 0) {
                        dyndnsUserField.grabFocus();
                    } else if (new String(dyndnsPasswordField.getPassword()).length()
                            == 0)
                    {
                        dyndnsPasswordField.grabFocus();
                    }

                    if (!StringUtils.isEmpty(getNewDyndns())
                        && dyndnsUserField.getText().length() != 0
                        && new String(dyndnsPasswordField.getPassword()).length() != 0)
                    {
                        // update
                        getController().getDynDnsManager().forceUpdate();
                        updatedIPField
                            .setText(ConfigurationEntry.DYNDNS_LAST_UPDATED_IP
                                .getValue(getController()));
                    } else {
                        updateButton.setEnabled(false);
                        getController().getDynDnsManager()
                            .showPanelErrorMessage();
                    }
                    updateButton.setEnabled(true);
                }
            }.start();
        }
    }
}
