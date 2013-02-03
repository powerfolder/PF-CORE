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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.panel.ArchiveModeSelectorPanel;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.update.ManuallyInvokedUpdateHandler;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;
import de.dal33t.powerfolder.util.update.Updater;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;
    private JTextField nickField;
    private JCheckBox createPowerFoldersDesktopShortcutsBox;
    private JCheckBox startWithWindowsBox;
    private ActionLabel startWithMacOSLabel;
    private JCheckBox folderAutoSetupBox;
    private JCheckBox updateCheck;
    private boolean originalQuitOnX;
    private JCheckBox usePowerFolderIconBox;
    private JComboBox xBehaviorChooser;
    private ArchiveModeSelectorPanel archiveModeSelectorPanel;
    private ValueModel modeModel;
    private ValueModel versionModel;
    private JComboBox archiveCleanupCombo;
    private Action cleanupAction;
    private JCheckBox folderSyncCB;
    private JLabel folderSyncLabel;
    private JSlider folderSyncSlider;
    private JComboBox languageChooser;
    private JCheckBox usePowerFolderLink;

    private boolean needsRestart;

    public GeneralSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.general.title");
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

        updateCheck = new JCheckBox(Translation.getTranslation("preferences.general.check_for_program_updates"));
        updateCheck.setSelected(PreferencesEntry.CHECK_UPDATE.getValueBoolean(getController()));

        xBehaviorChooser = createXBehaviorChooser();

        // Windows only...
        if (OSUtil.isWindowsSystem()) {

            boolean createPowerFoldersDesktopShortcut = PreferencesEntry.DISPLAY_POWERFOLDERS_SHORTCUT
                .getValueBoolean(getController());
            createPowerFoldersDesktopShortcutsBox = new JCheckBox(
                Translation.getTranslation("preferences.general.create_powerfolder_shortcut"),
                createPowerFoldersDesktopShortcut);
            if (WinUtils.getInstance() != null && !OSUtil.isWebStart()) {
                startWithWindowsBox = new JCheckBox(
                    Translation
                        .getTranslation("preferences.general.start_with_windows"));
                startWithWindowsBox.setSelected(WinUtils.getInstance()
                    .isPFStartup(getController()));
            }

            usePowerFolderIconBox = SimpleComponentFactory.createCheckBox(
                    Translation.getTranslation("preferences.general.use_pf_icon"));
            usePowerFolderIconBox.setSelected(ConfigurationEntry.USE_PF_ICON.getValueBoolean(getController()));
        }

        if (OSUtil.isWindowsVistaSystem() || OSUtil.isMacOS()) {
            usePowerFolderLink = SimpleComponentFactory.createCheckBox(
                    Translation.getTranslation("preferences.general.show_pf_link"));
            usePowerFolderLink.setSelected(PreferencesEntry.USE_PF_LINK.getValueBoolean(getController()));
        }

        if (MacUtils.isSupported()) {
            startWithMacOSLabel = new ActionLabel(getController(),
                new BaseAction("action_preferences.dialog.start_with_macosx",
                    getController())
                {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            MacUtils.getInstance().setPFStartup(true,
                                getController());
                        } catch (IOException e1) {
                            logWarning("Unable to setup auto start on logon. "
                                + e);
                        }
                    }
                });
        }

        folderAutoSetupBox = new JCheckBox(
            Translation.getTranslation("preferences.general.auto_setup_folders"));
        folderAutoSetupBox.setSelected(ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS
            .getValueBoolean(getController()));

        modeModel = new ValueHolder();
        versionModel = new ValueHolder();
        archiveModeSelectorPanel = new ArchiveModeSelectorPanel(
            getController(), modeModel, versionModel);
        archiveModeSelectorPanel.setArchiveMode(ArchiveMode
            .valueOf(ConfigurationEntry.DEFAULT_ARCHIVE_MODE
                .getValue(getController())),
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                .getValueInt(getController()));

        archiveCleanupCombo = new JComboBox();
        archiveCleanupCombo.addItem(Translation
            .getTranslation("preferences.general.archive_cleanup_day")); // 1
        archiveCleanupCombo.addItem(Translation
            .getTranslation("preferences.general.archive_cleanup_week")); // 7
        archiveCleanupCombo.addItem(Translation
            .getTranslation("preferences.general.archive_cleanup_month")); // 31
        archiveCleanupCombo.addItem(Translation
            .getTranslation("preferences.general.archive_cleanup_year")); // 365
        archiveCleanupCombo.addItem(Translation
            .getTranslation("preferences.general.archive_cleanup_never")); // 2147483647
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

        folderSyncCB = new JCheckBox(
            Translation
                .getTranslation("preferences.general.folder_sync_warn_use"));
        folderSyncCB.setSelected(ConfigurationEntry.FOLDER_SYNC_USE
            .getValueBoolean(getController()));

        folderSyncSlider = new JSlider();
        folderSyncSlider.setMinimum(1);
        folderSyncSlider.setMaximum(30);
        folderSyncSlider.setValue(ConfigurationEntry.FOLDER_SYNC_WARN_SECONDS
            .getValueInt(getController()) / 60 / 60 / 24);
        folderSyncSlider.setMinorTickSpacing(1);

        folderSyncSlider.setPaintTicks(true);
        folderSyncSlider.setPaintLabels(true);

        Dictionary<Integer, JLabel> dictionary = new Hashtable<Integer, JLabel>();
        dictionary.put(1, new JLabel("1"));
        dictionary.put(10, new JLabel("10"));
        dictionary.put(20, new JLabel("20"));
        dictionary.put(30, new JLabel("30"));
        folderSyncSlider.setLabelTable(dictionary);

        folderSyncLabel = new JLabel(Translation.getTranslation("preferences.general.folder_sync_text"));

        folderSyncCB.addChangeListener(new FolderChangeListener());

        doFolderChangeEvent();

    }

    /**
     * Builds general ui panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(
                new JLabel(Translation
                    .getTranslation("preferences.general.nickname")), cc.xy(1,
                    row));
            builder.add(nickField, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation("preferences.general.language")), cc.xy(1, row));
            builder.add(languageChooser, cc.xy(3, row));

            row += 2;
            builder.add(createUpdateCheckPanel(), cc.xyw(3, row, 2));

            if (usePowerFolderLink != null) {
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(usePowerFolderLink, cc.xyw(3, row, 2));
            }

            // Add info for non-windows systems
            if (OSUtil.isWindowsSystem()) { // Windows System

                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(createPowerFoldersDesktopShortcutsBox, cc.xyw(3, row, 2));

                if (startWithWindowsBox != null) {
                    builder.appendRow("3dlu");
                    builder.appendRow("pref");
                    row += 2;
                    builder.add(startWithWindowsBox, cc.xyw(3, row, 2));
                }

                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(usePowerFolderIconBox, cc.xyw(3, row, 2));
            } else {
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(new JLabel(Translation.getTranslation("preferences.general.non_windows_info"),
                        SwingConstants.CENTER), cc.xyw(1, row, 4));
                if (startWithMacOSLabel != null) {
                    builder.appendRow("3dlu");
                    builder.appendRow("pref");
                    row += 2;
                    builder.add(startWithMacOSLabel.getUIComponent(), cc.xyw(3, row, 2));
                }
            }

            row += 2;
            builder.add(folderAutoSetupBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation("preferences.general.exit_behavior")), cc.xy(1, row));
            builder.add(xBehaviorChooser, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation(
                    "preferences.general.default_archive_mode_text")),
                    cc.xy(1, row));
            builder.add(
                threePanel(
                        archiveModeSelectorPanel.getUIComponent(),
                        archiveCleanupCombo, new JButton(cleanupAction)),
                    cc.xyw(3, row, 2));

            row += 2;
            builder.add(folderSyncCB, cc.xyw(3, row, 2));

            row += 2;
            builder.add(folderSyncLabel, cc.xy(1, row));
            builder.add(folderSyncSlider, cc.xy(3, row));
            panel = builder.getPanel();
        }
        return panel;
    }

    private JPanel createUpdateCheckPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(updateCheck, cc.xy(1, 1));
        builder.add(createCheckForUpdatesButton(), cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Creates an internationalized check for updates button. This button will
     * invoke the manual update checker.
     */
    private JButton createCheckForUpdatesButton() {
        JButton checkForUpdatesButton = new JButton(Translation.getTranslation("preferences.general.check_for_updates_text"));
        checkForUpdatesButton.setToolTipText(Translation.getTranslation("preferences.general.check_for_updates_tips"));
        checkForUpdatesButton.setMnemonic(
                Translation.getTranslation("preferences.general.check_for_updates_key").trim().charAt(0));
        checkForUpdatesButton.addActionListener(new UpdateAction());
        checkForUpdatesButton.setBackground(Color.WHITE);
        return checkForUpdatesButton;
    }


    private JComboBox createLanguageChooser() {
        // Create combobox
        JComboBox chooser = new JComboBox();
        for (Locale locale1 : Translation.getSupportedLocales()) {
            chooser.addItem(locale1);
        }

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

        // Initialize chooser with the active locale.
        chooser.setSelectedItem(Translation.getActiveLocale());

        return chooser;
    }



    private static Component threePanel(Component component1,
        Component component2, Component component3) {
        FormLayout layout = new FormLayout("pref, 3dlu, pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(component1, cc.xy(1, 1));
        builder.add(component2, cc.xy(3, 1));
        builder.add(component3, cc.xy(5, 1));
        return builder.getPanel();
    }

    public void undoChanges() {
    }

    private void doFolderChangeEvent() {
        folderSyncLabel.setEnabled(folderSyncCB.isSelected());
        folderSyncSlider.setEnabled(folderSyncCB.isSelected());
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

        if (usePowerFolderLink != null) {
            boolean newValue = usePowerFolderLink.isSelected();
            configureLinksPlaces(newValue);
            PreferencesEntry.USE_PF_LINK.setValue(getController(), usePowerFolderLink.isSelected());
        }

        if (createPowerFoldersDesktopShortcutsBox != null) {
            // Desktop PowerFolders shortcut.
            boolean oldValue = PreferencesEntry.DISPLAY_POWERFOLDERS_SHORTCUT
                .getValueBoolean(getController());
            boolean newValue = createPowerFoldersDesktopShortcutsBox
                .isSelected();
            if (oldValue ^ newValue) {
                PreferencesEntry.DISPLAY_POWERFOLDERS_SHORTCUT.setValue(
                    getController(), newValue);
                getUIController().configureDesktopShortcut(false);
            }
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

        if (usePowerFolderIconBox != null) {
            // PowerFolder icon
            ConfigurationEntry.USE_PF_ICON.setValue(getController(),
                Boolean.toString(usePowerFolderIconBox.isSelected()));
        }

        ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS.setValue(getController(),
            folderAutoSetupBox.isSelected());
        // Re-run setup if selected.
        if (folderAutoSetupBox.isSelected()
            && getController().getOSClient().isLoggedIn()) {
            getController().schedule(new Runnable() {
                public void run() {
                    getController().getFolderRepository().updateFolders(
                        getController().getOSClient().getAccount());
                }
            }, 0);
        }

        try {
            ConfigurationEntry.DEFAULT_ARCHIVE_MODE.setValue(getController(),
                ((ArchiveMode) modeModel.getValue()).name());
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS.setValue(
                getController(), versionModel.getValue().toString());
        } catch (Exception e) {
            logWarning("Unable to store archive settings: " + e);
        }

        ConfigurationEntry.FOLDER_SYNC_USE.setValue(getController(),
            String.valueOf(folderSyncCB.isSelected()));

        ConfigurationEntry.FOLDER_SYNC_WARN_SECONDS.setValue(getController(),
            String.valueOf(folderSyncSlider.getValue() * 60 * 60 * 24));

        if (WinUtils.isSupported()) {
            boolean changed = WinUtils.getInstance().isPFStartup(
                getController()) != startWithWindowsBox.isSelected();
            if (changed) {
                try {
                    if (WinUtils.getInstance() != null) {
                        WinUtils.getInstance().setPFStartup(
                            startWithWindowsBox.isSelected(), getController());
                    }
                } catch (IOException e) {
                    logWarning("Unable to setup autostart: " + e);
                }
            }
        }

    }

    private void configureLinksPlaces(boolean newValue) {
        if (WinUtils.isSupported()) {
            try {
                WinUtils.getInstance().setPFLinks(newValue, getController());
            } catch (IOException e) {
                logSevere(e);
            }
        } else if (MacUtils.isSupported()) {
            try {
                MacUtils.getInstance().setPFPlaces(newValue, getController());
            } catch (IOException e) {
                logSevere(e);
            }
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class FolderChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            doFolderChangeEvent();
        }
    }

    private static class MyCleanupAction extends BaseAction {

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
                Updater updater = new Updater(getController(), getController()
                    .getUpdateSettings(), handler);
                updater.start();
            }
            PreferencesEntry.CHECK_UPDATE.setValue(getController(), true);
        }
    }

    /**
     * Creates a X behavior chooser.
     * Option 0 is Exit program
     * Option 1 is Minimize to system tray
     */
    private JComboBox createXBehaviorChooser() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(Translation.getTranslation(
                "preferences.general.exit_behavior_exit"));
        if (OSUtil.isSystraySupported()) {
            model.addElement(Translation.getTranslation(
                    "preferences.general.exit_behavior_minimize"));
        }

        JComboBox combo = new JComboBox(model);
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
