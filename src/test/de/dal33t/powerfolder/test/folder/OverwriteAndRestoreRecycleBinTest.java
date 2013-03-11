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

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * TODO ADD JAVADOC
 */
public class OverwriteAndRestoreRecycleBinTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        makeFriends();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    public void xtestOverwriteToRecycleAndRestoreMultiple() throws Exception {
        for (int i = 0; i < 100; i++) {
            testOverwriteToRecycleAndRestore();
            tearDown();
            setUp();
        }
    }

    /**
     * Test the overwrite of file (due to sync) results in copy of old one in
     * RecycleBin. After that the file is restored.
     * 
     * @throws IOException
     */
    public void testOverwriteToRecycleAndRestore() throws IOException {
        final File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());

        scanFolder(getFolderAtBart());

        FileInfo fInfoBart = getFolderAtBart().getKnownFiles().iterator()
            .next();

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() == 1;
            }
        });
        FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        final File testFileLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());

        assertTrue(fInfoLisa.isVersionDateAndSizeIdentical(fInfoBart));
        assertEquals(testFileBart.length(), testFileLisa.length());

        TestHelper.waitMilliSeconds(2500);
        // overwrite file at Bart
        TestHelper.createTestFile(getFolderAtBart().getLocalBase(),
            testFileBart.getName(), new byte[]{6, 5, 6, 7});
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles()
                    .iterator().next();
                FileInfo fInfoBart = getFolderAtBart().getKnownFiles()
                    .iterator().next();
                return fInfoLisa.isVersionDateAndSizeIdentical(fInfoBart)
                    && (testFileBart.length() == testFileLisa.length());
            }

            public String message() {
                FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles()
                    .iterator().next();
                FileInfo fInfoBart = getFolderAtBart().getKnownFiles()
                    .iterator().next();
                return "At Lisa: " + fInfoLisa.toDetailString() + " At Bart: "
                    + fInfoBart.toDetailString();
            }
        });
        assertEquals(4, getFolderAtLisa().getKnownFiles().iterator().next()
            .getSize());
        assertEquals(4, getFolderAtBart().getKnownFiles().iterator().next()
            .getSize());

        CopyOrMoveFileArchiver archiveAtLisa = getFolderAtLisa().getFileArchiver();
        assertEquals(1, archiveAtLisa.getArchivedFilesInfos(fInfoLisa).size());
        FileInfo infoAtLisa = archiveAtLisa.getArchivedFilesInfos(fInfoLisa)
            .get(0);

        // Restore
        archiveAtLisa.restore(infoAtLisa,
            infoAtLisa.getDiskFile(getContollerLisa().getFolderRepository()));

        scanFolder(getFolderAtLisa());
        // File should be still in archive
        assertEquals(1, archiveAtLisa.getArchivedFilesInfos(fInfoLisa).size());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles()
                    .iterator().next();
                FileInfo fInfoBart = getFolderAtBart().getKnownFiles()
                    .iterator().next();
                return fInfoLisa.isVersionDateAndSizeIdentical(fInfoBart)
                    && (testFileBart.length() == testFileLisa.length());
            }

            public String message() {
                FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles()
                    .iterator().next();
                FileInfo fInfoBart = getFolderAtBart().getKnownFiles()
                    .iterator().next();
                return "Bart's file: " + fInfoBart.toDetailString()
                    + ", Lisa's file: " + fInfoLisa.toDetailString();
            }
        });
    }
}
