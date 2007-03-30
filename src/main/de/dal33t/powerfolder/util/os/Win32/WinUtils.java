package de.dal33t.powerfolder.util.os.Win32;

import java.io.File;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Util;

/**
 * Utilities for windows.
 * 
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 */
public class WinUtils {
    private static Logger LOG = Logger.getLogger(WinUtils.class);

	/** The file system directory that contains the programs that appear in the Startup folder for all users. A typical path is C:\Documents and Settings\All Users\Start Menu\Programs\Startup. Valid only for Windows NT systems. */
	public final static int CSIDL_COMMON_STARTUP = 0x0018;

	/** The file system directory that corresponds to the user's Startup program group. The system starts these programs whenever any user logs onto Windows NT or starts Windows 95. A typical path is C:\Documents and Settings\\username\\Start Menu\\Programs\\Startup. */
	public final static int CSIDL_STARTUP = 0x0007;

	private static WinUtils instance;
	
	private WinUtils() {
	}
	
	public static WinUtils getInstance() {
		if (instance == null) {
            // FIXME Do not copy libraries to local execution directory
            Util.copyResourceTo("winutils.dll",
                "de/dal33t/powerfolder/util/os/Win32", new File("."), true);            
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
	public native void createLink(ShellLink link, String lnkTarget);
	private native void init();
}
