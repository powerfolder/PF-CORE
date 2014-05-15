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
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.panel.LineSpeedSelectionPanel;

/**
 * Panel for basic setup like nick, networking mode, etc.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.8 $
 */
public class BasicSetupPanel extends PFWizardPanel {

    private ValueModel nameModel;
    private ValueModel networkingModeModel;
    private LineSpeedSelectionPanel wanLineSpeed;
    private JTextField nameField;
    private JComboBox languageChooser;
    private DefaultFolderWizardHelper defaultFolderHelper;
    private WizardPanel nextPanel;

    public BasicSetupPanel(Controller controller, WizardPanel nextPanel) {
        super(controller);
        Reject.ifNull(nextPanel, "Nextpanel is null");
        this.nextPanel = nextPanel;
    }

    public boolean hasNext() {
        return !StringUtils.isBlank((String) nameModel.getValue());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, 100dlu, pref:grow",
            "pref, 6dlu, pref, 6dlu, pref, 6dlu, top:pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        builder.addLabel(
            Translation.getTranslation("wizard.basic_setup.computer_name"),
            cc.xy(1, 1));
        builder.add(nameField, cc.xy(3, 1));
        builder.addLabel(
            Translation.getTranslation("preferences.network.line_settings"),
            cc.xywh(1, 3, 1, 1, "default, top"));
        builder.add(wanLineSpeed.getUiComponent(), cc.xy(3, 3));
        builder.addLabel(
            Translation.getTranslation("wizard.basic_setup.language_restart"),
            cc.xy(1, 5));
        builder.add(languageChooser, cc.xy(3, 5));
        builder.add(defaultFolderHelper.getUIComponent(), cc.xyw(3, 7, 2));

        return builder.getPanel();
    }

    public WizardPanel next() {

        // Set nick
        String nick = (String) nameModel.getValue();
        if (!StringUtils.isBlank(nick)) {
            getController().changeNick(nick, true);
        }
        // Set networking mode
        boolean privateNetworking = networkingModeModel.getValue() instanceof PrivateNetworking;
        boolean lanOnlyNetworking = networkingModeModel.getValue() instanceof LanOnlyNetworking;

        if (privateNetworking) {
            getController().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        } else if (lanOnlyNetworking) {
            getController().setNetworkingMode(NetworkingMode.LANONLYMODE);
        } else {
            throw new IllegalStateException("invalid net working mode");
        }

        TransferManager tm = getController().getTransferManager();
        tm.setUploadCPSForWAN(wanLineSpeed.getUploadSpeedKBPS());
        tm.setDownloadCPSForWAN(wanLineSpeed.getDownloadSpeedKBPS());

        // Set locale
        if (languageChooser.getSelectedItem() instanceof Locale) {
            Locale locale = (Locale) languageChooser.getSelectedItem();
            // Save settings
            Translation.saveLocalSetting(locale);
        } else {
            // Remove setting
            Translation.saveLocalSetting(null);
        }

        if (getController().getOSClient().isLoggedIn()
            || getController().isLanOnly())
        {
            // Setup default folder and go to what to do panel
            return defaultFolderHelper.next(nextPanel, getWizardContext());
        }

        return nextPanel;
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        nameModel = new ValueHolder(getController().getMySelf().getNick());

        nameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        // Language selector
        languageChooser = createLanguageChooser();

        nameField = BasicComponentFactory.createTextField(nameModel, false);
        // Ensure minimum dimension
        UIUtil.ensureMinimumWidth(107, nameField);

        wanLineSpeed = new LineSpeedSelectionPanel(getController(), true, false);
        TransferManager tm = getController().getTransferManager();
        wanLineSpeed.setSpeedKBPS(
                ConfigurationEntry.TRANSFER_LIMIT_AUTODETECT.getValueBoolean(
                        getController()), tm.getUploadCPSForWAN() / 1024,
            tm.getDownloadCPSForWAN() / 1024);

        networkingModeModel = new ValueHolder();
        // Network mode chooser
        JComboBox networkingModeChooser = SimpleComponentFactory
                .createComboBox(networkingModeModel);
        networkingModeChooser.addItem(new PrivateNetworking());
        networkingModeChooser.addItem(new LanOnlyNetworking());
        NetworkingMode mode = getController().getNetworkingMode();
        switch (mode) {
            case PRIVATEMODE:
                networkingModeChooser.setSelectedIndex(0);
                break;
            case LANONLYMODE:
                networkingModeChooser.setSelectedIndex(1);
                break;
        }
        wanLineSpeed.setEnabled(networkingModeChooser.getSelectedItem()
                instanceof PrivateNetworking);
        networkingModeChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                wanLineSpeed.setEnabled(e.getItem() instanceof PrivateNetworking);
            }
        });

        defaultFolderHelper = new DefaultFolderWizardHelper(getController(),
            getController().getOSClient());
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.basic_setup.title");
    }

    // Helper classes *********************************************************

    private static class PrivateNetworking {
        public String toString() {
            return Translation.getTranslation("general.network_mode.private");
        }
    }

    private static class LanOnlyNetworking {
        public String toString() {
            return Translation.getTranslation("general_network_mode.lan_only");
        }
    }

    /**
     * Creates a language chooser, which contains the supported locales
     *
     * @return a language chooser, which contains the supported locales
     */
    private static JComboBox createLanguageChooser() {
        // Create combobox
        JComboBox chooser = new JComboBox();
        for (Locale locale1 : Translation.getSupportedLocales()) {
            chooser.addItem(locale1);
        }
        // Set current locale as selected
        chooser.setSelectedItem(Translation.getResourceBundle().getLocale());

        // Add renderer
        chooser.setRenderer(new MyDefaultListCellRenderer());

        // Initialize chooser with the active locale.
        chooser.setSelectedItem(Translation.getActiveLocale());
        return chooser;
    }

    private static class MyDefaultListCellRenderer extends
        DefaultListCellRenderer
    {
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);
            if (value instanceof Locale) {
                Locale locale = (Locale) value;
                setText(locale.getDisplayName(locale));
            } else {
                setText("- unknown -");
            }
            return this;
        }
    }
}
