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
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.TextLinesPanelBuilder;

/**
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.16 $
 */
public class AboutDialog extends PFUIComponent {

    // read from jar manifest
    private String buildDate;
    private String buildTime;

    /**
     * used to change the order of focus ok button needs focus, so enter will
     * dismiss this dialog
     */
    private Component[] focusList;
    /** points to the current component with focus * */
    private int focusNumber;

    private JPanel panel;
    private JLabel logoLabel;

    private JPanel team;
    private JPanel contributers;
    private JPanel translators;
    private JPanel system;
    private JPanel powerFolder;

    private LinkLabel docLink;
    private LinkLabel homeLink;
    private LinkLabel supportLink;

    private static final int HEADER_FONT_SIZE = 16;
    private JDialog dialog;
    private JButton checkForUpdatesButton;
    private JButton systemMonitorButton;
    private JButton bugReportButton;
    private JButton okButton;
    private ActionListener closeAction;
    /** when enter is pressed the button/action with focus is called * */
    private ActionListener generalAction;
    private ActionListener updateAction;
    private ActionListener systemMonitorAction;
    private ActionListener bugReportAction;
    private PacmanPanel pacmanPanel;

    public AboutDialog(Controller controller) {
        super(controller);
    }

    public void open() {
        dialog = new JDialog(getUIController().getMainFrame().getUIComponent());
        dialog.setTitle(Translation.getTranslation("about_dialog.title"));
        dialog.setModal(true);
        dialog.setContentPane(getUIComponent());
        dialog.setFocusTraversalPolicy(new MyFocusTraversalPolicy());
        JComponent rootPane = dialog.getRootPane();
        // register keys
        KeyStroke strokeEsc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        KeyStroke strokeEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        rootPane.registerKeyboardAction(closeAction, strokeEsc,
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        rootPane.registerKeyboardAction(generalAction, strokeEnter,
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();

        int x = ((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                - dialog.getWidth()) / 2;
        int y = ((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight()
                - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.setResizable(false);
        dialog.setVisible(true);

        okButton.requestFocus();

    }

    private JComponent getUIComponent() {
        if (panel == null) {
            // Init components
            initComponents();
            // Main layout
            FormLayout layout = new FormLayout("pref, 2dlu, fill:pref",
                "fill:pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU2_BORDER);
            CellConstraints cc = new CellConstraints();
            builder.add(logoLabel, cc.xy(1, 1));
            builder.add(createRightPanel(), cc.xy(3, 1));
            builder.add(createToolbar(), cc.xyw(1, 2, 3));

            panel = builder.getPanel();
            panel.setBackground(Color.WHITE);

        }
        return panel;
    }

    private void initComponents() {
        readDateTimeFromJar();
        closeAction = new CloseAction();
        generalAction = new GeneralAction();
        updateAction = new UpdateAction();
        systemMonitorAction = new SystemMonitorAction();
        bugReportAction = new BugReportAction();

        pacmanPanel = new PacmanPanel();

        logoLabel = buildAboutAnimation();

        docLink = new LinkLabel(getController(), Translation
            .getTranslation("about_dialog.documentation"),
            ConfigurationEntry.PROVIDER_WIKI_URL.getValue(getController()));
        SimpleComponentFactory.setFontSize((JLabel) docLink.getUiComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);
        homeLink = new LinkLabel(getController(), Translation
            .getTranslation("about_dialog.home_page"),
            ConfigurationEntry.PROVIDER_WIKI_URL.getValue(getController()));
        SimpleComponentFactory.setFontSize((JLabel) homeLink.getUiComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);
        supportLink = new LinkLabel(getController(), Translation
            .getTranslation("about_dialog.support"),
            ConfigurationEntry.PROVIDER_WIKI_URL.getValue(getController()));
        SimpleComponentFactory.setFontSize((JLabel) supportLink.getUiComponent(),
            SimpleComponentFactory.BIG_FONT_SIZE);

        powerFolder = createTextBox(Translation
            .getTranslation("general.powerfolder"), Translation.getTranslation(
                "about_dialog.power_folder.text", Controller.PROGRAM_VERSION)
            + '\n'
            + Translation.getTranslation("about_dialog.power_folder.build_date",
                buildDate)
            + '\n'
            + Translation.getTranslation("about_dialog.power_folder.build_time",
                buildTime)
            + '\n'
            + Translation.getTranslation("about_dialog.power_folder.max",
                Runtime.getRuntime().maxMemory() / 1024 / 1024)
            + '\n'
            + Translation.getTranslation("about_dialog.power_folder.used",
                Runtime.getRuntime().totalMemory() / 1024 / 1024)
            + '\n'
            + Translation.getTranslation(
                "about_dialog.power_folder.distribution", getController()
                    .getDistribution().getName()));

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        system = createTextBox(Translation
            .getTranslation("about_dialog.your_system.title"), Translation
            .getTranslation("about_dialog.your_system.java_version",
                    JavaVersion.systemVersion().toString())
            + '\n'
            + Translation.getTranslation("about_dialog.your_system.os", System
                .getProperty("os.name"))
            + '\n'
            + Translation.getTranslation("about_dialog.your_system.screen",
                dim.width, dim.height));

        team = createTextBox(
            Translation.getTranslation("about_dialog.team"),
            "Bernhard Rutkowsky\nCecilia Saltori\nChristian Sprajc\nDennis Waldherr\nFlorian Lahr\nHarry Glasgow\n");

        contributers = createTextBox(
            Translation.getTranslation("about_dialog.contributers"),
            "Anas Hnidi\nDaniel Harabor\nDane Smith\nJan van Oosterom\nMichael Petrovic-Brings\nNick Khazov\nThorsten Lenze\nPavel Tenenbaum\nPeter H&uuml;ren\nOliver H&auml;usler");

        translators = createTextBox(Translation
            .getTranslation("about_dialog.translators"), "Bayan El Ameen\n"
            + "Cecilia Saltori\n" + "Javier Isassi\n" + "Keblo\n"
            + "Olle Wikstrom\n" + "Zhang Jia\n ");
    }

    /**
     * Builds and returns the About animation
     * 
     * @return the link to the
     */
    private JLabel buildAboutAnimation() {
        if (Icons.getInstance().ABOUT_ANIMATION instanceof ImageIcon) {
            ((ImageIcon) Icons.getInstance().ABOUT_ANIMATION).getImage().flush();
            ((ImageIcon) Icons.getInstance().ABOUT_ANIMATION).getImage()
                .setAccelerationPriority(0.2F);

        }
        JLabel logo = new JLabel(Icons.getInstance().ABOUT_ANIMATION);
        logo.setSize(new Dimension(Icons.getInstance().ABOUT_ANIMATION.getIconWidth(),
            Icons.getInstance().ABOUT_ANIMATION.getIconHeight()));
        logo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 3) {
                    pacmanPanel.activate();
                }
            }
        });
        return logo;
    }

    private JPanel createRightPanel() {
        FormLayout layout = new FormLayout(
            "pref:grow, pref:grow, pref:grow, pref:grow",
            "fill:pref:grow, fill:pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        CellConstraints cc = new CellConstraints();
        builder.add(createGeneralPanel(), cc.xywh(1, 1, 2, 1));
        builder.add(powerFolder, cc.xy(1, 2));
        builder.add(system, cc.xy(2, 2));
        builder.add(team, cc.xy(3, 1));

        builder.add(translators, cc.xy(3, 2));
        builder.add(contributers, cc.xywh(4, 1, 1, 2));

        JPanel rightPanel = builder.getPanel();
        rightPanel.setBackground(Color.WHITE);
        return rightPanel;

    }

    private JPanel createToolbar() {
        FormLayout layout = new FormLayout("pref:grow, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        // builder.setBorder(Borders.DLU2_BORDER);
        CellConstraints cc = new CellConstraints();

        createBugReportButton();
        createCheckForUpdatesButton();
        createSystemMonitorButton();
        createOKButton();
        focusList = new Component[]{okButton, bugReportButton,
                checkForUpdatesButton, systemMonitorButton};
        JPanel buttons = ButtonBarFactory.buildRightAlignedBar(
                checkForUpdatesButton, bugReportButton, systemMonitorButton, okButton);
        buttons.setOpaque(false);

        builder.add(pacmanPanel, cc.xy(1, 1));
        builder.add(buttons, cc.xy(3, 1));
        JPanel jPanel = builder.getPanel();
        jPanel.setOpaque(true);
        jPanel.setBackground(Color.WHITE);
        return jPanel;
    }

    private JPanel createGeneralPanel() {
        FormLayout layout = new FormLayout("pref",
            "pref, 15dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(TextLinesPanelBuilder.createTextPanel(Translation
            .getTranslation("about_dialog.sync_your_world"), HEADER_FONT_SIZE),
            cc.xy(1, 1));
        builder.add(homeLink.getUiComponent(), cc.xy(1, 3));
        builder.add(docLink.getUiComponent(), cc.xy(1, 5));
        builder.add(supportLink.getUiComponent(), cc.xy(1, 7));

        TitledBorder titledBorder = new TitledBorder(Translation
            .getTranslation("about_dialog.general_information"));
        titledBorder.setTitleColor(Color.BLACK);
        builder.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(2,
            2, 2, 2)));

        JPanel generalPanel = builder.getPanel();
        generalPanel.setBackground(Color.WHITE);
        return generalPanel;
    }

    /**
     * Creates an internationlaized ok button. Hides this aboutbox, cleans up
     * resources.
     * 
     * @return the ok Button
     */
    private void createOKButton() {
        okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key")
            .trim().charAt(0));
        okButton.addActionListener(closeAction);
        okButton.setBackground(Color.WHITE);
    }

