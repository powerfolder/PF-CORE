/* $Id: UIController.java,v 1.86 2006/04/28 23:14:29 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;

import org.apache.commons.lang.StringUtils;

import snoozesoft.systray4j.*;

import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.action.*;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.render.BlinkManager;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.*;

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
    private ChatModel chatModel;
    private boolean started;

    // List of pending jobs, execute when ui is opend
    private List pendingJobs;

    /**
     * Initializes a new UI controller. open UI with #start
     * 
     * @param controller
     * @throws UIException
     */
    public UIController(Controller controller) {
        super(controller);
        pendingJobs = Collections.synchronizedList(new LinkedList());

        boolean defaultLFsupported = !(Util.isWindowsMEorOlder() || Util
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
            log().verbose("Opening splashscreen");
            // Show splash if not starting minimized
            splash = new SplashScreen(getController(), 260000);
        }
        
        started = false;
    }

   
    /**
     * Starts the UI
     */
    public void start() {
        mainFrame = new MainFrame(getController());

        // install system tray files
        installNativeFiles();
        
        // create the chatModel
        chatModel = new ChatModel(getController());

        blinkManager = new BlinkManager(getController());

        // now load
        log().debug("Building UI");
        mainFrame.buildUI();
        log().debug("UI built");

        // // Show window contents while dragging
        // Toolkit.getDefaultToolkit().setDynamicLayout(true);
        // System.setProperty("sun.awt.noerasebackground", "true");

        // Completely loaded now
        //setLoadingCompletion(100);

        if (Util.isSystraySupported()) {
            // Not create systray on windows before 2000 systems
            String iconPath = Controller.getTempFilesLocation()
                .getAbsolutePath()
                + "/";
            defaultIcon = new SysTrayMenuIcon(iconPath + "PowerFolder",
                "openui");
            sysTrayMenu = new SysTrayMenu(defaultIcon, getController()
                .getMySelf().getNick()
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

            SysTrayMenuItem pfLabel = new SysTrayMenuItem(getController()
                .getMySelf().getNick()
                + " | "
                + Translation.getTranslation("systray.powerfolder",
                    Controller.PROGRAM_VERSION), "gotohp");
            sysTrayMenu.addItem(pfLabel);
            pfLabel.addSysTrayMenuListener(this);

            defaultIcon.addSysTrayMenuListener(this);

            UpdateSystrayTask updateSystrayTask = new UpdateSystrayTask();
            getController().scheduleAndRepeat(updateSystrayTask, 5000);

            // Switch Systray show/hide menuitem dynamically
            mainFrame.getUIComponent().addComponentListener(
                new ComponentAdapter() {
                    public void componentShown(ComponentEvent arg0) {
                        opentUI.setLabel(Translation
                            .getTranslation("systray.hide"));
                        opentUI.setActionCommand("hideui");

                    }

                    public void componentHidden(ComponentEvent arg0) {
                        opentUI.setLabel(Translation
                            .getTranslation("systray.show"));
                        opentUI.setActionCommand("openui");

                    }
                });
        } else {
            log().warn("System tray currently only supported on windows (>98)");
            mainFrame.getUIComponent().setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);
        }

        if (getController().isStartMinimized()) {
            log().warn("Starting minimized");
        }
        mainFrame.getUIComponent().setVisible(
            !getController().isStartMinimized());

        // Add mouse listner for double click
        getControlQuarter().getUITree().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object target = getControlQuarter().getSelectedItem();
                    if (target != null) {
                        doubleClickOn(target);
                    }
                }
            }
        });

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
            PFWizard wizard = new PFWizard(getController());
            wizard.open();

            // Now never again, only on button
            getController().getPreferences().putBoolean("openwizard2", false);
        }
    }

    public void hideSplash() {
        if (splash != null) {
            // Disable splash
            splash.setVisible(false);
            splash.dispose();
        }
    }
    
    private class UpdateSystrayTask extends TimerTask {
        public void run() {
            String tooltip = Translation.getTranslation("general.powerfolder")
                + " " + getController().getMySelf().getNick();
            int count = 0;
            Folder[] folders = getController().getFolderRepository()
                .getFolders();
            for (Folder folder : folders) {
                if (folder.isSynchronizing()) {
                    count++;
                }
            }

            if (count > 0) {
                tooltip += " "
                    + Translation.getTranslation("systray.tooltip.syncing");
            } else {
                tooltip += " "
                    + Translation.getTranslation("systray.tooltip.insync");
            }
            double totalCPSdown = 0;
            TransferManager tm = getController().getTransferManager();
            Download[] downloads = tm.getActiveDownloads();
            for (Download download : downloads) {
                TransferCounter counter = download.getCounter();
                totalCPSdown += counter.calculateAverageCPS();
            }

            double totalCPSup = 0;

            Upload[] uploads = tm.getActiveUploads();
            for (Upload upload : uploads) {
                TransferCounter counter = upload.getCounter();
                totalCPSup += counter.calculateAverageCPS();
            }

            tooltip += " "
                + Translation.getTranslation("systray.tooltip.up",

                Format.NUMBER_FORMATS.format(totalCPSup / 1024))
                + " "
                + Translation.getTranslation("systray.tooltip.down",
                    Format.NUMBER_FORMATS.format(totalCPSdown / 1024));

            sysTrayMenu.setToolTip(tooltip);
        }
    }

    /**
     * Shuts the ui down
     */
    public void shutdown() {
        if (splash != null) {
            splash.shutdown();
        }
        
        if (started) {
            mainFrame.storeValues();
            mainFrame.getUIComponent().setVisible(false);
            mainFrame.getUIComponent().dispose();

            // Close systray
            if (Util.isSystraySupported()) {
                sysTrayMenu.hideIcon();
                sysTrayMenu.removeAll();
            }
        }

        started = false;
    }

    public BlinkManager getBlinkManager() {
        return blinkManager;
    }

    /**
     * Returns the setted ui theme as String (classname)
     * 
     * @return
     */
    public String getUIThemeConfig() {
        return getController().getConfig().getProperty("uitheme");
    }

    /**
     * Answers if the ui controller is started
     * 
     * @return
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Make this public
     * 
     * @return
     */
    public Controller getController() {
        return super.getController();
    }

    public ControlQuarter getControlQuarter() {
        return mainFrame.getControlQuarter();
    }

    public InformationQuarter getInformationQuarter() {
        return mainFrame == null ? null : mainFrame.getInformationQuarter();
    }

    /**
     * Sets the loading percentage
     * 
     * @param percentage
     */
    public void setLoadingCompletion(int percentage) {
        if (splash != null) {
            splash.setCompletionPercentage(percentage);            
        }
    }

    // Selection code *********************************************************

    /**
     * Double clicken on
     * 
     * @param target
     */
    private void doubleClickOn(Object target) {
        log().verbose("Doubleclicked on " + target);
        // Dont join folder on doubleclick
        if (target instanceof FolderInfo) {
            // Perform the folder action
            getFolderJoinLeaveAction().actionPerformed(null);
        }
        //        
        // else if (target instanceof Folder) {
        // getSendMessageAction().actionPerformed(null);
        // } else if (target instanceof Member) {
        // getSendMessageAction().actionPerformed(null);
        // }
    }

    /**
     * @return the mainframe
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }
    
    /**
     * @return the model holding all chat data
     */
    public ChatModel getChatModel() {
        return chatModel;
    }

    // Systray interface/install code *****************************************

    /**
     * Sets the icon of the systray
     * 
     * @param iconName
     */
    public synchronized void setTrayIcon(String iconName) {
        if (!Util.isSystraySupported()) {
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
        if (!Util.isWindowsSystem()) {
            // not install on non windows systems
            return;
        }
        // we don't use Systray on windows older than win2K
        if (!Util.isWindowsMEorOlder()) {
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
                    getScanAllFoldersAction()
                        .actionPerformed(
                            new ActionEvent(e.getSource(),
                                ActionEvent.ACTION_PERFORMED, e
                                    .getActionCommand()));
                }
            });

        } else if ("gotohp".equals(e.getActionCommand())) {
            try {
                BrowserLauncher.openURL("http://www.powerfolder.com");
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
            log().warn("Added runner to pending jobs: " + runner);
            // Add to pending jobs
            pendingJobs.add(runner);
        }
    }

    /**
     * Displays a message dialog with the given parameters
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
    private Action folderJoinLeaveAction;
    private Action folderCreateAction;
    private Action openAboutAction;
    private Action toggleSilentModeAction;

    // on folders
    private Action requestFileListAction;
    // private SendMessageAction sendMessageAction;
    private Action scanFolderAction;
    private Action scanAllFoldersAction;

    private Action inviteAction;
    private Action setMasterNodeAction;

    // on members
    private Action requestReportAction;
    private Action reconnectAction;

    // private Action showFileInfoAction;

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

    public Action getFolderJoinLeaveAction() {
        if (folderJoinLeaveAction == null) {
            // Folder join leave action acts upon selection of control quarter
            folderJoinLeaveAction = new FolderJoinLeaveAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return folderJoinLeaveAction;
    }

    /**
     * Create invitation action with toolbar icons
     * 
     * @return
     */
    public Action createToolbarInvitationAction() {
        return new FolderInvitationAction(getController());
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

    public Action getRequestFileListAction() {
        if (requestFileListAction == null) {
            requestFileListAction = new RequestFileListAction(getController(),
                getControlQuarter().getSelectionModel());
        }
        return requestFileListAction;
    }

    public Action getScanFolderAction() {
        if (scanFolderAction == null) {
            scanFolderAction = new ScanFolderAction(getController());
        }
        return scanFolderAction;
    }

    public Action getScanAllFoldersAction() {
        if (scanAllFoldersAction == null) {
            scanAllFoldersAction = new ScanAllFoldersAction(getController());
        }
        return scanAllFoldersAction;
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
            inviteAction = new InviteAction(Icons.FOLDER_ACTION,
                getController(), getControlQuarter().getSelectionModel());
        }
        return inviteAction;
    }

    public Action getSetMasterNodeAction() {
        if (setMasterNodeAction == null) {
            setMasterNodeAction = new SetMasterNodeAction(getController());
        }
        return setMasterNodeAction;
    }
}