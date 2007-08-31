package de.dal33t.powerfolder.util.os.Win32;

import java.util.Collection;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.os.NetworkHelper;
import de.dal33t.powerfolder.util.os.OSUtil;

public class NetworkHelperImpl extends NetworkHelper {
    private static Logger LOG = Logger.getLogger(NetworkHelperImpl.class);
    private static boolean error = false;

    public static boolean loadLibrary() {
        if (error) {
            // Don't try forever if once failed
            return false;
        }
        
    	error = !OSUtil.loadLibrary(LOG, "netutil");
    	return !error;
    }

    public native Collection<String[]> getInterfaceAddresses();
}
