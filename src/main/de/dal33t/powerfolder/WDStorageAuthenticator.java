package de.dal33t.powerfolder;


import com.sun.java.browser.dom.DOMUnsupportedException;
import de.dal33t.powerfolder.clientserver.SecurityService;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.java.browser.dom.DOMService.getService;

/**
 * PFS-2871: Client authentication for WD NAS storage.
 */

public class WDStorageAuthenticator extends PFComponent implements Runnable {

    private ServerSocket serverSocket;
    private Thread myThread;

    private static final Logger log = Logger
            .getLogger(WDStorageAuthenticator.class.getName());

    public WDStorageAuthenticator(Controller controller) {
        super(controller);
    }

    public void start() {
        Integer port = ConfigurationEntry.WD_STORAGE_WEB_PORT
                .getValueInt(getController());
        try {
            // Only bind to localhost
            serverSocket = new ServerSocket(port, 0,
                    InetAddress.getByName("10.0.4.122"));

            // Start thread
            myThread = new Thread(this, "WD Storage Authenticator");
            myThread.start();
        } catch (UnknownHostException e) {
            log.warning("Unable to open WD Storage Authenticator on port " + port
                    + ": " + e);
            log.log(Level.FINER, "UnknownHostException", e);
        } catch (IOException e) {
            log.warning("Unable to open WD Storage Authenticator on port " + port
                    + ": " + e);
            log.log(Level.FINER, "IOException", e);
        }
    }

    public void run() {
        log.info("Listening for authentication requests on port "
                + serverSocket.getLocalPort());
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                log.log(Level.FINER, "Socket closed, stopping", e);
                break;
            }

            log.finer("Authentication request from " + socket);
            try {
                String address = socket.getInetAddress().getHostAddress();
                if (address.equals("10.0.4.122")) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    String line = reader.readLine();
                    if (line == null) {
                        logFine("Did not receive valid authentication request");
                    } else if (line.startsWith("GET")) {
                        if (line.contains("wdToken")) {
                            authenticateUser(line);
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                logWarning("Problems parsing authentication request from " + socket + ". " + e);
                logFiner(e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        logWarning("Unable to close socket " + socket
                                + ". " + e);
                    }
                }
            }
        }
    }

    private void authenticateUser(String line) {

        String tokenSecret = line.substring(line.indexOf("wdToken=") + 8, line.lastIndexOf(" "));
        getController().getOSClient().login(tokenSecret);

    }

    public static boolean hasRunningInstance() {
        return hasRunningInstance(Integer
                .valueOf(ConfigurationEntry.WD_STORAGE_WEB_PORT.getDefaultValue()));
    }

    public static boolean hasRunningInstance(int port) {
        ServerSocket testSocket = null;
        try {
            // Only bind to localhost
            testSocket = new ServerSocket(port, 30,
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


}