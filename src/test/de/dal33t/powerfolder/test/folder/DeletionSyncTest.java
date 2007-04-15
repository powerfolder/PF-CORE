package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.Condition;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;

/**
 * Tests the correct synchronization of file deletions.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @version $Revision: 1.5 $
 */
public class DeletionSyncTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("DeletionSyncTest.setUp()");
        super.setUp();
        connectBartAndLisa();
        // Note: Don't make friends, SYNC_PC profile should sync even if PCs are
        // not friends.
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    /**
     * TRAC #394
     */
    public void testDeleteAndRestore() {
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        // Create a file with version = 1
        File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        TestHelper.changeFile(testFileBart);
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()[0],
            getContollerBart());
        assertEquals(1, getFolderAtBart().getKnownFiles()[0].getVersion());

        // Let Lisa download the file via auto-dl
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFiles()[0].getVersion());

        // Now delete the file @ bart. This should NOT be mirrored to Lisa! (She
        // has only auto-dl, no deletion sync)
        assertTrue(testFileBart.delete());
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()[0],
            getContollerBart());
        assertEquals(2, getFolderAtBart().getKnownFiles()[0].getVersion());

        // @ Lisa, still the "old" version (=1).
        assertEquals(1, getFolderAtLisa().getKnownFiles()[0].getVersion());

        // Now let Bart re-download the file! -> Manually triggerd
        getContollerBart().getTransferManager().downloadNewestVersion(
            getFolderAtBart().getKnownFiles()[0]);
        TestHelper.waitMilliSeconds(500);

        // Check the file.

        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()[0],
            getContollerBart());
        // TODO: Discuss: The downloaded version should be 3 (?).
        // Version 3 of the file = restored.
        assertEquals(1, getFolderAtBart().getKnownFiles()[0].getVersion());
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testSingleFileDeleteSync() {
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        FileInfo fInfoBart = getFolderAtBart().getKnownFiles()[0];

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles()[0];
        File testFileLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());

        assertTrue(fInfoLisa.isCompletelyIdentical(fInfoBart));
        assertEquals(testFileBart.length(), testFileLisa.length());

        // Now delete the file at lisa
        assertTrue(testFileLisa.delete());
        getFolderAtLisa().forceScanOnNextMaintenance();
        getFolderAtLisa().maintain();

        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        assertEquals(1, getFolderAtLisa().getKnownFiles()[0].getVersion());
        assertTrue(getFolderAtLisa().getKnownFiles()[0].isDeleted());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFiles()[0].isDeleted();
            }
        });

        assertEquals(1, getFolderAtBart().getKnownFilesCount());
        assertEquals(1, getFolderAtBart().getKnownFiles()[0].getVersion());
        assertTrue(getFolderAtBart().getKnownFiles()[0].isDeleted());

        // Assume only 1 file (=PowerFolder system dir)
        assertEquals(1, getFolderAtBart().getLocalBase().list().length);
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testMultipleFileDeleteSync() {
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        // Copy
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= nFiles;
            }
        });
        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());

        // Now delete the file at lisa
        FileInfo[] fInfosLisa = getFolderAtLisa().getKnownFiles();
        for (int i = 0; i < fInfosLisa.length; i++) {
            assertTrue(fInfosLisa[i].getDiskFile(
                getContollerLisa().getFolderRepository()).delete());
        }
        getFolderAtLisa().forceScanOnNextMaintenance();
        getFolderAtLisa().maintain();

        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());
        fInfosLisa = getFolderAtLisa().getKnownFiles();
        for (int i = 0; i < fInfosLisa.length; i++) {
            assertEquals(1, fInfosLisa[i].getVersion());
            assertTrue(fInfosLisa[i].isDeleted());
        }

        // Wait to sync the deletions
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFiles()[nFiles - 1]
                    .isDeleted();
            }
        });
        TestHelper.waitMilliSeconds(500);

        // Test the correct deletions state at bart
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());
        FileInfo[] fInfosBart = getFolderAtBart().getKnownFiles();
        for (int i = 0; i < fInfosBart.length; i++) {
            assertTrue(fInfosBart[i].isDeleted());
            assertEquals(1, fInfosBart[i].getVersion());
        }

        // Assume only 1 file (=PowerFolder system dir)
        assertEquals(1, getFolderAtBart().getLocalBase().list().length);

    }

    /**
     * Complex scenario to test the the correct deletion synchronization.
     * <p>
     * Related tickets: #9
     */
    public void testDeletionSyncScenario() {
        // file "host" and "client"
        getFolderAtBart().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File file1 = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "/TestFile.txt",
            "This are the contents of the testfile".getBytes());
        File file2 = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "/TestFile2.txt",
            "This are the contents  of the 2nd testfile".getBytes());
        File file3 = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "/sub/sub/TestFile3.txt",
            "This are the contents of the 3nd testfile".getBytes());

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        assertEquals(3, getFolderAtBart().getKnownFilesCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 3;
            }
        });

        // Test ;)
        assertEquals(3, getFolderAtLisa().getKnownFilesCount());

        // Version should be the 0 for new files
        for (FileInfo fileInfo : getFolderAtBart().getKnownFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // Version should be the 0 for new files
        for (FileInfo fileInfo : getFolderAtLisa().getKnownFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        assertEquals(getContollerBart().getRecycleBin().getSize(), 0);
        assertEquals(getContollerLisa().getRecycleBin().getSize(), 0);

        file1.delete();
        file2.delete();
        file3.delete();

        assertFalse(file1.exists());
        assertFalse(file2.exists());
        assertFalse(file3.exists());

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        // all 3 must be deleted
        FileInfo[] folder1Files = getFolderAtBart().getKnownFiles();
        for (FileInfo fileInfo : folder1Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
        }

        // Give them time to remote deletion
        TestHelper.waitMilliSeconds(3000);

        // all 3 must be deleted remote
        FileInfo[] folder2Files = getFolderAtLisa().getKnownFiles();
        for (FileInfo fileInfo : folder2Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
            File file = getFolderAtLisa().getDiskFile(fileInfo);
            assertFalse(file.exists());
        }
        assertEquals(getContollerLisa().getRecycleBin().getSize(), 3);

        // switch profiles
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        RecycleBin recycleBin = getContollerLisa().getRecycleBin();
        List<FileInfo> deletedFilesAtLisa = getContollerLisa().getRecycleBin()
            .getAllRecycledFiles();
        for (FileInfo deletedFileAtLisa : deletedFilesAtLisa) {
            recycleBin.restoreFromRecycleBin(deletedFileAtLisa);
        }

        // all 3 must not be deleted at folder2
        for (FileInfo fileAtLisa : getFolderAtLisa().getKnownFiles()) {
            assertFalse(fileAtLisa.isDeleted());
            assertEquals(2, fileAtLisa.getVersion());
            assertEquals(getContollerLisa().getMySelf().getInfo(), fileAtLisa
                .getModifiedBy());
            File file = getFolderAtLisa().getDiskFile(fileAtLisa);
            assertTrue(file.exists());
            assertEquals(fileAtLisa.getSize(), file.length());
            assertEquals(fileAtLisa.getModifiedDate().getTime(), file
                .lastModified());
        }

        getContollerBart().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Give them time to undelete sync (means downloading;)
        TestHelper.waitMilliSeconds(3000);

        // all 3 must not be deleted anymore at folder1
        for (FileInfo fileInfo : getFolderAtBart().getKnownFiles()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
            assertTrue(fileInfo.getDiskFile(
                getContollerBart().getFolderRepository()).exists());
        }

        for (FileInfo fileInfo : getFolderAtLisa().getKnownFiles()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
            assertTrue(fileInfo.getDiskFile(
                getContollerLisa().getFolderRepository()).exists());
        }
    }
}
