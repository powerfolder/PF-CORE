package de.dal33t.powerfolder.util.os;

import java.io.File;

import de.dal33t.powerfolder.util.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.RecycleDeleteImpl;

/**
 * Access to native recycle bin, now only implemented for win32 platform
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class RecycleDelete {
    private static boolean isWinLibLoaded = false;

    static {
        if (OSUtil.isWindowsSystem()) {
            isWinLibLoaded = RecycleDeleteImpl.loadLibrary();
        }
    }

    /** is Recycle Bin Delete supported on this platform? */
    public static boolean isSupported() {
        
        // only on windows 2000+, unicode should be checked in native code for
        // older windows
        return OSUtil.isWindowsSystem() && !OSUtil.isWindowsMEorOlder()
            && isWinLibLoaded;
    }

    /**
     * is a progress bar indication the deletion progress supported on this
     * platform?
     */
    public static boolean progressSupported() {
        return OSUtil.isWindowsSystem() && isWinLibLoaded;
    }

    /** is a Yes/No confirmation dialog supported */
    public static boolean confirmSupported() {
        return OSUtil.isWindowsSystem() && isWinLibLoaded;
    }

    /**
     * Will move file to recycle bin. filename is a fully qualified
     * path+filename to an existing file. (So check before calling this method
     * if file exists.) Check isSupported before use.
     * 
     * @param filename
     * @see isSupported
     * @see File.exists()
     */
    public static void delete(String filename) {
        if (!isSupported()) {
            throw new UnsupportedOperationException(
                "delete not supported on this platform");
        }
        File file = new File(filename);
        if (!file.exists()) {
            throw new IllegalStateException("file does not exists");
        }
        RecycleDeleteImpl.delete(filename);
    }

    /**
     * Will move file to recycle bin. filename is a fully qualified
     * path+filename to an existing file. (So check before calling this method
     * if file exists.) Check isSupported before use.
     * 
     * @param confirm
     *            show a yes/No confirm dialog
     * @param filename
     * @see isSupported
     * @see confirmSupported
     * @see File.exists()
     */
    public static void delete(String filename, boolean confirm) {
        if (!confirmSupported()) {
            throw new UnsupportedOperationException(
                "delete with confirm not supported on this platform");
        }
        if (!isSupported()) {
            throw new UnsupportedOperationException(
                "delete not supported on this platform");
        }
        File file = new File(filename);
        if (!file.exists()) {
            throw new IllegalStateException("file does not exists");
        }
        RecycleDeleteImpl.delete(filename, confirm);
    }

    /**
     * Will move file to recycle bin. filename is a fully qualified
     * path+filename to an existing file. (So check before calling this method
     * if file exists.) Check isSupported before use.
     * 
     * @param confirm
     *            show a yes/No confirm dialog
     * @param showProgress
     *            if set to true progress dialog is shown
     * @param filename
     * @see isSupported
     * @see confirmSupported
     * @see progressSupported
     * @see File.exists()
     */
    public static void delete(String filename, boolean confirm,
        boolean showProgress)
    {
        if (!confirmSupported()) {
            throw new UnsupportedOperationException(
                "delete with confirm not supported on this platform");
        }
        if (!isSupported()) {
            throw new UnsupportedOperationException(
                "delete not supported on this platform");
        }
        if (!progressSupported()) {
            throw new UnsupportedOperationException(
                "delete with progress bar not supported on this platform");
        }
        File file = new File(filename);
        if (!file.exists()) {
            throw new IllegalStateException("file does not exists");
        }
        RecycleDeleteImpl.delete(filename, confirm, showProgress);
    }
}
