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
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.net.SocketUtil;

/**
 * Central class which controls all actions
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.107 $
 */
public class Controller extends PFComponent {
    // program version
    public static final String PROGRAM_VERSION = "1.0.1 devel";

    // general wait time for all threads (5000 is a balanced value)
    public static final long WAIT_TIME = 5000;

    // the default config file
    private static final String DEFAULT_CONFIG_FILE = "PowerFolder.config";

    private CommandLine commandLine;
    private String configFile;
    private Properties config;
    private Date startTime;
    private boolean started;
    private boolean shuttingDown;
    private boolean restartRequested;
    private boolean verbose;
    private NodeManager nodeManager;
    private FolderRepository folderRepository;
    private ConnectionListener listener;
    private List additionalListener;
    private BroadcastMananger broadcastManager;
    private DynDnsManager dyndnsManager;
    private TransferManager transferManager;
    private RConManager rconManager;
    private ChatModel chatModel;
    private UIController uiController;

    private PluginManager pluginManager;

    private RecycleBin recycleBin;
    // the currently used socket to connect to a new member
    // used in shutdown, connection try ofter take 30s
    private Socket currentConnectingSocket;

    private java.util.Timer timer;

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
     * overwite the PFComponent.getController() that one returns null for this
     * Controller itself ;-)
     */
    public Controller getController() {
        return this;
    }

    /**
     * Creates a fresh Controller
     * 
     * @return
     */
    public static Controller createController() {
        return new Controller();
    }

    /**
     * Starts this controller loading the config from the default config file
     * 
     * @see #DEFAULT_CONFIG_FILE;
     */
    public void startDefaultConfig() {
        startConfig(DEFAULT_CONFIG_FILE);
    }

    /**
     * Starts a config with the given command line arguments
     * 
     * @param commandLineArgs
     */
    public void startConfig(CommandLine aCommandLine) {
        this.commandLine = aCommandLine;
        String configName = aCommandLine.getOptionValue("c");
        startConfig(configName);
    }

    /**
     * Starts controller with a special config file
     * 
     * @param filename
     */
    public void startConfig(String filename) {
        if (isStarted()) {
            throw new IllegalStateException(
                "Configuration already started, shutdown controller first");
        }

        additionalListener = Collections.synchronizedList(new ArrayList());
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

        timer = new java.util.Timer("Controller schedule timer", true);

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
        rconManager = new RConManager(this);
        rconManager.start();

        setLoadingCompletion(75);

        // Start all configured listener if not in silent mode
        startConfiguredListener();
        setLoadingCompletion(80);

        // open broadcast listener
        openBroadcastManager();

        // create the chatModel
        chatModel = new ChatModel(this);
        setLoadingCompletion(85);
        // Controller now started
        started = true;
        startTime = new Date();

        log().info("Controller started");

        // dyndns updater
        boolean onStartUpdate = Util.getBooleanProperty(getConfig(),
            "onStartUpdate", false);
        if (onStartUpdate) {
            getDynDnsManager().onStartUpdate();
        }

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

            if (Util.isWebStart()) {
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
        timer.schedule(task, period, period);
    }

    /** use to schedule a task */
    public void schedule(TimerTask task, long delay) {
        timer.schedule(task, delay);
    }

    private void openBroadcastManager() {
        if (!Boolean.valueOf(config.getProperty("disablebroadcasts"))
            .booleanValue())
        {
            try {
                broadcastManager = new BroadcastMananger(this);
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

    private boolean initializeListenerOnLocalPort() {
        String ports = config.getProperty("port");
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
                    if (listenerOpened && listener != null) {
                        // set reconnect on first successfull listener
                        nodeManager.getMySelf().getInfo().setConnectAddress(
                            listener.getLocalAddress());
                    }
                    if (!listenerOpened) {
                        // Abort if listner cannot be bound
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
        return true;
    }

    /**
     * Starts all configures connection listener
     */
    private void startConfiguredListener() {
        // Start the connection listener
        if (listener != null) {
            try {
                listener.start();
            } catch (ConnectionException e) {
                log().error("Problems starting listener " + listener, e);
            }
            for (Iterator it = additionalListener.iterator(); it.hasNext();) {
                try {
                    ConnectionListener addListener = (ConnectionListener) it
                        .next();
                    addListener.start();
                } catch (ConnectionException e) {
                    log().error("Problems starting listener " + listener, e);
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
        try {
            // Store config in misc base
            File file = new File(getConfigLocationBase(), getConfigName()
                + ".config");
            fOut = new BufferedOutputStream(new FileOutputStream(file));
            getConfig().store(fOut,
                "PowerFolder config file (v" + PROGRAM_VERSION + ")");
            fOut.close();
        } catch (IOException e) {
            log().error("Unable to save config", e);
        }
    }

    /**
     * Answers if controller is started (by config)
     * 
     * @return
     */
    public boolean isStarted() {
        return started;
    }

    /** true is shutdown still in progress */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Answers the uptime in milliseconds. Or -1 if not started yet
     * 
     * @return
     */
    public long getUptime() {
        if (startTime == null) {
            return -1;
        }
        return System.currentTimeMillis() - startTime.getTime();
    }

    /**
     * Answers if the controller is running is silentmode
     * 
     * @return
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
        firePropertyChange("silentMode", oldValue, isSilentMode());
    }

    /**
     * Answers if node is running in public networking mode
     * 
     * @return
     */
    public boolean isPublicNetworking() {
        // Default = private networking
        boolean publicNetworking = Util.getBooleanProperty(getConfig(),
            "publicnetworking", false);
        return publicNetworking;
    }

    /**
     * Sets networking mode
     * 
     * @param pubNet
     */
    public void setPublicNetworking(boolean pubNet) {
        boolean oldValue = isPublicNetworking();
        getConfig().put("publicnetworking", pubNet + "");

        if (oldValue != isPublicNetworking()) {
            saveConfig();
        }

        if (!isPublicNetworking()) {
            // Disco some body
            getNodeManager().disconnectUninterestingNodes();
        }

        firePropertyChange("publicNetworking", Boolean.valueOf(oldValue),
            Boolean.valueOf(isPublicNetworking()));
    }

    /**
     * Answers if this is a backupserver instance
     * 
     * @return
     */
    public boolean isBackupServer() {
        boolean backupServer = Util.getBooleanProperty(getConfig(),
            "backupserver", false);
        return backupServer;
    }

    /**
     * Answers if this controller has restricted connection to the network
     * 
     * @return
     */
    public boolean hasLimitedConnectivity() {
        boolean limitedConnectivity = !getConnectionListener()
            .hasIncomingConnections();
        synchronized (additionalListener) {
            for (Iterator it = additionalListener.iterator(); it.hasNext();) {
                ConnectionListener listener = (ConnectionListener) it.next();
                if (listener.hasIncomingConnections()) {
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
        if (listener != null) {
            listener.shutdown();
        }
        for (Iterator it = additionalListener.iterator(); it.hasNext();) {
            ConnectionListener addListener = (ConnectionListener) it.next();
            addListener.shutdown();
        }
        additionalListener.clear();
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
     * @return
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
     * @return
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
     * Returns the config, read from configfile before
     * 
     * @return
     */
    public Properties getConfig() {
        return config;
    }

    /**
     * Returns the command line of the start
     * 
     * @return
     */
    public CommandLine getCommandLine() {
        return commandLine;
    }

    /**
     * Returns local preferences, Preferences are stored till the next start
     * 
     * @return
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
     * @return
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
        getConfig().setProperty("nick", getMySelf().getNick());
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
     * @return
     */
    public DynDnsManager getDynDnsManager() {
        return dyndnsManager;
    }

    /**
     * Return the model holding all chat data
     * 
     * @return The ChatModel
     */
    public ChatModel getChatModel() {
        return chatModel;
    }

    /**
     * Returns the broadcast manager
     * 
     * @return
     */
    public BroadcastMananger getBroadcastManager() {
        return broadcastManager;
    }

    /**
     * @return
     */
    public NodeManager getNodeManager() {
        return nodeManager;
    }

    /**
     * Returns the folder repository
     * 
     * @return
     */
    public FolderRepository getFolderRepository() {
        return folderRepository;
    }

    /**
     * Returns the transfer manager of the controller
     * 
     * @return
     */
    public TransferManager getTransferManager() {
        return transferManager;
    }

    /**
     * Connects to a remote peer, with ip and port
     * 
     * @param address
     * @return
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
            String cfgBind = getConfig().getProperty("net.bindaddress");
            if (!StringUtils.isEmpty(cfgBind)) {
                currentConnectingSocket.bind(new InetSocketAddress(cfgBind, 0));
            }
            currentConnectingSocket.connect(address,
                Constants.SOCKET_CONNECT_TIMEOUT);
            SocketUtil.setupSocket(currentConnectingSocket);

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
     * @return
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
     * @return
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
     * Answers if the ui is enabled
     * 
     * @return
     */
    public boolean isUIEnabled() {
        return !isConsoleMode();
    }

    /**
     * Answers if we have the ui open
     * 
     * @return
     */
    public boolean isUIOpen() {
        return uiController != null && uiController.isStarted();
    }

    /**
     * Exposing UIController
     * 
     * @return
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
     * Opens the listener on local port
     * 
     * @return if succeced
     */
    private boolean openListener(int port) {
        log().debug("Opening incoming connection listener on port " + port);
        try {
            ConnectionListener newListener = new ConnectionListener(this, port);
            if (listener == null) {
                // its our primary listener
                listener = newListener;
            } else {
                additionalListener.add(newListener);
            }
            return true;
        } catch (ConnectionException e) {
            log().error(e);
            return false;
        }
    }

    /**
     * @return
     */
    public boolean hasListener() {
        return listener != null;
    }

    /**
     * Answers the connection listener
     * 
     * @return
     */
    public ConnectionListener getConnectionListener() {
        return listener;
    }

    /**
     * Answers if this controller is runing in verbose mode
     * 
     * @return
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Answers if this controller is started in testers mode
     * 
     * @return
     */
    public boolean isTester() {
        return commandLine != null && commandLine.hasOption('t');
    }

    public boolean isLanOnly() {
        String isLanOnlyStr= getController().getConfig().getProperty("lanOnly");
        if (isLanOnlyStr == null) {
            return false;
        }
        return ("true").equals(isLanOnlyStr);
    }
    
    /**
     * Returns the buildtime of this jar
     * 
     * @return
     */
    public Date getBuildTime() {
        File jar = new File("PowerFolder.jar");
        if (jar.exists()) {
            return new Date(jar.lastModified());
        }
        return null;
    }

    /**
     * Sets the loading completion of this controller
     * 
     * @param percentage
     */
    private void setLoadingCompletion(int percentage) {
        if (uiController != null) {
            uiController.setLoadingCompletion(percentage);
        }
    }

    /**
     * Answers if minimized start is wanted
     * 
     * @return
     */
    public boolean isStartMinimized() {
        return commandLine != null && commandLine.hasOption('m');
    }

    /**
     * The base directory where to store/load config files. or null if on
     * working path
     * 
     * @return
     */
    public File getConfigLocationBase() {
        // First check if we have a config file in local path
        File configFile = new File(getConfigName() + ".config");

        // Load configuration in misc file if config file if in
        if (Util.isWebStart() || !configFile.exists()) {
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
            if (Util.isWindowsSystem()) {
                // Hide on windows
                Util.makeHiddenOnWindows(base);
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
     * @return
     */
    public long getWaitTime() {
        return WAIT_TIME + (int) (Math.random() * 500);
    }
}