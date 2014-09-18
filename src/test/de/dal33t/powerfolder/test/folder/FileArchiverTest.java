package de.dal33t.powerfolder.test.folder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class FileArchiverTest extends TwoControllerTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestFolderContents();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
    }

    public void testCopyOrMoveFileArchiver() throws IOException {
        Folder fb = getFolderAtBart();
        Path tb = TestHelper.createRandomFile(fb.getLocalBase(), 1024);

        scanFolder(fb);
        TestHelper.waitMilliSeconds(3000);

        FileInfo fib = fb.getKnownFiles().iterator().next();

        Path archive = fb.getSystemSubDir().resolve("archive");
        Files.createDirectories(archive);
        FileArchiver fa = new FileArchiver(archive, getContollerBart()
            .getMySelf().getInfo());
        try {
            fa.archive(fib, tb, false);
        } catch (IOException e) {
            fail(e.toString());
        }

        Path expected = fb.getSystemSubDir().resolve("archive");
        expected = expected.resolve(fib.getRelativeName().replace(".",
            "_K_" + fib.getVersion() + "."));
        assertTrue(Files.exists(expected));
        assertEquals(Files.getLastModifiedTime(expected).toMillis(), fib
            .getModifiedDate().getTime());

        FileInfo fia = fa.getArchivedFilesInfos(fib).get(0);
        assertEquals(fib.getRelativeName(), fia.getRelativeName());
    }

    public void testBackupOnDownload() {
        final Folder fb = getFolderAtBart();

        Folder fl = getFolderAtLisa();
        Path tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });

        FileInfo fib = fb.getKnownFiles().iterator().next();
        Path eBart = fb.getSystemSubDir().resolve("archive");
        eBart = eBart.resolve(fib.getRelativeName().replace(".",
            "_K_" + fib.getVersion() + "."));
        Path eLisa = fl.getSystemSubDir().resolve("archive");
        eLisa = eLisa.resolve(fib.getRelativeName().replace(".",
            "_K_" + fib.getVersion() + "."));

        modLisaFile(tl, fib);

        assertTrue(Files.exists(eBart));
        assertFalse(Files.exists(eLisa));
    }

    public void testLimitedVersions() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveVersions(3);

        Folder fl = getFolderAtLisa();
        Path tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

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

        Path ver[] = new Path[4];
        Path archdir = fb.getSystemSubDir().resolve("archive");
        for (int i = 0; i < ver.length; i++) {
            ver[i] = archdir.resolve(fib.getRelativeName().replace(".",
                "_K_" + i + "."));
        }

        assertFalse(Files.exists(ver[0]));
        assertTrue(Files.exists(ver[1]));

        TestHelper.waitMilliSeconds(2100);
        modLisaFile(tl, fib);
        assertFalse(Files.exists(ver[1]));
        assertTrue(Files.exists(ver[2]));

        fb.setArchiveVersions(5);
        modLisaFile(tl, fib);
        assertTrue(Files.exists(ver[2]));

        modLisaFile(tl, fib);
        assertTrue(Files.exists(ver[2]));

        modLisaFile(tl, fib);
        assertFalse(Files.exists(ver[2]));
        assertTrue(Files.exists(ver[3]));
    }

    public void testChangeVersionsPerFile() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveVersions(3);

        Folder fl = getFolderAtLisa();
        Path tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });
        FileInfo fib = fb.getKnownFiles().iterator().next();
        assertEquals(0, fb.getFileArchiver().getSize());

        Path ver[] = new Path[5];
        for (int i = 0; i < ver.length; i++) {
            TestHelper.waitMilliSeconds(2100);
            modLisaFile(tl, fib);
        }
        Path archdir = fb.getSystemSubDir().resolve("archive");
        for (int i = 0; i < ver.length; i++) {
            ver[i] = archdir.resolve(fib.getRelativeName().replace(".",
                "_K_" + i + "."));
        }
        assertEquals(0, fl.getFileArchiver().getSize());
        assertFalse(Files.exists(ver[0]));
        assertFalse(Files.exists(ver[1]));
        assertTrue(Files.exists(ver[2]));
        assertTrue(Files.exists(ver[3]));
        assertTrue(Files.exists(ver[4]));

        fb.setArchiveVersions(1);
        assertTrue(fb.getFileArchiver().maintain());
        assertTrue(fb.getFileArchiver().getSize() > 0);
        assertFalse(Files.exists(ver[0]));
        assertFalse(Files.exists(ver[1]));
        assertFalse(Files.exists(ver[2]));
        assertFalse(Files.exists(ver[3]));
        assertTrue(Files.exists(ver[4]));

        fb.setArchiveVersions(0);
        assertTrue(fb.getFileArchiver().maintain());
        assertEquals(0, fb.getFileArchiver().getSize());
        assertFalse(Files.exists(ver[0]));
        assertFalse(Files.exists(ver[1]));
        assertFalse(Files.exists(ver[2]));
        assertFalse(Files.exists(ver[3]));
        assertFalse(Files.exists(ver[4]));
    }

    public void testUnlimitedFileArchive() throws IOException {
        int nVersion = 21;
        getFolderAtBart().setArchiveVersions(-1);

        Path f = TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());
        scanFolder(getFolderAtLisa());
        FileInfo fInfo = getFolderAtLisa().getKnownFiles().iterator().next();
        TestHelper.waitMilliSeconds(2100);
        modLisaFile(f, fInfo);

        FileArchiver aBart = getFolderAtBart().getFileArchiver();
        for (int i = 0; i < nVersion - 1; i++) {
            TestHelper.waitMilliSeconds(2100);
            assertEquals(i + 2, modLisaFile(f, fInfo).getVersion());
            assertTrue(
                "Archived versions not found. Got: "
                    + aBart.getArchivedFilesInfos(fInfo), aBart
                    .getArchivedFilesInfos(fInfo).size() > 0);
        }
        assertTrue(getFolderAtBart().getFileArchiver().getSize() > 0);
        assertEquals(nVersion, aBart.getArchivedFilesInfos(fInfo).size());

        List<FileInfo> archived = aBart.getArchivedFilesInfos(fInfo);
        assertEquals(nVersion, archived.size());

        // Now restore
        FileInfo versionInfo = archived.get(4);
        Path restoreTo = versionInfo.getDiskFile(getContollerBart()
            .getFolderRepository());
        aBart.restore(versionInfo, restoreTo);
        getFolderAtBart().scanChangedFile(versionInfo);

        archived = aBart.getArchivedFilesInfos(fInfo);
        assertEquals(nVersion, archived.size());
    }

    public void testRestoreInDeletedSubdir() throws IOException {
        getFolderAtLisa().setArchiveVersions(1);
        Path f = TestHelper.createRandomFile(getFolderAtLisa().getLocalBase()
            .resolve("subdir"));
        scanFolder(getFolderAtLisa());
        FileInfo fInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(), f);
        FileInfo dInfo = FileInfoFactory.lookupInstance(getFolderAtLisa(),
            f.getParent());
        getFolderAtLisa().removeFilesLocal(dInfo);

        assertTrue(getFolderAtLisa().getFileArchiver().restore(fInfo, f));
        assertTrue(Files.exists(f));
    }

    public void testNoConflictOnRestore() throws IOException {
        Path fileAtBart = TestHelper.createRandomFile(getFolderAtBart()
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
        Path fileAtLisa = fInfo.getDiskFile(getContollerLisa()
            .getFolderRepository());
        LoggingManager.setConsoleLogging(Level.FINER);

        FileInfo archiveFileInfo = FileInfoFactory.archivedFile(
            fInfo.getFolderInfo(), fInfo.getRelativeName(), null,
            fInfo.getSize(), fInfo.getModifiedBy(),
            new Date(System.currentTimeMillis() - 10000), 0, null, null);

        assertTrue(getFolderAtLisa().getFileArchiver().restore(archiveFileInfo,
            fileAtLisa));
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

    private FileInfo modLisaFile(Path file, final FileInfo fInfo) {
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
