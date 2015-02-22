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
 * $Id: WinUtils.java 20555 2012-12-25 04:15:08Z glasgow $
 */
package de.dal33t.powerfolder.util.os.Win32;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Utilities for windows. http://vbnet.mvps.org/index.html?code/browse/csidl.htm
 *
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 */
public class WinUtils extends Loggable {
    private static final Logger LOG = Logger
        .getLogger(WinUtils.class.getName());

    private static final String REGQUERY_UTIL = "reg query ";
    private static final String REGSTR_TOKEN = "REG_SZ";
    private static final String DESKTOP_FOLDER_CMD = REGQUERY_UTIL
        + "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\"
        + "Explorer\\Shell Folders\" /v DESKTOP";
    private static final String MSI_MARKER_FILE = "MSI_INSTALLATION.txt";

    /**
     * The file system directory that contains the programs that appear in the
     * Startup folder for all users. A typical path is C:\Documents and
     * Settings\All Users\Start Menu\Programs\Startup. Valid only for Windows NT
     * systems.
     */
    public static final int CSIDL_COMMON_STARTUP = 0x0018;

    /**
     * The file system directory that corresponds to the user's Startup program
     * group. The system starts these programs whenever any user logs onto
     * Windows NT or starts Windows 95. A typical path is C:\Documents and
     * Settings\\username\\Start Menu\\Programs\\Startup.
     */
    public static final int CSIDL_STARTUP = 0x0007;

    public static final int CSIDL_DESKTOP = 0x0000;

    // Eigene Dokumente / My Documents
    public static final int CSIDL_PERSONAL = 0x0005;

    // Favoriten / Favorites
    public static final int CSIDL_FAVORITES = 0x0006;

    // Start Menu / Start Menu (personal)
    public static final int CSIDL_START_MENU = 0x000B;

    // Meine Musik / My Music
    public static final int CSIDL_MYMUSIC = 0x000d;

    // Meine Videos / My Videaos
    public static final int CSIDL_MYVIDEO = 0x000e;

    // Meine Bilder / My Pictures
    public static final int CSIDL_MYPICTURES = 0x0027;

    // e.g. C:\Windows
    public static final int CSIDL_WINDOWS = 0x0024;

    // Application Data of current user. Roaming and Local
    public static final int CSIDL_APP_DATA = 0x001A;
    public static final int CSIDL_LOCAL_APP_DATA = 0x001C;

    // Application Data of ALL USERs.
    public static final int CSIDL_COMMON_APP_DATA = 0x0023;

    /*
     * Other CSLIDs:
     * http://vbnet.mvps.org/index.html?code/browse/csidlversions.htm
     */

    private static WinUtils instance;
    private static boolean error = false;

    private WinUtils() {
    }

    /**
     * @return true if this platform supports the winutils helpers.
     */
    public static boolean isSupported() {
        return getInstance() != null;
    }

    /**
     * @return the instance or NULL if not supported on this platform.
     */
    public static synchronized WinUtils getInstance() {
        if (!OSUtil.isWindowsSystem()) {
            return null;
        }
        if (instance == null && !error) {
            if (OSUtil.loadLibrary(WinUtils.class, "desktoputils")) {
                instance = new WinUtils();
                instance.init();
            } else {
                error = true;
            }
        }
        return instance;
    }

    /**
     * Retrieve a path from Windows.
     *
     * @param id
     *            the path-id to retrieve
     * @param defaultPath
     *            if true, returns the default path location instead of the
     *            current
     * @return
     */
    public native String getSystemFolderPath(int id, boolean defaultPath);

    public native void createLink(ShellLink link, String lnkTarget)
        throws IOException;

    private native void init();

    /**
     * Create a 'PowerFolders' link in Links, pointing to the PowerFolder base
     * dir.
     *
     * @param setup
     * @param controller
     * @throws IOException
     */
    public void setPFLinks(boolean setup, Controller controller)
        throws IOException
    {
        String userHome = System.getProperty("user.home");
        Path linksDir = Paths.get(userHome, "Links");
        if (Files.notExists(linksDir)) {
            logWarning("Could not locate the Links directory in " + userHome);
            return;
        }
        Path baseDir = controller.getFolderRepository()
            .getFoldersBasedir();
        Path shortCut = linksDir.resolve(baseDir.getFileName().toString()
            + Constants.LINK_EXTENSION);
        if (setup) {
            ShellLink link = new ShellLink(null, baseDir.getFileName().toString(),
                baseDir.toAbsolutePath().toString(), null);
            createLink(link, shortCut.toAbsolutePath().toString());
        } else {
            try {
                Files.deleteIfExists(shortCut);
            } catch (IOException ioe) {
                LOG.warning("Unable to delete shortcut " + shortCut.toString()
                    + ". " + ioe);
            }
        }
    }

