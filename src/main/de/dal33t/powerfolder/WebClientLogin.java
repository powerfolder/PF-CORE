package de.dal33t.powerfolder;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PFS-2871: Client authentication with HTTP web token.
 *
 * @author <a href="mailto:wiegmann@powerfolder.com>Jan Wiegmann</a>
 */

public class WebClientLogin extends PFComponent {

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
            serverSocket = new ServerSocket(port);

            // Start thread
            myThread = new Thread(new Worker(), "Web Client Login");
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

    public void stop() {

        if (myThread != null)
            myThread.interrupt();

        try {
            serverSocket.close();
        } catch (IOException e) {
            logWarning("Unable to close server socket @ " + serverSocket + " " + e, e);
        }
    }

    private class Worker implements Runnable {

        public void run() {
            log.info("Listening for authentication requests on port "
                    + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket;
                BufferedReader reader = null;

                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    log.log(Level.FINER, "Socket closed, stopping", e);
                    break;
                }
                log.info("Authentication request from " + socket);

                try {
                    reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));

                    String line = reader.readLine();

                    if (line != null && line.startsWith("GET")) {
                        if (line.contains("/login")) {
                            String inetAddress = getInetAddress();
                            if (inetAddress != null) {
                                sendAuthenticationRequest(socket.getOutputStream(), inetAddress);
                            } else {
                                logWarning("Process authentication request failed. " +
                                        "Client authentication request only supported from WDNAS devices. Are you an a WDNAS device?");
                            }
                        } else if (line.contains(Constants.LOGIN_PARAM_OR_HEADER_TOKEN)) {
                            consumeToken(line);
                            sendAuthSuccessRequest(socket.getOutputStream());
                        }
                    }
                } catch (Exception e) {
                    logWarning("Problems parsing authentication request from " + socket + ". " + e, e);
                    logFiner(e);
                } finally {
                    try {
                        if (reader != null)
                            reader.close();

                        if (socket != null)
                            socket.close();

                    } catch (IOException e) {
                        logWarning("Unable to close socket " + socket
                                + ". " + e);
                    }
                }
            }
        }
    }

    private String getInetAddress() throws SocketException {

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        String inetAddress = null;

        for (NetworkInterface networkInterface : Collections.list(nets)) {
            if (networkInterface.getDisplayName().contains("bond0")) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                for (InetAddress address : Collections.list(inetAddresses)) {
                    if (address instanceof Inet4Address) {
                        inetAddress = address.toString();
                    }
                }
            }
        }
        return inetAddress != null ? inetAddress.replace("/", "") : null;
    }

    private void sendAuthenticationRequest(OutputStream os, String originalURI) {

        if (originalURI == null) {
            try {
                originalURI = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                originalURI = ConfigurationEntry.HOSTNAME.getValue(getController());
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Location: ");
        stringBuilder.append(ConfigurationEntry.CONFIG_URL.getValue(getController()));
        stringBuilder.append("/login?autoLogin=1&originalURI=");
        stringBuilder.append("http://");
        stringBuilder.append(originalURI);
        stringBuilder.append(":");
        stringBuilder.append(ConfigurationEntry.WEB_CLIENT_PORT.getValue(getController()));

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true);

        pw.println("HTTP/1.1 301 Moved Permanently");
        pw.println(stringBuilder.toString());
        pw.println("Connection: close");
        pw.println("");
        pw.close();
    }

    private void sendAuthSuccessRequest(OutputStream os) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Location: ");
        stringBuilder.append(ConfigurationEntry.CONFIG_URL.getValue(getController()));
        stringBuilder.append("/authsuccess");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true);

        pw.println("HTTP/1.1 301 Moved Permanently");
        pw.println(stringBuilder.toString());
        pw.println("Connection: close");
        pw.println("");
        pw.close();
    }

    private void consumeToken(String line) {
        String tokenSecret = line.substring(line.indexOf(Constants.LOGIN_PARAM_OR_HEADER_TOKEN) + 6, line.lastIndexOf(" "));
        getController().getOSClient().login("WDMyCloud", tokenSecret);

        ConfigurationEntry.SERVER_CONNECT_TOKEN.setValue(getController().getConfig(), tokenSecret);
        getController().saveConfig();

        log.log(Level.INFO,
                "Successfully logged in to server.");
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