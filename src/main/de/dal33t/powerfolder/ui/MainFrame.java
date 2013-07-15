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
 * $Id: MainFrame.java 20917 2013-02-24 06:30:05Z glasgow $
 */
package de.dal33t.powerfolder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.RootPaneUI;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.PausedModeEvent;
import de.dal33t.powerfolder.event.PausedModeListener;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.ChangePreferencesPermission;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.event.SyncStatusEvent;
import de.dal33t.powerfolder.ui.event.SyncStatusListener;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.NeverAskAgainResponse;
import de.dal33t.powerfolder.ui.util.SyncIconButtonMini;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProUtil;
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

    private enum FrameMode {MAXIMIZED, NORMAL, COMPACT, MINIMIZED}

    public static final int MIN_MAIN_TABBED_WIDTH = 300;

    private JFrame uiComponent;
    private JLabel logoLabel;
    private JPanel centralPanel;
    private MainTabbedPane mainTabbedPane;
    private JPanel inlineInfoPanel;
    private JLabel inlineInfoLabel;
    private JButton inlineInfoCloseButton;
    private JSplitPane split;
    private ServerClient client;

    // Left mini panel
    private JButtonMini allInSyncButton;
    private SyncIconButtonMini syncingButton;
    private JButtonMini setupButton;
    private JButtonMini pauseButton;
    private JButtonMini syncIncompleteButton;
    private JLabel notConnectedLoggedInLabel;

    private ActionLabel upperMainTextActionLabel;
    private ActionLabel lowerMainTextActionLabel;
    private ActionLabel setupLabel;
    private JLabel zyncroLabel;
    
    private ActionLabel loginActionLabel;
    private JProgressBar usagePB;
    private ActionLabel noticesActionLabel;

    private DelayedUpdater mainStatusUpdater;
    private DelayedUpdater osStatusUpdater;

    // Right mini panel
    private ActionLabel expandCollapseActionLabel;
    private MyExpandCollapseAction expandCollapseAction;
    private ActionLabel openWebInterfaceActionLabel;
    private ActionLabel openFoldersBaseActionLabel;
    private ActionLabel pauseResumeActionLabel;
    private ActionLabel configurationActionLabel;
    private ActionLabel openDebugActionLabel;
    private ActionLabel openTransfersActionLabel;

    private FrameMode frameMode = FrameMode.NORMAL;
    private JButton3Icons closeButton;
    private JButton3Icons plusButton;
    private JButton3Icons minusButton;

    /** Has the main frame state been set after init? */
    private final AtomicBoolean frameStateSet = new AtomicBoolean();

    /**
     * @param controller
     *            the controller.
     * @throws HeadlessException
     */
    public MainFrame(Controller controller) throws HeadlessException {
        super(controller);

        mainStatusUpdater = new DelayedUpdater(getController());
        osStatusUpdater = new DelayedUpdater(getController());
        controller.getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        initComponents();
        configureUi();
        updateOnlineStorageDetails();

        // Start COMPACT for basic users, and NORMAL for experts.
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            frameMode = FrameMode.NORMAL;
        } else {
            frameMode = FrameMode.COMPACT;
        }
        setFrameMode(frameMode, true);
    }

    private JPanel createMiniPanel() {
        FormLayout layout = new FormLayout("left:pref:grow, left:pref",
            "top:pref:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("10dlu, 0, 0, 3dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(createLeftMiniPanel(), cc.xy(1, 1));
        builder.add(createRightMiniPanel(), cc.xy(2, 1));

        return builder.getPanel();
    }

    private Component createLeftMiniPanel() {
        CellConstraints cc = new CellConstraints();

        // UPPER PART
        FormLayout layoutUpper = new FormLayout("pref, 3dlu, pref:grow",
            "pref, pref");
        DefaultFormBuilder builderUpper = new DefaultFormBuilder(layoutUpper);
        PanelBuilder b = new PanelBuilder(new FormLayout("pref:grow",
            "pref:grow"));
        b.add(allInSyncButton, cc.xy(1, 1));
        b.add(syncingButton, cc.xy(1, 1));
        b.add(setupButton, cc.xy(1, 1));
        b.add(pauseButton, cc.xy(1, 1));
        b.add(syncIncompleteButton, cc.xy(1, 1));
        b.add(notConnectedLoggedInLabel, cc.xy(1, 1));
        builderUpper.add(b.getPanel(), cc.xywh(1, 1, 1, 2));
        builderUpper.add(upperMainTextActionLabel.getUIComponent(), cc.xy(3, 1));
        builderUpper
            .add(lowerMainTextActionLabel.getUIComponent(), cc.xy(3, 2));
        if (getController().getOSClient().getAccount()
            .hasPermission(FolderCreatePermission.INSTANCE))
        {
            builderUpper.add(setupLabel.getUIComponent(), cc.xy(3, 2));
        } else {
            // TODO: this is just a quick and dirty fix. Do something reasonable
            // here.
            if (ProUtil.isZyncro(getController())) {
                builderUpper.add(zyncroLabel, cc.xy(3, 2));
            } else {
                builderUpper.add(new JLabel(" "), cc.xy(3, 2));
            }
        }
        // UPPER PART END

        // LOWER PART
        FormLayout layoutLower = new FormLayout("pref, 100dlu",
                "pref, pref, pref");
        DefaultFormBuilder builderLower = new DefaultFormBuilder(layoutLower);
        // Include a spacer icon that lines up the pair with builderUpper
        // when allInSyncLabel has null icon.
        builderLower.add(new JLabel((Icon) null), cc.xywh(1, 1, 1, 2));
        builderLower.add(loginActionLabel.getUIComponent(), cc.xy(2, 1));
        builderLower.add(usagePB, cc.xy(2, 2));
        // Make sure the noticesActionLabel vertical space is maintained.
        builderLower.add(new JLabel(" "), cc.xy(1, 3));
        builderLower.add(noticesActionLabel.getUIComponent(), cc.xy(2, 3));
        // LOWER PART END

        // PUT TOGETHER
        FormLayout layoutMain = new FormLayout("pref", "pref, 5dlu, pref");
        DefaultFormBuilder builderMain = new DefaultFormBuilder(layoutMain);
        builderMain.setBorder(Borders.createEmptyBorder("0, 5dlu, 5dlu, 0"));
        builderMain.add(builderUpper.getPanel(), cc.xy(1, 1));
        builderMain.add(builderLower.getPanel(), cc.xy(1, 3));
        // PUT TOGETHER END

        return builderMain.getPanel();
    }

    private Component createRightMiniPanel() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref, pref, pref, pref, pref, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(expandCollapseActionLabel.getUIComponent(), cc.xy(1, 1));
        if (ConfigurationEntry.WEB_LOGIN_ALLOWED
            .getValueBoolean(getController()))
        {
            builder.add(openWebInterfaceActionLabel.getUIComponent(),
                cc.xy(1, 2));
        }
        builder.add(openFoldersBaseActionLabel.getUIComponent(), cc.xy(1, 3));
        builder.add(pauseResumeActionLabel.getUIComponent(), cc.xy(1, 4));
        builder.add(configurationActionLabel.getUIComponent(), cc.xy(1, 5));
        if (getController().isVerbose()) {
            builder.add(openDebugActionLabel.getUIComponent(), cc.xy(1, 6));
        }
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder.add(openTransfersActionLabel.getUIComponent(), cc.xy(1, 7));
        }

        return builder.getPanel();
    }

    private void configureUi() {

        // Display the title pane.
        uiComponent.getRootPane().putClientProperty(
            "Synthetica.titlePane.enabled", Boolean.FALSE);
        uiComponent.getRootPane().updateUI();

        FormLayout layout = new FormLayout("fill:pref:grow, pref, 3dlu, pref",
            "pref, fill:0:grow, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(logoLabel, cc.xyw(1, 1, 3));

        ButtonBarBuilder b = new ButtonBarBuilder();
        b.addFixed(minusButton);
        b.addFixed(plusButton);
        b.addFixed(closeButton);
        builder.add(b.getPanel(), cc.xywh(4, 1, 1, 1, "right, top"));

        builder.add(inlineInfoLabel,
            cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));
        builder.add(inlineInfoCloseButton,
            cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));

        builder.add(centralPanel, cc.xyw(1, 2, 4));

        builder.add(createMiniPanel(), cc.xyw(1, 3, 4));

        uiComponent.getContentPane().removeAll();
        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setResizable(true);

        Controller c = getController();

        // Pack elements and set to default size.
        uiComponent.pack();
        uiComponent.setSize(uiComponent.getWidth(),
                UIConstants.MAIN_FRAME_DEFAULT_HEIGHT);

        configureInlineInfo();
        updateMainStatus(SyncStatusEvent.NOT_STARTED);
        updateNoticesLabel();
    }

    /**
     * Show notices link if there are notices available.
     */
    private void updateNoticesLabel() {
        int unreadCount = (Integer) getController().getUIController()
                .getApplicationModel().getNoticesModel()
                .getUnreadNoticesCountVM().getValue();
        if (unreadCount == 0) {
            noticesActionLabel.setVisible(false);
        } else if (unreadCount == 1) {
            noticesActionLabel.setVisible(true);
            noticesActionLabel.setText(Translation.getTranslation(
                    "main_frame.unread_notices.single.text"));
        } else {
            noticesActionLabel.setVisible(true);
            noticesActionLabel.setText(Translation.getTranslation(
                    "main_frame.unread_notices.plural.text",
                    String.valueOf(unreadCount)));
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
     * Initializes all ui components
     */
    private void initComponents() {
        uiComponent = new JFrame();
        uiComponent.setTransferHandler(new MyTransferHandler());
        uiComponent.addWindowFocusListener(new MyWindowFocusListener());
        uiComponent.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));
        uiComponent.setBackground(Color.white);

        BaseAction mySetupAction = new MySetupAction(getController());
        mySetupAction.allowWith(FolderCreatePermission.INSTANCE);
        
        MyOpenFoldersBaseAction myOpenFoldersBaseAction =
                new MyOpenFoldersBaseAction(getController());
        allInSyncButton = new JButtonMini(myOpenFoldersBaseAction);
        allInSyncButton.setIcon(Icons.getIconById(Icons.SYNC_COMPLETE));
        allInSyncButton.setText(null);

        syncIncompleteButton = new JButtonMini(myOpenFoldersBaseAction);
        syncIncompleteButton.setIcon(Icons.getIconById(Icons.SYNC_INCOMPLETE));
        syncIncompleteButton.setText(null);

        pauseButton = new JButtonMini(new MyPauseResumeAction(getController()));
        pauseButton.setIcon(Icons.getIconById(Icons.PAUSE));
        pauseButton.setText(null);

        syncingButton = new SyncIconButtonMini(getController());
        syncingButton.addActionListener(myOpenFoldersBaseAction);
        syncingButton.setVisible(false);

        setupButton = new JButtonMini(mySetupAction);
        setupButton.setIcon(Icons.getIconById(Icons.ACTION_ARROW));
        setupButton.setText(null);

        notConnectedLoggedInLabel = new JLabel(Icons.getIconById(Icons.WARNING));

        upperMainTextActionLabel = new ActionLabel(getController(),
                new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                handleSyncTextClick();
            }
        });

        lowerMainTextActionLabel = new ActionLabel(getController(),
                new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                    handleSyncTextClick();
                }
            });

        if (ProUtil.isZyncro(getController())) {
            upperMainTextActionLabel.setNeverUnderline(true);
            lowerMainTextActionLabel.setNeverUnderline(true);
        }

        if (getController().getOSClient().getAccount()
            .hasPermission(FolderCreatePermission.INSTANCE))
        {
            setupLabel = new ActionLabel(getController(), mySetupAction);
        }

        zyncroLabel = new JLabel();
        loginActionLabel = new ActionLabel(getController(), new MyLoginAction(
            getController()));
        noticesActionLabel = new ActionLabel(getController(),
            new MyShowNoticesAction(getController()));
        updateNoticesLabel();

        usagePB = new JProgressBar();
        usagePB.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        usagePB.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    try {
                        if (StringUtils.isBlank(client.getUsername())) {
                            PFWizard.openLoginWizard(getController(), client);
                        } else if (ConfigurationEntry.WEB_LOGIN_ALLOWED
                            .getValueBoolean(getController()))
                        {
                            BrowserLauncher.openURL(client.getWebURL(
                                Constants.MY_ACCOUNT_URI, true));
                        }
                    } catch (IOException ex) {
                        logSevere(ex);
                    }
                }
            }
        });

        expandCollapseAction = new MyExpandCollapseAction(getController());
        expandCollapseActionLabel = new ActionLabel(getController(),
            expandCollapseAction);
        openWebInterfaceActionLabel = new ActionLabel(getController(),
            new MyOpenWebInterfaceAction(getController()));
        openFoldersBaseActionLabel = new ActionLabel(getController(),
                myOpenFoldersBaseAction);
        pauseResumeActionLabel = new ActionLabel(getController(),
            new MyPauseResumeAction(getController()));
        configurationActionLabel = new ActionLabel(getController(),
                new MyOpenPreferencesAction(getController()));
        openDebugActionLabel = new ActionLabel(getController(),
            new MyOpenDebugAction(getController()));
        openTransfersActionLabel = new ActionLabel(getController(),
            new MyOpenTransfersAction(getController()));

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

        plusButton = new JButton3Icons(
            Icons.getIconById(Icons.WINDOW_PLUS_NORMAL),
            Icons.getIconById(Icons.WINDOW_PLUS_HOVER),
            Icons.getIconById(Icons.WINDOW_PLUS_PUSH));
        plusButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doPlusOperation();
            }
        });

        minusButton = new JButton3Icons(
            Icons.getIconById(Icons.WINDOW_MINUS_NORMAL),
            Icons.getIconById(Icons.WINDOW_MINUS_HOVER),
            Icons.getIconById(Icons.WINDOW_MINUS_PUSH));
        minusButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doMinusOperation();
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
        inlineInfoCloseButton
            .addActionListener(new MyInlineCloseInfoActionListener());
        inlineInfoCloseButton.setContentAreaFilled(false);

        inlineInfoLabel = new JLabel();

        configurePauseResumeLink();

        client = getApplicationModel().getServerClientModel().getClient();
        client.addListener(new MyServerClientListener());

        // Start listening to notice changes.
        getController().getUIController().getApplicationModel()
            .getNoticesModel().getAllNoticesCountVM()
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    updateNoticesLabel();
                }
            });

        getController().getUIController().getApplicationModel()
            .getNoticesModel().getUnreadNoticesCountVM()
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    updateNoticesLabel();
                }
            });

        getController().getUIController().getApplicationModel().addSyncStatusListener(new SyncStatusListener() {
            public void syncStatusChanged(final SyncStatusEvent event) {
                mainStatusUpdater.schedule(new Runnable() {
                    public void run() {
                        updateMainStatus(event);
                    }
                });
            }

            public boolean fireInEventDispatchThread() {
                return true;
            }
        });

        getController().addPausedModeListener(new MyPausedModeListener());
    }

    private void handleSyncTextClick() {
        if (ProUtil.isZyncro(getController())) {
            return;
        }
        if (getController().isPaused()) {
            getController().setPaused(false);
        } else if (frameMode == FrameMode.COMPACT) {
            setFrameMode(FrameMode.NORMAL);
        } else {
            setFrameMode(FrameMode.COMPACT);
        }
        setLinkTooltips();
    }

    private void updateMainStatus(SyncStatusEvent event) {

        FolderRepositoryModel folderRepositoryModel = getUIController()
                .getApplicationModel().getFolderRepositoryModel();

        // Set visibility of buttons and labels.
        pauseButton.setVisible(event.equals(SyncStatusEvent.PAUSED));
        if (getController().getOSClient().getAccount()
            .hasPermission(FolderCreatePermission.INSTANCE)
            && setupLabel != null)
        {
            setupLabel.setVisible(event.equals(SyncStatusEvent.NOT_STARTED) || event.equals(SyncStatusEvent.NO_FOLDERS));
        }
        setupButton.setVisible(event.equals(SyncStatusEvent.NOT_STARTED) || event.equals(SyncStatusEvent.NO_FOLDERS));
        allInSyncButton.setVisible(event.equals(SyncStatusEvent.SYNCHRONIZED));
        syncingButton.setVisible(event.equals(SyncStatusEvent.SYNCING));
        syncingButton.spin(event.equals(SyncStatusEvent.SYNCING));
        syncIncompleteButton.setVisible(event.equals(SyncStatusEvent.SYNC_INCOMPLETE));
        notConnectedLoggedInLabel.setVisible(event.equals(SyncStatusEvent.NOT_CONNECTED) || event.equals(SyncStatusEvent.NOT_LOGGED_IN));

        // Default sync date.
        Date syncDate = folderRepositoryModel.getLastSyncDate();

        // Upper text / setup text stuff.
        double overallSyncPercentage =
                folderRepositoryModel.getOverallSyncPercentage();
        String upperText = " ";
        String setupText = " ";
        zyncroLabel.setText(" ");

        if (event.equals(SyncStatusEvent.PAUSED)) {
            String pausedTemp = overallSyncPercentage >= 0
                && overallSyncPercentage < 99.5d ? Format
                .formatDecimal(overallSyncPercentage) + '%' : "";
            upperText = Translation.getTranslation("main_frame.paused",
                pausedTemp);
        } else if (event.equals(SyncStatusEvent.NOT_STARTED)) {
            upperText = Translation.getTranslation("main_frame.not_running");
            setupText = Translation.getTranslation("main_frame.activate_now");
        } else if (event.equals(SyncStatusEvent.NO_FOLDERS)) {
            upperText = Translation.getTranslation("main_frame.no_folders");
            setupText = getApplicationModel().getActionModel()
                .getNewFolderAction().getName();
            zyncroLabel
                .setText(Translation.getTranslation("main_frame.choose_folders"));
        } else if (event.equals(SyncStatusEvent.SYNCING)) {
            syncDate = folderRepositoryModel.getEstimatedSyncDate();
            String syncingTemp = overallSyncPercentage >= 0
                && overallSyncPercentage < 99.5d ? Format
                .formatDecimal(overallSyncPercentage) + '%' : "...";
            upperText = Translation.getTranslation("main_frame.syncing",
                syncingTemp);
        } else if (event.equals(SyncStatusEvent.SYNCHRONIZED)) {
                upperText = Translation.getTranslation("main_frame.in_sync");
        } else if (event.equals(SyncStatusEvent.SYNC_INCOMPLETE)) {
                upperText = Translation.getTranslation(
                        "main_frame.sync_incomplete");
        } else if (event.equals(SyncStatusEvent.NOT_CONNECTED)) {
            upperText = Translation.getTranslation("main_frame.connecting.text");
        } else if (event.equals(SyncStatusEvent.LOGGING_IN)) {
            upperText = Translation.getTranslation("main_frame.logging_in.text");
        } else if (event.equals(SyncStatusEvent.NOT_LOGGED_IN)) {
            upperText = Translation.getTranslation("main_frame.log_in_failed.text");
        } else {
            logSevere("Not handling all sync states: " + event);
        }

        upperMainTextActionLabel.setText(upperText);
        if (getController().getOSClient().getAccount()
            .hasPermission(FolderCreatePermission.INSTANCE)
            && setupLabel != null)
        {
            setupLabel.setText(setupText);
        }

        // The lowerMainTextActionLabel and setupLabel share the same slot,
        // so visibility is mutually exclusive.
        boolean notStartedOrNoFolders = event.equals(SyncStatusEvent.NOT_STARTED)
            || event.equals(SyncStatusEvent.NO_FOLDERS);
        if (getController().getOSClient().getAccount()
            .hasPermission(FolderCreatePermission.INSTANCE)
            && setupLabel != null)
        {
            setupLabel.setVisible(notStartedOrNoFolders);
        }
        lowerMainTextActionLabel.setVisible(!notStartedOrNoFolders);

        // Lower text - sync date stuff.
        String lowerText = " ";
        if (syncDate != null) {
            //If ETA sync > 3 days: no text.
            if (!DateUtil.isDateMoreThanNDaysInFuture(syncDate, 3)) {
                String date = Format.formatDateShort(syncDate);
                boolean inFuture = syncDate.after(new Date());
                if (inFuture) {
                    // If ETA sync > 20 hours show text: "Estimated sync: in X days"
                    // If ETA sync > 45 minutes show text: "Estimated sync: in X hours"
                    // If ETA sync < 45 minutes show text: "Estimated sync: in X minutes"
                    if (DateUtil.isDateMoreThanNHoursInFuture(syncDate, 20)) {
                        int days = DateUtil.getDaysInFuture(syncDate);
                        if (days <= 1) {
                        lowerText = Translation.getTranslation(
                                "main_frame.sync_eta_one_day");
                        } else {
                            lowerText = Translation.getTranslation(
                                    "main_frame.sync_eta_days", String.valueOf(
                                            days));
                        }
                    } else if (DateUtil.isDateMoreThanNMinutesInFuture(syncDate,
                            45)) {
                        int hours = DateUtil.getHoursInFuture(syncDate);
                        if (hours <= 1) {
                        lowerText = Translation.getTranslation(
                                "main_frame.sync_eta_one_hour");
                        } else {
                            lowerText = Translation.getTranslation(
                                    "main_frame.sync_eta_hours", String.valueOf(
                                            hours));
                        }
                    } else {
                        int minutes = DateUtil.getMinutesInFuture(syncDate);
                        if (minutes <= 1) {
                        lowerText = Translation.getTranslation(
                                "main_frame.sync_eta_one_minute");
                        } else {
                            lowerText = Translation.getTranslation(
                                    "main_frame.sync_eta_minutes",
                                    String.valueOf(minutes));
                        }
                    }
                } else {
                    lowerText = Translation.getTranslation(
                        "main_frame.last_synced", date);
                }
            }
        }
        lowerMainTextActionLabel.setText(lowerText);
    }

    /**
     * Updates the title
     */
    public void updateTitle() {
        StringBuilder title = new StringBuilder();

        String appName = Translation.getTranslation("general.application.name");
        // @todo Unclear, please comment what this is about.
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
        Controller controller = getController();

        if (isMaximized()) {
            PreferencesEntry.MAIN_FRAME_MAXIMIZED.setValue(controller, true);
        } else {
            PreferencesEntry.MAIN_FRAME_MAXIMIZED.setValue(controller, false);
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
     * For non-experts, if they log in
     * and only have online folders and no local folders,
     * and if the user has not already changed the frame state manually
     * then change from compact to normal,
     * helping the user to see the online folders
     * so they can do something about it.
     */
    private void showOSFolderList() {
        if (frameStateSet.get()) {
            return;
        }
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            return;
        }
        if (!getController().getFolderRepository().getFolders().isEmpty()) {
            return;
        }
        if (client.getAccountFolders().isEmpty()) {
            return;
        }
        if (frameMode == FrameMode.COMPACT) {
            setFrameMode(FrameMode.NORMAL);
        }
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
        // Fix Synthetica maximization, otherwise it covers the task bar.
        // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
        RootPaneUI ui = uiComponent.getRootPane().getUI();
        if (ui instanceof SyntheticaRootPaneUI) {
            ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
        }

        inlineInfoPanel = panel;
        inlineInfoLabel.setText(title);

        configureInlineInfo();
    }

    private void closeInlineInfoPanel() {
        inlineInfoPanel = null;
        configureInlineInfo();
    }

    public boolean isShowingInfoInline() {
        return inlineInfoPanel != null;
    }

    private void configureInlineInfo() {
        boolean displaying = isShowingInfoInline();
        inlineInfoCloseButton.setVisible(displaying);

        if (displaying) {

            centralPanel.removeAll();
            split.setLeftComponent(mainTabbedPane.getUIComponent());
            split.setRightComponent(inlineInfoPanel);

            centralPanel.add(split, BorderLayout.CENTER);

        } else {
            // Split pane place holders
            split.setLeftComponent(new JPanel());
            split.setRightComponent(new JPanel());

            centralPanel.removeAll();
            centralPanel.add(mainTabbedPane.getUIComponent(),
                BorderLayout.CENTER);
            inlineInfoPanel = null;
            inlineInfoLabel.setText("");
        }
        if (frameMode == FrameMode.NORMAL) {
            configureNormalSize();
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
        uiComponent.toFront();
        uiComponent.requestFocus();
    }

    private void doCloseOperation() {
        if (OSUtil.isSystraySupported()) {
            if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
                handleExitFirstRequest();
            }
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

    private void doMinusOperation() {
        if (frameMode == FrameMode.MAXIMIZED || frameMode == FrameMode.NORMAL) {
            // To COMPACT mode.
            setFrameMode(FrameMode.COMPACT);
        } else {
            setFrameMode(FrameMode.MINIMIZED);
        }
    }

    private void doPlusOperation() {
        if (frameMode == FrameMode.MAXIMIZED) {
            // To NORMAL mode.
            setFrameMode(FrameMode.NORMAL);
        } else if (frameMode == FrameMode.NORMAL) {
            // To MAXIMIZED mode.
            setFrameMode(FrameMode.MAXIMIZED);
        } else {
            // To NORMAL mode.
            setFrameMode(FrameMode.NORMAL);
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

    private class MyWindowFocusListener implements WindowFocusListener {
        public void windowGainedFocus(WindowEvent e) {
            getUIController().setActiveFrame(UIController.MAIN_FRAME_ID);
        }

        public void windowLostFocus(WindowEvent e) {
            // Nothing to do here.
        }
    }

    private class MyWindowListener extends WindowAdapter {

        public void windowClosing(WindowEvent e) {
            doCloseOperation();
        }

        /**
         * Hide other frames when main frame gets minimized.
         * 
         * @param e
         */
        public void windowIconified(WindowEvent e) {
            getUIController().hideChildPanels();
            if (OSUtil.isSystraySupported()) {
                uiComponent.setVisible(false);
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
            // Let the MainTabbedPane know so it can scroll the Folders tab to the new folder.
            mainTabbedPane.folderCreated(e);
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
        if (getController().isPaused()) {
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

    private void checkCloudSpace() {
        if (PreferencesEntry.WARN_FULL_CLOUD.getValueBoolean(getController())) {
            if (client != null && client.isLoggedIn()) {
                if (!client.getAccount().getOSSubscription().isDisabled()) {
                    long storageSize = client.getAccount().getOSSubscription()
                            .getStorageSize();
                    long used = client.getAccountDetails().getSpaceUsed();
                    if (used >= storageSize * 9 / 10) {
                        // More than 90% used. Notify.
                        WarningNotice notice = new WarningNotice(
                                Translation.getTranslation(
                                        "warning_notice.title"),
                                Translation.getTranslation(
                                        "warning_notice.cloud_full_summary"),
                                Translation.getTranslation(
                                        "warning_notice.cloud_full_message"));
                        getUIController().getApplicationModel()
                                .getNoticesModel().handleNotice(notice);
                    }
                }
            }
        }
    }

    private void updateOnlineStorageDetails() {
        osStatusUpdater.schedule(new Runnable() {
            public void run() {
                updateOnlineStorageDetails0();
            }
        });
    }

    private void updateOnlineStorageDetails0() {
        double percentageUsed = 0;
        long totalStorage = 0;
        long spaceUsed = 0;
        if (StringUtils.isBlank(client.getUsername())) {
            loginActionLabel.setText(Translation
                .getTranslation("main_frame.account_not_set.text"));
        } else if (client.isPasswordEmpty()) {
            loginActionLabel.setText(Translation
                .getTranslation("main_frame.password_required.text"));
        } else if (client.isConnected()) {
            if (client.isLoggedIn()) {
                OnlineStorageSubscription storageSubscription = client
                    .getAccount().getOSSubscription();
                AccountDetails ad = client.getAccountDetails();
                if (storageSubscription.isDisabled()) {
                    loginActionLabel.setText(Translation
                        .getTranslation("main_frame.storage_subscription_disabled.text"));
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
            } else if (client.isLoggingIn()) {
                loginActionLabel.setText(Translation
                    .getTranslation("main_frame.logging_in.text"));
            } else {
                // Not logged in and not logging in? Looks like it has failed.
                loginActionLabel.setText(Translation
                    .getTranslation("main_frame.log_in_failed.text_click"));
                if (!PFWizard.isWizardOpen()) {
                    PFWizard.openLoginWizard(getController(), client);
                }
            }
        } else {
            loginActionLabel.setText(Translation
                .getTranslation("main_frame.connecting.text"));
        }
        usagePB.setValue((int) percentageUsed);
        usagePB.setToolTipText(Format.formatBytesShort(spaceUsed) + " / "
            + Format.formatBytesShort(totalStorage));
    }

    private void setFrameMode(FrameMode frameMode) {

        // Remember that the frame has been set manually.
        frameStateSet.set(true);

        setFrameMode(frameMode, false);
    }

    private void setFrameMode(FrameMode frameMode, boolean init) {
        expandCollapseAction.setShowExpand(frameMode == FrameMode.COMPACT);
        this.frameMode = frameMode;
        switch (frameMode) {
            case MAXIMIZED :
                // http://www.javasoft.de/synthetica/faq/#general-7
                if (uiComponent.getRootPane().getUI() instanceof SyntheticaRootPaneUI)
                {
                    ((SyntheticaRootPaneUI) uiComponent.getRootPane().getUI())
                            .setMaximizedBounds(uiComponent);
                }
                uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
                plusButton.setToolTipText(Translation
                        .getTranslation("main_frame.restore.tips"));
                plusButton.setIcons(
                        Icons.getIconById(Icons.WINDOW_PLUS_NORMAL),
                        Icons.getIconById(Icons.WINDOW_PLUS_HOVER),
                        Icons.getIconById(Icons.WINDOW_PLUS_PUSH));
                minusButton.setVisible(true);
                minusButton.setToolTipText(
                        Translation.getTranslation("main_frame.compact.tips"));
                checkSplitMinWidth();
                break;
            case NORMAL:
                uiComponent.setExtendedState(Frame.NORMAL);
                uiComponent.setResizable(true);
                plusButton.setToolTipText(
                        Translation.getTranslation("main_frame.maximize.tips"));
                plusButton.setIcons(Icons.getIconById(
                        Icons.WINDOW_MAXIMIZE_NORMAL), 
                        Icons.getIconById(Icons.WINDOW_MAXIMIZE_HOVER),
                        Icons.getIconById(Icons.WINDOW_MAXIMIZE_PUSH));
                minusButton.setVisible(true);
                minusButton.setToolTipText(
                        Translation.getTranslation("main_frame.compact.tips"));
                configureNormalSize();
                UIUtil.invokeLaterInEDT(new Runnable() {
                    public void run() {
                        configureNormalSize();
                    }
                });
                break;
            case COMPACT:
                uiComponent.setExtendedState(Frame.NORMAL);
                // Need to hide the child windows when minimizing.
                if (!init) {
                    closeInlineInfoPanel();
                    getUIController().hideChildPanels();
                }
                uiComponent.setSize(uiComponent.getMinimumSize());
                uiComponent.setResizable(false);
                plusButton.setToolTipText(
                        Translation.getTranslation("main_frame.restore.tips"));
                plusButton.setIcons(Icons.getIconById(Icons.WINDOW_PLUS_NORMAL),
                        Icons.getIconById(Icons.WINDOW_PLUS_HOVER),
                        Icons.getIconById(Icons.WINDOW_PLUS_PUSH));
                minusButton.setToolTipText(
                        Translation.getTranslation("main_frame.minimize.tips"));
                // Don't show minimize button if systray is available
                // and the exit button uses minimize option.
                minusButton.setVisible(!OSUtil.isSystraySupported() ||
                        PreferencesEntry.QUIT_ON_X.getValueBoolean(getController()));
                toFront();
                UIUtil.invokeLaterInEDT(new Runnable() {
                    public void run() {
                        uiComponent.setSize(uiComponent.getMinimumSize());
                    }
                });
                break;
            case MINIMIZED:
                uiComponent.setExtendedState(Frame.ICONIFIED);
                break;
        }

        setLinkTooltips();
    }

    private void configureNormalSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = isShowingInfoInline() ? Constants.DEFAULT_NORMAL_DOCKED_WIDTH : (int) uiComponent.getMinimumSize().getWidth();
        int x = (int) ((screenSize.getWidth() - w) / 2.0);
        int h = Constants.DEFAULT_NORMAL_HEIGHT;
        int y = (int) ((screenSize.getHeight() - h) / 2.0);

        uiComponent.setSize(w, h);
        uiComponent.setLocation(x, y);
        checkSplitMinWidth();
    }

    /**
     * If showing inline, check that the divider is not too wide.
     */
    private void checkSplitMinWidth() {
        if (isShowingInfoInline()) {
            if (split.getDividerLocation() < MIN_MAIN_TABBED_WIDTH) {
                split.setDividerLocation(MIN_MAIN_TABBED_WIDTH);
            }

            // No clue why this have to be done later.
            // However if not this change does not come thru
            // on the first time the inline component/splitpane is shown.
            UIUtil.invokeLaterInEDT(new Runnable() {
                public void run() {
                    if (split.getDividerLocation() < MIN_MAIN_TABBED_WIDTH) {
                        split.setDividerLocation(MIN_MAIN_TABBED_WIDTH);
                    }
                }
            });
        }
    }

    private void setLinkTooltips() {
        if (getController().isPaused()) {
            upperMainTextActionLabel.setToolTipText(Translation.getTranslation(
                    "action_resume_sync.description"));
            lowerMainTextActionLabel.setToolTipText(Translation.getTranslation(
                    "action_resume_sync.description"));
        } else if (frameMode == FrameMode.COMPACT) {
            upperMainTextActionLabel.setToolTipText(Translation.getTranslation(
                    "action_expand_interface.name"));
            lowerMainTextActionLabel.setToolTipText(Translation.getTranslation(
                    "action_expand_interface.name"));
        } else if (PreferencesEntry.MINIMAL_PREFERENCES
            .getValueBoolean(getController())
            && !PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            upperMainTextActionLabel.setToolTipText(Translation
                .getTranslation("main_frame.minimal.change_loging.tip"));
            lowerMainTextActionLabel.setToolTipText(Translation
                .getTranslation("main_frame.minimal.change_loging.tip"));
        } else {
            upperMainTextActionLabel.setToolTipText(Translation.getTranslation(
                    "action_collapse_interface.name"));
            lowerMainTextActionLabel.setToolTipText(Translation.getTranslation(
                    "action_collapse_interface.name"));
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyInlineCloseInfoActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == inlineInfoCloseButton) {
                closeInlineInfoPanel();
            }
        }
    }

    private class MyOpenWebInterfaceAction extends BaseAction {

        private MyOpenWebInterfaceAction(Controller controller) {
            super("action_open_web_interface", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // PFC-2349 : Don't freeze UI
            getController().getIOProvider().startIO(new Runnable() {
                public void run() {
                    try {
                        BrowserLauncher.openURL(client
                            .getLoginURLWithCredentials());
                    } catch (IOException e1) {
                        logWarning("Unable to open web portal", e1);
                    }
                }
            });
        }
    }

    private class MyExpandCollapseAction extends BaseAction {

        private MyExpandCollapseAction(Controller controller) {
            super("action_expand_interface", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (frameMode == FrameMode.MAXIMIZED || frameMode == FrameMode.NORMAL) {
                setFrameMode(FrameMode.COMPACT);    
            } else {
                setFrameMode(FrameMode.NORMAL);
            }
        }

        public void setShowExpand(boolean expand) {
            if (expand) {
                configureFromActionId("action_expand_interface");
            } else {
                configureFromActionId("action_collapse_interface");
            }
        }
    }

    private static class MyOpenFoldersBaseAction extends BaseAction {

        private MyOpenFoldersBaseAction(Controller controller) {
            super("action_open_folders_base", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // PFC-2349 : Don't freeze UI
            getController().getIOProvider().startIO(new Runnable() {
                public void run() {
                    PathUtils.openFile(getController().getFolderRepository()
                        .getFoldersBasedir());
                }
            });
        }
    }

    private class MySetupAction extends BaseAction {

        protected MySetupAction(Controller controller) {
            super(null, controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (!getController().getNodeManager().isStarted()) {
                getApplicationModel().getLicenseModel().getActivationAction()
                    .actionPerformed(e);
            } else if (getController().getFolderRepository().getFoldersCount() == 0)
            {
                getApplicationModel().getActionModel().getNewFolderAction()
                    .actionPerformed(e);
            }
        }
    }

    private class MyPauseResumeAction extends BaseAction {

        private MyPauseResumeAction(Controller controller) {
            super("action_resume_sync", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getUIController().askToPauseResume();
            setLinkTooltips();
        }
    }

    private class MyOpenTransfersAction extends BaseAction {

        private MyOpenTransfersAction(Controller controller) {
            super("action_open_transfers_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (frameMode == FrameMode.COMPACT) {
                setFrameMode(FrameMode.NORMAL);
            }
            getUIController().openTransfersInformation();
        }
    }

    private class MyOpenDebugAction extends BaseAction {

        private MyOpenDebugAction(Controller controller) {
            super("action_open_debug_information", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (frameMode == FrameMode.COMPACT) {
                setFrameMode(FrameMode.NORMAL);
            }
            getUIController().openDebugInformation();
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            updateOnlineStorageDetails();
            checkCloudSpace();
        }

        public void login(ServerClientEvent event) {
            showOSFolderList();
            updateOnlineStorageDetails();
            checkCloudSpace();
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

    private class MyLoginAction extends BaseAction {

        MyLoginAction(Controller controller) {
            super("action_login", controller);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard.openLoginWizard(getController(), client);
        }
    }

    private class MyShowNoticesAction extends BaseAction {

        MyShowNoticesAction(Controller controller) {
            super("action_show_notices", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (frameMode == FrameMode.COMPACT) {
                setFrameMode(FrameMode.NORMAL);
            }
            getController().getUIController().openNoticesCard();
        }
    }

    private class MyMouseWindowDragListener extends MouseAdapter {
        private int startX;
        private int startY;
        private boolean inDrag;

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                if (isMaximized()) {
                    setFrameMode(FrameMode.NORMAL);
                } else {
                    setFrameMode(FrameMode.MAXIMIZED);
                }
            }
        }

        /** Called when the mouse has been pressed. */
        public void mousePressed(MouseEvent e) {
            if (isMaximized()) {
                // No drag allowed if maximized
                return;
            }
            Point p = e.getPoint();
            startX = p.x;
            startY = p.y;
            inDrag = true;
        }

        /** Called when the mouse has been released. */
        public void mouseReleased(MouseEvent e) {
            if (isMaximized()) {
                // No drag allowed if maximized
                return;
            }
            inDrag = false;
        }

        // And two methods from MouseMotionListener:
        public void mouseDragged(MouseEvent e) {
            if (isMaximized()) {
                // No drag allowed if maximized
                return;
            }
            Point p = e.getPoint();
            if (inDrag) {
                int dx = p.x - startX;
                int dy = p.y - startY;
                Point l = uiComponent.getLocation();
                uiComponent.setLocation(l.x + dx, l.y + dy);
            }
        }
    }

    /**
     * Handle drag-n-drops of Folders direct into the application.
     */
    @SuppressWarnings("serial")
    private class MyTransferHandler extends TransferHandler {
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            try {
                Transferable t = support.getTransferable();
                List<Path> fileList = (List<Path>)
                        t.getTransferData(DataFlavor.javaFileListFlavor);

                // One at a time!
                if (fileList == null || fileList.size() != 1) {
                    logInfo("Skipping importData (multiple).");
                    return false;
                }

                // Directories only.
                Path file = fileList.get(0);
                if (!Files.isDirectory(file)) {
                    logInfo("Skipping importData (not directory).");
                    return false;
                }

                // Does user have folder create permission?
                if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
                        .getValueBoolean(getController())
                        && !getController().getOSClient().getAccount()
                        .hasPermission(FolderCreatePermission.INSTANCE))
                {
                    logInfo("Skipping importData (no folder create permission).");
                    return false;
                }

                // Make sure we do not already have this as a folder.
                if (getController().getFolderRepository()
                        .doesFolderAlreadyExist(file)) {
                    logInfo("Skipping importData (already have).");
                    return false;
                }

                // All good then...
                PFWizard.openExistingDirectoryWizard(getController(), file);

            } catch (UnsupportedFlavorException e) {
                logSevere(e);
                return false;
            } catch (IOException e) {
                logSevere(e);
                return false;
            }
            return true;
        }
    }

    private static class MyOpenPreferencesAction extends BaseAction {

        private MyOpenPreferencesAction(Controller controller) {
            super("action_open_preferences", controller);
            allowWith(ChangePreferencesPermission.INSTANCE);
        }

        public void actionPerformed(ActionEvent e) {
            getUIController().openPreferences();
        }
    }

    private class MyPausedModeListener implements PausedModeListener {
        public void setPausedMode(PausedModeEvent event) {
            configurePauseResumeLink();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
