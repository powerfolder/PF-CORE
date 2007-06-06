/* $Id$
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Frame;
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
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Checks the connectivity if run and opens a dialog when UI is open.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class LimitedConnectivityChecker extends Loggable {
    private static final String LIMITED_CONNECTIVITY_TEST_SUCCESSFULLY_STRING = "LIMITED CONNECTIVITY TEST SUCCESSFULLY";

    private static final Logger LOG = Logger
        .getLogger(LimitedConnectivityChecker.class);

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
        if (!resolveHostAndPort()) {
            log().warn("Unable resolve own host");
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
        CheckTask task = new CheckTask(controller);
        controller.schedule(task,
            Constants.LIMITED_CONNECTIVITY_CHECK_DELAY * 1000);

        // Support networking mode switch.
        controller.addPropertyChangeListener(
            Controller.PROPERTY_NETWORKING_MODE, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    controller.schedule(new CheckTask(controller), 0);
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

    public static class CheckTask extends TimerTask {
        private Controller controller;

        public CheckTask(Controller controller) {
            super();
            this.controller = controller;
        }

        @Override
        public void run()
        {
            if (controller.isLanOnly()) {
                // No limited connecvitiy in lan only mode.
                controller.setLimitedConnectivity(false);
                return;
            }
            if (!PreferencesEntry.TEST_CONNECTIVITY.getValueBoolean(controller))
            {
                return;
            }

            LimitedConnectivityChecker checker = new LimitedConnectivityChecker(
                controller);
            LOG.debug("Checking for limited connectivity (" + checker.getHost()
                + ":" + checker.getPort() + ")");
            // boolean wasLimited = controller.isLimitedConnectivity();
            boolean nowLimited = checker.hasLimitedConnecvitiy();
            controller.setLimitedConnectivity(nowLimited);

            if (nowLimited) {
                LOG.warn("Limited connectivity detected (" + checker.getHost()
                    + ":" + checker.getPort() + ")");
            } else {
                LOG.info("Connectivity is good (not limited)");
            }

            setSupernodeState(nowLimited);
        }

        private void setSupernodeState(boolean limitedCon) {
            boolean dyndnsSetup = controller.getConnectionListener()
                .getMyDynDns() != null;
            if (dyndnsSetup) {
                controller.getMySelf().getInfo().isSupernode = !limitedCon;
                if (controller.getMySelf().getInfo().isSupernode) {
                    LOG.debug("Acting as supernode on address "
                        + controller.getMySelf().getInfo().getConnectAddress());
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

        log().verbose("Will check connectivity on " + host + ":" + port);
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
            LOG.warn("Limited connectivity check failed for " + host + ":"
                + port, e);
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
            LOG.verbose("Limited connectivity check failed for " + host + ":"
                + port, e);
            return false;
        } catch (IOException e) {
            LOG.warn("Limited connectivity check failed for " + host + ":"
                + port, e);
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log().verbose(e);
                }
            }
        }
    }

    public static void showConnectivityWarning(final Controller controller) {
        Runnable showMessage = new Runnable() {
            public void run() {
                Frame parent = controller.getUIController().getMainFrame()
                    .getUIComponent();
                boolean showAgain = DialogFactory
                    .showNeverAskAgainMessageDialog(parent, Translation
                        .getTranslation("limitedconnection.title"), Translation
                        .getTranslation("limitedconnection.text"), Translation
                        .getTranslation("limitedconnection.dont_autodetect"));

                if (!showAgain) {
                    PreferencesEntry.TEST_CONNECTIVITY.setValue(controller,
                        false);
                    LOG.warn("store do not show this dialog again");
                }
            }
        };
        controller.getUIController().invokeLater(showMessage);
    }
}
