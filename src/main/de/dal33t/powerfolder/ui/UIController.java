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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.SynchronizationStatsListener;
import de.dal33t.powerfolder.event.SynchronizationStatsEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.chat.ChatFrame;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModelListener;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationFrame;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.notification.NotificationHandler;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The ui controller.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent {

    private static final Logger log = Logger.getLogger(UIController.class.getName());
    private static final long TEN_GIG = 10L << 30;

    private boolean started;
    private SplashScreen splash;
    private Image defaultIcon;
    private TrayIcon sysTrayMenu;
    private MainFrame mainFrame;
    private ChatModel chatModel;
    private SystemMonitorFrame systemMonitorFrame;
    private InformationFrame informationFrame;
    private ChatFrame chatFrame;

    private BlinkManager blinkManager;

    // List of pending jobs, execute when ui is opend
    private final List<Runnable> pendingJobs;
    private Menu sysTrayFoldersMenu;

    // The root of all models
    private ApplicationModel applicationModel;

    private boolean seenOome;

    private TransferManagerModel transferManagerModel;

    private final AtomicBoolean folderRepositorySynchronizing;

    /**
     * Initializes a new UI controller. open UI with #start
     * 
     * @param controller
     */
    public UIController(Controller controller) {
        super(controller);

        folderRepositorySynchronizing = new AtomicBoolean();

        configureOomeHandler();

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

        // Start listening for synchronization stats events.
        controller.getFolderRepository().addSynchronizationStatsListener(
                new MySynchronizationStatsListener());
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
        chatModel = applicationModel.getChatModel();
        blinkManager = new BlinkManager(getController(), chatModel);
        new ChatNotificationManager(chatModel);
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager());
        transferManagerModel.initialize();

        // now load
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    logFine("Building UI");
                    mainFrame.buildUI();
                    logFine("UI built");
                }
            });
        } catch (InterruptedException e) {
            logSevere("InterruptedException", e);
        } catch (InvocationTargetException e) {
            logSevere("InvocationTargetException", e);
        }

        // // Show window contents while dragging
        // Toolkit.getDefaultToolkit().setDynamicLayout(true);
        // System.setProperty("sun.awt.noerasebackground", "true");

        // Completely loaded now
        // setLoadingCompletion(100);

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

        // Show mainwindow
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
            hideSplash();
            PFWizard.openBasicSetupWizard(getController());
        }

        // Goes to the homepage if required.
        gotoHPIfRequired();
        detectAndShowLimitDialog();
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
        defaultIcon = Icons.getInstance().SYSTRAY_DEFAULT_ICON;
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

    /**
     * @return the model holding all chat data
     */
    public ChatModel getChatModel() {
        return chatModel;
    }

    public TransferManagerModel getTransferManagerModel() {
        return transferManagerModel;
    }

    /**
     * Shows an OutOfMemoryError to the user.
     * 
     * @param oome
     */
    public void showOutOfMemoryError(OutOfMemoryError oome) {
        if (!seenOome) {
            seenOome = true;
            int response = DialogFactory.genericDialog(mainFrame
                .getUIComponent(), Translation
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
        informationFrame.displayFolderFiles(folderInfo);
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
        applicationModel.getFolderRepositoryModel().syncFolder(folderInfo);
    }

    public void closeInformationFrame() {
        informationFrame.getUIComponent().setVisible(false);
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Class to listen for SynchronizationStatsEvents, affect the tooltip text.
     */
    private class MySynchronizationStatsListener implements
            SynchronizationStatsListener {

        public void synchronizationStatsChanged(SynchronizationStatsEvent event) {
            folderRepositorySynchronizing.set(event.isSynchronizing());
        }

        public boolean fireInEventDispatchThread() {
            // simple implementation, so do it now.
            return false;
        }
    }

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

    public BlinkManager getBlinkManager() {
        return blinkManager;
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
     * @param iconName
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

    private class ChatNotificationManager implements ChatModelListener {

        private ChatNotificationManager(ChatModel chatModel) {
            chatModel.addChatModelListener(this);
        }

        public void chatChanged(ChatModelEvent event) {
            if (event.isStatus()) {
                // Ignore status updates
                return;
            }
            notifyMessage(Translation.getTranslation("chat.notification.title"),
                    Translation.getTranslation("chat.notification.message"));
        }

        public boolean fireInEventDispatchThread() {
            return true;
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
                    + "\n\n" + text2);
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
     */
    public void notifyMessage(String title, String message) {

        if (mainFrame.isIconifiedOrHidden() && started
            && !getController().isShuttingDown())
        {
            NotificationHandler notificationHandler = new NotificationHandler(
                getController(), title, message);
            notificationHandler.show();
        }
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
        boolean runIfShown)
    {
        if (mainFrame.isIconifiedOrHidden() && started
            && !getController().isShuttingDown())
        {
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