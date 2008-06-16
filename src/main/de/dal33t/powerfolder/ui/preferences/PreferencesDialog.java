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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SwingWorker;

public class PreferencesDialog extends BaseDialog {

    private ValueModel mydnsndsModel;
    private JButton okButton;
    private JButton cancelButton;
    private List<PreferenceTab> preferenceTabs;
    private JTabbedPane tabbedPane;

    private DynDnsSettingsTab dynDnsSettingsTab;
    private AdvancedSettingsTab advancedSettingsTab;
    private static final int DYNDNS_TAB_INDEX = 3;
    private static final int ADVANCED_TAB_INDEX = 4;

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

    public ValueModel getDyndnsModel() {
        return mydnsndsModel;
    }

    void enableTab(int index, boolean flag) {
        tabbedPane.setEnabledAt(index, flag);
    }

    void selectTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    private void showTab(boolean enable, PreferenceTab tab, int tabindex) {
        Reject.ifNull(tab, "Unable to show/hide tab. Tab is null");
        Reject.ifTrue(tabindex < 0, "Unable to show/hide tab. Invalid index: "
            + tabindex);
        if (enable) {
            if (!preferenceTabs.contains(tab)) {
                preferenceTabs.add(tab);
            }
            // calculate a valid insert index before inserting
            int currentNumberOfTabs = tabbedPane.getTabCount();
            int newTabindex = Math.min(tabindex, currentNumberOfTabs);
            tabbedPane.insertTab(tab.getTabName(), null, tab.getUIPanel(),
                null, newTabindex);
        } else {
            preferenceTabs.remove(tab);
            tabbedPane.remove(tab.getUIPanel());
        }
        log().verbose("preferenceTabs: " + preferenceTabs);
        rePack();
    }

    private void showAdvancedTab(boolean enable) {
        showTab(enable, advancedSettingsTab, ADVANCED_TAB_INDEX);
    }

    void showDynDNSTab(boolean enable) {
        log().verbose("showing dyndns tab: " + enable);
        if (dynDnsSettingsTab == null) {
            // Initalize dyndns tab lazy
            dynDnsSettingsTab = new DynDnsSettingsTab(getController(),
                mydnsndsModel);
        }
        showTab(enable, dynDnsSettingsTab, DYNDNS_TAB_INDEX);
    }

    public Component getContent() {
        initComponents();

        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        builder.add(tabbedPane, cc.xy(1, 1));

        return builder.getPanel();
    }

    public void initComponents() {
        mydnsndsModel = new ValueHolder(ConfigurationEntry.DYNDNS_HOSTNAME
            .getValue(getController()));
        if (OSUtil.isMacOS()) {
            tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        } else {
            tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        }

        GeneralSettingsTab generalSettingsTab = new GeneralSettingsTab(
            getController());
        preferenceTabs.add(generalSettingsTab);
        tabbedPane.addTab("     " + generalSettingsTab.getTabName() + "      ",
            null, generalSettingsTab.getUIPanel(), null);

        NetworkSettingsTab networkSettingsTab = new NetworkSettingsTab(
            getController());
        preferenceTabs.add(networkSettingsTab);
        tabbedPane.addTab(networkSettingsTab.getTabName(), null,
            networkSettingsTab.getUIPanel(), null);

        dynDnsSettingsTab = new DynDnsSettingsTab(getController(),
            mydnsndsModel);
        preferenceTabs.add(dynDnsSettingsTab);
        tabbedPane.addTab(dynDnsSettingsTab.getTabName(), null,
            dynDnsSettingsTab.getUIPanel(), null);

        DialogsSettingsTab dialogsSettingsTab = new DialogsSettingsTab(
            getController());
        preferenceTabs.add(dialogsSettingsTab);
        tabbedPane.addTab(dialogsSettingsTab.getTabName(), null,
            dialogsSettingsTab.getUIPanel(), null);

        PluginSettingsTab pluginSettingsTab = new PluginSettingsTab(
            getController(), this);
        if (getController().getPluginManager().countPlugins() > 0) {
            preferenceTabs.add(pluginSettingsTab);
            tabbedPane.addTab(pluginSettingsTab.getTabName(), null,
                pluginSettingsTab.getUIPanel(), null);
        }
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (dynDnsSettingsTab == null) {
                    return;
                }
                if (tabbedPane.getSelectedComponent() == dynDnsSettingsTab
                    .getUIPanel())
                {
                    dynDnsSettingsTab.updateDynDnsInfo();
                }
            }
        });

        advancedSettingsTab = new AdvancedSettingsTab(getController());

        if (PreferencesEntry.SHOW_ADVANCED_SETTINGS
            .getValueBoolean(getController()))
        {
            preferenceTabs.add(advancedSettingsTab);
            tabbedPane.addTab(advancedSettingsTab.getTabName(), null,
                advancedSettingsTab.getUIPanel(), null);
        }

        // Behavior for advanced settings panel
        generalSettingsTab.getShowAdvancedSettingsModel()
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    showAdvancedTab(Boolean.TRUE.equals(evt.getNewValue()));
                }
            });
        showAdvancedTab(Boolean.TRUE.equals(generalSettingsTab
            .getShowAdvancedSettingsModel().getValue()));

        tabbedPane.setSelectedIndex(0);
        tabbedPane.setBorder(Borders.createEmptyBorder("3dlu,0,0,3dlu"));

        // Buttons
        okButton = createOKButton();
        cancelButton = createCancelButton();
    }

    /**
     * Creates the okay button for the whole pref dialog
     */
    private JButton createOKButton() {
        JButton theButton = createOKButton(new ActionListener() {
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
                // requires us to run the validation in a new thread that will
                // give the chance of the swing thread to update the GUI
                SwingWorker worker = new SwingWorker() {
                    @Override
                    public Object construct() {
                        // validate the user input and check the result
                        boolean succes = validateSettings();
                        if (!succes) {
                            return Boolean.FALSE;
                        }

                        // Save settings
                        saveSettings();
                        if (needsRestart()) {
                            handleRestartRequest();
                        }
                        return Boolean.TRUE;
                    }

                    @Override
                    public void finished() {
                        if (get() == Boolean.TRUE) {
                            setVisible(false);
                        }
                        okButton.setEnabled(true);
                    }
                };
                worker.start();
            }
        });
        return theButton;
    }

    private JButton createCancelButton() {
        return createCancelButton(new ActionListener() {
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
        int result = DialogFactory.genericDialog(getController()
            .getUIController().getMainFrame().getUIComponent(), Translation
            .getTranslation("preferences.dialog.restart.title"), Translation
            .getTranslation("preferences.dialog.restart.text"), new String[]{
            Translation.getTranslation("preferences.dialog.restart.restart"),
            Translation.getTranslation("general.cancel")}, 0,
            GenericDialogType.QUESTION); // Default is restart

        if (result == 0) { // Restart
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
        getController().saveConfig();
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
