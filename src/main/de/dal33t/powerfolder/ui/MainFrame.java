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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.RootPaneUI;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.OverallFolderStatEvent;
import de.dal33t.powerfolder.event.OverallFolderStatListener;
import de.dal33t.powerfolder.event.SilentModeEvent;
import de.dal33t.powerfolder.event.SilentModeListener;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.NeverAskAgainResponse;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

/**
 * Powerfolder gui mainframe
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {

    public static final int MIN_HEIGHT_UNCOMPACT = 500;
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
    private ServerClient client;

    private JLabel syncTextLabel;
    private JLabel syncDateLabel;
    private ActionLabel loginActionLabel;
    private JLabel allInSyncLabel;
    private JButtonMini allInSyncButton;
    private JProgressBar usagePB;
    private ActionLabel openWebInterfaceActionLabel;
    private ActionLabel openFoldersBaseActionLabel;
    private ActionLabel pauseResumeActionLabel;
    private ActionLabel configurationActoinLabel;

    private AtomicBoolean compact = new AtomicBoolean();
    private JButton compactButton;
    private JButton3Icons closeButton;

    /**
     * @param controller
     *            the controller.
     * @throws HeadlessException
     */
    public MainFrame(Controller controller) throws HeadlessException {
        super(controller);

        compact.set(!PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController()));
        controller.getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        initComponents();
        configureUi();
        updateOnlineStorageDetails();

    }

    private JPanel createMiniPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow, pref", "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createLeftMiniPanel(), cc.xy(1, 1));
        builder.add(createRightMiniPanel(), cc.xy(2, 1));

        return builder.getPanel();
    }

    private Component createLeftMiniPanel() {
        CellConstraints cc = new CellConstraints();

        FormLayout layoutUpper = new FormLayout("pref, 3dlu, 100dlu",
            "pref, pref");
        DefaultFormBuilder builderUpper = new DefaultFormBuilder(layoutUpper);
        builderUpper.setBorder(Borders.createEmptyBorder("3dlu, 0, 0, 0"));

        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builderUpper.add(allInSyncButton, cc.xywh(1, 1, 1, 2));
        } else {
            builderUpper.add(allInSyncLabel, cc.xywh(1, 1, 1, 2));
        }
        builderUpper.add(syncTextLabel, cc.xy(3, 1));
        builderUpper.add(syncDateLabel, cc.xy(3, 2));

        FormLayout layoutLower = new FormLayout("pref, 100dlu", "pref, pref");
        DefaultFormBuilder builderLower = new DefaultFormBuilder(layoutLower);

        // Include a spacer icon that lines up the pair with builderUpper
        // when allInSyncLabel has null icon.
        builderLower.add(new JLabel((Icon) null), cc.xywh(1, 1, 1, 2));
        builderLower.add(loginActionLabel.getUIComponent(), cc.xy(2, 1));
        builderLower.add(usagePB, cc.xy(2, 2));

        // 7/8dlu spacer to line up the synced icon / button with the individual
        // icons in the folder list. There is a very slight difference in icon
        // position between JLabels and JButtonMinis.
        FormLayout layoutMain;
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            layoutMain = new FormLayout("8dlu, pref", "pref, 3dlu, pref");
        } else {
            layoutMain = new FormLayout("7dlu, pref", "pref, 3dlu, pref");
        }

        DefaultFormBuilder builderMain = new DefaultFormBuilder(layoutMain);
        builderMain.add(builderUpper.getPanel(), cc.xy(2, 1));
        builderMain.add(builderLower.getPanel(), cc.xy(2, 3));

        return builderMain.getPanel();
    }

    private Component createRightMiniPanel() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(openWebInterfaceActionLabel.getUIComponent(), cc.xy(1, 1));
        builder.add(openFoldersBaseActionLabel.getUIComponent(), cc.xy(1, 2));
        builder.add(pauseResumeActionLabel.getUIComponent(), cc.xy(1, 3));
        builder.add(configurationActoinLabel.getUIComponent(), cc.xy(1, 4));

        return builder.getPanel();
    }

    private void configureUi() {

        // Display the title pane.
        uiComponent.getRootPane().putClientProperty(
            "Synthetica.titlePane.enabled", Boolean.FALSE);
        uiComponent.getRootPane().updateUI();

        FormLayout layout = new FormLayout("fill:pref:grow, pref, 3dlu, pref",
            "pref, pref, fill:0:grow, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(logoLabel, cc.xyw(1, 1, 3));
        builder.add(closeButton, cc.xywh(4, 1, 1, 1, "right, top"));

        builder.add(inlineInfoLabel,
            cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));
        builder.add(inlineInfoCloseButton,
            cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));

        builder.add(compactButton,
            cc.xyw(1, 2, 4, CellConstraints.CENTER, CellConstraints.BOTTOM));

        builder.add(centralPanel, cc.xyw(1, 3, 4));

        builder.add(createMiniPanel(), cc.xyw(1, 4, 4));

        uiComponent.getContentPane().removeAll();
        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setResizable(true);

        Controller c = getController();

        // Pack elements
        uiComponent.pack();
        mainWidth = uiComponent.getWidth();
        logFine("Main/Info width: " + mainWidth + " / ?");

        // Initial top-left corner
        int mainX = PreferencesEntry.MAIN_FRAME_X.getValueInt(c);
        int mainY = PreferencesEntry.MAIN_FRAME_Y.getValueInt(c);
        uiComponent.setLocation(mainX, mainY);

        relocateIfNecessary();
        configureInlineInfo();
        updateSyncStats();

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

        uiComponent = new JFrame() {
            // Add own pack method to pack for compact / uncompact mode.
            @Override
            public void pack() {
                super.pack();
                customPack();
            }
        };
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

        allInSyncLabel = new JLabel(Icons.getIconById(Icons.SYNC_COMPLETE));
        allInSyncLabel
            .setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        allInSyncLabel.addMouseListener(new SwitchCompactModeByMouse());

        allInSyncButton = new JButtonMini(new MyShowFoldersAction(
            getController()));
        allInSyncButton.setIcon(Icons.getIconById(Icons.SYNC_COMPLETE));
        allInSyncButton.setText(null);

        syncTextLabel = new JLabel(" ");
        syncTextLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        syncTextLabel.addMouseListener(new SwitchCompactModeByMouse());

        syncDateLabel = new JLabel(" ");
        syncDateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        syncDateLabel.addMouseListener(new SwitchCompactModeByMouse());

        loginActionLabel = new ActionLabel(getController(), new MyLoginAction(
            getController()));
        usagePB = new JProgressBar();
        usagePB.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        usagePB.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    PFWizard.openLoginWizard(getController(), client);
                }
            }
        });

        openWebInterfaceActionLabel = new ActionLabel(getController(),
            new MyOpenWebInterfaceAction(getController()));
        openFoldersBaseActionLabel = new ActionLabel(getController(),
            new MyOpenFoldersBaseAction(getController()));
        pauseResumeActionLabel = new ActionLabel(getController(),
            new MyPauseResumeAction(getController()));
        configurationActoinLabel = new ActionLabel(getController(),
            getApplicationModel().getActionModel().getOpenPreferencesAction());

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

        logoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MyMouseWindowDragListener logoMouseListener = new MyMouseWindowDragListener();
        logoLabel.addMouseListener(logoMouseListener);
        logoLabel.addMouseMotionListener(logoMouseListener);

        closeButton = new JButton3Icons(
            Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL),
            Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER),
            Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        closeButton.setToolTipText(Translation
            .getTranslation("main_frame.close.tips"));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCloseOperation();
            }
        });

        centralPanel = new JPanel(new BorderLayout(0, 0));

        mainTabbedPane = new MainTabbedPane(getController());

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

        client = getApplicationModel().getServerClientModel().getClient();
        client.addListener(new MyServerClientListener());

        getApplicationModel().getFolderRepositoryModel()
            .addOverallFolderStatListener(new MyOverallFolderStatListener());

        compactButton = new JButtonMini(Icons.getIconById(Icons.COMPACT),
            Translation.getTranslation("main_frame.compact.tips"));
        compactButton.addActionListener(myActionListener);
        compactButton.setVisible(false);
    }

    /**
     * Nice default sizes for compact and uncompact mode.
     */
    private void customPack() {
        if (compact.get()) {
            // Default pack height is okay in compact mode.
            uiComponent.setSize(MIN_WIDTH, uiComponent.getHeight());
            uiComponent.setResizable(false);
        } else {
            // Include space for the central content.
            uiComponent.setSize(MIN_WIDTH, MIN_HEIGHT_UNCOMPACT);
            uiComponent.setResizable(true);
        }
    }

    private void updateSyncStats() {
        boolean syncing = getApplicationModel().getFolderRepositoryModel()
            .wasSyncingAtDate();
        Date syncDate;
        if (syncing) {
            syncDate = getApplicationModel().getFolderRepositoryModel()
                .getEtaSyncDate();
        } else {
            syncDate = getApplicationModel().getFolderRepositoryModel()
                .getLastSyncDate();
        }

        if (isFiner()) {
            logFiner("Sync status: syncing? " + syncing + ", date: " + syncDate);
        }

        String syncStatsText;
        boolean synced = false;
        if (!getController().getNodeManager().isStarted()) {
            // Not started
            syncStatsText = Translation
                .getTranslation("main_frame.not_running");
        } else if (getController().getFolderRepository().getFoldersCount() == 0)
        {
            // No folders
            syncStatsText = Translation.getTranslation("main_frame.no_folders");
        } else if (syncDate == null && !syncing) { // Never synced
            syncStatsText = Translation
                .getTranslation("main_frame.never_synced");
        } else {
            if (syncing) {
                long aniIndex = System.currentTimeMillis() / 1000 % 3;
                syncStatsText = Translation
                    .getTranslation("main_frame.synchronizing." + aniIndex);
            } else {
                syncStatsText = Translation
                    .getTranslation("main_frame.in_sync");
                synced = true;
            }
        }
        syncTextLabel.setText(syncStatsText);

        String syncDateText = " ";
        if (syncDate != null) {
            if (DateUtil.isDateMoreThanNDaysInFuture(syncDate, 2)) {
                syncDateText = Translation
                    .getTranslation("main_frame.sync_unknown");
            } else {
                String date = Format.formatDateShort(syncDate);
                syncDateText = syncing ? Translation.getTranslation(
                    "main_frame.sync_eta", date) : Translation.getTranslation(
                    "main_frame.last_synced", date);
            }
        }
        syncDateLabel.setText(syncDateText);
        allInSyncButton.setVisible(synced);
        allInSyncLabel.setVisible(synced);
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
     * Stores all current window values.
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
     * Shuts down the program
     */
    private void exitProgram() {
        if (getUIController().isShutdownAllowed()) {
            uiComponent.setVisible(false);
            uiComponent.dispose();
            new Thread("Close PowerFolder Thread") {
                @Override
                public void run() {
                    getController().exit(0);
                }
            }.start();
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private final class SwitchCompactModeByMouse extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1) {
                switchCompactMode();
            }
        }
    }

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

                // Need to show the compact button if normal state.
                compactButton.setVisible(true);
            } else if (!wasMaximized && nowMaximized) {
                // Need to hide the compact button if maximized state.
                compactButton.setVisible(false);
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
            pauseResumeActionLabel.setText(Translation
                .getTranslation("action_resume_sync.name"));
            pauseResumeActionLabel.setToolTipText(Translation
                .getTranslation("action_resume_sync.description"));
        } else {
            pauseResumeActionLabel.setText(Translation
                .getTranslation("action_pause_sync.name"));
            pauseResumeActionLabel.setToolTipText(Translation
                .getTranslation("action_pause_sync.description"));
        }
    }

    private void updateOnlineStorageDetails() {
        double percentageUsed = 0;
        long totalStorage = 0;
        long spaceUsed = 0;
        boolean showBar = true;
        if (StringUtils.isBlank(client.getUsername())) {
            loginActionLabel.setText(Translation
                .getTranslation("main_frame.account_not_set.text"));
            showBar = false;
        } else if (client.isPasswordEmpty()) {
            loginActionLabel.setText(Translation
                .getTranslation("main_frame.password_required.text"));
            showBar = false;
        } else if (client.isConnected()) {
            if (client.isLoggedIn()) {
                OnlineStorageSubscription storageSubscription = client
                    .getAccount().getOSSubscription();
                AccountDetails ad = client.getAccountDetails();
                if (storageSubscription.isDisabled()) {
                    loginActionLabel.setText(Translation
                        .getTranslation("main_frame.account_disabled.text"));
                } else {
                    totalStorage = storageSubscription.getStorageSize();
                    spaceUsed = ad.getSpaceUsed();
                    if (totalStorage > 0) {
                        percentageUsed = 100.0d * (double) spaceUsed
                            / (double) totalStorage;
                    }
                    percentageUsed = Math.max(0.0d, percentageUsed);
                    percentageUsed = Math.min(100.0d, percentageUsed);
                    String s = client.getUsername();
                    if (!StringUtils.isEmpty(s)) {
                        loginActionLabel.setText(s);
                    }
                }
            } else {
                loginActionLabel.setText(Translation
                    .getTranslation("main_frame.loging_in.text"));
                showBar = false;
            }
        } else {
            loginActionLabel.setText(Translation
                .getTranslation("main_frame.connecting.text"));
            showBar = false;
        }
        // usagePB.setVisible(showBar);
        usagePB.setValue((int) percentageUsed);
        usagePB.setToolTipText(Format.formatBytesShort(spaceUsed) + " / "
            + Format.formatBytesShort(totalStorage));
    }

    private void switchCompactMode() {
        boolean compactMe = !compact.getAndSet(!compact.get());
        if (compactMe) {
            // Need to hide the child windows when minimize.
            closeInlineInfoPanel();
            getUIController().hideChildPanels();

            compactButton.setIcon(Icons.getIconById(Icons.UNCOMPACT));
            compactButton.setToolTipText(Translation
                .getTranslation("main_frame.uncompact.tips"));
            mainTabbedPane.getUIComponent().setVisible(false);
            compactButton.setVisible(false);
        } else {
            compactButton.setIcon(Icons.getIconById(Icons.COMPACT));
            compactButton.setToolTipText(Translation
                .getTranslation("main_frame.compact.tips"));
            mainTabbedPane.getUIComponent().setVisible(true);
            compactButton.setVisible(false);
        }
        if (uiComponent.getExtendedState() == Frame.NORMAL) {
            getUIComponent().pack();
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
            } else if (source == compactButton) {
                switchCompactMode();
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

    private class MyOpenWebInterfaceAction extends BaseAction {

        private MyOpenWebInterfaceAction(Controller controller) {
            super("action_open_web_interface", controller);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                BrowserLauncher.openURL(client.getLoginURLWithCredentials());
            } catch (IOException e1) {
                logWarning("Unable to open web portal", e1);
            }
        }
    }

    private static class MyOpenFoldersBaseAction extends BaseAction {

        private MyOpenFoldersBaseAction(Controller controller) {
            super("action_open_folders_base", controller);
        }

        public void actionPerformed(ActionEvent e) {
            FileUtils.openFile(getController().getFolderRepository()
                .getFoldersAbsoluteDir());
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

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void login(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverConnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }
    }

    private class MyOverallFolderStatListener implements
        OverallFolderStatListener
    {
        public void statCalculated(OverallFolderStatEvent e) {
            updateSyncStats();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyShowFoldersAction extends BaseAction {
        private MyShowFoldersAction(Controller controller) {
            super("action_show_folders_tab", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().getMainFrame().showFoldersTab();
        }
    }

    private class MyLoginAction extends BaseAction {

        MyLoginAction(Controller controller) {
            super("action_login", controller);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard.openLoginWizard(getController(), client);
        }
    }

    private class MyMouseWindowDragListener extends MouseAdapter {
        private int startX;
        private int startY;
        private boolean inDrag;

        /** Called when the mouse has been pressed. */
        public void mousePressed(MouseEvent e) {
            Point p = e.getPoint();
            startX = p.x;
            startY = p.y;
            inDrag = true;
        }

        /** Called when the mouse has been released. */
        public void mouseReleased(MouseEvent e) {
            inDrag = false;
        }

        // And two methods from MouseMotionListener:
        public void mouseDragged(MouseEvent e) {
            Point p = e.getPoint();
            if (inDrag) {
                int dx = p.x - startX;
                int dy = p.y - startY;
                Point l = uiComponent.getLocation();
                uiComponent.setLocation(l.x + dx, l.y + dy);
            }
        }
    }

}
