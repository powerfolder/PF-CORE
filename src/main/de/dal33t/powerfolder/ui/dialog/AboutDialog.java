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

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.ManuallyInvokedUpdateChecker;
import de.dal33t.powerfolder.util.Translation;
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
    private int focusNumber = 0;

    private JPanel panel;
    private JLabel logoLabel;

    private JPanel team;
    private JPanel testers;
    private JPanel contributers;
    private JPanel translators;
    private JPanel system;
    private JPanel powerFolder;

    private LinkLabel docLink;
    private LinkLabel homeLink;
    private LinkLabel supportLink;

    private static final int HEADER_FONT_SIZE = 16;
    private JDialog dialog;
    private JButton bugReportButton;
    private JButton checkForUpdatesButton;
    private JButton okButton;
    private ActionListener closeAction;
    /** when enter is pressed the button/action with focus is called * */
    private ActionListener generalAction;
    private ActionListener updateAction;

    public AboutDialog(Controller controller) {
        super(controller);
    }

    public void open() {
        dialog = new JDialog(getUIController().getMainFrame().getUIComponent());
        dialog.setTitle(Translation.getTranslation("about.dialog.title"));
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

        Component parent = dialog.getOwner();
        int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
        int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
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
                "fill:pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU2_BORDER);
            CellConstraints cc = new CellConstraints();
            builder.add(logoLabel, cc.xy(1, 1));
            builder.add(createRightPanel(), cc.xy(3, 1));

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

        logoLabel = buildAboutAnimation();

        docLink = new LinkLabel(Translation
            .getTranslation("about.dialog.documentation"),
            "http://docs.powerfolder.com");
        SimpleComponentFactory.setFontSize(docLink,
            SimpleComponentFactory.BIG_FONT_SIZE);
        homeLink = new LinkLabel(Translation
            .getTranslation("about.dialog.homepage"), Constants.POWERFOLDER_URL);
        SimpleComponentFactory.setFontSize(homeLink,
            SimpleComponentFactory.BIG_FONT_SIZE);
        supportLink = new LinkLabel(Translation
            .getTranslation("about.dialog.support"),
            "http://docs.powerfolder.com");
        SimpleComponentFactory.setFontSize(supportLink,
            SimpleComponentFactory.BIG_FONT_SIZE);

        powerFolder = createTextBox(Translation
            .getTranslation("general.powerfolder"), Translation
            .getTranslation("about.dialog.power_folder.text",
                Controller.PROGRAM_VERSION)
            + '\n'
            + Translation.getTranslation("about.dialog.power_folder.builddate",
                buildDate)
            + '\n'
            + Translation.getTranslation("about.dialog.power_folder.buildtime",
                buildTime)
            + '\n'
            + Translation.getTranslation("about.dialog.power_folder.max",
                Runtime.getRuntime().maxMemory() / 1024 / 1024)
            + '\n'
            + Translation.getTranslation("about.dialog.power_folder.used",
                Runtime.getRuntime().totalMemory() / 1024 / 1024)
        );

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        system = createTextBox(Translation
                .getTranslation("about.dialog.yoursystem.title"), Translation
                .getTranslation("about.dialog.yoursystem.java_version", System
                        .getProperty("java.version"))
                + '\n'
                + Translation.getTranslation("about.dialog.yoursystem.os", System
                .getProperty("os.name"))
                + '\n'
                + Translation.getTranslation("about.dialog.yoursystem.screen",
                dim.width, dim.height)
        );

        team = createTextBox(
            Translation.getTranslation("about.dialog.team"),
            "Bernhard Rutkowsky\nCecilia Saltori\nChristian Sprajc\nDennis Waldherr\nFlorian Lahr\nHarry Glasgow\nJay Sun\n");

        contributers = createTextBox(
            Translation.getTranslation("about.dialog.contributers"),
            "Daniel Harabor\nDane Smith\nJan van Oosterom\nThorsten Lenze\nPavel Tenenbaum\nOliver H&auml;usler");
        testers = createTextBox(Translation
            .getTranslation("about.dialog.testers"),
            "Michael Petrovic-Brings\nPeter H&uuml;ren\n \n ");

        translators = createTextBox(
            Translation.getTranslation("about.dialog.translators"),
            "Anas Hnidi\n" +
            "Cecilia Saltori\n" +
            "Jan Van Oosterom\n" +
            "Javier Isassi\n" +
            "Keblo\n" +
            "Nick Khazov\n" +
            "Olle Wikstrom\n" +
            "Zhang Jia\n ");
    }

    /**
     * Builds and returns the About animation
     * 
     * @return the link to the
     */
    private JLabel buildAboutAnimation() {
        if (Icons.ABOUT_ANIMATION instanceof ImageIcon) {
            ((ImageIcon) Icons.ABOUT_ANIMATION).getImage().flush();
            ((ImageIcon) Icons.ABOUT_ANIMATION).getImage()
                .setAccelerationPriority(0.2F);

        }
        JLabel logo = new JLabel(Icons.ABOUT_ANIMATION);
        logo.setSize(new Dimension(Icons.ABOUT_ANIMATION.getIconWidth(),
            Icons.ABOUT_ANIMATION.getIconHeight()));
        return logo;
    }

    private JPanel createRightPanel() {
        FormLayout layout = new FormLayout(
            "pref:grow, pref:grow, pref:grow, pref:grow",
            "fill:pref:grow, fill:pref:grow, fill:pref:grow, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        // builder.setBorder(Borders.DLU2_BORDER);
        CellConstraints cc = new CellConstraints();
        builder.add(createGeneralPanel(), cc.xywh(1, 1, 2, 2));
        builder.add(powerFolder, cc.xy(1, 3));
        builder.add(system, cc.xy(2, 3));
        builder.add(team, cc.xywh(3, 1, 1, 3));

        builder.add(translators, cc.xy(4, 1));
        builder.add(testers, cc.xy(4, 2));
        builder.add(contributers, cc.xy(4, 3));

        builder.add(createToolbar(), cc.xywh(1, 5, 4, 1));
        JPanel rightPanel = builder.getPanel();
        rightPanel.setBackground(Color.WHITE);
        return rightPanel;

    }

    private JPanel createToolbar() {
        JButton bugReport = createBugReportButton();
        JButton update = createCheckForUpdatesButton();
        JButton ok = createOKButton();
        focusList = new Component[]{ok, update};
        JPanel buttons = ButtonBarFactory.buildRightAlignedBar(bugReport,
            update, ok);
        buttons.setOpaque(false);
        return buttons;
    }

    private JPanel createGeneralPanel() {
        FormLayout layout = new FormLayout("pref",
            "pref, 8dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(TextLinesPanelBuilder.createTextPanel(Translation
            .getTranslation("about.dialog.professional_folder_sharing_tool"),
            HEADER_FONT_SIZE - 4), cc.xy(1, 1));
        builder.add(homeLink, cc.xy(1, 3));
        builder.add(docLink, cc.xy(1, 5));
        builder.add(supportLink, cc.xy(1, 7));

        TitledBorder titledBorder = new TitledBorder(Translation
            .getTranslation("about.dialog.general_information"));
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
    private JButton createOKButton() {
        okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key")
            .trim().charAt(0));
        okButton.addActionListener(closeAction);
        okButton.setBackground(Color.WHITE);
        return okButton;
    }

    /** performed if the enter key is pressed */
    private class GeneralAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (okButton.hasFocus()) {
                closeAction.actionPerformed(e);
            } else if (checkForUpdatesButton.hasFocus()) {
                updateAction.actionPerformed(e);
            }
        }
    }

    private class CloseAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
    }

    private class UpdateAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (getController().getUpdateSettings() != null) {
                new ManuallyInvokedUpdateChecker(getController(),
                    getController().getUpdateSettings()).start();
            }
            PreferencesEntry.CHECK_UPDATE.setValue(getController(), true);
        }
    }

    private class BugReportAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                BrowserLauncher.openURL(Constants.BUG_REPORT_URL);
            } catch (IOException e1) {
                log().error(e1);
            }
        }

    }

    /**
     * Creates an internationlaized check for updates button. This button will
     * invoke the manual updatechecker.
     * 
     * @return The Button
     */
    private JButton createCheckForUpdatesButton() {
        checkForUpdatesButton = new JButton(Translation
            .getTranslation("about.dialog.check_for_updates"));
        checkForUpdatesButton.setMnemonic(Translation.getTranslation(
            "about.dialog.check_for_updates.key").trim().charAt(0));
        checkForUpdatesButton.addActionListener(updateAction);
        checkForUpdatesButton.setBackground(Color.WHITE);
        return checkForUpdatesButton;
    }

    /**
     * @return a button that opens the bug report url
     */
    private JButton createBugReportButton() {
        bugReportButton = new JButton(Translation
            .getTranslation("about.dialog.send_bug_report"));
        bugReportButton.setMnemonic(Translation.getTranslation(
            "about.dialog.send_bug_report.key").trim().charAt(0));
        bugReportButton.addActionListener(new BugReportAction());
        bugReportButton.setBackground(Color.WHITE);
        return bugReportButton;
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
                builder.appendRow("4dlu");
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

            log().debug(attr.getValue("BuildDateTime"));

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
            log().error("Build date/time works only from jar.");
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
