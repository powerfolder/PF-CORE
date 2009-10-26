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
package de.dal33t.powerfolder.util.os.Win32;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Utilities for windows.
 * http://vbnet.mvps.org/index.html?code/browse/csidl.htm
 *
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 */
public class WinUtils extends Loggable {

    public static final String SHORTCUTNAME = "PowerFolder.lnk";

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

    // Eigenen Dokumente / My Documents
    public static final int CSIDL_PERSONAL = 0x0005;

    // Favoriten / Favorites
    public static final int CSIDL_FAVORITES = 0x0006;

    // Meine Musik / My Music
    public static final int CSIDL_MYMUSIC = 0x000d;

    // Meine Videos / My Videaos
    public static final int CSIDL_MYVIDEO = 0x000e;

    // Meine Bilder / My Pictures
    public static final int CSIDL_MYPICTURES = 0x0027;

    // e.g. C:\Windows
    public static final int CSIDL_WINDOWS = 0x0024;
    
    /**
     * Very common app data
     */
    public static final int CSIDL_COMMON_APPDATA = 0x0023;

    // Program files
    public static final int CSIDL_APP_DATA = 0x001A;
    public static final int CSIDL_LOCAL_SETTINGS_APP_DATA = 0x001C;

    /*
     * Other CSLIDs:
     * http://vbnet.mvps.org/index.html?code/browse/csidlversions.htm
     */

    private static WinUtils instance;
    private static boolean error = false;

    private WinUtils() {
	}

	public static synchronized WinUtils getInstance() {
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
	 * @param id the path-id to retrieve
	 * @param defaultPath if true, returns the default path location instead of the current
	 * @return
	 */
	public native String getSystemFolderPath(int id, boolean defaultPath);
	public native void createLink(ShellLink link, String lnkTarget) throws IOException;
	private native void init();

    /**
     * Create a 'PowerFolders' link in Links, pointing to the PowerFolder base
     * dir.
     * @param setup
     * @param baseDir
     * @throws IOException
     */
    public void setPFFavorite(boolean setup, Controller controller) throws
            IOException {
        String userHome = System.getProperty("user.home");
        File linksDir = new File(userHome, "Links");
        if (!linksDir.exists()) {
            logWarning("Could not locate the Links directory");
            return;
        }
        File shortCut = new File(linksDir, "PowerFolders.lnk");
        if (setup) {
            File baseDir = new File(controller.getFolderRepository()
                    .getFoldersBasedir());
            ShellLink link = new ShellLink(null, "PowerFolders",
                    baseDir.getAbsolutePath(), null);
            createLink(link, shortCut.getAbsolutePath());
        } else {
            shortCut.delete();
        }
    }

	public void setPFStartup(boolean setup) throws IOException {
		File pfile = new File(
			new File(System.getProperty("java.class.path")).getParentFile(),
			"PowerFolder.exe");
		if (!pfile.exists()) {
			logSevere("Couldn't find PowerFolder executable! "
					+ "Note: Setting up a shortcut only works "
					+ "when PowerFolder was started by PowerFolder.exe");
			return;
		}
		logFiner("Found " + pfile.getAbsolutePath());
		File pflnk = new File(getSystemFolderPath(CSIDL_STARTUP, false), SHORTCUTNAME);
        if (setup) {
            ShellLink sl = new ShellLink("--minimized", Translation
                    .getTranslation("winutils.shortcut.description"), pfile
                    .getAbsolutePath(), pfile.getParent());
            logFiner("Creating startup link: " + pflnk.getAbsolutePath());
            createLink(sl, pflnk.getAbsolutePath());
        } else {
            logFiner("Deleting startup link.");
            pflnk.delete();
        }
	}

	public boolean isPFStartup() {
		File pflnk = new File(getSystemFolderPath(CSIDL_STARTUP, false), SHORTCUTNAME);
		return pflnk.exists();
	}
}