    public static void removePFLinks(String shortcutName) {
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        Path linksDir = Paths.get(System.getProperty("user.home"), "Links");
        if (Files.notExists(linksDir)) {
            return;
        }
        Path shortCut = linksDir.resolve(shortcutName
            + Constants.LINK_EXTENSION);
        try {
            Files.delete(shortCut);
        } catch (IOException ioe) {
            LOG.warning("Unable to delete shortcut " + shortcutName + ". "
                + ioe);
        }
    }

    public static boolean isPFLinks(String shortcutName) {
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }
        Path linksDir = Paths.get(System.getProperty("user.home"), "Links");
        if (Files.notExists(linksDir)) {
            return false;
        }
        Path shortCut = linksDir.resolve(shortcutName
            + Constants.LINK_EXTENSION);

        return Files.exists(shortCut);
    }

    /**
     * @param setup
     *            @code True to set the start up item, @code false to remove it.
     * @param controller
     * @throws UnsupportedOperationException
     *             If requesting the status of a start up item is not supported
     *             on the platform, or the executable ".app" could not be
     *             located.
     * @throws IOException
     *             If setting the start up item did fail.
     */
    public void setPFStartup(boolean setup, Controller controller)
        throws IOException, UnsupportedOperationException
    {
        Path pfile = Paths.get(System.getProperty("java.class.path"))
            .getParent()
            .resolve(controller.getDistribution().getBinaryName() + ".exe");
        if (Files.notExists(pfile)) {
            pfile = Paths.get(controller.getDistribution().getBinaryName()
                + ".exe").toAbsolutePath();
            if (Files.notExists(pfile)) {
                String message = Translation.get(
                    "exception.startup_item.executable_not_found.text", controller
                        .getDistribution().getBinaryName(), pfile.getFileName()
                        .toString());
                logWarning(message);
                throw new UnsupportedOperationException(message);
            }
            return;
        }
        logFiner("Found " + pfile.toAbsolutePath());
        String shortCutname = controller.getDistribution().getName()
            + Constants.LINK_EXTENSION;
        Path pflnk = Paths.get(getSystemFolderPath(CSIDL_STARTUP, false),
            shortCutname);
        Path pflnkAll = Paths.get(getSystemFolderPath(CSIDL_COMMON_STARTUP,
            false), shortCutname);
        if (setup) {
            ShellLink sl = new ShellLink("--minimized",
                Translation.get("winutils.shortcut.description"),
                pfile.toAbsolutePath().toString(), pfile.getParent().toString());
            logInfo("Creating startup link: " + pflnk.toAbsolutePath());
            createLink(sl, pflnk.toAbsolutePath().toString());
        } else {
            logInfo("Deleting startup link.");
            try {
                Files.deleteIfExists(pflnk);
                Files.deleteIfExists(pflnkAll);

                // PFC-2164: Fallback. Also delete binary name links
                shortCutname = controller.getDistribution().getBinaryName()
                    + Constants.LINK_EXTENSION;
                pflnk = Paths.get(getSystemFolderPath(CSIDL_STARTUP, false),
                    shortCutname);
                pflnkAll = Paths.get(
                    getSystemFolderPath(CSIDL_COMMON_STARTUP, false), shortCutname);

                Files.deleteIfExists(pflnk);
                Files.deleteIfExists(pflnkAll);
            } catch (IOException ioe) {
                logInfo("Could not delete files '" + pflnk.toAbsolutePath()
                    + "' and '" + pflnkAll.toAbsolutePath() + "'", ioe);
            }
        }
    }

    /**
     * @param controller
     * @return @code True if the start up item is set, @code false otherwise.
     */
    public boolean hasPFStartup(Controller controller) {
        String shortCutname = controller.getDistribution().getName()
            + Constants.LINK_EXTENSION;
        Path pflnk = Paths.get(getSystemFolderPath(CSIDL_STARTUP, false),
            shortCutname);
        Path pflnkAll = Paths.get(getSystemFolderPath(CSIDL_COMMON_STARTUP,
            false), shortCutname);
        String shortCutname2 = controller.getDistribution().getBinaryName()
            + Constants.LINK_EXTENSION;
        Path pflnk2 = Paths.get(getSystemFolderPath(CSIDL_STARTUP, false),
            shortCutname2);
        Path pflnkAll2 = Paths.get(getSystemFolderPath(CSIDL_COMMON_STARTUP,
            false), shortCutname2);
        return Files.exists(pflnk) || Files.exists(pflnkAll) || Files.exists(pflnk2)
            || Files.exists(pflnkAll2);
    }

    /**
     * It returns the default location where the PowerFolder installer installs
     * the program.
     *
     * @return the path on a Windows installation or null if unable to resolve.
     */
    public static Path getProgramInstallationPath() {
        String programFiles = System.getenv("PROGRAMFILES");
        if (StringUtils.isBlank(programFiles)) {
            LOG.warning("Unable find installation path. Program files directory not found");
            return null;
        }
        Path f = Paths.get(programFiles + "/PowerFolder.com/PowerFolder");
        if (Files.exists(f)) {
            return f;
        }

        f = Paths.get(programFiles + " (x86)/PowerFolder.com/PowerFolder");
        if (Files.exists(f)) {
            return f;
        }
        // Try harder
        try {
            f = Paths.get("").toRealPath();
            if (Files.exists(f)) {
                return f;
            }
        } catch (IOException e) {
        }
        LOG.warning("Unable find installation path. Program files directory not found");
        return null;
    }

    /**
     * @return the APPDATA directory for placing application data (Current
     *         user).
     */
    public static String getAppDataCurrentUser() {
        String appDataname = System.getenv("APPDATA");
        if (StringUtils.isBlank(appDataname) && WinUtils.getInstance() != null)
        {
            appDataname = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_APP_DATA, false);
        }
        if (StringUtils.isBlank(appDataname) && OSUtil.isWindowsSystem()) {
            LOG.severe("Unable to find APPDATA (current user) directory");
        }
        return appDataname;
    }

    /**
     * @return the APPDATA directory for placing application data (All users).
     */
    public static String getAppDataAllUsers() {
        // "Normal" way. Get it from Windows DLLs directly
        if (WinUtils.getInstance() != null) {
            return WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_COMMON_APP_DATA, false);
        }

        // Windows Vista and Windows 7
        if (StringUtils.isNotBlank(System.getenv("ProgramData"))) {
            // Source:
            // http://en.wikipedia.org/wiki/Environment_variable#Default_Values_on_Microsoft_Windows
            String appDataAllUsers = System.getenv("ProgramData");
            Path dir = Paths.get(appDataAllUsers);
            if (Files.exists(dir)) {
                LOG.warning("Retrieved APPDATA (all users) via ENV(ProgramData): "
                    + appDataAllUsers);
                return appDataAllUsers;
            }
        }

        // Windows XP
        if (StringUtils.isNotBlank(System.getenv("USERPROFILE"))
            && StringUtils.isNotBlank(System.getenv("APPDATA"))
            && StringUtils.isNotBlank(System.getenv("ALLUSERSPROFILE")))
        {
            // Source:
            // http://stackoverflow.com/questions/2517940/windows-application-data-directory
            /*
             * Tested on WinXP 32 bit: APPDATA=C:\Dokumente und
             * Einstellungen\Administrator\Anwendungsdaten
             * USERPROFILE=C:\Dokumente und Einstellungen\Administrator
             * ALLUSERSPROFILE=C:\Dokumente und Einstellungen\All Users
             */
            String userProfile = System.getenv("USERPROFILE");
            String appData = System.getenv("APPDATA");
            String allUsersProfile = System.getenv("ALLUSERSPROFILE");
            String temp = appData.replace(userProfile, "");
            String appDataAllUsers = allUsersProfile + temp;
            Path dir = Paths.get(appDataAllUsers);
            if (Files.exists(dir)) {
                LOG.warning("Retrieved APPDATA (all users) via ENV(USERPROFILE/APPDATA/ALLUSERSPROFILE): "
                    + appDataAllUsers);
                return appDataAllUsers;
            }
        }

        // Fail!
        LOG.severe("Unable to find APPDATA (all users) directory");
        LOG.warning("Dump of environment variables: ");
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            LOG.warning(" " + entry.getKey() + "=" + entry.getValue());
        }
        return null;
    }

    private static final String TASKLIST = "tasklist";
    private static final String KILL = "taskkill /IM ";

    public static boolean isProcessRunging(String serviceName) throws Exception
    {
        Process p = Runtime.getRuntime().exec(TASKLIST);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {

            System.out.println(line);
            if (line.contains(serviceName)) {
                return true;
            }
        }
        return false;
    }

    public static void killProcess(String serviceName) throws Exception {
        Runtime.getRuntime().exec(KILL + serviceName);
    }

    public static boolean isMSI() {
        return Files.exists(getProgramInstallationPath().resolve(MSI_MARKER_FILE));
    }

    /**
     * @return
     */
    private static String getCurrentUserDesktopPath() {
        try {
            Process process = Runtime.getRuntime().exec(DESKTOP_FOLDER_CMD);
            StreamReader reader = new StreamReader(process.getInputStream());

            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            int p = result.indexOf(REGSTR_TOKEN);

            if (p == -1)
                return null;
            return result.substring(p + REGSTR_TOKEN.length()).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            } catch (IOException e) {
                ;
            }
        }

        String getResult() {
            return sw.toString();
        }
    }

}
