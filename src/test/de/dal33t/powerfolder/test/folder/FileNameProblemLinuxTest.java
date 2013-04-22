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
package de.dal33t.powerfolder.test.folder;

import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * this test only runs on linux, since you cannot create files with these names
 * on windows.
 */
public class FileNameProblemLinuxTest extends ControllerTestCase {

    protected void setUp() throws Exception {
        if (OSUtil.isLinux()) {
            System.out.println("running linux specific Filename problem test");
            super.setUp();

            setupTestFolder(SyncProfile.HOST_FILES);

        }
    }

    /**
     * this test only runs on linux, since you cannot create files with these
     * names on windows.
     */
    public void testFindProblems() {
        if (OSUtil.isLinux()) {

            // not valid on windows (1)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "AUX");
            // not valid on windows (2)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "AUX.txt");
            // not valid on windows (3)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "LPT1");
            // valid on windows
            TestHelper.createRandomFile(getFolder().getLocalBase(), "xLPT1");
            // valid on windows
            TestHelper.createRandomFile(getFolder().getLocalBase(), "xAUX.txt");
            // not valid on windows, but this results in a directory 'test' with
            // file 'test' in it

            // TestHelper.createRandomFile(folder.getLocalBase(), "test/test");
            // not valid on windows (4)

            // our test fails on this file, because we regard a \ a directory
            // symbol
            // TestHelper.createRandomFile(folder.getLocalBase(),
            // "part1\\part2");
            // not valid on windows (4)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "?hhh");
            // not valid on windows (5)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "ddfgd*");
            // not valid on windows (6)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "<hhf");
            // not valid on windows (7)
            TestHelper
                .createRandomFile(getFolder().getLocalBase(), "hj\"gfgfg");
            // not valid on windows (8)
            TestHelper.createRandomFile(getFolder().getLocalBase(), ":sds");
            // not valid on windows (9)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "gfgf>");
            // not valid on windows (10)
            TestHelper.createRandomFile(getFolder().getLocalBase(), "gfgf<");

            if (true) {
                System.err.println("FIXME, DISABLED");
                return;
            }

            scanFolder(getFolder());
            // ScanResult result = folderScanner.syncFolder(getFolder());
            // assertEquals(12, result.getNewFiles().size());

            int handlerCalledCount = 0;
            assertEquals(1, handlerCalledCount);
            Path folderBaseDir = getFolder().getLocalBase();

          //  assertTrue("Files in dir: " + Arrays.asList(folderBaseDir.list()), false);
            assertTrue(Files.exists(folderBaseDir.resolve("AUX-1")));
            assertTrue(Files.exists(folderBaseDir.resolve("AUX-1.txt")));
            assertTrue(Files.exists(folderBaseDir.resolve("LPT1-1")));
            assertTrue(Files.exists(folderBaseDir.resolve("xLPT1")));
            assertTrue(Files.exists(folderBaseDir.resolve("xAUX.txt")));
            assertTrue(Files.exists(folderBaseDir.resolve("hhh")));
            assertTrue(Files.exists(folderBaseDir.resolve("ddfgd")));
            assertTrue(Files.exists(folderBaseDir.resolve("hhf")));
            assertTrue(Files.exists(folderBaseDir.resolve("hjgfgfg")));
            assertTrue(Files.exists(folderBaseDir.resolve("sds")));
            assertTrue(Files.exists(folderBaseDir.resolve("gfgf")));
            assertTrue(Files.exists(folderBaseDir.resolve("gfgf")));
        }
    }

    protected void tearDown() throws Exception {
        if (OSUtil.isLinux()) {
            super.tearDown();
        }
    }
}
