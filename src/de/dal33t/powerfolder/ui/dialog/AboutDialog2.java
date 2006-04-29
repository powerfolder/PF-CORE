package de.dal33t.powerfolder.ui.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif.component.UIFLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.ManuallyInvokedUpdateChecker;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.LinkLabel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.16 $
 */
public class AboutDialog2 extends PFUIComponent {

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
    private JPanel version;

    private LinkLabel docLink;
    private LinkLabel homeLink;
    private LinkLabel supportLink;

    private static final int HEADER_FONT_SIZE = 16;
    private JDialog dialog;
    private JButton checkForUpdatesButton;
    private JButton okButton;
    private ActionListener closeAction;
    /** when enter is pressed the button/action with focus is called * */
    private ActionListener generalAction;
    private ActionListener updateAction;

    public AboutDialog2(Controller controller) {
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

        startAnimation();

        Component parent = dialog.getOwner();
        int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
        int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.setResizable(false);
        dialog.setVisible(true);

        okButton.requestFocus();

    }

    /**
     * Start the animation
     */
    private void startAnimation() {
        Timer animationTimer = new Timer();
        animationTimer.schedule(new TimerTask() {
            @Override
            public void run()
            {                
                logoLabel.setIcon(Icons.ABOUT_1);
                sleepLong();                
                logoLabel.setIcon(Icons.ABOUT_2);
                sleep();
                logoLabel.setIcon(Icons.ABOUT_3);
                sleep();
                logoLabel.setIcon(Icons.ABOUT_4);
            }

            private void sleep() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            private void sleepLong() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 700);
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
        logoLabel = new JLabel(Icons.ABOUT_0);
        logoLabel.setSize(new Dimension(Icons.ABOUT_0.getIconWidth(),
            Icons.ABOUT_0.getIconHeight()));

        docLink = new LinkLabel(Translation
            .getTranslation("about.dialog.documentation"),
            "http://docs.powerfolder.com");
        SimpleComponentFactory.setFontSize(docLink,
            SimpleComponentFactory.BIG_FONT_SIZE);
        homeLink = new LinkLabel(Translation
            .getTranslation("about.dialog.homepage"),
            "http://www.powerfolder.com");
        SimpleComponentFactory.setFontSize(homeLink,
            SimpleComponentFactory.BIG_FONT_SIZE);
        supportLink = new LinkLabel(Translation
            .getTranslation("about.dialog.support"),
            "http://www.powerfolder.com/?q=node/support");
        SimpleComponentFactory.setFontSize(supportLink,
            SimpleComponentFactory.BIG_FONT_SIZE);

        version = createTextBox(Translation
            .getTranslation("about.dialog.version.title"), Translation
            .getTranslation("about.dialog.version.text",
                Controller.PROGRAM_VERSION)
            + "\n"
            + Translation.getTranslation("about.dialog.version.builddate",
                buildDate)
            + "\n"
            + Translation.getTranslation("about.dialog.version.buildtime",
                buildTime) + "\n ");

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        system = createTextBox(Translation
            .getTranslation("about.dialog.yoursystem.title"), Translation
            .getTranslation("about.dialog.yoursystem.java_version", System
                .getProperty("java.version"))
            + "\n"
            + Translation.getTranslation("about.dialog.yoursystem.os", System
                .getProperty("os.name"))
            + "\n"
            + Translation.getTranslation("about.dialog.yoursystem.screen",
                dim.width, dim.height) + "\n ");

        team = createTextBox(
            Translation.getTranslation("about.dialog.team"),
            "Albena Roshelova\nBernhard Rutkowsky\nChristian Sprajc\nDennis Waldherr\nFlorian Lahr\nJan van Oosterom\nMatthew Chandley\nOliver H&auml;usler");

        contributers = createTextBox(Translation
            .getTranslation("about.dialog.contributers"),
            "Daniel Harabor\nDane Smith\nThorsten Lenze\n \n ");
        testers = createTextBox(Translation
            .getTranslation("about.dialog.testers"),
            "Michael Petrovic-Brings\nPeter H&uuml;ren\n \n ");

        translators = createTextBox(Translation
            .getTranslation("about.dialog.translators"),
            "David Martin\nEric Meunier\nGabriele Falistocco\nKeblo\nPavel Tenenbaum\n \n ");
    }

    private JPanel createRightPanel() {
        FormLayout layout = new FormLayout(
            "pref:grow, pref:grow, pref:grow, pref:grow",
            "fill:pref:grow, fill:pref:grow, fill:pref:grow, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        // builder.setBorder(Borders.DLU2_BORDER);
        CellConstraints cc = new CellConstraints();
        builder.add(createGeneralPanel(), cc.xywh(1, 1, 2, 2));
        builder.add(version, cc.xy(1, 3));
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
        JButton update = createCheckForUpdatesButton();
        JButton ok = createOKButton();
        focusList = new Component[]{ok, update};
        JPanel buttons = ButtonBarFactory.buildRightAlignedBar(update, ok);
        buttons.setBackground(Color.WHITE);

        return buttons;
    }

    private JPanel createGeneralPanel() {
        FormLayout layout = new FormLayout("pref",
            "pref, 8dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createHeaderLabel(Translation
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
            new ManuallyInvokedUpdateChecker(getController()).start();
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
        return checkForUpdatesButton;
    }

    private JPanel createTextBox(String title, String contents) {
        String contentsArray[] = contents.split("\n");

        FormLayout contentsForm = new FormLayout("pref");
        PanelBuilder builder = new PanelBuilder(contentsForm);

        TitledBorder titledBorder = new TitledBorder(title);
        titledBorder.setTitleColor(Color.BLACK);
        builder.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(2,
            2, 2, 2)));
        // split into tokens
        int row = 1;
        CellConstraints cc = new CellConstraints();

        for (int i = 0; i < contentsArray.length; i++) {
            String lineText = contentsArray[i];
            if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("4dlu");
            } else {
                builder.appendRow("pref");
                builder.add(new JLabel("<HTML><BODY>" + contentsArray[i]
                    + "</BODY></HTML>"), cc.xy(1, row));
            }
            row += 1;
        }
        JPanel textBoxPanel = builder.getPanel();
        textBoxPanel.setBackground(Color.WHITE);
        return textBoxPanel;
    }

    private static JLabel createHeaderLabel(String text, int fontsize) {
        JLabel label = new UIFLabel(text, true);
        Font font = new Font(label.getFont().getFontName(), Font.BOLD, fontsize);
        label.setFont(font);
        return label;
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
            if (focusList[focusNumber].isEnabled() == false) {
                getComponentAfter(focusCycleRoot, focusList[focusNumber]);
            }
            return focusList[focusNumber];
        }

        public Component getComponentBefore(Container focusCycleRoot,
            Component aComponent)
        {
            focusNumber = (focusList.length + focusNumber - 1)
                % focusList.length;
            if (focusList[focusNumber].isEnabled() == false) {
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
