package de.dal33t.powerfolder.util.os.Win32;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Utilities for windows.
 * 
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 */
public class WinUtils {
    private static Logger LOG = Logger.getLogger(WinUtils.class);

    public final static String SHORTCUTNAME = "PowerFolder.lnk";
    
	/** The file system directory that contains the programs that appear in the Startup folder for all users. A typical path is C:\Documents and Settings\All Users\Start Menu\Programs\Startup. Valid only for Windows NT systems. */
	public final static int CSIDL_COMMON_STARTUP = 0x0018;

	/** The file system directory that corresponds to the user's Startup program group. The system starts these programs whenever any user logs onto Windows NT or starts Windows 95. A typical path is C:\Documents and Settings\\username\\Start Menu\\Programs\\Startup. */
	public final static int CSIDL_STARTUP = 0x0007;

	private static WinUtils instance;
	
	private WinUtils() {
	}
	
	public static synchronized WinUtils getInstance() {
		if (instance == null) {
            LOG.verbose("Loading library: winutils.dll");
			System.loadLibrary("winutils");
			instance = new WinUtils();
			instance.init();
		}
		return instance;
	}
	
	/**
	 * Retrieve a path from Windows.
	 * @param id the path-id to retrieve
	 * @param defaultPath if true return the default path location instead of the current
	 * @return
	 */
	public native String getSystemFolderPath(int id, boolean defaultPath);
	public native void createLink(ShellLink link, String lnkTarget) throws IOException;
	private native void init();

	public void setPFStartup(boolean setup) throws IOException {
		File pfile = new File( 
			new File(System.getProperty("java.class.path")).getParentFile(),
			"PowerFolder.exe");
		if (!pfile.exists()) {
			LOG.error("Couldn't find PowerFolder executable! " 
					+ "Note: Setting up a shortcut only works "
					+ "when PowerFolder was started by PowerFolder.exe");
			return;
		}
		LOG.verbose("Found " + pfile.getAbsolutePath());
		File pflnk = new File(getSystemFolderPath(CSIDL_STARTUP, false), SHORTCUTNAME);
		if (!setup) {
			LOG.verbose("Deleting startup link.");
			pflnk.delete();
		} else {
			ShellLink sl = new ShellLink();
			sl.path = pfile.getAbsolutePath();
			sl.workdir = pfile.getParent();
			sl.arguments = "";
			sl.description = Translation
				.getTranslation("winutils.shortcut.description");
			LOG.verbose("Creating startup link: " + pflnk.getAbsolutePath());
			createLink(sl, pflnk.getAbsolutePath());
		}
	}

	public boolean isPFStartup() {
		File pflnk = new File(getSystemFolderPath(CSIDL_STARTUP, false), SHORTCUTNAME);
		return pflnk.exists();
	}
}
