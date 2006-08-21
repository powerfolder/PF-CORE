/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.UUID;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
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
    private static final String BASEDIR1 = "build/test/controllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/controllerLisa/testFolder";

    private Folder folderBart;
    private Folder folderLisa;

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("FileTransferTest.setUp()");
        super.setUp();

        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);
        joinFolder(testFolder, new File(BASEDIR1), new File(BASEDIR2),
            SyncProfile.PROJECT_WORK);
        folderBart = getContollerBart().getFolderRepository().getFolder(
            testFolder);
        folderLisa = getContollerLisa().getFolderRepository().getFolder(
            testFolder);
    }

    /**
     * Test the file detection on start. This is a bug and should not happen.
     * Ticket #200.
     */
    public void testDetectOnStart() {

        // Create some random files
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());

        TestHelper.createRandomFile(folderLisa.getLocalBase());
        TestHelper.createRandomFile(folderLisa.getLocalBase());

        getContollerBart().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(500);
        getContollerBart().getFolderRepository().triggerMaintenance();

        getContollerLisa().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(500);
        getContollerLisa().getFolderRepository().triggerMaintenance();

        // Should not be scanned
        assertEquals(0, folderBart.getFilesCount());
        assertEquals(0, folderLisa.getFilesCount());
    }

    /**
     * Test if the files are transferred after the sync was triggered manually
     */
    public void testReceiveFiles() {
        // Create some random files (15 for bart, 2 for lisa)
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());

        TestHelper.createRandomFile(folderLisa.getLocalBase());
        TestHelper.createRandomFile(folderLisa.getLocalBase());

        // Both should be friends
        makeFriends();

        // Scan files on bart
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        
        assertEquals(15, folderBart.getFilesCount());

        // List should still don't know any files
        assertEquals(0, folderLisa.getFilesCount());

        // Wait for filelist from bart
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return folderLisa.getExpecedFiles(false).length >= 15;
            }
        });

        // Now perform manual sync on lisa
        getContollerLisa().getFolderRepository().getFileRequestor()
            .requestMissingFiles(folderLisa, true, false, false);

        // Copy
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return folderLisa.getFilesCount() >= 15;
            }
        });

        // Both should have the files now
        assertEquals(15, folderBart.getFilesCount());
        assertEquals(15, folderLisa.getFilesCount());
    }

    /**
     * Test if the files are transferred after the sync was triggered manually
     */
    public void testReceiveDeletes() {
        // Create some random files
        File rndFile1 = TestHelper.createRandomFile(folderBart.getLocalBase());
        File rndFile2 = TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());

        File rndFile3 = TestHelper.createRandomFile(folderLisa.getLocalBase());
        TestHelper.createRandomFile(folderLisa.getLocalBase());

        // Both should be friends
        makeFriends();

        // Scan files
        
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        folderLisa.forceScanOnNextMaintenance();
        folderLisa.maintain();
        
        assertEquals(3, folderBart.getFilesCount());
        assertEquals(2, folderLisa.getFilesCount());

        // Wait for filelists
        TestHelper.waitForCondition(2, new Condition() {
            public boolean reached() {
                return folderLisa.getExpecedFiles(false).length >= 3;
            }
        });
        TestHelper.waitForCondition(2, new Condition() {
            public boolean reached() {
                return folderBart.getExpecedFiles(false).length >= 2;
            }
        });

        // Now perform manual sync on lisa
        getContollerLisa().getFolderRepository().getFileRequestor()
            .requestMissingFiles(folderLisa, true, false, false);
        getContollerBart().getFolderRepository().getFileRequestor()
            .requestMissingFiles(folderBart, true, false, false);

        // Copy
        TestHelper.waitForCondition(25, new Condition() {
            public boolean reached() {
                return folderLisa.getFilesCount() >= 5;
            }
        });
        TestHelper.waitForCondition(25, new Condition() {
            public boolean reached() {
                return folderBart.getFilesCount() >= 5;
            }
        });

        // Both should have 5 files now
        assertEquals(5, folderBart.getFilesCount());
        assertEquals(5, folderLisa.getFilesCount());

        // Delete
        assertTrue(rndFile1.delete());
        assertTrue(rndFile2.delete());
        assertTrue(rndFile3.delete());

        // Scan files
        
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();        
        assertEquals(2, countDeleted(folderBart.getFiles()));
        
        folderLisa.forceScanOnNextMaintenance();
        folderLisa.maintain();        
        assertEquals(1, countDeleted(folderLisa.getFiles()));

        // Filelist transfer
        TestHelper.waitMilliSeconds(1000);

        // Now handle remote deletings
        folderLisa.handleRemoteDeletedFiles(true);
        folderBart.handleRemoteDeletedFiles(true);

        assertEquals(3, countDeleted(folderBart.getFiles()));
        assertEquals(2, countExisting(folderBart.getFiles()));
        assertEquals(3, countDeleted(folderLisa.getFiles()));
        assertEquals(2, countExisting(folderLisa.getFiles()));
        // Check deleted files.
        // Directory should contain onyl 2 files (+2 = system dir)
        assertEquals(2 + 1, folderLisa.getLocalBase().list().length);
        assertEquals(2 + 1, folderBart.getLocalBase().list().length);
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
