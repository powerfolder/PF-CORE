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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowStateListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.RootPaneUI;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
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

    public static final int MIN_HEIGHT = 300;
    public static final int MIN_WIDTH = PreferencesEntry.MAIN_FRAME_WIDTH
        .getDefaultValueInt();

    /**
     * The width of the main tabbed pane when in NORMAL state
     */
    private int normalWidth;

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

        controller.getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

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

        final Controller c = getController();
        int width = PreferencesEntry.MAIN_FRAME_WIDTH.getValueInt(c);
        int height = PreferencesEntry.MAIN_FRAME_HEIGHT.getValueInt(c);
        if (width < MIN_WIDTH) {
            width = MIN_WIDTH;
        }
        if (height < MIN_HEIGHT) {
            height = MIN_HEIGHT;
        }
        uiComponent.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        uiComponent.setSize(width, height);
        uiComponent.setPreferredSize(new Dimension(width, height));

        // Pack elements
        uiComponent.pack();
        normalWidth = uiComponent.getWidth();

        // Initial top-left corner
        int mainX = PreferencesEntry.MAIN_FRAME_X.getValueInt(c);
        if (mainX == PreferencesEntry.MAIN_FRAME_X.getDefaultValueInt()
            && INLINE_INFO_LEFT == PreferencesEntry.INLINE_INFO_MODE
                .getValueInt(getController()))
        {
            mainX = Toolkit.getDefaultToolkit().getScreenSize().width - mainX
                - uiComponent.getWidth();
        }
        int mainY = PreferencesEntry.MAIN_FRAME_Y.getValueInt(c);
        uiComponent.setLocation(mainX, mainY);

        relocateIfNecessary();

        // everything is decided in window listener
        uiComponent
            .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        configureInlineInfo();

        uiComponent.addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                if (e.getNewState() == Frame.NORMAL) {
                    if (shouldShowInfoInline() && isShowingInfoInline()) {
                        closeInlineInfoPanel();
                    }
                }
                if (e.getNewState() == Frame.MAXIMIZED_BOTH) {
                    if (shouldShowInfoInline() && !isShowingInfoInline()) {
                        // Prevent full screen mode without info inline
                        uiComponent.setExtendedState(Frame.NORMAL);
                    }
                }
            }
        });

        // add window listener, checks if exit is needed on pressing X
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (OSUtil.isSystraySupported()) {
                    handleExitFirstRequest();
                    boolean quitOnX = PreferencesEntry.QUIT_ON_X
                        .getValueBoolean(c);
                    if (quitOnX) {
                        exitProgram();
                    } else if (!OSUtil.isMacOS()) {
                        getUIController().hideChildPanels();
                        uiComponent.setVisible(false);
                    } else {
                        uiComponent.setState(Frame.ICONIFIED);
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
                boolean minToSysTray = PreferencesEntry.MIN_TO_SYS_TRAY
                    .getValueBoolean(c) && !OSUtil.isMacOS();
                if (minToSysTray) {
                    getUIController().hideChildPanels();
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
                        c.tryToExit(0);
                    }
                }.start();
            }
        });

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
            }

            public void windowLostFocus(WindowEvent e) {
                // Nothing to do here.
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
        Controller c = getController();

        if ((uiComponent.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
        {
            // PreferencesEntry.MAIN_FRAME_MAXIMIZED.setValue(c, true);
        } else {
            // PreferencesEntry.MAIN_FRAME_MAXIMIZED.setValue(c, false);

            PreferencesEntry.MAIN_FRAME_X.setValue(c, uiComponent.getX());
            PreferencesEntry.MAIN_FRAME_Y.setValue(c, uiComponent.getY());

            // If info is inline and info is showing, do not store width because
            // info will not show at start up and the frame will be W-I-D-E.
            if (uiComponent.getWidth() > 0
                && (PreferencesEntry.INLINE_INFO_MODE.getValueInt(c) == INLINE_INFO_FREE || inlineInfoPanel == null))
            {
                PreferencesEntry.MAIN_FRAME_WIDTH.setValue(c, uiComponent
                    .getWidth());
            }
            if (uiComponent.getHeight() > 0) {
                PreferencesEntry.MAIN_FRAME_HEIGHT.setValue(c, uiComponent
                    .getHeight());
            }

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
     * Shows the folders tab.
     */
    public void showFoldersTab() {
        mainTabbedPane.setActiveTab(MainTabbedPane.FOLDERS_INDEX);
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

    public void showInlineInfoPanel(JPanel panel, String title) {
        // Fix Synthetica maximization, otherwise it covers the task
        // bar. See
        // http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
        RootPaneUI ui = uiComponent.getRootPane().getUI();
        if (ui instanceof SyntheticaRootPaneUI) {
            ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
        }

        int inline = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController());
        if (!isShowingInfoInline()) {
            normalWidth = uiComponent.getWidth();
        } else if (inline == INLINE_INFO_LEFT) {
            normalWidth = uiComponent.getWidth() - split.getDividerLocation();
        } else if (inline == INLINE_INFO_RIGHT) {
            normalWidth = split.getDividerLocation();
        }

        inlineInfoPanel = panel;
        inlineInfoLabel.setText(title);

        // Always maximize to avoid weird stuff.
        uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        configureInlineInfo();
    }

    private void closeInlineInfoPanel() {
        inlineInfoPanel = null;
        configureInlineInfo();
        uiComponent.setExtendedState(Frame.NORMAL);
    }

    public boolean shouldShowInfoInline() {
        int inline = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController());
        return inline != INLINE_INFO_FREE;
    }

    public boolean isShowingInfoInline() {
        return inlineInfoPanel != null;
    }

    private void configureInlineInfo() {
        int inline = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController());
        boolean displaying = isShowingInfoInline();
        inlineInfoCloseButton.setVisible(inline != INLINE_INFO_FREE
            && displaying);

        if (inline != INLINE_INFO_FREE && displaying) {
            // Make sure the info inline panel does not take the full width
            // and hiding the main tabbed pane
            inlineInfoPanel.setSize(new Dimension(inlineInfoPanel
                .getMinimumSize().width, inlineInfoPanel.getHeight()));

            centralPanel.removeAll();
            if (inline == INLINE_INFO_LEFT) {
                split.setLeftComponent(inlineInfoPanel);
                split.setRightComponent(mainTabbedPane.getUIComponent());
            } else {
                split.setLeftComponent(mainTabbedPane.getUIComponent());
                split.setRightComponent(inlineInfoPanel);
            }

            final int dividerLocation;

            if (inline == INLINE_INFO_LEFT) {
                dividerLocation = uiComponent.getWidth() - normalWidth;
            } else {
                dividerLocation = normalWidth;
            }

            split.setDividerLocation(dividerLocation);
            centralPanel.add(split, BorderLayout.CENTER);

            // No clue why this have to be done later.
            // However if not this change does not come thru
            // on the first time the inline component/splitpane is shown.
            UIUtil.invokeLaterInEDT(new Runnable() {
                public void run() {
                    split.setDividerLocation(dividerLocation);
                }
            });
        } else {
            // Splitpane place holders
            split.setLeftComponent(new JPanel());
            split.setRightComponent(new JPanel());

            centralPanel.removeAll();
            centralPanel.add(mainTabbedPane.getUIComponent(),
                BorderLayout.CENTER);
            inlineInfoPanel = null;
            inlineInfoLabel.setText("");
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
        GraphicsEnvironment ge = GraphicsEnvironment
            .getLocalGraphicsEnvironment();
        if (ge.getScreenDevices().length != 1) {
            // TODO: Relocate on any screen
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

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyFolderRepositoryListener implements
            FolderRepositoryListener {

        // If showing the inline panel and the folder has been removed, 
        // close the inline panel.
        public void folderRemoved(FolderRepositoryEvent e) {
            if (isShowingInfoInline()) {
                closeInlineInfoPanel();
            }
        }

        public void folderCreated(FolderRepositoryEvent e) {
            // Don't care.
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            // Don't care.
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            // Don't care.
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == inlineInfoCloseButton) {
                closeInlineInfoPanel();
            }
        }
    }
}