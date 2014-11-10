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
 * $Id: UIController.java 20916 2013-02-23 04:05:10Z glasgow $
 */
package de.dal33t.powerfolder.ui;

import java.awt.EventQueue;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.problem.LocalDeletionProblem;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderAutoCreateEvent;
import de.dal33t.powerfolder.event.FolderAutoCreateListener;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.InvitationHandler;
import de.dal33t.powerfolder.event.LocalMassDeletionEvent;
import de.dal33t.powerfolder.event.MassDeletionHandler;
import de.dal33t.powerfolder.event.PausedModeEvent;
import de.dal33t.powerfolder.event.PausedModeListener;
import de.dal33t.powerfolder.event.RemoteMassDeletionEvent;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.ChangePreferencesPermission;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.ui.contextmenu.ContextMenuHandler;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.dialog.PauseDialog;
import de.dal33t.powerfolder.ui.dialog.SingleFileTransferDialog;
import de.dal33t.powerfolder.ui.iconoverlay.IconOverlayHandler;
import de.dal33t.powerfolder.ui.information.InformationFrame;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.model.BoundPermission;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.notices.FolderAutoCreateNotice;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.OutOfMemoryNotice;
import de.dal33t.powerfolder.ui.notices.SimpleNotificationNotice;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.notification.PreviewNotificationHandler;
import de.dal33t.powerfolder.ui.preferences.PreferencesDialog;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.NeverAskAgainResponse;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.util.update.UIUpdateHandler;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.BrowserLauncher.URLProducer;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.SystemUtil;
import de.dal33t.powerfolder.util.update.Updater;
import de.dal33t.powerfolder.util.update.UpdaterHandler;

