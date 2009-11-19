/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.util.os;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Util;

import java.awt.SystemTray;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OSUtil {

    private static final Logger log = Logger.getLogger(OSUtil.class.getName());
    private static boolean sysTraySupport = true;

    // no instances
    private OSUtil() {
    }

    private static Boolean windows;

    /**
     * @return if current system is running windows
     */
    public static boolean isWindowsSystem() {
        if (windows == null) {
            String os = System.getProperty("os.name");
            windows = Boolean.valueOf(os != null
                && os.toLowerCase().indexOf("windows") >= 0);
        }
        return windows.booleanValue();
    }

    /**
     * @return if current system is running windows vista
     */
    public static boolean isWindowsVistaSystem() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("windows vista") >= 0;
    }

    /**
     * @return true if this is a Google Android system
     */
    public static boolean isAnroidSystem() {
        String java = System.getProperty("java.vendor");
        return java != null && java.toLowerCase().indexOf("android") >= 0;
    }

    /**
     * @return true if the operating system is win Me or older (98, 95)
     */
    public static boolean isWindowsMEorOlder() {
        String os = System.getProperty("os.name");
        return os.endsWith("Me") || os.endsWith("98") || os.endsWith("95");
    }

    /**
     * @return if the operating system is mac os
     */
    public static boolean isMacOS() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("mac");
    }

    /**
     * @return if the operating system is a linux os
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
        return System.getProperty("systemservice", "false").equalsIgnoreCase(
            "true");
    }

    /**
     * Systray only on win2000 and newer. win 98/ME gives a "could not create
     * main-window error"
     * 
     * @return if systray is supported on this platform
     */
    public static boolean isSystraySupported() {
        if (!sysTraySupport) {
            return false;
        }
        try {
            return SystemTray.isSupported();
        } catch (LinkageError e) {
            return false;
        }
    }

    /**
     * Disable Systray support.
     */
    public static void disableSystray() {
        sysTraySupport = false;
    }

    private static boolean loadLibrary(Class clazz, String file,
        boolean absPath, boolean logErrorsVerbose)
    {
        try {
            log.finer(clazz.getName() + " --> Loading library: " + file);
            if (absPath) {
                System.load(file);
            } else {
                System.loadLibrary(file);
            }
            return true;
        } catch (UnsatisfiedLinkError e) {
            if (logErrorsVerbose) {
                log.log(Level.FINER, "UnsatisfiedLinkError", e);
            } else {
                log.log(Level.SEVERE, "UnsatisfiedLinkError", e);
            }
            return false;
        }
    }

    /**
     * Tries to load a library of PowerFolder. It tries to load the lib from
     * several locations.
     * 
     * @param clazz
     * @param lib
     * @return if succeeded
     */
    public static boolean loadLibrary(Class clazz, String lib) {
        String dir = "";
        if (isWindowsSystem()) {
            dir = "win32libs";
        } else if (isMacOS() || isLinux()) {
            dir = "lin32libs";
        }

        String file = System.mapLibraryName(lib);
        File targetFile = new File(Controller.getTempFilesLocation(), file);
        targetFile.deleteOnExit();
        File fLib = Util.copyResourceTo(file, dir, targetFile);
        if (fLib == null) {
            targetFile = new File(Controller.getTempFilesLocation(), file
                + "-1");
            fLib = Util.copyResourceTo(file, dir, targetFile);
        }

        if (fLib == null) {
            log.severe(clazz.getName() + " --> Completely failed to load "
                + lib + ": Failed to copy resource!");
            return false;
        }
        if (loadLibrary(clazz, lib, false, true)) {
            return true;
        }
        if (loadLibrary(clazz, fLib.getAbsolutePath(), true, false)) {
            return true;
        }
        log.severe(clazz.getName() + " --> Completely failed to load " + lib
            + " - see error above!");
        return false;
    }
}
