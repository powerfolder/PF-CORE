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
 * $Id: OSUtil.java 18875 2012-05-15 00:10:09Z sprajc $
 */
package de.dal33t.powerfolder.util.os;

import java.awt.SystemTray;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;

public class OSUtil {

    private static final Logger log = Logger.getLogger(OSUtil.class.getName());
    private static Boolean sysTraySupport;

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
     * @return if current system is running windows vista
     */
    public static boolean isWindows7System() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("windows 7") >= 0;
    }

    /**
     * @return if current system is running windows XP
     */
    public static boolean isWindowsXPSystem() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("windows xp") >= 0;
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
     * Tested on Mac OS X 10.6.5 Build 10H574.
     *
     * @return if the operating system is mac os x 10.6 or newer.
     */
    public static boolean isMacOSSnowLeopardOrNewer() {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Mac OS X")) {
            return false;
        }

        // split the "10.x.y" version number
        String osVersion = System.getProperty("os.version");
        String[] fragments = osVersion.split("\\.");

        // sanity check the "10." part of the version
        if (!fragments[0].equals("10")) {
            return false;
        }
        if (fragments.length < 2) {
            return false;
        }

        // check if Mac OS X 10.6(.y)
        try {
            int minorVers = Integer.parseInt(fragments[1]);
            if (minorVers >= 6) {
                return true;
            }
        } catch (NumberFormatException e) {
            // was not an integer
        }

        return false;
    }

    /**
     * @return if the operating system is a linux os
     */
    public static boolean isLinux() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().indexOf("linux") != -1;
    }

    /**
     * Tested on Windows Vista 64 bit with 32 bit VM. This method correctly
     * returns false on this setup.
     * <P>
     * http://stackoverflow.com/questions/807263/how-do-i-detect-which-kind-of-
     * jre-is-installed-32bit-vs-64bit
     *
     * @return true if this VM is running a 64 bit version. false if 32 bit.
     */
    public static boolean is64BitPlatform() {
        String arch = System.getProperty("sun.arch.data.model");
        if (arch != null) {
            return arch.contains("64");
        }
        // Try harder
        return System.getProperty("os.arch").contains("64");
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
     * #2751: java.vm.name=Excelsior JET
     *
     * @return
     */
    public static boolean isJETRuntime() {
        return System.getProperty("java.vm.name", "Oracle VM").toLowerCase()
            .contains("jet");
    }

    /**
     * Systray only on win2000 and newer. win 98/ME gives a "could not create
     * main-window error"
     *
     * @return if systray is supported on this platform
     */
    public static boolean isSystraySupported() {
        if (sysTraySupport == null) {
            sysTraySupport = SystemTray.isSupported();
        }
        return sysTraySupport;
    }

    /**
     * @return @code True if start up items are supported on the current
     *         platform, @code fals otherwise
     */
    public static boolean isStartupItemSupported() {
        return OSUtil.isWindowsSystem() || OSUtil.isMacOS();
    }

    /**
     * @param controller
     * @return @code True if the system has a start up item implemented, @code
     *         false otherwise.
     * @throws UnsupportedOperationException
     *             If the platform does not support start up items.
     */
    public static boolean hasPFStartup(Controller controller)
        throws UnsupportedOperationException
    {
        if (OSUtil.isWindowsSystem() && WinUtils.isSupported()) {
            return WinUtils.getInstance().hasPFStartup(controller);
        } else if (OSUtil.isMacOS() && MacUtils.isSupported()) {
            return MacUtils.getInstance().hasPFStartup(controller);
        }

        throw new UnsupportedOperationException(
            "This platform does not support start up items");
    }

    /**
     * @param setup
     * @code True to set the start up item, @code false to remove it.
     * @param controller
     *            The controller
     * @throws IOException
     *             If the start up item could not be set.
     * @throws UnsupportedOperationException
     *             If this method was called on a platform that does not support
     *             to set the start up item.
     */
    public static void setPFStartup(boolean setup, Controller controller)
        throws IOException, UnsupportedOperationException
    {
        if (OSUtil.isWindowsSystem()) {
            WinUtils.getInstance().setPFStartup(setup, controller);
        } else if (OSUtil.isMacOS()) {
            MacUtils.getInstance().setPFStartup(setup, controller);
        }

        throw new UnsupportedOperationException(
            "This platform does not support start up items");
    }

    /**
     * Disable Systray support.
     */
    public static void disableSystray() {
        sysTraySupport = false;
    }

    private static boolean loadLibrary(Class<?> clazz, String file,
        boolean absPath, boolean quiet)
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
            if (quiet) {
                log.log(Level.FINER, "UnsatisfiedLinkError. " + e);
            } else {
                log.log(Level.WARNING, "UnsatisfiedLinkError. " + e);
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
    public static boolean loadLibrary(Class<?> clazz, String lib) {
        String dir = "";
        if (isWindowsSystem()) {
            dir = is64BitPlatform() ? "win64libs" : "win32libs";
        } else if (isLinux()) {
            dir = is64BitPlatform() ? "lin64libs" : "lin32libs";
        } else if (isMacOS()) {
            dir = "mac64libs";
        }

        String fileName = System.mapLibraryName(lib);
        Path targetFile = null;
        Path fLib;

        int i = 0;
        do {
            String libName = lib;
            if (i > 0) {
                libName += "-" + i;
            }
            String altFileName = System.mapLibraryName(libName);
            targetFile = Controller.getTempFilesLocation().resolve(
                altFileName);
            targetFile.toFile().deleteOnExit();
            boolean quiet = i != 1;
            fLib = Util.copyResourceTo(fileName, dir, targetFile, false, quiet);

            // Usually not possible.
            if (loadLibrary(clazz, lib, false, true)) {
                return true;
            }
            if (fLib != null) {
                try {
                    if (loadLibrary(clazz, fLib.toAbsolutePath().toString(), true, quiet))
                    {
                        return true;
                    }
                } catch (UnsatisfiedLinkError e) {
                    log.warning("Unable to load library " + lib + ": " + e);
                }
            }
            i++;
        } while (i < 5);

        if (fLib == null) {
            log.warning(clazz.getName() + " --> Completely failed to load "
                + lib + ": Failed to copy resource to " + targetFile);
            return false;
        }

        log.warning(clazz.getName() + " --> Completely failed to load " + lib
            + " - see error above!");
        return false;
    }
}
