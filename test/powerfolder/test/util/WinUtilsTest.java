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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.ShellLink;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Note: You need desktoputils.dll in the test classpath for this to work.
 */
public class WinUtilsTest extends TestCase {
    public void testSystemFolders() {
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        WinUtils wu = WinUtils.getInstance();
        assertNotNull(
            "Could not get instance. Is desktoputils.dll in the classpath?", wu);
        assertNotNull(wu.getSystemFolderPath(WinUtils.CSIDL_STARTUP, false));
        assertNotNull(wu.getSystemFolderPath(WinUtils.CSIDL_PERSONAL, false));
        assertNotNull(WinUtils.getAppDataAllUsers());
        assertNotNull(WinUtils.getAppDataCurrentUser());
        assertNotNull(WinUtils.getProgramInstallationPath());
    }

    public void testLinkCreation() throws IOException {
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        ShellLink sl = new ShellLink("test1 test2", "Link creation test",
            "Dummy", null);
        WinUtils wu = WinUtils.getInstance();
        assertNotNull(
            "Could not get instance. Is desktoputils.dll in the classpath?", wu);
        Path f = TestHelper.getTestDir().resolve("test.lnk");
        Files.createDirectories(f.getParent());
        wu.createLink(sl, f.toAbsolutePath().toString());
        assertTrue(Files.exists(f));
        Files.delete(f);
        wu.createLink(sl, f.toAbsolutePath().toString());
        assertTrue(Files.exists(f));
        Files.delete(f);
    }

    public void testGetAllUserAppData() {
        String appData = WinUtils.getAppDataAllUsers();
        if (OSUtil.isWindows7System() || OSUtil.isWindowsVistaSystem()) {
            assertEquals("C:\\ProgramData", appData);
        } else if (OSUtil.isWindowsSystem()) {
            assertEquals("C:\\Dokumente und Einstellungen\\All Users", appData);
        }
        System.out.println(appData);
    }
}
