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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
    private JCheckBox verboseCB;
    private boolean originalVerbose;
    private JCheckBox lockUICB;
    private JCheckBox underlineLinkCB;
    private JCheckBox autoExpandCB;
    private JCheckBox showHiddenFilesCB;
    private JLabel skinLabel;
    private JComboBox<String> skinCombo;

    private boolean needsRestart;

    public AdvancedSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("exp.preferences.advanced.title");
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

        useOnlineStorageCB = new JCheckBox(Translation.getTranslation("exp.preferences.advanced.online_storage_text"));
        useOnlineStorageCB.setToolTipText(Translation.getTranslation("exp.preferences.advanced.online_storage_tip"));
        useOnlineStorageCB.setSelected(PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(getController()));

        originalVerbose = ConfigurationEntry.VERBOSE.getValueBoolean(getController());
        verboseCB = SimpleComponentFactory.createCheckBox(Translation.getTranslation("exp.preferences.advanced.verbose"));
        verboseCB.setSelected(ConfigurationEntry.VERBOSE.getValueBoolean(getController()));

        lockUICB = SimpleComponentFactory.createCheckBox(Translation.getTranslation("exp.preferences.advanced.ui_locked"));
        lockUICB.setSelected(ConfigurationEntry.USER_INTERFACE_LOCKED.getValueBoolean(getController()));

        underlineLinkCB = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("exp.preferences.advanced.underline_link"));
        underlineLinkCB.setVisible(false);
        underlineLinkCB.setSelected(PreferencesEntry.UNDERLINE_LINKS.getValueBoolean(getController()));

        autoExpandCB = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("exp.preferences.advanced.auto_expand"));
        autoExpandCB.setVisible(false);
        autoExpandCB.setSelected(PreferencesEntry.AUTO_EXPAND.getValueBoolean(getController()));

        showHiddenFilesCB = SimpleComponentFactory
            .createCheckBox(Translation
                .getTranslation("exp.preferences.warnings_notifications.show_hidden_files"));
        showHiddenFilesCB.setSelected(PreferencesEntry.SHOW_HIDDEN_FILES
            .getValueBoolean(getController()));

        // Windows only...
        if (OSUtil.isWindowsSystem()) {

            if (WinUtils.getInstance() != null) {
                ValueModel startWithWindowsVM = new ValueHolder(WinUtils
                    .getInstance().hasPFStartup(getController()));
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
                Translation.getTranslation("exp.preferences.advanced.skin_text"));
            DefaultComboBoxModel<String> skinComboModel = new DefaultComboBoxModel<>();
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
            skinCombo = new JComboBox<>(skinComboModel);

            if (selected > -1) {
                skinCombo.setSelectedIndex(selected);
            }
            skinLabel.setVisible(getController().getDistribution()
                .allowSkinChange());
            skinCombo.setVisible(getController().getDistribution()
                .allowSkinChange());
        }
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
                Translation.getTranslation("exp.preferences.advanced.server"),
                cc.xy(1, row));
            builder.add(severSelector.getUIComponent(), cc.xy(3, row));

            if (!getController().isBackupOnly()) {
                row += 2;
                builder.add(useOnlineStorageCB, cc.xy(3, row));
            }

            row += 2;
            builder.add(showHiddenFilesCB, cc.xyw(3, row, 2));

            row += 2;
            builder.add(verboseCB, cc.xy(3, row));

            row += 2;
            builder.add(lockUICB, cc.xyw(3, row, 2));

            if (skinLabel != null && skinCombo != null) {
                row += 2;
                builder.add(skinLabel, cc.xy(1, row));
                builder.add(skinCombo, cc.xy(3, row));
            }

            row += 2;
            builder.add(underlineLinkCB, cc.xyw(3, row, 2));

            row += 2;
            builder.add(autoExpandCB, cc.xyw(3, row, 2));

            panel = builder.getPanel();

        }
        return panel;
    }

    public void save() {

        PreferencesEntry.USE_ONLINE_STORAGE.setValue(getController(), useOnlineStorageCB.isSelected());

        // Verbose logging
        if (originalVerbose ^ verboseCB.isSelected()) {
            // Verbose setting changed.
            needsRestart = true;
        }
        ConfigurationEntry.VERBOSE.setValue(getController(), Boolean.toString(verboseCB.isSelected()));

        // Use underlines
        PreferencesEntry.UNDERLINE_LINKS.setValue(getController(),
            underlineLinkCB.isSelected());

        PreferencesEntry.AUTO_EXPAND.setValue(getController(),
            autoExpandCB.isSelected());

        PreferencesEntry.SHOW_HIDDEN_FILES.setValue(getController(),
            showHiddenFilesCB.isSelected());

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

        getController().saveConfig();

    }
}