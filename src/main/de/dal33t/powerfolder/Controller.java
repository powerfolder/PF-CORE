/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
 * $Id: Controller.java 21251 2013-03-19 01:46:23Z sprajc $
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.distribution.Distribution;
import de.dal33t.powerfolder.distribution.PowerFolderBasic;
import de.dal33t.powerfolder.distribution.PowerFolderPro;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.message.SettingsChange;
import de.dal33t.powerfolder.net.*;
import de.dal33t.powerfolder.plugin.PluginManager;
import de.dal33t.powerfolder.security.SecurityManager;
import de.dal33t.powerfolder.security.SecurityManagerClient;
import de.dal33t.powerfolder.task.PersistentTaskManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.FileBrowserIntegration;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.dialog.SyncFolderDialog;
import de.dal33t.powerfolder.ui.dialog.UIUnLockDialog;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.util.LimitedConnectivityChecker;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.SystemUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;
import de.dal33t.powerfolder.util.update.UpdateSetting;
import org.apache.commons.cli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.beans.ExceptionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Central class gives access to all core components in PowerFolder. Make sure
 * to extend PFComponent so you always have a reference to the main
 * {@link Controller}.
 * @
 * @author Christian Sprajc
 * @version $Revision: 1.107 $
 */
public class Controller extends PFComponent {
    private static final Logger log = Logger.getLogger(Controller.class
        .getName());

    private static final int MAJOR_VERSION = 11;
    private static final int MINOR_VERSION = 3;
    private static final int REVISION_VERSION = 370;

    /**
     * Program version.
     */
    public static final String PROGRAM_VERSION = MAJOR_VERSION + "."
        + MINOR_VERSION + "." + REVISION_VERSION;

    /** general wait time for all threads (5000 is a balanced value) */
    private static final long WAIT_TIME = 5000;

    /** The command line entered by the user when starting the program */
    private CommandLine commandLine;

    /** filename of the current configFile */
    private String configFilename;
    /**
     * The actual config file.
     */
    private Path configFile;
    private Path configFolderFile;

    /** The config properties */
    private SplitConfig config;

    /**
     * The preferences
     */
    private Preferences preferences;

    /**
     * The distribution running.
     */
    private Distribution distribution;

    /** Program start time */
    private Date startTime;

    /** Are we in started state? */
    private volatile boolean started;

    /** Are we trying to shutdown? */
    private volatile boolean shuttingDown;

    /** Is a restart requested */
    private boolean restartRequested;

    /** Are we in verbose mode? */
    private boolean verbose;

    /** Should we request debug reports? */
    private boolean debugReports;

    /**
     * If running is paused mode
     */
    private volatile boolean paused;

    /**
     * cache the networking mode in a field so we dont heve to do all this
     * comparing
     */
    private NetworkingMode networkingMode;

    /** The nodemanager that holds all members */
    private NodeManager nodeManager;

    /**
     * Responsible for (re-)connecting to other nodes.
     */
    private ReconnectManager reconnectManager;

    /** The FolderRepository that holds all "joined" folders */
    private FolderRepository folderRepository;

    /** The Listener to incomming connections of other PowerFolder clients */
    private ConnectionListener connectionListener;

    /** The basic io provider */
    private IOProvider ioProvider;

    /** Icon overlays and context menus */
    private FileBrowserIntegration fbIntegration;

    /**
     * besides the default listener we may have a list of connection listeners
     * that listen on other ports
     */
    private List<ConnectionListener> additionalConnectionListeners;

    private final List<InvitationHandler> invitationHandlers;
    private final List<MassDeletionHandler> massDeletionHandlers;

    /** The BroadcastManager send "broadcasts" on the LAN so we can */
    private BroadcastMananger broadcastManager;

    /**
     * The DynDNS manager that handles the working arwound for user with a
     * dynnamip IP address.
     */
    private DynDnsManager dyndnsManager;

    private PersistentTaskManager taskManager;

    private Callable<TransferManager> transferManagerFactory = new Callable<TransferManager>()
    {
        @Override
        public TransferManager call() throws Exception {
            return new TransferManager(Controller.this);
        }
    };

    /** Handels the up and downloads */
    private TransferManager transferManager;

    /**
     * Remote Commands listener, a protocol handler for powerfolder links:
     * powerfolder://
     */
    private RemoteCommandManager rconManager;

    /**
     * Listener for authentication requests on WD NAS storages.
     */

    private WebClientLogin webClientLogin;

    /** Holds the User interface */
    private UIController uiController;

    /** holds all installed plugins */
    private PluginManager pluginManager;
    /**
     * The security manager, handles access etc.
     */
    private SecurityManager securityManager;

    /**
     * The Online Storage client
     */
    private ServerClient osClient;

    /** global Threadpool */
    private ScheduledExecutorService threadPool;

    /** Remembers if a port on the local firewall was opened */
    private final boolean portWasOpened = false;

    /**
     * If we have limited connectivity
     */
    private boolean limitedConnectivity;

    private final PausedModeListener pausedModeListenerSupport;

    private final NetworkingModeListener networkingModeListenerSupport;

    private final LimitedConnectivityListener limitedConnectivityListenerSupport;

    private ScheduledFuture<?> pauseResumeFuture;

