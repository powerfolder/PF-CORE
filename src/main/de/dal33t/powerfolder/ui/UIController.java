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

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.event.AskForFriendshipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.InvitationHandler;
import de.dal33t.powerfolder.event.LocalMassDeletionEvent;
import de.dal33t.powerfolder.event.MassDeletionHandler;
import de.dal33t.powerfolder.event.RemoteMassDeletionEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.skin.Skin;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.chat.ChatFrame;
import de.dal33t.powerfolder.ui.dialog.SingleFileTransferDialog;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationFrame;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.notices.AskForFriendshipEventNotice;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.SimpleNotificationNotice;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.notification.NotificationHandler;
import de.dal33t.powerfolder.ui.notification.Slider;
import de.dal33t.powerfolder.ui.render.MainFrameBlinkManager;
import de.dal33t.powerfolder.ui.render.SysTrayBlinkManager;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.util.update.UIUpdateHandler;
import de.dal33t.powerfolder.util.update.Updater;
import de.dal33t.powerfolder.util.update.UpdaterHandler;

/**
 * The ui controller.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.86 $
 */
public class UIController extends PFComponent {

    private static final Logger log = Logger.getLogger(UIController.class
        .getName());
    private static final long FIVE_GIG = 5L << 30;

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

    private final AtomicInteger activeFrame;

    /**
     * The UI distribution running.
     */
    private Skin[] skins;

    private Skin activeSkin;

    private boolean limitDialogShown = false;

