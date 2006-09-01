package de.dal33t.powerfolder.util.os.Win32;

import java.io.File;
import java.util.Collection;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.NetworkHelper;

public class NetworkHelperImpl extends NetworkHelper {
    private static Logger LOG = Logger.getLogger(RecycleDeleteImpl.class);
    public final static String LIBRARY = "netutil";

    public static boolean loadLibrary() {
        
        try {
        	File netutil = Util.copyResourceTo(LIBRARY + ".dll", 
        			"de/dal33t/powerfolder/util/os/Win32", 
        			new File("."), true);
        	if (netutil == null) {
        		LOG.error("Couldn't load " + LIBRARY);
        		return false;
        	}
            LOG.verbose("Loading library: " + LIBRARY);
            System.loadLibrary(LIBRARY);
            return true;
        } catch (UnsatisfiedLinkError e) {
            LOG.error(e);
            // WORKAROUND: For PowerFolder webstart this workaround is
            // required (FIXME Still needed?)
            try {
                File base = new File(Controller.getTempFilesLocation(), LIBRARY);
                LOG.warn("Loading library (harder): " + base.getAbsolutePath());
                System.loadLibrary(base.getAbsolutePath());
                return true;
            } catch (UnsatisfiedLinkError e2) {
                LOG.error(e2);
                return false;
            }
        }
    }

    public native Collection<String[]> getInterfaceAddresses();
}
