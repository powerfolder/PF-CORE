/* $Id$
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Frame;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Checks the connectivity if run and opens a dialog when UI is open.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class LimitedConnectivityChecker extends TimerTask {
    private static final Logger LOG = Logger
        .getLogger(LimitedConnectivityChecker.class);
    /**
     * the number of seconds (aprox) of delay till the connection is tested and
     * a warning may be displayed
     */
    public static final int TEST_CONNECTIVITY_DELAY = 300;

    // the pref name that holds a boolean value if the connection should be
    // tested and a warning displayed if no incomming connections
    public static final String PREF_NAME_TEST_CONNECTIVITY = "test_for_connectivity";

    private Controller controller;

    public LimitedConnectivityChecker(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        this.controller = controller;
    }

    private Controller getController() {
        return controller;
    }

    @Override
    public void run()
    {
        final Preferences pref = getController().getPreferences();
        boolean testConnectivity = pref.getBoolean(PREF_NAME_TEST_CONNECTIVITY,
            true); // true = default
        if (testConnectivity && !getController().isLanOnly()) {
            LOG.debug("Checking connecvitivty");
            if (getController().hasLimitedConnectivity()
                && getController().isUIOpen())
            {
                LOG.warn("Limited connectivity detected");
                Runnable showMessage = new Runnable() {
                    public void run() {
                        boolean showAgain = showLimitedConnectivityWarning(controller
                            .getUIController().getMainFrame().getUIComponent());
                        if (!showAgain) {
                            pref.putBoolean(PREF_NAME_TEST_CONNECTIVITY, false);
                            LOG.warn("store do not show this dialog again");
                        }
                    }
                };
                getController().getUIController().invokeLater(showMessage);
            }
        }
    }

    /**
     * Shows a dialog for limited connectivity.
     * 
     * @param parent
     *            the parent
     * @return if the user doesn't want to see the dialog again.
     */
    private static final boolean showLimitedConnectivityWarning(Frame parent) {
        return DialogFactory.showNeverAskAgainMessageDialog(parent, Translation
            .getTranslation("limitedconnection.title"), Translation
            .getTranslation("limitedconnection.text"), Translation
            .getTranslation("general.show_never_again"));
    }
}
