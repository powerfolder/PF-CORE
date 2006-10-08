/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper.Condition;

/**
 * Test the project work sync mode.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ProjectWorkSyncTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        joinTestFolder(SyncProfile.PROJECT_WORK);
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
        assertEquals(0, getFolderAtBart().getFilesCount());
        assertEquals(0, getFolderAtLisa().getFilesCount());
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
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        assertEquals(expectedFilesAtBart, getFolderAtBart().getFilesCount());

        // List should still don't know any files
        assertEquals(0, getFolderAtLisa().getFilesCount());

        // Wait for filelist from bart
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getIncomingFiles(false).size() >= expectedFilesAtBart;
            }
        });

        // Now perform manual sync on lisa
        getContollerLisa().getFolderRepository().getFileRequestor()
            .requestMissingFiles(getFolderAtLisa(), true, false, false);

        // Copy
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getFilesCount() >= expectedFilesAtBart;
            }
        });

        // Both should have the files now
        assertEquals(expectedFilesAtBart, getFolderAtBart().getFilesCount());
        assertEquals(expectedFilesAtBart, getFolderAtLisa().getFilesCount());
    }

    /**
     * Test if the files are transferred after the sync was triggered manually
     */
    public void testReceiveDeletes() {
        // Create some random files
        File rndFile1 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        File rndFile2 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());

        File rndFile3 = TestHelper.createRandomFile(getFolderAtLisa()
            .getLocalBase());
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());

        // Both should be friends
        makeFriends();

        // Scan files

        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        getFolderAtLisa().forceScanOnNextMaintenance();
        getFolderAtLisa().maintain();

        assertEquals(3, getFolderAtBart().getFilesCount());
        assertEquals(2, getFolderAtLisa().getFilesCount());

        // Wait for filelists
        TestHelper.waitForCondition(2, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getIncomingFiles(false).size() >= 3;
            }
        });
        TestHelper.waitForCondition(2, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getIncomingFiles(false).size() >= 2;
            }
        });

        // Now perform manual sync on lisa
        getContollerLisa().getFolderRepository().getFileRequestor()
            .requestMissingFiles(getFolderAtLisa(), true, false, false);
        getContollerBart().getFolderRepository().getFileRequestor()
            .requestMissingFiles(getFolderAtBart(), true, false, false);

        // Copy
        TestHelper.waitForCondition(25, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getFilesCount() >= 5;
            }
        });
        TestHelper.waitForCondition(25, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getFilesCount() >= 5;
            }
        });

        // Both should have 5 files now
        assertEquals(5, getFolderAtBart().getFilesCount());
        assertEquals(5, getFolderAtLisa().getFilesCount());

        // Delete
        assertTrue(rndFile1.delete());
        assertTrue(rndFile2.delete());
        assertTrue(rndFile3.delete());

        // Scan files

        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        assertEquals(2, countDeleted(getFolderAtBart().getFiles()));

        getFolderAtLisa().forceScanOnNextMaintenance();
        getFolderAtLisa().maintain();
        assertEquals(1, countDeleted(getFolderAtLisa().getFiles()));

        // Filelist transfer
        TestHelper.waitMilliSeconds(1000);

        // Now handle remote deletings
        getFolderAtLisa().handleRemoteDeletedFiles(true);
        getFolderAtBart().handleRemoteDeletedFiles(true);

        assertEquals(3, countDeleted(getFolderAtBart().getFiles()));
        assertEquals(2, countExisting(getFolderAtBart().getFiles()));
        assertEquals(3, countDeleted(getFolderAtLisa().getFiles()));
        assertEquals(2, countExisting(getFolderAtLisa().getFiles()));
        // Check deleted files.
        // Directory should contain onyl 2 files (+2 = system dir)
        assertEquals(2 + 1, getFolderAtLisa().getLocalBase().list().length);
        assertEquals(2 + 1, getFolderAtBart().getLocalBase().list().length);
    }

    private int countDeleted(FileInfo[] files) {
        int deleted = 0;
        for (FileInfo info : files) {
            if (info.isDeleted()) {
                deleted++;
            }
        }
        return deleted;
    }

    private int countExisting(FileInfo[] files) {
        int existing = 0;
        for (FileInfo info : files) {
            if (!info.isDeleted()) {
                existing++;
            }
        }
        return existing;
    }
}
