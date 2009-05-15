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

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.chat.ChatFrame;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationFrame;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.notification.NotificationHandler;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.dialog.SingleFileTransferDialog;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.ui.render.SysTrayBlinkManager;
import de.dal33t.powerfolder.ui.render.MainFrameBlinkManager;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * The ui controller.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent {

    private static final Logger log = Logger.getLogger(UIController.class.getName());
    private static final long TEN_GIG = 10L << 30;
    
    public static final int MAIN_FRAME_ID = 0;
    public static final int INFO_FRAME_ID = 1;
    public static final int CHAT_FRAME_ID = 2;
    public static final int WIZARD_DIALOG_ID = 3;

    private boolean started;
    private SplashScreen splash;
    private Image defaultIcon;
    private TrayIcon sysTrayMenu;
    private MainFrame mainFrame;
    private SystemMonitorFrame systemMonitorFrame;
    private InformationFrame informationFrame;
    private ChatFrame chatFrame;
    private WeakReference<JDialog> wizardDialogReference;

    // List of pending jobs, execute when ui is opend
    private final List<Runnable> pendingJobs;
    private Menu sysTrayFoldersMenu;

    // The root of all models
    private ApplicationModel applicationModel;

    private boolean seenOome;

    private TransferManagerModel transferManagerModel;

    private final AtomicBoolean folderRepositorySynchronizing;

    private final AtomicInteger activeFrame = new AtomicInteger();

    /**
     * The UI distribution running.
     */
    private Skin[] skins;

    private Skin activeSkin;

    /**
     * Initializes a new UI controller. open UI with #start
     * 
     * @param controller
     */
    public UIController(Controller controller) {
        super(controller);

        folderRepositorySynchronizing = new AtomicBoolean();

        configureOomeHandler();

        // Initialize look and feel / icon set
        initSkin();

        pendingJobs = Collections.synchronizedList(new LinkedList<Runnable>());

        boolean defaultLFsupported = !(OSUtil.isWindowsMEorOlder() ||
                OSUtil.isMacOS());
        if (defaultLFsupported) {
            boolean lafInitalized = false;
            try {
                // Now setup the theme
                if (getUILookAndFeelConfig() != null) {
                    Class<?> lafClass = Class.forName(getUILookAndFeelConfig());
                    LookAndFeel laf = (LookAndFeel) lafClass.newInstance();
                    LookAndFeelSupport.setLookAndFeel(laf);
                    lafInitalized = true;
                }
            } catch (IllegalAccessException e) {
                log.log(Level.SEVERE,
                    "Unable to set look and feel, switching back to default", e);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE,
                    "Unable to set look and feel, switching back to default", e);
            } catch (InstantiationException e) {
                log.log(Level.SEVERE,
                    "Unable to set look and feel, switching back to default", e);
            } catch (UnsupportedLookAndFeelException e) {
                log.log(Level.SEVERE,
                    "Unable to set look and feel, switching back to default", e);
            } catch (ClassCastException e) {
                log.log(Level.SEVERE,
                    "Unable to set look and feel, switching back to default", e);
            }

            if (!lafInitalized) {
                try {
                    // Set default l&f
                    LookAndFeelSupport.setLookAndFeel(new SyntheticaSilverMoonLookAndFeel());
                } catch (UnsupportedLookAndFeelException e) {
                    logSevere("Unable to set look and feel", e);
                } catch (ParseException e) {
                    logSevere("Unable to set look and feel", e);
                }
            }
        }

        if (!controller.isStartMinimized()) {
            // Show splash if not starting minimized
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        logFiner("Opening splashscreen");
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
        chatFrame = new ChatFrame(getController());
        systemMonitorFrame = new SystemMonitorFrame(getController());
        started = false;
    }

    /**
     * Configure a handler for OutOfMemoryErrors. Note that the Logger must be
     * configured to process Severe messages.
     */
    private void configureOomeHandler() {
        Handler oomeHandler = new Handler() {
            public void publish(LogRecord record) {
                Throwable throwable = record.getThrown();
                if (throwable instanceof OutOfMemoryError) {
                    OutOfMemoryError oome = (OutOfMemoryError) throwable;
                    showOutOfMemoryError(oome);
                }
            }

            public void flush() {
            }

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
        // set default implementations for handlers
        registerCoreHandlers();

        // The central application model
        applicationModel = new ApplicationModel(getController());
        applicationModel.initialize();

        // create the Frame
        mainFrame = new MainFrame(getController());

        // create the models
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager());
        transferManagerModel.initialize();

        if (OSUtil.isSystraySupported()) {
            initalizeSystray();
        } else {
            logWarning("System tray currently only supported on windows (>98)");
            mainFrame.getUIComponent().setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);
        }

        if (getController().isStartMinimized()) {
            logWarning("Starting minimized");
        }

        // Show main window
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    mainFrame.getUIComponent().setVisible(
                        !OSUtil.isSystraySupported()
                            || !getController().isStartMinimized());
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

        // Open wizard on first start. PRO version has activation wizard first
        if (!Util.isRunningProVersion()
            && getController().getPreferences().getBoolean("openwizard2", true))
        {
            UIUtil.invokeLaterInEDT(new Runnable() {

                // Don't block start!
                public void run() {
                    hideSplash();
                    PFWizard.openBasicSetupWizard(getController());
                }
            });
        }

        // Goes to the home page if required.
        gotoHPIfRequired();
        detectAndShowLimitDialog();

        // Start the blinkers later, so the UI is fully displayed first.
        UIUtil.invokeLaterInEDT(new Runnable() {
            public void run() {
                new SysTrayBlinkManager(UIController.this);
                new MainFrameBlinkManager(UIController.this);
            }
        });

        // Warn if there are any folders that have not been synchronized
        // recently.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                warnAboutOldSyncs();
            }
        });
    }

    /**
     * This creates a warning about folders that have not been synchronized
     * in a long time.
     */
    private void warnAboutOldSyncs() {
        if (!PreferencesEntry.FOLDER_SYNC_USE.getValueBoolean(getController())) {
            return;
        }

        // Calculate the date that folders should be synced by.
        Integer syncWarnDays =
                PreferencesEntry.FOLDER_SYNC_WARN.getValueInt(getController());
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -syncWarnDays);
        Date warningDate = cal.getTime();

        List<Folder> unsyncedFolders = new ArrayList<Folder>();

        for (Folder folder :
                getController().getFolderRepository().getFolders()) {
            Date lastSyncDate = folder.getLastSyncDate();
            if (lastSyncDate != null) {
                if (lastSyncDate.before(warningDate)) {
                    unsyncedFolders.add(folder);
                }
            }
        }

        if (unsyncedFolders.isEmpty()) {
            return;
        }

        String message;
        if (unsyncedFolders.size() == 1) {
            // Warn about a single unsynced folder.
            message = Translation.getTranslation(
                    "uicontroller.unsynchronized_folder.single",
                    unsyncedFolders.get(0).getInfo().name, syncWarnDays);
        } else {
            // Warn about multiple unsynced folders.
            message = Translation.getTranslation(
                    "uicontroller.unsynchronized_folder.multiple",
                    syncWarnDays);
        }
        WarningEvent warning = new WarningEvent(getController(),
                Translation.getTranslation(
                    "uicontroller.unsynchronized_folder.title"), message);
        applicationModel.getWarningsModel().pushWarning(warning);
    }

    private void gotoHPIfRequired() {
        if (Util.isRunningProVersion() && !Util.isTrial(getController())) {
            return;
        }
        String prefKey = "startCount" + Controller.PROGRAM_VERSION;
        int thisVersionStartCount = getController().getPreferences().getInt(
            prefKey, 0);
        // Go to HP every 20 starts
        if (thisVersionStartCount % 20 == 0) {
            try {
                BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_BUY_URL
                    .getValue(getController()));
            } catch (IOException e1) {
                log.log(Level.WARNING, "Unable to goto PowerFolder homepage", e1);
            }
        }
        thisVersionStartCount++;
        getController().getPreferences().putInt(prefKey, thisVersionStartCount);
    }

    private void detectAndShowLimitDialog() {
        if (Util.isRunningProVersion()) {
            return;
        }
        long totalFolderSize = calculateTotalLocalSharedSize();
        logFine("Local shared folder size: "
            + Format.formatBytes(totalFolderSize));
        boolean limitHit = totalFolderSize > TEN_GIG
            || getController().getFolderRepository().getFoldersCount() > 3;
        if (limitHit) {
            getController().getNodeManager().shutdown();
            getController().getIOProvider().shutdown();
            new FreeLimitationDialog(getController()).open();
        }
    }

    private long calculateTotalLocalSharedSize() {
        long totalSize = 0L;
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            totalSize += folder.getStatistic().getSize(
                getController().getMySelf());
        }
        return totalSize;
    }

    private void initalizeSystray() {
        defaultIcon = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
        if (defaultIcon == null) {
            logSevere("Unable to retrieve default system tray icon. "
                + "System tray disabled");
            OSUtil.disableSystray();
            return;
        }
        sysTrayMenu = new TrayIcon(defaultIcon);
        sysTrayMenu.setImageAutoSize(true);
        sysTrayMenu.setToolTip(getController().getMySelf().getNick()
            + " | "
            + Translation.getTranslation("systray.powerfolder",
                Controller.PROGRAM_VERSION));
        PopupMenu menu = new PopupMenu();

        sysTrayMenu.setPopupMenu(menu);

        ActionListener systrayActionHandler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ("openui".equals(e.getActionCommand())) {
                    mainFrame.getUIComponent().setVisible(true);
                } else if ("hideui".equals(e.getActionCommand())) {
                    mainFrame.getUIComponent().setVisible(false);
                } else if ("exit".equals(e.getActionCommand())) {
                    // Exit to system
                    getController().tryToExit(0);
                } else if ("syncall".equals(e.getActionCommand())) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            SyncAllFoldersAction.perfomSync(getController());
                        }
                    });
                } else if ("gotohp".equals(e.getActionCommand())) {
                    try {
                        BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_URL
                            .getValue(getController()));
                    } catch (IOException e1) {
                        log.log(Level.WARNING, "Unable to goto PowerFolder homepage", e1);
                    }
                }
            }
        };
        MenuItem item = menu.add(new MenuItem("PowerFolder.com"));
        item.setActionCommand("gotohp");
        item.addActionListener(systrayActionHandler);

        menu.addSeparator();

        Menu notificationsMenu = new Menu(Translation
                .getTranslation("systray.notifications"));
        menu.add(notificationsMenu);
        notificationsMenu.addActionListener(systrayActionHandler);

        final CheckboxMenuItem chatMenuItem = new CheckboxMenuItem(Translation
                .getTranslation("systray.notifications.chat"));
        notificationsMenu.add(chatMenuItem);
        chatMenuItem.setState((Boolean) applicationModel
                .getChatNotificationsValueModel().getValue());
        chatMenuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                applicationModel.getChatNotificationsValueModel().setValue(
                        chatMenuItem.getState());
            }
        });
        applicationModel.getChatNotificationsValueModel().addValueChangeListener(
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                chatMenuItem.setState((Boolean) evt.getNewValue());
            }
        });

        final CheckboxMenuItem systemMenuItem = new CheckboxMenuItem(Translation
                .getTranslation("systray.notifications.system"));
        notificationsMenu.add(systemMenuItem);
        systemMenuItem.setState((Boolean) applicationModel
                .getSystemNotificationsValueModel().getValue());
        systemMenuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                applicationModel.getSystemNotificationsValueModel().setValue(
                        systemMenuItem.getState());
            }
        });
        applicationModel.getSystemNotificationsValueModel().addValueChangeListener(
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                systemMenuItem.setState((Boolean) evt.getNewValue());
            }
        });

        sysTrayFoldersMenu = new Menu(Translation
            .getTranslation("general.powerfolder"));
        sysTrayFoldersMenu.setEnabled(false);
        if (OSUtil.isMacOS() || OSUtil.isWindowsSystem()) {
            menu.add(sysTrayFoldersMenu);
            menu.addSeparator();
        }

        item = menu.add(new MenuItem(Translation
            .getTranslation("systray.sync_all")));
        item.setActionCommand("syncall");
        item.addActionListener(systrayActionHandler);

        final MenuItem opentUI = menu.add(new MenuItem(Translation
            .getTranslation("systray.show")));
        opentUI.setActionCommand("openui");
        opentUI.addActionListener(systrayActionHandler);

        menu.addSeparator();

        item = menu
            .add(new MenuItem(Translation.getTranslation("systray.exit")));
        item.setActionCommand("exit");
        item.addActionListener(systrayActionHandler);

        sysTrayMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Previously was double click, this isn't supported by this
                // systray implementation
                // Double clicked, open gui directly
                mainFrame.getUIComponent().setVisible(true);
                mainFrame.getUIComponent().setState(Frame.NORMAL);
            }
        });

        try {
            SystemTray.getSystemTray().add(sysTrayMenu);
        } catch (AWTException e) {
            logSevere("AWTException", e);
            OSUtil.disableSystray();
            return;
        }
        getController().scheduleAndRepeat(new UpdateSystrayTask(), 5000L);

        // Switch Systray show/hide menuitem dynamically
        mainFrame.getUIComponent().addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent arg0) {
                opentUI.setLabel(Translation.getTranslation("systray.hide"));
                opentUI.setActionCommand("hideui");
            }

            public void componentHidden(ComponentEvent arg0) {
                opentUI.setLabel(Translation.getTranslation("systray.show"));
                opentUI.setActionCommand("openui");

            }
        });

        // Load initial folders in menu.
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            addFolderToSysTray(folder);
        }
    }

    /**
     * Registeres handlers/listeners for core callbacks
     */
    private void registerCoreHandlers() {
        FolderRepository repo = getController().getFolderRepository();
        repo.setFileNameProblemHandler(new FileNameProblemHandlerDefaultImpl(
            getController()));
    }

    /**
     * Add a folder to the SysTray menu structure.
     * 
     * @param folder
     */
    private void addFolderToSysTray(Folder folder) {
        for (int i = 0; i < sysTrayFoldersMenu.getItemCount(); i++) {
            MenuItem menuItem = sysTrayFoldersMenu.getItem(i);
            if (menuItem.getLabel().equals(folder.getName())) {
                logWarning("Already have folder " + folder.getName());
                return;
            }
        }

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
        final File localBase = folder.getLocalBase();
        final String folderName = folder.getName();
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (localBase.exists()) {
                    try {
                        FileUtils.openFile(localBase);
                    } catch (IOException e1) {
                        log.log(Level.WARNING, "Problem opening folder " + folderName, e1);
                    }
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
     * Returns the skin - may be empty;
     *
     * @return
     */
    public Skin[] getSkins() {
        return skins;
    }

    /**
     * Returns the active skin - may be null.
     * @return
     */
    public Skin getActiveSkin() {
        return activeSkin;
    }

    private void initSkin() {
        ServiceLoader<Skin> skinLoader = ServiceLoader.load(Skin.class);
        List<Skin> skinList = new ArrayList<Skin>();
        for (Skin sk : skinLoader) {
            skinList.add(sk);
        }

        skins = new Skin[skinList.size()];
        int i = 0;
        for (Skin skin : skinList) {
            skins[i++] = skin;
        }

        if (skins.length == 1) {
            // Single skin? - use this one.
            activeSkin = skins[0];
            String fileName = activeSkin.getIconsPropertiesFileName();
            Icons.loadOverrideFile(fileName);
        } else if (skins.length > 1) {
            // @todo harry - temporary - need a preference entry here
            activeSkin = skins[0];
            String fileName = activeSkin.getIconsPropertiesFileName();
            Icons.loadOverrideFile(fileName);
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
            int response = DialogFactory.genericDialog(getController(),
                Translation
                .getTranslation("low_memory.error.title"), Translation
                .getTranslation("low_memory.error.text"),
                new String[]{
                    Translation.getTranslation("general.ok"),
                    Translation
                        .getTranslation("dialog.already_running.exit_button")},
                0, GenericDialogType.ERROR);
            if (response == 1) { // Exit
                getController().exit(0);
            }
        }
    }

    /**
     * Displays the information window if not already displayed.
     */
    public void displaySystemMonitorWindow() {
        systemMonitorFrame.getUIComponent().setVisible(true);
    }

    /**
     * Displays the information window if not already displayed.
     */
    private void displayInformationWindow() {
        informationFrame.getUIComponent().setVisible(true);
    }

    /**
     * Opens the Files information for a folder.
     *
     * @param folderInfo info of the folder to display files information for.
     */
    public void openFilesInformation(FolderInfo folderInfo) {
        openFilesInformation(folderInfo, Integer.MIN_VALUE);
    }

    /**
     * Opens the Files information for a folder.
     *
     * @param folderInfo info of the folder to display files information for.
     */
    public void openFilesInformationLatest(FolderInfo folderInfo) {
        informationFrame.displayFolderFilesLatest(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Files information for a folder.
     *
     * @param folderInfo info of the folder to display files information for.
     * @param directoryFilterMode the directory filter mode to be in
     */
    public void openFilesInformation(FolderInfo folderInfo, int directoryFilterMode) {
        informationFrame.displayFolderFiles(folderInfo, directoryFilterMode);
        displayInformationWindow();
    }

    /**
     * Opens the Settings information for a folder.
     *
     * @param folderInfo info of the folder to display member settings
     * information for.
     */
    public void openSettingsInformation(FolderInfo folderInfo) {
        informationFrame.displayFolderSettings(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Memberss information for a folder.
     *
     * @param folderInfo info of the folder to display member computer
     * information for.
     */
    public void openMembersInformation(FolderInfo folderInfo) {
        informationFrame.displayFolderMembers(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Files information for a folder.
     *
     * @param memberInfo info of the folder to display files information for.
     */
    public void openChat(MemberInfo memberInfo) {
        chatFrame.displayChat(memberInfo);
        chatFrame.getUIComponent().setVisible(true);
    }

    public void openDownloadsInformation() {
        informationFrame.displayDownloads();
        displayInformationWindow();
    }

    public void openUploadsInformation() {
        informationFrame.displayUploads();
        displayInformationWindow();
    }

    public void openDebugInformation() {
        informationFrame.displayDebug();
        displayInformationWindow();
    }
    
    public void openInformationCard(InformationCard card) {
        informationFrame.displayCard(card);
        displayInformationWindow();
    }

    /**
     * Call when non-quitOnX close called. Hides child frames.
     */
    public void hideChildPanels() {
        informationFrame.getUIComponent().setVisible(false);
        chatFrame.getUIComponent().setVisible(false);
        systemMonitorFrame.getUIComponent().setVisible(false);
    }

    public void syncFolder(FolderInfo folderInfo) {
        Folder folder = getController().getFolderRepository().getFolder(folderInfo);

        if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder.getSyncProfile()))
        {
            // Ask for more sync options on that folder if on project sync
            new SyncFolderPanel(getController(), folder).open();
        } else {

            // Let other nodes scan now!
            folder.broadcastScanCommand();

            // Recommend scan on this
            folder.recommendScanOnNextMaintenance();

            // Now trigger the scan
            getController().getFolderRepository().triggerMaintenance();

            // Trigger file requesting.
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folderInfo);
        }
    }

    public void closeInformationFrame() {
        informationFrame.getUIComponent().setVisible(false);
    }

    /**
     * This method handles movement of the main frame and nudges any
     * MagneticFrames. USE_MAGNETIC_FRAMES pref xor control key activates this.
     *
     * @param diffX
     * @param diffY
     */
    public void mainFrameMoved(boolean controlKeyDown, int diffX, int diffY) {

        Boolean magnetic = PreferencesEntry.USE_MAGNETIC_FRAMES
                .getValueBoolean(getController());
        if (magnetic ^ controlKeyDown) {
            informationFrame.nudge(diffX,  diffY);
            chatFrame.nudge(diffX,  diffY);
            systemMonitorFrame.nudge(diffX,  diffY);
        }
    }

    /**
     * Handles single file transfer requests. Displays dialog to send offer to
     * member.
     *
     * @param file
     * @param node
     */
    public void transferSingleFile(File file, Member node) {
        SingleFileTransferDialog sftd = new SingleFileTransferDialog(
                getController(), file, node);
        sftd.open();
    }

    /**
     * This returns most recently active PowerFolder frame. Possibly
     * the InformationFrame, ChatFrame or (default) MainFrame. Used by dialogs,
     * so focus does not always jump to the wrong (Main) frame.
     *
     * @return
     */
    public Window getActiveFrame() {

        int f = activeFrame.get();
        if (f == INFO_FRAME_ID) {
            JFrame infoComponent = informationFrame.getUIComponent();
            if (infoComponent.isVisible()) {
                return infoComponent;
            }
        } else if (f == CHAT_FRAME_ID) {
            JFrame chatComponent = chatFrame.getUIComponent();
            if (chatComponent.isVisible()) {
                return chatComponent;
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
     * Hide the Online Storage lines in the home tab.
     */
    public void hideOSLines() {
        mainFrame.hideOSLines();
    }

    /**
     * Handle the case where all files have been deleted from a folder.
     * Perhaps the user actually wants to stop managing the folder?
     *
     * @param folder
     */
    public void handleTotalFolderDeletion(final Folder folder) {
        Runnable runnable = new Runnable() {
            public void run() {
                int result = DialogFactory.genericDialog(getController(),
                        Translation.getTranslation(
                                "uicontroller.empty_folder.title"),
                        Translation.getTranslation(
                                "uicontroller.empty_folder.message",
                                folder.getName()), new String[]{
                        Translation.getTranslation(
                                "uicontroller.empty_folder.stop_managing"),
                        Translation.getTranslation(
                                "uicontroller.empty_folder.send_deletions")},
                        1, "Total-Folder-Delete", GenericDialogType.WARN);
                if (result == 0) { // Leave folder
                    logInfo("User decided to leave forlder "
                            + folder.getInfo().name
                            + " because all files are deleted.");
                    getController().getFolderRepository().removeFolder(
                            folder, true);
                } else { // Broadcast as usual.
                    folder.scanLocalFiles(true);
                }
            }
        };
        WarningEvent we = new WarningEvent("local folder delete", runnable);
        getController().pushWarningEvent(we);

    }

    public boolean chatFrameVisible() {
        return chatFrame.getUIComponent().isVisible();
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class UpdateSystrayTask extends TimerTask {
        public void run() {
            StringBuilder tooltip = new StringBuilder();

            tooltip.append(Translation.getTranslation("general.powerfolder"));
            tooltip.append(' ');
            if (folderRepositorySynchronizing.get()) {
                tooltip.append(Translation
                    .getTranslation("systray.tooltip.syncing"));
            } else {
                tooltip.append(Translation
                    .getTranslation("systray.tooltip.in_sync"));
            }
            double totalCPSdownKB = getController().getTransferManager()
                .getDownloadCounter().calculateAverageCPS() / 1024;
            double totalCPSupKB = getController().getTransferManager()
                .getUploadCounter().calculateAverageCPS() / 1024;

            String downText;

            if (totalCPSdownKB > 1024) {
                downText = Translation.getTranslation(
                    "systray.tooltip.down.mb", Format.getNumberFormat().format(
                        totalCPSdownKB / 1024));
            } else {
                downText = Translation.getTranslation("systray.tooltip.down",
                    Format.getNumberFormat().format(totalCPSdownKB));
            }

            String upText;
            if (totalCPSupKB > 1024) {
                upText = Translation.getTranslation("systray.tooltip.up.mb",
                    Format.getNumberFormat().format(totalCPSupKB / 1024));
            } else {
                upText = Translation.getTranslation("systray.tooltip.up",
                    Format.getNumberFormat().format(totalCPSupKB));
            }

            tooltip.append(' ' + upText + ' ' + downText);
            sysTrayMenu.setToolTip(tooltip.toString());
        }
    }

    /**
     * Shuts the ui down
     */
    public void shutdown() {
        hideSplash();

        if (started) {
            informationFrame.storeValues();
            informationFrame.getUIComponent().setVisible(false);
            informationFrame.getUIComponent().dispose();

            chatFrame.storeValues();
            chatFrame.getUIComponent().setVisible(false);
            chatFrame.getUIComponent().dispose();

            systemMonitorFrame.storeValues();
            systemMonitorFrame.getUIComponent().setVisible(false);
            systemMonitorFrame.getUIComponent().dispose();

            mainFrame.storeValues();
            mainFrame.getUIComponent().setVisible(false);
            mainFrame.getUIComponent().dispose();

            // Close systray
            if (OSUtil.isSystraySupported()) {
                SystemTray.getSystemTray().remove(sysTrayMenu);
            }
        }

        started = false;
    }

    /**
     * @return the setted ui laf as String (classname)
     */
    public String getUILookAndFeelConfig() {
        return PreferencesEntry.UI_LOOK_AND_FEEL.getValueString(getController());
    }

    /**
     * @return true if the ui controller is started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @return the controller
     */
    public Controller getController() {
        return super.getController();
    }

    /**
     * Sets the loading percentage
     * 
     * @param percentage
     * @param nextPerc
     */
    public void setLoadingCompletion(int percentage, int nextPerc) {
        if (splash != null) {
            splash.setCompletionPercentage(percentage, nextPerc);
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

    // Systray interface/install code *****************************************

    /**
     * Sets the icon of the systray
     * 
     * @param icon
     */
    public synchronized void setTrayIcon(Image icon) {
        if (!OSUtil.isSystraySupported()) {
            return;
        }
        if (sysTrayMenu == null) {
            return;
        }
        if (icon == null) {
            sysTrayMenu.setImage(defaultIcon);
        } else {
            sysTrayMenu.setImage(icon);
        }
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

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        private final AtomicBoolean synchronizing = new AtomicBoolean();

        public void folderRemoved(FolderRepositoryEvent e) {
            removeFolderFromSysTray(e.getFolder());
            checkStatus();
        }

        public void folderCreated(FolderRepositoryEvent e) {
            addFolderToSysTray(e.getFolder());
            checkStatus();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            checkStatus();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            checkStatus();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        /**
         * Display folder synchronization info. A copy of the MyFolders quick
         * info panel text.
         */
        private void checkStatus() {
            long nTotalBytes = 0;
            FolderRepository repo = getController().getFolderRepository();
            Folder[] folders = repo.getFolders();

            int synchronizingFolders = 0;
            for (Folder folder : folders) {
                if (folder.isTransferring()
                    || Double.compare(folder.getStatistic()
                        .getTotalSyncPercentage(), 100.0d) != 0)
                {
                    synchronizingFolders++;
                }
                nTotalBytes += folder.getStatistic().getTotalSize();
            }

            String text1;
            boolean changed = false;
            synchronized (synchronizing) {
                if (synchronizingFolders == 0) {
                    text1 = Translation
                        .getTranslation("quickinfo.my_folders.in_sync_all");
                    if (synchronizing.get()) {
                        changed = true;
                        synchronizing.set(false);
                    }
                }

                else {
                    text1 = Translation.getTranslation(
                        "quickinfo.my_folders.syncing", synchronizingFolders);
                    if (!synchronizing.get()) {
                        changed = true;
                        synchronizing.set(true);
                    }
                }
            }

            // Disabled popup of sync start.
            if (changed
                && ConfigurationEntry.SHOW_SYSTEM_NOTIFICATIONS
                    .getValueBoolean(getController()))
            {
                String text2 = Translation.getTranslation(
                    "quickinfo.my_folders.powerfolders", Format
                        .formatBytes(nTotalBytes), folders.length);

                notifyMessage(Translation
                    .getTranslation("quickinfo.my_folders.title"), text1
                    + "\n\n" + text2, false);
            }
        }
    }

    /**
     * Shows a notification message only if the UI is minimized.
     *
     * @param title
     *            The title to display under 'PowerFolder'.
     * @param message
     *            Message to show if notification is displayed.
     * @param chat
     *           True if this is a chat message,
     *           otherwise it is a system message
     */
    public void notifyMessage(String title, String message, boolean chat) {
        if (started && mainFrame.isIconifiedOrHidden()
            && !getController().isShuttingDown()) {
            if (chat ? (Boolean) applicationModel
                    .getChatNotificationsValueModel().getValue() : (Boolean)
                    applicationModel.getSystemNotificationsValueModel()
                            .getValue()) {
                NotificationHandler notificationHandler = new NotificationHandler(
                    getController(), title, message, true);
                notificationHandler.show();
            }
        }
    }

    /**
     * Only use this for preview from the DialogSettingsTab.
     * It by-passes all the usual safty checks.
     *
     * @param title
     * @param message
     */
    public void previewMessage(String title, String message) {

            NotificationHandler notificationHandler = new NotificationHandler(
                getController(), title, message, false);
            notificationHandler.show();
    }

    /**
     * Run a task via the notification system. If the UI is minimized, a
     * notification message will appear. If the user selects the accept button,
     * the task runs. If the UI is not minimized, the task runs anyway.
     * 
     * @param title
     *            The title to display under 'PowerFolder'.
     * @param message
     *            Message to show if notification is displayed.
     * @param task
     *            Task to do if user selects 'accept' option or if UI is not
     *            minimized.
     * @param runIfShown
     *            Whether to run the task if PF is already shown.
     */
    public void notifyMessage(String title, String message, TimerTask task,
        boolean runIfShown) {
        if (started && mainFrame.isIconifiedOrHidden() &&
                !getController().isShuttingDown() && (Boolean)
                applicationModel.getSystemNotificationsValueModel()
                        .getValue()) {
            NotificationHandler notificationHandler = new NotificationHandler(
                getController(), title, message, task);
            notificationHandler.show();
        } else {
            if (runIfShown) {
                task.run();
            }
        }
    }
}