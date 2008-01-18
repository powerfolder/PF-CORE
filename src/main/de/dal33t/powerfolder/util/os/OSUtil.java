package de.dal33t.powerfolder.util.os;

import java.io.File;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Util;
import snoozesoft.systray4j.SysTrayMenu;

public class OSUtil {

    // no instances
    private OSUtil() {
    }

    /**
     * Answers if current system is running windows
     *
     * @return
     */
    public static boolean isWindowsSystem() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("windows") >= 0;
    }

    /**
     * Answers if current system is running windows vista
     *
     * @return
     */
    public static boolean isWindowsVistaSystem() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("windows vista") >= 0;
    }

    /**
     * Answers if the operating system is win Me or older (98, 95)
     * 
     * @return
     */
    public static boolean isWindowsMEorOlder() {
        String os = System.getProperty("os.name");
        return os.endsWith("Me") || os.endsWith("98") || os.endsWith("95");
    }

    /**
     * Answers if the operating system is mac os
     * 
     * @return
     */
    public static boolean isMacOS() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("mac");
    }

    /**
     * Answers if the operating system is a linux os
     * 
     * @return
     */
    public static boolean isLinux() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().indexOf("linux") != -1;
    }

    /**
     * Determines if this is a web start via Java WebStart
     * 
     * @return true if started via web
     */
    public static boolean isWebStart() {
        return !System.getProperty("using.webstart", "false").equals("false");
    }

    /**
     * @return true if powerfolder runs as system service.
     */
    public static boolean isSystemService() {
        return System.getProperty("systemservice", "false").equalsIgnoreCase("true");
    }

    /**
     * Systray only on win2000 and newer. win 98/ME gives a "could not create
     * main-window error"
     */
    public static boolean isSystraySupported() {
        return isWindowsSystem() && !isWindowsMEorOlder()
            && SysTrayMenu.isAvailable();
    }

    
    private static boolean loadLibrary(Logger log, String file, boolean absPath) {
    	try {
            log.verbose("Loading library: " + file);
            if (absPath) {
            	System.load(file);
            } else {
            	System.loadLibrary(file);
            }
			return true;
		} catch (UnsatisfiedLinkError e) {
			log.verbose(e);
			return false;
		}
    }
    
    /**
     * Tries to load a library of PowerFolder.
     * It tries to load the lib from several locations.
     * @param log 
     * @param lib
     */
    public static boolean loadLibrary(Logger log, String lib) {
    	if (loadLibrary(log, lib, false)) {
    		return true;
    	}
		if (loadLibrary(log, "src/etc/" + lib, false)) {
			return true;
		}

		log.error("Failed to load " + lib + " the 'normal' way. Trying to copy over the libraries.");
		String ext = "dll";
		if (OSUtil.isLinux()) {
			ext = "so";
		}
		File fLib = Util.copyResourceTo(lib + "." + ext, "", 
				new File(System.getProperty("java.io.tmpdir")), true);
		if (fLib != null && loadLibrary(log, fLib.getAbsolutePath(), true)) {
			return true;
		}  
		log.error("Completely failed to load " + lib);
		return false;
    }
}
