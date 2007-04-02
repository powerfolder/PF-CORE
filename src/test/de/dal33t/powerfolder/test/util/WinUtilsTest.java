package de.dal33t.powerfolder.test.util;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.util.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.ShellLink;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

public class WinUtilsTest extends TestCase {
	public void testSystemFolders() {
		if (!OSUtil.isWindowsSystem())
			return;
		WinUtils wu = WinUtils.getInstance();
		assertNotNull(wu
				.getSystemFolderPath(WinUtils.CSIDL_STARTUP, false));
	}
	
	public void testLinkCreation() {
		if (!OSUtil.isWindowsSystem())
			return;
		ShellLink sl = new ShellLink();
		sl.path = "Dummy";
		sl.arguments = "test1 test2";
		sl.description = "Link creation test";
		WinUtils wu = WinUtils.getInstance();
		File f = new File(TestHelper.getTestDir(), "test.lnk");
		try {
			f.getParentFile().mkdirs();
			wu.createLink(sl, f.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		assertTrue(f.exists());
		f.delete();
	}
}
