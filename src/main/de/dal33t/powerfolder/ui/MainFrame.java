/* $Id: MainFrame.java,v 1.44 2006/03/07 09:27:20 schaatser Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.component.UIFSplitPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Powerfoldes gui mainframe
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {
    private JFrame uiComponent;

    /** The toolbar ontop */
    private Toolbar toolbar;

    /** The main split pane */
    private JSplitPane mainPane;

    /** left side */
    private ControlQuarter controlQuarter;

    /** right side */
    private InformationQuarter informationQuarter;

    /**
     * The status bar on the lower side of the screen.
     */
    private StatusBar statusBar;

    /**
     * @param controller
     *            the controller.
     * @throws java.awt.HeadlessException
     */
    public MainFrame(Controller controller) throws HeadlessException {
        super(controller);

        // Initalize controle and informationquarter eager, since some model get
        // used. e.g. NavTreeModel which is built in controlquarter
        controlQuarter = new ControlQuarter(getController());
        informationQuarter = new InformationQuarter(controlQuarter,
            getController());
    }

    /**
     * Builds the UI
     */
    public void buildUI() {
        if (uiComponent == null) {
            // Initalize components
            initComponents();
        }

        FormLayout layout = new FormLayout("fill:pref:grow",
            "pref, 4dlu, fill:0:grow, 1dlu, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 2dlu, 2dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(toolbar.getUIComponent(), cc.xy(1, 1));
        builder.add(mainPane, cc.xy(1, 3));
        builder.add(statusBar.getUIComponent(), cc.xy(1, 5));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setBackground(Color.white);
        uiComponent.setResizable(true);

        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("mainframe.x", 100), prefs.getInt(
            "mainframe.y", 100));

        mainPane.setContinuousLayout(true);
        mainPane.setResizeWeight(0.3);

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("mainframe.width", 950);
        int height = prefs.getInt("mainframe.height", 630);
        uiComponent.setSize(width, height);
        // uiComponent.setSize(950, 630);

        // Now set divider location
        int defaultDividerLocation = (int) ((mainPane.getWidth() - mainPane
            .getDividerSize()) / 3.4);
        mainPane.setDividerLocation(getController().getPreferences().getInt(
            "mainframe.dividerlocation", defaultDividerLocation));

        if (prefs.getBoolean("mainframe.maximized", false)) {
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // everything is decided in window listener
        uiComponent.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // add window listener, checks if exit is needed on pressing X
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                boolean quitOnX = getController().getPreferences().getBoolean(
                    "quitonx", false);
                if (quitOnX || !OSUtil.isSystraySupported()) {
                    // Quit if quit onx is active or not running with system
                    // tray
                    getController().exit(0);
                } else {
                    uiComponent.setVisible(false);
                }
            }
        });

    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        log()
            .debug(
                "Screen resolution: "
                    + Toolkit.getDefaultToolkit().getScreenSize());

        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.POWERFOLDER_IMAGE);
        // TODO: Maybe own theme: uiComponent.setUndecorated(true);

        mainPane = new UIFSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlQuarter
            .getUIComponent(), informationQuarter.getUIComponent());
        mainPane.setBorder(Borders.EMPTY_BORDER);
        mainPane.setDividerSize(6);
        //mainPane.setOneTouchExpandable(true);

        controlQuarter.setSelected(controlQuarter.getNavigationTreeModel()
            .getRootNode());

        // Create toolbar
        toolbar = new Toolbar(getController());
        
        statusBar = new StatusBar(getController());

        updateTitle();
    }

    /**
     * Updates the title
     */
    public void updateTitle() {

        String title = "PowerFolder v" + Controller.PROGRAM_VERSION;
        if (getController().isVerbose()) {
            // Append in front of programm name in verbose mode
            title = getController().getMySelf().getNick() + " | " + title;
        } else {
            // Otherwise append nick at end
            title += " | " + getController().getMySelf().getNick();
        }

        if (getController().isVerbose()
            && getController().getBuildTime() != null)
        {
            title += " | build: " + getController().getBuildTime();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (cal.get(Calendar.DAY_OF_MONTH) == 21
            && cal.get(Calendar.MONTH) == 2)
        {
            title += " | Happy birthday archi !";
        }
        uiComponent.setTitle(title);
    }

    /**
     * @return the ui panel of the mainframe.
     */
    public JFrame getUIComponent() {
        return uiComponent;
    }

    /**
     * Stores all current window valus.
     */
    public void storeValues() {
        // Store main window preferences
        Preferences prefs = getController().getPreferences();

        if ((getUIComponent().getExtendedState() & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH)
        {
            prefs.putInt("mainframe.x", getUIComponent().getX());
            prefs.putInt("mainframe.width", getUIComponent().getWidth());
            prefs.putInt("mainframe.y", getUIComponent().getY());
            prefs.putInt("mainframe.height", getUIComponent().getHeight());
            prefs.putBoolean("mainframe.maximized", false);
        } else {
            prefs.putBoolean("mainframe.maximized", true);
        }
        prefs
            .putInt("mainframe.dividerlocation", mainPane.getDividerLocation());
    }

    /*
     * Exposing ***************************************************************
     */

    ControlQuarter getControlQuarter() {
        return controlQuarter;
    }

    InformationQuarter getInformationQuarter() {
        return informationQuarter;
    }
}