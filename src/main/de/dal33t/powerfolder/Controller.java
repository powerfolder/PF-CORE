/* $Id: Controller.java,v 1.107 2006/05/01 19:20:42 schaatser Exp $
 */
package de.dal33t.powerfolder;

import java.awt.Component;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Security;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.message.SettingsChange;
import de.dal33t.powerfolder.net.*;
import de.dal33t.powerfolder.plugin.PluginManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * Central class gives access to all core components in PowerFolder. Make sure
 * To extend PFComponent so you always have a refrence to this class.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.107 $
 */
public class Controller extends PFComponent {
    /**
     * cache the networking mode in a field so we dont heve to do all this
     * comparing
     */
    private NetworkingMode networkingMode;

    /**
     * the (java beans like) property, listen to changes of the networkng mode
     * by calling addPropertyChangeListener with this as parameter
     */
    public static final String PROPERTY_NETWORKING_MODE = "networkingMode";

    /**
     * program version. include "devel" if its a development version.
     */
    public static final String PROGRAM_VERSION = "1.0.2 devel";

    /** general wait time for all threads (5000 is a balanced value) */
    private static final long WAIT_TIME = 5000;

    /** the default config file */
    private static final String DEFAULT_CONFIG_FILE = "PowerFolder.config";

    /** The command line entered by the user when starting the program */
    private CommandLine commandLine;

    /** filename of the current configFile */
    private String configFile;

    /** The config properties */
    private Properties config;

    /** Program start time */
    private Date startTime;

    /** Are we in started state? */
    private boolean started;

    /** Are we trying to shutdown? */
    private boolean shuttingDown;

    /** Is a restart requested */
    private boolean restartRequested;

    /** Are we in verbose mode? */
    private boolean verbose;

    /** The nodemanager that holds all members */
    private NodeManager nodeManager;

    /** The FolderRepository that holds all "joined" folders */
    private FolderRepository folderRepository;

    /** The Listener to incomming connections of other PowerFolder clients */
    private ConnectionListener connectionListener;

    /**
     * besides the default listener we may have a list of connection listeners
     * that listen on other ports
     */
    private List<ConnectionListener> additionalConnectionListeners;

    /** The BroadcastManager send "broadcasts" on the LAN so we can */
    private BroadcastMananger broadcastManager;

    /**
     * The DynDNS manager that handles the working arwound for user with a
     * dynnamip IP address.
     */
    private DynDnsManager dyndnsManager;

    /** Handels the up and downloads */
    private TransferManager transferManager;

    /**
     * Remote Commands listener, a protocol handler for powerfolder links:
     * powerfolder://
     */
    private RemoteCommandManager rconManager;

    /** Holds the User interface */
    private UIController uiController;

    /** holds all installed plugins */
    private PluginManager pluginManager;

    /** Handles the movement of files from and to the powerfolder recycle bin */
    private RecycleBin recycleBin;

    /**
     * the currently used socket to connect to a new member used in shutdown,
     * connection try ofter take 30s
     */
    private Socket currentConnectingSocket;

    /**
     * A global timer used for sheduling things like updates every x seconds in
     * the UI
     */
    private Timer timer;

