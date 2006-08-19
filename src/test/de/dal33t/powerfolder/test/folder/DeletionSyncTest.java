package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper.Condition;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests the correct synchronization of file deletions.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @version $Revision: 1.5 $
 */
public class DeletionSyncTest extends TwoControllerTestCase {
    private static final String BASEDIR1 = "build/test/controllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/controllerLisa/testFolder";
    private Folder folderAtBart;
    private Folder folderAtLisa;

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("DeletionSyncTest.setUp()");
        super.setUp();
        makeFriends();
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        joinFolder(testFolder, new File(BASEDIR1), new File(BASEDIR2));
        folderAtBart = getContollerBart().getFolderRepository().getFolder(
            testFolder);
        folderAtLisa = getContollerLisa().getFolderRepository().getFolder(
            testFolder);
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testSingleFileDeleteSync() {
        folderAtBart.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        folderAtLisa.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File testFileBart = TestHelper.createRandomFile(folderAtBart
            .getLocalBase());
        folderAtBart.scanLocalFiles(true);
        TestHelper.waitMilliSeconds(2000);
        FileInfo fInfoBart = folderAtBart.getFiles()[0];

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return folderAtLisa.getFilesCount() >= 1;
            }
        });
        assertEquals(1, folderAtLisa.getFilesCount());
        FileInfo fInfoLisa = folderAtLisa.getFiles()[0];
        File testFileLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());

        assertTrue(fInfoLisa.completelyIdentical(fInfoBart));
        assertEquals(testFileBart.length(), testFileLisa.length());

        // Now delete the file at lisa
        assertTrue(testFileLisa.delete());
        folderAtLisa.scanLocalFiles(true);
        TestHelper.waitMilliSeconds(1000);
        
        assertEquals(1, folderAtLisa.getFilesCount());
        assertEquals(1, folderAtLisa.getFiles()[0].getVersion());
        assertTrue(folderAtLisa.getFiles()[0].isDeleted());

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return folderAtBart.getFiles()[0].isDeleted();
            }
        });

        assertEquals(1, folderAtBart.getFilesCount());
        assertEquals(1, folderAtBart.getFiles()[0].getVersion());
        assertTrue(folderAtBart.getFiles()[0].isDeleted());
        
        // Assume only 1 file (=PowerFolder system dir)
        assertEquals(1, folderAtBart.getLocalBase().list().length);
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testMultipleFileDeleteSync() {
        folderAtBart.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        folderAtLisa.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(folderAtBart.getLocalBase());
        }
        folderAtBart.scanLocalFiles(true);
        TestHelper.waitMilliSeconds(1000);
        TestHelper.waitForCondition(20, new TestHelper.Condition() {
            public boolean reached() {
                return folderAtLisa.getFilesCount() >= nFiles;
            }
        });
        assertEquals(nFiles, folderAtLisa.getFilesCount());

        // Now delete the file at lisa
        FileInfo[] fInfosLisa = folderAtLisa.getFiles();
        for (int i = 0; i < fInfosLisa.length; i++) {
            assertTrue(fInfosLisa[i].getDiskFile(
                getContollerLisa().getFolderRepository()).delete());
        }

        folderAtLisa.scanLocalFiles(true);
        TestHelper.waitMilliSeconds(1000);
        assertEquals(nFiles, folderAtLisa.getFilesCount());
        fInfosLisa = folderAtLisa.getFiles();
        for (int i = 0; i < fInfosLisa.length; i++) {
            assertEquals(1, fInfosLisa[i].getVersion());
            assertTrue(fInfosLisa[i].isDeleted());
        }

        // Wait to sync the deletions
        TestHelper.waitForCondition(20, new TestHelper.Condition() {
            public boolean reached() {
                return folderAtBart.getFiles()[nFiles - 1].isDeleted();
            }
        });
        TestHelper.waitMilliSeconds(500);

        // Test the correct deletions state at bart
        assertEquals(nFiles, folderAtBart.getFilesCount());
        FileInfo[] fInfosBart = folderAtBart.getFiles();
        for (int i = 0; i < fInfosBart.length; i++) {
            assertTrue(fInfosBart[i].isDeleted());
            assertEquals(1, fInfosBart[i].getVersion());
        }

        // Assume only 1 file (=PowerFolder system dir)
        assertEquals(1, folderAtBart.getLocalBase().list().length);
    }

    /**
     * Complex scenario to test the the correct deletion synchronization.
     * <p>
     * Related tickets: #9
     */
    public void testDeletionSyncScenario() {
        // file "host" and "client"
        folderAtBart.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        folderAtLisa.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File file1 = TestHelper
            .createTestFile(folderAtBart.getLocalBase(), "/TestFile.txt",
                "This are the contents of the testfile".getBytes());
        File file2 = TestHelper.createTestFile(folderAtBart.getLocalBase(),
            "/TestFile2.txt", "This are the contents  of the 2nd testfile"
                .getBytes());
        File file3 = TestHelper.createTestFile(folderAtBart.getLocalBase(),
            "/sub/sub/TestFile3.txt",
            "This are the contents of the 3nd testfile".getBytes());

        // Let him scan the new content
        folderAtBart.scanLocalFiles(true);
        TestHelper.waitMilliSeconds(1000);
        assertEquals(3, folderAtBart.getFilesCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderAtLisa.getFilesCount() >= 3;
            }
        });

        // Test ;)
        assertEquals(3, folderAtLisa.getFilesCount());

        // Version should be the 0 for new files
        for (FileInfo fileInfo : folderAtBart.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // Version should be the 0 for new files
        for (FileInfo fileInfo : folderAtLisa.getFiles()) {
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
        folderAtBart.scanLocalFiles(true);
        TestHelper.waitMilliSeconds(1000);
        // all 3 must be deleted
        FileInfo[] folder1Files = folderAtBart.getFiles();
        for (FileInfo fileInfo : folder1Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
        }

        // Give them time to remote deletion
        TestHelper.waitMilliSeconds(3000);

        // all 3 must be deleted remote
        FileInfo[] folder2Files = folderAtLisa.getFiles();
        for (FileInfo fileInfo : folder2Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
            File file = folderAtLisa.getDiskFile(fileInfo);
            assertFalse(file.exists());
        }
        assertEquals(getContollerLisa().getRecycleBin().getSize(), 3);

        // switch profiles
        folderAtLisa.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        folderAtBart.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        RecycleBin recycleBin = getContollerLisa().getRecycleBin();
        List<FileInfo> deletedFilesAtLisa = getContollerLisa().getRecycleBin()
            .getAllRecycledFiles();
        for (FileInfo deletedFileAtLisa : deletedFilesAtLisa) {
            recycleBin.restoreFromRecycleBin(deletedFileAtLisa);
        }

        // all 3 must not be deleted at folder2
        for (FileInfo fileAtLisa : folderAtLisa.getFiles()) {
            assertFalse(fileAtLisa.isDeleted());
            assertEquals(2, fileAtLisa.getVersion());
            assertEquals(getContollerLisa().getMySelf().getInfo(), fileAtLisa
                .getModifiedBy());
            File file = folderAtLisa.getDiskFile(fileAtLisa);
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
        for (FileInfo fileInfo : folderAtBart.getFiles()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
            assertTrue(fileInfo.getDiskFile(
                getContollerBart().getFolderRepository()).exists());
        }

        // Version should be the same (file did not change, it was only deleted
        // and restored!)
        for (FileInfo fileInfo : folderAtLisa.getFiles()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
            assertTrue(fileInfo.getDiskFile(
                getContollerLisa().getFolderRepository()).exists());
        }
    }

}