    private Controller() {
        // Do some TTL fixing for dyndns resolving
        Security.setProperty("networkaddress.cache.ttl", "0");
        Security.setProperty("networkaddress.cache.negative.ttl", "0");
        System.setProperty("sun.net.inetaddr.ttl", "0");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
            "PowerFolder");
        invitationHandlers = new CopyOnWriteArrayList<InvitationHandler>();
        massDeletionHandlers = new CopyOnWriteArrayList<MassDeletionHandler>();
        pausedModeListenerSupport = ListenerSupportFactory
            .createListenerSupport(PausedModeListener.class);
        networkingModeListenerSupport = ListenerSupportFactory
            .createListenerSupport(NetworkingModeListener.class);
        limitedConnectivityListenerSupport = ListenerSupportFactory
            .createListenerSupport(LimitedConnectivityListener.class);
        AntiSerializationVulnerability.checkClasspath();
    }

    /**
     * Overwite the PFComponent.getController() otherwise that one returns null
     * for this Controller itself.
     *
     * @return a reference to this
     */
    @Override
    public Controller getController() {
        return this;
    }

    /**
     * Creates a fresh Controller.
     *
     * @return the controller
     */
    public static Controller createController() {
        return new Controller();
    }

    /**
     * Starts this controller loading the config from the default config file
     */
    public void startDefaultConfig() {
        startConfig(Constants.DEFAULT_CONFIG_FILE);
    }

    /**
     * Starts a config with the given command line arguments
     *
     * @param aCommandLine
     *            the command line as specified by the user
     */
    public void startConfig(CommandLine aCommandLine) {
        commandLine = aCommandLine;
        String[] configNames = aCommandLine.getOptionValues("c");
        String configName = configNames != null && configNames.length > 0
            && StringUtils.isNotBlank(configNames[0]) ? configNames[0] : null;
        if (StringUtils.isNotBlank(configName)
            && (configName.startsWith("http:") || configName
                .startsWith("https:")))
        {
            if (configNames.length > 1) {
                configName = configNames[1];
            } else {
                configName = Constants.DEFAULT_CONFIG_FILE;
            }
        }
        startConfig(configName);
    }

    /** initTranslation
     * Init translation bundles
     * @author Christoph Kappel <kappel@powerfolder.com>
     **/

    public void initTranslation() {
        // Initialize resource bundle eager
        // check forced language file from commandline
        if (commandLine != null && commandLine.hasOption("f")) {
            String langfilename = commandLine.getOptionValue("f");
            try {
                ResourceBundle resourceBundle = new ForcedLanguageFileResourceBundle(
                    langfilename);
                Translation.setResourceBundle(resourceBundle);
                logInfo("Loading language bundle from file " + langfilename);
            } catch (FileNotFoundException fnfe) {
                logSevere("forced language file (" + langfilename
                    + ") not found: " + fnfe.getMessage());
                logSevere("using setup language");
                Translation.resetResourceBundle();
            } catch (IOException ioe) {
                logSevere("forced language file io error: " + ioe.getMessage());
                logSevere("using setup language");
                Translation.resetResourceBundle();
            }
        } else {
            Translation.resetResourceBundle();
        }
        Translation.getResourceBundle();
    }

    /**
     * Starts controller with a special config file, and creates and starts all
     * components of PowerFolder.
     *
     * @param filename
     *            The filename to uses as config file (located in the
     *            "getConfigLocationBase()")
     */
    public void startConfig(String filename) {
        if (started) {
            throw new IllegalStateException(
                "Configuration already started, shutdown controller first");
        }

        initTranslation();

        // loadConfigFile
        if (!loadConfigFile(filename)) {
            return;
        }

        start();
    }

    /** start
     * Starts controller and all other components of PowerFolder
     * @author Christoph <kappel@powerfolder.com>
     **/

    public void start() {
        boolean isDefaultConfig = Constants.DEFAULT_CONFIG_FILE
            .startsWith(getConfigName());
        if (isDefaultConfig) {
            // To keep compatible with previous versions
            preferences = Preferences.userNodeForPackage(PowerFolder.class);
        } else {
            preferences = Preferences.userNodeForPackage(PowerFolder.class)
                .node(getConfigName());

        }

        // initialize logger
        // Enabled verbose mode if in config.
        // This logs to file for analysis.
        verbose = ConfigurationEntry.VERBOSE.getValueBoolean(this);
        initLogger();

        if (verbose) {
            ByteSerializer.BENCHMARK = true;
            scheduleAndRepeat(new Runnable() {
                @Override
                public void run() {
                    ByteSerializer.printStats();
                }
            }, 600000L, 600000L);
            Profiling.setEnabled(false);
            Profiling.reset();
        }

        String arch = OSUtil.is64BitPlatform() ? "64bit" : "32bit";
        logFine("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + arch + ")");
        logFine("Java: " + JavaVersion.systemVersion().toString() + " ("
            + System.getProperty("java.vendor") + ')');
        logFine("Current time: " + new Date());
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        logFine("Max Memory: " + Format.formatBytesShort(maxMemory)
            + ", Total Memory: " + Format.formatBytesShort(totalMemory));
        if (!Desktop.isDesktopSupported() && isUIEnabled()) {
            logWarning("Desktop utility not supported");
        }

        // If we have a new config. clear the preferences.
        clearPreferencesOnConfigSwitch();

        // Load and set http proxy settings
        HTTPProxySettings.loadFromConfig(this);

        // PFC-2670: Start
        boolean localAllTrustCert = false;
        if (ConfigurationEntry.SECURITY_SSL_TRUST_ANY
            .getValueBoolean(getController()))
        {
        	localAllTrustCert = true;
            NetworkUtil.installAllTrustingSSLManager();
        }
        // PFC-2670: End

        // #2179: Load from server. How to handle timeouts?
        // Command line option -c http://are.de
        ConfigurationLoader.loadAndMergeCLI(this);
        // Config entry in file
        ConfigurationLoader.loadAndMergeConfigURL(this);
        // Read from installer temp file
        ConfigurationLoader.loadAndMergeFromInstaller(this);

        if (verbose != ConfigurationEntry.VERBOSE.getValueBoolean(this)) {
            verbose = ConfigurationEntry.VERBOSE.getValueBoolean(this);
            initLogger();
        }

        // PFC-2670: Start
        // Setting might have changed.
        if (ConfigurationEntry.SECURITY_SSL_TRUST_ANY
            .getValueBoolean(getController()))
        {
            NetworkUtil.installAllTrustingSSLManager();
        } else if (localAllTrustCert) {
            // Locally was set to trust, but remote profile forbids this.
            // Exit->Restart
            logWarning("Security break protection: Trust any SSL certificate was turned on, but is disallowed by server profile. Please restart the client");
            exit(66);
        }
        // PFC-2670: End

        // Init paused only if user expects pause to be permanent or
        // "while I work"
        int pauseSecs = ConfigurationEntry.PAUSE_RESUME_SECONDS
            .getValueInt(getController());
        paused = PreferencesEntry.PAUSED.getValueBoolean(this)
            && (pauseSecs == Integer.MAX_VALUE || pauseSecs == 0);

        // Now set it, just in case it was paused in permanent mode.
        PreferencesEntry.PAUSED.setValue(this, paused);

        // Load and set http proxy settings again.
        HTTPProxySettings.loadFromConfig(this);

        // Initialize branding/preconfiguration of the client
        initDistribution();
        logFine("Build time: " + getBuildTime());
        logInfo("Program version " + PROGRAM_VERSION);

        if (getDistribution().getBinaryName().toLowerCase()
            .contains("powerfolder"))
        {
            Debug.writeSystemProperties();
        }

        if (ConfigurationEntry.KILL_RUNNING_INSTANCE.getValueBoolean(this)) {
            killRunningInstance();
        }
        FolderList.removeMemberFiles(this);

        // Initialize plugins
        setupProPlugins();
        pluginManager = new PluginManager(this);
        pluginManager.init();

        // create node manager
        nodeManager = new NodeManager(this);

        // Only one task brother left...
        taskManager = new PersistentTaskManager(this);
        // Folder repository
        folderRepository = new FolderRepository(this);
        setLoadingCompletion(0, 10);

        // Create transfer manager
        // If this is a unit test it might have been set before.
        try {
            transferManager = transferManagerFactory.call();
        } catch (Exception e) {
            logSevere("Exception", e);
        }

        reconnectManager = new ReconnectManager(this);
        // Create os client
        osClient = new ServerClient(this);

        if (isUIEnabled()) {
            uiController = new UIController(this);
            if (ConfigurationEntry.USER_INTERFACE_LOCKED.getValueBoolean(this))
            {
                // Don't let the user pass this step.
                new UIUnLockDialog(this).openAndWait();
            }
        }

        setLoadingCompletion(10, 20);

        // The io provider.
        ioProvider = new IOProvider(this);
        ioProvider.start();

        // Set hostname by CLI
        if (commandLine != null && commandLine.hasOption('d')) {
            String host = commandLine.getOptionValue("d");
            if (StringUtils.isNotBlank(host)) {
                InetSocketAddress addr = Util.parseConnectionString(host);
                if (addr != null) {
                    ConfigurationEntry.HOSTNAME.setValue(this,
                        addr.getHostName());
                    ConfigurationEntry.NET_BIND_PORT.setValue(this,
                        addr.getPort());
                }
            }
        }

        // initialize dyndns manager
        dyndnsManager = new DynDnsManager(this);

        setLoadingCompletion(20, 30);

        // initialize listener on local port
        if (!initializeListenerOnLocalPort()) {
            return;
        }
        if (!isUIEnabled()) {
            // Disable paused function
            paused = false;
        }

        setLoadingCompletion(30, 35);

        // Start the nodemanager
        nodeManager.init();
        if (!ProUtil.isRunningProVersion()) {
            // Nodemanager gets later (re) started by ProLoader.
            nodeManager.start();
        }

        setLoadingCompletion(35, 60);
        securityManager = new SecurityManagerClient(this, osClient);

        // init repo (read folders)
        folderRepository.init();
        logInfo("Dataitems: " + Debug.countDataitems(Controller.this));
        // init of folders takes rather long so a big difference with
        // last number to get smooth bar... ;-)
        setLoadingCompletion(60, 65);

        // start repo maintainance Thread
        folderRepository.start();
        setLoadingCompletion(65, 70);

        // Start the transfer manager thread
        transferManager.start();
        setLoadingCompletion(70, 75);

        // Initialize rcon manager
        startRConManager();

        // Initialize WD storage authenticator.
        startWebClientLogin();

        setLoadingCompletion(75, 80);

        // Start all configured listener if not in paused mode
        startConfiguredListener();
        setLoadingCompletion(80, 85);

        // open broadcast listener
        openBroadcastManager();
        setLoadingCompletion(85, 90);

        // Controller now started
        started = true;
        startTime = new Date();

        // Now taskmanager
        taskManager.start();

        logInfo("Controller started");

        // dyndns updater
        /*
         * boolean onStartUpdate = ConfigurationEntry.DYNDNS_AUTO_UPDATE
         * .getValueBoolean(this).booleanValue(); if (onStartUpdate) {
         * getDynDnsManager().onStartUpdate(); }
         */
        dyndnsManager.updateIfNessesary();

        setLoadingCompletion(90, 100);

        // Login to OS
        if (Feature.OS_CLIENT.isEnabled()) {
            try {
                osClient.loginWithLastKnown();
            } catch (Exception e) {
                logWarning("Unable to login with last known username. " + e);
                logFiner(e);
            }
        }

        // Start Plugins
        pluginManager.start();

        // open UI
        if (isConsoleMode()) {
            logFine("Running in console");
        } else {
            logFine("Opening UI");
            openUI();
        }

        if (!this.getMySelf().isServer()) {
            enableFileBrowserIntegration(this);
        }

        // Load anything that was not handled last time.
        loadPersistentObjects();

        setLoadingCompletion(100, 100);
        if (!isConsoleMode()) {
            uiController.hideSplash();
        }

        if (ConfigurationEntry.AUTO_CONNECT.getValueBoolean(this)) {
            // Now start the connecting process
            reconnectManager.start();
        } else {
            logFine("Not starting reconnection process. "
                + "Config auto.connect set to false");
        }
        // Start connecting to OS client.
        if (Feature.OS_CLIENT.isEnabled()
            && ConfigurationEntry.SERVER_CONNECT.getValueBoolean(this))
        {
            osClient.start();
        } else {
            logInfo("Not connecting to server (" + osClient.getServerString()
                + "): Disabled");
        }

        // Setup our background working tasks
        setupPeriodicalTasks();

        if (MacUtils.isSupported()) {
            if (isFirstStart()) {
                MacUtils.getInstance().setPFStartup(true, this);
            }
            MacUtils.getInstance().setAppReOpenedListener(this);
        }

        if (pauseSecs == 0) {
            // Activate adaptive logic
            setPaused(paused);
        }
    }

    private void enableFileBrowserIntegration(Controller controller) {
        // PFC-2395: Start
        fbIntegration = new FileBrowserIntegration(getController());
        fbIntegration.start();
        // PFC-2395: End
    }

    private void clearPreferencesOnConfigSwitch() {
        String lastNodeIdObf = PreferencesEntry.LAST_NODE_ID
            .getValueString(this);
        String thisNodeId = ConfigurationEntry.NODE_ID.getValue(this);
        try {
            if (StringUtils.isNotBlank(lastNodeIdObf)
                && !LoginUtil.matches(Util.toCharArray(thisNodeId),
                    lastNodeIdObf))
            {
                int i = 0;
                for (String key : preferences.keys()) {
                    preferences.remove(key);
                    i++;
                }
                logWarning("Cleared " + i
                    + " preferences, new config/nodeid found");
            }
        } catch (BackingStoreException e1) {
            logWarning("Unable to clear preferences. " + e1);
        }
    }

    /**
     * For each folder, kick off scan.
     */
    public void performFullSync() {
        // Let other nodes scan now!
        folderRepository.broadcastScanCommandOnAllFolders();

        // Force scan on all folders, of repository was selected
        Collection<Folder> folders = folderRepository.getFolders();
        for (Folder folder : folders) {

            // Never sync preview folders
            if (folder != null && !folder.isPreviewOnly()) {
                // Ask for more sync options on that folder if on project sync
                if (Util.isAwtAvailable()
                    && folder.getSyncProfile().equals(
                        SyncProfile.MANUAL_SYNCHRONIZATION))
                {
                    new SyncFolderDialog(this, folder).open();
                } else {
                    // Recommend scan on this folder
                    folder.recommendScanOnNextMaintenance();
                }
            }
        }

        setPaused(false);

        // Now trigger the scan
        folderRepository.triggerMaintenance();

        // Trigger file requesting
        folderRepository.getFileRequestor().triggerFileRequesting();

        // Fresh reconnection try!
        reconnectManager.buildReconnectionQueue();

    }

    /**
     * Add invitation listener.
     *
     * @param l
     */
    public void addInvitationHandler(InvitationHandler l) {
        invitationHandlers.add(l);
    }

    /**
     * Remove invitation listener.
     *
     * @param l
     */
    public void removeInvitationHandler(InvitationHandler l) {
        invitationHandlers.remove(l);
    }

    /**
     * Add mass delete listener.
     *
     * @param l
     */
    public void addMassDeletionHandler(MassDeletionHandler l) {
        massDeletionHandlers.add(l);
    }

    /**
     * Remove mass delete listener.
     *
     * @param l
     */
    public void removeMassDeletionHandler(MassDeletionHandler l) {
        massDeletionHandlers.remove(l);
    }

    private void setupProPlugins() {
        String pluginConfig = ConfigurationEntry.PLUGINS.getValue(this);
        boolean autoSetupPlugins = StringUtils.isEmpty(pluginConfig)
            || !pluginConfig.contains(Constants.PRO_LOADER_PLUGIN_CLASS);
        if (ProUtil.isRunningProVersion() && autoSetupPlugins) {
            logFine("Setting up pro loader");
            String newPluginConfig = Constants.PRO_LOADER_PLUGIN_CLASS;
            if (!StringUtils.isBlank(pluginConfig)) {
                newPluginConfig += ',' + pluginConfig;
            }
            ConfigurationEntry.PLUGINS.setValue(this, newPluginConfig);
        }
    }

    private void initLogger() {

        // Set a nice prefix for file looging file names.
        String configName = getConfigName();
        if (configName != null) {
            LoggingManager.setPrefix(configName);
        }

        if (verbose) {
            String str = ConfigurationEntry.LOG_LEVEL_CONSOLE.getValue(this);
            Level consoleLevel = LoggingManager.levelForName(str);
            LoggingManager.setConsoleLogging(consoleLevel != null
                ? consoleLevel
                : Level.WARNING);

            // Enable file logging
            str = ConfigurationEntry.LOG_LEVEL_FILE.getValue(this);
            boolean rotate = ConfigurationEntry.LOG_FILE_ROTATE
                .getValueBoolean(this);
            Level fileLevel = LoggingManager.levelForName(str);
            LoggingManager.setFileLogging(fileLevel != null
                ? fileLevel
                : Level.FINE, rotate);

            // Switch on the document handler.
            if (isUIEnabled()) {
                str = PreferencesEntry.DOCUMENT_LOGGING.getValueString(this);
                Level uiLogLevel = LoggingManager.levelForName(str);
                LoggingManager.setDocumentLogging(uiLogLevel != null
                    ? uiLogLevel
                    : Level.WARNING, this);
            }

            if (LoggingManager.isLogToFile()) {
                logFine("Logging to file '"
                    + LoggingManager.getLoggingFileName() + '\'');
            } else {
                logInfo("No logging to file");
            }

            if (ConfigurationEntry.LOG_SYSLOG_HOST.hasNonBlankValue(this)) {
                str = ConfigurationEntry.LOG_SYSLOG_LEVEL.getValue(this);
                Level syslogLevel = LoggingManager.levelForName(str);
                LoggingManager.setSyslogLogging(syslogLevel != null
                    ? syslogLevel
                    : Level.WARNING, this);
            }
        }

        if (commandLine != null && commandLine.hasOption('l')) {
            String str = commandLine.getOptionValue('l');
            Level consoleLevel = LoggingManager.levelForName(str);
            if (consoleLevel != null) {
                LoggingManager.setConsoleLogging(consoleLevel);
            }
        }

        // Enable debug reports.
        debugReports = ConfigurationEntry.DEBUG_REPORTS.getValueBoolean(this);

        LoggingManager.clearBuffer();
        int maxDays = ConfigurationEntry.LOG_FILE_DELETE_DAYS
            .getValueInt(getController());
        if (maxDays >= 0) {
            LoggingManager.removeOldLogs(maxDays);
        }
    }

    /**
     * Loads a config file (located in "getConfigLocationBase()")
     *
     * @param theFilename
     * @return false if unsuccessful, true if file found and reading succeeded.
     */
    public boolean loadConfigFile(String theFilename) {

        /* Init stuff (moved here from {@link startConfig} */
        additionalConnectionListeners = Collections
            .synchronizedList(new ArrayList<ConnectionListener>());
        started = false;
        shuttingDown = false;
        threadPool = new WrappedScheduledThreadPoolExecutor(
            Constants.CONTROLLER_MIN_THREADS_IN_THREADPOOL, new NamedThreadFactory(
                "Controller-Thread-"));

        // PFI-312
        PathUtils.setIOExceptionListener(new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception e) {
                if (e instanceof FileSystemException
                    && e.toString().toLowerCase()
                        .contains("too many open files"))
                {
                    logSevere("Detected I/O Exception: " + e.getMessage());
                    logSevere("Please adjust limits for open file handles on this server");
                    exit(1);
                }
            }
        });

        String filename = theFilename;
        if (filename == null) {
            filename = Constants.DEFAULT_CONFIG_FILE;
        }

        if (filename.indexOf('.') < 0) {
            // append .config extension
            filename += ".config";
        }

        configFilename = null;
        config = new SplitConfig();
        configFilename = filename;
        configFile = getConfigLocationBase();

        if (configFile == null) {
            configFile = Paths.get(filename).toAbsolutePath();
        } else {
            configFile = configFile.resolve(filename);
        }

        BufferedInputStream bis = null;
        try {
            if (Files.exists(configFile)) {
                logInfo("Loading configfile " + configFile.toString());
            } else {
                logFine("Config file does not exist. " + configFile.toString());
                throw new FileNotFoundException();
            }
            if (OSUtil.isWebStart()) {
                logFine("WebStart, config file location: "
                    + configFile.toString());
            }

            bis = new BufferedInputStream(Files.newInputStream(configFile));
            config.load(bis);
        } catch (FileNotFoundException e) {
            logWarning("Unable to start config, file '" + filename
                + "' not found, using defaults");
        } catch (IOException e) {
            logSevere("Unable to start config from file '" + filename + '\'');
            config = null;
            return false;
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e) {
                // Ignore.
            }
        }

        String folderfilename = filename.replace(".config", "-Folder.config");
        configFolderFile = getConfigLocationBase();
        if (configFolderFile == null) {
            configFolderFile = Paths.get(folderfilename).toAbsolutePath();
        } else {
            configFolderFile = configFolderFile.resolve(folderfilename);
        }

        if (Files.exists(configFolderFile)) {
            try {
                logInfo("Loading folder configfile "
                    + configFolderFile.toString());
                bis = new BufferedInputStream(
                    Files.newInputStream(configFolderFile));
                config.load(bis);
            } catch (FileNotFoundException e) {
                logWarning("Unable to start config, file '" + folderfilename
                    + "' not found, using defaults");
            } catch (IOException e) {
                logSevere("Unable to start config from file '" + folderfilename
                    + '\'');
                configFolderFile = null;
                return false;
            } finally {
                try {
                    if (bis != null) {
                        bis.close();
                    }
                } catch (Exception e) {
                    // Ignore.
                }
            }
        } else {
            logFine("Folder config file does not exist. "
                + configFolderFile.toString());
        }
        return true;
    }

    /**
     * Schedules a task to be executed periodically repeated without initial
     * delay. Until removed. ATTENTION: Tasks may be executed concurrently if
     * long running - especially longer than the period time. Take proper action
     * if you need to avoid concurrent runs, e.g. with AtomicBoolean.
     *
     * @param task
     *            the task to schedule
     * @param period
     *            the time in ms between executions
     */
    public ScheduledFuture<?> scheduleAndRepeat(Runnable task, long period) {
        if (!shuttingDown) {
            return threadPool.scheduleWithFixedDelay(task, 0, period,
                TimeUnit.MILLISECONDS);
        }
        return null;
    }

    /**
     * Schedules a task to be executed periodically repeated with initial
     * delay. Until removed. ATTENTION: Tasks may be executed concurrently if
     * long running - especially longer than the period time. Take proper action
     * if you need to avoid concurrent runs, e.g. with AtomicBoolean.
     *
     * @param task
     *            the task to schedule
     * @param initialDelay
     *            the initial delay in ms
     * @param period
     *            the time in ms between executions
     * @return
     */
    public ScheduledFuture<?> scheduleAndRepeat(Runnable task,
        long initialDelay, long period)
    {
        if (!shuttingDown) {
            return threadPool.scheduleWithFixedDelay(task, initialDelay,
                period, TimeUnit.MILLISECONDS);
        }
        return null;
    }

    /**
     * Use to schedule a task to be executed ONCE after the initial delay.
     *
     * @param task
     *            the task to schedule
     * @param delay
     *            the initial delay in ms
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay) {
        if (!shuttingDown) {
            return threadPool.schedule(task, delay, TimeUnit.MILLISECONDS);
        }
        return null;
    }

    /**
     * Removes a schduled task for the threadpool
     *
     * @param task
     */
    public void removeScheduled(Runnable task) {
        if (!shuttingDown) {
            if (threadPool instanceof ScheduledThreadPoolExecutor) {
                ((ScheduledThreadPoolExecutor) threadPool).remove(task);
                ((ScheduledThreadPoolExecutor) threadPool).purge();
            } else {
                logSevere("Unable to remove scheduled task. Wrong threadpool. "
                    + task);
            }
        }
    }

    /**
     * Removes a scheduled task for the threadpool
     *
     * @param future
     */
    public boolean removeScheduled(ScheduledFuture<?> future) {
        if (!shuttingDown) {
            if (threadPool instanceof ScheduledThreadPoolExecutor) {
                return ((ScheduledThreadPoolExecutor) threadPool)
                    .remove((Runnable) future);
            } else {
                logSevere("Unable to remove scheduled task. Wrong threadpool. "
                    + future);
            }
        }
        return false;
    }

    /**
     * Sets up the task, which should be executes periodically.
     */
    private void setupPeriodicalTasks() {

        // ============
        // Test the connectivity after a while.
        // ============
        LimitedConnectivityChecker.install(this);

        // ============
        // Schedule a task to do housekeeping every day, just after midnight.
        // ============
        Calendar cal = new GregorianCalendar();
        long now = cal.getTime().getTime();

        // Midnight tomorrow morning.
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.DATE, 1);

        // Add a few seconds to be sure the file name definately is for
        // tomorrow.
        cal.add(Calendar.SECOND, 2);

        long midnight = cal.getTime().getTime();
        // How long to wait initially?
        long secondsToMidnight = (midnight - now) / 1000;
        logFine("Initial log reconfigure in " + secondsToMidnight + " seconds");
        threadPool.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                performHousekeeping(true);
            }
        }, secondsToMidnight, 60 * 60 * 24, TimeUnit.SECONDS);

        // Also run housekeeping one minute after start up.
        threadPool.schedule(() -> {
            performHousekeeping(false);
        } , 1, TimeUnit.MINUTES);
        
        // ============
        // Do profiling
        // ============
        if (Profiling.ENABLED) {
            threadPool.scheduleWithFixedDelay(new TimerTask() {
                @Override
                public void run() {
                    logFine(Profiling.dumpStats());
                }
            }, 0, 1, TimeUnit.MINUTES);
        }

        // ============
        // Hourly tasks
        // ============
        // @todo what's this for? comment?
        boolean alreadyDetected = ConfigurationEntry.TRANSFER_LIMIT_AUTODETECT
            .getValueBoolean(this)
            && ConfigurationEntry.UPLOAD_LIMIT_WAN.getValueInt(this) > 0;
        // If already detected wait 10 mins before next test. Otherwise start
        // instantly.
        long initialDelay = alreadyDetected ? 600 : 5;
        threadPool.scheduleWithFixedDelay(new TimerTask() {
            @Override
            public void run() {
                performHourly();
            }
        }, initialDelay, 3600, TimeUnit.SECONDS);

        // =========
        // Profiling
        // =========
        // final Collector cpu = CollectorFactory.getFactory().createCollector(
        // CollectorID.CPU_USAGE.id);
        threadPool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!verbose) {
                    return;
                }
                if (isFine()) {
                    logFine("Dataitems: "
                        + Debug.countDataitems(Controller.this));
                }
                String dump = Debug.dumpCurrentStacktraces(false);
                if (StringUtils.isNotBlank(dump)
                    && isFine()
                    && ConfigurationEntry.LOG_ACTIVE_THREADS
                        .getValueBoolean(getController()))
                {
                    logFine("Active threads:\n\n" + dump);
                } else {
                    logFine("No active threads");
                }
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    /**
     * These tasks get performed every hour.
     */
    private void performHourly() {
        if (ConfigurationEntry.TRANSFER_LIMIT_AUTODETECT.getValueBoolean(this))
        {
            FutureTask<Object> recalculateRunnable = transferManager
                .getRecalculateAutomaticRate();
            threadPool.execute(recalculateRunnable);
        }
    }

    private void openBroadcastManager() {
        if (ConfigurationEntry.NET_BROADCAST.getValueBoolean(this)) {
            try {
                broadcastManager = new BroadcastMananger(this,
                  ConfigurationEntry.D2D_ENABLED.getValueBoolean(this));
                broadcastManager.start();
            } catch (ConnectionException e) {
                logSevere("Unable to open broadcast manager, you wont automatically connect to clients on LAN: "
                    + e.getMessage());
                logSevere("ConnectionException", e);
            }
        } else {
            logInfo("Auto client discovery in LAN via broadcast disabled");
        }
    }

    /**
     * General houskeeping task. Runs one minute after start and every midnight.
     *
     * @param midnightRun
     *            true if this is the midnight invokation, false if this is at
     *            start up.
     */
    private void performHousekeeping(boolean midnightRun) {
        log.fine("Performing housekeeping " + midnightRun);
        Date now = new Date();
        if (midnightRun) {
            // Reconfigure log file after midnight.
            logFine("Reconfiguring logs for new day: " + now);
            initLogger();
            LoggingManager.resetFileLogging();
            int days = ConfigurationEntry.LOG_FILE_DELETE_DAYS
                .getValueInt(getController());
            if (days >= 0) {
                LoggingManager.removeOldLogs(days);
            }
            logFine("Reconfigured logs for new day: " + now);

            backupConfigAssets();
            folderRepository.cleanupOldArchiveFiles();            
        }
        
        // Prune stats.
        transferManager.pruneStats();
    }

    /**
     * #2526
     */
    private void backupConfigAssets() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        Path backupDir = getMiscFilesLocation().resolve(
            "backups/" + dateFormat.format(new Date()));
        if (Files.notExists(backupDir)) {
            try {
                Files.createDirectories(backupDir);
            } catch (IOException ioe) {
                logInfo("Could not create directory '"
                    + backupDir.toAbsolutePath().toString() + "'");
            }
        }
        Path configBackup = backupDir.resolve(configFile.getFileName());
        try {
            PathUtils.copyFile(configFile, configBackup);
        } catch (IOException e) {
            logWarning("Unable to backup file " + configFile + ". " + e);
        }
        if (Files.exists(configFolderFile)) {
            Path configFolderBackup = backupDir.resolve(configFolderFile
                .getFileName());
            try {
                PathUtils.copyFile(configFolderFile, configFolderBackup);
            } catch (IOException e) {
                logWarning("Unable to backup file " + configFolderFile + ". "
                    + e);
            }
        }
        Path myKeyFile = getMiscFilesLocation().resolve(
            getConfigName() + ".mykey");
        Path mykeyBackup = backupDir.resolve(myKeyFile.getFileName());
        if (Files.exists(myKeyFile)) {
            try {
                PathUtils.copyFile(myKeyFile, mykeyBackup);
            } catch (IOException e) {
                logWarning("Unable to backup file " + myKeyFile + ". " + e);
            }
        }
        Path dbFile = getMiscFilesLocation().resolve("Accounts.h2.db");
        Path dbBackup = backupDir.resolve(dbFile.getFileName());
        if (Files.exists(dbFile)) {
            try {
                PathUtils.copyFile(dbFile, dbBackup);
            } catch (IOException e) {
                logWarning("Unable to backup file " + dbFile + ". " + e);
            }
        }
    }

    /**
     * Starts the rcon manager.
     */
    private void startRConManager() {
        if (RemoteCommandManager.hasRunningInstance()) {
            alreadyRunningCheck();
        }
        if (!ConfigurationEntry.NET_RCON_MANAGER.getValueBoolean(this)) {
            logWarning("Not starting RemoteCommandManager");
            return;
        }
        rconManager = new RemoteCommandManager(this);
        rconManager.start();
    }

    /**
     * Starts the WD storage authenticator.
     */
    private void startWebClientLogin() {
        if (WebClientLogin.hasRunningInstance()) {
            alreadyRunningCheck();
        }
        if (ConfigurationEntry.WEB_CLIENT_PORT.hasNonBlankValue(this)
                && ConfigurationEntry.WEB_CLIENT_PORT.getValueInt(this) > 0) {
            webClientLogin = new WebClientLogin(this);
            webClientLogin.start();
        }
    }

    /**
     * Starts a connection listener for each port found in config property
     * "port" ("," separeted), if "random-port" is set to "true" this "port"
     * entry will be ignored and a random port will be used (between 49152 and
     * 65535).
     */
    private boolean initializeListenerOnLocalPort() {
        if (ConfigurationEntry.NET_BIND_RANDOM_PORT.getValueBoolean(this)) {
            bindRandomPort();
        } else {
            String ports = ConfigurationEntry.NET_BIND_PORT.getValue(this);
            if ("0".equals(ports)) {
                logWarning("Not opening connection listener. (port=0)");
            } else {
                if (ports == null) {
                    ports = String.valueOf(ConnectionListener.DEFAULT_PORT);
                }
                StringTokenizer nizer = new StringTokenizer(ports, ",");
                while (nizer.hasMoreElements()) {
                    String portStr = nizer.nextToken();
                    try {
                        int port = Integer.parseInt(portStr);
                        boolean listenerOpened = openListener(port, false);
                        if (listenerOpened && connectionListener != null) {
                            // set reconnect on first successfull listener
                            nodeManager
                                .getMySelf()
                                .getInfo()
                                .setConnectAddress(
                                    connectionListener.getAddress());
                        }
                        if (!listenerOpened && !isUIOpen()) {
                            logSevere("Couldn't bind to port " + port);
                            // exit(1);
                            // fatalStartError(Translation
                            // .getTranslation("dialog.bind_error"));
                            // return false; // Shouldn't reach this!
                        }
                    } catch (NumberFormatException e) {
                        logFine("Unable to read listener port ('" + portStr
                            + "') from config");
                    }
                }
                // If this is the GUI version we didn't kill the program yet,
                // even though
                // there might have been multiple failed tries.
                if (connectionListener == null) {
                    portBindFailureProblem(ports);
                }
            }
        }

        /* Check whether to start D2D, too */
        boolean useD2D = ConfigurationEntry.D2D_ENABLED.getValueBoolean(this);
        int     port   = ConfigurationEntry.D2D_PORT.getValueInt(this);

        if(useD2D) {
            logInfo("D2D is enabled");

            boolean listenerOpened = openListener(port, useD2D);

            if(!listenerOpened) {
                logSevere("Couldn't bind to port " + port);
            } else logInfo("Listening on D2D port " + port);
        }

        return true;
    }

    /**
     * Call to notify the Controller of a problem while binding a required
     * listening socket.
     *
     * @param ports
     */
    private void portBindFailureProblem(String ports) {
        if (!isUIEnabled()) {
            logSevere("Unable to open incoming port from the portlist: "
                + ports);
            exit(1);
            return;
        }

        // Must use JOptionPane here because there is no Controller yet for
        // DialogFactory!
        int response = JOptionPane
            .showOptionDialog(
                null,
                Translation.get("dialog.bind_error.option.text"),
                Translation.get("dialog.bind_error.option.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{
                    Translation
                        .get("dialog.bind_error.option.ignore"),
                    Translation.get("dialog.bind_error.option.exit")},
                0);
        switch (response) {
            case 1 :
                exit(0);
                break;
            default :
                bindRandomPort();
                break;
        }
    }

    /**
     * Tries to bind a random port
     */
    private void bindRandomPort() {
        if ((openListener(ConnectionListener.DEFAULT_PORT, false)
            || openListener(0, false))
            && connectionListener != null)
        {
            nodeManager.getMySelf().getInfo()
                .setConnectAddress(connectionListener.getAddress());
        } else {
            logSevere("failed to open random port!!!");
            fatalStartError(Translation.get("dialog.bind_error"));
        }
    }

    /**
     * Starts all configured connection listener
     */
    private void startConfiguredListener() {
        // Start the connection listener
        if (connectionListener != null) {
            try {
                connectionListener.start();
            } catch (ConnectionException e) {
                logSevere("Problems starting listener " + connectionListener, e);
            }
            for (ConnectionListener additionalConnectionListener : additionalConnectionListeners)
            {
                try {
                    additionalConnectionListener.start();
                } catch (ConnectionException e) {
                    logSevere("Problems starting listener "
                        + connectionListener, e);
                }
            }
        }
    }

    /**
     * Saves the current config to disk
     */
    public synchronized void saveConfig() {
        if (!started) {
            return;
        }
        logFine("Saving config (" + getConfigName() + ".config)");

        Path file;
        Path tempFile;
        Path folderFile;
        Path tempFolderFile;
        Path backupFile;
        if (getConfigLocationBase() == null) {
            file = Paths.get(getConfigName() + ".config").toAbsolutePath();
            tempFile = Paths.get(getConfigName() + ".writing.config")
                .toAbsolutePath();
            folderFile = Paths.get(getConfigName() + "-Folder.config")
                .toAbsolutePath();
            tempFolderFile = Paths.get(
                getConfigName() + "-Folder.writing.config").toAbsolutePath();
            backupFile = Paths.get(getConfigName() + ".config.backup")
                .toAbsolutePath();
        } else {
            file = getConfigLocationBase().resolve(getConfigName() + ".config");
            tempFile = getConfigLocationBase().resolve(
                getConfigName() + ".writing.config").toAbsolutePath();
            backupFile = getConfigLocationBase().resolve(
                getConfigName() + ".config.backup");
            folderFile = getConfigLocationBase().resolve(
                getConfigName() + "-Folder.config");
            tempFolderFile = getConfigLocationBase().resolve(
                getConfigName() + "-Folder.writing.config").toAbsolutePath();
        }

        try {
            // Backup is done in #backupConfigAssets
            Files.deleteIfExists(backupFile);

            String distName = "PowerFolder";
            if (distribution != null
                && StringUtils.isNotBlank(distribution.getName()))
            {
                distName = distribution.getName();
            }

            Properties prev = new Properties();
            if (Files.exists(file)) {
                try (BufferedInputStream in = new BufferedInputStream(
                    Files.newInputStream(file))) {
                    prev.load(in);
                }
            }

            if (!prev.equals(config.getRegular())) {
                // Store config in misc base
                PropertiesUtil.saveConfig(tempFile, config.getRegular(),
                    distName + " config file (v" + PROGRAM_VERSION + ')');
                Files.deleteIfExists(file);
                try {
                    Files.move(tempFile, file);
                } catch (IOException e) {
                    Files.copy(tempFile, file);
                    Files.delete(tempFile);
                }
            } else {
                if (isFine()) {
                    logFine("Not storing config to " + file
                        + ". Base config remains unchanged");
                }
            }

            if (!config.getFolders().isEmpty()) {
                Properties prevFolders = new Properties();
                if (Files.exists(folderFile)) {
                    try (BufferedInputStream in = new BufferedInputStream(
                        Files.newInputStream(folderFile))) {
                        prevFolders.load(in);
                    }
                }
                if (!prevFolders.equals(config.getFolders())) {
                    PropertiesUtil
                        .saveConfig(tempFolderFile, config.getFolders(),
                            distName + " folders config file (v"
                                + PROGRAM_VERSION + ')');
                    Files.deleteIfExists(folderFile);
                    try {
                        Files.move(tempFolderFile, folderFile);
                    } catch (IOException e) {
                        Files.copy(tempFolderFile, folderFile);
                        Files.delete(tempFolderFile);
                    }
                }
            }
        } catch (IOException e) {
            // FATAL
            logSevere("Unable to save config. " + e, e);
            exit(1);
        } catch (Exception e) {
            // major problem , setting code is wrong
            e.printStackTrace();
            logSevere("major problem , setting code is wrong", e);
        }
    }

    /**
     * Answers if controller is started (by config)
     *
     * @return true if controller is started (by config)
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @return true is shutdown still in progress
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * the uptime in milliseconds.
     *
     * @return The uptime time in millis, or -1 if not started yet
     */
    public long getUptime() {
        if (startTime == null) {
            return -1;
        }
        return System.currentTimeMillis() - startTime.getTime();
    }

    /**
     * @return Name of the JAR file on windows installations.
     */
    public String getJARName() {
        if (distribution != null && distribution.getBinaryName() != null) {
            return distribution.getBinaryName() + ".jar";
        }
        logSevere("Unable to get JAR name for distribution: " + distribution,
            new RuntimeException());
        return "PowerFolder.jar";
    }

    /**
     * @return Name of the L4J INI file on windows installations.
     */
    public String getL4JININame() {
        if (distribution != null && distribution.getBinaryName() != null) {
            return distribution.getBinaryName() + ".l4j.ini";
        }
        logSevere("Unable to get l4j.ini name for distribution: "
            + distribution);
        return "PowerFolder.l4j.ini";
    }

    /**
     * Sets the paused mode.
     *
     * @param newPausedValue
     */
    public void setPaused(boolean newPausedValue) {
        setPaused0(newPausedValue, false);
    }

    /**
     * Sets the paused mode.
     *
     * @param newPausedValue
     */
    private synchronized void setPaused0(boolean newPausedValue,
        boolean changedByAdaptiveLogic)
    {
        boolean oldPausedValue = paused;
        paused = newPausedValue;

        if (newPausedValue) {
            folderRepository.getFolderScanner().abortScan();
            transferManager.abortAllDownloads();
            transferManager.abortAllUploads();
        } else {
            folderRepository.triggerMaintenance();
            folderRepository.getFileRequestor().triggerFileRequesting();
            for (Folder folder : folderRepository.getFolders()) {
                folder.broadcastFileRequestCommand();
            }
        }
        if (oldPausedValue != newPausedValue) {
            transferManager.updateSpeedLimits();
        }
        PreferencesEntry.PAUSED.setValue(this, newPausedValue);
        pausedModeListenerSupport.setPausedMode(new PausedModeEvent(
            newPausedValue));

        if (pauseResumeFuture != null) {
            try {
                pauseResumeFuture.cancel(true);
                if (!removeScheduled(pauseResumeFuture)) {
                    logSevere("Unable to remove pause task: "
                        + pauseResumeFuture, new RuntimeException(
                        "Unable to remove pause task: " + pauseResumeFuture));
                }
                logFine("Cancelled resume task");
            } catch (Exception e) {
                e.printStackTrace();
                logSevere(e);
            }
        }
        int delay = ConfigurationEntry.PAUSE_RESUME_SECONDS.getValueInt(this);
        if (newPausedValue) {
            if (delay == 0) {
                // User adaptive. Check for user inactivity
                pauseResumeFuture = scheduleAndRepeat(
                    new PauseResumeTask(true), 1000);
            } else if (delay < Integer.MAX_VALUE) {
                pauseResumeFuture = schedule(new PauseResumeTask(false),
                    delay * 1000);
                logFine("Scheduled resume task in " + delay + " seconds.");
            }
        } else {
            if (delay == 0 && changedByAdaptiveLogic) {
                // Turn on pause again when user gets active
                pauseResumeFuture = scheduleAndRepeat(
                    new PauseResumeTask(true), 1000);
            } else {
                pauseResumeFuture = null;
            }
        }
    }

    /**
     * @return true if the controller is paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Answers if node is running in LAN only networking mode
     *
     * @return true if in LAN only mode else false
     */
    public boolean isLanOnly() {
        return getNetworkingMode() == NetworkingMode.LANONLYMODE;
    }

    /**
     * If this client is running in backup only mode.
     * <p>
     * Backup only client feature. Controls:
     * <p>
     * 1) If the client can send invitations
     * <p>
     * 2) If the client can add friends
     * <p>
     * 3) The client can connect to others except the server.
     *
     * @return true if running as backup only client.
     */
    public boolean isBackupOnly() {
        return false;
    }

    /**
     * returns the enum with the current networkin mode.
     *
     * @return The Networking mode either NetworkingMode.PUBLICMODE,
     *         NetworkingMode.PRIVATEMODE or NetworkingMode.LANONLYMODE
     */
    public NetworkingMode getNetworkingMode() {
        if (networkingMode == null) {
            if (isBackupOnly()) {
                // ALWAYS server only mode.
                networkingMode = NetworkingMode.SERVERONLYMODE;
                return networkingMode;
            }
            // default = private
            String value = ConfigurationEntry.NETWORKING_MODE.getValue(this);
            try {
                networkingMode = NetworkingMode.valueOf(value);
            } catch (Exception e) {
                logSevere(
                    "Unable to read networking mode, reverting to PRIVATE_ONLY_MODE: "
                        + e.toString(), e);
                networkingMode = NetworkingMode.PRIVATEMODE;
            }
        }
        return networkingMode;
    }

    public void addPausedModeListener(PausedModeListener listener) {
        ListenerSupportFactory.addListener(pausedModeListenerSupport, listener);
    }

    public void removePausedModeListener(PausedModeListener listener) {
        ListenerSupportFactory.removeListener(pausedModeListenerSupport,
            listener);
    }

    public void addNetworkingModeListener(NetworkingModeListener listener) {
        ListenerSupportFactory.addListener(networkingModeListenerSupport,
            listener);
    }

    public void removeNetworkingModeListener(NetworkingModeListener listener) {
        ListenerSupportFactory.removeListener(networkingModeListenerSupport,
            listener);
    }

    public void addLimitedConnectivityListener(
        LimitedConnectivityListener listener)
    {
        ListenerSupportFactory.addListener(limitedConnectivityListenerSupport,
            listener);
    }

    public void removeLimitedConnectivityListener(
        LimitedConnectivityListener listener)
    {
        ListenerSupportFactory.removeListener(
            limitedConnectivityListenerSupport, listener);
    }

    public void setNetworkingMode(NetworkingMode newMode) {
        setNetworkingMode(newMode, true);
    }
    
    public void setNetworkingMode(NetworkingMode newMode, boolean restartNodeManager) {
        if (isBackupOnly() && newMode != NetworkingMode.SERVERONLYMODE) {
            // ALWAYS server only mode if backup-only.
            newMode = NetworkingMode.SERVERONLYMODE;
            logWarning("Backup only client. Only supports server only networking mode");
        }
        logFine("setNetworkingMode: " + newMode);
        NetworkingMode oldMode = getNetworkingMode();
        if (newMode != oldMode) {
            ConfigurationEntry.NETWORKING_MODE.setValue(this, newMode.name());

            networkingMode = newMode;
            networkingModeListenerSupport
                .setNetworkingMode(new NetworkingModeEvent(oldMode, newMode));

            // Restart nodeManager
            // PFS-1922:
            if (restartNodeManager) {
                nodeManager.shutdown();
                nodeManager.start();
            }
            reconnectManager.buildReconnectionQueue();
        }
    }

    /**
     * Answers if this controller has restricted connection to the network
     *
     * @return true if no incoming connections, else false.
     */
    public boolean isLimitedConnectivity() {
        return limitedConnectivity;
    }

    public void setLimitedConnectivity(boolean limitedConnectivity) {
        boolean oldValue = this.limitedConnectivity;
        this.limitedConnectivity = limitedConnectivity;
        LimitedConnectivityEvent e = new LimitedConnectivityEvent(oldValue,
            limitedConnectivity);
        limitedConnectivityListenerSupport.setLimitedConnectivity(e);
    }

    /**
     * Shuts down controller and exits to system with the given status
     *
     * @param status
     *            the status to exit with.
     */
    public void exit(int status) {
        if (Feature.EXIT_ON_SHUTDOWN.isDisabled()) {
            System.err
                .println("Running in JUnit testmode, no system.exit() called");
            return;
        }
        shutdown();
        System.exit(status);
    }

    /**
     * Shuts down the controller and requests and moves into restart requested
     * state
     */
    public void shutdownAndRequestRestart() {
        restartRequested = true;
        shutdown();
    }

    /**
     * @return true if the controller was shut down, with the request to restart
     */
    public boolean isRestartRequested() {
        return restartRequested;
    }

    /**
     * Shuts down all activities of this controller
     */
    public synchronized void shutdown() {
        if (shuttingDown || !started) {
            return;
        }
        PathUtils.setIOExceptionListener(null);
        shuttingDown = true;
        logInfo("Shutting down...");
        setFirstStart(false);
        // if (started && !OSUtil.isSystemService()) {
        // // Save config need a started in that method so do that first
        // saveConfig();
        // }

        if (Profiling.isEnabled()) {
            logFine(Profiling.dumpStats());
        }

        // Save anything important that has not been handled.
        savePersistentObjects();

        // stop
        boolean wasStarted = started;
        started = false;
        startTime = null;

        PreferencesEntry.LAST_NODE_ID.setValue(this,
            LoginUtil.hashAndSalt(getMySelf().getId()));

        if (taskManager != null) {
            logFine("Shutting down task manager");
            taskManager.shutdown();
        }

        if (threadPool != null) {
            logFine("Shutting down global threadpool");
            threadPool.shutdownNow();
        }

        if (fbIntegration != null) {
            fbIntegration.shutdown();
        }

        if (isUIOpen()) {
            logFine("Shutting down UI");
            uiController.shutdown();
        }

        if (rconManager != null) {
            logFine("Shutting down RConManager");
            rconManager.shutdown();
        }

        logFine("Shutting down connection listener(s)");
        if (connectionListener != null) {
            connectionListener.shutdown();
        }
        for (ConnectionListener addListener : additionalConnectionListeners) {
            addListener.shutdown();
        }
        additionalConnectionListeners.clear();
        if (broadcastManager != null) {
            logFine("Shutting down broadcast manager");
            broadcastManager.shutdown();
        }

        if (transferManager != null) {
            logFine("Shutting down transfer manager");
            transferManager.shutdown();
        }

        if (nodeManager != null) {
            logFine("Shutting down node manager");
            nodeManager.shutdown();
        }

        if (ioProvider != null) {
            logFine("Shutting down io provider");
            ioProvider.shutdown();
        }

        // shut down folder repository
        if (folderRepository != null) {
            logFine("Shutting down folder repository");
            folderRepository.shutdown();
        }

        if (reconnectManager != null) {
            logFine("Shutting down reconnect manager");
            reconnectManager.shutdown();
        }

        if (pluginManager != null) {
            logFine("Shutting down plugin manager");
            pluginManager.shutdown();
        }

        if (MacUtils.isSupported()) {
            MacUtils.getInstance().removeAppReOpenedListener(this);
        }

        if (webClientLogin != null){
            webClientLogin.stop();
        }

        if (wasStarted) {
            System.out.println("------------ " + PowerFolder.NAME + " "
                + PROGRAM_VERSION + " Controller Shutdown ------------");
        }

        // remove current config
        // config = null;
        shuttingDown = false;
        logInfo("Shutting down done");

        LoggingManager.closeFileLogging();
        backupConfigAssets();
    }

    public ScheduledExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * Returns a debug report
     *
     * @return the Debug report.
     */
    public String getDebugReport() {
        return Debug.buildDebugReport(this);
    }

    /**
     * Writes the debug report to diks
     */
    public void writeDebugReport() {
        try {
            FileOutputStream fOut = new FileOutputStream(getConfigName()
                + ".report.txt");
            String report = getDebugReport();
            fOut.write(report.getBytes());
            fOut.close();
        } catch (FileNotFoundException e) {
            logSevere("FileNotFoundException", e);
        } catch (IOException e) {
            logSevere("IOException", e);
        }
    }

    /**
     * Answers the current config name loaded <configname>.properties
     *
     * @return The name of the current config
     */
    public String getConfigName() {
        if (configFilename == null) {
            return null;
        }
        String configName = configFilename;
        int dot = configName.indexOf('.');
        if (dot > 0) {
            configName = configName.substring(0, dot);
        }
        return configName;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public Path getConfigFolderFile() {
        return configFolderFile;
    }

    /**
     * Returns the config, read from the configfile.
     *
     * @return the config as properties object
     */
    public Properties getConfig() {
        return config;
    }

    /**
     * Returns the command line of the start
     *
     * @return The command line
     */
    public CommandLine getCommandLine() {
        return commandLine;
    }

    public String getCLIUsername() {
        return commandLine != null ? commandLine.getOptionValue("u") : null;
    }

    public String getCLIPassword() {
        return commandLine != null ? commandLine.getOptionValue("p") : null;
    }

    /**
     * Returns local preferences, Preferences are stored till the next start. On
     * windows they are stored in the registry.
     *
     * @return The preferences
     */
    public Preferences getPreferences() {
        return preferences;
    }

    /**
     * @return true if this is the first start of PowerFolder of this config.
     */
    public boolean isFirstStart() {
        return preferences.getBoolean("openwizard2", true);
    }

    public void setFirstStart(boolean bool) {
        preferences.putBoolean("openwizard2", bool);
    }

    /**
     * @return the distribution of this client.
     */
    public Distribution getDistribution() {
        return distribution;
    }

    /**
     * @return the configured update settings for the current distribution
     */
    public UpdateSetting getUpdateSettings() {
        return UpdateSetting.create(this);
    }

    /**
     * Answers the own identity, of course with no connection
     *
     * @return a referens to the member object representing myself.
     */
    @Override
    public Member getMySelf() {
        return nodeManager != null ? nodeManager.getMySelf() : null;
    }

    /**
     * Changes the nick and tells other nodes
     *
     * @param newNick
     *            the new nick
     * @param saveConfig
     *            true if the config should be save directly otherwise you have
     *            to do it by hand
     */
    public void changeNick(String newNick, boolean saveConfig) {
        getMySelf().setNick(newNick);
        ConfigurationEntry.NICK.setValue(this, getMySelf().getNick());
        if (saveConfig) {
            saveConfig();
        }
        // broadcast nickchange
        nodeManager.broadcastMessage(new SettingsChange(getMySelf()));
        if (isUIOpen()) {
            // Update title
            uiController.getMainFrame().updateTitle();
        }
    }

    /**
     * @return the io provider.
     */
    public IOProvider getIOProvider() {
        return ioProvider;
    }

    /**
     * @return the Online Storage client.
     */
    public ServerClient getOSClient() {
        return osClient;
    }

    /**
     * Retruns the plugin manager
     *
     * @return the plugin manager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Returns the dyndns manager
     *
     * @return the dyndns manager
     */
    public DynDnsManager getDynDnsManager() {
        return dyndnsManager;
    }

    /**
     * Returns the broadcast manager
     *
     * @return broadcast manager
     */
    public BroadcastMananger getBroadcastManager() {
        return broadcastManager;
    }

    /**
     * Returns the NodeManager
     *
     * @return the NodeManager
     */
    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public ReconnectManager getReconnectManager() {
        return reconnectManager;
    }

    public PersistentTaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Returns the folder repository
     *
     * @return the folder repository
     */
    public FolderRepository getFolderRepository() {
        return folderRepository;
    }

    /**
     * Returns the transfer manager of the controller
     *
     * @return transfer manager
     */
    public TransferManager getTransferManager() {
        return transferManager;
    }

    /**
     * ONLY USE THIS METHOD FOR TESTING PURPOSES!
     *
     * @param factory
     */
    public void setTransferManagerFactory(Callable<TransferManager> factory) {
        Reject.ifNull(factory, "TransferManager factory is null");
        if (transferManager != null) {
            throw new IllegalStateException("TransferManager was already set!");
        }
        transferManagerFactory = factory;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * Injects a security manager.
     *
     * @param securityManager
     *            the security manager to set.
     */
    public void setSecurityManager(SecurityManager securityManager) {
        logFiner("Security manager set: " + securityManager);
        this.securityManager = securityManager;
    }

    /**
     * Connects to a remote peer, with ip and port
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  address  Address to connect to
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return The connected {@link Node}
     */
    public Member connect(InetSocketAddress address) throws ConnectionException
    {
      return connect(address, false);
    }

    /** connect
     * Connects to a remote peer, with ip and port
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  address  Address to connect to
     * @param  useD2D   Whether to use D2D proto
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return The connected {@link Node}
     **/

    public Member
    connect(InetSocketAddress address,
      boolean useD2D) throws ConnectionException
    {
      if(!started)
        {
          logInfo("NOT Connecting to " + address + ". Controller not started");

          throw new ConnectionException("NOT Connecting to " + address
             + ". Controller not started");
        }

      if(0 >= address.getPort())
        {
          // connect to defaul port
          logWarning("Unable to connect, port illegal " + address.getPort());
        }

      logFine("Connecting to " + address + (useD2D ? "via D2D" : "") + "...");

      ConnectionHandler conHan = ioProvider.getConnectionHandlerFactory()
        .tryToConnect(address, useD2D);

      // Accept new node
      return nodeManager.acceptConnection(conHan);
    }

    /**
     * Connect to a remote node Interprets a string as connection string Format
     * is expeced as ' <connect host>' or ' <connect host>: <port>'
     *
     * @param connectStr
     * @return the member that connected under the given addresse
     * @throws ConnectionException
     * @returns the connected node
     */
    public Member connect(String connectStr) throws ConnectionException {
        return connect(Util.parseConnectionString(connectStr));
    }

    /**
     * Answers if controller is started in console mode
     *
     * @return true if in console mode
     */
    public boolean isConsoleMode() {
        if (commandLine != null) {
            if (commandLine.hasOption('s')) {
                return true;
            }
        }
        if (config != null) {
            if (ConfigurationEntry.DISABLE_GUI.getValueBoolean(this)) {
                return true;
            }
        }
        if (Feature.UI_ENABLED.isDisabled()) {
            return true;
        }
        return GraphicsEnvironment.isHeadless();
    }

    /**
     * Whether to display notifications bottom-left instead of the normal
     * bottom-right. Primarily a development switch for running two PFs on one
     * PC.
     *
     * @return true if notifications should be displayed on the left.
     */
    public boolean isNotifyLeft() {
        return commandLine != null && commandLine.hasOption('y');
    }

    /**
     * Opens the graphical user interface
     */
    private void openUI() {
        uiController.start();
    }

    /**
     * Answers if the user interface (ui) is enabled
     *
     * @return true if the user interface is enabled, else false
     */
    public boolean isUIEnabled() {
        return !isConsoleMode();
    }

    /**
     * Answers if we have the ui open
     *
     * @return true if the uiserinterface is actualy started
     */
    public boolean isUIOpen() {
        return uiController != null && uiController.isStarted();
    }

    /**
     * Exposing UIController, acces to all UserInterface elements
     *
     * @return the UIController
     */
    public UIController getUIController() {
        return uiController;
    }

    /**
     * Waits for the ui to open, afterwards it is guranteed that uicontroller is
     * started
     */
    public void waitForUIOpen() {
        if (!isUIEnabled()) {
            throw new IllegalStateException(
                "Unable to ui to open, ui is not enabled");
        }
        while (!isUIOpen()) {
            try {
                // Wait....
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logFiner("InterruptedException", e);
                break;
            }
        }
    }

    /**
     * Opens the listener on local port. The first listener is set to
     * "connectionListener". All others are added the the list of
     * additionalConnectionListeners.
     *
     * @param  port    Port to open listener to
     * @param  useD2D  Whether to use D2D proto (FIXME: Might be a bit
     *                 pointless this way but allows to use this proto
     *                 on any port later <kappel@powerfolder.com>)
     *
     * @return if succeeded
     */
    private boolean openListener(int port, boolean useD2D) {
        String bind = ConfigurationEntry.NET_BIND_ADDRESS.getValue(this);
        logFine("Opening incoming connection listener on port " + port
            + " on interface " + (bind != null ? bind : "(all)"));
        while (true) {
            try {
                ConnectionListener newListener = new ConnectionListener(this,
                    port, bind, useD2D);
                if (connectionListener == null) {
                    // its our primary listener
                    connectionListener = newListener;
                } else {
                    additionalConnectionListeners.add(newListener);
                }
                return true;
            } catch (ConnectionException e) {
                logWarning("Unable to bind to port " + port);
                logFiner("ConnectionException", e);
                if (bind != null) {
                    logSevere("This could've been caused by a binding error on the interface... Retrying without binding");
                    bind = null;
                } else { // Already tried binding once or not at all so get
                    // out
                    return false;
                }
            }
        }
    }

    /**
     * Do we have a connection listener?
     *
     * @return true if we have a connection listener, otherwise false
     */
    public boolean hasConnectionListener() {
        return connectionListener != null;
    }

    /**
     * Gets the connection listener
     *
     * @return the connection listener
     */
    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    /** getAdditionalConnectionListeners
     * Gets a list of registered connection listeners
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return List of {@link ConnectionListener}
     **/

    public List<ConnectionListener> getAdditionalConnectionListeners() {
        return this.additionalConnectionListeners;
    }

    /**
     * Answers if this controller is runing in verbose mode. Set verbose=true on
     * config file to enable this, this gives access to all kinds of debugging
     * stuff.
     *
     * @return true if we are in verbose mode
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Answers if debug reports should be requested. Set debugReports=true on
     * config file to enable this, this request node information. Only enabled
     * if in verbose mode.
     *
     * @see RequestNodeInformation
     * @return true if we are in verbose mode
     */
    public boolean isDebugReports() {
        return debugReports && verbose;
    }

    /**
     * Returns the buildtime of this jar
     *
     * @return the Date the application jar was build.
     */
    public Date getBuildTime() {
        Path jar = Paths.get(getJARName());
        if (Files.exists(jar)) {
            try {
                return new Date(Files.getLastModifiedTime(jar).toMillis());
            } catch (IOException ioe) {
                return null;
            }
        }
        return null;
    }

    /**
     * Sets the loading completion of this controller. Used in the splash
     * screen.
     *
     * @param percentage
     *            the percentage complete
     */
    private void setLoadingCompletion(int percentage, int nextPerc) {
        if (uiController != null) {
            uiController.setLoadingCompletion(percentage, nextPerc);
        }
    }

    /**
     * Answers if minimized start is wanted. Use startup option -m to enable
     * this.
     *
     * @return if a minimized startup should be performed.
     */
    public boolean isStartMinimized() {
        return commandLine != null && commandLine.hasOption('m');
    }

    /**
     * The base directory where to store/load config files. or null if on
     * working path
     *
     * @return The File object representing the absolute location of where the
     *         config files are/should be stored.
     */
    private Path getConfigLocationBase() {
        // First check if we have a config file in local path
        Path aConfigFile = Paths.get(getConfigName() + ".config")
            .toAbsolutePath();

        // Load configuration in misc file if config file if in
        if (OSUtil.isWebStart() || Files.notExists(aConfigFile)) {
            if (isFiner()) {
                logFiner("Config location base: "
                    + getMiscFilesLocation().toString());
            }
            return getMiscFilesLocation();
        }

        // Otherwise use local path as configuration base
        return null;
    }

    /**
     * Answers the path, where to load/store miscellanouse files created by
     * PowerFolder. e.g. .nodes files
     *
     * @return the file base, a directory
     */
    public static Path getMiscFilesLocation() {
        Path base;
        Path unixConfigDir = Paths.get(System.getProperty("user.home"),
            "." + Constants.MISC_DIR_NAME).toAbsolutePath();
        if (OSUtil.isWindowsSystem()
            && Feature.WINDOWS_MISC_DIR_USE_APP_DATA.isEnabled())
        {
            String appData;
            if (Feature.CONFIGURATION_ALL_USERS.isEnabled()) {
                appData = WinUtils.getAppDataAllUsers();
            } else {
                appData = WinUtils.getAppDataCurrentUser();
            }

            if (StringUtils.isBlank(appData)) {
                // Appdata not found. Fallback.
                return unixConfigDir;
            }

            Path windowsConfigDir = Paths.get(appData, Constants.MISC_DIR_NAME)
                .toAbsolutePath();
            base = windowsConfigDir;

            // Check if migration is necessary
            if (Files.exists(unixConfigDir)) {
                boolean migrateConfig;
                if (Files.exists(windowsConfigDir)) {
                    // APPDATA/PowerFolder does not yet contain a config file OR
                    Filter<Path> filter = new Filter<Path>() {
                        @Override
                        public boolean accept(Path entry) {
                            return entry.getFileName().toString()
                                .endsWith("config");
                        }
                    };
                    try (DirectoryStream<Path> stream = Files
                        .newDirectoryStream(windowsConfigDir, filter)) {
                        migrateConfig = !stream.iterator().hasNext();
                    } catch (IOException ioe) {
                        log.info(ioe.getMessage());
                        migrateConfig = true;
                    }
                } else {
                    // Migrate if APPDATA/PowerFolder not existing yet.
                    migrateConfig = true;
                }

                if (migrateConfig) {
                    boolean migrationOk = migrateWindowsMiscLocation(
                        unixConfigDir, windowsConfigDir);
                    if (!migrationOk) {
                        // Fallback, migration failed.
                        base = unixConfigDir;
                    }
                }
            }
        } else {
            base = unixConfigDir;
        }
        if (Files.notExists(base)) {
            try {
                Files.createDirectories(base);
            } catch (IOException ioe) {
                log.severe("Failed to create "
                    + base.toAbsolutePath().toString() + ". " + ioe);
            }
        }
        return base;
    }

    /**
     * Migrate config dir (if necessary) in Windows from user.home to APPDATA.
     * Pre Version 4, the config was in 'user.home'/.PowerFolder.
     * 'APPDATA'/PowerFolder is a more normal Windows location for application
     * data.
     *
     * @param unixBaseDir
     *            the old user.home based config directory.
     * @param windowsBaseDir
     *            the preferred APPDATA based config directory.
     */
    private static boolean migrateWindowsMiscLocation(Path unixBaseDir,
        Path windowsBaseDir)
    {
        if (Files.notExists(windowsBaseDir)) {
            try {
                Files.createDirectories(windowsBaseDir);
            } catch (IOException ioe) {
                log.severe("Failed to create "
                    + windowsBaseDir.toAbsolutePath().toString() + ". " + ioe);
            }
        }
        try {
            PathUtils.recursiveMove(unixBaseDir, windowsBaseDir);
            log.warning("Migrated config from " + unixBaseDir + " to "
                + windowsBaseDir);
            return true;
        } catch (IOException e) {
            log.warning("Failed to migrate config from " + unixBaseDir + " to "
                + windowsBaseDir + ". " + e);
            return false;
        }
    }

    /**
     * Answers the path, where to load/store temp files created by PowerFolder.
     *
     * @return the file base, a directory
     */
    public static Path getTempFilesLocation() {
        Path base = Paths.get(System.getProperty("java.io.tmpdir"));
        if (Files.notExists(base)) {
            try {
                Files.createDirectories(base);
            } catch (IOException ioe) {
                log.warning("Could not create temp files location '"
                    + base.toAbsolutePath().toString() + "'. " + ioe);
            }
        }
        return base;
    }

    private void killRunningInstance() {
        if (RemoteCommandManager.hasRunningInstance()) {
            logWarning("Found a running instance. Trying to shut it down...");
            RemoteCommandManager.sendCommand(RemoteCommandManager.QUIT);
            Waiter w = new Waiter(10000L);
            while (RemoteCommandManager.hasRunningInstance() && !w.isTimeout())
            {
                w.waitABit();
            }
            if (!RemoteCommandManager.hasRunningInstance()) {
                logInfo("Was able to shut down running instance. Continue normal");
                return;
            }
            logWarning("Was NOT able to shut down running instance.");
        }
    }

    /**
     * Called if controller has detected a already running instance
     */
    private void alreadyRunningCheck() {
        Component parent = null;
        if (isUIOpen()) {
            parent = uiController.getMainFrame().getUIComponent();
        }
        if (!isStartMinimized() && isUIEnabled() && !commandLine.hasOption('z'))
        {
            Object[] options = {Translation
                .get("dialog.already_running.show_button")};
            int exitOption = 0;
            if (verbose) {
                options = new Object[]{
                    Translation
                        .get("dialog.already_running.start_button"),
                    Translation
                        .get("dialog.already_running.exit_button")};
                exitOption = 1;
            }
            if (JOptionPane.showOptionDialog(parent,
                Translation.get("dialog.already_running.warning"),
                Translation.get("dialog.already_running.title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]) == exitOption)
            { // exit pressed
              // Try to bring existing instance to the foreground.
                RemoteCommandManager.sendCommand(RemoteCommandManager.SHOW_UI);
                exit(1);
            } else {
                exit(1);
            }
        } else {
            // If no gui show error but start anyways
            logWarning("PowerFolder already running");
        }
    }

    private void fatalStartError(String message) {
        Component parent = null;
        if (isUIOpen()) {
            parent = uiController.getMainFrame().getUIComponent();
        }
        if (isUIEnabled()) {
            Object[] options = {Translation
                .get("dialog.already_running.exit_button")};
            JOptionPane.showOptionDialog(parent, message,
                Translation.get("dialog.fatal_error.title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,
                options, options[0]);
        } else {
            logSevere(message);
        }
        exit(1);
    }

    private void initDistribution() {
        try {
            if (ConfigurationEntry.DIST_CLASSNAME.hasNonBlankValue(getController())) {
                Class<?> distClass = Class
                    .forName(ConfigurationEntry.DIST_CLASSNAME
                        .getValue(getController()));
                distribution = (Distribution) distClass.newInstance();
            }

            if (distribution == null) {
                ServiceLoader<Distribution> brandingLoader = ServiceLoader
                    .load(Distribution.class);
                for (Distribution br : brandingLoader) {
                    if (distribution != null) {
                        logWarning("Found multiple distribution classes: "
                            + br.getName() + ", already using "
                            + distribution.getName());
                        break;
                    }
                    distribution = br;
                }
            }

            if (distribution == null) {
                if (ProUtil.isRunningProVersion()) {
                    distribution = new PowerFolderPro();
                } else {
                    distribution = new PowerFolderBasic();
                }
                logFine("Distributon not found. Falling back to "
                    + distribution.getName());
            }
            distribution.init(this);
            logInfo("Running distribution: " + distribution.getName());
        } catch (Exception e) {
            logSevere("Failed to initialize distribution "
                + (distribution == null ? "null" : distribution.getName()), e);

            // Fallback
            try {
                if (distribution == null) {
                    if (ProUtil.isRunningProVersion()) {
                        distribution = new PowerFolderPro();
                    } else {
                        distribution = new PowerFolderBasic();
                    }
                }
                logInfo("Running distribution: " + distribution.getName());
                distribution.init(this);
            } catch (Exception e2) {
                logSevere("Failed to initialize fallback distribution", e2);
            }
        }
    }

    /**
     * Answers the waittime for threads time differst a bit to avoid
     * concurrencies
     *
     * @return The time to wait
     */
    public static long getWaitTime() {
        return WAIT_TIME;
    }

    @Override
    public String toString() {
        return "Controller '" + getMySelf() + '\'';
    }

    /**
     * Distribute ask for friendship events.
     *
     * @param event
     */
    public void makeFriendship(MemberInfo memberInfo) {
        if (networkingMode == NetworkingMode.SERVERONLYMODE) {
            logFine("Ignoring ask for friendship from client " + memberInfo
                + ". Running in server only mode");
            return;
        }

        // Is this a friend already?
        Member member = memberInfo.getNode(this, false);
        if (member != null) {
            if (member.isFriend()) {
                log.fine("Ignoring ask for friendship from "
                    + memberInfo.getNick() + ". Already friend.");
                return;
            }
            if (member.isServer()) {
                log.fine("Ignoring ask for friendship from "
                    + memberInfo.getNick() + ". is a server.");
                return;
            }
            // Hack alert(tm):
            String lnick = member.getNick().toLowerCase();
            boolean isPowerFolderCloud = lnick.contains("powerfolder")
                && lnick.contains("cloud");
            if (isPowerFolderCloud) {
                log.fine("Ignoring ask for friendship from "
                    + memberInfo.getNick() + ". is a pf server.");
                return;
            }
        }

        // A new friend!
        member.setFriend(true, null);
    }

    /**
     * Distribute invitations.
     *
     * @param invitation
     */
    public void invitationReceived(Invitation invitation) {
        for (InvitationHandler handler : invitationHandlers) {
            handler.gotInvitation(invitation);
        }
    }

    /**
     * Distribute local mass deletion notifications.
     *
     * @param event
     */
    public void localMassDeletionDetected(LocalMassDeletionEvent event) {
        for (MassDeletionHandler massDeletionHandler : massDeletionHandlers) {
            massDeletionHandler.localMassDeletion(event);
        }
    }

    /**
     * Distribute remote mass deletion notifications.
     *
     * @param event
     */
    public void remoteMassDeletionDetected(RemoteMassDeletionEvent event) {
        for (MassDeletionHandler massDeletionHandler : massDeletionHandlers) {
            massDeletionHandler.remoteMassDeletion(event);
        }
    }

    /**
     * Save anything important that was not handled.
     */
    private void savePersistentObjects() {

        if (started && isUIEnabled()) {

            // Save unhandled notices.
            List<Notice> notices = new ArrayList<Notice>();
            for (Notice notice : uiController.getApplicationModel()
                .getNoticesModel().getAllNotices())
            {
                if (notice.isPersistable()) {
                    notices.add(notice);
                }
            }
            Path file = getMiscFilesLocation().resolve(
                getConfigName() + ".notices");
            try (ObjectOutputStream outputStream = new ObjectOutputStream(
                Files.newOutputStream(file))) {
                logInfo("There are " + notices.size() + " notices to persist.");
                outputStream.writeUnshared(notices);
            } catch (FileNotFoundException e) {
                logSevere("FileNotFoundException", e);
            } catch (IOException e) {
                logSevere("IOException", e);
            }
        }
    }

    /**
     * Load anything that was not handled last time.
     */
    @SuppressWarnings("unchecked")
    private void loadPersistentObjects() {

        if (isUIEnabled()) {
            // Load notices.
            Path file = getMiscFilesLocation().resolve(
                getConfigName() + ".notices");
            if (Files.exists(file)) {
                logInfo("Loading notices");
                try (ObjectInputStream inputStream = new ObjectInputStream(
                    Files.newInputStream(file))) {
                    List<Notice> notices = (List<Notice>) inputStream
                        .readObject();
                    inputStream.close();
                    for (Notice notice : notices) {
                        uiController.getApplicationModel().getNoticesModel()
                            .handleSystemNotice(notice, true);
                    }
                    logFine("Loaded " + notices.size() + " notices.");
                } catch (FileNotFoundException e) {
                    logSevere("FileNotFoundException", e);
                } catch (IOException e) {
                    logSevere("IOException", e);
                } catch (ClassNotFoundException e) {
                    logSevere("ClassNotFoundException", e);
                } catch (ClassCastException e) {
                    logSevere("ClassCastException", e);
                }
            } else {
                logInfo("No notices found - probably first start of PF.");
            }
        }
    }

    /**
     * Wait for the repo to finish syncing. Then request system shutdown and
     * exit PF.
     *
     * @param password
     *            required only for Linux shutdowns.
     */
    public void shutdownAfterSync(final String password) {
        final AtomicBoolean oneShot = new AtomicBoolean(true);
        scheduleAndRepeat(new Runnable() {
            @Override
            public void run() {
                // ALPS Problem: Change to check for all in sync.

                if (oneShot.get() && folderRepository.isInSync()) {
                    // Make sure we only try to shutdown once,
                    // in case the user aborts the shutdown.
                    oneShot.set(false);
                    log.info("Sync and shutdown in sync.");
                    if (SystemUtil.shutdown(password)) {
                        log.info("Shutdown command issued.");
                        exit(0);
                    } else {
                        log.warning("Shutdown command failed.");
                    }
                }
            }
        }, 10000, 10000);
    }

    /**
     * Waits for the repo to finish syncing. Then request system shutdown and
     * exit PF.
     *
     * @param secWait
     *            number of seconds to wait.
     */
    public void exitAfterSync(int secWait) {
        logInfo("Sync and exit initiated. Begin check in " + secWait + 's');
        final AtomicBoolean oneShot = new AtomicBoolean(true);
        scheduleAndRepeat(new Runnable() {
            @Override
            public void run() {
                // ALPS Problem: Change to check for all in sync.
                if (oneShot.get() && folderRepository.isInSync()) {
                    // Make sure we only try to shutdown once,
                    // in case the user aborts the shutdown.
                    oneShot.set(false);
                    log.info("I'm in sync - exit now. Sync and exit was triggered.");
                    exit(0);
                }
            }
        }, 1000L * secWait, 10000);
    }

    /**
     * #2485
     */
    private class PauseResumeTask extends TimerTask {
        private final boolean userAdaptive;

        public PauseResumeTask(boolean whenUserIsInactive) {
            this.userAdaptive = whenUserIsInactive;
        }

        @Override
        public void run() {
            if (userAdaptive && isUIOpen()) {
                ApplicationModel appModel = uiController.getApplicationModel();
                if (appModel.isUserActive()) {
                    if (!isPaused()) {
                        getController().schedule(new Runnable() {
                            @Override
                            public void run() {
                                setPaused0(true, true);
                                log.info("User active. Executed pause task.");
                            }
                        }, 50);
                    }
                } else {
                    // Resume if user is not active
                    if (isPaused()) {
                        getController().schedule(new Resumer(), 50);
                    }
                }
            } else {
                // Simply unpause after X seconds
                setPaused0(false, true);
                log.info("Executed resume task.");
            }
        }
    }

    private class Resumer implements Runnable {
        @Override
        public void run() {
            setPaused0(false, true);
            log.info("User inactive. Executed resume task.");
        }
    }

}
