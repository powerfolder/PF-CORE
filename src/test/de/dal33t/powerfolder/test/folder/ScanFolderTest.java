package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Tests the scanning of file in the local folders.
 * <p>
 * TODO Test scan of folder which already has a database.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ScanFolderTest extends ControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        getController().setSilentMode(true);
        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }
    
    public void testScanSingleFileMulti() throws Exception {
        for (int i = 0; i < 10; i++) {
            testScanSingleFile();
            tearDown();
            setUp();
        }
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
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        TestHelper.changeFile(file);
        scanFolder();

        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(1, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(2, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Delete.
        assertTrue(file.delete());
        scanFolder();
        assertTrue(!file.exists());
        assertTrue(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(3, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Restore.
        TestHelper.createRandomFile(file.getParentFile(), file.getName());
        scanFolder();
        assertEquals(4, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // 15 more filechanges
        for (int i = 0; i < 15; i++) {
            TestHelper.changeFile(file);
            scanFolder();
            assertEquals(5 + i, getFolder().getKnowFilesAsArray()[0]
                .getVersion());
            assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
            assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        }

        // Do some afterchecks.
        assertEquals(1, getFolder().getKnownFilesCount());
    }

    /**
     * Tests scanning of a file that only changes the last modification date,
     * but not the size.
     */
    public void testScanLastModifiedOnlyChanged() {
        File file = TestHelper.createRandomFile(getFolder().getLocalBase());
        long s = file.length();
        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(s, file.length());
        // 20 secs in future
        file.setLastModified(file.lastModified() + 1000L * 20);
        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(1, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(s, file.length());
        // 100 seks into the past
        file.setLastModified(file.lastModified() - 1000L * 100);
        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(2, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(s, file.length());
    }

    /**
     * Tests the scan of a file that doesn't has changed the last modification
     * date, but the size only.
     */
    public void testScanSizeOnlyChanged() {
        File file = TestHelper.createRandomFile(getFolder().getLocalBase());
        long lm = file.lastModified();
        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(lm, getFolder().getKnowFilesAsArray()[0].getModifiedDate()
            .getTime());
        // 20 secs in future
        TestHelper.changeFile(file);
        file.setLastModified(lm);
        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(1, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(lm, getFolder().getKnowFilesAsArray()[0].getModifiedDate()
            .getTime());
        // 100 seks into the past
        TestHelper.changeFile(file);
        file.setLastModified(lm);
        scanFolder();
        assertEquals(1, getFolder().getKnownFilesCount());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);
        assertEquals(2, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(lm, getFolder().getKnowFilesAsArray()[0].getModifiedDate()
            .getTime());
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
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Delete.
        assertTrue(file.delete());
        scanFolder();
        assertTrue(!file.exists());
        assertTrue(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertEquals(2, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Restore.
        TestHelper.createRandomFile(file.getParentFile(), file.getName());
        scanFolder();
        assertEquals(3, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(4, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

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
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Move file one subdirectory up
        File destFile = new File(file.getParentFile().getParentFile(), file
            .getName());
        assertTrue(file.renameTo(destFile));
        scanFolder();

        // Should have two fileinfos: one deleted and one new.
        assertEquals(2, getFolder().getKnownFilesCount());
        FileInfo destFileInfo = retrieveFileInfo(destFile);
        assertFileMatch(destFile, destFileInfo);
        assertEquals(0, destFileInfo.getVersion());

        FileInfo srcFileInfo = retrieveFileInfo(file);
        assertFileMatch(file, srcFileInfo);
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
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Delete file
        assertTrue(file.delete());
        scanFolder();

        // Check
        FileInfo fInfo = getFolder().getKnowFilesAsArray()[0];
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(1, fInfo.getVersion());
        assertTrue(fInfo.isDeleted());
        assertFileMatch(file, fInfo);

        // Scan again some times
        scanFolder();
        scanFolder();
        scanFolder();

        // Check again
        fInfo = getFolder().getKnowFilesAsArray()[0];
        assertEquals(1, getFolder().getKnownFilesCount());
        assertEquals(1, fInfo.getVersion());
        assertTrue(fInfo.isDeleted());
        assertFileMatch(file, fInfo);
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
        Collection<FileInfo> files = getFolder().getKnownFiles();
        for (FileInfo info : files) {
            assertEquals(0, info.getVersion());
            assertFalse(info.isDeleted());
            File diskFile = info.getDiskFile(getController()
                .getFolderRepository());
            assertFileMatch(diskFile, info);
            assertTrue(testFiles.contains(diskFile));
        }
    }

    /**
     * Tests the scan of very many files.
     * <p>
     * TOT Notes: This test takes @ 11000 files aprox. 40-107 (86) seconds.
     */
    public void testScanExtremlyManyFiles() {
        final int nFiles = 44000;
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < nFiles; i++) {
            files.add(TestHelper
                .createRandomFile(getFolder().getLocalBase(), 5));
        }
        scanFolder();
        assertEquals(nFiles, getFolder().getKnownFilesCount());

        for (File file : files) {
            FileInfo fInfo = retrieveFileInfo(file);
            assertFileMatch(file, fInfo);
            assertEquals(fInfo.getName(), 0, fInfo.getVersion());
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

        assertEquals(testFile.getName(), getFolder().getKnowFilesAsArray()[0]
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
        assertEquals(0, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        TestHelper.changeFile(file);
        scanFolder();
        assertEquals(1, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

        // Okay from now on we have a good state.
        // Now change the disk file 1 day into the past
        File diskFile = getFolder().getKnowFilesAsArray()[0]
            .getDiskFile(getController().getFolderRepository());
        diskFile.setLastModified(diskFile.lastModified() - 24 * 60 * 60 * 1000);
        scanFolder();
        assertEquals(2, getFolder().getKnownFiles().iterator().next().getVersion());
        assertFalse(getFolder().getKnowFilesAsArray()[0].isDeleted());
        assertFileMatch(file, getFolder().getKnowFilesAsArray()[0]);

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

    private void scanFolder() {
        scanFolder(getFolder());
    }
}
