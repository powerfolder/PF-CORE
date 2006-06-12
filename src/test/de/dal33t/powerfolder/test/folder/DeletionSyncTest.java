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

    private static final String BASEDIR1 = "build/test/controllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/controllerLisa/testFolder";
    // private static final Logger LOG =
    // Logger.getLogger(DeletionSyncTest.class);
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
        folderAtBart.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        folderAtLisa.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File file1 = createTestFile(folderAtBart, "/TestFile.txt",
            "This are the contents of the testfile".getBytes());
        File file2 = createTestFile(folderAtBart, "/TestFile2.txt",
            "This are the contents  of the 2nd testfile".getBytes());
        File file3 = createTestFile(folderAtBart, "/sub/sub/TestFile3.txt",
            "This are the contents of the 3nd testfile".getBytes());

        // Let him scan the new content
        folderAtBart.forceScanOnNextMaintenance();
        folderAtBart.maintain();
        assertEquals(3, folderAtBart.getFilesCount());

        // Give them time to copy

        Thread.sleep(3000);

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

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        assertEquals(getContollerBart().getRecycleBin().getSize(), 0);
        assertEquals(getContollerLisa().getRecycleBin().getSize(), 0);

        file1.delete();
        file2.delete();
        file3.delete();

        assertFalse(file1.exists());
        assertFalse(file2.exists());
        assertFalse(file3.exists());

        // Let him scan the new content
        folderAtBart.forceScanOnNextMaintenance();
        folderAtBart.maintain();

        // all 3 must be deleted
        FileInfo[] folder1Files = folderAtBart.getFiles();
        for (FileInfo fileInfo : folder1Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
        }

        // Give them time to remote deletion
        Thread.sleep(3000);

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

        // Give them time to undelete sync (means downloading;)
        Thread.sleep(300000);

        getContollerBart().getFolderRepository().getFileRequestor().triggerFileRequesting();

        for (FileInfo fileAtBartExpected : folderAtBart.getExpecedFiles(false))
        {
            assertEquals(3, fileAtBartExpected.getVersion());
            assertFalse(fileAtBartExpected.isDeleted());
        }
        
        // all 3 must not be deleted anymore at folder1
        for (FileInfo fileInfo : folderAtBart.getFiles()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
        }

        // Version should be the same (file did not change, it was only deleted
        // and restored!)
        for (FileInfo fileInfo : folderAtLisa.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // Version should be the same (file did not change, it was only deleted
        // and restored!)
        for (FileInfo fileInfo : folderAtBart.getFiles()) {
            assertEquals(0, fileInfo.getVersion());
        }
    }

}
