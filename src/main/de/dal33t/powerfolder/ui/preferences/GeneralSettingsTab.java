package de.dal33t.powerfolder.ui.preferences;

import java.awt.Component;
import java.awt.event.*;
import java.util.Locale;
import java.util.Properties;

import javax.swing.*;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.ComboBoxAdapter;
import com.jgoodies.binding.adapter.PreferencesAdapter;
import com.jgoodies.binding.value.*;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.theme.ThemeSupport;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class GeneralSettingsTab extends PFUIComponent implements PreferenceTab {
    static final String SHOWADVANGEDSETTINGS = "showadvangedsettings";
    private JPanel panel;
    private PreferencesDialog preferencesDialog;
    private JTextField nickField;
    private JCheckBox createDesktopShortcutsBox;

    private JComboBox languageChooser;
    private JComboBox colorThemeChooser;
    private JComboBox xBehaviorChooser;
    private JComponent localBaseSelectField;
    private ValueModel localBaseHolder;

    private JCheckBox showAdvangedSettingsBox;
   // private ValueModel showAdvancedSettingsModel;

    private boolean needsRestart = false;
    // The original theme
    private PlasticTheme oldTheme;
    // The triggers the writing into core
    private Trigger writeTrigger;

    public GeneralSettingsTab(Controller controller,
        PreferencesDialog preferencesDialog)
    {
        super(controller);
        this.preferencesDialog = preferencesDialog;
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

    public void undoChanges() {
        PlasticTheme activeTheme = ThemeSupport.getActivePlasticTheme();
        // Reset to old look and feel
        if (!Util.equals(oldTheme, activeTheme)) {
            ThemeSupport.setPlasticTheme(oldTheme, getUIController()
                .getMainFrame().getUIComponent(), null);
            colorThemeChooser.setSelectedItem(oldTheme);
        }
    }

    /**
     * Initalizes all needed ui components
     */
    private void initComponents() {
        writeTrigger = new Trigger();

        nickField = new JTextField(getController().getMySelf().getNick());

        ValueModel csModel = new PreferencesAdapter(getController()
            .getPreferences(), "createdesktopshortcuts", Boolean.TRUE);
        createDesktopShortcutsBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(csModel, writeTrigger), "");
        // Only available on windows systems
        createDesktopShortcutsBox.setEnabled(Util.isWindowsSystem());

        // Language selector
        languageChooser = createLanguageChooser();

        // Build color theme chooser
        colorThemeChooser = createThemeChooser();
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            colorThemeChooser.setEnabled(true);
            oldTheme = PlasticXPLookAndFeel.getPlasticTheme();
        } else {
            // Only available if PlasicXPLookAndFeel is enabled
            colorThemeChooser.setEnabled(false);
        }

        // Create xBehaviorchooser
        ValueModel xBehaviorModel = new PreferencesAdapter(getController()
            .getPreferences(), "quitonx", Boolean.FALSE);
        // Build behavior chooser
        xBehaviorChooser = createXBehaviorChooser(new BufferedValueModel(
            xBehaviorModel, writeTrigger));
        // Only available on systems with system tray
        xBehaviorChooser.setEnabled(Util.isSystraySupported());
        if (!xBehaviorChooser.isEnabled()) {
            // Display exit on x if not enabled
            xBehaviorModel.setValue(Boolean.TRUE);
        }

        // Local base selection
        localBaseHolder = new ValueHolder(getController().getFolderRepository()
            .getFoldersBasedir());
        localBaseSelectField = ComplexComponentFactory
            .createDirectorySelectionField(Translation
                .getTranslation("preferences.dialog.basedir.title"),
                localBaseHolder, null);

        showAdvangedSettingsBox = SimpleComponentFactory.createCheckBox();        
        showAdvangedSettingsBox.setSelected("true".equals(getController()
            .getConfig().get(SHOWADVANGEDSETTINGS)));
        showAdvangedSettingsBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                preferencesDialog.showAdvangedTab(showAdvangedSettingsBox
                    .isSelected());
            }
        });
    }

    /**
     * Builds general ui panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:100dlu, 7dlu, 30dlu, 3dlu, 15dlu, 10dlu, 30dlu, 30dlu, pref",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, top:pref, 3dlu, top:pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 7dlu");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.createEmptyBorder("3dlu, 0dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.nickname")), cc.xy(1, row));
            builder.add(nickField, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.createdesktopshortcuts")),
                cc.xy(1, row));
            builder.add(createDesktopShortcutsBox, cc.xy(3, row));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.language")), cc.xy(1, row));
            builder.add(languageChooser, cc.xywh(3, row, 7, 1));

            row += 2;
            builder
                .add(new JLabel(Translation
                    .getTranslation("preferences.dialog.xbehavior")), cc.xy(1,
                    row));
            builder.add(xBehaviorChooser, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.colortheme")), cc
                .xy(1, row));
            builder.add(colorThemeChooser, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.basedir")), cc.xy(1, row));
            builder.add(localBaseSelectField, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.showadvanged")), cc.xy(1,
                row));
            builder.add(showAdvangedSettingsBox, cc.xywh(3, row, 7, 1));

            // Add info for non-windows systems
            if (!Util.isWindowsSystem()) {
                builder.appendRow(new RowSpec("pref"));
                builder.appendRow(new RowSpec("7dlu"));
                // Add info for non-windows system
                row += 2;
                builder.add(new JLabel(Translation
                    .getTranslation("preferences.dialog.nonwindowsinfo")), cc
                    .xywh(1, row, 7, 1));
            }
            panel = builder.getPanel();
        }
        return panel;
    }

    public void save() {
        Properties config = getController().getConfig();
        // Write properties into core
        writeTrigger.triggerCommit();
        // Set locale
        if (languageChooser.getSelectedItem() instanceof Locale) {
            Locale locale = (Locale) languageChooser.getSelectedItem();
            // Check if we need to restart
            needsRestart |= !Util.equals(locale.getLanguage(), Translation
                .getActiveLocale().getLanguage());
            // Save settings
            Translation.saveLocalSetting(locale);
        } else {
            // Remove setting
            Translation.saveLocalSetting(null);
        }
        // Set folder base
        config.setProperty("foldersbase", (String) localBaseHolder.getValue());

        // Store ui theme
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            PlasticTheme theme = PlasticXPLookAndFeel.getPlasticTheme();
            config.put("uitheme", theme.getClass().getName());
            if (!Util.equals(theme, oldTheme)) {
                // FIXME: Themechange does not repaint SimpleInternalFrames.
                // thus restart required
                needsRestart = true;
            }
        }
        // Nickname
        if (!StringUtils.isBlank(nickField.getText())) {
            getController().changeNick(nickField.getText(), false);
        }

        // setAdvanged        
        config.setProperty(SHOWADVANGEDSETTINGS, showAdvangedSettingsBox
            .isSelected()
            + "");
    }

    /**
     * Creates a language chooser, which contains the supported locales
     * 
     * @return
     */
    private JComboBox createLanguageChooser() {
        // Create combobox
        JComboBox chooser = new JComboBox();
        Locale[] locales = Translation.getSupportedLocales();
        for (int i = 0; i < locales.length; i++) {
            chooser.addItem(locales[i]);
        }
        // Set current locale as selected
        chooser.setSelectedItem(Translation.getResourceBundle().getLocale());

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
        return chooser;
    }

    /**
     * Build the ui theme chooser combobox
     */
    private JComboBox createThemeChooser() {
        JComboBox chooser = new JComboBox();
        final PlasticTheme[] availableThemes = ThemeSupport
            .getAvailableThemes();
        for (int i = 0; i < availableThemes.length; i++) {
            chooser.addItem(availableThemes[i].getName());
            if (availableThemes[i].getClass().getName().equals(
                getUIController().getUIThemeConfig()))
            {
                chooser.setSelectedIndex(i);
            }
        }
        chooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                PlasticTheme theme = availableThemes[colorThemeChooser
                    .getSelectedIndex()];
                ThemeSupport.setPlasticTheme(theme, getUIController()
                    .getMainFrame().getUIComponent(), null);

            }
        });
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
    private JComboBox createXBehaviorChooser(final ValueModel xBehaviorModel) {
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
                if (((Boolean) value).booleanValue()) {
                    setText(Translation
                        .getTranslation("preferences.dialog.xbehavior.exit"));
                } else {
                    setText(Translation
                        .getTranslation("preferences.dialog.xbehavior.minimize"));
                }
                return this;
            }
        });
        return chooser;
    }
}
