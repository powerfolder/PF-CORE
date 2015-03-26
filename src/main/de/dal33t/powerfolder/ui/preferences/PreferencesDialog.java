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
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

public class PreferencesDialog extends BaseDialog {

    private ValueModel myDynDnsModel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private List<PreferenceTab> preferenceTabs;
    private JTabbedPane tabbedPane;

    private InformationTab informationTab;
    private GeneralSettingsTab generalSettingsTab;
    private AdvancedSettingsTab advancedSettingsTab;
    private NetworkSettingsTab networkSettingsTab;
    private WarningsNotificationsSettingsTab warningsNotificationsSettingsTab;
    private DynDnsSettingsTab dynDnsSettingsTab;
    private ExpertSettingsTab expertSettingsTab;
    private PluginSettingsTab pluginSettingsTab;

    public PreferencesDialog(Controller controller) {
        super(Senior.MAIN_FRAME, controller, true);
        preferenceTabs = new ArrayList<PreferenceTab>();
    }

    public JDialog getDialog() {
        return dialog;
    }

    public String getTitle() {
        return Translation.get("preferences.dialog.title");
    }

    protected Icon getIcon() {
        return null;
    }

    public ValueModel getDynDnsModel() {
        return myDynDnsModel;
    }

    void enableTab(int index, boolean flag) {
        tabbedPane.setEnabledAt(index, flag);
    }

    void selectTab(int index) {
        tabbedPane.setSelectedIndex(index);
    }

