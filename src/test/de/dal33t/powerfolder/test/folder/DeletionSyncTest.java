package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;
//import de.dal33t.powerfolder.util.Logger;

public class DeletionSyncTest extends TwoControllerTestCase {

    private static final String BASEDIR1 = "build/test/controller1/testFolder";
    private static final String BASEDIR2 = "build/test/controller2/testFolder";
    //private static final Logger LOG = Logger.getLogger(DeletionSyncTest.class);
    private Folder folder1;
    private Folder folder2;

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("DeletionSyncTest.setUp()");
        super.setUp();
        makeFriends();
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folder1 = getContoller1().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR1));

        folder2 = getContoller2().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR2));

        // Give them time to join        
            Thread.sleep(500);        

        checkFolderJoined();
    }

    /**
     * Helper to check that controllers have join all folders
     */
    private void checkFolderJoined() {
        assertEquals(2, folder1.getMembersCount());
        assertEquals(2, folder2.getMembersCount());
    }

    private static File createTestFile(Folder folder, String filename,
        byte[] contents) throws IOException
    {
        File file = new File(folder.getLocalBase().getAbsoluteFile(), filename);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        // System.out.println(file.getAbsolutePath());

        FileOutputStream fOut = new FileOutputStream(file);
        fOut.write(contents);
        fOut.close();

        return file;
    }

    public void testDeletionSync() throws IOException, InterruptedException {
        // file "host" and "client"
        folder1.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        folder2.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File file1 = createTestFile(folder1, "/TestFile.txt",
            "This are the contents of the testfile".getBytes());
        File file2 = createTestFile(folder1, "/TestFile2.txt",
            "This are the contents  of the 2nd testfile".getBytes());
        File file3 = createTestFile(folder1, "/sub/sub/TestFile3.txt",
            "This are the contents of the 3nd testfile".getBytes());

        // Let him scan the new content
        folder1.forceNextScan();
        folder1.scan();
        assertEquals(3, folder1.getFilesCount());

        // Give them time to copy

        Thread.sleep(3000);

        // Test ;)
        assertEquals(3, folder2.getFilesCount());

        // Version should be the 0 for new files
        for (FileInfo fileInfo : folder1.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // Version should be the 0 for new files
        for (FileInfo fileInfo : folder2.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // No active downloads?
        assertEquals(0, getContoller2().getTransferManager()
            .getActiveDownloadCount());

        assertEquals(getContoller1().getRecycleBin().getSize(), 0);
        assertEquals(getContoller2().getRecycleBin().getSize(), 0);

        file1.delete();
        file2.delete();
        file3.delete();

        assertFalse(file1.exists());
        assertFalse(file2.exists());
        assertFalse(file3.exists());

        // Let him scan the new content
        folder1.forceNextScan();
        folder1.scan();

        // all 3 must be deleted
        FileInfo[] folder1Files = folder1.getFiles();
        for (FileInfo fileInfo : folder1Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
        }

        // Give them time to remote deletion
        Thread.sleep(3000);

        // all 3 must be deleted remote
        FileInfo[] folder2Files = folder2.getFiles();
        for (FileInfo fileInfo : folder2Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
            File file = folder2.getDiskFile(fileInfo);
            assertFalse(file.exists());
        }
        assertEquals(getContoller2().getRecycleBin().getSize(), 3);

        // switch profiles
        folder2.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        folder1.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        RecycleBin recycleBin = getContoller2().getRecycleBin();
        List<FileInfo> folder2deletedFiles = getContoller2().getRecycleBin()
            .getAllRecycledFiles();
        for (FileInfo deletedFileInfo : folder2deletedFiles) {
            recycleBin.restoreFromRecycleBin(deletedFileInfo);
        }

        // all 3 must not be deleted at folder2
        for (FileInfo fileInfo : folder2.getFiles()) {
            assertFalse(fileInfo.isDeleted());
            assertEquals(2, fileInfo.getVersion());
            assertEquals(getController2NodeID(), fileInfo.getModifiedBy().id);
            File file = folder2.getDiskFile(fileInfo);
            assertTrue(file.exists());
        }

        // Give them time to undelete sync (means downloading;)
        Thread.sleep(3000);

        // all 3 must not be deleted anymore at folder1
        for (FileInfo fileInfo : folder1.getFiles()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
        }

        // Version should be the same (file did not change, it was only deleted
        // and restored!)
        for (FileInfo fileInfo : folder2.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // Version should be the same (file did not change, it was only deleted
        // and restored!)
        for (FileInfo fileInfo : folder1.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }
    }

}
