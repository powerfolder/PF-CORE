package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.ArchiveMode;
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

    public void testCopyOrMoveFileArchiver() {
        Folder fb = getFolderAtBart();
        File tb = TestHelper.createRandomFile(fb.getLocalBase(), 1024);

        scanFolder(fb);
        TestHelper.waitMilliSeconds(3000);

        FileInfo fib = fb.getKnownFiles().iterator().next();

        CopyOrMoveFileArchiver fa = ArchiveMode.FULL_BACKUP.getInstance(fb);
        try {
            fa.archive(fib, tb, false);
        } catch (IOException e) {
            fail(e.toString());
        }

        File expected = new TFile(fb.getSystemSubDir(), "archive");
        expected = new TFile(expected, fib.getRelativeName() + "_K_"
            + fib.getVersion());
        assertTrue(expected.exists());
        assertEquals(expected.lastModified(), fib.getModifiedDate().getTime());
    }

    public void testBackupOnDownload() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveMode(ArchiveMode.FULL_BACKUP);

        Folder fl = getFolderAtLisa();
        File tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });

        FileInfo fib = fb.getKnownFiles().iterator().next();
        File eBart = new TFile(fb.getSystemSubDir(), "archive");
        eBart = new TFile(eBart, fib.getRelativeName() + "_K_"
            + fib.getVersion());
        File eLisa = new TFile(fl.getSystemSubDir(), "archive");
        eLisa = new TFile(eLisa, fib.getRelativeName() + "_K_"
            + fib.getVersion());

        modLisaFile(tl, fib);

        assertTrue(eBart.exists());
        assertFalse(eLisa.exists());
    }

    public void testLimitedVersions() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveMode(ArchiveMode.FULL_BACKUP);
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
        for (int i = 0; i < ver.length; i++) {
            ver[i] = new TFile(archdir, fib.getRelativeName() + "_K_" + i);
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

    public void testChangeVersionsPerFile() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveMode(ArchiveMode.FULL_BACKUP);
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
        for (int i = 0; i < ver.length; i++) {
            ver[i] = new TFile(archdir, fib.getRelativeName() + "_K_" + i);
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

        CopyOrMoveFileArchiver aBart = getFolderAtBart().getFileArchiver();
        for (int i = 0; i < nVersion; i++) {
            TestHelper.waitMilliSeconds(2100);
            assertEquals(i + 2, modLisaFile(f, fInfo).getVersion());
            assertTrue(
                "Archived versions not found. Got: "
                    + aBart.getArchivedFilesInfos(fInfo), aBart
                    .getArchivedFilesInfos(fInfo).size() > 0);
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
        FileInfo dInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(),
            f.getParentFile());
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
}
