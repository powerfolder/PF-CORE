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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.TextLinesPanelBuilder;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.JavaVersion;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.ui.util.update.ManuallyInvokedUpdateHandler;
import de.dal33t.powerfolder.util.update.Updater;

public class InformationTab extends PFComponent implements PreferenceTab {

    private static final int HEADER_FONT_SIZE = 16;

    private String buildDate;
    private String buildTime;

    private JPanel panel;

    public InformationTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.information.title");
    }

    public boolean needsRestart() {
        return false;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        return true;
    }

    private static JPanel createTeamPanel() {
    return createTextBox(
        Translation.getTranslation("preferences.information.team"),
        "Bernhard Rutkowsky\nCecilia Saltori\nChristian Sprajc\nDennis Waldherr\nFlorian Lahr\nHarry Glasgow\n");
    }

    private void initComponents() {
        readDateTimeFromJar();
        FormLayout layout = new FormLayout(
            "pref:grow, pref:grow, pref:grow",
            "fill:pref:grow, fill:pref:grow, fill:pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createGeneralBox(), cc.xywh(1, 1, 2, 1));
        builder.add(createPowerFolderBox(), cc.xy(1, 2));
        builder.add(createSystemBox(), cc.xy(2, 2));
        if (getController().getDistribution().showCredentials()) {
            builder.add(createTeamPanel(), cc.xy(3, 1));
            builder.add(createTranslators(), cc.xy(3, 2));
        }
        builder.add(ButtonBarFactory.buildCenteredBar(
                createActivateButton()), cc.xyw(1, 3, 3));

        panel = builder.getPanel();
    }

    private JButton createActivateButton() {
        JButton activateButton = new JButton(
            Translation.getTranslation("preferences.information.activate_text"));
        activateButton.setToolTipText(Translation
            .getTranslation("preferences.information.activate_tips"));
        activateButton.setMnemonic(Translation
            .getTranslation("preferences.information.activate_key").trim().charAt(0));
        Action action = getController().getUIController().getApplicationModel()
                .getLicenseModel().getActivationAction();
        if (action != null) {
            activateButton.addActionListener(action);
        }
        activateButton.setBackground(Color.WHITE);
        boolean changeLoginAllowed =
                ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED
            .getValueBoolean(getController());
        activateButton.setEnabled(changeLoginAllowed);
        return activateButton;
    }

    private static JPanel createTranslators() {
        return createTextBox(
            Translation.getTranslation("preferences.information.translators"),
            "Bayan El Ameen\n" + "Cecilia Saltori\n" + "Javier Isassi\n"
                + "Keblo\n" + "Olle Wikstrom\n" + "Zhang Jia\n ");
    }

    /**
     * Reads the date and time from the jarfile manifest "BuildDateTime"
     * property. This is set by build.xml when ant creates the jar file
     *
     * @see #buildDate
     * @see #buildTime
     */
    private void readDateTimeFromJar() {
        try {
            File jar = new File(getController().getJARName());
            JarFile file = new JarFile(jar);
            Manifest mf = file.getManifest();
            Attributes attr = mf.getMainAttributes();

            logFine(attr.getValue("BuildDateTime"));

            String buildDateTimeString = attr.getValue("BuildDateTime");
            SimpleDateFormat parser = new SimpleDateFormat();
            // must match the format in build.xml
            parser.applyPattern("MMMM/dd/yyyy hh:mm:ss a, z");
            Date buildDateTime = parser.parse(buildDateTimeString);

            SimpleDateFormat localizedFormatter = new SimpleDateFormat(
                "HH:mm:ss z", Translation.getActiveLocale());

            buildTime = localizedFormatter.format(buildDateTime);
            // localizedFormatter.applyPattern("dd MMMM yyyy");
            localizedFormatter.applyPattern(Translation
                .getTranslation("general.localized_date_format"));
            buildDate = localizedFormatter.format(buildDateTime);

            file.close();
        } catch (Exception e) {
            logInfo("Build date/time works only from jar.");
            buildTime = "n/a";
            buildDate = "n/a";
        }
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

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        long dbSize = Debug.countDataitems(getController());

        return createTextBox(
            Translation.getTranslation("preferences.information.your_system_title"),
            Translation.getTranslation("preferences.information.your_system_java_version",
                JavaVersion.systemVersion().toString())
                + '\n'
                + Translation.getTranslation("preferences.information.your_system_os",
                    System.getProperty("os.name"))
                + " ("
                + (OSUtil.is64BitPlatform() ? "64bit" : "32bit")
                + ')'
                + '\n'
                + Translation.getTranslation("preferences.information.screen",
                    String.valueOf(dim.width), String.valueOf(dim.height))
                + '\n'
                + Translation.getTranslation(
                    "preferences.information.power_folder_max",
                    String
                        .valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024))
                + '\n'
                + Translation.getTranslation(
                    "preferences.information.power_folder_used",
                    String
                        .valueOf(Runtime.getRuntime().totalMemory() / 1024 / 1024))
                + '\n'
                + Translation.getTranslation(
                    "preferences.information.power_folder_data_size",
                    Format.formatBytesShort(calculateTotalLocalSharedSize()))
                + '\n'
                + Translation.getTranslation(
                    "preferences.information.power_folder_db_size", String.valueOf(dbSize)));
    }

    private String readLicense() {
    Object licKey = getController().getUIController().getApplicationModel().getLicenseModel()
        .getLicenseKeyModel().getValue();
    return licKey != null ? Translation.getTranslation(
            "preferences.information.power_folder_license", licKey.toString()) : "";
    }

    private JPanel createPowerFolderBox() {
        return createTextBox(
                Translation.getTranslation("general.application.name"),
                Translation.getTranslation("preferences.information.power_folder_text",
                        Controller.PROGRAM_VERSION)
                        + '\n'
                        + Translation.getTranslation(
                        "preferences.information.power_folder_build_date", buildDate)
                        + '\n'
                        + Translation.getTranslation(
                        "preferences.information.power_folder_build_time", buildTime)
                        + '\n'
                        + Translation.getTranslation(
                        "preferences.information.power_folder_distribution",
                        getController().getDistribution().getName()) + '\n' +
                        readLicense());
    }

    private static JPanel createTextBox(String title, String contents) {
        String[] contentsArray = contents.split("\n");

        FormLayout contentsForm = new FormLayout("pref");
        PanelBuilder builder = new PanelBuilder(contentsForm);

        TitledBorder titledBorder = new TitledBorder(title);
        titledBorder.setTitleColor(Color.BLACK);
        builder.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(2,
            2, 2, 2)));
        // split into tokens
        int row = 1;
        CellConstraints cc = new CellConstraints();

        for (String lineText : contentsArray) {
            if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("3dlu");
            } else {
                builder.appendRow("pref");
                builder.add(new JLabel("<HTML><BODY>" + lineText
                    + "</BODY></HTML>"), cc.xy(1, row));
            }
            row += 1;
        }
        JPanel textBoxPanel = builder.getPanel();
        textBoxPanel.setBackground(Color.WHITE);
        return textBoxPanel;
    }

    private JPanel createGeneralBox() {
        FormLayout layout = new FormLayout("pref",
            "pref, 15dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(TextLinesPanelBuilder.createTextPanel(
            Translation.getTranslation("preferences.information.app_description"),
            HEADER_FONT_SIZE), cc.xy(1, 1));
        builder.add(createHomeLink().getUIComponent(), cc.xy(1, 3));
        builder.add(createDocLink().getUIComponent(), cc.xy(1, 5));
        builder.add(createSupportLink().getUIComponent(), cc.xy(1, 7));

        TitledBorder titledBorder = new TitledBorder(
            Translation.getTranslation("preferences.information.general_information"));
        titledBorder.setTitleColor(Color.BLACK);
        builder.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(2,
            2, 2, 2)));

        JPanel generalPanel = builder.getPanel();
        generalPanel.setBackground(Color.WHITE);
        return generalPanel;
    }

    private LinkLabel createHomeLink() {
        LinkLabel homeLink =  new LinkLabel(getController(),
            Translation.getTranslation("preferences.information.home_page"),
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
            Translation.getTranslation("preferences.information.documentation"),
            docLinkStr);
        docLink.setVisible(StringUtils.isNotBlank(docLinkStr));
        SimpleComponentFactory.setFontSize(docLink.getUIComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);
        return docLink;
    }

    private LinkLabel createSupportLink() {
        LinkLabel supportLink = new LinkLabel(getController(),
            Translation.getTranslation("preferences.information.support"),
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