    private Controller() {
        super();
        // Do some TTL fixing for dyndns resolving
        Security.setProperty("networkaddress.cache.ttl", "0");
        System.setProperty("sun.net.inetaddr.ttl", "0");

        // Default exception logger
        Thread
            .setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                public void uncaughtException(Thread t, Throwable e) {
                    log().error("Exception in " + t + ": " + e.toString(), e);
                }

            });
    }

    /**
     * Overwite the PFComponent.getController() otherwise that one returns null
     * for this Controller itself.
     * 
     * @return a reference to this
     */
    public Controller getController() {
        return this;
    }

    /**
     * Creates a fresh Controller
     * 
     * @return the controller
     */
    public static Controller createController() {
        return new Controller();
    }

    /**
     * Starts this controller loading the config from the default config file
     * 
     * @see #DEFAULT_CONFIG_FILE
     */
    public void startDefaultConfig() {
        startConfig(DEFAULT_CONFIG_FILE);
    }

    /**
     * Starts a config with the given command line arguments
     * 
     * @param aCommandLine
     *            the command line as specified by the user
     */
    public void startConfig(CommandLine aCommandLine) {
        this.commandLine = aCommandLine;
        String configName = aCommandLine.getOptionValue("c");
        startConfig(configName);
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
        if (isStarted()) {
            throw new IllegalStateException(
                "Configuration already started, shutdown controller first");
        }

        additionalConnectionListeners = Collections
            .synchronizedList(new ArrayList<ConnectionListener>());
        started = false;

        // Initalize resouce bundle eager
        // check forced language file from commandline
        if (getCommandLine() != null && getCommandLine().hasOption("f")) {
            String langfilename = getCommandLine().getOptionValue("f");
            try {
                ResourceBundle resourceBundle = new ForcedLanguageFileResourceBundle(
                    langfilename);
                Translation.setResourceBundle(resourceBundle);
            } catch (FileNotFoundException fnfe) {
                log().error(
                    "forced language file (" + langfilename + ") not found: "
                        + fnfe.getMessage());
                log().error("using setup language");
                Translation.resetResourceBundle();
                Translation.getResourceBundle();
            } catch (IOException ioe) {
                log().error(
                    "forced language file io error: " + ioe.getMessage());
                log().error("using setup language");
                Translation.resetResourceBundle();
                Translation.getResourceBundle();
            }
        } else {
            Translation.resetResourceBundle();
            Translation.getResourceBundle();
        }

        Logger.setLogBuffer(50000);

        // loadConfigFile
        if (!loadConfigFile(filename)) {
            return;
        }

        if (isUIEnabled()) {
            uiController = new UIController(this);
        }

        setLoadingCompletion(0);

        // initialize logger
        initLogger();

        if (isTester()) {
            log()
                .warn(
                    "Testers mode enabled, will check for new development versions");
        }

        timer = new Timer("Controller schedule timer", true);

        // initialize dyndns manager
        dyndnsManager = new DynDnsManager(this);

        // initialize transfer manager
        transferManager = new TransferManager(this);

        setLoadingCompletion(10);

        // start node manager
        nodeManager = new NodeManager(this);

        // Folder repository
        folderRepository = new FolderRepository(this);

        setLoadingCompletion(20);

        // initialize listener on local port
        if (!initializeListenerOnLocalPort()) {
            return;
        }

        setLoadingCompletion(30);

        // Start the nodemanager
        nodeManager.start();
        setLoadingCompletion(35);

        // init repo (read folders)
        folderRepository.init();
        // init of folders takes rather long so a big difference with
        // last number to get smooth bar... ;-)
        setLoadingCompletion(60);

        // load recycle bin needs to be done after folder repo init
        // and before repo start
        recycleBin = new RecycleBin(this);

        // start repo maintainance Thread
        folderRepository.start();
        setLoadingCompletion(65);

        // Start the transfer manager thread
        transferManager.start();
        setLoadingCompletion(70);

        // Initalize rcon manager
        startRConManager();

        setLoadingCompletion(75);

        // Start all configured listener if not in silent mode
        startConfiguredListener();
        setLoadingCompletion(80);

        // open broadcast listener
        openBroadcastManager();

        setLoadingCompletion(85);
        // Controller now started
        started = true;
        startTime = new Date();

        log().info("Controller started");

        // dyndns updater
        /*
        boolean onStartUpdate = ConfigurationEntry.DYNDNS_AUTO_UPDATE
            .getValueBoolean(this).booleanValue();
        if (onStartUpdate) {
            getDynDnsManager().onStartUpdate();
        }
        */
        getDynDnsManager().update();

        setLoadingCompletion(90);
        // open UI
        if (isConsoleMode()) {
            log().debug("Running in console");
        } else {
            log().debug("Opening UI");
            openUI();
        }

        // Initalize plugins
        pluginManager = new PluginManager(this);

        // Now start the connecting process
        nodeManager.startConnecting();

        setLoadingCompletion(100);
        if (!isConsoleMode()) {
            uiController.hideSplash();
        }

        // Add share
        // getFolderRepository().getShares().addShare(new File("/shares"));
    }

    private void initLogger() {
        // enabled verbose mode if in config
        String verboseStr = config.getProperty("verbose");
        if (verboseStr != null
            && ("yes".equalsIgnoreCase(verboseStr) || "true"
                .equalsIgnoreCase(verboseStr)))
        {
            verbose = true;
            // Remove debug directory
            Logger.deleteDebugDir();

            // Enable loggin
            Logger.setEnabledTextPanelLogging(true);
            Logger.setEnabledConsoleLogging(true);

            // MORE LOG
            String logFilename = getConfigName() + ".log.txt";
            Logger.setLogFile(logFilename);
            if (Logger.isLogToFileEnabled()) {
                log().info(
                    "Running in VERBOSE mode, logging to file '" + logFilename
                        + "'");
            } else {
                log()
                    .info(
                        "Running in VERBOSE mode, not logging to file (enable in Logger.java)'");
            }
        }
    }

    /**
     * Loads a config file (located in "getConfigLocationBase()")
     * 
     * @return false if unsuccesfull, true if file found and reading succeded.
     */
    private boolean loadConfigFile(String filename) {
        if (filename == null) {
            filename = DEFAULT_CONFIG_FILE;
        }

        if (filename.indexOf('.') < 0) {
            // append .config extension
            filename += ".config";
        }

        log().debug("Starting from configfile '" + filename + "'");
        configFile = null;
        config = new Properties();
        BufferedInputStream bis = null;
        try {
            configFile = filename;
            File file = new File(getConfigLocationBase(), filename);

            if (OSUtil.isWebStart()) {
                log()
                    .debug(
                        "WebStart, config file location: "
                            + file.getAbsolutePath());
            }
            bis = new BufferedInputStream(new FileInputStream(file));
            config.load(bis);
        } catch (FileNotFoundException e) {
            log().warn(
                "Unable to start config, file '" + filename
                    + "' not found, using defaults");
        } catch (IOException e) {
            log().error("Unable to start config from file '" + filename + "'");
            config = null;
            return false;
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return true;
    }

    /** use to schedule a task (like a ui updater) and repeat every period */
    public void scheduleAndRepeat(TimerTask task, long period) {
        if (!isShuttingDown()) {
            timer.schedule(task, period, period);
        }
    }

    public void scheduleAndRepeat(TimerTask task, int delay, long period) {
        if (!isShuttingDown()) {
            timer.schedule(task, delay, period);
        }
    }

    /** use to schedule a task */
    public void schedule(TimerTask task, long delay) {
        if (!isShuttingDown()) {
            timer.schedule(task, delay);
        }
    }

    /**
     * creates and starts the Broadcast manager, will not be created if config
     * property disablebroadcasts=true
     */
    private void openBroadcastManager() {
        if (!Boolean.valueOf(config.getProperty("disablebroadcasts"))
            .booleanValue())
        {
            try {
                broadcastManager = new BroadcastMananger(this);
                broadcastManager.start();
            } catch (ConnectionException e) {
                log()
                    .warn(
                        "Unable to open broadcast manager, you wont automatically join pf-network on local net: "
                            + e.getMessage());
                log().verbose(e);
            }
        } else {
            log().warn("Auto-local subnet connection disabled");
        }
    }

    /**
     * Starts the rcon manager
     */
    private void startRConManager() {
        if (!Boolean.valueOf(config.getProperty("disablercon")).booleanValue())
        {
            rconManager = new RemoteCommandManager(this);
            rconManager.start();
        } else {
            log().warn("RCon manager disabled");
        }
    }

    private static final int MAX_RANDOM_PORTS_TO_TRY = 20;

    /**
     * Starts a connection listener for each port found in config property
     * "port" ("," separeted), if "random-port" is set to "true" this "port" entry will be
     * ignored and a random port will be used (between 49152 and 65535).
     */
    private boolean initializeListenerOnLocalPort() {
        boolean random = ConfigurationEntry.NET_BIND_RANDOM_PORT
            .getValueBoolean(getController());
        if (random) {
            Random generator = new Random();
            int port = generator.nextInt(65535 - 49152) + 49152;
            int tryCount = 0;
            while (!openListener(port) && tryCount++ < MAX_RANDOM_PORTS_TO_TRY) {
                port = generator.nextInt(65535 - 49152) + 49152;
            }
            if (connectionListener == null) {
                log().error("failed to open random port!!!");
            } else {
                // set reconnect on first successfull listener
                nodeManager.getMySelf().getInfo().setConnectAddress(
                    connectionListener.getLocalAddress());
            }

        } else {
            String ports = ConfigurationEntry.NET_BIND_PORT
                .getValue(getController());
            if (!"0".equals(ports)) {
                if (ports == null) {
                    ports = "-1";
                }
                StringTokenizer nizer = new StringTokenizer(ports, ",");
                while (nizer.hasMoreElements()) {
                    String portStr = nizer.nextToken();
                    try {
                        int port = Integer.parseInt(portStr);
                        boolean listenerOpened = openListener(port);
                        if (listenerOpened && connectionListener != null) {
                            // set reconnect on first successfull listener
                            nodeManager.getMySelf().getInfo()
                                .setConnectAddress(
                                    connectionListener.getLocalAddress());
                        }
                        if (!listenerOpened) {
                            // Abort if listener cannot be bound
                            alreadyRunning();
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        log().debug(
                            "Unable to read listener port ('" + portStr
                                + "') from config");
                    }
                }
            } else {
                log().warn("Not opening connection listener. (port=0)");
            }
        }
        return true;
    }

    /**
     * Starts all configures connection listener
     */
    private void startConfiguredListener() {
        // Start the connection listener
        if (connectionListener != null) {
            try {
                connectionListener.start();
            } catch (ConnectionException e) {
                log().error("Problems starting listener " + connectionListener,
                    e);
            }
            for (Iterator it = additionalConnectionListeners.iterator(); it
                .hasNext();)
            {
                try {
                    ConnectionListener addListener = (ConnectionListener) it
                        .next();
                    addListener.start();
                } catch (ConnectionException e) {
                    log().error(
                        "Problems starting listener " + connectionListener, e);
                }
            }
        }
    }

    /**
     * Saves the current config to disk
     */
    public synchronized void saveConfig() {
        if (!isStarted()) {
            return;
        }
        log().debug("Saving config (" + getConfigName() + ".config)");
        OutputStream fOut;
        File file = new File(getConfigLocationBase(), getConfigName()
            + ".config");
        File backupFile = new File(getConfigLocationBase(), getConfigName()
            + ".config.backup");
        try {
            // make backup
            if (file.exists()) {
                FileUtils.copyFile(file, backupFile);
            }
            // Store config in misc base
            fOut = new BufferedOutputStream(new FileOutputStream(file));
            getConfig().store(fOut,
                "PowerFolder config file (v" + PROGRAM_VERSION + ")");
            fOut.close();
        } catch (IOException e) {
            log().error("Unable to save config", e);
        } catch (Exception e) {
            // major problem , setting code is wrong
            System.out.println("major problem , setting code is wrong");
            e.printStackTrace();
            log().error("major problem , setting code is wrong", e);
            // restore old settings file because it was probably flushed with
            // this error
            try {
                FileUtils.copyFile(backupFile, file);
            } catch (Exception e2) {

            }
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

    /** true is shutdown still in progress */
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
     * Answers if the controller is running in silentmode
     * 
     * @return true if the controller is running in silentmode
     */
    public boolean isSilentMode() {
        return getPreferences().getBoolean("silentMode", false);
    }

    /**
     * Sets the silent mode
     * 
     * @param silent
     *            true to turn on, false to turn off
     */
    public void setSilentMode(boolean silent) {
        boolean oldValue = isSilentMode();
        getPreferences().putBoolean("silentMode", silent);

        boolean silentModeStateChanged = oldValue != isSilentMode();

        if (silentModeStateChanged) {

            // if (silentMode) {
            // log().warn("Shutting down all incoming listener");
            // if (listener != null) {
            // listener.shutdown();
            // }
            // for (Iterator it = additionalListener.iterator(); it.hasNext();)
            // {
            // ConnectionListener addListener = (ConnectionListener) it
            // .next();
            // addListener.shutdown();
            // }
            // } else {
            // // Start all listener
            // startConfiguredListener();
            // }
        }
        //
        getTransferManager().updateSpeedLimits();
        firePropertyChange("silentMode", oldValue, isSilentMode());
    }

    /**
     * Answers if node is running in public networking mode
     * 
     * @return true if in public mode else false
     */
    public boolean isPublicNetworking() {
        return getNetworkingMode().equals(NetworkingMode.PUBLICMODE);
    }

    /**
     * Answers if node is running in private networking mode
     * 
     * @return true if in private mode else false
     */
    public boolean isPrivateNetworking() {
        return getNetworkingMode().equals(NetworkingMode.PRIVATEMODE);
    }

    /**
     * Answers if node is running in LAN only networking mode
     * 
     * @return true if in LAN only mode else false
     */
    public boolean isLanOnly() {
        return getNetworkingMode().equals(NetworkingMode.LANONLYMODE);
    }

    /**
     * Should ZIP compression on LAN be enabled.
     * 
     * @return true if lan compression should be enables else false.
     */
    public boolean useZipOnLan() {
        return ConfigurationEntry.USE_ZIP_ON_LAN.getValueBoolean(this);
    }

    /**
     * returns the enum with the current networkin mode.
     * 
     * @return The Networking mode either NetworkingMode.PUBLICMODE,
     *         NetworkingMode.PRIVATEMODE or NetworkingMode.LANONLYMODE
     */
    public NetworkingMode getNetworkingMode() {
        if (networkingMode == null) {
            // old settings remove in new
            if (getConfig().containsKey("publicnetworking")) {
                if ("true".equals(getConfig().getProperty("publicnetworking")))
                {
                    ConfigurationEntry.NETWORKING_MODE.setValue(this,
                        NetworkingMode.PUBLICMODE.name());
                } else {
                    ConfigurationEntry.NETWORKING_MODE.setValue(this,
                        NetworkingMode.PRIVATEMODE.name());
                }
                getConfig().remove("publicnetworking");
            }

            // default = private

            String value = ConfigurationEntry.NETWORKING_MODE.getValue(this);
            if (value.equalsIgnoreCase(NetworkingMode.LANONLYMODE.name())) {
                networkingMode = NetworkingMode.LANONLYMODE;
            } else if (value.equalsIgnoreCase(NetworkingMode.PUBLICMODE.name())) {
                networkingMode = NetworkingMode.PUBLICMODE;
            } else {
                networkingMode = NetworkingMode.PRIVATEMODE;
            }
        }
        return networkingMode;
    }

    public void setNetworkingMode(NetworkingMode newMode) {
        log().debug("setNetworkingMode: " + newMode);
        NetworkingMode oldValue = getNetworkingMode();
        if (!newMode.equals(oldValue)) {
            ConfigurationEntry.NETWORKING_MODE.setValue(this, newMode.name());
            switch (newMode) {
                case PUBLICMODE : {
                    break;
                }
                case PRIVATEMODE : {
                    getNodeManager().disconnectUninterestingNodes();
                    break;
                }
                case LANONLYMODE : {
                    getNodeManager().disconnectUninterestingNodes();
                    break;
                }
            }

            // Restart nodemanager
            nodeManager.shutdown();
            nodeManager.start();
            
            networkingMode = newMode;
            firePropertyChange(PROPERTY_NETWORKING_MODE, oldValue, newMode
                .toString());            
        }
    }

    /**
     * Answers if this is a backupserver instance
     * <p>
     * TODO specify beheviour of a backup server ...?
     * 
     * @return true if this is a backup server else false
     */
    public boolean isBackupServer() {
        return ConfigurationEntry.BACKUP_SERVER.getValueBoolean(this)
            .booleanValue();
    }

    /**
     * Answers if this controller has restricted connection to the network
     * 
     * @return true if no incomming connections, else false.
     */
    public boolean hasLimitedConnectivity() {
        if (getConnectionListener() == null) {
            return true;
        }
        boolean limitedConnectivity = !getConnectionListener()
            .hasIncomingConnections();
        synchronized (additionalConnectionListeners) {
            for (Iterator it = additionalConnectionListeners.iterator(); it
                .hasNext();)
            {
                ConnectionListener aListener = (ConnectionListener) it.next();
                if (aListener.hasIncomingConnections()) {
                    limitedConnectivity = false;
                }
            }
        }
        return limitedConnectivity;
    }

    /**
     * Closes the currently connecting outgoing socket
     */
    public void closeCurrentConnectionTry() {
        // shut down current connection try
        if (currentConnectingSocket != null) {
            try {
                currentConnectingSocket.close();
            } catch (IOException e) {
                log().verbose(e);
            }
        }
    }

    /**
     * Shutsdown controller and exits to system with the given status
     * 
     * @param status
     */
    public void exit(int status) {
        if ("true".equalsIgnoreCase(System.getProperty("powerfolder.test"))) {
            System.err
                .println("Running in JUnit testmode, no system.exit() called");
            return;
        }
        if (status == 0) { // only on normal shutdown
            if (isShutDownAllowed()) {
                shutdown();
                System.exit(status);
            } else {
                log().warn("not allow shutdown");
            }
        } else {
            shutdown();
            System.exit(status);
        }
    }

    /**
     * Shuts down the controller and requests and moves into restart requested
     * state
     */
    public void shutdownAndRequestRestart() {
        shuttingDown = true;
        restartRequested = true;
        shutdown();
    }

    /**
     * Answers if the controller was shut down, with the request to restart
     */
    public boolean isRestartRequested() {
        return restartRequested;
    }

    /** do we allow a normal shutdown as this time? */
    private boolean isShutDownAllowed() {
        return folderRepository.isShutdownAllowed();
    }

    /**
     * Shuts down all activities of this controller
     */
    public synchronized void shutdown() {
        shuttingDown = true;
        log().info("Shutting down...");
        if (started) {
            // Save config need a started in that method so do that first
            saveConfig();
        }

        // stop
        boolean wasStarted = started;
        started = false;
        startTime = null;

        if (timer != null) {
            log().debug("Cancel global timer");
            timer.cancel();
        }

        // shut down current connection try
        closeCurrentConnectionTry();

        if (isUIOpen()) {
            log().debug("Shutting down UI");
            uiController.shutdown();
        }

        if (rconManager != null) {
            log().debug("Shutting down RConManager");
            rconManager.shutdown();
        }

        log().debug("Shutting down connection listener(s)");
        if (connectionListener != null) {
            connectionListener.shutdown();
        }
        for (Iterator it = additionalConnectionListeners.iterator(); it
            .hasNext();)
        {
            ConnectionListener addListener = (ConnectionListener) it.next();
            addListener.shutdown();
        }
        additionalConnectionListeners.clear();
        if (broadcastManager != null) {
            log().debug("Shutting down broadcast manager");
            broadcastManager.shutdown();
        }

        if (transferManager != null) {
            log().debug("Shutting down transfer manager");
            transferManager.shutdown();
        }

        // shut down folder repository
        if (folderRepository != null) {
            log().debug("Shutting down folder repository");
            folderRepository.shutdown();
        }

        if (nodeManager != null) {
            log().debug("Shutting down node manager");
            nodeManager.shutdown();
        }

        if (pluginManager != null) {
            log().debug("Shutting down plugin manager");
            pluginManager.shutdown();
        }

        if (wasStarted) {
            System.out.println("------------ PowerFolder "
                + Controller.PROGRAM_VERSION
                + " Controller Shutdown ------------");
        }

        // remove current config
        config = null;
        shuttingDown = false;
        log().info("Shutting down done");
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
            log().error(e);
        } catch (IOException e) {
            log().error(e);
        }
    }

    /**
     * Answers the current config name loaded <configname>.properties
     * 
     * @return The name of the current config
     */
    public String getConfigName() {
        if (configFile == null) {
            return null;
        }

        String configName = configFile;
        int dot = configName.indexOf('.');
        if (dot > 0) {
            configName = configName.substring(0, dot);
        }
        return configName;
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

    /**
     * Returns local preferences, Preferences are stored till the next start. On
     * windows they are stored in the registry.
     * 
     * @return The preferences
     */
    public Preferences getPreferences() {
        boolean isDefaultConfig = DEFAULT_CONFIG_FILE
            .startsWith(getConfigName());
        if (isDefaultConfig) {
            // To keep compatible with previous versions
            return Preferences.userNodeForPackage(PowerFolder.class);
        }
        return Preferences.userNodeForPackage(PowerFolder.class).node(
            getConfigName());
    }

    /**
     * Answers the own identity, of course with no connection
     * 
     * @return a referens to the member object representing myself.
     */
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
        getNodeManager().broadcastMessage(new SettingsChange(getMySelf()));
        if (isUIOpen()) {
            // Update title
            getUIController().getMainFrame().updateTitle();
        }
    }

    /**
     * Retruns the internal powerfolder recycle bin
     * 
     * @return the RecycleBin
     */
    public RecycleBin getRecycleBin() {
        return recycleBin;
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
     * Connects to a remote peer, with ip and port
     * 
     * @param address
     * @throws ConnectionException
     *             if connection failed
     */
    public void connect(InetSocketAddress address) throws ConnectionException {
        try {
            if (!isStarted()) {
                log()
                    .info(
                        "NOT Connecting to " + address
                            + ". Controller not started");
                return;
            }

            if (address.getPort() <= 0) {
                // connect to defaul port
                log().warn(
                    "Unable to connect, port illegal " + address.getPort());
            }
            log().info("Connecting to " + address + "...");

            currentConnectingSocket = new Socket();
            String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS
                .getValue(getController());
            if (!StringUtils.isEmpty(cfgBind)) {
                currentConnectingSocket.bind(new InetSocketAddress(cfgBind, 0));
            }
            currentConnectingSocket.connect(address,
                Constants.SOCKET_CONNECT_TIMEOUT);
            NetworkUtil.setupSocket(currentConnectingSocket);

            // Accept new node
            getNodeManager().acceptNodeAsynchron(currentConnectingSocket);
        } catch (IOException e) {
            throw new ConnectionException(Translation.getTranslation(
                "dialog.unable_to_connect_to_address", address), e);
        }
    }

    /**
     * Connect to a remote node Interprets a string as connection string Format
     * is expeced as ' <connect host>' or ' <connect host>: <port>'
     * 
     * @param connectStr
     * @throws ConnectionException
     */
    public void connect(final String connectStr) throws ConnectionException {
        connect(parseConnectionString(connectStr));
    }

    /**
     * Interprets a string as connection string and returns the address. null is
     * returns if parse failed. Format is expeced as ' <connect ip>' or '
     * <connect ip>: <port>'
     * 
     * @param connectStr
     *            The connectStr to parse
     * @return a InetSocketAddress created based on the connecStr
     */
    private InetSocketAddress parseConnectionString(String connectStr) {
        if (connectStr == null) {
            return null;
        }
        String ip = connectStr.trim();
        int remotePort = ConnectionListener.DEFAULT_PORT;

        // format <ip/dns> or <ip/dns>:<port> expected
        // e.g. localhost:544
        int dotdot = connectStr.indexOf(':');
        if (dotdot >= 0 && dotdot < connectStr.length()) {
            ip = connectStr.substring(0, dotdot);
            try {
                remotePort = Integer.parseInt(connectStr.substring(dotdot + 1,
                    connectStr.length()));
            } catch (NumberFormatException e) {
                log().warn(
                    "Illegal port in " + connectStr + ", triing default port");
            }
        }

        // try to connect
        InetSocketAddress connectAddress = new InetSocketAddress(ip, remotePort);
        return connectAddress;
    }

    /**
     * Answers if controller is started in console mode
     * 
     * @return true if in console mode
     */
    public boolean isConsoleMode() {
        if (commandLine != null) {
            return commandLine.hasOption('s');
        }
        if (config != null) {
            return (config.getProperty("disableui") != null && config
                .getProperty("disableui").equalsIgnoreCase("true"));
        }
        return false;
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
                log().verbose(e);
                break;
            }
        }
    }

    /**
     * Opens the listener on local port. The first listener is set to
     * "connectionListener". All others are added the the list of
     * additionalConnectionListeners.
     * 
     * @return if succeced
     */
    private boolean openListener(int port) {
        log().debug("Opening incoming connection listener on port " + port);
        try {
            ConnectionListener newListener = new ConnectionListener(this, port);
            if (connectionListener == null) {
                // its our primary listener
                connectionListener = newListener;
            } else {
                additionalConnectionListeners.add(newListener);
            }
            return true;
        } catch (ConnectionException e) {
            log().error(e);
            return false;
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
     * Answers the connection listener
     * 
     * @return the connection listener
     */
    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    /**
     * Answers if this controller is runing in verbose mode. Set verbose=true on
     * config file to enable this, this gives acces to all kinds of debugging
     * stuff.
     * 
     * @return true if we are in verbose mode
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Answers if this controller is started in testers mode. Start powerfolder
     * with opetion -t to enable this.
     * 
     * @return true if we are in tester mode
     */
    public boolean isTester() {
        return commandLine != null && commandLine.hasOption('t');
    }

    /**
     * Returns the buildtime of this jar
     * 
     * @return the Date the PowerFolder.jar was build.
     */
    public Date getBuildTime() {
        File jar = new File("PowerFolder.jar");
        if (jar.exists()) {
            return new Date(jar.lastModified());
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
    private void setLoadingCompletion(int percentage) {
        if (uiController != null) {
            uiController.setLoadingCompletion(percentage);
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
    private File getConfigLocationBase() {
        // First check if we have a config file in local path
        File aConfigFile = new File(getConfigName() + ".config");

        // Load configuration in misc file if config file if in
        if (OSUtil.isWebStart() || !aConfigFile.exists()) {
            log().warn(
                "Config location base: "
                    + getMiscFilesLocation().getAbsolutePath());
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
    public static File getMiscFilesLocation() {
        File base = new File(System.getProperty("user.home") + "/.PowerFolder");
        if (!base.exists()) {
            base.mkdirs();
            if (OSUtil.isWindowsSystem()) {
                // Hide on windows
                FileUtils.makeHiddenOnWindows(base);
            }
        }
        return base;
    }

    /**
     * Answers the path, where to load/store temp files created by PowerFolder.
     * 
     * @return the file base, a directory
     */
    public static File getTempFilesLocation() {
        File base = new File(System.getProperty("java.io.tmpdir"));
        if (base == null || !base.exists()) {
            System.err.println("temp dir does not exsits");
        }
        return base;
    }

    /**
     * Called if controller has detected a already running instance
     */
    private void alreadyRunning() {
        Component parent = null;
        if (isUIOpen()) {
            parent = uiController.getMainFrame().getUIComponent();
        }
        if (isUIEnabled()) {
            Object[] options = new Object[]{Translation
                .getTranslation("dialog.alreadyrunning.exitbutton")};
            JOptionPane.showOptionDialog(parent, Translation
                .getTranslation("dialog.alreadyrunning.warning"), Translation
                .getTranslation("dialog.alreadyrunning.title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,
                options, options[0]);
        } else {
            log().error("PowerFolder already running");
        }
        exit(1);
    }

    /**
     * Answers the waittime for threads time differst a bit to avoid
     * concurrencies
     * 
     * @return The time to wait
     */
    public long getWaitTime() {
        return WAIT_TIME;
    }

}