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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the finding of same files.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FindSameFilesTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);
    }

    /**
     * Tests the adapting of the filelist from remote. Should find the same file
     * from remote peer.
     *
     * @throws IOException
     */
    public void testFilelistAdapt() throws IOException {
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);

        Path testFile = TestHelper
            .createRandomFile(getFolderAtBart().getLocalBase(), "TestFile.txt");
        scanFolder(getFolderAtBart());
        // File should be found. version: 0
        assertEquals(
            "Expected version at bart: 0. Got: " + getFolderAtBart()
                .getKnownFiles().iterator().next().toDetailString(),
            0,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());
        TestHelper.changeFile(testFile);
        scanFolder(getFolderAtBart());
        // File changed. version: 1
        assertEquals(
            "Expected version at bart: 1. Got: " + getFolderAtBart()
                .getKnownFiles().iterator().next().toDetailString(),
            1,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());
        TestHelper.changeFile(testFile);
        scanFolder(getFolderAtBart());
        // File changed. version: 2
        assertEquals(
            "Expected version at bart: 2. Got: " + getFolderAtBart()
                .getKnownFiles().iterator().next().toDetailString(),
            2,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());
        TestHelper.changeFile(testFile);
        scanFolder(getFolderAtBart());
        // File changed. version: 3
        assertEquals(
            "Expected version at bart: 3. Got: " + getFolderAtBart()
                .getKnownFiles().iterator().next().toDetailString(),
            3,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());

        // File gets copied to lisa.
        Path testFileCopy = getFolderAtLisa().getLocalBase().resolve(
            "TestFile.txt");
        Files.copy(testFile, testFileCopy);

        // somehow the copie process is not complete sometimes what results in
        // different filesizes!
        TestHelper.waitMilliSeconds(1000);

        // Let lisa scan it.
        scanFolder(getFolderAtLisa());

        // List should have detected the file from bart as the same!
        assertEquals(0, getFolderAtLisa().getIncomingFiles().size());

        // File modifications should be adapted from Bart, because same file!
        assertEquals(
            "Expected version at bart: 3. Got: " + getFolderAtBart()
                .getKnownFiles().iterator().next().toDetailString(),
            3,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());
        assertEquals(
            "Expected version at lisa: 3. Got: " + getFolderAtLisa()
                .getKnownFiles().iterator().next().toDetailString(),
            3,
            getFolderAtLisa().getKnownFiles().iterator().next().getVersion());
        assertEquals(getContollerBart().getMySelf().getInfo(), getFolderAtLisa()
            .getKnownFiles().iterator().next().getModifiedBy());
    }

}
