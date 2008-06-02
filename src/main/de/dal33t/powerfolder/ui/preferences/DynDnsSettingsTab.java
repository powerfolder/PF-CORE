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

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SwingWorker;

public class DynDnsSettingsTab extends PFComponent implements PreferenceTab {
    // disposition constants for status messages
    final static int DISP_INFO = 0; // just informing message
    final static int DISP_WARNING = 1; // warning
    final static int DISP_ERROR = 2; // error

    // FIXME: This is very ugly(tm) public vars!
    public static String password;
    public static String username;
    public static String newDyndns;
    public static String dyndnsSystem;

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
        return Translation.getTranslation("preferences.dialog.dyndns.title");
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
        if (!StringUtils.isBlank(theDyndnsHost)) {
            ConfigurationEntry.DYNDNS_HOSTNAME.setValue(getController(),
                theDyndnsHost);
        } else {
            ConfigurationEntry.DYNDNS_HOSTNAME.removeValue(getController());
        }

        if (!StringUtils.isBlank(theDyndnsHost)) {
            if (!StringUtils.isBlank(dyndnsUserField.getText())) {
                ConfigurationEntry.DYNDNS_USERNAME.setValue(getController(),
                    dyndnsUserField.getText());
            } else {
                ConfigurationEntry.DYNDNS_USERNAME.removeValue(getController());
            }

            String thePassword = new String(dyndnsPasswordField.getPassword());
            if (!StringUtils.isBlank(thePassword)) {
                ConfigurationEntry.DYNDNS_PASSWORD.setValue(getController(),
                    thePassword);
            } else {
                ConfigurationEntry.DYNDNS_PASSWORD.removeValue(getController());
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
                "right:100dlu, 3dlu, 80dlu, 3dlu, left:40dlu",
                "pref, 3dlu, pref, 7dlu, pref, 4dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, "
                    + "3dlu, pref, 7dlu, pref, 7dlu");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 0, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.add(myDnsLabel, cc.xy(1, row));
            builder.add(myDnsField, cc.xywh(3, row, 1, 1));
            // FIXME correct URL
            builder.add(Help.createWikiLinkLabel(Translation
                .getTranslation("general.what_is_this"), "DYN-Dns"),
                cc.xy(5, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsAutoUpdate"), cc.xy(
                1, row));
            builder.add(cbAutoUpdate, cc.xywh(3, row, 3, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsLoginPanel"), cc.xy(
                1, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsUserName"), cc.xy(1,
                row));
            builder.add(dyndnsUserField, cc.xywh(3, row, 3, 1));

            row += 2;
            dyndnsPasswordField.setEchoChar('*');
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsPassword"), cc.xy(1,
                row));
            builder.add(dyndnsPasswordField, cc.xywh(3, row, 3, 1));

            row += 4;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsCurrentIP"), cc.xy(1,
                row));
            builder.add(currentIPField, cc.xywh(3, row, 3, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.dyndnsUpdatedIP"), cc.xy(1,
                row));
            builder.add(updatedIPField, cc.xywh(3, row, 3, 1));

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
        myDnsLabel = new LinkLabel(Translation
            .getTranslation("preferences.dialog.dyndns"),
            "http://www.powerfolder.com/node/guide_supernode");

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

        cbAutoUpdate = SimpleComponentFactory.createCheckBox();
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
        log().debug(
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
            getController()).booleanValue();
    }

    private JButton createUpdateButton(ActionListener listener) {
        updateButton = new JButton(Translation
            .getTranslation("preferences.dialog.dyndnsUpdateButton"));
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

    private final class UpdateDynDnsAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    updateButton.setEnabled(false);

                    username = dyndnsUserField.getText();
                    password = new String(dyndnsPasswordField.getPassword());
                    newDyndns = (String) mydnsndsModel.getValue();
                    if (dyndnsUserField.getText().equals("")) {
                        dyndnsUserField.grabFocus();
                    } else if (new String(dyndnsPasswordField.getPassword())
                        .equals(""))
                    {
                        dyndnsPasswordField.grabFocus();
                    }

                    if (!StringUtils.isEmpty(newDyndns)
                        && !dyndnsUserField.getText().equals("")
                        && !new String(dyndnsPasswordField.getPassword())
                            .equals(""))
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
