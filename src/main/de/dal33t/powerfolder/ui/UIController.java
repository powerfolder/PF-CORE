/* $Id: UIController.java,v 1.86 2006/04/28 23:14:29 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.lang.StringUtils;

import snoozesoft.systray4j.SysTrayMenu;
import snoozesoft.systray4j.SysTrayMenuEvent;
import snoozesoft.systray4j.SysTrayMenuIcon;
import snoozesoft.systray4j.SysTrayMenuItem;
import snoozesoft.systray4j.SysTrayMenuListener;

import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.ui.action.ConnectAction;
import de.dal33t.powerfolder.ui.action.CreateShortcutAction;
import de.dal33t.powerfolder.ui.action.FolderCreateAction;
import de.dal33t.powerfolder.ui.action.FolderLeaveAction;
import de.dal33t.powerfolder.ui.action.OpenAboutBoxAction;
import de.dal33t.powerfolder.ui.action.OpenPreferencesAction;
import de.dal33t.powerfolder.ui.action.OpenWizardAction;
import de.dal33t.powerfolder.ui.action.ReconnectAction;
import de.dal33t.powerfolder.ui.action.RequestReportAction;
import de.dal33t.powerfolder.ui.action.SendInvitationAction;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.action.ToggleSilentModeAction;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.folder.FileNameProblemHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.friends.AskForFriendshipHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.ui.recyclebin.RecycleBinConfirmationHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.ui.webservice.WebServiceClientModel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;

/**
 * The ui controller
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent implements SysTrayMenuListener {
    private static final LookAndFeel DEFAULT_LOOK_AND_FEEL = new PlasticXPLookAndFeel();
    private static final PlasticTheme DEFAULT_THEME = new ExperienceBlue();

    private SplashScreen splash;
    private SysTrayMenuIcon defaultIcon;
    private SysTrayMenuIcon currentIcon;
    private SysTrayMenu sysTrayMenu;
    private MainFrame mainFrame;
    private BlinkManager blinkManager;
    private NotificationManager notificationManager;
    private ChatModel chatModel;
    private boolean started;
    // List of pending jobs, execute when ui is opend
    private List<Runnable> pendingJobs;

    // UI Models
    private NodeManagerModel nodeManagerModel;
    private FolderRepositoryModel folderRepoModel;
    private TransferManagerModel transferManagerModel;
    private WebServiceClientModel webserviceClientModel;

    /**
     * Initializes a new UI controller. open UI with #start
     * 
     * @param controller
     */
    public UIController(Controller controller) {
        super(controller);

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
                    Class themeClass = Class.forName(getUIThemeConfig());
                    PlasticTheme theme = (PlasticTheme) themeClass
                        .newInstance();
                    PlasticXPLookAndFeel.setPlasticTheme(theme);
                    themeInitalized = true;
                }
            } catch (IllegalAccessException e) {
                log()
                    .error(
                        "Unable to set look and feel, switching back to default",
                        e);
            } catch (ClassNotFoundException e) {
                log()
                    .error(
                        "Unable to set look and feel, switching back to default",
                        e);
            } catch (InstantiationException e) {
                log()
                    .error(
                        "Unable to set look and feel, switching back to default",
                        e);
            }

            if (!themeInitalized) {
                // Set default theme
                PlasticXPLookAndFeel.setPlasticTheme(DEFAULT_THEME);
            }

            try {
                // Set l&f
                UIManager.setLookAndFeel(DEFAULT_LOOK_AND_FEEL);
            } catch (UnsupportedLookAndFeelException e) {
                log().error("Unable to set look and feel", e);
            }
        }

        if (!controller.isStartMinimized()) {
            // Show splash if not starting minimized
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        log().verbose("Opening splashscreen");
                        splash = new SplashScreen(getController(), 260 * 1000);
                    }
                });
            } catch (InterruptedException e) {
                log().error(e);
            } catch (InvocationTargetException e) {
                log().error(e);
            }
        }

        started = false;
    }

    /**
     * Starts the UI
     */
    public void start() {
        // set default implementations for handlers
        registerCoreHandlers();

        // create the Frame
        mainFrame = new MainFrame(getController());

        // install system tray files
        installNativeFiles();

        // create the models
        NavTreeModel navTreeModel = mainFrame.getControlQuarter()
            .getNavigationTreeModel();
        chatModel = new ChatModel(getController());
        nodeManagerModel = new NodeManagerModel(getController(), navTreeModel,
            chatModel);
        blinkManager = new BlinkManager(getController(), chatModel);
        notificationManager = new NotificationManager(getController());
        folderRepoModel = new FolderRepositoryModel(getController(),
            navTreeModel);
        folderRepoModel.initalize();

        transferManagerModel = new TransferManagerModel(getController());
        webserviceClientModel = new WebServiceClientModel(getController());

        // now load
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    log().debug("Building UI");
                    mainFrame.buildUI();
                    log().debug("UI built");
                }
            });
        } catch (InterruptedException e) {
            log().error(e);
        } catch (InvocationTargetException e) {
            log().error(e);
        }

        // // Show window contents while dragging
        // Toolkit.getDefaultToolkit().setDynamicLayout(true);
        // System.setProperty("sun.awt.noerasebackground", "true");

        // Completely loaded now
        // setLoadingCompletion(100);

        if (OSUtil.isSystraySupported()) {
            initalizeSystray();
        } else {
            log().warn("System tray currently only supported on windows (>98)");
            mainFrame.getUIComponent().setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);
        }

        if (getController().isStartMinimized()) {
            log().warn("Starting minimized");
        }

        // Show mainwindow
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    mainFrame.getUIComponent().setVisible(
                        !getController().isStartMinimized());
                }
            });
        } catch (InterruptedException e) {
            log().error(e);
        } catch (InvocationTargetException e) {
            log().error(e);
        }

        started = true;

        // Process all pending runners
        synchronized (pendingJobs) {
            if (!pendingJobs.isEmpty()) {
                log().verbose(
                    "Executing " + pendingJobs.size() + " pending ui jobs");
                for (Iterator it = pendingJobs.iterator(); it.hasNext();) {
                    Runnable runner = (Runnable) it.next();
                    SwingUtilities.invokeLater(runner);
                }
            }
        }

        // Do some eager intialization
        getController().schedule(new TimerTask() {
            @Override
            public void run()
            {
                DialogFactory.createFileChooser();
            }
        }, 0);

        // Open wizard on first start
        if (getController().getPreferences().getBoolean("openwizard2", true)) {
            hideSplash();

            PFWizard.openBasicSetupWizard(getController());
            // Now never again, only on button
            getController().getPreferences().putBoolean("openwizard2", false);
        }

        // Goes to the homepage if required.
        gotoHPIfRequired();
        detectAndShowLimitDialog();
    }

    private void gotoHPIfRequired() {
        if (Util.isRunningProVersion()) {
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
                log().warn("Unable to goto PowerFolder homepage", e1);
            }
        }
        thisVersionStartCount++;
        getController().getPreferences().putInt(prefKey, thisVersionStartCount);
    }

    private void detectAndShowLimitDialog() {
        if (Util.isRunningProVersion() || getController().isLanOnly()) {
            return;
        }
        long totalFolderSize = calculateTotalLocalSharedSize();
        log().debug(
            "Local shared folder size: " + Format.formatBytes(totalFolderSize));
        boolean limitHit = totalFolderSize > 10L * 1024L * 1024L * 1024L
            || getController().getFolderRepository().getFoldersCount() > 3;
        if (limitHit) {
            getController().getNodeManager().shutdown();
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
        // Not create systray on windows before 2000 systems
        String iconPath = Controller.getTempFilesLocation().getAbsolutePath()
            + "/";
        defaultIcon = new SysTrayMenuIcon(iconPath + Icons.ST_POWERFOLDER,
            "openui");
        sysTrayMenu = new SysTrayMenu(defaultIcon, getController().getMySelf()
            .getNick()
            + " | "
            + Translation.getTranslation("systray.powerfolder",
                Controller.PROGRAM_VERSION));

        SysTrayMenuItem exit = new SysTrayMenuItem(Translation
            .getTranslation("systray.exit"), "exit");
        exit.addSysTrayMenuListener(this);
        sysTrayMenu.addItem(exit);
        sysTrayMenu.addSeparator();

        final SysTrayMenuItem opentUI = new SysTrayMenuItem(Translation
            .getTranslation("systray.show"), "openui");
        opentUI.addSysTrayMenuListener(this);
        sysTrayMenu.addItem(opentUI);

        SysTrayMenuItem sync = new SysTrayMenuItem(Translation
            .getTranslation("systray.syncall"), "syncall");
        sync.addSysTrayMenuListener(this);
        sysTrayMenu.addItem(sync);
        sysTrayMenu.addSeparator();

        SysTrayMenuItem hpLabel = new SysTrayMenuItem("PowerFolder.com",
            "gotohp");
        sysTrayMenu.addItem(hpLabel);
        hpLabel.addSysTrayMenuListener(this);

        SysTrayMenuItem pfLabel = new SysTrayMenuItem(getController()
            .getMySelf().getNick()
            + " | "
            + Translation.getTranslation("systray.powerfolder",
                Controller.PROGRAM_VERSION), "gotohp");
        sysTrayMenu.addItem(pfLabel);
        pfLabel.addSysTrayMenuListener(this);

        defaultIcon.addSysTrayMenuListener(this);
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

    public void hideSplash() {
        if (splash != null) {
            splash.shutdown();
        }
    }

    private class UpdateSystrayTask extends TimerTask {
        public void run() {
            String tooltip = Translation.getTranslation("general.powerfolder");
            tooltip += " ";
            if (getController().getFolderRepository().isAnyFolderSyncing()) {
                tooltip += Translation
                    .getTranslation("systray.tooltip.syncing");
            } else {
                tooltip += Translation.getTranslation("systray.tooltip.insync");
            }
            double totalCPSdownKB = getController().getTransferManager()
                .getTotalDownloadTrafficCounter().calculateAverageCPS() / 1024;
            double totalCPSupKB = getController().getTransferManager()
                .getTotalUploadTrafficCounter().calculateAverageCPS() / 1024;

            String downText = null;
            String upText = null;

            if (totalCPSdownKB > 1024) {
                downText = Translation.getTranslation(
                    "systray.tooltip.down.mb", Format.NUMBER_FORMATS
                        .format(totalCPSdownKB / 1024));
            } else {
                downText = Translation.getTranslation("systray.tooltip.down",
                    Format.NUMBER_FORMATS.format(totalCPSdownKB));
            }

            if (totalCPSupKB > 1024) {
                upText = Translation.getTranslation("systray.tooltip.up.mb",
                    Format.NUMBER_FORMATS.format(totalCPSupKB / 1024));
            } else {
                upText = Translation.getTranslation("systray.tooltip.up",
                    Format.NUMBER_FORMATS.format(totalCPSupKB));
            }

            tooltip += " " + upText + " " + downText;
            sysTrayMenu.setToolTip(tooltip);
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
                sysTrayMenu.hideIcon();
                sysTrayMenu.removeAll();
                // DO not DISPOSE: #557
                // SysTrayMenu.dispose();
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
     * @return the model of the web service client
     */
    public WebServiceClientModel getWebServiceClientModel() {
        return webserviceClientModel;
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

        if (!StringUtils.isBlank(iconName)) {
            // Install Icon if nessesary from jar
            String iconFileName = iconName + SysTrayMenuIcon.getExtension();
            File icon = Util.copyResourceTo(iconFileName, "icons", Controller
                .getTempFilesLocation(), true);
            if (icon == null) {
                log().error("Icon not found: icons/" + iconFileName);
            } else {
                currentIcon = new SysTrayMenuIcon(icon.getAbsolutePath());
                currentIcon.addSysTrayMenuListener(this);
                if (sysTrayMenu != null) {
                    sysTrayMenu.setIcon(currentIcon);
                }
            }
        } else {
            if (sysTrayMenu != null) {
                sysTrayMenu.setIcon(defaultIcon);
            }
            if (currentIcon != null) {
                // Remove listener
                currentIcon.removeSysTrayMenuListener(this);
            }
            currentIcon = null;
        }
    }

    /**
     * Installs the needed libs and resources needed for system tray and other
     * native things
     */
    private void installNativeFiles() {
        if (!OSUtil.isWindowsSystem()) {
            // not install on non windows systems
            return;
        }
        // we don't use Systray on windows older than win2K
        if (!OSUtil.isWindowsMEorOlder()) {
            // create in current working dir:
            File systray = Util.copyResourceTo("systray4j.dll", "snoozesoft",
                new File("./"), true);
            if (systray == null) {
                log().error("Problem installing systray");
            }

            // copy PowerFolder icon from jar to temp dir
            File icon = Util.copyResourceTo("PowerFolder.ico", "icons",
                Controller.getTempFilesLocation(), true);
            if (icon == null) {
                log().warn("Problem installing icon");
            }
        }
        // copy delete dll to current working dirdir
        File delete = Util.copyResourceTo("delete.dll",
            "de/dal33t/powerfolder/util/os/Win32", new File("./"), true);
        if (delete == null) {
            log().error("Problem installing delete");
        }
    }

    public void menuItemSelected(final SysTrayMenuEvent e) {
        if ("openui".equals(e.getActionCommand())) {
            mainFrame.getUIComponent().setVisible(true);
        } else if ("hideui".equals(e.getActionCommand())) {
            mainFrame.getUIComponent().setVisible(false);
        } else if ("exit".equals(e.getActionCommand())) {
            // Exit to system
            getController().exit(0);
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
                log().warn("Unable to goto PowerFolder homepage", e1);
            }
        }
    }

    public void iconLeftClicked(SysTrayMenuEvent e) {
        // log().debug("iconLeftClicked event, command: " +
        // e.getActionCommand());
    }

    public void iconLeftDoubleClicked(SysTrayMenuEvent e) {
        // Double clicked, open gui directly
        mainFrame.getUIComponent().setVisible(true);
        mainFrame.getUIComponent().setState(JFrame.NORMAL);
        // hack for jump to chat of member
        if (blinkManager.isMemberBlinking()) {
            Member member = blinkManager.getABlinkingMember();
            getControlQuarter().setSelected(member);
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
        if (isStarted()) {
            SwingUtilities.invokeLater(runner);
        } else {
            log().debug("Added runner to pending jobs: " + runner);
            // Add to pending jobs
            pendingJobs.add(runner);
        }
    }

    /**
     * Displays a message dialog with the given parameters
     * <P>
     * TODO Refactor: Move this into DialogFactory
     * 
     * @param icon
     *            the message icon
     * @param title
     *            the dialog title
     * @param text
     *            the content text
     */
    public void showMessage(final Icon icon, final String title,
        final String text)
    {
        Runnable showMessage = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(getMainFrame().getUIComponent(),
                    text, title, JOptionPane.INFORMATION_MESSAGE, icon);
            }
        };
        invokeLater(showMessage);
    }

    /**
     * Displays a OK/Cancel dialog with a given text. UI has to be started
     * before this method is called.
     * <p>
     * FIXME: Wrap this into EventDispatcher thread
     * <P>
     * TODO Refactor: Move this into DialogFactory
     * 
     * @param icon
     *            the icon (can be null)
     * @param title
     *            the title
     * @param text
     *            the text
     * @return JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    public int showOKCancelDialog(final Icon icon, final String title,
        final String text)
    {
        if (!isStarted()) {
            log().error("Triing to show a dialog while UI not open!",
                new IllegalStateException());
        }

        return JOptionPane.showConfirmDialog(getMainFrame().getUIComponent(),
            text, title, JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE, icon);
    }

    /**
     * Displays a error message dialog with the given parameters
     * <P>
     * TODO Refactor: Move this into DialogFactory
     * 
     * @param title
     *            the dialog title
     * @param text
     *            the content text
     * @param throwable
     *            the optional throwable, may be empty
     */
    public void showErrorMessage(final String title, final String text,
        final Throwable throwable)
    {
        Runnable showMessage = new Runnable() {
            public void run() {
                String innerText = text;
                if (getController().isVerbose() && throwable != null) {
                    innerText += "\nReason: " + throwable;
                }
                JOptionPane.showMessageDialog(getMainFrame().getUIComponent(),
                    innerText, title, JOptionPane.ERROR_MESSAGE);
            }
        };
        invokeLater(showMessage);
    }

    /**
     * Displays a warning message dialog with the given parameters
     * <P>
     * TODO Refactor: Move this into DialogFactory
     * 
     * @param title
     *            the dialog title
     * @param text
     *            the content text
     */
    public void showWarningMessage(final String title, final String text) {
        Runnable showMessage = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(getMainFrame().getUIComponent(),
                    text, title, JOptionPane.WARNING_MESSAGE);
            }
        };
        invokeLater(showMessage);
    }

    // Actions ****************************************************************

    // used primary in control quarter
    private Action openWizardAction;
    private Action connectAction;
    private Action openPreferencesAction;
    private Action folderCreateAction;
    private Action folderLeaveAction;
    private Action openAboutAction;
    private Action toggleSilentModeAction;

    // on folders
    private Action syncAllFoldersAction;

    private Action inviteAction;

    // on members
    private Action requestReportAction;
    private Action reconnectAction;
    private Action createShortcutAction;

    Action getOpenWizardAction() {
        if (openWizardAction == null) {
            openWizardAction = new OpenWizardAction(getController());
        }
        return openWizardAction;
    }

    Action getConnectAction() {
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
            folderLeaveAction = new FolderLeaveAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return folderLeaveAction;
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
}