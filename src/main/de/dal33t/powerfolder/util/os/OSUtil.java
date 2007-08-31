package de.dal33t.powerfolder.util.os;

import de.dal33t.powerfolder.util.Logger;
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
        return (os != null) ? os.toLowerCase().indexOf("windows") >= 0 : false;
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

    
    /**
     * Tries to load a library of powerfolder.
     * Since there might be no dlls/sos in the folder if acting as a dev it also tries the src/etc path. 
     * @param log 
     * @param lib
     */
    public static boolean loadLibrary(Logger log, String lib) {
    	try {
            log.verbose("Loading library: winutils.dll");
			System.loadLibrary(lib);
			return true;
		} catch (UnsatisfiedLinkError e) {
			log.error(
				"Error loading " + lib + " library. Retrying with /src/etc path...");
			try {
				System.loadLibrary("src/etc/" + lib);
				log.info("Successfully loaded " + lib + " from /src/etc path.");
				return true;
			} catch (UnsatisfiedLinkError e2) {
				log.error(e2);
			}
		}
		return false;
    }
}
