package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.Condition;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;

/**
 * Tests the scanning of file in the local folders.
 * <p>
 * TODO Test scan of folder which already has a database.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ScanFolderTest extends ControllerTestCase {

    private boolean initalScanOver = false;
    private boolean scanned;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepoListener());
        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return initalScanOver;
            }
        });
        System.out.println("Inital scan over, setup ready");
    }

    /**
     * Tests the scan of one single file, including updates, deletion and
     * restore of the file.
     */
    public void testScanSingleFile() {
        File file = TestHelper.createRandomFile(getFolder().getLocalBase(),
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(0, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(2, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        // Delete.
        assertTrue(file.delete());
        scanFolder();
        assertTrue(!file.exists());
        assertTrue(getFolder().getKnownFiles()[0].isDeleted());
        assertEquals(3, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        // Restore.
        TestHelper.createRandomFile(file.getParentFile(), file.getName());
        scanFolder();
        assertEquals(4, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        // 15 more filechanges
        for (int i = 0; i < 15; i++) {
            TestHelper.changeFile(file);
            scanFolder();
            assertEquals(5 + i, getFolder().getKnownFiles()[0].getVersion());
            assertFalse(getFolder().getKnownFiles()[0].isDeleted());
            matches(file, getFolder().getKnownFiles()[0]);
        }

        // Do some afterchecks.
        assertEquals(1, getFolder().getKnownFilesCount());
    }

    /**
     * Tests the scan of one single file in a subdirectory.
     */
    public void testScanSingleFileInSubdir() {
        File subdir = new File(getFolder().getLocalBase(),
            "subDir1/SUBDIR2.ext");
        assertTrue(subdir.mkdirs());
        File file = TestHelper.createRandomFile(subdir, 10 + (int) (Math
            .random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(0, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        // Delete.
        assertTrue(file.delete());
        scanFolder();
        assertTrue(!file.exists());
        assertTrue(getFolder().getKnownFiles()[0].isDeleted());
        assertEquals(2, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        // Restore.
        TestHelper.createRandomFile(file.getParentFile(), file.getName());
        scanFolder();
        assertEquals(3, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(4, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        // Do some afterchecks.
        assertEquals(1, getFolder().getKnownFilesCount());
    }

    public void testScanFileMovement() {
        File subdir = new File(getFolder().getLocalBase(),
            "subDir1/SUBDIR2.ext");
        assertTrue(subdir.mkdirs());
        File file = TestHelper.createRandomFile(subdir, 10 + (int) (Math
            .random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(0, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        // Move file one subdirectory up
        File destFile = new File(file.getParentFile().getParentFile(), file
            .getName());
        assertTrue(file.renameTo(destFile));
        scanFolder();

        // Should have two fileinfos: one deleted and one new.
        assertEquals(2, getFolder().getKnownFilesCount());
        FileInfo destFileInfo = retrieveFileInfo(destFile);
        matches(destFile, destFileInfo);
        assertEquals(0, destFileInfo.getVersion());

        FileInfo srcFileInfo = retrieveFileInfo(file);
        matches(file, srcFileInfo);
        assertEquals(1, srcFileInfo.getVersion());
        assertTrue(srcFileInfo.isDeleted());
    }

    public void testScanFileDeletion() {
        File subdir = new File(getFolder().getLocalBase(),
            "subDir1/SUBDIR2.ext");
        assertTrue(subdir.mkdirs());
        File file = TestHelper.createRandomFile(subdir, 10 + (int) (Math
            .random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(0, getFolder().getKnownFiles()[0].getVersion());
        matches(file, getFolder().getKnownFiles()[0]);

        // Delete file
        assertTrue(file.delete());
        scanFolder();

        // Check
        FileInfo fInfo = getFolder().getKnownFiles()[0];
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(1, fInfo.getVersion());
        assertTrue(fInfo.isDeleted());
        matches(file, fInfo);

        // Scan again some times
        scanFolder();
        scanFolder();
        scanFolder();

        // Check again
        fInfo = getFolder().getKnownFiles()[0];
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(1, fInfo.getVersion());
        assertTrue(fInfo.isDeleted());
        matches(file, fInfo);
    }

    /**
     * Tests the scan of multiple files in multiple subdirectories.
     */
    public void testScanMulipleFilesInSubdirs() {
        int nFiles = 500;
        Set<File> testFiles = new HashSet<File>();

        // Create a inital folder structure
        File currentSubDir = new File(getFolder().getLocalBase(), "subDir1");
        for (int i = 0; i < nFiles; i++) {
            if (Math.random() > 0.95) {
                // Change subdir
                do {
                    int depth = (int) (Math.random() * 3);
                    String fileName = "";
                    for (int j = 0; j < depth; j++) {
                        fileName += TestHelper.createRandomFilename() + "/";
                    }
                    fileName += TestHelper.createRandomFilename();
                    currentSubDir = new File(getFolder().getLocalBase(),
                        fileName);
                } while (!currentSubDir.mkdirs());
                // System.err.println("New subdir: "
                // + currentSubDir.getAbsolutePath());
            }

            if (!currentSubDir.equals(getFolder().getLocalBase())) {
                if (Math.random() > 0.9) {
                    // Go one directory up
                    // System.err.println("Moving up from "
                    // + currentSubDir.getAbsoluteFile());
                    currentSubDir = currentSubDir.getParentFile();
                } else if (Math.random() > 0.95) {
                    // Go one directory up

                    File subDirCanidate = new File(currentSubDir, TestHelper
                        .createRandomFilename());
                    // System.err.println("Moving down to "
                    // + currentSubDir.getAbsoluteFile());
                    if (!subDirCanidate.isFile()) {
                        currentSubDir = subDirCanidate;
                        currentSubDir.mkdirs();
                    }
                }
            }

            File file = TestHelper.createRandomFile(currentSubDir);
            testFiles.add(file);
        }

        scanFolder();

        // Test
        assertEquals(nFiles, getFolder().getKnownFilesCount());
        List<FileInfo> files = getFolder().getKnownFilesList();
        for (FileInfo info : files) {
            assertEquals(0, info.getVersion());
            assertFalse(info.isDeleted());
            File diskFile = info.getDiskFile(getController()
                .getFolderRepository());
            matches(diskFile, info);
            assertTrue(testFiles.contains(diskFile));
        }

        // TestHelper.changeFile(file);
        // scanFolder();
        // assertEquals(1, getFolder().getFiles()[0].getVersion());
        // matches(file, getFolder().getFiles()[0]);
        //
        // // Delete.
        // assertTrue(file.delete());
        // scanFolder();
        // assertTrue(!file.exists());
        // assertTrue(getFolder().getFiles()[0].isDeleted());
        // assertEquals(2, getFolder().getFiles()[0].getVersion());
        // matches(file, getFolder().getFiles()[0]);
        //
        // // Restore.
        // TestHelper.createRandomFile(file.getParentFile(), file.getName());
        // scanFolder();
        // assertEquals(3, getFolder().getFiles()[0].getVersion());
        // assertFalse(getFolder().getFiles()[0].isDeleted());
        // matches(file, getFolder().getFiles()[0]);
        //
        // TestHelper.changeFile(file);
        // scanFolder();
        // assertEquals(4, getFolder().getFiles()[0].getVersion());
        // matches(file, getFolder().getFiles()[0]);
        //
        // // Do some afterchecks.
        // assertEquals(1, getFolder().getFilesCount());
    }

    /**
     * Tests the scan of very many files.
     * <p>
     * TOT Notes: This test takes @ 11000 files aprox. 40-107 (86) seconds.
     */
    public void testScanExtremlyManyFiles() {
        final int nFiles = 11000;
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < nFiles; i++) {
            files.add(TestHelper
                .createRandomFile(getFolder().getLocalBase(), 5));
        }
        scanFolder();
        assertEquals(nFiles, getFolder().getKnownFilesCount());

        for (File file : files) {
            FileInfo fInfo = retrieveFileInfo(file);
            matches(file, fInfo);
            assertEquals(0, fInfo.getVersion());
        }
    }

    /**
     * Test the scan of file and dirs, that just change the case.
     * <p>
     * e.g. "TestDir/SubDir/MyFile.txt" to "testdir/subdir/myfile.txt"
     * <p>
     * TRAC #232
     */
    public void testCaseChangeScan() {
        File testFile = TestHelper.createRandomFile(getFolder().getLocalBase(),
            "TESTFILE.TXT");
        scanFolder(getFolder());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolder().getKnownFilesCount() == 1;
            }
        });

        assertEquals(testFile.getName(), getFolder().getKnownFiles()[0]
            .getFilenameOnly());

        // Change case
        testFile.renameTo(new File(getFolder().getLocalBase(), "testfile.txt"));

        scanFolder();

        // HOW TO HANDLE THAT? WHAT TO EXPECT??
        // assertEquals(1, getFolderAtBart().getFilesCount());
    }

    /**
     * Tests the scan of one single file that gets changed into the past. This
     * test should ensure definied behavior.
     * <p>
     * Related TRAC ticket: #464
     */
    public void testScanLastModificationDateInPast() {
        File file = TestHelper.createRandomFile(getFolder().getLocalBase(),
            10 + (int) (Math.random() * 100));

        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(0, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);

        // Okay from now on we have a good state.
        // Now change the disk file 1 day into the past
        File diskFile = getFolder().getKnownFiles()[0].getDiskFile(getController()
            .getFolderRepository());
        diskFile.setLastModified(diskFile.lastModified() - 24 * 60 * 60 * 1000);
        scanFolder();
        assertEquals(2, getFolder().getKnownFiles()[0].getVersion());
        assertFalse(getFolder().getKnownFiles()[0].isDeleted());
        matches(file, getFolder().getKnownFiles()[0]);
        
        // Do some afterchecks.
        assertEquals(1, getFolder().getKnownFilesCount());
    }

    // Helper *****************************************************************

    /**
     * @param file
     * @return the fileinfo in the test folder for this file.
     */
    private FileInfo retrieveFileInfo(File file) {
        return getFolder().getFile(new FileInfo(getFolder(), file));
    }

    /**
     * Tests if the diskfile matches the fileinfo. Checks name, lenght/size,
     * modification date and the deletion status.
     * 
     * @param diskFile
     *            the diskfile to compare
     * @param fInfo
     *            the fileinfo
     */
    private void matches(File diskFile, FileInfo fInfo) {
        boolean nameMatch = diskFile.getName().equals(fInfo.getFilenameOnly());
        boolean sizeMatch = diskFile.length() == fInfo.getSize();
        boolean fileObjectEquals = diskFile.equals(fInfo
            .getDiskFile(getController().getFolderRepository()));
        boolean deleteStatusMatch = diskFile.exists() == !fInfo.isDeleted();
        boolean lastModifiedMatch = diskFile.lastModified() == fInfo
            .getModifiedDate().getTime();

        // Skip last modification test when diskfile is deleted.
        boolean matches = !diskFile.isDirectory() && nameMatch && sizeMatch
            && (!diskFile.exists() || lastModifiedMatch) && deleteStatusMatch
            && fileObjectEquals;

        assertTrue("FileInfo does not match physical file. \nFileInfo:\n "
            + fInfo.toDetailString() + "\nFile:\n " + diskFile.getName()
            + ", size: " + diskFile.length() + ", lastModified: "
            + new Date(diskFile.lastModified()) + "\n\nWhat matches?:\nName: "
            + nameMatch + "\nSize: " + sizeMatch + "\nlastModifiedMatch: "
            + lastModifiedMatch + "\ndeleteStatus: " + deleteStatusMatch
            + "\nFileObjectEquals: " + fileObjectEquals, matches);
    }

    /**
     * Scans a folder and waits for the scan to complete.
     */
    private void scanFolder() {
        scanned = false;
        getFolder().forceScanOnNextMaintenance();
        getController().getFolderRepository().triggerMaintenance();
        TestHelper.waitForCondition(200, new Condition() {
            public boolean reached() {
                return scanned;
            }
        });
        assertTrue("Folder was not scanned as requested", scanned);
    }

    private final class MyFolderRepoListener implements
        FolderRepositoryListener
    {
        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            initalScanOver = true;
            scanned = true;
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
