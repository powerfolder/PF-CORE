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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

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
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.ui.DialogFactory;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {


    private JPanel panel;
    private JTextField nickField;
    private JCheckBox createDesktopShortcutsBox;

    private JCheckBox startWithWindowsBox;

    private JTextField locationTF;
    private ValueModel locationModel;
    private JComponent locationField;

    private JSlider autoCleanupSlider;

    private JCheckBox showAdvancedSettingsBox;
    private ValueModel showAdvancedSettingsModel;

    private JCheckBox useRecycleBinBox;

    private JCheckBox usePowerFolderIconBox;

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
     * @return the model containing the visibible-state of the advanced settings
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
        nickField = new JTextField(getController().getMySelf().getNick());

        // Local base selection
        locationModel = new ValueHolder(getController().getFolderRepository()
            .getFoldersBasedir());

        // Behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocationComponents();
            }
        });

        locationField = createLocationField();

        autoCleanupSlider = new JSlider(0, 30, PreferencesEntry.
                AUTO_CLEANUP_FREQUENCY.getValueInt(getController()));
        autoCleanupSlider.setMajorTickSpacing(5);
        autoCleanupSlider.setMinorTickSpacing(1);

        autoCleanupSlider.setPaintTicks(true);
        autoCleanupSlider.setPaintLabels(true);


        showAdvancedSettingsBox = BasicComponentFactory.createCheckBox(
            showAdvancedSettingsModel, Translation
                .getTranslation("preferences.dialog.show_advanced"));

        ValueModel urbModel = new ValueHolder(
            ConfigurationEntry.USE_RECYCLE_BIN.getValueBoolean(getController()));
        useRecycleBinBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(urbModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.use_recycle_bin"));

        // Windows only...
        if (OSUtil.isWindowsSystem()) {

            ValueModel csModel = new PreferencesAdapter(getController()
                .getPreferences(), "createdesktopshortcuts", Boolean.TRUE);
            createDesktopShortcutsBox = BasicComponentFactory
                .createCheckBox(
                    new BufferedValueModel(csModel, writeTrigger),
                    Translation
                        .getTranslation("preferences.dialog.create_desktop_shortcuts"));

            if (WinUtils.getInstance() != null) {
                ValueModel startWithWindowsVM = new ValueHolder(WinUtils.getInstance()
                        .isPFStartup());
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
                startWithWindowsBox = BasicComponentFactory.createCheckBox(
                    tmpModel, Translation
                        .getTranslation("preferences.dialog.start_with_windows"));
            }

            // DesktopIni does not work on Vista
            if (OSUtil.isWindowsSystem()) {
                ValueModel pfiModel = new ValueHolder(
                    ConfigurationEntry.USE_PF_ICON
                        .getValueBoolean(getController()));
                usePowerFolderIconBox = BasicComponentFactory.createCheckBox(
                    new BufferedValueModel(pfiModel, writeTrigger), Translation
                        .getTranslation("preferences.dialog.use_pf_icon"));
            }

        }
    }

    /**
     * Builds general ui panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.nickname")), cc.xy(1, row));
            builder.add(nickField, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.base_dir")), cc.xy(1, row));
            builder.add(locationField, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.auto_cleanup")),
                    cc.xy(1, row));
            builder.add(autoCleanupSlider, cc.xy(3, row));

            row += 2;
            builder.add(useRecycleBinBox, cc.xyw(3, row, 2));

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
            } else {
                builder.appendRow("3dlu");
                builder.appendRow("pref");

                row += 2;
                builder.add(new JLabel(Translation
                    .getTranslation("preferences.dialog.non_windows_info"),
                    SwingConstants.CENTER), cc.xyw(1, row, 4));
            }

            row += 2;
            builder.add(showAdvancedSettingsBox, cc.xyw(3, row, 2));

            panel = builder.getPanel();
        }
        return panel;
    }

    public void undoChanges() {
    }

    /**
     * Called when the location model changes value. Sets the location text
     * field value and enables the location button.
     */
    private void updateLocationComponents() {
        String value = (String) locationModel.getValue();
        locationTF.setText(value);
    }

    /**
     * Creates a pair of location text field and button.
     *
     * @param folderInfo
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("122dlu, 3dlu, pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButtonMini(Icons.DIRECTORY,
                Translation.getTranslation("folder_create.dialog.select_file.text"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    public void save() {
        // Write properties into core
        writeTrigger.triggerCommit();

        // Set folder base
        String folderbase = (String) locationModel.getValue();
        ConfigurationEntry.FOLDER_BASEDIR.setValue(getController(), folderbase);

        // Nickname
        if (!StringUtils.isBlank(nickField.getText())) {
            getController().changeNick(nickField.getText(), false);
        }

        PreferencesEntry.AUTO_CLEANUP_FREQUENCY.setValue(getController(),
                autoCleanupSlider.getValue());

        // setAdvanced
        PreferencesEntry.SHOW_ADVANCED_SETTINGS.setValue(getController(),
            showAdvancedSettingsBox.isSelected());

        // UseRecycleBin
        ConfigurationEntry.USE_RECYCLE_BIN.setValue(getController(), Boolean
            .toString(useRecycleBinBox.isSelected()));

        if (usePowerFolderIconBox != null) {
            // PowerFolder icon
            ConfigurationEntry.USE_PF_ICON.setValue(getController(), Boolean
                .toString(usePowerFolderIconBox.isSelected()));
        }
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) locationModel.getValue();
            String file = DialogFactory.chooseDirectory(getController(),
                initial);
            locationModel.setValue(file);
        }
    }
}
