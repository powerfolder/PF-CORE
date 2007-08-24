package de.dal33t.powerfolder.util.os.Win32;

import java.io.File;
import java.util.Collection;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.os.NetworkHelper;

public class NetworkHelperImpl extends NetworkHelper {
    private static Logger LOG = Logger.getLogger(NetworkHelperImpl.class);
    public final static String LIBRARY = "netutil";
    private static boolean error = false;

    public static boolean loadLibrary() {
        if (error) {
            // Don't try forever if once failed
            return false;
        }
        
        try {
            LOG.verbose("Loading library: " + LIBRARY);
            System.loadLibrary(LIBRARY);
            return true;
        } catch (UnsatisfiedLinkError e) {
            LOG.error("loading library step one failed: " + LIBRARY, e);
            // WORKAROUND: For PowerFolder webstart this workaround is
            // required (FIXME Still needed?)
            try {
                File base = new File(Controller.getTempFilesLocation(), LIBRARY);
                LOG.warn("Loading library (harder): " + base.getAbsolutePath());
                System.loadLibrary(base.getAbsolutePath());
                LOG.verbose("Loading library: " + LIBRARY + " succeded");
                return true;
            } catch (UnsatisfiedLinkError e2) {
                LOG.error("Loading library failed: " + LIBRARY, e2);
                error = true;
                return false;
            }
        }
    }

    public native Collection<String[]> getInterfaceAddresses();
}
