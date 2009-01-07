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
package de.dal33t.powerfolder;

import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.wizard.ChooseDiskLocationPanel;
import de.dal33t.powerfolder.ui.wizard.FolderSetupPanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.WizardContextAttributes;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;

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

    private static final Logger log = Logger.getLogger(RemoteCommandManager.class.getName());

    // The default port to listen for remote commands
    private static final int DEFAULT_REMOTECOMMAND_PORT = 1338;
    // The default prefix for all rcon commands
    private static final String REMOTECOMMAND_PREFIX = "PowerFolder_RCON_COMMAND";
    // The default encoding
    private static final String ENCODING = "UTF8";
    // The prefix for pf links
    private static final String POWERFOLDER_LINK_PREFIX = "powerfolder://";

    // All possible commands
    public static final String QUIT = "QUIT";
    public static final String OPEN = "OPEN;";
    public static final String MAKEFOLDER = "MAKEFOLDER;";

    // Private vars
    private ServerSocket serverSocket;
    private Thread myThread;

    /**
     * Initalization
     * 
     * @param controller
     */
    public RemoteCommandManager(Controller controller) {
        super(controller);
    }

    /**
     * Checks if there is a running instance of RemoteComamndManager. Determains
     * this by opening a server socket port on the DEFAULT_REMOTECOMMAND_PORT.
     * 
     * @return true if port allready taken
     */
    public static boolean hasRunningInstance() {
        ServerSocket testSocket = null;
        try {
            // Only bind to localhost
            testSocket = new ServerSocket(DEFAULT_REMOTECOMMAND_PORT, 0,
                InetAddress.getByName("127.0.0.1"));

            // Server socket can be opend, no instance of PowerFolder running
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
        try {
            log.log(Level.SEVERE, "Sending remote command '" + command + '\'') ;
            Socket socket = new Socket("127.0.0.1", 1338);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket
                .getOutputStream(), ENCODING));

            writer.println(REMOTECOMMAND_PREFIX + ';' + command);
            writer.flush();
            writer.close();
            socket.close();

            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to send remote command", e);
        }
        return false;
    }

    /**
     * Starts the remote command processor
     */
    public void start() {
        try {
            // Only bind to localhost
            serverSocket = new ServerSocket(DEFAULT_REMOTECOMMAND_PORT, 0,
                InetAddress.getByName("127.0.0.1"));

            // Start thread
            myThread = new Thread(this, "Remote command Manager");
            myThread.start();
        } catch (UnknownHostException e) {
            log.warning(
                "Unable to open remote command manager on port "
                    + DEFAULT_REMOTECOMMAND_PORT + ": " + e);
            log.log(Level.FINER, "UnknownHostException", e);
        } catch (IOException e) {
            log.warning(
                "Unable to open remote command manager on port "
                    + DEFAULT_REMOTECOMMAND_PORT + ": " + e);
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
        log.info(
            "Listening for remote commands on port "
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
                    if (line.startsWith(REMOTECOMMAND_PREFIX)) {
                        processCommand(line.substring(REMOTECOMMAND_PREFIX
                            .length() + 1));
                    }
                }
                socket.close();
            } catch (IOException e) {
                log.warning("Problems parsing remote command from " + socket);
            }
        }
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
        log.fine("Received remote command: '" + command + '\'');
        if (QUIT.equalsIgnoreCase(command)) {
            getController().exit(0);
        } else if (command.startsWith(OPEN)) {
            // Open files
            String fileStr = command.substring(OPEN.length());

            // Open all files in remote command
            StringTokenizer nizer = new StringTokenizer(fileStr, ";");
            while (nizer.hasMoreTokens()) {
                String token = nizer.nextToken();
                if (token.toLowerCase().startsWith(POWERFOLDER_LINK_PREFIX)) {
                    // We got a link
                    openLink(token);
                } else {
                    // Must be a file
                    File file = new File(token);
                    openFile(file);
                }

            }
        } else if (command.startsWith(MAKEFOLDER)) {
            String folders = command.substring(MAKEFOLDER.length());
            if (getController().isUIOpen()) {
                // Popup application
                getController().getUIController().getMainFrame()
                    .getUIComponent().setVisible(true);
                getController().getUIController().getMainFrame()
                    .getUIComponent().setExtendedState(Frame.NORMAL);
            }
            String nick = getController().getMySelf().getNick();
            for (String s : folders.split(";")) {
                File file = new File(s);
                String lastPart = file.getName();

                // Folder name = nick + '-' + last part of folder path.
                makeFolder(nick + '-' + lastPart, s);
            }
        } else {
            log.warning("Remote command not recognizable '" + command + '\'');
        }
    }

    /**
     * Opens a powerfolder link and executes it
     * 
     * @param link
     */
    private void openLink(String link) {
        String plainLink = link.substring(POWERFOLDER_LINK_PREFIX.length());
        log.warning("Got plain link: " + plainLink);

        // Chop off ending /
        if (plainLink.endsWith("/")) {
            plainLink = plainLink.substring(1, plainLink.length() - 1);
        }

        try {
            // Parse link
            StringTokenizer nizer = new StringTokenizer(plainLink, "|");
            // Get type
            String type = nizer.nextToken();

            if ("file".equalsIgnoreCase(type)) {
                // Decode the url form
                String name = Util.decodeFromURL(nizer.nextToken());
                // SECRET /PUBLIC (depcrecated)
                nizer.nextToken();
                String id = Util.decodeFromURL(nizer.nextToken());

                FolderInfo folder = new FolderInfo(name, id);

                String filename = Util.decodeFromURL(nizer.nextToken());
                FileInfo fInfo = new FileInfo(folder, filename);

                // FIXME: Show warning/join panel if not on folder

                // Enqueue for download
                getController().getTransferManager().downloadNewestVersion(
                    fInfo);
            }
        } catch (NoSuchElementException e) {
            log.severe("Illegal link '" + link + '\'');
        }
    }

    /**
     * Opens a file and processes its content
     * 
     * @param file
     */
    private void openFile(File file) {
        if (!file.exists()) {
            log.warning("File not found " + file.getAbsolutePath());
            return;
        }

        if (file.getName().endsWith(".invitation")) {
            // Load invitation file
            Invitation invitation = InvitationUtil.load(file);
            if (invitation != null) {
                getController().invitationReceived(invitation, true);
            }
        } else if (file.getName().endsWith(".nodes")) {
            // Load nodes file
            MemberInfo[] nodes = loadNodesFile(file);
            // Enqueue new nodes
            if (nodes != null) {
                getController().getNodeManager().queueNewNodes(nodes);
            }
        }
    }

    /**
     * "Converts" the given folder to a PowerFolder.
     * 
     * @param folder
     *            the location of the folder
     */
    private void makeFolder(String name, String folder) {
        if (getController().isUIEnabled()) {
            FolderSetupPanel setupPanel = new FolderSetupPanel(getController());
            ChooseDiskLocationPanel panel = new ChooseDiskLocationPanel(
                getController(), folder, setupPanel);
            PFWizard wizard = new PFWizard(getController());
            wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                Icons.FILE_SHARING_PICTO);
            wizard.getWizardContext().setAttribute(WizardContextAttributes
                    .INITIAL_FOLDER_NAME, name);
            wizard.open(panel);
        } else {
            log.warning(
                    "Remote creation of folders in non-gui mode is not supported yet.");
        }
    }

    /**
     * Tries to load a list of nodes from a nodes file. Returns null if wasn't
     * able to read the file
     * 
     * @param file
     *            The file to load from
     * @return array of MemberInfo, null if failed
     */
    private MemberInfo[] loadNodesFile(File file) {
        try {
            InputStream fIn = new BufferedInputStream(new FileInputStream(file));
            ObjectInputStream oIn = new ObjectInputStream(fIn);
            // Load nodes
            List nodes = (List) oIn.readObject();

            log.warning("Loaded " + nodes.size() + " nodes");
            MemberInfo[] nodesArrary = new MemberInfo[nodes.size()];
            nodes.toArray(nodesArrary);

            return nodesArrary;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to load nodes from file '" + file + "'.", e);
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, "Illegal format of nodes file '" + file + "'.", e);
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Illegal format of nodes file '" + file + "'.", e);
        }

        return null;
    }
}
