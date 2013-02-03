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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.FirewallUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;

public class ExpertSettingsTab extends PFComponent implements PreferenceTab {

    private JPanel panel;
    private JCheckBox useZipOnInternetCheckBox;
    private JCheckBox useZipOnLanCheckBox;
    private JCheckBox useDeltaSyncOnLanCheckBox;
    private JCheckBox useDeltaSyncOnInternetCheckBox;
    private JCheckBox useSwarmingOnLanCheckBox;
    private JCheckBox useSwarmingOnInternetCheckBox;

    private JTextField locationTF;
    private ValueModel locationModel;
    private JComponent locationField;

    private JCheckBox conflictDetectionBox;

    private JCheckBox massDeleteBox;
    private JSlider massDeleteSlider;

    private boolean needsRestart;

    public ExpertSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.expert_title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        return true;
    }

    private void initComponents() {

        massDeleteBox = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("preferences.expert.use_mass_delete"));
        massDeleteBox.setSelected(ConfigurationEntry.MASS_DELETE_PROTECTION.getValueBoolean(getController()));
        massDeleteBox.addItemListener(new MassDeleteItemListener());
        massDeleteSlider = new JSlider(20, 100, ConfigurationEntry.MASS_DELETE_THRESHOLD
                .getValueInt(getController()));
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

        conflictDetectionBox = new JCheckBox(Translation.getTranslation("preferences.expert.use_conflict_handling"));
        conflictDetectionBox.setSelected(ConfigurationEntry.CONFLICT_DETECTION.getValueBoolean(getController()));

        // Local base selection
        locationModel = new ValueHolder(getController().getFolderRepository().getFoldersBasedirString());

        // Behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocationComponents();
            }
        });

        locationField = createLocationField();

        useZipOnLanCheckBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("preferences.expert.use_zip_on_lan"));
        useZipOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.expert.use_zip_on_lan_tooltip"));
        useZipOnLanCheckBox.setSelected(ConfigurationEntry.USE_ZIP_ON_LAN
            .getValueBoolean(getController()));

        // Always uses compression on internet
        useZipOnInternetCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.expert.use_zip_on_internet"));
        useZipOnInternetCheckBox.setSelected(true);
        useZipOnInternetCheckBox.setEnabled(false);

        useDeltaSyncOnLanCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.expert.use_delta_on_lan"));
        useDeltaSyncOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.expert.use_delta_on_lan_tooltip"));
        useDeltaSyncOnLanCheckBox
            .setSelected(ConfigurationEntry.USE_DELTA_ON_LAN
                .getValueBoolean(getController()));

        useDeltaSyncOnInternetCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.expert.use_delta_on_internet"));
        useDeltaSyncOnInternetCheckBox
            .setToolTipText(Translation
                .getTranslation("preferences.expert.use_delta_on_internet_tooltip"));
        useDeltaSyncOnInternetCheckBox
            .setSelected(ConfigurationEntry.USE_DELTA_ON_INTERNET
                .getValueBoolean(getController()));

        useSwarmingOnLanCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.expert.swarming_lan"));
        useSwarmingOnLanCheckBox.setToolTipText(Translation
            .getTranslation("preferences.expert.swarming_lan_tooltip"));
        useSwarmingOnLanCheckBox
            .setSelected(ConfigurationEntry.USE_SWARMING_ON_LAN
                .getValueBoolean(getController()));

        useSwarmingOnInternetCheckBox = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("preferences.expert.swarming_internet"));
        useSwarmingOnInternetCheckBox.setToolTipText(Translation
            .getTranslation("preferences.expert.swarming_internet_tooltip"));
        useSwarmingOnInternetCheckBox
            .setSelected(ConfigurationEntry.USE_SWARMING_ON_INTERNET
                    .getValueBoolean(getController()));
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
     * Enable the mass delete slider if the box is selected.
     */
    private void enableMassDeleteSlider() {
        massDeleteSlider.setEnabled(massDeleteBox.isSelected());
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

        JButton locationButton = new JButtonMini(Icons
            .getIconById(Icons.DIRECTORY), Translation
            .getTranslation("preferences.expert.select_directory_text"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            String rows = "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref,  3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref";
            if (FirewallUtil.isFirewallAccessible()) {
                rows = "pref, 3dlu, " + rows;
            }

            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow", rows);
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.expert.base_dir")), cc.xy(1, row));
            builder.add(locationField, cc.xyw(3, row, 2));

            row += 2;
            builder.add(conflictDetectionBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(massDeleteBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(new JLabel(Translation.getTranslation(
                    "preferences.expert.mass_delete_threshold")),
                cc.xy(1, row));
            builder.add(massDeleteSlider, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation.getTranslation("preferences.expert.zip_compression"), cc.xy(1, row));
            ButtonBarBuilder zipBar = ButtonBarBuilder
                .createLeftToRightBuilder();
            zipBar.addGridded(useZipOnInternetCheckBox);
            zipBar.addRelatedGap();
            zipBar.addGridded(useZipOnLanCheckBox);
            builder.add(zipBar.getPanel(), cc.xyw(3, row, 2));

            row += 2;
            builder.addLabel(Translation.getTranslation("preferences.expert.delta_sync"), cc.xy(1, row));
            ButtonBarBuilder deltaBar = ButtonBarBuilder.createLeftToRightBuilder();
            deltaBar.addGridded(useDeltaSyncOnInternetCheckBox);
            deltaBar.addRelatedGap();
            deltaBar.addGridded(useDeltaSyncOnLanCheckBox);
            builder.add(deltaBar.getPanel(), cc.xyw(3, row, 2));

            row += 2;
            builder.addLabel(Translation.getTranslation("preferences.expert.swarming"), cc.xy(1, row));
            ButtonBarBuilder swarmingBar = ButtonBarBuilder.createLeftToRightBuilder();
            swarmingBar.addGridded(useSwarmingOnInternetCheckBox);
            swarmingBar.addRelatedGap();
            swarmingBar.addGridded(useSwarmingOnLanCheckBox);
            builder.add(swarmingBar.getPanel(), cc.xyw(3, row, 2));

            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the advanced settings.
     */
    public void save() {

        ConfigurationEntry.CONFLICT_DETECTION.setValue(getController(), conflictDetectionBox.isSelected());

        ConfigurationEntry.MASS_DELETE_PROTECTION.setValue(getController(), massDeleteBox.isSelected());
        ConfigurationEntry.MASS_DELETE_THRESHOLD.setValue(getController(), massDeleteSlider.getValue());

        // Set folder base
        String oldFolderBaseString = getController().getFolderRepository()
            .getFoldersBasedirString();
        String newFolderBaseString = (String) locationModel.getValue();
        getController().getFolderRepository().setFoldersBasedir(newFolderBaseString);
        if (!StringUtils.isEqual(oldFolderBaseString, newFolderBaseString)) {
            getController().getUIController().configureDesktopShortcut(true);
        }

        // zip on lan?
        boolean current = ConfigurationEntry.USE_ZIP_ON_LAN
            .getValueBoolean(getController());
        if (current != useZipOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_ZIP_ON_LAN.setValue(getController(), String
                .valueOf(useZipOnLanCheckBox.isSelected()));
        }

        // delta on lan?
        current = ConfigurationEntry.USE_DELTA_ON_LAN
            .getValueBoolean(getController());
        if (current != useDeltaSyncOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_DELTA_ON_LAN.setValue(getController(),
                String.valueOf(useDeltaSyncOnLanCheckBox.isSelected()));
            needsRestart = true;
        }

        current = ConfigurationEntry.USE_DELTA_ON_INTERNET
            .getValueBoolean(getController());
        if (current != useDeltaSyncOnInternetCheckBox.isSelected()) {
            ConfigurationEntry.USE_DELTA_ON_INTERNET.setValue(getController(),
                Boolean.toString(useDeltaSyncOnInternetCheckBox.isSelected()));
            needsRestart = true;
        }

        // Swarming
        current = ConfigurationEntry.USE_SWARMING_ON_LAN
            .getValueBoolean(getController());
        if (current != useSwarmingOnLanCheckBox.isSelected()) {
            ConfigurationEntry.USE_SWARMING_ON_LAN.setValue(getController(),
                String.valueOf(useSwarmingOnLanCheckBox.isSelected()));
            needsRestart = true;
        }

        current = ConfigurationEntry.USE_SWARMING_ON_INTERNET
            .getValueBoolean(getController());
        if (current != useSwarmingOnInternetCheckBox.isSelected()) {
            ConfigurationEntry.USE_SWARMING_ON_INTERNET.setValue(
                getController(), Boolean.toString(useSwarmingOnInternetCheckBox
                    .isSelected()));
            needsRestart = true;
        }
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) locationModel.getValue();
            List<File> files = DialogFactory.chooseDirectory(getController()
                .getUIController(), initial, false);
            if (!files.isEmpty()) {
                File newLocation = files.get(0);
                // Make sure that the user is not setting this to the base dir
                // of an existing folder.
                for (Folder folder : getController().getFolderRepository()
                    .getFolders(true))
                {
                    if (folder.getLocalBase().equals(newLocation)) {
                        DialogFactory.genericDialog(getController(),
                                Translation.getTranslation("preferences.expert.duplicate_local_base_title"),
                                Translation.getTranslation("preferences.expert.duplicate_local_base_message",
                                        folder.getName()),
                                GenericDialogType.ERROR);
                        return;
                    }
                }
                locationModel.setValue(newLocation.getAbsolutePath());
            }
        }
    }

    private class MassDeleteItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            enableMassDeleteSlider();
        }
    }

}