/**
 * The ui controller.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent {

    public static final int MAIN_FRAME_ID = 0;
    public static final int INFO_FRAME_ID = 1;
    public static final int WIZARD_DIALOG_ID = 3;

    public static final int MAX_RECENTLY_CHANGED_FILES = 20;

    private static final String COMMAND_OPEN_UI = "open-ui";
    private static final String COMMAND_HIDE_UI = "hide-ui";
    private static final String COMMAND_SYNC_ALL = "sync-all";
    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_SYNC_SHUTDOWN = "sync-shutdown";
    private static final String COMMAND_SYNC_EXIT = "sync-exit";
    private static final String COMMAND_WEB = "web";
    private static final String COMMAND_PAUSE = "pause";
    private static final String COMMAND_RESUME = "resume";
    private static final String COMMAND_PREFERENCES = "preferences";
    private static final String COMMAND_BROWSE = "browse";
    private static final String COMMAND_RECENTLY_CHANGED = "recently-changed-";

    private boolean started;
    private SplashScreen splash;
    private TrayIconManager trayIconManager;
    private IconOverlayHandler iconOverlayHandler;
    private MainFrame mainFrame;
    private SystemMonitorFrame systemMonitorFrame;
    private final InformationFrame informationFrame;
    private WeakReference<JDialog> wizardDialogReference;

    // List of pending jobs, execute when ui is opened
    private final List<Runnable> pendingJobs;
    private Menu sysTrayFoldersMenu;
    private MenuItem pauseResumeMenu;
    private Menu recentlyChangedMenu;
    @SuppressWarnings("unused")
    private BoundPermission changePrefsPermission;

    // The root of all models
    private ApplicationModel applicationModel;

    private boolean seenOome;

    private TransferManagerModel transferManagerModel;
    private FolderListener folderListener;

    private final AtomicInteger activeFrame;
    private final AtomicBoolean synchronizing = new AtomicBoolean();
    private final DelayedUpdater statusUpdater;

    private final Map<Long, FileInfo> recentlyChangedFiles = new HashMap<Long, FileInfo>(
        MAX_RECENTLY_CHANGED_FILES);
    private final MenuItem[] recentMenuItems = new MenuItem[MAX_RECENTLY_CHANGED_FILES];
    private final PreferencesDialog preferencesDialog;
    /**
     * The UI distribution running.
     */
    private Skin[] skins;

    private Skin activeSkin;

    private final DelayedUpdater recentlyChangedUpdater = new DelayedUpdater(
        getController(), 5000L);

    /**
     * Initializes a new UI controller. open UI with #start
     *
     * @param controller
     */
    public UIController(Controller controller) {
        super(controller);

        activeFrame = new AtomicInteger();
        statusUpdater = new DelayedUpdater(getController(), 1000L);

        preferencesDialog = new PreferencesDialog(controller);

        configureOomeHandler();

        // Initialize look and feel / icon set
        initSkin();

        if (OSUtil.isMacOS()) {
            UIUtil.setMacDockImage(Icons.getImageById(Icons.LOGO128X128));
        }

        pendingJobs = Collections.synchronizedList(new LinkedList<Runnable>());

        if (!controller.isStartMinimized()) {
            // Show splash if not starting minimized
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        logFiner("Opening splash screen");
                        splash = new SplashScreen(getController(), 260 * 1000);
                    }
                });
            } catch (InterruptedException e) {
                logSevere("InterruptedException", e);
            } catch (InvocationTargetException e) {
                logSevere("InvocationTargetException", e);
            }
        }

        informationFrame = new InformationFrame(getController());
        if (Feature.SYSTEM_MONITOR.isEnabled()) {
            systemMonitorFrame = new SystemMonitorFrame(getController());
        }
        started = false;
    }

    /**
     * Configure a handler for OutOfMemoryErrors. Note that the Logger must be
     * configured to process Severe messages.
     */
    private void configureOomeHandler() {
        Handler oomeHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                Throwable throwable = record.getThrown();
                if (throwable instanceof OutOfMemoryError) {
                    OutOfMemoryError oome = (OutOfMemoryError) throwable;
                    showOutOfMemoryError(oome);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        Logger logger = Logger.getLogger("");
        logger.addHandler(oomeHandler);
    }

    /**
     * Starts the UI
     */
    public void start() {
        if (getController().isVerbose()) {
            // EventDispatchThreadHangMonitor.initMonitoring();
            // RepaintManager
            // .setCurrentManager(new CheckThreadViolationRepaintManager());
        }

        // PFC-2423
        if (PreferencesEntry.BEGINNER_MODE.getValueBoolean(getController())
            && !PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            // Configure view for beginner mode:
            ConfigurationEntry.FILES_ENABLED.setValue(getController(), false);
            ConfigurationEntry.SETTINGS_ENABLED
                .setValue(getController(), false);
            ConfigurationEntry.MEMBERS_ENABLED.setValue(getController(), false);
            // ConfigurationEntry.PROBLEMS_ENABLED.setValue(getController(),
            // false);
        } else {
            // Show it in Expert and Advanced mode
            ConfigurationEntry.FILES_ENABLED.setValue(getController(), true);
            ConfigurationEntry.SETTINGS_ENABLED.setValue(getController(), true);
            ConfigurationEntry.MEMBERS_ENABLED.setValue(getController(), true);
            // ConfigurationEntry.PROBLEMS_ENABLED.setValue(getController(),
            // false);
        }

        // The central application model
        applicationModel = new ApplicationModel(getController());
        applicationModel.initialize();

        // create the Frame
        mainFrame = new MainFrame(getController());

        // create the models
        folderListener = new MyFolderListener();
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            folder.addFolderListener(folderListener);
        }
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());

        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager());
        transferManagerModel.initialize();

        if (OSUtil.isLinux()) {
            // PFC-2331
            TrayIconManager.whitelistSystray(getController());
        }

        if (OSUtil.isSystraySupported()) {
            initializeSystray();
        } else {
            logWarning("System tray currently only supported on windows (>98)");
            mainFrame.getUIComponent().setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);
        }

        // PFC-2395: Start
        try {
            NativityControl nc = NativityControlUtil.getNativityControl();
            if (!nc.connect()) {
                logFine("Could not initialize for context menu!");
                nc.disconnect();
            } else {
                if (PreferencesEntry.ENABLE_CONTEXT_MENU
                    .getValueBoolean(getController()))
                {
                    ContextMenuControlUtil.getContextMenuControl(nc,
                        new ContextMenuHandler(getController()));
                }
    
                iconOverlayHandler = new IconOverlayHandler(getController());
                FileIconControlUtil.getFileIconControl(nc, iconOverlayHandler)
                    .enableFileIcons();
                iconOverlayHandler.start();
            }
        } catch (RuntimeException re) {
            logWarning("Context or file icons could not be loaded. " + re);
        }
        // PFC-2395: End

        if (getController().isStartMinimized() || PreferencesEntry.BEGINNER_MODE.getValueBoolean(getController())) {
            logInfo("Starting minimized");
        }

        // Show main window
        try {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    mainFrame.getUIComponent().setVisible(
                        (!OSUtil.isSystraySupported()
                            || !getController().isStartMinimized()) && !PreferencesEntry.BEGINNER_MODE.getValueBoolean(getController()));
                    if (!getController().isStartMinimized() && !PreferencesEntry.BEGINNER_MODE.getValueBoolean(getController())) {
                        mainFrame.toFront();
                    }
                }
            });
        } catch (InterruptedException e) {
            logSevere("InterruptedException", e);
        } catch (InvocationTargetException e) {
            logSevere("InvocationTargetException", e);
        }

        started = true;

        // Process all pending runners
        synchronized (pendingJobs) {
            if (!pendingJobs.isEmpty()) {
                logFiner("Executing " + pendingJobs.size() + " pending ui jobs");
                for (Runnable runner : pendingJobs) {
                    SwingUtilities.invokeLater(runner);
                }
            }
        }

        UpdaterHandler updateHandler = new UIUpdateHandler(getController());
        Updater.installPeriodicalUpdateCheck(getController(), updateHandler);

        getController().addMassDeletionHandler(new MyMassDeletionHandler());
        getController().addInvitationHandler(new MyInvitationHandler());
        getController().getFolderRepository().addFolderAutoCreateListener(
            new MyFolderAutoCreateListener());
    }

    public void askToPauseResume() {
        final boolean silent = getController().isPaused();
        if (silent) {
            // Resuming - nothing to ask.
            getController().schedule(new Runnable() {
                @Override
                public void run() {
                    getController().setPaused(!silent);
                }
            }, 0);
        } else {
            if (PreferencesEntry.SHOW_ASK_FOR_PAUSE
                .getValueBoolean(getController()))
            {
                PauseDialog pd = new PauseDialog(getController());
                pd.open();
            } else {
                getController().schedule(new Runnable() {
                    @Override
                    public void run() {
                        getController().setPaused(!silent);
                    }
                }, 0);
            }
        }

    }

    private void initializeSystray() {
        trayIconManager = new TrayIconManager(this);
        PopupMenu menu = new PopupMenu();

        TrayIcon trayIcon = trayIconManager.getTrayIcon();
        if (trayIcon != null) {
            trayIcon.setPopupMenu(menu);
        }

        ActionListener systrayActionHandler = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (COMMAND_OPEN_UI.equals(e.getActionCommand())) {
                    mainFrame.toFront();
                } else if (COMMAND_HIDE_UI.equals(e.getActionCommand())) {
                    mainFrame.getUIComponent().setVisible(false);
                } else if (COMMAND_EXIT.equals(e.getActionCommand())) {
                    // Exit to system
                    if (isShutdownAllowed()) {
                        getController().exit(0);
                    }
                } else if (COMMAND_SYNC_SHUTDOWN.equals(e.getActionCommand())) {
                    if (OSUtil.isLinux()) {
                        FormLayout layout = new FormLayout(
                            "pref, pref:grow, 3dlu, pref, pref",
                            "3dlu, pref, 3dlu, pref, 3dlu");
                        PanelBuilder builder = new PanelBuilder(layout);
                        CellConstraints cc = new CellConstraints();
                        builder.add(
                            new JLabel(Translation
                                .getTranslation("shutdown.message")), cc.xyw(2,
                                2, 3));
                        builder.add(
                            new JLabel(Translation
                                .getTranslation("shutdown.prompt")), cc
                                .xy(2, 4));
                        JPasswordField textField = new JPasswordField(20);
                        builder.add(textField, cc.xy(4, 4));
                        int i = DialogFactory.genericDialog(
                            getController(),
                            Translation.getTranslation("shutdown.title"),
                            builder.getPanel(),
                            new String[]{
                                Translation.getTranslation("general.ok"),
                                Translation.getTranslation("general.cancel")},
                            0, GenericDialogType.QUESTION);
                        if (i == 0) {
                            String password = Util.toString(textField
                                .getPassword());
                            getController().performFullSync();
                            getController().shutdownAfterSync(password);
                        }
                    } else {
                        getController().performFullSync();
                        getController().shutdownAfterSync(null);
                    }
                } else if (COMMAND_SYNC_EXIT.equals(e.getActionCommand())) {
                    getController().performFullSync();
                    getController().exitAfterSync(4);
                } else if (COMMAND_SYNC_ALL.equals(e.getActionCommand())) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            getController().performFullSync();
                        }
                    });
                } else if (COMMAND_WEB.equals(e.getActionCommand())) {
                    BrowserLauncher.open(getController(), new URLProducer() {
                        @Override
                        public String url() {
                            return getController().getOSClient()
                                .getLoginURLWithCredentials();
                        }
                    });
                } else if (COMMAND_BROWSE.equals(e.getActionCommand())) {
                    PathUtils.openFile(getController().getFolderRepository()
                        .getFoldersBasedir());
                } else if (COMMAND_PAUSE.equals(e.getActionCommand())
                    || COMMAND_RESUME.equals(e.getActionCommand()))
                {
                    askToPauseResume();
                } else if (COMMAND_PREFERENCES.equals(e.getActionCommand())) {
                    preferencesDialog.open();
                } else if (e.getActionCommand().startsWith(
                    COMMAND_RECENTLY_CHANGED))
                {
                    int index = e.getActionCommand().lastIndexOf('-');
                    String suffix = e.getActionCommand().substring(index + 1);
                    int item = Integer.valueOf(suffix);
                    synchronized (recentlyChangedFiles) {
                        int i = 0;
                        for (FileInfo fileInfo : recentlyChangedFiles.values())
                        {
                            if (i++ == item) {
                                // Open file in the file browser, checking if
                                // deleted.
                                UIController uiController = getController()
                                    .getUIController();
                                uiController.openFileInformation(fileInfo);
                                break;
                            }
                        }
                    }
                }
            }
        };

        // /////////////////////////
        // Open / close menu item //
        // /////////////////////////
        final MenuItem openUI = new MenuItem(
            Translation.getTranslation("systray.show"));
        menu.add(openUI);
        openUI.setActionCommand(COMMAND_OPEN_UI);
        openUI.addActionListener(systrayActionHandler);

        // //////
        // Web //
        // //////
        MenuItem item;
        if (ConfigurationEntry.WEB_LOGIN_ALLOWED
            .getValueBoolean(getController()))
        {
            item = menu.add(new MenuItem(Translation
                .getTranslation("action_open_web_interface.name")));
            item.setActionCommand(COMMAND_WEB);
            item.addActionListener(systrayActionHandler);
        }

        // //////////
        // Folders //
        // //////////
        sysTrayFoldersMenu = new Menu(
            Translation.getTranslation("general.folder"));
        sysTrayFoldersMenu.setEnabled(false);
        if (Feature.SYSTRAY_ALL_FOLDERS.isEnabled()) {
            menu.add(sysTrayFoldersMenu);
        }

        // /////////
        // Browse //
        // /////////
        item = menu.add(new MenuItem(Translation
            .getTranslation("action_open_folders_base.name")));
        item.setActionCommand(COMMAND_BROWSE);
        item.addActionListener(systrayActionHandler);

        // /////////////////
        // Pause / Resume //
        // /////////////////
        pauseResumeMenu = new MenuItem(
            Translation.getTranslation("action_resume_sync.name"));
        menu.add(pauseResumeMenu);
        pauseResumeMenu.addActionListener(systrayActionHandler);
        getController().addPausedModeListener(new MyPausedModeListener());
        configurePauseResumeLink();

        // /////////
        // Recent //
        // /////////
        recentlyChangedMenu = new Menu(
            Translation.getTranslation("uicontroller.recently_changed"));
        recentlyChangedMenu.setEnabled(false);
        menu.add(recentlyChangedMenu);
        for (int i = 0; i < MAX_RECENTLY_CHANGED_FILES; i++) {
            recentMenuItems[i] = new MenuItem();
            recentMenuItems[i].setActionCommand(COMMAND_RECENTLY_CHANGED + i);
            recentMenuItems[i].addActionListener(systrayActionHandler);
        }

        // //////////////
        // Preferences //
        // //////////////
        final MenuItem prefItem = menu.add(new MenuItem(Translation
            .getTranslation("action_open_preferences.name")));
        prefItem.setActionCommand(COMMAND_PREFERENCES);
        prefItem.addActionListener(systrayActionHandler);
        changePrefsPermission = new BoundPermission(getController(),
            ChangePreferencesPermission.INSTANCE)
        {
            @Override
            public void hasPermission(boolean hasPermission) {
                prefItem.setEnabled(hasPermission);
            }
        };

        menu.addSeparator();

        // ////////////////
        // Sync Shutdown //
        // ////////////////
        if (SystemUtil.isShutdownSupported()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            item = menu.add(new MenuItem(Translation
                .getTranslation("systray.sync_shutdown")));
            item.setActionCommand(COMMAND_SYNC_SHUTDOWN);
            item.addActionListener(systrayActionHandler);
        }

        // ////////////
        // Sync Exit //
        // ////////////
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            item = menu.add(new MenuItem(Translation
                .getTranslation("systray.sync_exit")));
            item.setActionCommand(COMMAND_SYNC_EXIT);
            item.addActionListener(systrayActionHandler);
        }

        // ///////
        // Exit //
        // ///////
        item = menu
            .add(new MenuItem(Translation.getTranslation("systray.exit")));
        item.setActionCommand(COMMAND_EXIT);
        item.addActionListener(systrayActionHandler);

        if (trayIcon != null) {
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainFrame.toFront();
                }
            });
        }

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            logSevere("Exception", e);
            OSUtil.disableSystray();
            return;
        }

        // Switch Systray show/hide menuitem dynamically
        mainFrame.getUIComponent().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent arg0) {
                openUI.setLabel(Translation.getTranslation("systray.hide"));
                openUI.setActionCommand(COMMAND_HIDE_UI);
            }

            @Override
            public void componentHidden(ComponentEvent arg0) {
                openUI.setLabel(Translation.getTranslation("systray.show"));
                openUI.setActionCommand(COMMAND_OPEN_UI);
            }
        });

        mainFrame.getUIComponent().addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                openUI.setLabel(Translation.getTranslation("systray.show"));
                openUI.setActionCommand(COMMAND_OPEN_UI);
            }
        });

        // Load initial folders in menu.
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            addFolderToSysTray(folder);
        }
    }

    /**
     * Add a folder to the SysTray menu structure.
     *
     * @param folder
     */
    private void addFolderToSysTray(Folder folder) {
        MenuItem menuItem = new MenuItem(folder.getName());
        // Insert in the correct position.
        boolean done = false;
        for (int i = 0; i < sysTrayFoldersMenu.getItemCount(); i++) {
            if (sysTrayFoldersMenu.getItem(i).getLabel().toLowerCase()
                .compareTo(folder.getName().toLowerCase()) > 0)
            {
                sysTrayFoldersMenu.insert(menuItem, i);
                done = true;
                break;
            }
        }
        if (!done) {
            sysTrayFoldersMenu.add(menuItem);
        }
        sysTrayFoldersMenu.setEnabled(true);
        final Path localBase = folder.getCommitOrLocalDir();
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Files.exists(localBase)) {
                    PathUtils.openFile(localBase);
                }
            }
        });
    }

    /**
     * Remove a folder from the SysTray menu structure.
     *
     * @param folder
     */
    private void removeFolderFromSysTray(Folder folder) {
        for (int i = 0; i < sysTrayFoldersMenu.getItemCount(); i++) {
            MenuItem menuItem = sysTrayFoldersMenu.getItem(i);
            if (menuItem.getLabel().equals(folder.getName())) {
                sysTrayFoldersMenu.remove(i);
            }
        }
        if (sysTrayFoldersMenu.getItemCount() == 0) {
            sysTrayFoldersMenu.setEnabled(false);
        }
    }

    public void hideSplash() {
        if (splash != null) {
            splash.shutdown();
        }
    }

    public TransferManagerModel getTransferManagerModel() {
        return transferManagerModel;
    }

    /**
     * @return the available skins - may be empty;
     */
    public Skin[] getSkins() {
        return skins;
    }

    /**
     * @return the active skin - may be null.
     */
    public Skin getActiveSkin() {
        return activeSkin;
    }

    private void initSkin() {

        List<Skin> skinList = new ArrayList<Skin>();

        // Now all skins (defaults + additional skins)
        ServiceLoader<Skin> skinLoader = ServiceLoader.load(Skin.class);
        for (Skin sk : skinLoader) {
            logFine("Loading skin " + sk.getName());
            skinList.add(sk);
        }

        skins = new Skin[skinList.size()];
        int i = 0;
        for (Skin skin : skinList) {
            // Check for dupes.
            for (int j = 0; j < i; j++) {
                if (skins[j].getName().equals(skin.getName())) {
                    logSevere("Multiple skins with name: " + skin.getName());
                }
            }
            skins[i++] = skin;
        }

        String skinName = PreferencesEntry.SKIN_NAME
            .getValueString(getController());
        boolean found = false;
        for (Skin skin : skins) {
            if (skin.getName().equals(skinName)) {
                activeSkin = skin;
                found = true;
                break;
            }
        }
        if (!found) {
            // Can not find one with this name - use the first one.
            activeSkin = skins[0];
            PreferencesEntry.SKIN_NAME.setValue(getController(),
                activeSkin.getName());
        }

        Properties props = activeSkin.getIconsProperties();
        if (props != null) {
            Icons.setIconProperties(props);
        }
        try {
            LookAndFeelSupport.setLookAndFeel(activeSkin.getLookAndFeel());
        } catch (UnsupportedLookAndFeelException | ParseException e) {
            logSevere(
                "Failed to set look and feel for skin " + activeSkin.getName(),
                e);
        }
    }

    /**
     * Shows an OutOfMemoryError to the user.
     *
     * @param oome
     */
    public void showOutOfMemoryError(OutOfMemoryError oome) {
        if (!seenOome) {
            seenOome = true;
            applicationModel.getNoticesModel().handleNotice(
                new OutOfMemoryNotice(oome));
        }
    }

    /**
     * Displays the information window if not already displayed.
     */
    public void displaySystemMonitorWindow() {
        if (systemMonitorFrame != null) {
            UIUtil.putOnScreen(systemMonitorFrame.getUIComponent());
            systemMonitorFrame.getUIComponent().setVisible(true);
        }
    }

    /**
     * Displays the information window if not already displayed.
     */
    private void displayInformationWindow() {
        mainFrame.showInlineInfoPanel((JPanel) informationFrame
            .getUIComponent().getContentPane(), informationFrame
            .getUIComponent().getTitle());
    }

    public void openFileInformation(FileInfo fileInfo) {
        if (ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController())) {
            informationFrame.displayFile(fileInfo);
            displayInformationWindow();
        }
    }

    /**
     * Opens the Files information for a folder.
     *
     * @param folderInfo
     *            info of the folder to display files information for.
     */
    public void openFilesInformationLatest(FolderInfo folderInfo) {
        if (ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController())) {
            informationFrame.displayFolderFilesLatest(folderInfo);
            displayInformationWindow();
        }
    }

    public void openFilesInformationDeleted(FolderInfo folderInfo) {
        if (ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController())) {
            informationFrame.displayFolderFilesDeleted(folderInfo);
            displayInformationWindow();
        }
    }

    public void openFilesInformationUnsynced(FolderInfo folderInfo) {
        if (ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController())) {
            informationFrame.displayFolderFilesUnsynced(folderInfo);
            displayInformationWindow();
        }
    }

    /**
     * Opens the Files information for a folder.
     *
     * @param folderInfo
     *            info of the folder to display files information for.
     * @return if the files information was actually opened
     */
    public boolean openFilesInformation(FolderInfo folderInfo) {
        if (ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController())) {
            informationFrame.displayFolderFiles(folderInfo);
            displayInformationWindow();
            return true;
        }
        return false;
    }

    /**
     * Opens the Settings information for a folder.
     *
     * @param folderInfo
     *            info of the folder to display member settings information for.
     */
    public void openSettingsInformation(FolderInfo folderInfo) {
        if (ConfigurationEntry.SETTINGS_ENABLED
            .getValueBoolean(getController()))
        {
            informationFrame.displayFolderSettings(folderInfo);
            displayInformationWindow();
        }
    }

    /**
     * Displays the Settings information move folder dialog.
     *
     * @param folderInfo
     *            info of the folder to display member settings information for.
     */
    public void moveLocalFolder(FolderInfo folderInfo) {
        informationFrame.moveLocalFolder(folderInfo);
    }

    /**
     * Opens the Members information for a folder.
     *
     * @param folderInfo
     *            info of the folder to display member computer information for.
     */
    public void openMembersInformation(FolderInfo folderInfo) {
        if (ConfigurationEntry.MEMBERS_ENABLED.getValueBoolean(getController()))
        {
            informationFrame.displayFolderMembers(folderInfo);
            displayInformationWindow();
        }
    }

    /**
     * Opens the Problems information for a folder.
     *
     * @param folderInfo
     *            info of the folder to display problems information for.
     */
    public void openProblemsInformation(FolderInfo folderInfo) {
        if (ConfigurationEntry.PROBLEMS_ENABLED
            .getValueBoolean(getController()))
        {
            informationFrame.displayFolderProblems(folderInfo);
            displayInformationWindow();
        }
    }

    public void openTransfersInformation() {
        informationFrame.displayTransfers();
        displayInformationWindow();
    }

    public void openDebugInformation() {
        informationFrame.displayDebug();
        displayInformationWindow();
    }

    public void openNoticesCard() {
        informationFrame.displayNotices();
        displayInformationWindow();
    }

    /**
     * Call when non-quitOnX close called. Hides child frames.
     */
    public void hideChildPanels() {
        informationFrame.getUIComponent().setVisible(false);
        if (systemMonitorFrame != null) {
            systemMonitorFrame.getUIComponent().setVisible(false);
        }
    }

    /**
     * Handles single file transfer requests. Displays dialog to send offer to
     * member.
     *
     * @param file
     * @param node
     */
    public void transferSingleFile(Path file, Member node) {
        SingleFileTransferDialog sftd = new SingleFileTransferDialog(
            getController(), file, node);
        sftd.open();
    }

    /**
     * Attention: If possible use method
     * {@link UIUtil#getParentWindow(ActionEvent)} to determine the active
     * window.
     * <p>
     * This returns most recently active PowerFolder frame. Possibly the
     * InformationFrame or (default) MainFrame. Used by dialogs, so focus does
     * not always jump to the wrong (Main) frame.
     * <P>
     *
     * @return the active frame.
     */
    public Window getActiveFrame() {

        int f = activeFrame.get();
        if (f == INFO_FRAME_ID) {
            JFrame infoComponent = informationFrame.getUIComponent();
            if (infoComponent.isVisible()) {
                return infoComponent;
            }
        } else if (f == WIZARD_DIALOG_ID) {
            if (wizardDialogReference != null) {
                JDialog wizardDialog = wizardDialogReference.get();
                if (wizardDialog != null) {
                    return wizardDialog;
                }
            }
        }

        // Default - main frame
        return mainFrame.getUIComponent();
    }

    public void setActiveFrame(int activeFrameId) {
        activeFrame.set(activeFrameId);
    }

    public void setWizardDialogReference(JDialog wizardDialog) {
        wizardDialogReference = new WeakReference<JDialog>(wizardDialog);
    }

    /**
     * Shuts the ui down
     */
    public void shutdown() {
        hideSplash();

        if (started) {
            informationFrame.getUIComponent().setVisible(false);
            informationFrame.getUIComponent().dispose();

            if (systemMonitorFrame != null) {
                systemMonitorFrame.storeValues();
                systemMonitorFrame.getUIComponent().setVisible(false);
                systemMonitorFrame.getUIComponent().dispose();
            }

            mainFrame.storeValues();
            mainFrame.getUIComponent().setVisible(false);
            mainFrame.getUIComponent().dispose();

            // Close systray
            if (OSUtil.isSystraySupported() && trayIconManager != null) {
                SystemTray.getSystemTray()
                    .remove(trayIconManager.getTrayIcon());
            }

            if (iconOverlayHandler != null) {
                iconOverlayHandler.stop();
            }
        }

        started = false;
    }

    /**
     * @return true if the ui controller is started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @return true if the information frame is showing a folder.
     */
    public boolean isShowingFolder() {
        return isShowingInfo() && informationFrame.isShowingFolder();
    }

    /**
     * @return true if the info panel is displayed currently
     */
    public boolean isShowingInfo() {
        return mainFrame.isShowingInfoInline();
    }

    /**
     * Sets the loading percentage
     *
     * @param percentage
     * @param nextPercentage
     */
    public void setLoadingCompletion(int percentage, int nextPercentage) {
        if (splash != null) {
            splash.setCompletionPercentage(percentage, nextPercentage);
        }
    }

    // Exposing ***************************************************************

    /**
     * @return the mainframe
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * For a more convenience way you can also use
     * PFUIComponent.getApplicationModel()
     *
     * @return the application model
     * @see PFUIComponent#getApplicationModel()
     */
    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    // Message dialog helpers *************************************************

    /**
     * Invokes a runner for later processing. It is ENSURED, that UI is open,
     * when the runner is executed
     *
     * @param runner
     */
    public void invokeLater(Runnable runner) {
        if (started) {
            SwingUtilities.invokeLater(runner);
        } else {
            logFine("Added runner to pending jobs: " + runner);
            // Add to pending jobs
            pendingJobs.add(runner);
        }
    }

    /**
     * Only use this for preview from the DialogSettingsTab.
     *
     * @param title
     * @param message
     */
    public void previewMessage(String title, String message) {
        PreviewNotificationHandler notificationHandler = new PreviewNotificationHandler(
            getController(), title, message);
        notificationHandler.show();
    }

    private void handleFolderAutoCreate(FolderAutoCreateEvent event) {
        applicationModel.getNoticesModel().handleNotice(
            new FolderAutoCreateNotice(event.getFolderInfo()));
    }

    /**
     * Scan results have been created after the user requested folder sync. So
     * give the user some feedback.
     *
     * @param scanResult
     */
    public void scanResultCreated(ScanResult scanResult) {
        // UI hidden?
        if (mainFrame == null || mainFrame.isIconifiedOrHidden()) {
            return;
        }
        int newSize = scanResult.getNewFiles().size();
        int changedSize = scanResult.getChangedFiles().size();
        int deletedSize = scanResult.getDeletedFiles().size();
        StringBuilder sb = new StringBuilder();
        sb.append(Translation.getTranslation("uicontroller.sync_info.start")
            + "\n\n" + '(');
        boolean addComma = false;
        if (newSize > 0) {
            sb.append(Translation.getTranslation("uicontroller.sync_info.new",
                String.valueOf(newSize)));
            addComma = true;
        }
        if (changedSize > 0) {
            if (addComma) {
                sb.append(", ");
            }
            sb.append(Translation.getTranslation(
                "uicontroller.sync_info.changed", String.valueOf(changedSize)));
            addComma = true;
        }
        if (deletedSize > 0) {
            if (addComma) {
                sb.append(", ");
            }
            sb.append(Translation.getTranslation(
                "uicontroller.sync_info.deleted", String.valueOf(deletedSize)));
        }
        if (newSize == 0 && changedSize == 0 && deletedSize == 0) {
            sb.append(Translation
                .getTranslation("uicontroller.sync_info.no_changes_detected"));
        }
        sb.append(')');
        if (newSize > 0 || changedSize > 0) {
            sb.append("\n\n");
            sb.append(Translation.getTranslation(
                "uicontroller.sync_info.transfer",
                String.valueOf(newSize + changedSize)));
        }
        DialogFactory.genericDialog(getController(),
            Translation.getTranslation("uicontroller.sync_info.title"),
            sb.toString(), GenericDialogType.INFO);
    }

    // public void clearBlink() {
    // if (trayIconManager != null) {
    // trayIconManager.clearBlink();
    // }
    // }

    /**
     * Special case. A folder has just been created from an invite. Switch to
     * the folder tab and crack open the new folder info.
     *
     * @param folderInfo
     */
    public void displayInviteFolderContents(FolderInfo folderInfo) {
        mainFrame.showFoldersTab();
        openFilesInformation(folderInfo);
    }

    private void configurePauseResumeLink() {
        if (getController().isPaused()) {
            pauseResumeMenu.setLabel(Translation
                .getTranslation("action_resume_sync.name"));
            pauseResumeMenu.setActionCommand(COMMAND_RESUME);
        } else {
            pauseResumeMenu.setLabel(Translation
                .getTranslation("action_pause_sync.name"));
            pauseResumeMenu.setActionCommand(COMMAND_PAUSE);
        }
    }

    private void checkStatus() {
        // logInfo("From", new RuntimeException());
        statusUpdater.schedule(new Runnable() {
            @Override
            public void run() {
                checkStatus0();
            }
        });
    }

    /**
     * Display folder synchronization info. A copy of the MyFolders quick info
     * panel text.
     */
    private void checkStatus0() {
        long nTotalBytes = 0;
        FolderRepository repo = getController().getFolderRepository();
        Collection<Folder> folders = repo.getFolders();

        int synchronizingFolders = 0;
        for (Folder folder : folders) {
            if (folder.isTransferring()
                || Double.compare(folder.getStatistic()
                    .getAverageSyncPercentage(), 100.0d) != 0)
            {
                synchronizingFolders++;
            }
            nTotalBytes += folder.getStatistic().getTotalSize();
        }

        String text1;
        boolean changed = false;
        synchronized (synchronizing) {
            if (synchronizingFolders == 0) {
                text1 = Translation.getTranslation("check_status.in_sync_all");
                if (synchronizing.get()) {
                    changed = true;
                    synchronizing.set(false);
                }
            }

            else {
                text1 = Translation.getTranslation("check_status.syncing",
                    String.valueOf(synchronizingFolders));
                if (!synchronizing.get()) {
                    changed = true;
                    synchronizing.set(true);
                }
            }
        }

        // Disabled popup of sync start.
        if (changed) {
            String text2 = Translation.getTranslation(
                "check_status.powerfolders", Format.formatBytes(nTotalBytes),
                String.valueOf(folders.size()));

            applicationModel.getNoticesModel().handleNotice(
                new SimpleNotificationNotice(Translation
                    .getTranslation("check_status.title"), text1 + "\n\n"
                    + text2));
        }
    }

    /**
     * Maintain a list of the most recently changed files.
     *
     * @param fileInfo
     */
    private void addRecentFileChange(FileInfo fileInfo) {
        if (recentlyChangedMenu == null) {
            return;
        }
        if (fileInfo.getFolderInfo().isMetaFolder()) {
            return;
        }
        Folder folder = fileInfo.getFolder(getController()
            .getFolderRepository());
        if (folder == null) {
            return;
        }
        if (folder.getDiskItemFilter().isExcluded(fileInfo)) {
            return;
        }
        synchronized (recentlyChangedFiles) {

            // Only keep latest version of any particular file; remove earlier
            // versions.
            for (Iterator<Long> iterator = recentlyChangedFiles.keySet()
                .iterator(); iterator.hasNext();)
            {
                Long next = iterator.next();
                FileInfo info = recentlyChangedFiles.get(next);
                if (fileInfo.getRelativeName().equals(info.getRelativeName())) {
                    iterator.remove();
                }
            }

            Long time = new Date().getTime();
            while (recentlyChangedFiles.containsKey(time)) {
                // Get a unique time for the key.
                time += 1;
            }
            recentlyChangedFiles.put(time, fileInfo);

            // Find the earliest change.
            if (recentlyChangedFiles.size() > MAX_RECENTLY_CHANGED_FILES) {
                Long first = recentlyChangedFiles.keySet().iterator().next();
                recentlyChangedFiles.remove(first);
            }
        }

        // Delay updating the actual menu so we don't spam the UI with multiple
        // updates.
        recentlyChangedUpdater.schedule(new Runnable() {
            @Override
            public void run() {

                // Update menu.
                synchronized (recentlyChangedFiles) {
                    recentlyChangedMenu.removeAll();
                    int i = 0;
                    for (FileInfo info : recentlyChangedFiles.values()) {
                        MenuItem menuItem = recentMenuItems[i++];
                        recentlyChangedMenu.add(menuItem);
                        menuItem.setLabel(info.getFilenameOnly());
                    }
                    recentlyChangedMenu.setEnabled(!recentlyChangedFiles
                        .isEmpty());
                }
            }
        });

    }

    public void closePreferencesDialog() {
        if (preferencesDialog != null) {
            preferencesDialog.close();
        }
    }

    public void openPreferences() {
        if (preferencesDialog != null) {
            preferencesDialog.open();
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        @Override
        public void folderRemoved(FolderRepositoryEvent e) {
            removeFolderFromSysTray(e.getFolder());
            e.getFolder().removeFolderListener(folderListener);
            checkStatus();
        }

        @Override
        public void folderCreated(FolderRepositoryEvent e) {
            addFolderToSysTray(e.getFolder());
            e.getFolder().addFolderListener(folderListener);
            checkStatus();
        }

        @Override
        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        @Override
        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    private class MyFolderListener extends FolderAdapter {

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public void statisticsCalculated(FolderEvent folderEvent) {
            // logWarning("Stats calced for " + folderEvent.getFolder(), new
            // RuntimeException());
            checkStatus();
        }

        @Override
        public void fileChanged(FolderEvent folderEvent) {
            Collection<FileInfo> collection = folderEvent.getScannedFileInfos();
            if (collection != null) {
                for (FileInfo fileInfo : collection) {
                    if (!fileInfo.isDiretory()) {
                        addRecentFileChange(fileInfo);
                    }
                }
            }
        }

        @Override
        public void filesDeleted(FolderEvent folderEvent) {
            Collection<FileInfo> collection = folderEvent.getDeletedFileInfos();
            if (collection != null) {
                for (FileInfo fileInfo : collection) {
                    if (!fileInfo.isDiretory()) {
                        addRecentFileChange(fileInfo);
                    }
                }
            }
        }
    }

    private class MyTransferManagerListener extends TransferManagerAdapter {

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public void downloadQueued(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void downloadStarted(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void downloadAborted(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void downloadBroken(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void downloadCompleted(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void uploadStarted(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void uploadAborted(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void uploadBroken(TransferManagerEvent event) {
            checkStatus();
        }

        @Override
        public void uploadCompleted(TransferManagerEvent event) {
            checkStatus();
        }

    }

    /**
     * Class to handle local and remote mass deletion events. This pushes
     * warnings into the app model.
     */
    private class MyMassDeletionHandler implements MassDeletionHandler {
        @Override
        public void localMassDeletion(LocalMassDeletionEvent event) {
            if (ProUtil.isZyncro(getController())) {
                LocalDeletionProblem ldp = new LocalDeletionProblem(event
                    .getFolder().getInfo(), event.getFile());
                event.getFolder().addProblem(ldp);
            }
        }

        @Override
        public void remoteMassDeletion(RemoteMassDeletionEvent event) {
            String message;
            if (event.isPercentage()) {
                message = Translation.getTranslation(
                    "uicontroller.remote_mass_delete.warning_message", event
                        .getMemberInfo().nick, String.valueOf(event
                        .getDeleteFigure()), event.getFolderInfo()
                        .getLocalizedName(), event.getOldProfile().getName(),
                    event.getNewProfile().getName());
            } else {
                message = Translation.getTranslation(
                    "uicontroller.remote_mass_delete.warning_absolute_message",
                    event.getMemberInfo().nick, String.valueOf(event
                        .getDeleteFigure()), event.getFolderInfo()
                        .getLocalizedName(), event.getOldProfile().getName(),
                    event.getNewProfile().getName());
            }

            WarningNotice notice = new WarningNotice(
                Translation.getTranslation("warning_notice.title"),
                Translation.getTranslation("warning_notice.mass_deletion",
                    event.getFolderInfo().getLocalizedName()), message);
            applicationModel.getNoticesModel().handleNotice(notice);
        }
    }

    private class MyInvitationHandler implements InvitationHandler {
        @Override
        public void gotInvitation(Invitation invitation) {
            boolean autoAccepted = false;

            if (ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS
                .getValueBoolean(getController()))
            {
                // Automatically accept this invitation, if possible.
                autoAccepted = getController().getFolderRepository()
                    .autoAcceptInvitation(invitation);
            }

            if (autoAccepted) {
                // Just tell the user what happened
                Notice notice = new SimpleNotificationNotice(
                    Translation.getTranslation("notice.invitation.title"),
                    Translation.getTranslation("notice.invitation.summary",
                        invitation.getInvitorUsername(),
                        invitation.folder.getLocalizedName()));
                applicationModel.getNoticesModel().handleNotice(notice);
            } else {
                // Let user decide what to do with the invitation.
                Notice notice = new InvitationNotice(
                    Translation.getTranslation("notice.invitation.title"),
                    Translation.getTranslation("notice.invitation.summary",
                        invitation.getInvitorUsername(),
                        invitation.folder.getLocalizedName()), invitation);
                applicationModel.getNoticesModel().handleNotice(notice);
            }
        }
    }

    private class MyFolderAutoCreateListener implements
        FolderAutoCreateListener
    {

        @Override
        public boolean fireInEventDispatchThread() {
            return true;
        }

        @Override
        public void folderAutoCreated(FolderAutoCreateEvent e) {
            handleFolderAutoCreate(e);
        }
    }

    /**
     * Can we shut down? If WARN_ON_CLOSE, let user know if there are any
     * folders still syncing.
     *
     * @return if all clear to shut down.
     */
    public boolean isShutdownAllowed() {
        boolean warnOnClose = PreferencesEntry.WARN_ON_CLOSE
            .getValueBoolean(getController());
        if (warnOnClose) {
            Collection<Folder> folderCollection = getController()
                .getFolderRepository().getFolders();
            List<Folder> foldersToWarn = new ArrayList<Folder>(
                folderCollection.size());
            for (Folder folder : folderCollection) {
                if (folder.isTransferring()) {
                    logWarning("Close warning on folder: " + folder);
                    foldersToWarn.add(folder);
                }
            }
            if (!foldersToWarn.isEmpty()) {
                StringBuilder folderslist = new StringBuilder();
                for (Folder folder : foldersToWarn) {
                    folderslist.append("\n     - "
                        + folder.getInfo().getLocalizedName());
                }
                String title = Translation
                    .getTranslation("uicontroller.warn_on_close.title");
                String text;
                if (applicationModel.getFolderRepositoryModel().isSyncing()) {
                    Date syncDate = applicationModel.getFolderRepositoryModel()
                        .getEstimatedSyncDate();
                    text = Translation.getTranslation(
                        "uicontroller.warn_on_close_eta.text",
                        folderslist.toString(),
                        Format.formatDateShort(syncDate));
                } else {
                    text = Translation.getTranslation(
                        "uicontroller.warn_on_close.text",
                        folderslist.toString());
                }
                String question = Translation
                    .getTranslation("general.neverAskAgain");
                NeverAskAgainResponse response = DialogFactory.genericDialog(
                    getController(),
                    title,
                    text,
                    new String[]{
                        Translation
                            .getTranslation("uicontroller.continue_exit"),
                        Translation.getTranslation("general.cancel")}, 0,
                    GenericDialogType.QUESTION, question);
                if (response.isNeverAskAgain()) {
                    PreferencesEntry.WARN_ON_CLOSE.setValue(getController(),
                        false);
                }
                return response.getButtonIndex() == 0;
            }

            // No folders unsynced
            return true;
        }

        // Do not warn on close, so we allow shut down
        return true;
    }

    private class MyPausedModeListener implements PausedModeListener {

        @Override
        public boolean fireInEventDispatchThread() {
            return true;
        }

        @Override
        public void setPausedMode(PausedModeEvent event) {
            configurePauseResumeLink();
        }
    }

}
