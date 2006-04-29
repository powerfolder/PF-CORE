package de.dal33t.powerfolder.util.os.Win32;

import java.io.File;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Logger;

public class RecycleDeleteImpl {
    private static Logger LOG = Logger.getLogger(RecycleDeleteImpl.class);
    public final static String LIBRARY = "delete";
    
    public static boolean loadLibrary() {
        
        try {
            LOG.verbose("Loading library: " + LIBRARY);
            System.loadLibrary(RecycleDeleteImpl.LIBRARY);
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
   

    /**
     * @param filename
     *            a fully qualified path/filename
     * @param confirm
     *            show a yes/No confirm dialog
     * @param showProgress
     *            if set to true progress dialog is shown
     */
    public static native void delete(String filename, boolean confirm,
        boolean showProgress);

    /**
     * @param filename
     *            a fully qualified path/filename
     * @param confirm
     *            show a yes/No confirm dialog
     */
    public static native void delete(String filename, boolean confirm);

    /**
     * @param filename
     *            a fully qualified path/filename
     */
    public static native void delete(String filename);

}
