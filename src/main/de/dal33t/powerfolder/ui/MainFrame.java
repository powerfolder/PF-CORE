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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.plaf.RootPaneUI;
import java.awt.Color;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

/**
 * Powerfoldes gui mainframe
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {

    private JFrame uiComponent;

    private MainTabbedPane mainTabbedPane;

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
            "pref, 1dlu, fill:0:grow, 1dlu, pref, 0dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 2dlu, 2dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Icons.LOGO400UI, SwingConstants.LEFT), cc.xy(1, 1));
        builder.add(mainTabbedPane.getUIComponent(), cc.xy(1, 3));
        builder.add(statusBar.getUIComponent(), cc.xy(1, 5));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setBackground(Color.white);
        uiComponent.setResizable(true);

        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("mainframe4.x", 100), prefs.getInt(
            "mainframe4.y", 100));

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("mainframe4.width", 500);
        int height = prefs.getInt("mainframe4.height", 600);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        uiComponent.setSize(width, height);

        if (prefs.getBoolean("mainframe4.maximized", false)) {
            // Fix Synthetica maximization, otherwise it covers the task bar.
            // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
            RootPaneUI ui = uiComponent.getRootPane().getUI();
            if (ui instanceof SyntheticaRootPaneUI) {
                ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
            }
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

        mainTabbedPane = new MainTabbedPane(getController());

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
            prefs.putBoolean("mainframe4.maximized", true);
        } else {
            prefs.putInt("mainframe4.x", uiComponent.getX());
            if (uiComponent.getWidth() > 0) {
                prefs.putInt("mainframe4.width", uiComponent.getWidth());
            }
            prefs.putInt("mainframe4.y", uiComponent.getY());
            if (uiComponent.getHeight() > 0) {
                prefs.putInt("mainframe4.height", uiComponent.getHeight());
            }
            prefs.putBoolean("mainframe4.maximized", false);
        }
    }

    /*
     * Exposing ***************************************************************
     */

    InformationQuarter getInformationQuarter() {
        return null;
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

    private void configureSyncNowAction() {
//        getUIController().getSyncAllFoldersAction().setEnabled(
//            getController().getFolderRepository().getFolders().length != 0);
    }

    private class RepositoryListener implements FolderRepositoryListener {
        public boolean fireInEventDispatchThread() {
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