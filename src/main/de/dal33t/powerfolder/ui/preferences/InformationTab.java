/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.preferences;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.TextLinesPanelBuilder;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.JavaVersion;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;

public class InformationTab extends PFComponent implements PreferenceTab {

    private static final int HEADER_FONT_SIZE = 16;
    private JPanel panel;

    public InformationTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.get("exp.preferences.information.title");
    }

    public boolean needsRestart() {
        return false;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        return true;
    }


    private void initComponents() {
        // PFC-3643: modern, flat, theme-aware layout. The dated etched TitledBorder
        // boxes are replaced by section separators (see createSection); the general
        // section spans the full width, with PowerFolder / System side by side below.
        FormLayout layout = new FormLayout(
            "pref:grow, 14dlu, pref:grow",
            "pref, 12dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("10dlu, 12dlu, 10dlu, 12dlu"));
        CellConstraints cc = new CellConstraints();
        builder.add(createGeneralBox(), cc.xyw(1, 1, 3));
        builder.add(createPowerFolderBox(), cc.xy(1, 3, "fill, top"));
        builder.add(createSystemBox(), cc.xy(3, 3, "fill, top"));

        panel = builder.getPanel();
    }

    private JButton createActivateButton() {
        boolean isActivated = getController().getNodeManager().isStarted();

        // PFC-2508
        if (isActivated) {
            return null;
        }

        // PFC-2580
        boolean isLicenseCheckEnabled = getController().getUIController()
            .getApplicationModel().getLicenseModel().getActivationAction() != null;
        if (!isLicenseCheckEnabled) {
            return null;
        }

        JButton activateButton = new JButton(
            Translation.get("exp.preferences.information.activate_text"));
        activateButton.setToolTipText(Translation
            .get("exp.preferences.information.activate_tips"));
        activateButton.setMnemonic(Translation
            .get("exp.preferences.information.activate_key").trim().charAt(0));
        Action action = getController().getUIController().getApplicationModel()
                .getLicenseModel().getActivationAction();
        if (action != null) {
            activateButton.addActionListener(action);
        }
        boolean changeLoginAllowed =
                ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED
            .getValueBoolean(getController());
        activateButton.setEnabled(changeLoginAllowed);
        return activateButton;
    }

    private long calculateTotalLocalSharedSize() {
        long totalSize = 0;
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            totalSize += folder.getStatistic().getLocalSize();
        }
        return totalSize;
    }

    private JPanel createSystemBox() {
        return createTextBox(
            Translation.get("exp.preferences.information.your_system_title"),
            Translation.get("exp.preferences.information.your_system_java_version",
                JavaVersion.systemVersion().toString())
                + '\n'
                + Translation.get("exp.preferences.information.your_system_os",
                    System.getProperty("os.name"))
                + " ("
                + (OSUtil.is64BitPlatform() ? "64bit" : "32bit")
                + ')'
                + '\n'
                + Translation.get(
                    "exp.preferences.information.power_folder_max",
                    String
                        .valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024))
                + '\n'
                + Translation.get(
                    "exp.preferences.information.power_folder_used",
                    String
                        .valueOf(Runtime.getRuntime().totalMemory() / 1024 / 1024)));
    }

    private JPanel createPowerFolderBox() {
        String config = getController().getConfig().getProperty("config.url", "Default");
        if (config != null) {
            int lastSlash = config.lastIndexOf("/");
            int lastDot = config.lastIndexOf(".config");

            if (lastDot > 0) {
                config = config.substring(lastSlash + 1, lastDot);
            } else {
                config = "Default";
            }
        }
        long dbSize = Debug.countDataitems(getController());

        return createTextBox(
                Translation.get("general.application.name"),
                Translation.get("exp.preferences.information.power_folder_text",
                        Controller.PROGRAM_VERSION)
                        + '\n'
                        + Translation.get(
                        "exp.preferences.information.power_folder_distribution",
                        getController().getDistribution().getName()) + '\n'
                        + Translation.get("exp.preferences.information.config_name", config)
                        + '\n'
                        + Translation.get(
                        "exp.preferences.information.power_folder_data_size",
                        Format.formatBytesShort(calculateTotalLocalSharedSize()))
                        + '\n'
                        + Translation.get(
                        "exp.preferences.information.power_folder_db_size", String.valueOf(dbSize))
                ,createActivateButton()
        );

    }

    private static JPanel createTextBox(String title, String contents) {
        return createTextBox(title, contents, null);
    }

    private static JPanel createTextBox(String title, String contents, JButton button) {
        String[] contentsArray = contents.split("\n");

        // PFC-3643: a flat section = a titled separator header, then the content
        // lines as plain (theme-coloured) labels on a transparent panel.
        FormLayout contentsForm = new FormLayout("pref:grow", "pref, 5dlu");
        PanelBuilder builder = new PanelBuilder(contentsForm);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(title, cc.xy(1, 1));
        int row = 2;

        for (String lineText : contentsArray) {
            if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("3dlu");
                row += 1;
            } else {
                builder.appendRow("pref");
                row += 1;
                builder.add(new JLabel(lineText), cc.xy(1, row));
            }
        }

        if (button != null) {
            // Gap, then button
            builder.appendRow("8dlu");
            row += 1;
            builder.appendRow("pref");
            row += 1;
            builder.add(button, cc.xy(1, row));
        }

        JPanel textBoxPanel = builder.getPanel();
        textBoxPanel.setOpaque(false);
        return textBoxPanel;
    }

    private JPanel createGeneralBox() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, 8dlu, pref, 12dlu, pref, 6dlu, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(
            Translation.get("exp.preferences.information.general_information"),
            cc.xy(1, 1));
        builder.add(TextLinesPanelBuilder.createTextPanel(
            Translation.get("exp.preferences.information.app_description"),
            HEADER_FONT_SIZE), cc.xy(1, 3));
        builder.add(createHomeLink().getUIComponent(), cc.xy(1, 5));
        builder.add(createDocLink().getUIComponent(), cc.xy(1, 7));
        builder.add(createSupportLink().getUIComponent(), cc.xy(1, 9));

        JPanel generalPanel = builder.getPanel();
        generalPanel.setOpaque(false);
        return generalPanel;
    }

    private LinkLabel createHomeLink() {
        LinkLabel homeLink =  new LinkLabel(getController(),
            Translation.get("exp.preferences.information.home_page"),
            ConfigurationEntry.PROVIDER_URL.getValue(getController()));
        SimpleComponentFactory.setFontSize(homeLink.getUIComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);
        return homeLink;
    }

    private LinkLabel createDocLink() {
        String docLinkStr = ConfigurationEntry.PROVIDER_WIKI_URL
            .getValue(getController());
        if (StringUtils.isBlank(docLinkStr)) {
            docLinkStr = ConfigurationEntry.PROVIDER_QUICKSTART_URL
                .getValue(getController());
        }
        LinkLabel docLink = new LinkLabel(getController(),
            Translation.get("exp.preferences.information.documentation"),
            docLinkStr);
        docLink.setVisible(StringUtils.isNotBlank(docLinkStr));
        SimpleComponentFactory.setFontSize(docLink.getUIComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);
        return docLink;
    }

    private LinkLabel createSupportLink() {
        LinkLabel supportLink = new LinkLabel(getController(),
            Translation.get("exp.preferences.information.support"),
            ConfigurationEntry.PROVIDER_SUPPORT_URL.getValue(getController()));
        SimpleComponentFactory.setFontSize(supportLink.getUIComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);
        return supportLink;
    }

    /**
     * Creates the JPanel for advanced settings
     *
     * @return the created panel
     */
    public JPanel getUIPanel() {
        // Put the panel in a panel with a pref:grow Y-axis.
        // So it does not  s-t-r-e-a-c-h  vertically.
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(panel, cc.xy(1, 1));
        return builder.getPanel();
    }

    public void save() {
        // Nothing to do here.
    }

}