    public JComponent getContent() {
        initComponents();

        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            return tabbedPane;
        } else {
            generalSettingsTab.getUIPanel().setBorder(Borders.createEmptyBorder("14dlu, 14dlu, 14dlu, 14dlu"));
            return generalSettingsTab.getUIPanel();
        }
    }

    public void initComponents() {
        preferenceTabs.clear();

        myDynDnsModel = new ValueHolder(
            ConfigurationEntry.HOSTNAME.getValue(getController()));

        tabbedPane = new JTabbedPane(SwingConstants.TOP,
            JTabbedPane.WRAP_TAB_LAYOUT);


        // General tab
        generalSettingsTab = new GeneralSettingsTab(getController());
        preferenceTabs.add(generalSettingsTab);
        tabbedPane.addTab(generalSettingsTab.getTabName(),
            generalSettingsTab.getUIPanel());

        // Advanced tab
        advancedSettingsTab = new AdvancedSettingsTab(getController());
        preferenceTabs.add(advancedSettingsTab);
        tabbedPane.addTab(advancedSettingsTab.getTabName(),
            advancedSettingsTab.getUIPanel());

        // Warnings and Notifications tab
        warningsNotificationsSettingsTab = new WarningsNotificationsSettingsTab(getController());
        preferenceTabs.add(warningsNotificationsSettingsTab);
        tabbedPane.addTab(warningsNotificationsSettingsTab.getTabName(),
            warningsNotificationsSettingsTab.getUIPanel());

        // Information tab
        informationTab = new InformationTab(getController());
        preferenceTabs.add(informationTab);
        tabbedPane.addTab(informationTab.getTabName(),
            informationTab.getUIPanel());

        Boolean expertMode = PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController());
        if (expertMode) {

            // Expert tab
            expertSettingsTab = new ExpertSettingsTab(getController());
            preferenceTabs.add(expertSettingsTab);
            tabbedPane.addTab(expertSettingsTab.getTabName(),
                expertSettingsTab.getUIPanel());

            // Network tab
            networkSettingsTab = new NetworkSettingsTab(getController());
            preferenceTabs.add(networkSettingsTab);
            tabbedPane.addTab(networkSettingsTab.getTabName(),
                networkSettingsTab.getUIPanel());

            // DynDns tab
            if (!getController().isBackupOnly()) {
                dynDnsSettingsTab = new DynDnsSettingsTab(getController(),
                    myDynDnsModel);
                preferenceTabs.add(dynDnsSettingsTab);
                tabbedPane.addTab(dynDnsSettingsTab.getTabName(),
                    dynDnsSettingsTab.getUIPanel());
            }

            // Plugins tab
            if (getController().getPluginManager().countPlugins() > 0) {
                pluginSettingsTab = new PluginSettingsTab(getController(), this);
                preferenceTabs.add(pluginSettingsTab);
                tabbedPane.addTab(pluginSettingsTab.getTabName(),
                    pluginSettingsTab.getUIPanel());
            }
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

        tabbedPane.setSelectedIndex(0);

        // Buttons
        okButton = createOKButton();
        cancelButton = createCancelButton();
        helpButton = createHelpButton();
    }

    private JButton createHelpButton() {
        if (!Help.hasWiki(getController())) {
            return null;
        }
        Action action = new BaseAction("action_help", getController()) {
            private static final long serialVersionUID = 100L;

            public void actionPerformed(ActionEvent e) {
                helpAction();
            }
        };
        return new JButton(action);
    }

    /**
     * Creates the okay button for the whole pref dialog
     */
    private JButton createOKButton() {
        return createOKButton(new ActionListener() {
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
                SwingWorker<Boolean , Object> worker = new SwingWorker<Boolean , Object>() {

                    protected Boolean doInBackground() throws Exception {
                        try {
                            // validate the user input and check the result
                            boolean success = validateSettings();
                            if (!success) {
                                return false;
                            }

                            // Save settings
                            saveSettings();
                            if (needsRestart()) {
                                handleRestartRequest();
                            }
                            return true;
                        } catch (Exception ex) {
                            logSevere(ex);
                            return false;
                        }
                    }

                    public void done() {
                        try {
                            if (get()) {
                                close();
                            }
                        } catch (Exception e1) {
                            logSevere(e1);
                            DialogFactory.genericDialog(getController(), Translation.get("preferences.dialog.save_error.title"),
                                    Translation.get("preferences.dialog.save_error.message"), GenericDialogType.ERROR);
                        } finally {
                            okButton.setEnabled(true);
                        }
                    }
                };
                worker.execute();
            }
        });
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
        int result = DialogFactory.genericDialog(getController(), Translation
            .get("preferences.dialog.restart.title"), Translation
            .get("preferences.dialog.restart.text"), new String[]{
            Translation.get("preferences.dialog.restart.restart")}, 0,
            GenericDialogType.INFO); // Default is restart

        if (result == 0) { // Restart
            getController().shutdownAndRequestRestart();
        }
    }

    protected Component getButtonBar() {
        if (helpButton != null) {
            return ButtonBarFactory.buildCenteredBar(helpButton, okButton,
                cancelButton);
        } else {
            return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
        }
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    private void saveSettings() {
        for (PreferenceTab tab : preferenceTabs) {
            tab.save();
        }
        getController().saveConfig();
    }

    /**
     * call undoChanges on all tabs, those changes that where done immediately
     * like laf change
     */
    private void undoChanges() {
        for (PreferenceTab tab : preferenceTabs) {
            tab.undoChanges();
        }
    }

    /**
     * Validates the settings before saving them persistently
     */
    private boolean validateSettings() {
        for (PreferenceTab tab : preferenceTabs) {
            boolean success = tab.validate();
            if (!success) {
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

    public void helpAction() {
        Component component = tabbedPane.getSelectedComponent();
        String article = "";
        if (generalSettingsTab != null
            && component == generalSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_GENERAL;
        } else if (advancedSettingsTab != null
            && component == advancedSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_UI;
        } else if (networkSettingsTab != null
            && component == networkSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_NETWORK;
        } else if (warningsNotificationsSettingsTab != null
            && component == warningsNotificationsSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_DIALOG;
        } else if (dynDnsSettingsTab != null
            && component == dynDnsSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_DYN_DNS;
        } else if (expertSettingsTab != null
            && component == expertSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_EXPERT;
        } else if (pluginSettingsTab != null
            && component == pluginSettingsTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_PLUGIN;
        } else if (informationTab != null
            && component == informationTab.getUIPanel())
        {
            article = WikiLinks.SETTINGS_PLUGIN;
        }

        String wikiArticleURL = Help
            .getWikiArticleURL(getController(), article);
        BrowserLauncher.openURL(getController(), wikiArticleURL);
    }
}
