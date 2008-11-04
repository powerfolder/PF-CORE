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
package de.dal33t.powerfolder.util.ui;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.message.KnownNodes;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import org.apache.commons.lang.StringUtils;

import javax.swing.JFrame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks the connectivity if run and opens a dialog when UI is open.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class LimitedConnectivityChecker {

    private static final Logger log = Logger
        .getLogger(LimitedConnectivityChecker.class.getName());
    private static final String LIMITED_CONNECTIVITY_TEST_SUCCESSFULLY_STRING = "LIMITED CONNECTIVITY TEST SUCCESSFULLY";
    private Controller controller;
    private String host;
    private int port;

    public LimitedConnectivityChecker(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        this.controller = controller;
    }

    /**
     * Central method to check if the connectivity is limited. Method call may
     * take some time.
     * 
     * @return true the connectivty is limited.
     */
    public boolean hasLimitedConnecvitiy() {
        if (!controller.getNodeManager().getMySelf().isSupernode()) {
            if (controller.getOSClient().isConnected()) {
                log
                    .fine("No limited connectivity. Connected to the Online Storage");
                return false;
            }
            if (controller.getIOProvider().getRelayedConnectionManager()
                .getRelay() != null)
            {
                log.fine("No limited connectivity. Connected to a relay");
                return false;
            }
            // If not, try the full incoming connection check.
        }

        // Be more restrictive on supernode. Needs incoming connections from
        // internet. or clients without a connected webservice.
        if (!resolveHostAndPort()) {
            log.warning("Unable resolve own host");
            return true;
        }

        // Try two times, just to make sure we don't hit a full backlog
        boolean connectOK = isConnectPossible() || isConnectPossible()
            || isConnectPossible();
        return !connectOK;
    }

    /**
     * Installs a task to check the connecvitity of this system once. or when
     * the networking mode changes.
     * 
     * @param controller
     */
    public static void install(final Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        CheckTask task = new CheckTask(controller, true);
        controller.getThreadPool().execute(task);

        // Support networking mode switch.
        controller.addPropertyChangeListener(
            Controller.PROPERTY_NETWORKING_MODE, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    controller.getIOProvider().startIO(
                        new CheckTask(controller, false));
                }
            });
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    // Helper class ***********************************************************

    public static class CheckTask implements Runnable {
        private Controller controller;
        private boolean delay;

        public CheckTask(Controller controller, boolean delay) {
            super();
            this.controller = controller;
            this.delay = delay;
        }

        public void run() {
            if (controller.isLanOnly()) {
                // No limited connecvitiy in lan only mode.
                controller.setLimitedConnectivity(false);
                return;
            }
            if (!PreferencesEntry.TEST_CONNECTIVITY.getValueBoolean(controller))
            {
                return;
            }

            if (delay) {
                try {
                    Thread
                        .sleep(Constants.LIMITED_CONNECTIVITY_CHECK_DELAY * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
            LimitedConnectivityChecker checker = new LimitedConnectivityChecker(
                controller);
            log.fine("Checking for limited connectivity (" + checker.getHost()
                + ':' + checker.getPort() + ')');
            // boolean wasLimited = controller.isLimitedConnectivity();
            boolean nowLimited = checker.hasLimitedConnecvitiy();
            controller.setLimitedConnectivity(nowLimited);

            if (nowLimited) {
                log.warning("Limited connectivity detected ("
                    + checker.getHost() + ':' + checker.getPort() + ')');
            } else {
                log.info("Connectivity is good (not limited)");
            }

            setSupernodeState(nowLimited);
        }

        private void setSupernodeState(boolean limitedCon) {
            boolean dyndnsSetup = controller.getConnectionListener()
                .getMyDynDns() != null;
            if (dyndnsSetup) {
                controller.getMySelf().getInfo().isSupernode = !limitedCon;
                if (controller.getMySelf().getInfo().isSupernode) {
                    log.fine("Acting as supernode on address "
                        + controller.getMySelf().getInfo().getConnectAddress());
                    // Broadcast our new status, we want stats ;)
                    controller.getNodeManager().broadcastMessage(
                        new KnownNodes(controller.getMySelf().getInfo()));
                }
            }
        }
    }

    // Internal logic *********************************************************

    private boolean resolveHostAndPort() {
        String dyndns = ConfigurationEntry.DYNDNS_HOSTNAME.getValue(controller);
        boolean hasDyndnsSetup = !StringUtils.isEmpty(dyndns);
        port = controller.getConnectionListener().getPort();

        // 1.1) Setup with dyndns = best inet setup
        if (hasDyndnsSetup) {
            try {
                InetAddress.getAllByName(dyndns);
                host = dyndns;
            } catch (UnknownHostException e) {
                // Dyndns host could not be resolved
                host = null;
            }
        }

        // 1.2) Setup with provider ip = moderate inet setup
        if (StringUtils.isEmpty(host)) {
            host = controller.getDynDnsManager().getIPviaHTTPCheckIP();
        }

        log.finer("Will check connectivity on " + host + ':' + port);
        return !StringUtils.isEmpty(host);
    }

    private boolean isConnectPossible() {
        Reject.ifBlank(host, "Hostname or port resolved, resolve it first");
        Reject.ifTrue(port <= 0, "Hostname or port resolved, resolve it first");

        URL url;
        try {
            url = new URL(Constants.LIMITED_CONNECTIVTY_CHECK_URL + "?host="
                + host + "&port=" + port);
        } catch (MalformedURLException e) {
            log.log(Level.WARNING, "Limited connectivity check failed for "
                + host + ':' + port, e);
            return false;
        }

        InputStream in = null;
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(30 * 1000);
            con.setReadTimeout(30 * 1000);
            con.connect();
            in = con.getInputStream();
            Reader reader = new InputStreamReader(new BufferedInputStream(in));
            CharBuffer buf = CharBuffer.allocate(in.available());
            reader.read(buf);
            reader.close();
            String testString = new String(buf.array());
            return testString
                .contains(LIMITED_CONNECTIVITY_TEST_SUCCESSFULLY_STRING);
        } catch (SocketTimeoutException e) {
            log.log(Level.WARNING, "Limited connectivity check failed for "
                + host + ':' + port, e);
            log.log(Level.FINER, "SocketTimeoutException", e);
            return false;
        } catch (IOException e) {
            log.log(Level.WARNING, "Limited connectivity check failed for "
                + host + ':' + port, e);
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.log(Level.FINER, "IOException", e);
                }
            }
        }
    }

    public static void showConnectivityWarning(final Controller controllerArg) {
        Runnable showMessage = new Runnable() {
            public void run() {
                JFrame parent = controllerArg.getUIController().getMainFrame()
                    .getUIComponent();
                NeverAskAgainResponse response = DialogFactory.genericDialog(
                    parent, Translation
                        .getTranslation("limited_connection.title"),
                    Translation.getTranslation("limited_connection.text"),
                    new String[]{Translation.getTranslation("general.ok")}, 0,
                    GenericDialogType.INFO, Translation
                        .getTranslation("limited_connection.dont_autodetect"));

                if (response.isNeverAskAgain()) {
                    PreferencesEntry.TEST_CONNECTIVITY.setValue(controllerArg,
                        false);
                    log.warning("store do not show this dialog again");
                }
            }
        };
        controllerArg.getUIController().invokeLater(showMessage);
    }
}
