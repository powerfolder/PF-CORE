package de.dal33t.powerfolder;


import de.dal33t.powerfolder.ui.model.ApplicationModel;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PFS-2871: Client authentication with web requests.
 * @author <a href="mailto:wiegmann@powerfolder.com>Jan Wiegmann</a>
 */

public class WebClientLogin extends PFComponent implements Runnable {

    private ServerSocket serverSocket;
    private Thread myThread;

    private static final Logger log = Logger
            .getLogger(WebClientLogin.class.getName());

    public WebClientLogin(Controller controller) {
        super(controller);
    }

    public void start() {
        Integer port = ConfigurationEntry.WEB_CLIENT_PORT
                .getValueInt(getController());
        try {
            // Only bind to localhost
            serverSocket = new ServerSocket(port);

            // Start thread
            myThread = new Thread(this, "Web Client Login");
            myThread.start();
            logInfo("");
        } catch (UnknownHostException e) {
            log.warning("Unable to open Web Client Login on port " + port
                    + ": " + e);
            log.log(Level.FINER, "UnknownHostException", e);
        } catch (IOException e) {
            log.warning("Unable to open Web Client Login on port " + port
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
            log.info("Authentication request from " + socket);
            try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    String line = reader.readLine();
                    if (line == null) {
                        logFine("Did not receive valid authentication request");
                    } else if (line.startsWith("GET")) {
                        if (line.contains("/login")) {
                            sendAuthenticationRequest(socket.getOutputStream());
                            break;
                        } else if (line.contains(Constants.LOGIN_PARAM_OR_HEADER_TOKEN)){
                            consumeToken(line);
                            break;
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

    private void sendAuthenticationRequest(OutputStream os){

        String locationURL;
        try {
            locationURL = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            locationURL = ConfigurationEntry.HOSTNAME.getValue(getController());
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Location: ");
        stringBuilder.append(ConfigurationEntry.CONFIG_URL.getValue(getController()));
        stringBuilder.append("/login?autoLogin=1&originalURI=");
        stringBuilder.append("http://");
        stringBuilder.append(locationURL);
        stringBuilder.append(":");
        stringBuilder.append(ConfigurationEntry.WEB_CLIENT_PORT.getValue(getController()));

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true);
        pw.println("HTTP/1.1 301 Moved Permanently");
        pw.println(stringBuilder.toString());
        pw.println("Connection: close");
        pw.println("");
    }

    private void consumeToken(String line) {
        String tokenSecret = line.substring(line.indexOf(Constants.LOGIN_PARAM_OR_HEADER_TOKEN) + 8, line.lastIndexOf(" "));
        getController().getOSClient().login(tokenSecret);
    }

    public static boolean hasRunningInstance() {
        return hasRunningInstance(Integer
                .valueOf(ConfigurationEntry.WEB_CLIENT_PORT.getDefaultValue()));
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