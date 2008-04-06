package de.dal33t.powerfolder.util.os;

import java.io.File;
import java.lang.reflect.Field;

import org.jdesktop.jdic.tray.SystemTray;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Util;

public class OSUtil {
    // This really should only be executed once per VM
    static {
        installThirdPartyLibraries();
        System.setProperty("java.library.path", Controller.getTempFilesLocation()
            .getAbsolutePath()
            + System.getProperty("path.separator")
            + System.getProperty("java.library.path"));
        hackUnlockLibraryPath(Logger.getLogger(Controller.class));
    }
    
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
        try {
            return SystemTray.getDefaultSystemTray() != null;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }


    private static boolean loadLibrary(Logger log, String file, boolean absPath, boolean logErrorsVerbose) {
        try {
            log.verbose("Loading library: " + file);
            if (absPath) {
                System.load(file);
            } else {
                System.loadLibrary(file);
            }
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (logErrorsVerbose) {
                log.verbose(e);
            } else {
                log.error(e);
            }
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
        String dir = "";
        if (OSUtil.isWindowsSystem()) {
            dir = "win32libs/";
        } else if (OSUtil.isMacOS() || OSUtil.isLinux()) {
            dir = "lin32libs/";
        }

        String file = System.mapLibraryName(lib);
        File fLib = Util.copyResourceTo(file, dir, 
            Controller.getTempFilesLocation(), true);

        if (fLib == null) { 
            log.error("Completely failed to load " + lib + ": Failed to copy resource!");
            return false;
        }
        if (loadLibrary(log, lib, false, true)) {
            return true;
        }  
        if (loadLibrary(log, fLib.getAbsolutePath(), true, false)) {
            return true;
        }  
        log.error("Completely failed to load " + lib + " - see error above!");
        return false;
    }

    public static void installThirdPartyLibraries() {
        String dir = "";
        if (OSUtil.isWindowsSystem()) {
            dir = "win32libs/";
        } else if (OSUtil.isMacOS() || OSUtil.isLinux()) {
            dir = "lin32libs/";
        }
        String[] libraries = new String[] {
            "jdic", "tray"
        };
        for (String file: libraries) {
            Util.copyResourceTo(System.mapLibraryName(file), dir, Controller.getTempFilesLocation(), true);
        }
    }
    
    /**
     * Calling this will unlock the java.library.path property.
     * <b>NOTE:</b>This method accesses a java internal private variable. 
     * If SUN decides to change this code in the future, this method will do
     * nothing at all, besides logging an error.
     * @param log
     */
    public static void hackUnlockLibraryPath(Logger log) {
        try {
            Field usr_paths = ClassLoader.class.getDeclaredField("usr_paths");
            usr_paths.setAccessible(true);
            usr_paths.set(ClassLoader.getSystemClassLoader(), null);
            usr_paths.setAccessible(false);
        } catch (Exception e) {
            if (log != null) {
                log.error(e);
            } else {
                e.printStackTrace();
            }
        }
    }
}
