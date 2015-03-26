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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.panel.ArchiveModeSelectorPanel;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.update.ManuallyInvokedUpdateHandler;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.update.Updater;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;
    private JTextField nickField;
    private JCheckBox runOnStartupBox;
    private JCheckBox updateCheck;
    private boolean originalQuitOnX;
    private JComponent locationField;
    private JTextField locationTF;
    private ValueModel locationModel;
    private JComboBox<String> xBehaviorChooser;
    private ArchiveModeSelectorPanel archiveModeSelectorPanel;
    private ValueModel versionModel;
    private JComboBox<String> archiveCleanupCombo;
    private Action cleanupAction;
    private JComboBox<Locale> languageChooser;
    private JCheckBox modeChooser;
    
    private boolean needsRestart;

    public GeneralSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.get("preferences.general.title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public boolean validate() {
        return true;
    }

    // Exposing *************************************************************

    /**
     * Initializes all needed ui components
     */
    private void initComponents() {

        languageChooser = createLanguageChooser();

        nickField = new JTextField(getController().getMySelf().getNick());

        updateCheck = new JCheckBox(Translation.get("preferences.general.check_for_program_updates"));
        updateCheck.setSelected(PreferencesEntry.CHECK_UPDATE.getValueBoolean(getController()));

        xBehaviorChooser = createXBehaviorChooser();

        if (OSUtil.isStartupItemSupported()) {
            runOnStartupBox = new JCheckBox(
                Translation
                    .get("preferences.general.start_with_windows"));
            try {
                runOnStartupBox.setSelected(OSUtil.hasPFStartup(getController()));
            } catch (UnsupportedOperationException uoe) {

                runOnStartupBox = null;
            }
        }

        versionModel = new ValueHolder();
        archiveModeSelectorPanel = new ArchiveModeSelectorPanel(
            getController(), versionModel);
        archiveModeSelectorPanel.setArchiveMode(
                ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS.getValueInt(getController()));

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
        
        archiveCleanupCombo = new JComboBox<String>();
        archiveCleanupCombo.addItem(Translation
            .get("preferences.general.archive_cleanup_day")); // 1
        archiveCleanupCombo.addItem(Translation
            .get("preferences.general.archive_cleanup_week")); // 7
        archiveCleanupCombo.addItem(Translation
            .get("preferences.general.archive_cleanup_month")); // 31
        archiveCleanupCombo.addItem(Translation
            .get("preferences.general.archive_cleanup_year")); // 365
        archiveCleanupCombo.addItem(Translation
            .get("preferences.general.archive_cleanup_never")); // 2147483647
        int cleanup = ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS
            .getValueInt(getController());
        switch (cleanup) {
            case 1 :
                archiveCleanupCombo.setSelectedIndex(0);
                break;
            case 7 :
                archiveCleanupCombo.setSelectedIndex(1);
                break;
            case 31 :
            default :
                archiveCleanupCombo.setSelectedIndex(2);
                break;
            case 365 :
                archiveCleanupCombo.setSelectedIndex(3);
                break;
            case Integer.MAX_VALUE :
                archiveCleanupCombo.setSelectedIndex(4);
                break;
            case 0 :
                archiveCleanupCombo.setSelectedIndex(4);
                break;
        }

        cleanupAction = new MyCleanupAction(getController());

        // +++ PFC-2385
        modeChooser = new JCheckBox();
        modeChooser.setSelected(PreferencesEntry.MODE_SELECT.getValueBoolean(getController()));
        modeChooser.setText(Translation.get("preferences_show_advaned_options"));
        // END PFC-2385
    }

    /**
     * Builds general ui panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 163dlu, pref:grow",
                "pref, 10dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 8dlu, pref, 0dlu, pref, 0dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 3;

            // Start: PFC-2385
            if (PreferencesEntry.MODE_SELECT.getValueBoolean(getController())) {
                row += 2;
                builder.add(
                    new JLabel(Translation
                        .get("preferences.general.mode.title")), cc.xy(
                        1, row));
                builder.add(modeChooser, cc.xy(3, row));
            }
            // End: PFC-2385
            
            // Start: PFC-2631
            row += 2;
            builder.add(
                new JLabel(Translation
                    .get("exp.preferences.expert.base_dir")), cc.xy(1,
                    row));
            builder.add(locationField, cc.xyw(3, row, 2));
            // End: PFC-2631
            
            row += 2;
            builder.add(
                new JLabel(Translation
                    .get("preferences.general.account_label")), cc
                    .xy(1, row));
            builder.add(createChangeAccountLogoutPanel(), cc.xyw(3, row, 2));

            row += 2;
            builder.add(
                new JLabel(Translation
                    .get("preferences.general.nickname")), cc.xy(1,
                    row));
            builder.add(nickField, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation.get("preferences.general.language")), cc.xy(1, row));
            builder.add(languageChooser, cc.xy(3, row));

            if (PreferencesEntry.VIEW_ACHIVE.getValueBoolean(getController())) {
                row += 2;
                builder
                    .add(
                        new JLabel(
                            Translation
                                .get("preferences.general.default_archive_mode_text")),
                        cc.xy(1, row, CellConstraints.RIGHT, CellConstraints.TOP));
                builder.add(
                    threePanel(archiveModeSelectorPanel.getUIComponent(),
                        archiveCleanupCombo, new JButton(cleanupAction)), cc
                        .xyw(3, row, 2));
            }

            if (OSUtil.isStartupItemSupported() && runOnStartupBox != null) {
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(
                    new JLabel(Translation
                        .get("preferences.general.start_behavior")),
                    cc.xy(1, row));
                builder.add(runOnStartupBox, cc.xyw(3, row, 2));
            }

            row += 2;
            builder.add(
                new JLabel(Translation
                    .get("preferences.general.exit_behavior")), cc
                    .xy(1, row));
            builder.add(xBehaviorChooser, cc.xy(3, row));

            // PFC-2461: Completely disable updates via preferences
            if (ConfigurationEntry.ENABLE_UPDATE.getValueBoolean(getController())) {
                row += 2;
                builder.add(new JLabel(Translation.get("preferences.general.check_for_updates_text")), cc.xy(1, row));
                builder.add(updateCheck, cc.xy(3, row));

                row +=2;
                builder.add(createUpdateCheckPanel(), cc.xyw(3, row, 2));
            }

            panel = builder.getPanel();
        }
        return panel;
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

        JButton locationButton = new JButtonMini(
            Icons.getIconById(Icons.DIRECTORY),
            Translation
                .get("exp.preferences.expert.select_directory_text"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }
    
    /**
     * Called when the location model changes value. Sets the location text
     * field value and enables the location button.
     */
    private void updateLocationComponents() {
        String value = (String) locationModel.getValue();
        locationTF.setText(value);
    }
    
    private JPanel createUpdateCheckPanel() {
        FormLayout layout = new FormLayout("80dlu, 3dlu, pref", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        //builder.add(updateCheck, cc.xy(3, 1));
        builder.add(new JLabel(Translation.get("exp.preferences.information.power_folder_text",
            Controller.PROGRAM_VERSION)), cc.xy(1,1));
        builder.add(createCheckForUpdatesButton(), cc.xy(1, 3));
        return builder.getPanel();
    }

    /**
     * Creates an internationalized check for updates button. This button will
     * invoke the manual update checker.
     */
    private JButton createCheckForUpdatesButton() {
        JButton checkForUpdatesButton = new JButton(Translation.get("preferences.general.check_for_updates_text"));
        checkForUpdatesButton.setToolTipText(Translation.get("preferences.general.check_for_updates_tips"));
        checkForUpdatesButton.setMnemonic(
                Translation.get("preferences.general.check_for_updates_key").trim().charAt(0));
        checkForUpdatesButton.addActionListener(new UpdateAction());
        checkForUpdatesButton.setBackground(Color.WHITE);
        return checkForUpdatesButton;
    }

    private JPanel createChangeAccountLogoutPanel() {
        FormLayout layout = new FormLayout("80dlu, 3dlu, 80dlu", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createChangeAccountButton(), cc.xy(1, 1));
        builder.add(createLogoutButton(), cc.xy(3, 1));
        return builder.getPanel();
    }

    private JButton createChangeAccountButton() {
        JButton changeAccountButton = new JButton(Translation.get("preferences.general.change_account_text"));
        changeAccountButton.setToolTipText(Translation.get("preferences.general.change_account_tips"));
        changeAccountButton.setMnemonic(Translation.get("preferences.general.change_account_key").trim().charAt(0));
        changeAccountButton.addActionListener(new ChangeAccountAction());
        changeAccountButton.setBackground(Color.WHITE);
        return changeAccountButton;
    }

    private JButton createLogoutButton() {
        JButton logoutButton = new JButton(
            Translation.get("preferences.general.logout_text"));
        logoutButton.setToolTipText(Translation
            .get("preferences.general.logout_tips"));
        logoutButton.setMnemonic(Translation
            .get("preferences.general.logout_key").trim().charAt(0));
        logoutButton.addActionListener(new LogoutAction());
        logoutButton.setBackground(Color.WHITE);
        return logoutButton;
    }

    private JComboBox<Locale> createLanguageChooser() {
        // Create combobox
        JComboBox<Locale> chooser = new JComboBox<>();
        for (Locale locale1 : Translation.getSupportedLocales()) {
            chooser.addItem(locale1);
        }

        // Add renderer
        chooser.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 100L;

            public Component getListCellRendererComponent(JList<?> list,
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

        // Initialize chooser with the active locale.
        chooser.setSelectedItem(Translation.getActiveLocale());

        return chooser;
    }


    private static Component threePanel(Component component1,
        Component component2, Component component3) {
        FormLayout layout = new FormLayout("80dlu, 3dlu, 80dlu", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(component1, cc.xy(1, 1));
        builder.add(component2, cc.xy(3, 1));
        builder.add(component3, cc.xy(1, 3));
        return builder.getPanel();
    }

    public void undoChanges() {
    }

    public void save() {

        // Nickname
        if (!StringUtils.isBlank(nickField.getText())) {
            getController().changeNick(nickField.getText(), false);
        }

        // Set locale
        if (languageChooser.getSelectedItem() instanceof Locale) {
            Locale locale = (Locale) languageChooser.getSelectedItem();
            // Check if we need to restart
            needsRestart |= !Util.equals(locale, Translation.getActiveLocale());
            // Save settings
            Translation.saveLocalSetting(locale);
        } else {
            // Remove setting
            Translation.saveLocalSetting(null);
        }

        PreferencesEntry.CHECK_UPDATE.setValue(getController(), updateCheck.isSelected());

        PreferencesEntry.QUIT_ON_X.setValue(getController(),
                xBehaviorChooser.getSelectedIndex() == 0); // Quit on exit.
        if (xBehaviorChooser.getSelectedIndex() == 0 ^ originalQuitOnX) {
            // Need to restart to redraw minimize button.
            needsRestart = true;
        }

        int index = archiveCleanupCombo.getSelectedIndex();
        switch (index) {
            case 0 : // 1 day
                ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS.setValue(
                    getController(), 1);
                break;
            case 1 : // 1 week
                ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS.setValue(
                    getController(), 7);
                break;
            case 2 : // 1 month
            default :
                ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS.setValue(
                    getController(), 31);
                break;
            case 3 : // 1 year
                ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS.setValue(
                    getController(), 365);
                break;
            case 4 : // never
                ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS.setValue(
                    getController(), 0);
                break;
        }

        try {
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS.setValue(
                getController(), versionModel.getValue().toString());
        } catch (Exception e) {
            logWarning("Unable to store archive settings: " + e);
        }

        if (OSUtil.isStartupItemSupported() && runOnStartupBox != null) {
            boolean oldValue = runOnStartupBox.isSelected();
            boolean changed = OSUtil.hasPFStartup(getController()) != oldValue;

            if (changed) {
                try {
                    OSUtil.setPFStartup(runOnStartupBox.isSelected(),
                        getController());
                } catch (IOException ioe) {
                    logWarning(ioe.getMessage());
                } catch (UnsupportedOperationException uoe) {
                    logWarning(uoe.getMessage());
                    DialogFactory.genericDialog(getController(), Translation
                        .get("exception.startup_item.title"), uoe
                        .getMessage(), new String[]{Translation
                        .get("general.ok")}, 0,
                        GenericDialogType.INFO);
                }
            }
        }

        // Set folder base
        FolderRepository repo = getController().getFolderRepository();
        String oldFolderBaseString = repo.getFoldersBasedirString();
        String oldBaseDirName = repo.getFoldersBasedir().getFileName().toString();
        String newFolderBaseString = (String) locationModel.getValue();
        repo.setFoldersBasedir(newFolderBaseString);
        if (!StringUtils.isEqual(oldFolderBaseString, newFolderBaseString)) {
            repo.updateShortcuts(oldBaseDirName);
        }

        
        // Start: PFC-2385
        if (PreferencesEntry.MODE_SELECT.getValueBoolean(getController())) {
            boolean expertModeActive = PreferencesEntry.EXPERT_MODE.getValueBoolean(getController());

            if(modeChooser.isSelected()) {
                PreferencesEntry.EXPERT_MODE.setValue(getController(), true); 
                if (expertModeActive) {
                    needsRestart = true;
                }
            } else {
                PreferencesEntry.EXPERT_MODE.setValue(getController(), false);
                if (!expertModeActive) {
                    needsRestart = true;
                }
            }
        }
        // End: PFC-2385

        getController().saveConfig();
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private static class MyCleanupAction extends BaseAction {

        private static final long serialVersionUID = 10L;

        private MyCleanupAction(Controller controller) {
            super("action_cleanup_archive", controller);
        }

        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getController().getFolderRepository()
                        .cleanupOldArchiveFiles();
                }
            });
        }
    }

    private class UpdateAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (getController().getUpdateSettings() != null) {
                ManuallyInvokedUpdateHandler handler = new ManuallyInvokedUpdateHandler(
                    getController());
                Updater updater = new Updater(getController(), handler);
                updater.start();
            }
            PreferencesEntry.CHECK_UPDATE.setValue(getController(), true);
        }
    }

    private class LogoutAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SwingWorker<Object, Object> logout = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    getController().getOSClient().logout();
                    return null;
                }
            };

            logout.execute();
        }
    }

    private class ChangeAccountAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SwingWorker<Object, Object> logout = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    getController().getOSClient().logout();
                    return null;
                }

            };
            logout.execute();

            PFWizard.openLoginWizard(getController(), getController()
                .getOSClient());
        }
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) locationModel.getValue();
            List<Path> files = DialogFactory.chooseDirectory(getController()
                .getUIController(), initial, false);
            if (!files.isEmpty()) {
               Path newLocation = files.get(0);
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
                                    .get("exp.preferences.expert.duplicate_local_base_title"),
                                Translation
                                    .get(
                                        "exp.preferences.expert.duplicate_local_base_message",
                                        folder.getName()),
                                GenericDialogType.ERROR);
                        return;
                    }
                }
                locationModel.setValue(newLocation.toAbsolutePath().toString());
            }
        }
    }
    
    /**
     * Creates a X behavior chooser.
     * Option 0 is Exit program
     * Option 1 is Minimize to system tray
     */
    private JComboBox<String> createXBehaviorChooser() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(Translation.get(
                "preferences.general.exit_behavior_exit"));
        if (OSUtil.isSystraySupported()) {
            model.addElement(Translation.get(
                    "preferences.general.exit_behavior_minimize"));
        }

        JComboBox<String> combo = new JComboBox<>(model);
        combo.setEnabled(OSUtil.isSystraySupported());
        if (OSUtil.isSystraySupported() &&
                !PreferencesEntry.QUIT_ON_X.getValueBoolean(
                        getController())) {
            combo.setSelectedIndex(1); // Minimize option.
        }

        originalQuitOnX = combo.getSelectedIndex() == 0;

        return combo;
    }

}
