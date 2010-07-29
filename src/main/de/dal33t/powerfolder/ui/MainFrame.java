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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.RootPaneUI;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

/**
 * Powerfoldes gui mainframe
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {

    public static final int INLINE_INFO_FREE = 0;
    public static final int INLINE_INFO_LEFT = 1;
    public static final int INLINE_INFO_RIGHT = 2;

    private final AtomicBoolean controlKeyDown = new AtomicBoolean();
    private final AtomicInteger oldX = new AtomicInteger();
    private final AtomicInteger oldY = new AtomicInteger();
    private boolean packWidthNext;

    private JFrame uiComponent;
    private JLabel logoLabel;
    private JPanel centralPanel;
    private MainTabbedPane mainTabbedPane;
    private JPanel inlineInfoPanel;
    private JLabel inlineInfoLabel;
    private JButton inlineInfoCloseButton;
    private JSplitPane split;

    /**
     * The menu bar that handles F5 for sync, etc. This is not visible in the
     * GUI.
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

        FormLayout layout = new FormLayout("fill:pref:grow, pref, 3dlu, pref",
            "0dlu, pref, 1dlu, fill:0:grow, 1dlu, pref");
        // menu head body footer
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("3dlu, 0, 2dlu, 0"));

        CellConstraints cc = new CellConstraints();

        builder.add(menuBar, cc.xyw(1, 1, 4));

        builder.add(logoLabel, cc.xy(1, 2));
        builder.add(inlineInfoLabel, cc.xy(2, 2, CellConstraints.DEFAULT,
            CellConstraints.BOTTOM));
        builder.add(inlineInfoCloseButton, cc.xy(4, 2, CellConstraints.DEFAULT,
            CellConstraints.BOTTOM));

        builder.add(centralPanel, cc.xyw(1, 4, 4));
        builder.add(statusBar.getUIComponent(), cc.xyw(1, 6, 4));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setOneTouchExpandable(false);

        uiComponent.getContentPane().add(
            GradientPanel.create(builder.getPanel()));
        uiComponent.setBackground(Color.white);
        uiComponent.setResizable(true);

        // Pack elements
        uiComponent.pack();

        Preferences prefs = getController().getPreferences();
        int width = prefs.getInt("mainframe4.width", 350);
        int height = prefs.getInt("mainframe4.height", 700);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        uiComponent.setSize(width, height);

        // Initial top-left corner
        uiComponent.setLocation(prefs.getInt("mainframe4.x", 50), prefs.getInt(
            "mainframe4.y", 50));

        relocateIfNecessary();

        oldX.set(uiComponent.getX());
        oldY.set(uiComponent.getY());

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
                boolean minToSysTray = PreferencesEntry.MIN_TO_SYS_TRAY
                    .getValueBoolean(getController());
                if (minToSysTray) {
                    uiComponent.setVisible(false);
                } else {
                    super.windowIconified(e);
                }
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

        configureInlineInfo();
    }

    /**
     * Must do this AFTER mainFrame is constructed else info frame may nudge and
     * not have a reference to mainframe ui component.
     */
    public void attachListeners() {
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
        if (uiComponent.isAlwaysOnTopSupported()
            && PreferencesEntry.MAIN_ALWAYS_ON_TOP
                .getValueBoolean(getController()))
        {
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
                        .getValueBoolean(getController()))
                {
                    // Translucency is 1 - opacity.
                    float opacity = 1.0f - PreferencesEntry.TRANSLUCENT_PERCENTAGE
                        .getValueInt(getController()) / 100.0f;
                    UIUtil.applyTranslucency(uiComponent, opacity);
                }
            }
        });
        uiComponent.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));

        logoLabel = new JLabel();
        logoLabel.setIcon(Icons.getIconById(Icons.LOGO400UI));
        logoLabel.setHorizontalAlignment(SwingConstants.LEFT);

        createMenuBar();

        centralPanel = new JPanel(new BorderLayout(0, 0));

        mainTabbedPane = new MainTabbedPane(getController());

        statusBar = new StatusBar(getController());

        updateTitle();

        inlineInfoCloseButton = new JButton3Icons(Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        inlineInfoCloseButton.setToolTipText(Translation
            .getTranslation("main_frame.inline_info_close.tip"));
        inlineInfoCloseButton.addActionListener(new MyActionListener());
        inlineInfoCloseButton.setContentAreaFilled(false);

        inlineInfoLabel = new JLabel();
    }

    /**
     * Menu is not visible, it just holds the SyncAll action, so it reacts to
     * key press.
     */
    private void createMenuBar() {
        menuBar = new JMenuBar();
        JMenuItem syncAllMenuItem = new JMenuItem(new SyncAllFoldersAction(
            getController()));
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
            // If info is inline and info is showing, do not store width because
            // info will not show at start up and the frame will be W-I-D-E.
            if (uiComponent.getWidth() > 0
                && (PreferencesEntry.INLINE_INFO_MODE
                    .getValueInt(getController()) == INLINE_INFO_FREE || inlineInfoPanel == null))
            {
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
     * @return true, if application is currently minimized
     */
    public boolean isMaximized() {
        return (uiComponent.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
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
     * Set the Icon for the sttus tab.
     * 
     * @param statusIcon
     */
    public void setStatusTabIcon(Icon statusIcon) {
        mainTabbedPane.setStatusIcon(statusIcon);
    }

    /**
     * Set the Icon for the folders tab.
     * 
     * @param foldersIcon
     */
    public void setFoldersTabIcon(Icon foldersIcon) {
        mainTabbedPane.setFoldersIcon(foldersIcon);
    }

    /**
     * Set the Icon for the computers tab.
     * 
     * @param computersIcon
     */
    public void setComputersTabIcon(Icon computersIcon) {
        mainTabbedPane.setComputersIcon(computersIcon);
    }

    /**
     * @return the selected main tab index.
     */
    public int getSelectedMainTabIndex() {
        return mainTabbedPane.getSelectedTabIndex();
    }

    /**
     * Show the pending messages button in the status bar.
     * 
     * @param show
     */
    public void showPendingMessages(boolean show) {
        statusBar.showPendingMessages(show);
    }

    public void setNetworkingModeStatus(NetworkingMode networkingMode) {
        statusBar.setNetworkingModeStatus(networkingMode);
    }

    public void showInlineInfoPanel(final JPanel panel, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                packWidthNext = inlineInfoPanel == null;
                inlineInfoPanel = panel;
                inlineInfoLabel.setText(title);
                configureInlineInfo();
            }
        });
    }

    private void closeInlineInfoPanel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                packWidthNext = true;
                inlineInfoPanel = null;
                configureInlineInfo();
            }
        });
    }

    private void configureInlineInfo() {
        boolean wasMaximized = isMaximized();
        int inline = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController());
        boolean displaying = inlineInfoPanel != null;
        inlineInfoCloseButton.setVisible(inline != INLINE_INFO_FREE
            && displaying);

        int originalX = uiComponent.getX();
        int originalWidth = uiComponent.getWidth();

        if (inline != INLINE_INFO_FREE && displaying) {
            centralPanel.removeAll();
            int currentDividerLocation = split.getDividerLocation();
            if (inline == INLINE_INFO_LEFT) {
                split.setLeftComponent(inlineInfoPanel);
                split.setRightComponent(mainTabbedPane.getUIComponent());
            } else {
                split.setLeftComponent(mainTabbedPane.getUIComponent());
                split.setRightComponent(inlineInfoPanel);
            }
            centralPanel.add(split, BorderLayout.CENTER);
            if (packWidthNext) {
                packWidth(wasMaximized);
            }
            int targetWidth;
            int mainTabbedWidth = (int) mainTabbedPane.getUIComponent()
                .getPreferredSize().getWidth();
            if (inline == INLINE_INFO_LEFT) {
                int uiWidth = uiComponent.getWidth();
                targetWidth = uiWidth
                    - Math.max(uiWidth - currentDividerLocation,
                        mainTabbedWidth);
                if (targetWidth <= 0) {
                    targetWidth = uiWidth - mainTabbedWidth;
                }

                // Move so UI right side to original position.
                uiComponent.setLocation(originalX - uiComponent.getWidth()
                    + originalWidth, uiComponent.getY());
            } else {
                targetWidth = Math.max(currentDividerLocation, mainTabbedWidth);
                if (targetWidth <= 0) {
                    targetWidth = mainTabbedWidth;
                }

                // Move so UI left side to original position.
                uiComponent.setLocation(originalX, uiComponent.getY());
            }
            split.setDividerLocation(targetWidth);
        } else {
            // Splitpane place holders
            split.setLeftComponent(new JPanel());
            split.setRightComponent(new JPanel());

            centralPanel.removeAll();
            centralPanel.add(mainTabbedPane.getUIComponent(),
                BorderLayout.CENTER);
            inlineInfoPanel = null;
            inlineInfoLabel.setText("");
            if (packWidthNext) {
                packWidth(wasMaximized);
            }
            if (inline == INLINE_INFO_LEFT) {
                // Collapsing of left inline.
                // Move so UI right side to original position.
                uiComponent.setLocation(originalX - uiComponent.getWidth()
                    + originalWidth, uiComponent.getY());
            }
        }

        relocateIfNecessary();
    }

    /**
     * Did we move the UI outside the screen boundary?
     */
    private void relocateIfNecessary() {
        if (isIconified() || isMaximized()) {
            // Don't care.
            return;
        }
        // Now adjust for off-screen problems.
        int uiY = uiComponent.getY();
        int uiX = uiComponent.getX();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int uiWidth = uiComponent.getWidth();
        if (uiX < 0) {
            uiComponent.setLocation(0, uiY);
        }
        if (uiX + uiWidth > screenWidth) {
            uiComponent.setLocation(screenWidth - uiWidth, uiY);
        }
        if (uiY < 0 || uiY > (int) screenSize.getHeight()) {
            uiComponent.setLocation(uiComponent.getX(), 0);
        }
    }

    private void packWidth(boolean wasMaximized) {
        if (!wasMaximized) {
            uiComponent.setSize(new Dimension(
                uiComponent.getMinimumSize().width, uiComponent.getHeight()));
        }
        packWidthNext = false;
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    /**
     * Listen for control key, to use in MyComponentAdapter. // todo - This is
     * really ugly. Does any one know a better way of detecting the contol key?
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

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final Object source = e.getSource();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (source == inlineInfoCloseButton) {
                        closeInlineInfoPanel();
                    }
                }
            });
        }
    }
}