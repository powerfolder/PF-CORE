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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.*;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

public class AdvancedSettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;

    private ServerSelectorPanel severSelector;
    private JCheckBox useOnlineStorageCB;
    private JCheckBox verboseBox;
    private boolean originalVerbose;
    private JCheckBox expertModeBox;
    private JCheckBox lockUICB;
    private JCheckBox underlineLinkBox;
    private JCheckBox autoExpandCB;
    private JLabel skinLabel;
    private JComboBox skinCombo;
    private JCheckBox autoDetectFoldersCB;

    private boolean needsRestart;

    public AdvancedSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.advanced.title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public boolean validate() {
        return true;
    }

    public void undoChanges() {
    }

    /**
     * Initalizes all needed ui components
     */
    private void initComponents() {

        severSelector = new ServerSelectorPanel(getController());

        useOnlineStorageCB = new JCheckBox(Translation.getTranslation("preferences.advanced.online_storage_text"));
        useOnlineStorageCB.setToolTipText(Translation.getTranslation("preferences.advanced.online_storage_tip"));
        useOnlineStorageCB.setSelected(PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(getController()));

        originalVerbose = ConfigurationEntry.VERBOSE.getValueBoolean(getController());
        verboseBox = SimpleComponentFactory.createCheckBox(Translation.getTranslation("preferences.advanced.verbose"));
        verboseBox.setSelected(ConfigurationEntry.VERBOSE.getValueBoolean(getController()));

        expertModeBox = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("preferences.advanced.expert_mode"));
        expertModeBox.setSelected(PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()));

        lockUICB = SimpleComponentFactory.createCheckBox(Translation.getTranslation("preferences.advanced.ui_locked"));
        lockUICB.setSelected(ConfigurationEntry.USER_INTERFACE_LOCKED.getValueBoolean(getController()));

        underlineLinkBox = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("preferences.advanced.underline_link"));
        underlineLinkBox.setVisible(false);
        underlineLinkBox.setSelected(PreferencesEntry.UNDERLINE_LINKS.getValueBoolean(getController()));

        autoExpandCB = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("preferences.advanced.auto_expand"));
        autoExpandCB.setVisible(false);
        autoExpandCB.setSelected(PreferencesEntry.AUTO_EXPAND.getValueBoolean(getController()));

        // Windows only...
        if (OSUtil.isWindowsSystem()) {

            if (WinUtils.getInstance() != null) {
                ValueModel startWithWindowsVM = new ValueHolder(WinUtils
                    .getInstance().isPFStartup(getController()));
                startWithWindowsVM
                    .addValueChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            try {
                                if (WinUtils.getInstance() != null) {
                                    WinUtils.getInstance().setPFStartup(
                                        evt.getNewValue().equals(true),
                                        getController());
                                }
                            } catch (IOException e) {
                                logSevere("IOException", e);
                            }
                        }
                    });
            }
        }

        if (getUIController().getSkins().length > 1) {
            skinLabel = new JLabel(
                Translation.getTranslation("preferences.advanced.skin_text"));
            DefaultComboBoxModel skinComboModel = new DefaultComboBoxModel();
            String skinName = PreferencesEntry.SKIN_NAME
                .getValueString(getController());
            int selected = -1;
            int i = 0;
            for (Skin skin : getUIController().getSkins()) {
                skinComboModel.addElement(skin.getName());
                if (skin.getName().equals(skinName)) {
                    selected = i;
                }
                i++;
            }
            skinCombo = new JComboBox(skinComboModel);

            if (selected > -1) {
                skinCombo.setSelectedIndex(selected);
            }
            skinLabel.setVisible(getController().getDistribution()
                .allowSkinChange());
            skinCombo.setVisible(getController().getDistribution()
                .allowSkinChange());
        }

        autoDetectFoldersCB = new JCheckBox(
                Translation.getTranslation("preferences.advanced.auto_detect_folders"));
        autoDetectFoldersCB.setSelected(ConfigurationEntry.LOOK_FOR_FOLDER_CANDIDATES.getValueBoolean(getController()));
    }

    /**
     * Builds general ui panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 140dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 1;
            builder.addLabel(
                Translation.getTranslation("preferences.advanced.server"),
                cc.xy(1, row));
            builder.add(severSelector.getUIComponent(), cc.xy(3, row));

            if (!getController().isBackupOnly()) {
                row += 2;
                builder.add(useOnlineStorageCB, cc.xy(3, row));
            }

            row += 2;
            builder.add(verboseBox, cc.xy(3, row));

            row += 2;
            builder.add(expertModeBox, cc.xy(3, row));

            row += 2;
            builder.add(lockUICB, cc.xyw(3, row, 2));

            if (skinLabel != null && skinCombo != null) {
                row += 2;
                builder.add(skinLabel, cc.xy(1, row));
                builder.add(skinCombo, cc.xy(3, row));
            }

            row += 2;
            builder.add(autoDetectFoldersCB, cc.xyw(3, row, 2));

            row += 2;
            builder.add(underlineLinkBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(autoExpandCB, cc.xyw(3, row, 2));

            panel = builder.getPanel();

        }
        return panel;
    }

    public void save() {

        PreferencesEntry.USE_ONLINE_STORAGE.setValue(getController(), useOnlineStorageCB.isSelected());

        // Verbose logging
        if (originalVerbose ^ verboseBox.isSelected()) {
            // Verbose setting changed.
            needsRestart = true;
        }
        ConfigurationEntry.VERBOSE.setValue(getController(), Boolean.toString(verboseBox.isSelected()));

        // Advanced
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()) ^ expertModeBox.isSelected()) {
            needsRestart = true;
        }
        PreferencesEntry.EXPERT_MODE.setValue(getController(), expertModeBox.isSelected());

        // Use underlines
        PreferencesEntry.UNDERLINE_LINKS.setValue(getController(),
            underlineLinkBox.isSelected());

        PreferencesEntry.AUTO_EXPAND.setValue(getController(),
            autoExpandCB.isSelected());

        ConfigurationEntry.USER_INTERFACE_LOCKED.setValue(getController(),
            String.valueOf(lockUICB.isSelected()));

        if (skinCombo != null) {
            String skinName = PreferencesEntry.SKIN_NAME
                .getValueString(getController());
            if (!skinCombo.getSelectedItem().equals(skinName)) {
                PreferencesEntry.SKIN_NAME.setValue(getController(),
                    (String) skinCombo.getSelectedItem());
                needsRestart = true;
            }
        }

        boolean originalLookForFolders = ConfigurationEntry.LOOK_FOR_FOLDER_CANDIDATES.getValueBoolean(getController());
        ConfigurationEntry.LOOK_FOR_FOLDER_CANDIDATES.setValue(getController(), autoDetectFoldersCB.isSelected());
        if (originalLookForFolders ^ autoDetectFoldersCB.isSelected()) {
            needsRestart = true;
        }

        getController().saveConfig();

    }
}