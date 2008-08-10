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
package de.dal33t.powerfolder.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.component.UIFSplitPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.StartPanel;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.ui.action.HasDetailsPanel;
import de.dal33t.powerfolder.ui.folder.FolderPanel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.ui.transfer.DownloadsPanel;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Powerfoldes gui mainframe
 * <p>
 * TODO #278 Throw away, I think it's better to start with MainWindow from
 * scratch.
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
            "pref, 4dlu, fill:0:grow, 1dlu, pref, 0dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 2dlu, 2dlu"));
        CellConstraints cc = new CellConstraints();

        // This menu bar is not displayed (0dlu).
        // It is only used to trigger Actions by accelerator keys.
        JMenuBar mb = new JMenuBar();
        JMenuItem mi = new JMenuItem(getUIController()
            .getSyncAllFoldersAction());
        mb.add(mi);
        mi = new JMenuItem(new MySyncFolderAction());
        mb.add(mi);
        mi = new JMenuItem(new MyCleanupAction());
        mb.add(mi);
        mi = new JMenuItem(new MyDetailsAction());
        mb.add(mi);

        builder.add(toolbar.getUIComponent(), cc.xy(1, 1));
        builder.add(mainPane, cc.xy(1, 3));
        builder.add(statusBar.getUIComponent(), cc.xy(1, 5));
        builder.add(mb, cc.xy(1, 6));

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

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width;
        int height;
        int divider;
        if (screen.width <= 1024) {
            width = prefs.getInt("mainframe.width", 950);
            height = prefs.getInt("mainframe.height", 630);
            divider = 250;
        } else {
            width = prefs.getInt("mainframe.width", 1100);
            height = prefs.getInt("mainframe.height", 730);
            divider = 300;
        }
        uiComponent.setSize(width, height);
        // uiComponent.setSize(950, 630);

        // Now set divider location
        mainPane.setDividerLocation(getController().getPreferences().getInt(
            "mainframe.dividerlocation", divider));

        if (prefs.getBoolean("mainframe.maximized", false)) {
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // everything is decided in window listener
        uiComponent
            .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // add window listener, checks if exit is needed on pressing X
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                boolean quitOnX = PreferencesEntry.QUIT_ON_X
                    .getValueBoolean(getController());
                if (quitOnX || !OSUtil.isSystraySupported()) {
                    // Quit if quit onx is active or not running with system
                    // tray
                    uiComponent.setVisible(false);
                    uiComponent.dispose();
                    new Thread("Close PowerFolder Thread") {
                        @Override
                        public void run() {
                            getController().tryToExit(0);
                        }
                    }.start();
                } else {
                    uiComponent.setVisible(false);
                }
            }
        });

        // Listen to changes in the number of folders in the repository.
        getController().getFolderRepository().addFolderRepositoryListener(
            new RepositoryListener());
        // Ensure we are up to date.
        configureSyncNowAction();

    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        logFine("Screen resolution: "
            + Toolkit.getDefaultToolkit().getScreenSize());

        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.POWERFOLDER_IMAGE);
        // TODO: Maybe own theme: uiComponent.setUndecorated(true);

        mainPane = new UIFSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlQuarter
            .getUIComponent(), informationQuarter.getUIComponent());
        mainPane.setBorder(Borders.EMPTY_BORDER);
        mainPane.setDividerSize(6);
        // mainPane.setOneTouchExpandable(true);

        // Set up the initial selection on the control quarter.
        String startPanelName = PreferencesEntry.START_PANEL
            .getValueString(getController());
        StartPanel startPanel = StartPanel.valueForLegacyName(startPanelName);
        if (startPanel.equals(StartPanel.OVERVIEW)) {
            controlQuarter.selectOverview();
        } else if (startPanel.equals(StartPanel.MY_FOLDERS)) {
            controlQuarter.selectMyFolders();
        } else if (startPanel.equals(StartPanel.DOWNLOADS)) {
            controlQuarter.selectDownloads();
        }

        // Create toolbar
        toolbar = new Toolbar(getController());

        statusBar = new StatusBar(getController());

        updateTitle();
    }

    /**
     * Updates the title
     */
    public void updateTitle() {

        StringBuilder title = new StringBuilder();
        String initial = "PowerFolder v" + Controller.PROGRAM_VERSION;
        if (getController().isVerbose()) {
            // Append in front of programm name in verbose mode
            title.append(getController().getMySelf().getNick() + " | "
                + initial);
        } else {
            // Otherwise append nick at end
            title.append(initial + " | "
                + getController().getMySelf().getNick());
        }

        if (getController().isVerbose()
            && getController().getBuildTime() != null)
        {
            title.append(" | build: " + getController().getBuildTime());
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (cal.get(Calendar.DAY_OF_MONTH) == 21
            && cal.get(Calendar.MONTH) == 2)
        {
            title.append(" | Happy birthday archi !");
        }
        uiComponent.setTitle(title.toString());
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

        if ((uiComponent.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
        {
            prefs.putBoolean("mainframe.maximized", true);
        } else {
            prefs.putInt("mainframe.x", uiComponent.getX());
            prefs.putInt("mainframe.width", uiComponent.getWidth());
            prefs.putInt("mainframe.y", uiComponent.getY());
            prefs.putInt("mainframe.height", uiComponent.getHeight());
            prefs.putBoolean("mainframe.maximized", false);
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

    /**
     * @return true, if application is currently minimized
     */
    public boolean isIconified() {
        return (uiComponent.getExtendedState() & Frame.ICONIFIED) != 0;
    }

    /**
     * Determine if application is currently minimized or hidden (for example,
     * in the systray)
     * 
     * @return true, if application is currently minimized or hidden
     */
    public boolean isIconifiedOrHidden() {
        return isIconified() || !uiComponent.isVisible();
    }

    /**
     * Restore application from its minimized state
     */
    public void deiconify() {
        // Popup whole application
        uiComponent.setVisible(true);
        int state = uiComponent.getExtendedState();
        // Clear the iconified bit
        state &= ~Frame.ICONIFIED;
        // Deiconify the frame
        uiComponent.setExtendedState(state);
    }

    /**
     * Simple class to call controller scanSelectedFolder.
     */
    private class MySyncFolderAction extends AbstractAction {
        MySyncFolderAction() {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_1,
                ActionEvent.ALT_MASK));
        }

        /**
         * Scan selected folder (if any).
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            getController().getUIController().getFolderRepositoryModel()
                .scanSelectedFolder();
        }
    }

    /**
     * Simple class to call cleanup in downloads.
     */
    private class MyCleanupAction extends AbstractAction {
        MyCleanupAction() {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_3,
                ActionEvent.ALT_MASK));
        }

        /**
         * Scan selected folder (if any).
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {

            DownloadsPanel panel = getController().getUIController()
                .getInformationQuarter().getDownloadsPanel();

            if (panel != null) {
                // Ensure this has been fully created before it is called.
                panel.getUIComponent();

                // Clear downloads
                panel.clearDownloads();
            }
        }
    }

    /**
     * Simple action class to call details in the selected information panel (if
     * appropriate).
     */
    private class MyDetailsAction extends AbstractAction {

        MyDetailsAction() {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_4,
                ActionEvent.ALT_MASK));
        }

        /**
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            Object o = getControlQuarter().getSelectedItem();
            if (o instanceof Folder) {
                Folder f = (Folder) o;
                if (f.isPreviewOnly()) {
                    FolderPanel panel = getInformationQuarter()
                        .getPreviewFolderPanel();
                    panel.toggleDetails();
                } else {
                    FolderPanel panel = getInformationQuarter()
                        .getMyFolderPanel();
                    panel.toggleDetails();
                }
            } else if (o instanceof String) {
                String s = (String) o;
                HasDetailsPanel p = null;
                if (s.equals(RootNode.DOWNLOADS_NODE_LABEL)) {
                    p = getInformationQuarter().getDownloadsPanel();
                } else if (s.equals(RootNode.UPLOADS_NODE_LABEL)) {
                    p = getInformationQuarter().getUploadsPanel();
                }
                if (p != null) {
                    p.toggeDetails();
                }
            }
        }
    }

    private void configureSyncNowAction() {
        getUIController().getSyncAllFoldersAction().setEnabled(
            getController().getFolderRepository().getFolders().length != 0);
    }

    private class RepositoryListener implements FolderRepositoryListener {
        public boolean fireInEventDispathThread() {
            return false;
        }

        public void folderCreated(FolderRepositoryEvent e) {
            configureSyncNowAction();
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            configureSyncNowAction();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }
    }
}