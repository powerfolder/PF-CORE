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
import java.awt.event.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.RootPaneUI;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.SilentModeListener;
import de.dal33t.powerfolder.event.SilentModeEvent;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.OpenPreferencesAction;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

/**
 * Powerfolder gui mainframe
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {

    public static final int MIN_HEIGHT = 300;
    public static final int MIN_WIDTH = PreferencesEntry.MAIN_FRAME_WIDTH
        .getDefaultValueInt();
    public static final int MIN_INFO_WIDTH = 500;

    /**
     * The width of the main tabbed pane when in NORMAL state
     */
    private int mainWidth;
    private int infoWidth;

    private JFrame uiComponent;
    private JLabel logoLabel;
    private JPanel centralPanel;
    private MainTabbedPane mainTabbedPane;
    private JPanel inlineInfoPanel;
    private JLabel inlineInfoLabel;
    private JButton inlineInfoCloseButton;
    private JSplitPane split;
    private final AtomicBoolean compactModeActive = new AtomicBoolean();

    private JButton uncompactModeButton;
    private JButton closeButton;
    private JLabel syncTextLabel;
    private JLabel syncDateLabel;
    private JLabel accountLabel;
    private JProgressBar usagePB;
    private ActionLabel openWebInterfaceLabel;
    private ActionLabel pauseResumeLabel;
    private ActionLabel configurationLabel;
    private JLabel compactLogoLabel;

    /**
     * The status bar on the lower edge of the main frame.
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

        initComponents();
        configureUiUncompact();

    }

    private void configureUiCompact() {

        // Hide the title pane in compact mode.
        uiComponent.getRootPane().putClientProperty(
                "Synthetica.titlePane.enabled", Boolean.FALSE);
        uiComponent.getRootPane().updateUI();

        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createUpperCompactSection(), cc.xy(1, 1));
        builder.add(createLowerCompactSection(), cc.xy(1, 2));

//        builder.add(new JLabel(Translation.getTranslation(
//                "main_frame.open_web_interface.text")),
//                cc.xy(1, row));
//        builder.add(openWebInterfaceLabel, cc.xy(3, row));
//        builder.add(pauseResumeLabel, cc.xy(1, row));
//        builder.add(pauseResumeButton, cc.xy(3, row));

        uiComponent.getContentPane().removeAll();
        uiComponent.setMinimumSize(new Dimension(20, 20));
        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setExtendedState(Frame.NORMAL);
        uiComponent.pack();
        uiComponent.setResizable(false);
        relocateIfNecessary();
    }

    private Component createLowerCompactSection() {
        FormLayout layout = new FormLayout("fill:pref:grow, pref",
            "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createLowerLeftCompactSection(), cc.xy(1, 1));
        builder.add(createLowerRightCompactSection(), cc.xy(2, 1));
        
        return builder.getPanel();
    }

    private Component createLowerLeftCompactSection() {
        FormLayout layout = new FormLayout("100dlu",
            "pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(syncTextLabel, cc.xy(1, 1));
        builder.add(syncDateLabel, cc.xy(1, 2));
        builder.add(accountLabel, cc.xy(1, 3));
        builder.add(usagePB, cc.xy(1, 4));

        return builder.getPanel();
    }

    private Component createLowerRightCompactSection() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(openWebInterfaceLabel.getUIComponent(), cc.xy(1, 1));
        builder.add(pauseResumeLabel.getUIComponent(), cc.xy(1, 2));
        builder.add(configurationLabel.getUIComponent(), cc.xy(1, 3));

        return builder.getPanel();
    }

    private Component createUpperCompactSection() {
        FormLayout layout = new FormLayout("fill:pref:grow, pref, pref",
            "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(compactLogoLabel, cc.xy(1, 1));
        builder.add(uncompactModeButton, cc.xy(2, 1));
        builder.add(closeButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    private void configureUiUncompact() {

        // Display the title pane in uncompact mode.
        uiComponent.getRootPane().putClientProperty(
                "Synthetica.titlePane.enabled", Boolean.TRUE);
        uiComponent.getRootPane().updateUI();

        FormLayout layout = new FormLayout("fill:pref:grow, pref, 3dlu, pref",
            "pref, 1dlu, fill:0:grow, 1dlu, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(logoLabel, cc.xyw(1, 1, 4));
        builder.add(inlineInfoLabel,
            cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));
        builder.add(inlineInfoCloseButton,
            cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));

        builder.add(centralPanel, cc.xyw(1, 3, 4));
        builder.add(statusBar.getUIComponent(), cc.xyw(1, 5, 4));

        uiComponent.getContentPane().removeAll();
        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setResizable(true);

        Controller c = getController();
        int mainY = PreferencesEntry.MAIN_FRAME_Y.getValueInt(c);

        int width = PreferencesEntry.MAIN_FRAME_WIDTH.getValueInt(c);
        int height = PreferencesEntry.MAIN_FRAME_HEIGHT.getValueInt(c);
        if (height <= 0) {
            // Default height. 2x the default border from bottom away.
            height = Toolkit.getDefaultToolkit().getScreenSize().height - mainY
                - 2 * Constants.UI_DEFAULT_SCREEN_BORDER;
        }
        if (width < MIN_WIDTH) {
            width = MIN_WIDTH;
        }
        if (height < MIN_HEIGHT) {
            height = MIN_HEIGHT;
        }
        uiComponent.setMinimumSize(new Dimension(width, height));

        // Pack elements
        uiComponent.pack();
        mainWidth = uiComponent.getWidth();
        logFine("Main/Info width: " + mainWidth + " / ?");

        // Initial top-left corner
        int mainX = PreferencesEntry.MAIN_FRAME_X.getValueInt(c);
        uiComponent.setLocation(mainX, mainY);

        relocateIfNecessary();

        configureInlineInfo();

        if (PreferencesEntry.MAIN_FRAME_MAXIMIZED
            .getValueBoolean(getController()))
        {
            if (uiComponent.getRootPane().getUI() instanceof SyntheticaRootPaneUI)
            {
                ((SyntheticaRootPaneUI) uiComponent.getRootPane().getUI())
                    .setMaximizedBounds(uiComponent);
            }
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
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
                getController(),
                Translation.getTranslation("dialog.ask_for_quit_on_x.title"),
                Translation.getTranslation("dialog.ask_for_quit_on_x.text"),
                options, 0, GenericDialogType.QUESTION,
                Translation.getTranslation("general.neverAskAgain"));

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
            + Toolkit.getDefaultToolkit().getScreenSize()
            + " / Width over all monitors: "
            + UIUtil.getScreenWidthAllMonitors());

        uiComponent = new JFrame();
        if (uiComponent.isAlwaysOnTopSupported()
            && PreferencesEntry.MAIN_ALWAYS_ON_TOP
                .getValueBoolean(getController()))
        {
            uiComponent.setAlwaysOnTop(true);
        }
        uiComponent.addWindowFocusListener(new MyWindowFocusListner());
        uiComponent.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));
        uiComponent.setBackground(Color.white);

        MyActionListener myActionListener = new MyActionListener();

        syncTextLabel = new JLabel("sync text");
        syncTextLabel.setIcon(Icons.getIconById(Icons.SYNC_COMPLETE));

        // Sync-complete icon on the right of text, but left aligned.
        syncTextLabel.setComponentOrientation(
                ComponentOrientation.RIGHT_TO_LEFT);
        syncTextLabel.setHorizontalAlignment(SwingConstants.LEFT);

        syncDateLabel = new JLabel("sync date");
        accountLabel = new JLabel("account@powerfolder.com");
        usagePB = new JProgressBar();

        uncompactModeButton = new JButtonMini(
            Icons.getIconById(Icons.UNCOMACT),
            Translation.getTranslation("main_frame.uncompact.tips"));
        uncompactModeButton.addActionListener(myActionListener);

        closeButton = new JButtonMini(
            Icons.getIconById(Icons.CLOSE),
            Translation.getTranslation("main_frame.close.tips"));
        closeButton.addActionListener(myActionListener);

        compactLogoLabel = new JLabel(Icons.getIconById(Icons.LOGO400UI));
        MyHybridMouseListener hybridMouseListener = new MyHybridMouseListener();
        compactLogoLabel.addMouseMotionListener(hybridMouseListener);
        compactLogoLabel.addMouseListener(hybridMouseListener);

        openWebInterfaceLabel = new ActionLabel(getController(),
                new MyOpenWebInterfaceAction(getController()));
        pauseResumeLabel = new ActionLabel(getController(),
                new MyPauseResumeAction(getController()));
        configurationLabel = new ActionLabel(getController(),
                new OpenPreferencesAction(getController()));

        // add window listener, checks if exit is needed on pressing X
        MyWindowListener myWindowListener = new MyWindowListener();
        uiComponent.addWindowListener(myWindowListener);
        uiComponent.addWindowStateListener(myWindowListener);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setOneTouchExpandable(false);

        // everything is decided in window listener
        uiComponent
            .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        logoLabel = new JLabel();
        logoLabel.setIcon(Icons.getIconById(Icons.LOGO400UI));
        logoLabel.setHorizontalAlignment(SwingConstants.LEFT);

        centralPanel = new JPanel(new BorderLayout(0, 0));

        mainTabbedPane = new MainTabbedPane(getController());

        statusBar = new StatusBar(getController());

        updateTitle();

        inlineInfoCloseButton = new JButton3Icons(
            Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL),
            Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER),
            Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        inlineInfoCloseButton.setToolTipText(Translation
            .getTranslation("main_frame.inline_info_close.tip"));
        inlineInfoCloseButton.addActionListener(myActionListener);
        inlineInfoCloseButton.setContentAreaFilled(false);

        inlineInfoLabel = new JLabel();

        getController().addSilentModeListener(new MySilentModeListener());
        configurePauseResumeLink();
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
        title.append(appName);

        if (getController().isVerbose()) {
            // Append in front of program name in verbose mode
            title.append(" v" + Controller.PROGRAM_VERSION);
            if (getController().getBuildTime() != null) {
                title.append(" | build: " + getController().getBuildTime());
            }
            title.append(" | " + getController().getMySelf().getNick());
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

        PreferencesEntry.MAIN_FRAME_WIDTH.setValue(c, mainWidth);
        PreferencesEntry.INFO_WIDTH.setValue(getController(), infoWidth);

        if (isMaximized()) {
            PreferencesEntry.MAIN_FRAME_MAXIMIZED.setValue(c, true);
        } else {
            PreferencesEntry.MAIN_FRAME_MAXIMIZED.setValue(c, false);

            PreferencesEntry.MAIN_FRAME_X.setValue(c, uiComponent.getX());
            PreferencesEntry.MAIN_FRAME_Y.setValue(c, uiComponent.getY());

            // If info is inline and info is showing, do not store width because
            // info will not show at start up and the frame will be W-I-D-E.

            if (uiComponent.getWidth() > 0
                && (!shouldShowInfoInline() || !isShowingInfoInline()))
            {
                PreferencesEntry.MAIN_FRAME_WIDTH.setValue(c,
                    uiComponent.getWidth());
            }

            if (uiComponent.getHeight() > 0) {
                PreferencesEntry.MAIN_FRAME_HEIGHT.setValue(c,
                    uiComponent.getHeight());
            }

            if (isShowingInfoInline()) {
                PreferencesEntry.INFO_WIDTH.setValue(getController(),
                    inlineInfoPanel.getWidth());
            }

            mainTabbedPane.storeValues();
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

        if (isShowingInfoInline()) {
            mainWidth = split.getDividerLocation();
        } else if (!isMaximized()) {
            mainWidth = uiComponent.getWidth();
        } else {
            // Maximized window. Let info take the rest of the right screen.
            infoWidth = uiComponent.getWidth() - mainWidth;
        }
        logFine("Main/Info width: " + mainWidth + " / " + infoWidth);

        inlineInfoPanel = panel;
        inlineInfoLabel.setText(title);

        configureInlineInfo();
    }

    private void closeInlineInfoPanel() {
        if (isShowingInfoInline()) {
            mainWidth = split.getDividerLocation();
            infoWidth = inlineInfoPanel.getWidth() + split.getDividerSize() + 8;
            logFine("Main/Info width: " + mainWidth + " / " + infoWidth);
        }
        inlineInfoPanel = null;
        configureInlineInfo();
    }

    public boolean shouldShowInfoInline() {
        int inline = PreferencesEntry.INLINE_INFO_MODE
            .getValueInt(getController());
        return inline != 0;
    }

    public boolean isShowingInfoInline() {
        return inlineInfoPanel != null;
    }

    private void configureInlineInfo() {
        boolean inline = shouldShowInfoInline();
        boolean displaying = isShowingInfoInline();
        inlineInfoCloseButton.setVisible(inline && displaying);

        if (inline && displaying) {
            // Make sure the info inline panel does not take the full width
            // and hiding the main tabbed pane
            // inlineInfoPanel.setSize(new Dimension(inlineInfoPanel
            // .getMinimumSize().width, inlineInfoPanel.getHeight()));

            centralPanel.removeAll();
            split.setLeftComponent(mainTabbedPane.getUIComponent());
            split.setRightComponent(inlineInfoPanel);

            if (infoWidth <= 0) {
                infoWidth = PreferencesEntry.INFO_WIDTH
                    .getValueInt(getController());
                if (infoWidth <= 0) {
                    infoWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                    infoWidth -= mainWidth;
                    infoWidth -= uiComponent.getLocationOnScreen().x;
                    infoWidth -= Constants.UI_DEFAULT_SCREEN_BORDER;
                }
            }

            // #2440:
            if (infoWidth < MIN_INFO_WIDTH) {
                infoWidth = MIN_INFO_WIDTH;
            }

            logFine("Main/Info width: " + mainWidth + " / " + infoWidth);

            if (!isMaximized()) {
                int width = adjustWidthToScreen(mainWidth + infoWidth);
                if (width - mainWidth < MIN_INFO_WIDTH) {
                    width = mainWidth + MIN_INFO_WIDTH;
                }
                uiComponent.setSize(width, uiComponent.getSize().height);
            }

            final int dividerLocation = mainWidth;
            centralPanel.add(split, BorderLayout.CENTER);
            split.setDividerLocation(dividerLocation);

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
            if (!isMaximized()) {
                uiComponent.setSize(mainWidth, uiComponent.getSize().height);
            }
        }

        relocateIfNecessary();
    }

    private int adjustWidthToScreen(int width) {
        // int overScreenPX = width
        // - Toolkit.getDefaultToolkit().getScreenSize().width
        // + uiComponent.getLocationOnScreen().x;

        int overScreenPX = width - UIUtil.getScreenWidthAllMonitors()
            + uiComponent.getLocationOnScreen().x;

        if (overScreenPX > 0) {
            width -= overScreenPX;
            width -= Constants.UI_DEFAULT_SCREEN_BORDER;
        }
        return width;
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

    public void reconfigureForCompactMode(final boolean compactMode) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (compactModeActive.getAndSet(compactMode) != compactMode) {
                    if (compactMode) {
                        configureUiCompact();
                    } else {
                        configureUiUncompact();
                    }
                }
            }
        });
    }

    /**
     * Source:
     * http://stackoverflow.com/questions/309023/howto-bring-a-java-window
     * -to-the-front
     */
    public void toFront() {
        uiComponent.setVisible(true);
        int state = uiComponent.getExtendedState();
        state &= ~Frame.ICONIFIED;
        uiComponent.setExtendedState(state);
        uiComponent.setAlwaysOnTop(true);
        uiComponent.toFront();
        uiComponent.requestFocus();
        uiComponent.setAlwaysOnTop(false);
    }

    private void doCloseOperation() {
        if (OSUtil.isSystraySupported()) {
            handleExitFirstRequest();
            boolean quitOnX = PreferencesEntry.QUIT_ON_X.getValueBoolean(getController());
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

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyWindowFocusListner implements WindowFocusListener {
        public void windowGainedFocus(WindowEvent e) {
            getUIController().setActiveFrame(UIController.MAIN_FRAME_ID);
        }

        public void windowLostFocus(WindowEvent e) {
            // Nothing to do here.
        }
    }

    private class MyWindowListener extends WindowAdapter {

        @Override
        public void windowStateChanged(WindowEvent e) {
            boolean wasMaximized = (e.getOldState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            boolean nowMaximized = (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            if (wasMaximized && !nowMaximized) {
                logFine("old: " + e.getOldState() + ", new: " + e.getNewState());
                if (isShowingInfoInline()) {
                    // Prevents the following:
                    // 1) Start with main only
                    // 2) Maximize window
                    // 3) Show info inline
                    // 4) De-maximize
                    // -> Main window must not return to main width but
                    // main+info width.
                    int width = adjustWidthToScreen(uiComponent.getWidth()
                        + infoWidth);
                    uiComponent.setSize(width, uiComponent.getSize().height);
                } else {
                    // Prevents the following:
                    // 1) Start with main only
                    // 2) Show info inline
                    // 3) Maximize window
                    // 4) Close info.
                    // 5) De-maximize
                    // -> Main window must not return to main+info width but
                    // main width only.
                    uiComponent
                        .setSize(mainWidth, uiComponent.getSize().height);
                }
            }

        }

        public void windowClosing(WindowEvent e) {
            doCloseOperation();
        }

        /**
         * Hide other frames when main frame gets minimized.
         * 
         * @param e
         */
        public void windowIconified(WindowEvent e) {
            boolean minToSysTray = PreferencesEntry.MIN_TO_SYS_TRAY
                .getValueBoolean(getController());
            if (minToSysTray) {
                getUIController().hideChildPanels();
                uiComponent.setVisible(false);
            } else {
                super.windowIconified(e);
            }
        }
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

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

    private void configurePauseResumeLink() {
        if (getController().isSilentMode()) {
            pauseResumeLabel.setText(Translation.getTranslation("action_resume_sync.name"));
            pauseResumeLabel.setToolTipText(Translation.getTranslation("action_resume_sync.description"));
        } else {
            pauseResumeLabel.setText(Translation.getTranslation("action_pause_sync.name"));
            pauseResumeLabel.setToolTipText(Translation.getTranslation("action_pause_sync.description"));
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == inlineInfoCloseButton) {
                closeInlineInfoPanel();
            } else if (source == uncompactModeButton) {
                getUIController().reconfigureForCompactMode(false);
            } else if (source == closeButton) {
                doCloseOperation();
            }
        }
    }

    private class MySilentModeListener implements SilentModeListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void setSilentMode(SilentModeEvent event) {
            configurePauseResumeLink();
        }
    }

    /**
     * Mouse(Motion)Listener to handle dragging in compact mode.
     */
    private class MyHybridMouseListener implements MouseListener,
            MouseMotionListener {

        private int originalMouseXOnScreen;
        private int originalMouseYOnScreen;
        private int originalFrameX;
        private int originalFrameY;
        private Cursor originalCursor;

        /**
         * Remember where we started.
         *
         * @param e
         */
        public void mousePressed(MouseEvent e) {
            originalMouseXOnScreen = e.getXOnScreen();
            originalMouseYOnScreen = e.getYOnScreen();
            originalFrameX = getUIComponent().getX();
            originalFrameY = getUIComponent().getY();
        }

        /**
         * How much have we dragged?
         *
         * @param e
         */
        public void mouseDragged(MouseEvent e) {
            int x = originalFrameX + e.getXOnScreen() - originalMouseXOnScreen;
            int y = originalFrameY + e.getYOnScreen() - originalMouseYOnScreen;
            getUIComponent().setLocation(x, y);
        }

        /**
         * Make the cursor a hand.
         *
         * @param e
         */
        public void mouseEntered(MouseEvent e) {
            originalCursor = CursorUtils.setHandCursor(getUIComponent());
        }

        /**
         * Reset the cursor.
         *
         * @param e
         */
        public void mouseExited(MouseEvent e) {
            if (originalCursor != null) {
                CursorUtils.returnToOriginal(getUIComponent(), originalCursor);
            }
        }

        public void mouseReleased(MouseEvent e) {
            // Don't care
        }

        public void mouseMoved(MouseEvent e) {
            // Don't care
        }

        public void mouseClicked(MouseEvent e) {
            // Don't care
        }
    }

    private class MyOpenWebInterfaceAction extends BaseAction {

        private MyOpenWebInterfaceAction(Controller controller) {
            super("action_open_web_interface", controller);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_URL
                    .getValue(getController()));
            } catch (IOException e1) {
                logWarning("Unable to goto PowerFolder homepage", e1);
            }
        }
    }

    private static class MyPauseResumeAction extends BaseAction {

        private MyPauseResumeAction(Controller controller) {
            super("action_pause_sync", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().setSilentMode(!getController().isSilentMode());
        }
    }
}
