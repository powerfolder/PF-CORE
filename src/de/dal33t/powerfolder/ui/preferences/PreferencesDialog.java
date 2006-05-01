package de.dal33t.powerfolder.ui.preferences;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTabbedPane;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

public class PreferencesDialog extends BaseDialog {

    private ValueModel mydnsndsModel;
    private JButton okButton;
    private JButton cancelButton;
    private List<PreferenceTab> preferenceTabs;
    private JTabbedPane tabbedPane;
    static final int DYNDNS_TAB_INDEX = 1;
    static final int GENERAL_TAB_INDEX = 0;

    public PreferencesDialog(Controller controller) {
        super(controller, true, false);
        preferenceTabs = new ArrayList<PreferenceTab>();
    }
    public JDialog getDialog() {
        return getUIComponent();
    }
    public String getTitle() {
        return Translation.getTranslation("preferences.dialog.title");
    }

    protected Icon getIcon() {
        return Icons.PREFERENCES;
    }

    void enableTab(int index, boolean flag) {
        tabbedPane.setEnabledAt(index, flag);
    }

    void selectTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public Component getContent() {
        mydnsndsModel = new ValueHolder(getController().getConfig()
            .getProperty("mydyndns"));
        mydnsndsModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String dyndns = (String) evt.getNewValue();
                // Enable tab when dyndns host ist set
                tabbedPane.setEnabledAt(1, !StringUtils.isBlank(dyndns));
            }
        });
        initComponents();
        GeneralSettingsTab generalSettingsTab = new GeneralSettingsTab(
            getController(), this, mydnsndsModel);
        DynDnsSettingsTab dynDnsSettingsTab = new DynDnsSettingsTab(
            getController(), this, mydnsndsModel);
        AdvangedSettingsTab advangedSettingsTab = new AdvangedSettingsTab(
            getController());
        PluginSettingsTab pluginSettingsTab = new PluginSettingsTab(
            getController(), this);

        preferenceTabs.add(generalSettingsTab);
        preferenceTabs.add(dynDnsSettingsTab);
        preferenceTabs.add(advangedSettingsTab);
        preferenceTabs.add(pluginSettingsTab);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(generalSettingsTab.getTabName(), null,
            generalSettingsTab.getUIPanel(), null);
        tabbedPane.addTab(dynDnsSettingsTab.getTabName(), null,
            dynDnsSettingsTab.getUIPanel(), null);
        tabbedPane.addTab(advangedSettingsTab.getTabName(), null,
            advangedSettingsTab.getUIPanel(), null);
        tabbedPane.addTab(pluginSettingsTab.getTabName(), null,
            pluginSettingsTab.getUIPanel(), null);

        tabbedPane.setSelectedIndex(0);
        // Enable only if dyndns host ist set
        tabbedPane.setEnabledAt(DYNDNS_TAB_INDEX, !StringUtils
            .isBlank((String) mydnsndsModel.getValue()));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        builder.add(tabbedPane, cc.xy(1, 1));

        return builder.getPanel();
    }

    public void initComponents() {
        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //
                // OK button event handler
                //
                // disable the OK button while we are in the
                // working thread (i.e. prohibit re-entrance)
                okButton.setEnabled(false);

                // since we are performing a validation
                // that could take some time we need to warn the user about it.
                // However updating the gui while the task is progressing,
                // requires
                // us to run the validation in a new thread that will
                // give the chance of the swing thread to update the GUI
                new Thread("Preferences saver/validator") {
                    public void run() {

                        // validate the user input and check the result
                        boolean succes = validateSettings();                        
                        if (!succes) {
                            okButton.setEnabled(true);
                            return;
                        }

                        // Save settings
                        saveSettings();
                        if (needsRestart()) {
                            handleRestartRequest();
                        }
                        close();

                    } // end run
                }.start(); // start the working thread
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
                undoChanges();
            }
        });
    }

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

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    private void saveSettings() {
        for (PreferenceTab tab : preferenceTabs) {
            tab.save();
        }
    }

    /**
     * call undoChanges on all tabs, those changes that where done immediately
     * like theme change
     */
    private void undoChanges() {
        for (PreferenceTab tab : preferenceTabs) {
            tab.undoChanges();
        }
    }

    /**
     * Validates the settings before saving them persistantly
     */
    private boolean validateSettings() {
        for (PreferenceTab tab : preferenceTabs) {
            boolean succes = tab.validate();
            if (!succes) {
                return false;
            }
        }
        return true;
    }

    private boolean needsRestart() {
        for (PreferenceTab tab : preferenceTabs) {
            if (tab.needsRestart()) {
                return true;
            }
        }
        return false;
    }
}