    /**
     * Initializes a new UI controller. open UI with #start
     * 
     * @param controller
     */
    public UIController(Controller controller) {
        super(controller);

        folderRepositorySynchronizing = new AtomicBoolean();

        activeFrame = new AtomicInteger();

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
        getController().addMassDeletionHandler(new MyMassDeletionHandler());
        started = false;
        getController().addInvitationHandler(new MyInvitationHandler());
        getController().addAskForFriendshipListener(
            new MyAskForFriendshipListener());
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
    @SuppressWarnings("serial")
    public void start() {
        if (getController().isVerbose()) {
            // EventDispatchThreadHangMonitor.initMonitoring();
            // RepaintManager
            // .setCurrentManager(new CheckThreadViolationRepaintManager());
        }

        // The central application model
        applicationModel = new ApplicationModel(getController());
        applicationModel.initialize();

        // create the Frame
        mainFrame = new MainFrame(getController());
        // attach listeners after construct so they can back-reference mainFrame
        mainFrame.attachListeners();

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
            logInfo("Starting minimized");
        }

        mainFrame.setNetworkingModeStatus(getController().getNetworkingMode());

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

        chatFrame.initializeChatModelListener(applicationModel.getChatModel());

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
        if (getController().getPreferences().getBoolean("openwizard2", true)
            && (!ProUtil.isRunningProVersion() || Feature.BETA.isEnabled()))
        {
            UIUtil.invokeLaterInEDT(new Runnable() {
                // Don't block start!
                public void run() {
                    hideSplash();
                    PFWizard.openBasicSetupWizard(getController());
                }
            });
        }

        // Start the blinkers later, so the UI is fully displayed first.
        UIUtil.invokeLaterInEDT(new Runnable() {
            public void run() {
                new SysTrayBlinkManager(UIController.this);
                new MainFrameBlinkManager(UIController.this);
            }
        });

        UpdaterHandler updateHandler = new UIUpdateHandler(getController());
        Updater.installPeriodicalUpdateCheck(getController(), updateHandler);

        // #1838 Ads in trial
        if (!ProUtil.isRunningProVersion() || ProUtil.isTrial(getController()))
        {
            gotoHPIfRequired();
            // Show promo after 10 seconds
            if (getController().getDistribution().showClientPromo()) {
                getController().scheduleAndRepeat(new TimerTask() {
                    @Override
                    public void run() {
                        UIUtil.invokeLaterInEDT(new Runnable() {
                            public void run() {
                                showPromoGFX(getMainFrame().getUIComponent());
                            }
                        });
                    }
                }, 10 * 1000L, 1000L * 60 * 60);
            }
        }

        // Check limits
        if (!ProUtil.isRunningProVersion()) {
            getController().scheduleAndRepeat(new TimerTask() {
                @Override
                public void run() {
                    checkLimits(false);
                }
            }, 30L * 1000);
            applicationModel.getLicenseModel().setActivationAction(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        checkLimits(true);
                    }
                });
        }
    }

    private void checkLimits(boolean forceOpen) {
        long totalFolderSize = calculateTotalLocalSharedSize();
        logFine("Local shared folder size: "
            + Format.formatBytes(totalFolderSize));
        boolean limitHit = totalFolderSize > FIVE_GIG
            || getController().getFolderRepository().getFoldersCount() > 3;
        if (limitHit) {
            getController().getNodeManager().shutdown();
            if (!limitDialogShown || forceOpen) {
                limitDialogShown = true;
                new FreeLimitationDialog(getController()).open();
            }
        } else {
            if (!getController().getNodeManager().isStarted()) {
                getController().getNodeManager().start();
            }
        }
    }

    private long calculateTotalLocalSharedSize() {
        long totalSize = 0L;
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            totalSize += folder.getStatistic().getSize(
                getController().getMySelf());
        }
        return totalSize;
    }

    public void showPromoGFX(Window parent) {
        try {
            JLabel promoLabel = new JLabel(new ImageIcon(new URL(
                Constants.PROVIDER_CLIENT_PROMO_URL)));
            promoLabel.setSize(new Dimension(230, 230));
            Border border = BorderFactory.createCompoundBorder(BorderFactory
                .createLineBorder(Color.DARK_GRAY), Borders
                .createEmptyBorder("15, 15, 15, 15"));
            promoLabel.setBorder(border);
            promoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            promoLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        BrowserLauncher.openURL(ProUtil
                            .getBuyNowURL(getController()));
                    } catch (IOException e1) {
                        logWarning("Unable to goto homepage", e1);
                    }
                }
            });
            notifyComponent(promoLabel, parent, 20);
        } catch (MalformedURLException e) {
            logWarning("Unable to show promo gfx. " + e, e);
        }
    }

    private void gotoHPIfRequired() {
        String prefKey = "startCount" + Controller.PROGRAM_VERSION;
        int thisVersionStartCount = getController().getPreferences().getInt(
            prefKey, 0);
        // Go to HP every 5 starts
        if (thisVersionStartCount % 5 == 2) {
            try {
                BrowserLauncher.openURL(ProUtil.getBuyNowURL(getController()));
            } catch (IOException e1) {
                logWarning("Unable to goto homepage", e1);
            }
        }
        thisVersionStartCount++;
        getController().getPreferences().putInt(prefKey, thisVersionStartCount);
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
                        log.log(Level.WARNING,
                            "Unable to goto PowerFolder homepage", e1);
                    }
                }
            }
        };
        MenuItem item = menu.add(new MenuItem(Translation
            .getTranslation("general.application.name")));
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
        applicationModel.getChatNotificationsValueModel()
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    chatMenuItem.setState((Boolean) evt.getNewValue());
                }
            });

        final CheckboxMenuItem systemMenuItem = new CheckboxMenuItem(
            Translation.getTranslation("systray.notifications.system"));
        notificationsMenu.add(systemMenuItem);
        systemMenuItem.setState((Boolean) applicationModel
            .getSystemNotificationsValueModel().getValue());
        systemMenuItem.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                applicationModel.getSystemNotificationsValueModel().setValue(
                    systemMenuItem.getState());
            }
        });
        applicationModel.getSystemNotificationsValueModel()
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    systemMenuItem.setState((Boolean) evt.getNewValue());
                }
            });

        sysTrayFoldersMenu = new Menu(Translation
            .getTranslation("general.folder"));
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
        final File localBase = folder.getCommitOrLocalDir();
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (localBase.exists()) {
                    FileUtils.openFile(localBase);
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
            PreferencesEntry.SKIN_NAME.setValue(getController(), activeSkin
                .getName());
        }

        Properties props = activeSkin.getIconsProperties();
        if (props != null) {
            Icons.setIconProperties(props);
        }
        try {
            LookAndFeelSupport.setLookAndFeel(activeSkin.getLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            logSevere("Failed to set look and feel for skin "
                + activeSkin.getName(), e);
        } catch (ParseException e) {
            logSevere("Failed to set look and feel for skin "
                + activeSkin.getName(), e);
        }
    }

    /**
     * Shows an OutOfMemoryError to the user.
     * 
     * @param oome
     */
    public void showOutOfMemoryError(OutOfMemoryError oome) {
        // @todo convert this to a warning notification?
        if (!seenOome) {
            seenOome = true;
            // http\://www.powerfolder.com/wiki/Memory_configuration
            String memoryConfigHelp = Help.getWikiArticleURL(getController(),
                WikiLinks.MEMORY_CONFIGURATION);
            String infoText = Translation.getTranslation(
                "low_memory.error.text", memoryConfigHelp);
            int response = DialogFactory.genericDialog(getController(),
                Translation.getTranslation("low_memory.error.title"), infoText,
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
        UIUtil.putOnScreen(systemMonitorFrame.getUIComponent());
        systemMonitorFrame.getUIComponent().setVisible(true);
    }

    /**
     * Displays the information window if not already displayed.
     */
    private void displayInformationWindow() {
        if (mainFrame.shouldShowInfoInline()) {
            mainFrame.showInlineInfoPanel((JPanel) informationFrame
                .getUIComponent().getContentPane(), informationFrame
                .getUIComponent().getTitle());
        } else {
            JFrame frame = informationFrame.getUIComponent();
            if (frame.getExtendedState() == Frame.ICONIFIED) {
                frame.setExtendedState(Frame.NORMAL);
            }
            UIUtil.putOnScreen(frame);
            frame.setVisible(true);
        }
    }

    /**
     * Opens the Files information for a folder.
     * 
     * @param folderInfo
     *            info of the folder to display files information for.
     */
    public void openFilesInformationLatest(FolderInfo folderInfo) {
        informationFrame.displayFolderFilesLatest(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Files information for a folder.
     * 
     * @param folderInfo
     *            info of the folder to display files information for.
     */
    public void openFilesInformationIncoming(FolderInfo folderInfo) {
        informationFrame.displayFolderFilesIncoming(folderInfo);
        displayInformationWindow();
    }

    public void openFilesInformationDeleted(FolderInfo folderInfo) {
        informationFrame.displayFolderFilesDeleted(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Files information for a folder.
     * 
     * @param folderInfo
     *            info of the folder to display files information for.
     */
    public void openFilesInformation(FolderInfo folderInfo) {
        informationFrame.displayFolderFiles(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Settings information for a folder.
     * 
     * @param folderInfo
     *            info of the folder to display member settings information for.
     */
    public void openSettingsInformation(FolderInfo folderInfo) {
        informationFrame.displayFolderSettings(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Members information for a folder.
     * 
     * @param folderInfo
     *            info of the folder to display member computer information for.
     */
    public void openMembersInformation(FolderInfo folderInfo) {
        informationFrame.displayFolderMembers(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Problems information for a folder.
     * 
     * @param folderInfo
     *            info of the folder to display problems information for.
     */
    public void openProblemsInformation(FolderInfo folderInfo) {
        informationFrame.displayFolderProblems(folderInfo);
        displayInformationWindow();
    }

    /**
     * Opens the Files information for a folder.
     * 
     * @param memberInfo
     *            info of the folder to display files information for.
     */
    public void openChat(MemberInfo memberInfo) {
        if (memberInfo != null) {
            chatFrame.displayChat(memberInfo, true);
        }
        JFrame frame = chatFrame.getUIComponent();
        if (frame.getExtendedState() == Frame.ICONIFIED) {
            frame.setExtendedState(Frame.NORMAL);
        }
        UIUtil.putOnScreen(chatFrame.getUIComponent());
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

    public void openNoticesCard() {
        informationFrame.displayNotices();
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

    // TODO Move to ApplicationModel
    public void syncFolder(Folder folder) {
        if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder.getSyncProfile()))
        {
            // Ask for more sync options on that folder if on project sync
            new SyncFolderPanel(getController(), folder).open();
        } else {
            getController().setSilentMode(false);

            // Let other nodes scan now!
            folder.broadcastScanCommand();

            // Recommend scan on this. User request, so recommend with true.
            folder.recommendScanOnNextMaintenance(true);

            // Now trigger the scan
            getController().getFolderRepository().triggerMaintenance();

            // Trigger file requesting.
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folder.getInfo());
        }
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
            informationFrame.nudge(diffX, diffY);
            chatFrame.nudge(diffX, diffY);
            systemMonitorFrame.nudge(diffX, diffY);
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
     * Attention: If possible use method
     * {@link UIUtil#getParentWindow(ActionEvent)} to determine the active
     * window.
     * <p>
     * This returns most recently active PowerFolder frame. Possibly the
     * InformationFrame, ChatFrame or (default) MainFrame. Used by dialogs, so
     * focus does not always jump to the wrong (Main) frame.
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

    public boolean chatFrameVisible() {
        return chatFrame.getUIComponent().isVisible();
    }

    /**
     * Show the pending messages button in the status bar.
     * 
     * @param show
     */
    public void showPendingMessages(boolean show) {
        mainFrame.showPendingMessages(show);
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    private class UpdateSystrayTask extends TimerTask {
        public void run() {
            StringBuilder tooltip = new StringBuilder();

            tooltip.append(Translation
                .getTranslation("general.application.name"));
            tooltip.append(' ');
            if (folderRepositorySynchronizing.get()) {
                tooltip.append(Translation
                    .getTranslation("systray.tooltip.syncing"));
            } else {
                tooltip.append(Translation
                    .getTranslation("systray.tooltip.in_sync"));
            }
            double totalCPSdownKB = getController().getTransferManager()
                .getDownloadCounter().calculateCurrentKBS();
            double totalCPSupKB = getController().getTransferManager()
                .getUploadCounter().calculateCurrentKBS();

            String downText;

            if (totalCPSdownKB > 1024) {
                downText = Translation.getTranslation(
                    "systray.tooltip.down.mb", Format
                        .formatDecimal(totalCPSdownKB / 1024));
            } else {
                downText = Translation.getTranslation("systray.tooltip.down",
                    Format.formatDecimal(totalCPSdownKB));
            }

            String upText;
            if (totalCPSupKB > 1024) {
                upText = Translation.getTranslation("systray.tooltip.up.mb",
                    Format.formatDecimal(totalCPSupKB / 1024));
            } else {
                upText = Translation.getTranslation("systray.tooltip.up",
                    Format.formatDecimal(totalCPSupKB));
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
            if (OSUtil.isSystraySupported() && sysTrayMenu != null) {
                SystemTray.getSystemTray().remove(sysTrayMenu);
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
        return informationFrame.isShowingFolder();
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

    public Image getTrayIcon() {
        return sysTrayMenu != null ? sysTrayMenu.getImage() : null;
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
            Collection<Folder> folders = repo.getFolders(true);

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
                    text1 = Translation
                        .getTranslation("check_status.in_sync_all");
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
                    "check_status.powerfolders", Format
                        .formatBytes(nTotalBytes), String.valueOf(folders
                        .size()));

                applicationModel.getNoticesModel().handleNotice(
                    new SimpleNotificationNotice(Translation
                        .getTranslation("check_status.title"), text1 + "\n\n"
                        + text2));
            }
        }
    }

    /**
     * Only use this for preview from the DialogSettingsTab. It by-passes all
     * the usual safty checks.
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
     * Show a chat message popup notification.
     * 
     * @param title
     *            message title
     * @param message
     *            the message to popup
     */
    public void showChatNotification(String title, String message) {
        if (started && !getController().isShuttingDown()) {
            if ((Boolean) applicationModel.getChatNotificationsValueModel()
                .getValue())
            {
                NotificationHandler notificationHandler = new NotificationHandler(
                    getController(), title, message, true);
                notificationHandler.show();
            }
        }
    }

    public void notifyComponent(JComponent content) {
        Slider slider = new Slider(content,
            PreferencesEntry.NOTIFICATION_DISPLAY.getValueInt(getController()),
            PreferencesEntry.NOTIFICATION_TRANSLUCENT
                .getValueInt(getController()), getController().isNotifyLeft());
        slider.show();
    }

    public void notifyComponent(JComponent content, Window owner,
        int seconds2Display)
    {
        Slider slider = new Slider(content, owner, seconds2Display,
            PreferencesEntry.NOTIFICATION_TRANSLUCENT
                .getValueInt(getController()), getController().isNotifyLeft());
        slider.show();
    }

    /**
     * Class to handle local and remote mass deletion events. This pushes
     * warnings into the app model.
     */
    private class MyMassDeletionHandler implements MassDeletionHandler {
        public void localMassDeletion(final LocalMassDeletionEvent event) {
            WarningNotice notice = new WarningNotice(Translation
                .getTranslation("warning_notice.title"), Translation
                .getTranslation("warning_notice.mass_deletion"), new Runnable()
            {
                public void run() {
                    int response = DialogFactory
                        .genericDialog(
                            getController(),
                            Translation
                                .getTranslation("uicontroller.local_mass_delete.title"),
                            Translation.getTranslation(
                                "uicontroller.local_mass_delete.message", event
                                    .getFolderInfo().name),
                            new String[]{
                                Translation
                                    .getTranslation("uicontroller.local_mass_delete.broadcast_deletions"),
                                Translation
                                    .getTranslation("uicontroller.local_mass_delete.remove_folder_locally"),
                                Translation.getTranslation("general.close")},
                            0, GenericDialogType.WARN);
                    if (response == 0) {
                        // Broadcast deletions
                        FolderRepository folderRepository = getController()
                            .getFolderRepository();
                        Folder folder = folderRepository.getFolder(event
                            .getFolderInfo());
                        folder.scanLocalFiles(true);
                    } else if (response == 1) {
                        // Remove folder locally
                        FolderRepository folderRepository = getController()
                            .getFolderRepository();
                        Folder folder = folderRepository.getFolder(event
                            .getFolderInfo());
                        folderRepository.removeFolder(folder, false);
                    }
                }
            });
            applicationModel.getNoticesModel().addNotice(notice);
        }

        public void remoteMassDeletion(RemoteMassDeletionEvent event) {
            String message;
            if (event.isPercentage()) {
                message = Translation.getTranslation(
                    "uicontroller.remote_mass_delete.warning_message", event
                        .getMemberInfo().nick, String.valueOf(event
                        .getDeleteFigure()), event.getFolderInfo().name, event
                        .getOldProfile().getName(), event.getNewProfile()
                        .getName());
            } else {
                message = Translation.getTranslation(
                    "uicontroller.remote_mass_delete.warning_absolute_message",
                    event.getMemberInfo().nick, String.valueOf(event
                        .getDeleteFigure()), event.getFolderInfo().name, event
                        .getOldProfile().getName(), event.getNewProfile()
                        .getName());
            }

            WarningNotice notice = new WarningNotice(getController(),
                Translation.getTranslation("warning_notice.title"), Translation
                    .getTranslation("warning_notice.mass_deletion"), message);
            applicationModel.getNoticesModel().addNotice(notice);
        }
    }

    private class MyInvitationHandler implements InvitationHandler {
        public void gotInvitation(Invitation invitation, boolean sendIfJoined) {
            Notice notice = new InvitationNotice(Translation
                .getTranslation("notice.invitation.title"), Translation
                .getTranslation("notice.invitation.summary", invitation
                    .getInvitor().getNick(), invitation.folder.name),
                invitation);
            applicationModel.getNoticesModel().handleNotice(notice);
        }
    }

    private class MyAskForFriendshipListener implements
        AskForFriendshipListener
    {
        public void askForFriendship(AskForFriendshipEvent event) {
            if (PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
                .getValueBoolean(getController()))
            {
                Notice notice = new AskForFriendshipEventNotice(Translation
                    .getTranslation("notice.ask_for_friendship.title"),
                    Translation.getTranslation(
                        "notice.ask_for_friendship.summary", event
                            .getMemberInfo().getNick()), event);
                applicationModel.getNoticesModel().handleNotice(notice);
            }
        }
    }

}