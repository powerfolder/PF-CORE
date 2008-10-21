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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.ui.action.ConnectAction;
import de.dal33t.powerfolder.ui.action.CreateShortcutAction;
import de.dal33t.powerfolder.ui.action.FindFriendAction;
import de.dal33t.powerfolder.ui.action.FolderCreateAction;
import de.dal33t.powerfolder.ui.action.FolderRemoveAction;
import de.dal33t.powerfolder.ui.action.OpenAboutBoxAction;
import de.dal33t.powerfolder.ui.action.OpenPreferencesAction;
import de.dal33t.powerfolder.ui.action.OpenWizardAction;
import de.dal33t.powerfolder.ui.action.PreviewFolderRemoveAction;
import de.dal33t.powerfolder.ui.action.PreviewJoinAction;
import de.dal33t.powerfolder.ui.action.ReconnectAction;
import de.dal33t.powerfolder.ui.action.RequestReportAction;
import de.dal33t.powerfolder.ui.action.SendInvitationAction;
import de.dal33t.powerfolder.ui.action.ShowHidePreviewFoldersAction;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.action.ToggleSilentModeAction;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.folder.FileNameProblemHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.friends.AskForFriendshipHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.ui.notification.NotificationHandler;
import de.dal33t.powerfolder.ui.recyclebin.RecycleBinConfirmationHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.ui.webservice.ServerClientModel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.TreeNodeList;
import org.apache.commons.lang.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
 * <P>
 * TODO Remove all Actions an put them into Models.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent {

    private static final Logger log = Logger.getLogger(UIController.class.getName());

    private static final LookAndFeel DEFAULT_LOOK_AND_FEEL = new PlasticXPLookAndFeel();

    private static final PlasticTheme DEFAULT_THEME = new ExperienceBlue();

    private boolean started;
    private SplashScreen splash;
    private Image defaultIcon;
    private TrayIcon sysTrayMenu;
    private MainFrame mainFrame;
    private BlinkManager blinkManager;

    // List of pending jobs, execute when ui is opend
    private List<Runnable> pendingJobs;
    private Menu sysTrayFoldersMenu;

    // The root of all models
    private ApplicationModel applicationModel;

    // TODO #278: UI Models: Move into ApplicationModel
    private ChatModel chatModel;
    private NodeManagerModel nodeManagerModel;
    private FolderRepositoryModel folderRepoModel;
    private TransferManagerModel transferManagerModel;
    private ServerClientModel serverClientModel;
    // TODO #278: Move into FolderRepoModel
    private final ValueModel hidePreviewsVM;
    private boolean seenOome;

    /**
     * Initializes a new UI controller. open UI with #start
     * 
     * @param controller
     */
    public UIController(Controller controller) {
        super(controller);

        configureOomeHandler();

        pendingJobs = Collections.synchronizedList(new LinkedList<Runnable>());

        // SwingHelper #365
        // RepaintManager.setCurrentManager(new
        // CheckThreadViolationRepaintManager());
        // EventDispatchThreadHangMonitor.initMonitoring();

        // Set properties for font policy (jgoodies looks)
        System.setProperty("Windows.controlFont", "Dialog-plain-11");
        System.setProperty("Windows.menuFont", "Dialog-plain-12");
        System.setProperty("Plastic.controlFont", "Dialog-plain-11");
        System.setProperty("Plastic.menuFont", "Dialog-plain-12");

        boolean defaultLFsupported = !(OSUtil.isWindowsMEorOlder() || OSUtil
            .isMacOS());
        if (defaultLFsupported) {
            boolean themeInitalized = false;
            try {
                // Now setup the theme
                if (getUIThemeConfig() != null) {
                    Class<?> themeClass = Class.forName(getUIThemeConfig());
                    PlasticTheme theme = (PlasticTheme) themeClass
                        .newInstance();
                    PlasticXPLookAndFeel.setPlasticTheme(theme);
                    themeInitalized = true;
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
            }

            if (!themeInitalized) {
                // Set default theme
                PlasticXPLookAndFeel.setPlasticTheme(DEFAULT_THEME);
            }

            try {
                // Set l&f
                UIManager.setLookAndFeel(DEFAULT_LOOK_AND_FEEL);
                // UIManager.put("Synthetica.tabbedPane.tab.selected.bold",
                // Boolean.FALSE);
                // UIManager.put("Synthetica.toolBar.button.font.style",
                // "PLAIN");
                // UIManager.put("Synthetica.toolBar.button.font.size", new
                // Integer(30));

                // SyntheticaLookAndFeel.setWindowsDecorated(false);
                // SyntheticaLookAndFeel.setExtendedFileChooserEnabled(false);
                // UIManager.setLookAndFeel(new
                // SyntheticaWhiteVisionLookAndFeel());
                // SyntheticaLookAndFeel.setFont("Dialog", 12);
                // UIManager.setLookAndFeel(new
                // SyntheticaBlackStarLookAndFeel());

            } catch (UnsupportedLookAndFeelException e) {
                logSevere("Unable to set look and feel", e);
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

        started = false;

        hidePreviewsVM = new ValueHolder();
        hidePreviewsVM.setValue(Boolean.FALSE);
        hidePreviewsVM.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ConfigurationEntry.HIDE_PREVIEW_FOLDERS.setValue(
                    getController(), Boolean.valueOf(
                        (Boolean) evt.getNewValue()).toString());
                getController().saveConfig();
                folderRepoModel.folderStructureChanged();
                getFolderRepositoryModel().getMyFoldersTableModel()
                    .folderStructureChanged();
            }
        });
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
        NavTreeModel navTreeModel = applicationModel.getNavTreeModel();
        chatModel = new ChatModel(getController());
        nodeManagerModel = new NodeManagerModel(getController(), navTreeModel,
            chatModel);
        blinkManager = new BlinkManager(getController(), chatModel);
        new ChatNotificationManager(chatModel);
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        folderRepoModel = new FolderRepositoryModel(getController(),
            navTreeModel);
        folderRepoModel.initalize();

        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager(), navTreeModel);
        transferManagerModel.initialize();
        applicationModel.getRootTabelModel().initialize();
        serverClientModel = new ServerClientModel(getController(),
            getController().getOSClient());

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

        hidePreviewsVM.setValue(ConfigurationEntry.HIDE_PREVIEW_FOLDERS
            .getValueBoolean(getController()));
    }

    private void gotoHPIfRequired() {
        if (Util.isRunningProVersion() && !Util.isTrial(getController())) {
            return;
        }
        String prefKey = "startCount" + Controller.PROGRAM_VERSION;
        int thisVersionStartCount = getController().getPreferences().getInt(
            prefKey, 0);
        // Go to HP every 20 starts
        if (thisVersionStartCount % 20 == 1) {
            try {
                BrowserLauncher.openURL(Constants.POWERFOLDER_PRO_URL);
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
        boolean limitHit = totalFolderSize > 10L * 1024L * 1024L * 1024L
            || getController().getFolderRepository().getFoldersCount() > 3;
        if (limitHit) {
            getController().getNodeManager().shutdown();
            getController().getIOProvider().shutdown();
            new FreeLimitationDialog(getController()).open();
        }
    }

    private long calculateTotalLocalSharedSize() {
        long totalSize = 0;
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            totalSize += folder.getStatistic().getSize(
                getController().getMySelf());
        }
        return totalSize;
    }

    private void initalizeSystray() {
        try {
            defaultIcon = ImageIO.read(Util.getResource(Icons.ST_POWERFOLDER,
                "icons"));
        } catch (IOException e) {
            logSevere("IOException", e);
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
                        BrowserLauncher.openURL(Constants.POWERFOLDER_URL);
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
                mainFrame.getUIComponent().setState(JFrame.NORMAL);
                // hack for jump to chat of member
                if (blinkManager.isMemberBlinking()) {
                    Member member = blinkManager.getABlinkingMember();
                    getControlQuarter().setSelected(member);
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(sysTrayMenu);
        } catch (AWTException e) {
            logSevere("AWTException", e);
            OSUtil.disableSystray();
            return;
        }
        getController().scheduleAndRepeat(new UpdateSystrayTask(), 5000);

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
        getController().getRecycleBin().setRecycleBinConfirmationHandler(
            new RecycleBinConfirmationHandlerDefaultImpl(getController()));
        FolderRepository repo = getController().getFolderRepository();
        repo
            .setInvitationReceivedHandler(new InvitationReceivedHandlerDefaultImpl(
                getController()));
        repo.setFileNameProblemHandler(new FileNameProblemHandlerDefaultImpl(
            getController()));
        getController().getNodeManager().setAskForFriendshipHandler(
            new AskForFriendshipHandlerDefaultImpl(getController()));

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

    public ValueModel getHidePreviewsValueModel() {
        return hidePreviewsVM;
    }

    public boolean isHidePreviews() {
        return (Boolean) hidePreviewsVM.getValue();
    }

    /**
     * Shows an OutOfMemoryError to the user.
     * 
     * @param oome
     */
    public void showOutOfMemoryError(OutOfMemoryError oome) {
        if (!seenOome) {
            seenOome = true;
            int response = DialogFactory.genericDialog(getMainFrame()
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

    private class UpdateSystrayTask extends TimerTask {
        public void run() {
            StringBuilder tooltip = new StringBuilder();

            tooltip.append(Translation.getTranslation("general.powerfolder"));
            tooltip.append(' ');
            if (getController().getFolderRepository().isAnyFolderTransferring())
            {
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
     * @return the setted ui theme as String (classname)
     */
    public String getUIThemeConfig() {
        return PreferencesEntry.UI_COLOUR_THEME.getValueString(getController());
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

    public ControlQuarter getControlQuarter() {
        return mainFrame.getControlQuarter();
    }

    public InformationQuarter getInformationQuarter() {
        return mainFrame == null ? null : mainFrame.getInformationQuarter();
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

    /**
     * @return the model holding all chat data
     */
    public ChatModel getChatModel() {
        return chatModel;
    }

    /**
     * @return the model representig the nodemanager
     */
    public NodeManagerModel getNodeManagerModel() {
        return nodeManagerModel;
    }

    public TransferManagerModel getTransferManagerModel() {
        return transferManagerModel;
    }

    /**
     * @return the model for the folder repository
     */
    public FolderRepositoryModel getFolderRepositoryModel() {
        return folderRepoModel;
    }

    /**
     * @return the model of the Online Storage client
     */
    public ServerClientModel getServerClientModel() {
        return serverClientModel;
    }

    // Systray interface/install code *****************************************

    /**
     * Sets the icon of the systray
     * 
     * @param iconName
     */
    public synchronized void setTrayIcon(String iconName) {
        if (!OSUtil.isSystraySupported()) {
            return;
        }
        if (StringUtils.isBlank(iconName)) {
            if (sysTrayMenu != null) {
                sysTrayMenu.setImage(defaultIcon);
            }
        } else {
            // Install Icon if nessesary from jar
            Image currentIcon;
            try {
                currentIcon = ImageIO.read(Util.getResource(iconName, "icons"));
            } catch (IOException e) {
                logSevere("IOException", e);
                return;
            }
            if (sysTrayMenu != null) {
                sysTrayMenu.setImage(currentIcon);
            }
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

    // Actions ****************************************************************

    // TODO Remove these actions and place them into the approriate model.
    // used primary in control quarter
    private Action openWizardAction;
    private Action connectAction;
    private Action openPreferencesAction;
    private Action folderCreateAction;
    private Action folderLeaveAction;
    private Action previewFolderRemoveAction;
    private Action previewJoinAction;
    private Action openAboutAction;
    private Action toggleSilentModeAction;
    private Action hidePreviewsAction;
    private Action findFriendAction;

    // on folders
    private Action syncAllFoldersAction;

    private Action inviteAction;

    // on members
    private Action requestReportAction;
    private Action reconnectAction;
    private Action createShortcutAction;

    public Action getOpenWizardAction() {
        if (openWizardAction == null) {
            openWizardAction = new OpenWizardAction(getController());
        }
        return openWizardAction;
    }

    public Action getConnectAction() {
        if (connectAction == null) {
            connectAction = new ConnectAction(getController());
        }
        return connectAction;
    }

    Action getOpenPreferencesAction() {
        if (openPreferencesAction == null) {
            openPreferencesAction = new OpenPreferencesAction(getController());
        }
        return openPreferencesAction;
    }

    public Action getFolderLeaveAction() {
        if (folderLeaveAction == null) {
            folderLeaveAction = new FolderRemoveAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return folderLeaveAction;
    }

    public Action getPreviewFolderRemoveAction() {
        if (previewFolderRemoveAction == null) {
            previewFolderRemoveAction = new PreviewFolderRemoveAction(
                getController(), getControlQuarter().getSelectionModel());
        }
        return previewFolderRemoveAction;
    }

    public Action getPreviewJoinAction() {
        if (previewJoinAction == null) {
            previewJoinAction = new PreviewJoinAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return previewJoinAction;
    }

    public Action getFolderCreateAction() {
        if (folderCreateAction == null) {
            folderCreateAction = new FolderCreateAction(getController());
        }
        return folderCreateAction;
    }

    public Action getOpenAboutAction() {
        if (openAboutAction == null) {
            openAboutAction = new OpenAboutBoxAction(getController());
        }
        return openAboutAction;
    }

    public Action getToggleSilentModeAction() {
        if (toggleSilentModeAction == null) {
            toggleSilentModeAction = new ToggleSilentModeAction(getController());
        }
        return toggleSilentModeAction;
    }

    public Action getHidePreviewsAction() {
        if (hidePreviewsAction == null) {
            hidePreviewsAction = new ShowHidePreviewFoldersAction(
                hidePreviewsVM, getController());
        }
        return hidePreviewsAction;
    }

    public Action getFindFriendAction() {
        if (findFriendAction == null) {
            findFriendAction = new FindFriendAction(getController());
        }
        return findFriendAction;
    }

    public Action getFolderCreateShortcutAction() {
        if (createShortcutAction == null) {
            createShortcutAction = new CreateShortcutAction(getController());
        }
        return createShortcutAction;
    }

    public Action getSyncAllFoldersAction() {
        if (syncAllFoldersAction == null) {
            syncAllFoldersAction = new SyncAllFoldersAction(getController());
        }
        return syncAllFoldersAction;
    }

    public Action getRequestReportAction() {
        if (requestReportAction == null) {
            requestReportAction = new RequestReportAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return requestReportAction;
    }

    public Action getReconnectAction() {
        if (reconnectAction == null) {
            reconnectAction = new ReconnectAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return reconnectAction;
    }

    public Action getInviteUserAction() {
        if (inviteAction == null) {
            inviteAction = new SendInvitationAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return inviteAction;
    }

    private class ChatNotificationManager implements
        ChatModel.ChatModelListener
    {

        private ChatNotificationManager(ChatModel chatModel) {
            chatModel.addChatModelListener(this);
        }

        public void chatChanged(ChatModel.ChatModelEvent event) {
            if (event.isStatus()) {
                // Ignore status updates
                return;
            }
            if (event.getSource() instanceof Member) {
                final Member m = (Member) event.getSource();

                TimerTask task = new TimerTask() {
                    public void run() {

                        // Find path to the chatting member.
                        TreeNodeList treeNodeList = getNodeManagerModel()
                            .getFriendsTreeNode();
                        int childCount = treeNodeList.getChildCount();
                        if (m != null) {
                            for (int i = 0; i < childCount; i++) {
                                TreeNode child = treeNodeList.getChildAt(i);
                                if (child != null
                                    && child instanceof DefaultMutableTreeNode)
                                {
                                    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) child;
                                    Object userObject = dmtn.getUserObject();
                                    if (userObject != null
                                        && userObject instanceof Member)
                                    {
                                        Member member = (Member) userObject;
                                        if (m.equals(member)) {
                                            // Found member, use as path.
                                            TreePath to = treeNodeList
                                                .getPathTo();
                                            Object[] path = new Object[3];
                                            path[0] = to.getPath()[0];
                                            path[1] = to.getPath()[1];
                                            path[2] = dmtn;
                                            TreePath tp = new TreePath(path);
                                            getControlQuarter()
                                                .getNavigationModel().setPath(
                                                    tp);
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        // If member not found for some reason, navigate to
                        // friend node.
                        getControlQuarter().getNavigationModel().setPath(
                            treeNodeList.getPathTo());

                    }
                };
                notifyMessage(Translation
                    .getTranslation("chat.notification.title"), Translation
                    .getTranslation("chat.notification.member_message", m
                        .getNick()), task, false);
            } else if (event.getSource() instanceof Folder) {
                final Folder f = (Folder) event.getSource();
                TimerTask task = new TimerTask() {
                    public void run() {

                        // Find path to the chatting folder.
                        TreeNodeList treeNodeList = getFolderRepositoryModel()
                            .getMyFoldersTreeNode();
                        int childCount = treeNodeList.getChildCount();
                        if (f != null) {
                            for (int i = 0; i < childCount; i++) {
                                TreeNode child = treeNodeList.getChildAt(i);
                                if (child != null
                                    && child instanceof TreeNodeList)
                                {
                                    TreeNodeList tnl = (TreeNodeList) child;
                                    Object userObject = tnl.getUserObject();
                                    if (userObject != null
                                        && userObject instanceof Folder)
                                    {
                                        Folder folder = (Folder) userObject;
                                        if (f.equals(folder)) {
                                            // Found folder, use as path.
                                            TreePath to = treeNodeList
                                                .getPathTo();
                                            Object[] path = new Object[3];
                                            path[0] = to.getPath()[0];
                                            path[1] = to.getPath()[1];
                                            path[2] = tnl;
                                            TreePath tp = new TreePath(path);
                                            getControlQuarter()
                                                .getNavigationModel().setPath(
                                                    tp);

                                            // Also select chat tab.
                                            getInformationQuarter()
                                                .displayChat(folder);
                                            return;
                                        }
                                    }
                                }
                            }
                        }

                        // If folder not found for some reason, navigate to
                        // folders node.
                        getControlQuarter().getNavigationModel().setPath(
                            treeNodeList.getPathTo());

                    }
                };

                notifyMessage(Translation
                    .getTranslation("chat.notification.title"), Translation
                    .getTranslation("chat.notification.message"), task, false);
            } else {
                notifyMessage(Translation
                    .getTranslation("chat.notification.title"), Translation
                    .getTranslation("chat.notification.message"));
            }
        }

        public boolean fireInEventDispathThread() {
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

        public boolean fireInEventDispathThread() {
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