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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Locale;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.ComboBoxAdapter;
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
import de.dal33t.powerfolder.ui.LookAndFeelSupport;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.ui.DialogFactory;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {


    private JPanel panel;
    private JTextField nickField;
    private JCheckBox createDesktopShortcutsBox;

    private JCheckBox startWithWindowsBox;

    private JCheckBox updateCheck;

    private JComboBox languageChooser;
    private JComboBox lookAndFeelChooser;
    private JComboBox xBehaviorChooser;
    private JTextField locationTF;
    private ValueModel locationModel;
    private JComponent locationField;
    private JCheckBox underlineLinkBox;
    private JCheckBox magneticFrameBox;
    private JCheckBox translucentMainFrameCB;
    private JLabel transPercLabel1;
    private JSlider transPercSlider;

    private JCheckBox showAdvancedSettingsBox;
    private ValueModel showAdvancedSettingsModel;

    private JCheckBox useRecycleBinBox;

    private JCheckBox usePowerFolderIconBox;

    private boolean needsRestart;
    // The original look and feel
    private LookAndFeel oldLaf;
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

    public void undoChanges() {
        LookAndFeel activeLaf = UIManager.getLookAndFeel();
        // Reset to old look and feel
        if (!Util.equals(oldLaf, activeLaf)) {
            try {
                LookAndFeelSupport.setLookAndFeel(oldLaf);
            } catch (UnsupportedLookAndFeelException e) {
                logSevere(e);
            }
            lookAndFeelChooser.setSelectedItem(oldLaf);
        }
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

        // Language selector
        languageChooser = createLanguageChooser();

        boolean checkForUpdate = PreferencesEntry.CHECK_UPDATE
            .getValueBoolean(getController());
        updateCheck = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.check_for_program_updates"),
            checkForUpdate);

        // Build color theme chooser
        oldLaf = UIManager.getLookAndFeel();
        lookAndFeelChooser = createLookAndFeelChooser();

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

        showAdvancedSettingsBox = BasicComponentFactory.createCheckBox(
            showAdvancedSettingsModel, Translation
                .getTranslation("preferences.dialog.show_advanced"));

        ValueModel ulModel = new ValueHolder(
            PreferencesEntry.UNDERLINE_LINKS.getValueBoolean(getController()));
        underlineLinkBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(ulModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.underline_link"));

        ValueModel mfModel = new ValueHolder(
            PreferencesEntry.USE_MAGNETIC_FRAMES.getValueBoolean(getController()));
        magneticFrameBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(mfModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.magnetic_frame"));

        ValueModel transModel = new ValueHolder(
            PreferencesEntry.TRANSLUCENT_MAIN_FRAME.getValueBoolean(getController()));
        translucentMainFrameCB = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(transModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.translucent_frame"));
        translucentMainFrameCB.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                enableTransPerc();
            }
        });

        transPercSlider = new JSlider();
        transPercSlider.setMinimum(10);
        transPercSlider.setMaximum(90);
        transPercSlider.setValue(PreferencesEntry.TRANSLUCENT_PERCENTAGE
                .getValueInt(getController()).intValue());
        transPercSlider.setMajorTickSpacing(20);
        transPercSlider.setMinorTickSpacing(5);

        transPercSlider.setPaintTicks(true);
        transPercSlider.setPaintLabels(true);

        Dictionary<Integer, JLabel> dictionary = new Hashtable<Integer, JLabel>();
        for (int i = 10; i <= 90; i += transPercSlider.getMajorTickSpacing())
        {
            dictionary.put(i, new JLabel(Integer.toString(i) + '%'));
        }
        transPercSlider.setLabelTable(dictionary);

        transPercLabel1 = new JLabel(Translation
                .getTranslation("preferences.dialog.translucent_text"));
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
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

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


            //////////////////////////////////////////
            // User Interface stuff only below here //
            //////////////////////////////////////////

            row += 2;
            builder.addSeparator(Translation.getTranslation(
                    "preferences.dialog.user_interface"),
                    cc.xyw(1, row, 4));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.language")), cc.xy(1, row));
            builder.add(languageChooser, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation
                    .getTranslation("preferences.dialog.exit_behavior")),
                    cc.xy(1, row));
            builder.add(xBehaviorChooser, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.color_theme")), cc
                .xy(1, row));
            builder.add(lookAndFeelChooser, cc.xy(3, row));

            row += 2;
            builder.add(updateCheck, cc.xyw(3, row, 2));

            row += 2;
            builder.add(underlineLinkBox, cc.xyw(3, row, 2));

            row += 2;
            builder.add(magneticFrameBox, cc.xyw(3, row, 2));

            if (Constants.OPACITY_SUPPORTED) {
                builder.appendRow("3dlu");
                builder.appendRow("pref");
                builder.appendRow("3dlu");
                builder.appendRow("pref");

                row += 2;
                builder.add(translucentMainFrameCB, cc.xyw(3, row, 2));

                row += 2;
                builder.add(transPercLabel1, cc.xy(1, row));
                builder.add(getSpinnerPanel(), cc.xy(3, row));
            }
            panel = builder.getPanel();

            enableTransPerc();
        }
        return panel;
    }

    private void enableTransPerc() {
        transPercLabel1.setEnabled(translucentMainFrameCB.isSelected());
        transPercSlider.setEnabled(translucentMainFrameCB.isSelected());
    }

    private Component getSpinnerPanel() {
        FormLayout layout = new FormLayout(
            "pref, pref:grow", "pref");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.add(transPercSlider, cc.xy(1, 1));
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

        // Set folder base
        String folderbase = (String) locationModel.getValue();
        ConfigurationEntry.FOLDER_BASEDIR.setValue(getController(), folderbase);

        // Store ui laf
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (!Util.equals(laf, oldLaf)) {
            PreferencesEntry.UI_LOOK_AND_FEEL.setValue(getController(),
                    laf.getClass().getName());
            needsRestart = true;
        }
        // Nickname
        if (!StringUtils.isBlank(nickField.getText())) {
            getController().changeNick(nickField.getText(), false);
        }

        // setAdvanced
        PreferencesEntry.SHOW_ADVANCED_SETTINGS.setValue(getController(),
            showAdvancedSettingsBox.isSelected());

        // Use underlines
        PreferencesEntry.UNDERLINE_LINKS.setValue(getController(),
            underlineLinkBox.isSelected());

        // Use magnetic frames
        PreferencesEntry.USE_MAGNETIC_FRAMES.setValue(getController(),
           magneticFrameBox.isSelected());

        PreferencesEntry.TRANSLUCENT_MAIN_FRAME.setValue(getController(),
           translucentMainFrameCB.isSelected());

        PreferencesEntry.TRANSLUCENT_PERCENTAGE.setValue(getController(),
                transPercSlider.getValue());

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
     * Creates a language chooser, which contains the supported locales
     * 
     * @return a language chooser, which contains the supported locales
     */
    private JComboBox createLanguageChooser() {
        // Create combobox
        JComboBox chooser = new JComboBox();
        Locale[] locales = Translation.getSupportedLocales();
        for (Locale locale1 : locales) {
            chooser.addItem(locale1);
        }
        if (Translation.isCustomLocale()) {
            chooser.addItem(Translation.getActiveLocale());
            chooser.setEnabled(false);
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
     * Build the ui look and feel chooser combobox
     */
    private JComboBox createLookAndFeelChooser() {
        JComboBox chooser = new JComboBox();
        final LookAndFeel[] availableLafs = LookAndFeelSupport
            .getAvailableLookAndFeels();
        String[] availableLafNames = LookAndFeelSupport
                .getAvailableLookAndFeelNames();
        for (int i = 0; i < availableLafs.length; i++) {
            chooser.addItem(availableLafNames[i]);
            if (availableLafs[i].getClass().getName().equals(
                getUIController().getUILookAndFeelConfig()))
            {
                chooser.setSelectedIndex(i);
            }
        }
        chooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                LookAndFeel laf = availableLafs[lookAndFeelChooser
                    .getSelectedIndex()];
                try {
                    LookAndFeelSupport.setLookAndFeel(laf);
                } catch (UnsupportedLookAndFeelException e1) {
                    logSevere(e1);
                }

            }
        });
        return chooser;
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

    /**
     * Creates a X behavior chooser, writes settings into model
     * 
     * @param xBehaviorModel
     *            the behavior model, writes true if should exit program, false
     *            if minimize to system is choosen
     * @return the combobox
     */
    private JComboBox createXBehaviorChooser(ValueModel xBehaviorModel) {
        // Build combobox model
        ComboBoxAdapter model = new ComboBoxAdapter(new Object[]{Boolean.FALSE,
            Boolean.TRUE}, xBehaviorModel);

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
