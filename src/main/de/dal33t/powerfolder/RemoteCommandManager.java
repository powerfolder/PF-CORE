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
 * $Id: RemoteCommandManager.java 21035 2013-03-13 14:14:58Z sprajc $
 */
package de.dal33t.powerfolder;

import java.awt.Frame;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jwf.WizardPanel;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import de.dal33t.powerfolder.task.CreateFolderOnServerTask;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.wizard.ChooseDiskLocationPanel;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TextPanelPanel;
import de.dal33t.powerfolder.ui.wizard.WizardContextAttributes;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Waiter;

/**
 * The remote command processor is responsible for binding on a socket and
 * receive and process any remote control commands. e.g. Load invitation file,
 * process powerfolder links or exit powerfolder.
 * <p>
 * Supported links:
 * <p>
 * <code>
 * Folder links:
 * PowerFolder://|folder|<foldername>|<P or S>|<folderid>|<size>|<numFiles>
 * <P or S> P = public, S = secret
 * PowerFolder://|folder|test|S|[test-AAgwZXFLgigj222]|99900000|1000
 * 
 * File links:
 * PowerFolder://|file|<foldername>|<P or S>|<folderid>|<fullpath_filename>
 * <P or S> P = public, S = secret
 * PowerFolder://|folder|test|S|[test-AAgwZXFLgigj222]|/test/New_text_docuement.txt
 * </code>
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class RemoteCommandManager extends PFComponent implements Runnable {

    // Parameters according to
    // http://www.powerfolder.com/wiki/Script_configuration
    private static final String FOLDER_SCRIPT_CONFIG_ID = "id";
    private static final String FOLDER_SCRIPT_CONFIG_NAME = "name";
    private static final String FOLDER_SCRIPT_CONFIG_DIR = "dir";
    private static final String FOLDER_SCRIPT_CONFIG_BACKUP_BY_SERVER = "backup_by_server";
    private static final String FOLDER_SCRIPT_CONFIG_SYNC_PROFILE = "syncprofile";
    private static final String FOLDER_SCRIPT_CONFIG_SILENT = "silent";
    private static final String FOLDER_SCRIPT_CONFIG_DL_SCRIPT = "dlscript";

    private static final Logger log = Logger
        .getLogger(RemoteCommandManager.class.getName());

    // The default prefix for all rcon commands
    private static final String REMOTECOMMAND_PREFIX = "PowerFolder_RCON_COMMAND";
    // The default encoding
    private static final String ENCODING = "UTF8";

    // All possible commands
    public static final String QUIT = "QUIT";
    public static final String OPEN = "OPEN;";
    public static final String SHOW_UI = "SHOWUI;";
    public static final String MAKEFOLDER = "MAKEFOLDER;";
    public static final String REMOVEFOLDER = "REMOVEFOLDER;";
    public static final String COPYLINK = "COPYLINK;";

    // Private vars
    private ServerSocket serverSocket;
    private Thread myThread;

    /**
     * Initialization
     * 
     * @param controller
     */
    public RemoteCommandManager(Controller controller) {
        super(controller);
    }

    /**
     * Checks if there is a running instance of RemoteCommandManager. Determines
     * this by opening a server socket port on the default remote command port.
     * 
     * @return true if port already taken
     */
    public static boolean hasRunningInstance() {
        return hasRunningInstance(Integer
            .valueOf(ConfigurationEntry.NET_RCON_PORT.getDefaultValue()));
    }

    /**
     * Checks if there is a running instance of RemoteComamndManager. Determines
     * this by opening a server socket port.
     * 
     * @param port
     *            the port to check
     * @return true if port already taken
     */
    public static boolean hasRunningInstance(int port) {
        ServerSocket testSocket = null;
        try {
            // Only bind to localhost
            testSocket = new ServerSocket(port, 0,
                InetAddress.getByName("127.0.0.1"));

            // Server socket can be opened, no instance of PowerFolder running
            log.fine("No running instance found");
            return false;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        } finally {
            if (testSocket != null) {
                try {
                    testSocket.close();
                } catch (IOException e) {
                    log.log(Level.SEVERE,
                        "Unable to close already running test socket. "
                            + testSocket, e);
                }
            }
        }
        log.warning("Running instance found");
        return true;
    }

    /**
     * Sends a remote command to a running instance of PowerFolder
     * 
     * @param command
     *            the command
     * @return true if succeeded, otherwise false
     */
    public static boolean sendCommand(String command) {
        return sendCommand(
            Integer.valueOf(ConfigurationEntry.NET_RCON_PORT.getDefaultValue()),
            command);
    }

    /**
     * Sends a remote command to a running instance of PowerFolder
     * 
     * @param port
     *            the port to send this to.
     * @param command
     *            the command
     * @return true if succeeded, otherwise false
     */
    public static boolean sendCommand(int port, String command) {
        try {
            log.log(Level.INFO, "Sending remote command '" + command + '\'');
            Socket socket = new Socket("127.0.0.1", port);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), ENCODING));

            writer.println(REMOTECOMMAND_PREFIX + ';' + command);
            writer.flush();
            writer.close();
            socket.close();

            return true;
        } catch (ConnectException e) {
            log.log(Level.WARNING,
                "Unable to connect to running instance to send remote command: "
                    + e.getMessage());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to send remote command", e);
        }
        return false;
    }

    /**
     * Starts the remote command processor
     */
    public void start() {
        Integer port = ConfigurationEntry.NET_RCON_PORT
            .getValueInt(getController());
        try {
            // Only bind to localhost
            serverSocket = new ServerSocket(port, 0,
                InetAddress.getByName("127.0.0.1"));

            // Start thread
            myThread = new Thread(this, "Remote command Manager");
            myThread.start();
        } catch (UnknownHostException e) {
            log.warning("Unable to open remote command manager on port " + port
                + ": " + e);
            log.log(Level.FINER, "UnknownHostException", e);
        } catch (IOException e) {
            log.warning("Unable to open remote command manager on port " + port
                + ": " + e);
            log.log(Level.FINER, "IOException", e);
        }
    }

    /**
     * Shuts down the rcon manager
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.log(Level.FINER, "Unable to close rcon socket", e);
            }
        }
    }

    public void run() {
        log.fine("Listening for remote commands on port "
            + serverSocket.getLocalPort());
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                log.log(Level.FINER, "Rcon socket closed, stopping", e);
                break;
            }

            log.finer("Remote command from " + socket);
            try {
                String address = socket.getInetAddress().getHostAddress();
                if (address.equals("localhost") || address.equals("127.0.0.1"))
                {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), ENCODING));
                    String line = reader.readLine();
                    if (line == null) {
                        logFine("Did not receive valid remote request");
                    } else if (line.startsWith(REMOTECOMMAND_PREFIX)) {
                        processCommand(line.substring(REMOTECOMMAND_PREFIX
                            .length() + 1));
                    } else if (line.startsWith("GET")
                        || line.startsWith("POST"))
                    {
                        processWebRequest(line, socket.getOutputStream());
                    } else {
                        logWarning("Unknown remote command: " + line);
                    }
                }
                // socket.close();
            } catch (Exception e) {
                logWarning("Problems parsing remote command from " + socket
                    + ". " + e);
                logFiner(e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void processWebRequest(String line, OutputStream out)
        throws IOException
    {
        // System.err.println(line);
        out = new BufferedOutputStream(out);
        Writer w = new OutputStreamWriter(out, Convert.UTF8);
        w.write("HTTP/1.1 200 OK\n");
        // w.write("Transfer-Encoding: chunked\n");

        if (line.contains("/info")) {
            w.write("Content-Type: text/javascript; charset=utf-8\n");
            w.write("\n");
            w.write("_jqjsp(\"");

            // JSON Start
            w.write("{");
            w.write("'nodeId':'"
                + getController().getMySelf().getId().replace("'", "\\'") + '\'');
            w.write(",");
            w.write("'nodeName':'"
                + getController().getMySelf().getNick().replace("'", "\\'")
                + '\'');
            w.write("}");
            // JSON End

            w.write("\");");
            w.close();
        } else if (line.contains("/open/")) {
            // TODO Error handling
            int start = line.indexOf("/open/");
            int end = line.indexOf(" HTTP");
            String addr = line.substring(start + 6, end);
            int fIdEnd = addr.indexOf('/');
            String fId64 = addr.substring(0, fIdEnd);
            String folderId = Base64.decodeString(fId64);
            Folder folder = getController().getFolderRepository().getFolder(
                folderId);
            String relativeName = addr.substring(fIdEnd + 1, addr.length());
            try {
                relativeName = URLDecoder.decode(relativeName, "UTF-8");
                relativeName = relativeName.replace("%20", " ");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding UTF-8 not found", e);
            }

            FileInfo lookupFile = FileInfoFactory.lookupInstance(
                folder.getInfo(), relativeName);
            Path file = lookupFile.getDiskFile(getController()
                .getFolderRepository());
            logInfo("Opening file: " + file);
            PathUtils.openFile(file);
        }
        w.close();
    }

    /**
     * Processes a remote command
     * 
     * @param command
     */
    private void processCommand(String command) {
        if (StringUtils.isBlank(command)) {
            log.severe("Received a empty remote command");
            return;
        }
        logFine("Processing remote command: '" + command + '\'');

        if (command.startsWith(MAKEFOLDER) || command.startsWith(COPYLINK)) {
            // Wait for hook up.
            Waiter w = new Waiter(60000);
            while (!w.isTimeout()
                && !getController().getOSClient().isLoggedIn()
                && Feature.OS_CLIENT.isEnabled())
            {
                w.waitABit();
            }
        }

        if (QUIT.equalsIgnoreCase(command)) {
            getController().exit(0);
        } else if (command.startsWith(OPEN)) {
            // Open files
            String fileStr = command.substring(OPEN.length());

            // Open all files in remote command
            StringTokenizer nizer = new StringTokenizer(fileStr, ";");
            while (nizer.hasMoreTokens()) {
                String token = nizer.nextToken();
                // Must be a file
                Path file = Paths.get(token).toAbsolutePath();
                openFile(file);
            }
        } else if (command.startsWith(SHOW_UI)) {
            // SYNC-87
            if (getController().isUIEnabled()) {
                getController().getUIController().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getController().getUIController().getMainFrame()
                            .toFront();
                    }
                });
            }
        } else if (command.startsWith(MAKEFOLDER)) {
            String folderConfig = command.substring(MAKEFOLDER.length());
            if (getController().isUIOpen()) {
                // Popup application
                getController().getUIController().getMainFrame()
                    .getUIComponent().setVisible(true);
                getController().getUIController().getMainFrame()
                    .getUIComponent().setExtendedState(Frame.NORMAL);
            }

            // Old style configuration was simply the Directory, e.g.
            // C:\The_path
            boolean oldStyle = !folderConfig.contains("dir=");
            if (oldStyle) {
                logWarning("Converted old style folder "
                    + "make command for directory " + folderConfig);
                // Convert to new style
                folderConfig = "dir=" + folderConfig;
            }
            // New style configuration
            // dir=%BASE_DIR%\IPAKI\BACKUP;name=IPAKI/BACKUP/%COMPUTERNAME%;syncprofile=true,true,true,true,5,false,12,0,m,Auto-sync;backup_by_server=true

            final String finalFolderConfig = folderConfig;
            getController().schedule(new Runnable() {
                public void run() {
                    makeFolder(finalFolderConfig);
                }
            }, 0);

        } else if (command.startsWith(REMOVEFOLDER)) {
            final String folderConfig = command
                .substring(REMOVEFOLDER.length());
            getController().schedule(new Runnable() {
                public void run() {
                    removeFolder(folderConfig);
                }
            }, 0);
        } else if (command.startsWith(COPYLINK)) {
            final String filename = command.substring(COPYLINK.length());
            getController().schedule(new Runnable() {
                public void run() {
                    copyLink(filename);
                }
            }, 0);
        } else {
            log.warning("Remote command not recognizable '" + command + '\'');
        }
    }

    protected void copyLink(String filename) {
        Path file = Paths.get(filename).toAbsolutePath();
        String absPath = file.toString();
        for (Folder folder : getController().getFolderRepository().getFolders())
        {

            if (absPath.startsWith(folder.getLocalBase().toAbsolutePath()
                .toString()))
            {
                final ServerClient client = getController().getOSClient();
                final FileInfo fInfo = FileInfoFactory.lookupInstance(folder,
                    file);

                if (client.isConnected()) {
                    getController().getIOProvider().startIO(new Runnable() {
                        public void run() {
                            // COOL MODE: Directly get file link without web
                            // browser.
                            String altURL = client.getFolderService()
                                .getFileLink(fInfo);
                            Util.setClipboardContents(altURL.replace(
                                Constants.GET_LINK_URI, Constants.DL_LINK_URI));
                        }
                    });
                }

                final String linkURL = client.getFileLinkURL(fInfo);
                try {
                    BrowserLauncher.openURL(linkURL);
                } catch (IOException e) {
                    logWarning("Unable to open in browser: " + linkURL);
                }

                return;
            }
        }
        logWarning("Unable to copy file link. File not contained in a shared folder");
        if (getController().isUIEnabled()) {
            getController().getUIController().getMainFrame().toFront();
            DialogFactory.genericDialog(getController(),
                    Translation.getTranslation("remote_command_manager.copy_link.error_title"),
                    Translation.getTranslation("remote_command_manager.copy_link.error_message", filename),
                    GenericDialogType.ERROR);
        }

    }

    /**
     * Opens a file and processes its content
     * 
     * @param file
     */
    private void openFile(Path file) {
        if (Files.notExists(file)) {
            log.warning("File not found " + file.toAbsolutePath().toString());
            return;
        }

        if (file.getFileName().toString().endsWith(".invitation")) {
            // Load invitation file
            Invitation invitation = InvitationUtil.load(file);
            if (invitation != null) {
                getController().invitationReceived(invitation);
            }
        } else if (file.getFileName().toString().endsWith(".nodes")) {
            // Load nodes file
            MemberInfo[] nodes = loadNodesFile(file);
            // Enqueue new nodes
            if (nodes != null) {
                getController().getNodeManager().queueNewNodes(nodes);
            }
        }
    }

    private void removeFolder(String folderConfig) {
        Map<String, String> config = parseFolderConfig(folderConfig);
        String id = config.get(FOLDER_SCRIPT_CONFIG_ID);
        String name = config.get(FOLDER_SCRIPT_CONFIG_NAME);
        String dirStr = config.get(FOLDER_SCRIPT_CONFIG_DIR);
        Path dir = StringUtils.isBlank(dirStr) ? null : Paths.get(dirStr);
        logFine("Remove folder command received. Config: " + folderConfig);
        if (StringUtils.isBlank(id) && StringUtils.isBlank(name) && dir == null)
        {
            logSevere("Unable to remove folder. Wrong parameters: "
                + folderConfig);
            return;
        }

        for (Folder candidate : getController().getFolderRepository()
            .getFolders())
        {
            if (StringUtils.isNotBlank(id)) {
                if (!candidate.getId().equals(id)) {
                    // ID given, but no match. Skip
                    continue;
                }
            }
            if (StringUtils.isNotBlank(name)) {
                if (!candidate.getName().equalsIgnoreCase(name)) {
                    // name given, but no match. Skip
                    continue;
                }
            }
            if (dir != null) {
                try {
                    if (!candidate.getLocalBase().equals(dir)
                        && !candidate.getLocalBase().toRealPath()
                            .equals(dir.toRealPath()))
                    {
                        // path given, but no match. Skip
                        continue;
                    }
                } catch (Exception e) {
                    logWarning("Unable to check by directory: " + candidate
                        + ". Dir: " + dir + ". " + e, e);
                }
            }

            logInfo("Removing folder: " + candidate + ". Matched by: "
                + folderConfig);
            // Ok this candidate matches! Remove it.
            getController().getFolderRepository().removeFolder(candidate, true);
        }
    }

    private void makeFolder(String folderConfig) {

        Map<String, String> config = parseFolderConfig(folderConfig);

        // Directory
        if (StringUtils.isBlank(config.get(FOLDER_SCRIPT_CONFIG_DIR))) {
            logSevere("Unable to parse make folder command. directory missing. "
                + folderConfig);
            return;
        }
        FolderRepository repository = getController().getFolderRepository();
        Path dir = Paths.get(config.get(FOLDER_SCRIPT_CONFIG_DIR));

        // Name
        String name;
        if (StringUtils.isNotBlank(config.get(FOLDER_SCRIPT_CONFIG_NAME))) {
            name = config.get(FOLDER_SCRIPT_CONFIG_NAME);
        } else {
            name = PathUtils.getSuggestedFolderName(dir);
        }

        if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
            .getValueBoolean(getController()))
        {
            if (!getController().getOSClient().getAccount()
                .hasPermission(FolderCreatePermission.INSTANCE))
            {
                if (getController().isUIEnabled()) {
                    getController().getUIController().getMainFrame().toFront();
                    DialogFactory.genericDialog(getController(),
                            Translation.getTranslation("remote_command_manager.make_folder.error_title"),
                            Translation.getTranslation("remote_command_manager.make_folder.error_message", name),
                            GenericDialogType.ERROR);
                }
                return;
            }
        }

        // Show user?
        boolean silent = "true".equalsIgnoreCase(config
            .get(FOLDER_SCRIPT_CONFIG_SILENT));

        // ID
        String id = config.get(FOLDER_SCRIPT_CONFIG_ID);
        boolean createInvitationFile = false;
        if (StringUtils.isEmpty(id)) {
            id = IdGenerator.makeFolderId();
            createInvitationFile = true;
        }

        if (ConfigurationEntry.FOLDER_CREATE_AVOID_DUPES
            .getValueBoolean(getController()))
        {
            Folder oldFolder = repository.findExistingFolder(dir);
            if (oldFolder != null) {
                oldFolder = repository.findExistingFolder(name);
            }
            if (oldFolder != null) {
                // Re-use old ID to prevent breaking existing setup.
                id = oldFolder.getId();
                logWarning("Deleting folder: " + oldFolder + " at "
                    + oldFolder.getLocalBase() + ". Replacing it new one at "
                    + dir);
                repository.removeFolder(oldFolder, true);
            }
        }

        String syncProfileFieldList = config
            .get(FOLDER_SCRIPT_CONFIG_SYNC_PROFILE);
        SyncProfile syncProfile = syncProfileFieldList != null ? SyncProfile
            .getSyncProfileByFieldList(syncProfileFieldList) : null;
        boolean backupByServer = "true".equals(config
            .get(FOLDER_SCRIPT_CONFIG_BACKUP_BY_SERVER));

        FolderInfo foInfo = new FolderInfo(name, id);

        String dlScript = config.get(FOLDER_SCRIPT_CONFIG_DL_SCRIPT);

        if (!silent && getController().isUIEnabled()) {
            PFWizard wizard = new PFWizard(getController(),
                Translation.getTranslation("wizard.pfwizard.folder_title"));
            wizard.getWizardContext().setAttribute(
                WizardContextAttributes.INITIAL_FOLDER_NAME, name);
            if (syncProfile != null) {
                wizard.getWizardContext()
                    .setAttribute(
                        WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE,
                        syncProfile);
            }
            wizard.getWizardContext().setAttribute(
                WizardContextAttributes.BACKUP_ONLINE_STOARGE,
                backupByServer
                    || StringUtils.isBlank(config
                        .get(FOLDER_SCRIPT_CONFIG_BACKUP_BY_SERVER)));
            wizard.getWizardContext().setAttribute(
                WizardContextAttributes.FOLDERINFO_ATTRIBUTE, foInfo);

            WizardPanel nextPanel = new FolderCreatePanel(getController());
            // Setup success panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setup_success"),
                Translation.getTranslation("wizard.success_join"));
            wizard.getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);
            ChooseDiskLocationPanel panel = new ChooseDiskLocationPanel(
                getController(), dir.toAbsolutePath().toString(), nextPanel);
            wizard.open(panel);
        } else {
            if (syncProfile == null) {
                syncProfile = SyncProfile.getDefault(getController());
            }
            FolderSettings settings = new FolderSettings(dir, syncProfile,
                dlScript,
                ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                    .getValueInt(getController()), true);
            repository.createFolder(foInfo, settings);
            if (backupByServer) {
                new CreateFolderOnServerTask(foInfo, null)
                    .scheduleTask(getController());
            }
        }
    }

    private Map<String, String> parseFolderConfig(String folderConfig) {
        Map<String, String> config = new HashMap<String, String>();
        StringTokenizer nizer = new StringTokenizer(folderConfig, ";");
        while (nizer.hasMoreTokens()) {
            String keyValuePair = nizer.nextToken();
            int equal = keyValuePair.indexOf('=');
            if (equal <= 0) {
                logSevere("Unable to parse make folder command: '"
                    + folderConfig + '\'');
                continue;
            }
            String key = keyValuePair.substring(0, equal);
            String value = keyValuePair.substring(equal + 1);
            config.put(key, value);
        }
        return config;
    }

    /**
     * Tries to load a list of nodes from a nodes file. Returns null if wasn't
     * able to read the file
     * 
     * @param file
     *            The file to load from
     * @return array of MemberInfo, null if failed
     */
    @SuppressWarnings({"unchecked"})
    private static MemberInfo[] loadNodesFile(Path file) {
        try (ObjectInputStream oIn = new ObjectInputStream(Files.newInputStream(file))) {
            // Load nodes
            List<MemberInfo> nodes = (List<MemberInfo>) oIn.readObject();

            log.warning("Loaded " + nodes.size() + " nodes");
            MemberInfo[] nodesArrary = new MemberInfo[nodes.size()];
            nodes.toArray(nodesArrary);

            return nodesArrary;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to load nodes from file '" + file
                + "'.", e);
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, "Illegal format of nodes file '" + file
                + "'.", e);
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Illegal format of nodes file '" + file
                + "'.", e);
        }

        return null;
    }
}
