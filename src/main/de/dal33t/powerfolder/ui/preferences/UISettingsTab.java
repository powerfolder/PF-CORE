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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.ui.UIUtil;

public class UISettingsTab extends PFUIComponent implements PreferenceTab {

    private JPanel panel;

    private JComboBox languageChooser;
    private JComboBox xBehaviorChooser;
    private JCheckBox minToSysTrayCB;
    private JCheckBox lockUICB;
    private JCheckBox underlineLinkBox;
    private JCheckBox magneticFrameBox;
    private JComboBox inlineInfoCombo;
    private JCheckBox translucentMainFrameCB;
    private JCheckBox mainAlwaysOnTopCB;
    private JCheckBox autoExpandCB;
    private JLabel transPercLabel;
    private JSlider transPercSlider;
    private int originalInline;

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

        ValueModel minToSysTrayModel = new ValueHolder(
            PreferencesEntry.MIN_TO_SYS_TRAY.getValueBoolean(getController()));
        minToSysTrayCB = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(minToSysTrayModel, writeTrigger),
            Translation.getTranslation("preferences.dialog.min_to_sys_tray"));

        ValueModel lockedModel = new ValueHolder(
            ConfigurationEntry.USER_INTERFACE_LOCKED
                .getValueBoolean(getController()));
        lockUICB = BasicComponentFactory.createCheckBox(new BufferedValueModel(
            lockedModel, writeTrigger), Translation
            .getTranslation("preferences.dialog.ui_locked"));

        ValueModel ulModel = new ValueHolder(PreferencesEntry.UNDERLINE_LINKS
            .getValueBoolean(getController()));
        underlineLinkBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(ulModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.underline_link"));

        ValueModel mfModel = new ValueHolder(
            PreferencesEntry.USE_MAGNETIC_FRAMES
                .getValueBoolean(getController()));
        magneticFrameBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(mfModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.magnetic_frame"));

        DefaultComboBoxModel inlineInfoCBM = new DefaultComboBoxModel();
        inlineInfoCBM.addElement(Translation
            .getTranslation("preferences.dialog.information_panel_separate"));
        inlineInfoCBM.addElement(Translation
            .getTranslation("preferences.dialog.information_panel_left"));
        inlineInfoCBM.addElement(Translation
            .getTranslation("preferences.dialog.information_panel_right"));
        inlineInfoCombo = new JComboBox(inlineInfoCBM);

        originalInline = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController());
        inlineInfoCombo.setSelectedIndex(originalInline);

        ValueModel transModel = new ValueHolder(
            PreferencesEntry.TRANSLUCENT_MAIN_FRAME
                .getValueBoolean(getController()));
        translucentMainFrameCB = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(transModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.translucent_frame"));
        translucentMainFrameCB.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                enableTransPerc();
                doMainFrameTranslucency();
            }
        });

        ValueModel onTopModel = new ValueHolder(
            PreferencesEntry.MAIN_ALWAYS_ON_TOP
                .getValueBoolean(getController()));
        mainAlwaysOnTopCB = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(onTopModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.main_on_top"));
        mainAlwaysOnTopCB.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                doMainOnTop(mainAlwaysOnTopCB.isSelected());
            }
        });

        ValueModel aeModel = new ValueHolder(PreferencesEntry.AUTO_EXPAND
            .getValueBoolean(getController()));
        autoExpandCB = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(aeModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.auto_expand"));

        transPercSlider = new JSlider();
        transPercSlider.setMinimum(10);
        transPercSlider.setMaximum(90);
        transPercSlider.setValue(PreferencesEntry.TRANSLUCENT_PERCENTAGE
            .getValueInt(getController()));
        transPercSlider.setMajorTickSpacing(20);
        transPercSlider.setMinorTickSpacing(5);

        transPercSlider.setPaintTicks(true);
        transPercSlider.setPaintLabels(true);

        Dictionary<Integer, JLabel> dictionary = new Hashtable<Integer, JLabel>();
        for (int i = 10; i <= 90; i += transPercSlider.getMajorTickSpacing()) {
            dictionary.put(i, new JLabel(Integer.toString(i) + '%'));
        }
        transPercSlider.setLabelTable(dictionary);
        transPercSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                doMainFrameTranslucency();
            }
        });

        transPercLabel = new JLabel(Translation
            .getTranslation("preferences.dialog.translucent_text"));

        // Windows only...
        if (OSUtil.isWindowsSystem()) {

            if (WinUtils.getInstance() != null) {
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
            }
        }

        if (getUIController().getSkins().length > 1) {
            skinLabel = new JLabel(Translation
                .getTranslation("preferences.dialog.skin_text"));
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
     * This temporarily sets the main frame translucency to match the controls.
     * If user cancels out, the MainFrame's focus listener will fix things.
     */
    private void doMainFrameTranslucency() {
        if (Constants.OPACITY_SUPPORTED) {
            if (translucentMainFrameCB.isSelected()) {
                // Translucency is 1 - opacity.
                float opacity = 1.0f - transPercSlider.getValue() / 100.0f;
                UIUtil.applyTranslucency(getController().getUIController()
                    .getMainFrame().getUIComponent(), opacity);
            } else {
                UIUtil.applyTranslucency(getController().getUIController()
                    .getMainFrame().getUIComponent(), 1.0f);
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
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.language")), cc.xy(1, row));
            builder.add(languageChooser, cc.xy(3, row));

            row += 2;
            if (skinLabel != null && skinCombo != null) {
                builder.add(skinLabel, cc.xy(1, row));
                builder.add(skinCombo, cc.xy(3, row));
            }

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.exit_behavior")), cc.xy(1,
                row));
            builder.add(xBehaviorChooser, cc.xy(3, row));

            row += 2;
            builder.add(minToSysTrayCB, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.information_panel")), cc
                .xy(1, row));
            builder.add(inlineInfoCombo, cc.xy(3, row));

            row += 2;
            builder.add(magneticFrameBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(lockUICB, cc.xyw(3, row, 2));

            row += 2;
            builder.add(underlineLinkBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(autoExpandCB, cc.xyw(3, row, 2));

            if (getUIController().getMainFrame().getUIComponent()
                .isAlwaysOnTopSupported())
            {
                row += 2;
                builder.add(mainAlwaysOnTopCB, cc.xyw(3, row, 2));
            }

            if (Constants.OPACITY_SUPPORTED) {
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                builder.appendRow("3dlu");
                builder.appendRow("pref");

                row += 2;
                builder.add(translucentMainFrameCB, cc.xyw(3, row, 2));

                row += 2;
                builder.add(transPercLabel, cc.xy(1, row));
                builder.add(getTransSpinnerPanel(), cc.xy(3, row));
            }

            panel = builder.getPanel();

            enableTransPerc();
        }
        return panel;
    }

    private void enableTransPerc() {
        transPercLabel.setEnabled(translucentMainFrameCB.isSelected());
        transPercSlider.setEnabled(translucentMainFrameCB.isSelected());
    }

    private Component getTransSpinnerPanel() {
        FormLayout layout = new FormLayout("pref, pref:grow", "pref");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.add(transPercSlider, cc.xy(1, 1));
        return builder.getPanel();
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

        // Use underlines
        PreferencesEntry.UNDERLINE_LINKS.setValue(getController(),
            underlineLinkBox.isSelected());

        // Use magnetic frames
        PreferencesEntry.USE_MAGNETIC_FRAMES.setValue(getController(),
            magneticFrameBox.isSelected());

        if (originalInline != inlineInfoCombo.getSelectedIndex()) {
            needsRestart = true;
        }

        // Use inline info
        PreferencesEntry.INLINE_INFO_MODE.setValue(getController(),
            inlineInfoCombo.getSelectedIndex());

        PreferencesEntry.TRANSLUCENT_MAIN_FRAME.setValue(getController(),
            translucentMainFrameCB.isSelected());

        PreferencesEntry.MAIN_ALWAYS_ON_TOP.setValue(getController(),
            mainAlwaysOnTopCB.isSelected());

        PreferencesEntry.AUTO_EXPAND.setValue(getController(), autoExpandCB
            .isSelected());

        PreferencesEntry.MIN_TO_SYS_TRAY.setValue(getController(),
            minToSysTrayCB.isSelected());

        ConfigurationEntry.USER_INTERFACE_LOCKED.setValue(getController(),
            String.valueOf(lockUICB.isSelected()));

        PreferencesEntry.TRANSLUCENT_PERCENTAGE.setValue(getController(),
            transPercSlider.getValue());

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