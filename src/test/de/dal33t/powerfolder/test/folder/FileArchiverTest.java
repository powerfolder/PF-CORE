package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.schlichtherle.truezip.file.TFile;

public class FileArchiverTest extends TwoControllerTestCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestFolderContents();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
    }
    
    /**
     * Helper utility to split a file name into name part and extension.
     * The extension part, if present, includes the '.' separator.
     * 
     * "testFile.txt" --> String[2]{"testFile",".txt"}
     * "testFileName" --> String[2]{"testFileName",""}
     * 
     * @param fileName
     * @return
     */
    private static String[] splitFileName(String fileName) {
        String[] fileNamePair = new String[2];
        if (fileName.contains(".")) {
            int pos = fileName.lastIndexOf(".");
            fileNamePair[0] = fileName.substring(0, pos);
            fileNamePair[1] = fileName.substring(pos); // Include the '.' with the extension.
        } else {
            fileNamePair[0] = fileName;
            fileNamePair[1] = "";
        }
        return fileNamePair;
    }

    /**
     * Check we can archive a file in the form <fileName>_K_n.<ext> .
     */
    public void testCopyOrMoveFileArchiver() {
        Folder fb = getFolderAtBart();
        File tb = TestHelper.createRandomFile(fb.getLocalBase(), 1024);

        scanFolder(fb);
        TestHelper.waitMilliSeconds(3000);

        FileInfo fib = fb.getKnownFiles().iterator().next();

        File archive = new TFile(fb.getSystemSubDir(), "archive");
        archive.mkdirs();
        FileArchiver fa = new FileArchiver(archive, getContollerBart().getMySelf().getInfo());
        try {
            fa.archive(fib, tb, false);
        } catch (IOException e) {
            fail(e.toString());
        }

        File expected = new TFile(fb.getSystemSubDir(), "archive");
        String[] fileNameParts = splitFileName(fib.getRelativeName()); 
        expected = new TFile(expected, fileNameParts[0] + "_K_" + fib.getVersion() + fileNameParts[1]);
        assertTrue(expected.exists());
        assertEquals(expected.lastModified(), fib.getModifiedDate().getTime());
    }

    /**
     * Check that files backup on download.
     */
    public void testBackupOnDownload() {
        final Folder fb = getFolderAtBart();

        Folder fl = getFolderAtLisa();
        File tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });

        FileInfo fib = fb.getKnownFiles().iterator().next();
        File expectedBart = new TFile(fb.getSystemSubDir(), "archive");
        String[] fileNameParts = splitFileName(fib.getRelativeName());
        expectedBart = new TFile(expectedBart, fileNameParts[0] + "_K_" + fib.getVersion() + fileNameParts[1]);
        
        File expectedLisa = new TFile(fl.getSystemSubDir(), "archive");
        expectedLisa = new TFile(expectedLisa, fileNameParts[0] + "_K_" + fib.getVersion() + fileNameParts[1]);

        modLisaFile(tl, fib);

        assertTrue(expectedBart.exists());
        assertFalse(expectedLisa.exists());
    }

    /**
     * Check versioning works, limited to the required number of files.
     */
    public void testLimitedVersions() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveVersions(3);

        Folder fl = getFolderAtLisa();
        File tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }

            public String message() {
                return "Known files: " + fb.getKnownFiles();
            }
        });
        FileInfo fib = fb.getKnownFiles().iterator().next();

        for (int i = 0; i < 4; i++) {
            TestHelper.waitMilliSeconds(2100);
            modLisaFile(tl, fib);
        }

        File ver[] = new TFile[4];
        File archdir = new TFile(fb.getSystemSubDir(), "archive");
        String[] fileNameParts = splitFileName(fib.getRelativeName());
        for (int i = 0; i < ver.length; i++) {
            
            ver[i] = new TFile(archdir, fileNameParts[0] + "_K_" + i + fileNameParts[1]);
        }

        assertFalse(ver[0].exists());
        assertTrue(ver[1].exists());

        TestHelper.waitMilliSeconds(2100);
        modLisaFile(tl, fib);
        assertFalse(ver[1].exists());
        assertTrue(ver[2].exists());

        fb.setArchiveVersions(5);
        modLisaFile(tl, fib);
        assertTrue(ver[2].exists());

        modLisaFile(tl, fib);
        assertTrue(ver[2].exists());

        modLisaFile(tl, fib);
        assertFalse(ver[2].exists());
        assertTrue(ver[3].exists());
    }

    /**
     * Check actual versions change when archive versions change
     */
    public void testChangeVersionsPerFile() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveVersions(3);

        Folder fl = getFolderAtLisa();
        File tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });
        FileInfo fib = fb.getKnownFiles().iterator().next();
        assertEquals(0, fb.getFileArchiver().getSize());

        File ver[] = new TFile[5];
        for (int i = 0; i < ver.length; i++) {
            TestHelper.waitMilliSeconds(2100);
            modLisaFile(tl, fib);
        }
        File archdir = new TFile(fb.getSystemSubDir(), "archive");
        String[] fileNameParts = splitFileName(fib.getRelativeName());
        for (int i = 0; i < ver.length; i++) {
            ver[i] = new TFile(archdir, fileNameParts[0] + "_K_" + i + fileNameParts[1]);
        }
        assertEquals(0, fl.getFileArchiver().getSize());
        assertFalse(ver[0].exists());
        assertFalse(ver[1].exists());
        assertTrue(ver[2].exists());
        assertTrue(ver[3].exists());
        assertTrue(ver[4].exists());

        fb.setArchiveVersions(1);
        assertTrue(fb.getFileArchiver().maintain());
        assertTrue(fb.getFileArchiver().getSize() > 0);
        assertFalse(ver[0].exists());
        assertFalse(ver[1].exists());
        assertFalse(ver[2].exists());
        assertFalse(ver[3].exists());
        assertTrue(ver[4].exists());

        fb.setArchiveVersions(0);
        assertTrue(fb.getFileArchiver().maintain());
        assertEquals(0, fb.getFileArchiver().getSize());
        assertFalse(ver[0].exists());
        assertFalse(ver[1].exists());
        assertFalse(ver[2].exists());
        assertFalse(ver[3].exists());
        assertFalse(ver[4].exists());
    }

    public void testUnlimitedFileArchive() throws IOException {
        int nVersion = 20;
        getFolderAtBart().setArchiveVersions(-1);

        File f = TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());
        scanFolder(getFolderAtLisa());
        FileInfo fInfo = getFolderAtLisa().getKnownFiles().iterator().next();
        TestHelper.waitMilliSeconds(2100);
        modLisaFile(f, fInfo);

        FileArchiver aBart = getFolderAtBart().getFileArchiver();
        for (int i = 0; i < nVersion; i++) {
            TestHelper.waitMilliSeconds(2100);
            assertEquals(i + 2, modLisaFile(f, fInfo).getVersion());
            assertTrue("Archived versions not found. Got: " +
                    aBart.getArchivedFilesInfos(fInfo), 
                    aBart.getArchivedFilesInfos(fInfo).size() > 0);
        }
        assertTrue(getFolderAtBart().getFileArchiver().getSize() > 0);
        assertEquals(nVersion + 1, aBart.getArchivedFilesInfos(fInfo).size());

        List<FileInfo> archived = aBart.getArchivedFilesInfos(fInfo);
        assertEquals(nVersion + 1, archived.size());

        // Now restore
        FileInfo versionInfo = archived.get(4);
        File restoreTo = versionInfo.getDiskFile(getContollerBart()
            .getFolderRepository());
        aBart.restore(versionInfo, restoreTo);
        getFolderAtBart().scanChangedFile(versionInfo);

        archived = aBart.getArchivedFilesInfos(fInfo);
        assertEquals(nVersion + 1, archived.size());
    }

    public void testRestoreInDeletedSubdir() throws IOException {
        getFolderAtLisa().setArchiveVersions(1);
        File f = TestHelper.createRandomFile(new File(getFolderAtLisa()
            .getLocalBase(), "subdir"));
        scanFolder(getFolderAtLisa());
        FileInfo fInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(), f);
        FileInfo dInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(), f.getParentFile());
        getFolderAtLisa().removeFilesLocal(dInfo);

        assertTrue(getFolderAtLisa().getFileArchiver().restore(fInfo, f));
        assertTrue(f.exists());
    }

    public void testNoConflictOnRestore() throws IOException {
        File fileAtBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() == 1;
            }

            public String message() {
                return "Files at lisa: " + getFolderAtLisa().getKnownFiles();
            }
        });
        TestHelper.waitMilliSeconds(3000);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());

        final FileInfo fInfo = getFolderAtBart().getFile(
            FileInfoFactory.lookupInstance(getFolderAtBart(), fileAtBart));
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getFileArchiver()
                    .getArchivedFilesInfos(fInfo).size() == 1;
            }

            public String message() {
                return "Archived files at lisa: "
                    + getFolderAtLisa().getFileArchiver()
                        .getArchivedFilesInfos(fInfo);
            }
        });

        TestHelper.waitMilliSeconds(2500);
        File fileAtLisa = fInfo.getDiskFile(getContollerLisa()
            .getFolderRepository());
        LoggingManager.setConsoleLogging(Level.FINER);
        assertTrue(getFolderAtLisa().getFileArchiver().restore(
            FileInfoFactory.archivedFile(fInfo.getFolderInfo(), fInfo
                .getRelativeName(), fInfo.getSize(), fInfo.getModifiedBy(),
                new Date(System.currentTimeMillis() - 10000), 0), fileAtLisa));
        getFolderAtLisa().scanChangedFile(fInfo);

        TestHelper.waitMilliSeconds(2500);
        assertEquals(2, getFolderAtLisa().getFile(fInfo).getVersion());
        assertEquals(1, getFolderAtLisa().getKnownItemCount());
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtBart().getFileArchiver()
                    .getArchivedFilesInfos(fInfo).size() == 1;
            }

            public String message() {
                return "Archived files at bart: "
                    + getFolderAtBart().getFileArchiver()
                        .getArchivedFilesInfos(fInfo);
            }
        });
        assertEquals(0, getFolderAtBart().countProblems());
    }

    private FileInfo modLisaFile(File file, final FileInfo fInfo) {
        TestHelper.changeFile(file);
        scanFolder(getFolderAtLisa());

        final FileInfo scanned = getFolderAtLisa().getFile(fInfo);

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                for (FileInfo fi : getFolderAtBart().getKnownFiles()) {
                    if (fi.isVersionDateAndSizeIdentical(scanned)) {
                        return true;
                    }
                }
                return false;
            }
        });
        return fInfo.getLocalFileInfo(getContollerLisa().getFolderRepository());
    }
    
    /**
     * Test the getBaseName method in FileArchiver.
     * Check that it can get base names for 'file_K_nnn.txt', 'file_K_nnn' and the old way of 'file.txt_K_nnn'.
     */
    public void testGetBaseName() {
        assertEquals("New way with extension", "file.txt", FileArchiver.getBaseName(new File("/bob/file_K_6.txt")));
        assertEquals("New way with no extension", "file", FileArchiver.getBaseName(new File("bob/file_K_6")));
        assertEquals("Old way with extension", "file.txt", FileArchiver.getBaseName(new File("file.txt_K_6")));
        try {
            FileArchiver.getBaseName(new File("file.txt"));
            fail("Not an archive file");
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }
    
    /**
     * Test the getBaseName method in FileArchiver.
     * Check that archive files belong to parent files.
     * Check for archive names like 'file_K_nnn.txt', 'file_K_nnn' and the old way of 'file.txt_K_nnn'.
     */
    public void testBelongsTo() {
        assertTrue("New way with extension", FileArchiver.belongsTo("/bob/file_K_6.txt", "/bob/file.txt"));
        assertTrue("New way with no extension", FileArchiver.belongsTo("bob/file_K_6", "bob/file"));
        assertTrue("Old way with extension", FileArchiver.belongsTo("file.txt_K_6", "file.txt"));
        assertFalse("Does not belong to", FileArchiver.belongsTo("file.txt_K_6", "word.doc"));
    }
    
    /**
     * The old way of storing archive files was like 'file.txt_K_nnn'.
     * The current way is like 'file_K_nnn.txt'.
     * So need to be sure that if there is an old archive file, it can be restored.
     */
    public void testOldArchiveVersions() throws IOException {
        
        // Create a file for Lisa and delete it.
        getFolderAtLisa().setArchiveVersions(1);
        File randomFile = TestHelper.createRandomFile(new File(getFolderAtLisa().getLocalBase(), "subdir"));
        scanFolder(getFolderAtLisa());
        FileInfo fileInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(), randomFile);
        FileInfo directoryInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(), randomFile.getParentFile());
        getFolderAtLisa().removeFilesLocal(directoryInfo);
        
        // Tinker with archive file name to make it look like the old archive type, with the '_K_nnn' at the end.
        File archiveFolder = new TFile(getFolderAtLisa().getSystemSubDir(), "archive/subdir");
        TFile archiveFile = (TFile) archiveFolder.listFiles()[0];
        File oldFileForamt = new File(archiveFile.getParent(), randomFile.getName() + "_K_" + fileInfo.getVersion());
        ((File)archiveFile).renameTo(oldFileForamt);  

        // Can we restore the old file format?
        assertTrue("File restore", getFolderAtLisa().getFileArchiver().restore(fileInfo, randomFile));
        assertTrue("Restored file", randomFile.exists());
    }
}
