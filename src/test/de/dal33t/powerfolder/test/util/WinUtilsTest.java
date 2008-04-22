package de.dal33t.powerfolder.test.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.ShellLink;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.test.TestHelper;

public class WinUtilsTest extends TestCase {
	public void testSystemFolders() {
		if (!OSUtil.isWindowsSystem())
			return;
		WinUtils wu = WinUtils.getInstance();
		assertNotNull(wu
				.getSystemFolderPath(WinUtils.CSIDL_STARTUP, false));
		assertNotNull(wu
		    .getSystemFolderPath(WinUtils.CSIDL_PERSONAL, false));
	}
	
	public void testLinkCreation() throws IOException {
		if (!OSUtil.isWindowsSystem())
			return;
		ShellLink sl = new ShellLink("test1 test2", "Link creation test", "Dummy", null);
		WinUtils wu = null;
        try {
            wu = WinUtils.getInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }
		File f = new File(TestHelper.getTestDir(), "test.lnk");
		f.getParentFile().mkdirs();
		wu.createLink(sl, f.getAbsolutePath());
		assertTrue(f.exists());
		f.delete();
        wu.createLink(sl, f.getAbsolutePath());
        assertTrue(f.exists());
        f.delete();
	}
}
