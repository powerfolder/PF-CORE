/* $Id: UIController.java,v 1.86 2006/04/28 23:14:29 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.lang.StringUtils;

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
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.ui.action.*;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.folder.FileNameProblemHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.friends.AskForFriendshipHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.ui.recyclebin.RecycleBinConfirmationHandlerDefaultImpl;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.ui.webservice.ServerClientModel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.notification.NotificationHandler;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * The ui controller.
 * <P>
 * TODO Remove all Actions an put them into Models.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent {
    private static final LookAndFeel DEFAULT_LOOK_AND_FEEL = new PlasticXPLookAndFeel();

    private static final PlasticTheme DEFAULT_THEME = new ExperienceBlue();

    private SplashScreen splash;
    private Image defaultIcon;
    private Image currentIcon;
    private TrayIcon sysTrayMenu;
    private MainFrame mainFrame;
    private BlinkManager blinkManager;
    private ChatModel chatModel;
    private boolean started;
    // List of pending jobs, execute when ui is opend
    private List<Runnable> pendingJobs;

    // UI Models
    private ApplicationModel applicationModel;
    private NodeManagerModel nodeManagerModel;
    private FolderRepositoryModel folderRepoModel;
    private TransferManagerModel transferManagerModel;
    private ServerClientModel serverClientModel;
    private final ValueModel hidePreviewsVM;

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

        // create the Frame
        mainFrame = new MainFrame(getController());

        // create the models
        NavTreeModel navTreeModel = applicationModel.getNavTreeModel();
        chatModel = new ChatModel(getController());
        nodeManagerModel = new NodeManagerModel(getController(), navTreeModel,
            chatModel);
        blinkManager = new BlinkManager(getController(), chatModel);
        new ChatNotificationManager(getController(), chatModel);
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        folderRepoModel = new FolderRepositoryModel(getController(),
            navTreeModel);
        folderRepoModel.initalize();

        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager(), navTreeModel);
        transferManagerModel.initialize();
        serverClientModel = new ServerClientModel(getController(),
            getController().getOSClient());

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
                        !OSUtil.isSystraySupported()
                            || !getController().isStartMinimized());
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

        // Open wizard on first start
        if (getController().getPreferences().getBoolean("openwizard2", true)) {
            hideSplash();
            PFWizard.openBasicSetupWizard(getController());
            // Now never again, only on button
            getController().getPreferences().putBoolean("openwizard2", false);
            getController().getPreferences()
                .putBoolean("openwizard_os2", false);
        }
        if (getController().getPreferences().getBoolean("openwizard_os2", true)
            && !getController().isLanOnly())
        {
            hideSplash();
            PFWizard.openLoginWebServiceWizard(getController(), true);
            getController().getPreferences()
                .putBoolean("openwizard_os2", false);
        }

        // Goes to the homepage if required.
        gotoHPIfRequired();
        detectAndShowLimitDialog();

        hidePreviewsVM.setValue(ConfigurationEntry.HIDE_PREVIEW_FOLDERS
            .getValueBoolean(getController()));
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
        if (Util.isRunningProVersion()) {
            return;
        }
        long totalFolderSize = calculateTotalLocalSharedSize();
        log().debug(
            "Local shared folder size: " + Format.formatBytes(totalFolderSize));
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
            log().error(e);
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
                        log().warn("Unable to goto PowerFolder homepage", e1);
                    }
                }
            }
        };
        MenuItem item = menu.add(new MenuItem("PowerFolder.com"));
        item.setActionCommand("gotohp");
        item.addActionListener(systrayActionHandler);

        menu.addSeparator();

        item = menu.add(new MenuItem(Translation
            .getTranslation("systray.syncall")));
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
            log().error(e);
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

    public ValueModel getHidePreviewsValueModel() {
        return hidePreviewsVM;
    }

    public boolean isHidePreviews() {
        return (Boolean) hidePreviewsVM.getValue();
    }

    private class UpdateSystrayTask extends TimerTask {
        public void run() {
            String tooltip = Translation.getTranslation("general.powerfolder");
            tooltip += " ";
            if (getController().getFolderRepository().isAnyFolderTransferring())
            {
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
        if (!StringUtils.isBlank(iconName)) {
            // Install Icon if nessesary from jar
            String iconFileName = iconName;
            try {
                currentIcon = ImageIO.read(Util.getResource(iconFileName,
                    "icons"));
            } catch (IOException e) {
                log().error(e);
                return;
            }
            if (sysTrayMenu != null) {
                sysTrayMenu.setImage(currentIcon);
            }
        } else {
            if (sysTrayMenu != null) {
                sysTrayMenu.setImage(defaultIcon);
            }
            currentIcon = null;
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
            log().debug("Added runner to pending jobs: " + runner);
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

    private class ChatNotificationManager implements ChatModel.ChatModelListener {

        private Controller controller;

        private ChatNotificationManager(Controller controller,
                                         ChatModel chatModel) {
            this.controller = controller;
            chatModel.addChatModelListener(this);
        }

        public void chatChanged(ChatModel.ChatModelEvent event) {

            if (event.isStatus()) {
                // Ignore status updates
                return;
            }

            System.out.println(event.getMessage());
            
            if (event.getSource() instanceof Member) {
                Member m = (Member) event.getSource();
                notifyMessage(
                        Translation.getTranslation("chat.notification.title"),
                        Translation.getTranslation("chat.notification.member_message",
                        m.getNick()));
            } else {
                notifyMessage(
                        Translation.getTranslation("chat.notification.title"),
                        Translation.getTranslation("chat.notification.message"));
            }
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyFolderRepositoryListener implements FolderRepositoryListener {

        private final AtomicBoolean synchronizing = new AtomicBoolean();

        public void folderRemoved(FolderRepositoryEvent e) {
            checkStatus();
        }

        public void folderCreated(FolderRepositoryEvent e) {
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
         * Display folder synchronization info.
         * A copy of the MyFolders quick info panel text.
         */
        private void checkStatus() {
            long nTotalBytes = 0;
            FolderRepository repo = getController().getFolderRepository();
            Folder[] folders = repo.getFolders();

            int synchronizingFolders = 0;
            for (Folder folder : folders) {
                if (folder.isTransferring()) {
                    synchronizingFolders++;
                }
                nTotalBytes += folder.getStatistic().getTotalSize();
            }

            String text1;
            boolean changed = false;
            synchronized (synchronizing) {
                if (synchronizingFolders == 0) {
                    text1 = Translation.getTranslation("quickinfo.myfolders.in_sync_all");
                    if (synchronizing.get()) {
                        changed = true;
                        synchronizing.set(false);
                    }
                } else {
                    text1 = Translation.getTranslation("quickinfo.myfolders.syncing",
                            synchronizingFolders);
                    if (!synchronizing.get()) {
                        changed = true;
                        synchronizing.set(true);
                    }
                }
            }

            if (changed) {
                String text2 = Translation.getTranslation(
                    "quickinfo.myfolders.powerfolders", Format.formatBytes(nTotalBytes),
                        folders.length);

                notifyMessage(Translation
                        .getTranslation("quickinfo.myfolders.title"),
                        text1 + "\n\n" + text2);
            }
        }
    }



    /**
     * Shows a notification message only if the UI is minimized.
     *
     * @param title
     *          The title to display under 'PowerFolder'.
     * @param message
     *          Message to show if notification is displayed.
     */
    public void notifyMessage(String title, String message)
    {
        if (mainFrame.isIconifiedOrHidden() && started &&
                ConfigurationEntry.SHOW_NOTIFICATIONS.getValueBoolean(getController())) {
            NotificationHandler notificationHandler = new NotificationHandler(
                    getController(), title, message);
            notificationHandler.show();
        }
    }

    /**
     * Run a task via the notification system. If the UI is minimized,
     * a notification message will appear. If the user selects the accept button,
     * the task runs. If the UI is not minimized, the task runs anyway.
     *
     * @param title
     *          The title to display under 'PowerFolder'.
     * @param message
     *          Message to show if notification is displayed.
     * @param task
     *          Task to do if user selects 'accept' option or if UI is not minimized.
     */
    public void notifyMessage(String title, String message,
                              TimerTask task)
    {
        if (mainFrame.isIconifiedOrHidden() && started &&
                ConfigurationEntry.SHOW_NOTIFICATIONS.getValueBoolean(getController())) {
            NotificationHandler notificationHandler = new NotificationHandler(
                    getController(), title, message, task);
            notificationHandler.show();
        } else {
            task.run();
        }
    }


}