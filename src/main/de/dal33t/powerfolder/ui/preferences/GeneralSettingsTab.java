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

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Dictionary;

import javax.swing.*;

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

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.ui.ArchiveModeSelectorPanel;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;
    private JTextField nickField;
    private JCheckBox createDesktopShortcutsBox;

    private JCheckBox startWithWindowsBox;

    private JCheckBox massDeleteBox;
    private JSlider massDeleteSlider;

    private JCheckBox showAdvancedSettingsBox;
    private ValueModel showAdvancedSettingsModel;

    private JCheckBox backupOnlyClientBox;

    private JCheckBox usePowerFolderIconBox;
    private JCheckBox usePowerFolderLink;

    private ArchiveModeSelectorPanel archiveModeSelectorPanel;
    private ValueModel modeModel;
    private ValueModel versionModel;

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
            showAdvancedSettingsModel, Translation
                .getTranslation("preferences.dialog.show_advanced"));

        backupOnlyClientBox = BasicComponentFactory.createCheckBox(
                backupOnlyClientModel, Translation
                .getTranslation("preferences.dialog.backup_only_clinet"));

        ValueModel massDeleteModel = new ValueHolder(
                PreferencesEntry.MASS_DELETE_PROTECTION.getValueBoolean(getController()));
        massDeleteBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(massDeleteModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.use_mass_delete"));
        massDeleteBox.addItemListener(new MassDeleteItemListener());
        massDeleteSlider = new JSlider(20, 100, PreferencesEntry.
                MASS_DELETE_THRESHOLD.getValueInt(getController()));
        massDeleteSlider.setMajorTickSpacing(20);
        massDeleteSlider.setMinorTickSpacing(5);
        massDeleteSlider.setPaintTicks(true);
        massDeleteSlider.setPaintLabels(true);
        Dictionary<Integer, JLabel> dictionary = new Hashtable<Integer, JLabel>();
        for (int i = 20; i <= 100; i += massDeleteSlider.getMajorTickSpacing()) {
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

            if (WinUtils.getInstance() != null && !OSUtil.isWebStart()) {
                ValueModel startWithWindowsVM = new ValueHolder(WinUtils
                    .getInstance().isPFStartup());
                startWithWindowsVM
                    .addValueChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            try {
                                if (WinUtils.getInstance() != null) {
                                    WinUtils.getInstance().setPFStartup(
                                        evt.getNewValue().equals(true));
                                }
                            } catch (IOException e) {
                                logSevere("IOException", e);
                            }
                        }
                    });
                ValueModel tmpModel = new BufferedValueModel(
                    startWithWindowsVM, writeTrigger);
                startWithWindowsBox = BasicComponentFactory
                    .createCheckBox(
                        tmpModel,
                        Translation
                            .getTranslation("preferences.dialog.start_with_windows"));
            }

            if (OSUtil.isWindowsSystem()) {
                ValueModel pfiModel = new ValueHolder(
                    ConfigurationEntry.USE_PF_ICON
                        .getValueBoolean(getController()));
                usePowerFolderIconBox = BasicComponentFactory.createCheckBox(
                    new BufferedValueModel(pfiModel, writeTrigger), Translation
                        .getTranslation("preferences.dialog.use_pf_icon"));

                ValueModel pflModel = new ValueHolder(
                    ConfigurationEntry.USE_PF_LINK
                        .getValueBoolean(getController()));
                usePowerFolderLink = BasicComponentFactory.createCheckBox(
                    new BufferedValueModel(pflModel, writeTrigger), Translation
                        .getTranslation("preferences.dialog.show_pf_link"));
            }
        }

        modeModel = new ValueHolder();
        versionModel = new ValueHolder();
        archiveModeSelectorPanel = new ArchiveModeSelectorPanel(getController(),
                modeModel, versionModel);
        archiveModeSelectorPanel.setArchiveMode(ArchiveMode.valueOf(
                PreferencesEntry.DEFAULT_ARCHIVE_MODE.getValueString(
                        getController())), PreferencesEntry.
                DEFAULT_ARCHIVE_VERIONS.getValueInt(getController()));
    }

    /**
     * Builds general ui panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                    .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.nickname")), cc.xy(1, row));
            builder.add(nickField, cc.xy(3, row));

            row += 2;
            builder.add(massDeleteBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.mass_delete_threshold")),
                    cc.xy(1, row));
            builder.add(massDeleteSlider, cc.xy(3, row));

            // Add info for non-windows systems
            if (OSUtil.isWindowsSystem()) { // Windows System
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                builder.appendRow("3dlu");
                builder.appendRow("pref");

                row += 2;
                builder.add(createDesktopShortcutsBox, cc.xyw(3, row, 2));

                if (startWithWindowsBox != null) {
                    row += 2;
                    builder.add(startWithWindowsBox, cc.xyw(3, row, 2));
                }

                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(usePowerFolderIconBox, cc.xyw(3, row, 2));

                // Links only available in Vista
                if (OSUtil.isWindowsVistaSystem()) {
                    builder.appendRow("3dlu");
                    builder.appendRow("pref");
                    row += 2;
                    builder.add(usePowerFolderLink, cc.xyw(3, row, 2));
                }
            } else {
                builder.appendRow("3dlu");
                builder.appendRow("pref");

                row += 2;
                builder.add(new JLabel(Translation
                    .getTranslation("preferences.dialog.non_windows_info"),
                    SwingConstants.CENTER), cc.xyw(1, row, 4));
            }

            row += 2;
            builder.add(backupOnlyClientBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(showAdvancedSettingsBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation(
                    "preferences.dialog.default_archive_mode.text")), cc.xy(1, 
                    row));
            builder.add(archiveModeSelectorPanel.getUIComponent(), cc.xyw(3,
                    row, 2));

            panel = builder.getPanel();
        }
        return panel;
    }

    public void undoChanges() {
    }

    /**
     * Enable the mass delete slider if the box is selected.
     */
    private void enableMassDeleteSlider() {
        massDeleteSlider.setEnabled(massDeleteBox.isSelected());
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

        // set bu only
        if (!ConfigurationEntry.BACKUP_ONLY_CLIENT.getValue(getController())
                .equals(String.valueOf(backupOnlyClientBox.isSelected()))) {
            needsRestart = true;
        }
        ConfigurationEntry.BACKUP_ONLY_CLIENT.setValue(getController(),
            String.valueOf(backupOnlyClientBox.isSelected()));

        if (usePowerFolderIconBox != null) {
            // PowerFolder icon
            ConfigurationEntry.USE_PF_ICON.setValue(getController(), Boolean
                .toString(usePowerFolderIconBox.isSelected()));
        }

        if (usePowerFolderLink != null) {
            boolean oldValue = Boolean.parseBoolean(ConfigurationEntry
                    .USE_PF_LINK.getValue(getController()));
            boolean newValue = usePowerFolderLink.isSelected();
            if (oldValue ^ newValue) {
                configureFavorite(newValue);
            }
            // PowerFolder favorite
            ConfigurationEntry.USE_PF_LINK.setValue(getController(), Boolean
                .toString(usePowerFolderLink.isSelected()));
        }

        PreferencesEntry.MASS_DELETE_PROTECTION.setValue(getController(),
                massDeleteBox.isSelected());
        PreferencesEntry.MASS_DELETE_THRESHOLD.setValue(getController(),
                massDeleteSlider.getValue());

        PreferencesEntry.DEFAULT_ARCHIVE_MODE.setValue(getController(),
                ((ArchiveMode) modeModel.getValue()).name());
        PreferencesEntry.DEFAULT_ARCHIVE_VERIONS.setValue(getController(),
                (Integer) versionModel.getValue());
    }

    private void configureFavorite(boolean newValue) {
        try {
            WinUtils.getInstance().setPFFavorite(newValue, getController());
        } catch (IOException e) {
            logSevere(e);
        }
    }
    private class MassDeleteItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            enableMassDeleteSlider();
        }
    }
}
