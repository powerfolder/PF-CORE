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
import java.util.Arrays;
import java.util.Collection;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Test the project work sync mode.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ProjectWorkSyncTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);
    }

    /**
     * Test the file detection on start. This is a bug and should not happen.
     * Ticket #200.
     */
    public void testDetectOnStart() {
        // Create some random files
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());

        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());

        getContollerBart().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(500);
        getContollerBart().getFolderRepository().triggerMaintenance();

        getContollerLisa().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(500);
        getContollerLisa().getFolderRepository().triggerMaintenance();

        // Should not be scanned
        assertEquals(0, getFolderAtBart().getKnownItemCount());
        assertEquals(0, getFolderAtLisa().getKnownItemCount());
    }

    /**
     * Test if the files are transferred after the sync was triggered manually
     */
    public void testReceiveFiles() {
        // Both should be friends
        makeFriends();

        // Create some random files (15 for bart, 2 for lisa)
        final int expectedFilesAtBart = 15;
        final int expectedFilesAtLisa = 2;
        for (int i = 0; i < expectedFilesAtBart; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
                "BartsTestFile" + i + ".xxx");
        }

        for (int i = 0; i < expectedFilesAtLisa; i++) {
            TestHelper.createRandomFile(getFolderAtLisa().getLocalBase(),
                "LisasTestFile" + i + ".xxx");
        }

        // Scan files on bart
        getFolderAtBart().setSyncProfile(SyncProfile.HOST_FILES);
        scanFolder(getFolderAtBart());

        assertEquals(expectedFilesAtBart, getFolderAtBart().getKnownItemCount());

        // List should still don't know any files
        assertEquals(0, getFolderAtLisa().getKnownItemCount());

        // Wait for filelist from bart
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getIncomingFiles().size() >= expectedFilesAtBart;
            }
        });

        // Now perform manual sync on lisa
        getContollerLisa().getFolderRepository().getFileRequestor()
            .requestMissingFiles(getFolderAtLisa(), false);

        // Copy
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() >= expectedFilesAtBart;
            }
        });

        // Both should have the files now
        assertEquals(expectedFilesAtBart, getFolderAtBart().getKnownItemCount());
        assertEquals(expectedFilesAtBart, getFolderAtLisa().getKnownItemCount());
    }

    /**
     * Test if the files are transferred after the sync was triggered manually
     */
    public void testReceiveDeletes() throws IOException {
        // Create some random files
        Path rndFile1 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        Path rndFile2 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());

        Path rndFile3 = TestHelper.createRandomFile(getFolderAtLisa()
            .getLocalBase());
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());

        // Both should be friends
        makeFriends();

        // Scan files
        getFolderAtBart().setSyncProfile(SyncProfile.HOST_FILES);
        getFolderAtLisa().setSyncProfile(SyncProfile.HOST_FILES);
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());
        assertEquals(3, getFolderAtBart().getKnownItemCount());
        assertEquals(2, getFolderAtLisa().getKnownItemCount());
        getFolderAtBart().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);

        // Wait for filelists
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getIncomingFiles().size() >= 3;
            }
        });
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getIncomingFiles().size() >= 2;
            }
        });

        // Now perform manual sync on lisa
        getContollerLisa().getFolderRepository().getFileRequestor()
            .requestMissingFiles(getFolderAtLisa(), false);
        getContollerBart().getFolderRepository().getFileRequestor()
            .requestMissingFiles(getFolderAtBart(), false);

        // Copy
        TestHelper.waitForCondition(25, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() >= 5;
            }
        });
        TestHelper.waitForCondition(25, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownItemCount() >= 5;
            }
        });

        // Both should have 5 files now
        assertEquals(5, getFolderAtBart().getKnownItemCount());
        assertEquals(5, getFolderAtLisa().getKnownItemCount());

        // Delete
        Files.delete(rndFile1);
        Files.delete(rndFile2);
        Files.delete(rndFile3);

        // Scan files

        getFolderAtBart().setSyncProfile(SyncProfile.HOST_FILES);
        scanFolder(getFolderAtBart());
        getFolderAtBart().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
        assertEquals(2, countDeleted(getFolderAtBart().getKnownFiles()));

        getFolderAtLisa().setSyncProfile(SyncProfile.HOST_FILES);
        scanFolder(getFolderAtLisa());
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
        assertEquals(1, countDeleted(getFolderAtLisa().getKnownFiles()));

        // Filelist transfer
        TestHelper.waitMilliSeconds(1000);

        // Now handle remote deletings
        getFolderAtLisa().syncRemoteDeletedFiles(true);
        getFolderAtBart().syncRemoteDeletedFiles(true);

        assertEquals(3, countDeleted(getFolderAtBart().getKnownFiles()));
        assertEquals(2, countExisting(getFolderAtBart().getKnownFiles()));
        assertEquals(3, countDeleted(getFolderAtLisa().getKnownFiles()));
        assertEquals(2, countExisting(getFolderAtLisa().getKnownFiles()));
        // Check deleted files.
        // Directory should contain onyl 2 files (+2 = system dir)
        assertEquals("Files at lisa: "
            + Arrays.asList(getFolderAtLisa().getLocalBase().toFile().list()), 2 + 1,
            getFolderAtLisa().getLocalBase().toFile().list().length);
        assertEquals("File at bart: "
            + Arrays.asList(getFolderAtBart().getLocalBase().toFile().list()), 2 + 1,
            getFolderAtBart().getLocalBase().toFile().list().length);
    }

    private int countDeleted(Collection<FileInfo> files) {
        int deleted = 0;
        for (FileInfo info : files) {
            if (info.isDeleted()) {
                deleted++;
            }
        }
        return deleted;
    }

    private int countExisting(Collection<FileInfo> files) {
        int existing = 0;
        for (FileInfo info : files) {
            if (!info.isDeleted()) {
                existing++;
            }
        }
        return existing;
    }
}
