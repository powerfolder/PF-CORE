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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.PreferencesAdapter;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.ui.ArchiveModeSelectorPanel;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;
    private JTextField nickField;
    private JCheckBox createPowerFoldersDesktopShortcutsBox;
    private JCheckBox createDesktopShortcutsBox;

    private JCheckBox startWithWindowsBox;

    private JCheckBox massDeleteBox;
    private JSlider massDeleteSlider;

    private JCheckBox showAdvancedSettingsBox;
    private ValueModel showAdvancedSettingsModel;

    private JCheckBox backupOnlyClientBox;

    private JCheckBox usePowerFolderIconBox;

    private ArchiveModeSelectorPanel archiveModeSelectorPanel;
    private ValueModel modeModel;
    private ValueModel versionModel;

    private JComboBox archiveCleanupCombo;

    private JCheckBox folderSyncCB;
    private JLabel folderSyncLabel;
    private JSlider folderSyncSlider;

    private boolean needsRestart;

    // The triggers the writing into core
    private Trigger writeTrigger;

    public GeneralSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.general.title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public boolean validate() {
        return true;
    }

    // Exposing *************************************************************

    /**
     * TODO Move this into a <code>PreferencesModel</code>
     * 
     * @return the model containing the visible-state of the advanced settings
     *         dialog
     */
    public ValueModel getShowAdvancedSettingsModel() {
        return showAdvancedSettingsModel;
    }

    /**
     * Initalizes all needed ui components
     */
    private void initComponents() {
        writeTrigger = new Trigger();

        showAdvancedSettingsModel = new ValueHolder(
            PreferencesEntry.SHOW_ADVANCED_SETTINGS
                .getValueBoolean(getController()));
        ValueModel backupOnlyClientModel = new ValueHolder(
            ConfigurationEntry.BACKUP_ONLY_CLIENT
                .getValueBoolean(getController()));

        nickField = new JTextField(getController().getMySelf().getNick());

        showAdvancedSettingsBox = BasicComponentFactory.createCheckBox(
            showAdvancedSettingsModel,
            Translation.getTranslation("preferences.dialog.show_advanced"));

        backupOnlyClientBox = BasicComponentFactory
            .createCheckBox(backupOnlyClientModel, Translation
                .getTranslation("preferences.dialog.backup_only_clinet"));

        ValueModel massDeleteModel = new ValueHolder(
            ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()));
        massDeleteBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(massDeleteModel, writeTrigger),
            Translation.getTranslation("preferences.dialog.use_mass_delete"));
        massDeleteBox.addItemListener(new MassDeleteItemListener());
        massDeleteSlider = new JSlider(20, 100,
            ConfigurationEntry.MASS_DELETE_THRESHOLD
                .getValueInt(getController()));
        massDeleteSlider.setMajorTickSpacing(20);
        massDeleteSlider.setMinorTickSpacing(5);
        massDeleteSlider.setPaintTicks(true);
        massDeleteSlider.setPaintLabels(true);
        Dictionary<Integer, JLabel> dictionary = new Hashtable<Integer, JLabel>();
        for (int i = 20; i <= 100; i += massDeleteSlider.getMajorTickSpacing())
        {
            dictionary.put(i, new JLabel(Integer.toString(i) + '%'));
        }
        massDeleteSlider.setLabelTable(dictionary);
        enableMassDeleteSlider();

        // Windows only...
        if (OSUtil.isWindowsSystem()) {

            ValueModel csModel = new PreferencesAdapter(getController()
                .getPreferences(), "createdesktopshortcuts", Boolean.TRUE);
            createDesktopShortcutsBox = BasicComponentFactory
                .createCheckBox(
                    new BufferedValueModel(csModel, writeTrigger),
                    Translation
                        .getTranslation("preferences.dialog.create_desktop_shortcuts"));

            boolean createPowerFoldersDesktopShortcut = PreferencesEntry.DISPLAY_POWERFOLDERS_SHORTCUT
                .getValueBoolean(getController());
            createPowerFoldersDesktopShortcutsBox = new JCheckBox(
                Translation
                    .getTranslation("preferences.dialog.dialogs.create_powerfolders_shotrcut"),
                createPowerFoldersDesktopShortcut);
            if (WinUtils.getInstance() != null && !OSUtil.isWebStart()) {
                startWithWindowsBox = new JCheckBox(
                    Translation
                        .getTranslation("preferences.dialog.start_with_windows"));
                startWithWindowsBox.setSelected(WinUtils.getInstance()
                    .isPFStartup(getController()));
            }

            if (OSUtil.isWindowsSystem()) {
                ValueModel pfiModel = new ValueHolder(
                    ConfigurationEntry.USE_PF_ICON
                        .getValueBoolean(getController()));
                usePowerFolderIconBox = BasicComponentFactory.createCheckBox(
                    new BufferedValueModel(pfiModel, writeTrigger), Translation
                        .getTranslation("preferences.dialog.use_pf_icon"));

            }
        }

        modeModel = new ValueHolder();
        versionModel = new ValueHolder();
        archiveModeSelectorPanel = new ArchiveModeSelectorPanel(
            getController(), modeModel, versionModel);
        archiveModeSelectorPanel.setArchiveMode(ArchiveMode
            .valueOf(ConfigurationEntry.DEFAULT_ARCHIVE_MODE
                .getValue(getController())),
            ConfigurationEntry.DEFAULT_ARCHIVE_VERIONS
                .getValueInt(getController()));

        archiveCleanupCombo = new JComboBox();
        archiveCleanupCombo.addItem(Translation.getTranslation(
                "preferences.dialog.archive_cleanup_day")); // 1
        archiveCleanupCombo.addItem(Translation.getTranslation(
                "preferences.dialog.archive_cleanup_week")); // 7
        archiveCleanupCombo.addItem(Translation.getTranslation(
                "preferences.dialog.archive_cleanup_month")); // 31
        archiveCleanupCombo.addItem(Translation.getTranslation(
                "preferences.dialog.archive_cleanup_year")); // 365
        archiveCleanupCombo.addItem(Translation.getTranslation(
                "preferences.dialog.archive_cleanup_never")); // 2147483647
        int cleanup = ConfigurationEntry.ARCHIVE_CLEANUP_DAYS.getValueInt(
                getController());
        switch (cleanup) {
            case 1:
                archiveCleanupCombo.setSelectedIndex(0);
                break;
            case 7:
                archiveCleanupCombo.setSelectedIndex(1);
                break;
            case 31:
            default:
                archiveCleanupCombo.setSelectedIndex(2);
                break;
            case 365:
                archiveCleanupCombo.setSelectedIndex(3);
                break;
            case Integer.MAX_VALUE:
                archiveCleanupCombo.setSelectedIndex(4);
                break;
        }

        folderSyncCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.folder_sync_warn.use"));
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

        dictionary = new Hashtable<Integer, JLabel>();
        dictionary.put(1, new JLabel("1"));
        dictionary.put(10, new JLabel("10"));
        dictionary.put(20, new JLabel("20"));
        dictionary.put(30, new JLabel("30"));
        folderSyncSlider.setLabelTable(dictionary);

        folderSyncLabel = new JLabel(
            Translation.getTranslation("preferences.dialog.folder_sync_text"));

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
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(
                new JLabel(Translation
                    .getTranslation("preferences.dialog.nickname")), cc.xy(1,
                    row));
            builder.add(nickField, cc.xy(3, row));

            // Add info for non-windows systems
            if (OSUtil.isWindowsSystem()) { // Windows System
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(createDesktopShortcutsBox, cc.xyw(3, row, 2));

                row += 2;
                builder.add(createPowerFoldersDesktopShortcutsBox,
                    cc.xyw(3, row, 2));

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
                builder.add(
                    new JLabel(Translation
                        .getTranslation("preferences.dialog.non_windows_info"),
                        SwingConstants.CENTER), cc.xyw(1, row, 4));
            }

            if (!getController().isBackupOnly()) {
                row += 2;
                builder.add(backupOnlyClientBox, cc.xyw(3, row, 2));
            }

            row += 2;
            builder.add(showAdvancedSettingsBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(massDeleteBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation(
                    "preferences.dialog.mass_delete_threshold")),
                    cc.xy(1, row));
            builder.add(massDeleteSlider, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation(
                    "preferences.dialog.default_archive_mode.text")),
                    cc.xy(1, row));
            builder.add(threePanel(archiveModeSelectorPanel.getUIComponent(),
                    new JLabel(Translation.getTranslation(
                            "preferences.dialog.archive_cleanup")),
                            archiveCleanupCombo),
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

    private static Component threePanel(Component component1,
                                        Component component2,
                                        Component component3) {
        FormLayout layout = new FormLayout("pref, 3dlu, pref, 3dlu, pref",
                "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(component1, cc.xy(1, 1));
        builder.add(component2, cc.xy(3, 1));
        builder.add(component3, cc.xy(5, 1));
        return builder.getPanel();
    }

    public void undoChanges() {
    }

    /**
     * Enable the mass delete slider if the box is selected.
     */
    private void enableMassDeleteSlider() {
        massDeleteSlider.setEnabled(massDeleteBox.isSelected());
    }

    private void doFolderChangeEvent() {
        folderSyncLabel.setEnabled(folderSyncCB.isSelected());
        folderSyncSlider.setEnabled(folderSyncCB.isSelected());
    }

    public void save() {
        // Write properties into core
        writeTrigger.triggerCommit();

        // Nickname
        if (!StringUtils.isBlank(nickField.getText())) {
            getController().changeNick(nickField.getText(), false);
        }

        // setAdvanced
        PreferencesEntry.SHOW_ADVANCED_SETTINGS.setValue(getController(),
            showAdvancedSettingsBox.isSelected());

        // Desktop PowerFolders shortcut.
        boolean oldValue = PreferencesEntry.DISPLAY_POWERFOLDERS_SHORTCUT
            .getValueBoolean(getController());
        boolean newValue = createPowerFoldersDesktopShortcutsBox.isSelected();
        if (oldValue ^ newValue) {
            PreferencesEntry.DISPLAY_POWERFOLDERS_SHORTCUT.setValue(
                getController(), newValue);
            getUIController().configureDesktopShortcut(false);
        }

        int index = archiveCleanupCombo.getSelectedIndex();
        switch (index) {
            case 0: // 1 day
                ConfigurationEntry.ARCHIVE_CLEANUP_DAYS.setValue(getController(), 1);
                break;
            case 1: // 1 week
                ConfigurationEntry.ARCHIVE_CLEANUP_DAYS.setValue(getController(), 7);
                break;
            case 2: // 1 month
            default:
                ConfigurationEntry.ARCHIVE_CLEANUP_DAYS.setValue(getController(),
                        31);
                break;
            case 3: // 1 year
                ConfigurationEntry.ARCHIVE_CLEANUP_DAYS.setValue(getController(),
                        365);
                break;
            case 4: // never
                ConfigurationEntry.ARCHIVE_CLEANUP_DAYS.setValue(getController(),
                        Integer.MAX_VALUE);
                break;
        }

        // set bu only
        if (!ConfigurationEntry.BACKUP_ONLY_CLIENT.getValue(getController())
            .equals(String.valueOf(backupOnlyClientBox.isSelected())))
        {
            needsRestart = true;
        }
        ConfigurationEntry.BACKUP_ONLY_CLIENT.setValue(getController(),
            String.valueOf(backupOnlyClientBox.isSelected()));

        if (usePowerFolderIconBox != null) {
            // PowerFolder icon
            ConfigurationEntry.USE_PF_ICON.setValue(getController(),
                Boolean.toString(usePowerFolderIconBox.isSelected()));
        }

        ConfigurationEntry.MASS_DELETE_PROTECTION.setValue(getController(),
            massDeleteBox.isSelected());
        ConfigurationEntry.MASS_DELETE_THRESHOLD.setValue(getController(),
            massDeleteSlider.getValue());

        try {
            ConfigurationEntry.DEFAULT_ARCHIVE_MODE.setValue(getController(),
                ((ArchiveMode) modeModel.getValue()).name());
            ConfigurationEntry.DEFAULT_ARCHIVE_VERIONS.setValue(
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
                    logWarning("IOException", e);
                }
            }
        }
    }

    private class MassDeleteItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            enableMassDeleteSlider();
        }
    }

    private class FolderChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            doFolderChangeEvent();
        }
    }

}