    /** performed if the enter key is pressed */
    private class GeneralAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (okButton.hasFocus()) {
                closeAction.actionPerformed(e);
            } else if (checkForUpdatesButton.hasFocus()) {
                updateAction.actionPerformed(e);
            } else if (bugReportButton.hasFocus()) {
                bugReportAction.actionPerformed(e);
            } else if (systemMonitorButton.hasFocus()) {
                systemMonitorAction.actionPerformed(e);
            }
        }
    }

    private class CloseAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }

    private class UpdateAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (getController().getUpdateSettings() != null) {
                new ManuallyInvokedUpdater(getController(), getController()
                    .getUpdateSettings()).start();
            }
            PreferencesEntry.CHECK_UPDATE.setValue(getController(), true);
        }
    }

    private class SystemMonitorAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            getUIController().displaySystemMonitorWindow();
            closeAction.actionPerformed(e);
        }
    }

    private class BugReportAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                BrowserLauncher
                    .openURL(ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL
                        .getValue(getController()));
            } catch (IOException e1) {
                logSevere("IOException", e1);
            }
        }

    }

    /**
     * Creates an internationlaized check for updates button. This button will
     * invoke the manual updatechecker.
     *
     * @return The Button
     */
    private JButton createSystemMonitorButton() {
        systemMonitorButton = new JButton(Translation
            .getTranslation("about_dialog.system_monitor.text"));
        systemMonitorButton.setToolTipText(Translation.getTranslation(
            "about_dialog.system_monitor.tips"));
        systemMonitorButton.setMnemonic(Translation.getTranslation(
            "about_dialog.system_monitor.key").trim().charAt(0));
        systemMonitorButton.setIcon(Icons.SYSTEM_MONITOR);
        systemMonitorButton.addActionListener(systemMonitorAction);
        systemMonitorButton.setBackground(Color.WHITE);
        return systemMonitorButton;
    }

    /**
     * Creates an internationlaized check for updates button. This button will
     * invoke the manual updatechecker.
     *
     * @return The Button
     */
    private void createCheckForUpdatesButton() {
        checkForUpdatesButton = new JButton(Translation
            .getTranslation("about_dialog.check_for_updates.text"));
        checkForUpdatesButton.setToolTipText(Translation.getTranslation(
            "about_dialog.check_for_updates.tips"));
        checkForUpdatesButton.setMnemonic(Translation.getTranslation(
            "about_dialog.check_for_updates.key").trim().charAt(0));
        checkForUpdatesButton.setIcon(Icons.UPDATES);
        checkForUpdatesButton.addActionListener(updateAction);
        checkForUpdatesButton.setBackground(Color.WHITE);
    }

    /**
     * @return a button that opens the bug report url
     */
    private void createBugReportButton() {
        bugReportButton = new JButton(Translation
            .getTranslation("about_dialog.send_bug_report.text"));
        bugReportButton.setIcon(Icons.DEBUG);
        bugReportButton.setToolTipText(Translation.getTranslation(
            "about_dialog.send_bug_report.tips"));
        bugReportButton.setMnemonic(Translation.getTranslation(
            "about_dialog.send_bug_report.key").trim().charAt(0));
        bugReportButton.addActionListener(bugReportAction);
        bugReportButton.setBackground(Color.WHITE);
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

    /**
     * Reads the date and time from the jarfile manifest "BuildDateTime"
     * property. This is set by build.xml when ant creates the jar file
     * 
     * @see #buildDate
     * @see #buildTime
     */
    private void readDateTimeFromJar() {
        try {
            File jar = new File("PowerFolder.jar");
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
                "hh:mm:ss z", Translation.getActiveLocale());

            buildTime = localizedFormatter.format(buildDateTime);
            // localizedFormatter.applyPattern("dd MMMM yyyy");
            localizedFormatter.applyPattern(Translation
                .getTranslation("general.localized_date_format"));
            buildDate = localizedFormatter.format(buildDateTime);

            file.close();
        } catch (Exception e) {
            logSevere("Build date/time works only from jar.");
            buildTime = "n/a";
            buildDate = "n/a";
        }
    }

    class MyFocusTraversalPolicy extends FocusTraversalPolicy {
        public Component getComponentAfter(Container focusCycleRoot,
            Component aComponent)
        {
            focusNumber = (focusNumber + 1) % focusList.length;
            if (!focusList[focusNumber].isEnabled()) {
                getComponentAfter(focusCycleRoot, focusList[focusNumber]);
            }
            return focusList[focusNumber];
        }

        public Component getComponentBefore(Container focusCycleRoot,
            Component aComponent)
        {
            focusNumber = (focusList.length + focusNumber - 1)
                % focusList.length;
            if (!focusList[focusNumber].isEnabled()) {
                getComponentBefore(focusCycleRoot, focusList[focusNumber]);
            }
            return focusList[focusNumber];
        }

        public Component getDefaultComponent(Container focusCycleRoot) {
            return focusList[0];
        }

        public Component getLastComponent(Container focusCycleRoot) {
            return focusList[focusList.length - 1];
        }

        public Component getFirstComponent(Container focusCycleRoot) {
            return focusList[0];
        }
    }
}
