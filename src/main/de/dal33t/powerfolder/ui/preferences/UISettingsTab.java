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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.ComboBoxAdapter;
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
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;

public class UISettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;

    private JComboBox languageChooser;
    private JComboBox xBehaviorChooser;
    private JCheckBox lockUICB;
    private JCheckBox underlineLinkBox;
    private JCheckBox infoDockedBox;
    private JCheckBox autoExpandCB;
    private boolean wasDocked;
    private JCheckBox updateCheck;
    private JCheckBox usePowerFolderLink;

    private JLabel skinLabel;
    private JComboBox skinCombo;

    private boolean needsRestart;
    // The triggers the writing into core
    private Trigger writeTrigger;

    public UISettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.ui.title");
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
        writeTrigger = new Trigger();

        // Language selector
        languageChooser = createLanguageChooser();

        // Create xBehaviorchooser
        ValueModel xBehaviorModel = PreferencesEntry.QUIT_ON_X
            .getModel(getController());
        // Build behavior chooser
        xBehaviorChooser = createXBehaviorChooser(new BufferedValueModel(
            xBehaviorModel, writeTrigger));
        // Only available on systems with system tray
        xBehaviorChooser.setEnabled(OSUtil.isSystraySupported());
        if (!xBehaviorChooser.isEnabled()) {
            // Display exit on x if not enabled
            xBehaviorModel.setValue(Boolean.TRUE);
        }

        boolean checkForUpdate = PreferencesEntry.CHECK_UPDATE
            .getValueBoolean(getController());
        updateCheck = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.check_for_program_updates"),
            checkForUpdate);

        ValueModel lockedModel = new ValueHolder(
            ConfigurationEntry.USER_INTERFACE_LOCKED
                .getValueBoolean(getController()));
        lockUICB = BasicComponentFactory.createCheckBox(new BufferedValueModel(
            lockedModel, writeTrigger), Translation
            .getTranslation("preferences.dialog.ui_locked"));

        ValueModel ulModel = new ValueHolder(
            PreferencesEntry.UNDERLINE_LINKS.getValueBoolean(getController()));
        underlineLinkBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(ulModel, writeTrigger),
            Translation.getTranslation("preferences.dialog.underline_link"));
        underlineLinkBox.setVisible(false);

        infoDockedBox = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.information_panel_docked"));

        wasDocked = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController()) > 0;
        infoDockedBox.setSelected(wasDocked);

        ValueModel aeModel = new ValueHolder(
            PreferencesEntry.AUTO_EXPAND.getValueBoolean(getController()));
        autoExpandCB = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(aeModel, writeTrigger),
            Translation.getTranslation("preferences.dialog.auto_expand"));
        autoExpandCB.setVisible(false);

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

            ValueModel pflModel = new ValueHolder(
                ConfigurationEntry.USE_PF_LINK.getValueBoolean(getController())
                    || WinUtils.isPFLinks(getController()));
            usePowerFolderLink = BasicComponentFactory.createCheckBox(
                new BufferedValueModel(pflModel, writeTrigger),
                Translation.getTranslation("preferences.dialog.show_pf_link"));

        }

        if (OSUtil.isMacOS()) {
            // Places
            ValueModel pflModel = new ValueHolder(
                ConfigurationEntry.USE_PF_LINK.getValueBoolean(getController()));
            usePowerFolderLink = BasicComponentFactory.createCheckBox(
                new BufferedValueModel(pflModel, writeTrigger),
                Translation.getTranslation("preferences.dialog.show_pf_link"));
        }

        if (getUIController().getSkins().length > 1) {
            skinLabel = new JLabel(
                Translation.getTranslation("preferences.dialog.skin_text"));
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
    }

    private void doMainOnTop(Boolean onTop) {
        getUIController().getMainFrame().getUIComponent().setAlwaysOnTop(onTop);
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
                    .getTranslation("preferences.dialog.language")), cc.xy(1,
                    row));
            builder.add(languageChooser, cc.xy(3, row));

            row += 2;
            if (skinLabel != null && skinCombo != null) {
                builder.add(skinLabel, cc.xy(1, row));
                builder.add(skinCombo, cc.xy(3, row));
            }

            row += 2;
            builder.add(
                new JLabel(Translation
                    .getTranslation("preferences.dialog.exit_behavior")), cc
                    .xy(1, row));
            builder.add(xBehaviorChooser, cc.xy(3, row));

            // Links only available in Vista
            if (usePowerFolderLink != null) {
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                row += 2;
                builder.add(usePowerFolderLink, cc.xyw(3, row, 2));
            }

            row += 2;
            builder.add(infoDockedBox, cc.xy(3, row));

            row += 2;
            builder.add(lockUICB, cc.xyw(3, row, 2));

            row += 2;
            builder.add(updateCheck, cc.xyw(3, row, 2));

            row += 2;
            builder.add(underlineLinkBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(autoExpandCB, cc.xyw(3, row, 2));

            panel = builder.getPanel();

        }
        return panel;
    }

    public void save() {
        // Write properties into core
        writeTrigger.triggerCommit();
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

        boolean checkForUpdate = updateCheck.isSelected();
        PreferencesEntry.CHECK_UPDATE.setValue(getController(), checkForUpdate);

        // Use underlines
        PreferencesEntry.UNDERLINE_LINKS.setValue(getController(),
            underlineLinkBox.isSelected());

        if (wasDocked != infoDockedBox.isSelected()) {
            needsRestart = true;
        }

        if (usePowerFolderLink != null) {
            boolean newValue = usePowerFolderLink.isSelected();
            configureLinksPlances(newValue);
            // PowerFolder favorite
            // ConfigurationEntry.USE_PF_LINK.setValue(getController(),
            // Boolean.toString(usePowerFolderLink.isSelected()));
        }

        // Use inline info
        PreferencesEntry.INLINE_INFO_MODE.setValue(getController(),
            infoDockedBox.isSelected() ? 2 : 0);

        PreferencesEntry.AUTO_EXPAND.setValue(getController(),
            autoExpandCB.isSelected());

        ConfigurationEntry.USER_INTERFACE_LOCKED.setValue(getController(),
            String.valueOf(lockUICB.isSelected()));

        getController().saveConfig();

        if (skinCombo != null) {
            String skinName = PreferencesEntry.SKIN_NAME
                .getValueString(getController());
            if (!skinCombo.getSelectedItem().equals(skinName)) {
                PreferencesEntry.SKIN_NAME.setValue(getController(),
                    (String) skinCombo.getSelectedItem());
                needsRestart = true;
            }
        }
    }

    private void configureLinksPlances(boolean newValue) {
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

    /**
     * Creates a language chooser, which contains the supported locales
     * 
     * @return a language chooser, which contains the supported locales
     */
    @SuppressWarnings("serial")
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

    /**
     * Creates a X behavior chooser, writes settings into model
     * 
     * @param xBehaviorModel
     *            the behavior model, writes true if should exit program, false
     *            if minimize to system is choosen
     * @return the combobox
     */
    @SuppressWarnings("serial")
    private JComboBox createXBehaviorChooser(ValueModel xBehaviorModel) {
        // Build combobox model
        ComboBoxAdapter<Boolean> model = new ComboBoxAdapter<Boolean>(
            new Boolean[]{Boolean.FALSE, Boolean.TRUE}, xBehaviorModel);

        // Create combobox
        JComboBox chooser = new JComboBox(model);

        // Add renderer
        chooser.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
                if ((Boolean) value) {
                    setText(Translation
                        .getTranslation("preferences.dialog.exit_behavior.exit"));
                } else {
                    setText(Translation
                        .getTranslation("preferences.dialog.exit_behavior.minimize"));
                }
                return this;
            }
        });
        return chooser;
    }
}