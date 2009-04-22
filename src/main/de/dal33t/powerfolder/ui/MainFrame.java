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
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.RootPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

/**
 * Powerfoldes gui mainframe
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {

    private final AtomicBoolean controlKeyDown = new AtomicBoolean();
    private final AtomicInteger oldX = new AtomicInteger();
    private final AtomicInteger oldY = new AtomicInteger();

    private JFrame uiComponent;

    private MainTabbedPane mainTabbedPane;

    /**
     * The menu bar that handles F5 for sync, etc.
     * This is not visible in the GUI.
     */
    private JMenuBar menuBar;

    /**
     * The status bar on the lower side of the screen.
     */
    private StatusBar statusBar;

    /**
     * @param controller
     *            the controller.
     * @throws HeadlessException
     */
    public MainFrame(Controller controller) throws HeadlessException {
        super(controller);

        // Need to do this NOW because everything must be built before anything
        // affects it, like tab icons.
        buildUI();
    }

    /**
     * Builds the UI
     */
    private void buildUI() {
        initComponents();

        FormLayout layout = new FormLayout("fill:pref:grow",
            "0dlu, pref, 1dlu, fill:0:grow, 1dlu, pref");
          // menu  head        body               footer
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("3dlu, 2dlu, 2dlu, 2dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(menuBar, cc.xy(1, 1));
        builder.add(new JLabel(Icons.getIconById(Icons.LOGO400UI), SwingConstants.LEFT), cc.xy(1, 2));
        builder.add(mainTabbedPane.getUIComponent(), cc.xy(1, 4));
        builder.add(statusBar.getUIComponent(), cc.xy(1, 6));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setBackground(Color.white);
        uiComponent.setResizable(true);

        Preferences prefs = getController().getPreferences();
        int width = prefs.getInt("mainframe4.width", 350);
        int height = prefs.getInt("mainframe4.height", 600);
        // Initial top-right corner
        uiComponent.setLocation(prefs.getInt("mainframe4.x",
                Toolkit.getDefaultToolkit().getScreenSize().width - 50 - width),
                prefs.getInt("mainframe4.y", 50));

        oldX.set(uiComponent.getX());
        oldY.set(uiComponent.getY());

        // Pack elements
        uiComponent.pack();

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
                if (OSUtil.isSystraySupported()) {
                    handleExitFirstRequest();
                    boolean quitOnX = PreferencesEntry.QUIT_ON_X
                            .getValueBoolean(getController());
                    if (quitOnX) {
                        exitProgram();
                    } else {
                        getUIController().hideChildPanels();
                        uiComponent.setVisible(false);
                    }
                } else {
                    // Quit if systray is not Supported by OS.
                    exitProgram();
                }
			}

            /**
             * Hide other frames when main frame gets minimized.
             * 
             * @param e
             */
            public void windowIconified(WindowEvent e) {
                getUIController().hideChildPanels();
                super.windowIconified(e);
            }

            /**
             * Shuts down the program
             */
            private void exitProgram() {
                uiComponent.setVisible(false);
                uiComponent.dispose();
                new Thread("Close PowerFolder Thread") {
                    @Override
                    public void run() {
                        getController().tryToExit(0);
                    }
                }.start();
            }
        });

        uiComponent.addComponentListener(new MyComponentAdapter());
        uiComponent.addMouseMotionListener(new MyMouseAdapter());
    }
    
    /**
     * Asks user about exit behavior of the program when the program is used for
     * the first time
     */
    private void handleExitFirstRequest() {
        boolean askForQuitOnX = PreferencesEntry.ASK_FOR_QUIT_ON_X
            .getValueBoolean(getController());
        if (askForQuitOnX) {
            // Prompt for personal message.
            String[] options = {
                Translation
                    .getTranslation("dialog.ask_for_quit_on_x.Minimize_button"),
                Translation
                    .getTranslation("dialog.ask_for_quit_on_x.Exit_button")};

            NeverAskAgainResponse response = DialogFactory.genericDialog(
                getController(), Translation
                    .getTranslation("dialog.ask_for_quit_on_x.title"),
                Translation.getTranslation("dialog.ask_for_quit_on_x.text"),
                options, 0, GenericDialogType.QUESTION, Translation
                    .getTranslation("general.neverAskAgain"));

            if (response.getButtonIndex() == 1) { // == Exit
                PreferencesEntry.QUIT_ON_X.setValue(getController(), true);
            } else {
                PreferencesEntry.QUIT_ON_X.setValue(getController(), false);
            }

            if (response.isNeverAskAgain()) {
                // don't ask me again
                PreferencesEntry.ASK_FOR_QUIT_ON_X.setValue(getController(),
                    false);
            }
        }
    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        logFine("Screen resolution: "
            + Toolkit.getDefaultToolkit().getScreenSize());

        uiComponent = new JFrame();
        if (uiComponent.isAlwaysOnTopSupported()&&
                PreferencesEntry.MAIN_ALWAYS_ON_TOP.getValueBoolean(
                        getController())) {
            uiComponent.setAlwaysOnTop(true);
        }
        uiComponent.addWindowFocusListener(new WindowFocusListener() {

            public void windowGainedFocus(WindowEvent e) {
                getUIController().setActiveFrame(UIController.MAIN_FRAME_ID);

                if (Constants.OPACITY_SUPPORTED) {
                    UIUtil.applyTranslucency(uiComponent, 1.0f);
                }
            }

            public void windowLostFocus(WindowEvent e) {
                if (Constants.OPACITY_SUPPORTED
                        && PreferencesEntry.TRANSLUCENT_MAIN_FRAME
                        .getValueBoolean(getController())) {
                    // Translucency is 1 - opacity.
                    float opacity = 1.0f - PreferencesEntry
                            .TRANSLUCENT_PERCENTAGE.getValueInt(getController())
                            /  100.0f;
                    UIUtil.applyTranslucency(uiComponent, opacity);
                }
            }
        });
        uiComponent.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));

        createMenuBar();

        mainTabbedPane = new MainTabbedPane(getController());

        statusBar = new StatusBar(getController());

        updateTitle();
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();
        JMenuItem syncAllMenuItem = new JMenuItem(new SyncAllFoldersAction(getController()));
        menuBar.add(syncAllMenuItem);
    }

    /**
     * Updates the title
     */
    public void updateTitle() {

        StringBuilder title = new StringBuilder();
        
        String appName = Translation.getTranslation("general.application.name");
        // Urg
        if (StringUtils.isEmpty(appName) || appName.startsWith("- ")) {
            appName = "PowerFolder";
        }
        String initial = appName + " v" + Controller.PROGRAM_VERSION;
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
     * Add a change listener to the main tabbed pane selection.
     *
     * @param l
     */
    public void addTabbedPaneChangeListener(ChangeListener l) {
        mainTabbedPane.addTabbedPaneChangeListener(l);
    }

    /**
     * Remove a change listener from the main tabbed pane.
     *
     * @param l
     */
    public void removeTabbedPaneChangeListener(ChangeListener l) {
        mainTabbedPane.removeTabbedPaneChangeListener(l);
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
     * Hide the Online Storage lines in the home tab.
     */
    public void hideOSLines() {
        mainTabbedPane.hideOSLines();
    }

    /**
     * Set the Icon for the home tab.
     *
     * @param homeIcon
     */
    public void setHomeTabIcon(Icon homeIcon) {
        mainTabbedPane.setHomeIcon(homeIcon);
    }

    /**
     * Set the Icon for the folders tab.
     *
     * @param homeIcon
     */
    public void setFoldersTabIcon(Icon foldersIcon) {
        mainTabbedPane.setFoldersIcon(foldersIcon);
    }

    /**
     * Set the Icon for the computers tab.
     *
     * @param homeIcon
     */
    public void setComputersTabIcon(Icon computersIcon) {
        mainTabbedPane.setComputersIcon(computersIcon);
    }

    /**
     * Get the selected main tab index.
     *
     * @return
     */
    public int getSelectedMainTabIndex() {
        return mainTabbedPane.getSelectedTabIndex();
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Listen for control key, to use in MyComponentAdapter.
     * // todo - This is really ugly. Does any one know a better way of
     * detecting the contol key?
     */
    private class MyMouseAdapter extends MouseAdapter {

        public void mouseDragged(MouseEvent e) {
            detectControlKey(e);
        }

        public void mouseClicked(MouseEvent e) {
            detectControlKey(e);
        }

        public void mouseMoved(MouseEvent e) {
            detectControlKey(e);
        }

        private void detectControlKey(MouseEvent e) {
            controlKeyDown.set((e.getModifiers() & InputEvent.CTRL_MASK) != 0);
        }
    }

    /**
     * Listen to movement of the main frame.
     */
    private class MyComponentAdapter extends ComponentAdapter {

        /**
         * Calculate the change in movement and notify the controller.
         *
         * @param e
         */
        public void componentMoved(ComponentEvent e) {
            synchronized (oldX) {
                int newX = uiComponent.getX();
                int newY = uiComponent.getY();
                int ox = oldX.getAndSet(uiComponent.getX());
                int oy = oldY.getAndSet(uiComponent.getY());
                int diffX = newX - ox;
                int diffY = newY - oy;
                getUIController().mainFrameMoved(controlKeyDown.get(), diffX,
                        diffY);
            }
        }
    }
}