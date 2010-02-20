package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.test.Condition;
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

    public void testCopyOrMoveFileArchiver() {
        Folder fb = getFolderAtBart();
        File tb = TestHelper.createRandomFile(fb.getLocalBase(), 1024);

        scanFolder(fb);
        TestHelper.waitMilliSeconds(3000);

        FileInfo fib = fb.getKnownFiles().iterator().next();

        FileArchiver fa = ArchiveMode.FULL_BACKUP.getInstance(fb);
        try {
            fa.archive(fib, tb, false);
        } catch (IOException e) {
            fail(e.toString());
        }

        File expected = new File(fb.getSystemSubDir(), "archive");
        expected = new File(expected, fib.getRelativeName() + "_K_"
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

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });

        FileInfo fib = fb.getKnownFiles().iterator().next();
        File eBart = new File(fb.getSystemSubDir(), "archive");
        eBart = new File(eBart, fib.getRelativeName() + "_K_"
            + fib.getVersion());
        File eLisa = new File(fl.getSystemSubDir(), "archive");
        eLisa = new File(eLisa, fib.getRelativeName() + "_K_"
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

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });
        FileInfo fib = fb.getKnownFiles().iterator().next();

        for (int i = 0; i < 4; i++) {
            modLisaFile(tl, fib);
        }

        File ver[] = new File[4];
        File archdir = new File(fb.getSystemSubDir(), "archive");
        for (int i = 0; i < ver.length; i++) {
            ver[i] = new File(archdir, fib.getRelativeName() + "_K_" + i);
        }

        assertFalse(ver[0].exists());
        assertTrue(ver[1].exists());

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

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return fb.getKnownFiles().size() > 0;
            }
        });
        FileInfo fib = fb.getKnownFiles().iterator().next();

        File ver[] = new File[5];
        for (int i = 0; i < ver.length; i++) {
            modLisaFile(tl, fib);
        }
        File archdir = new File(fb.getSystemSubDir(), "archive");
        for (int i = 0; i < ver.length; i++) {
            ver[i] = new File(archdir, fib.getRelativeName() + "_K_" + i);
        }
        assertFalse(ver[0].exists());
        assertFalse(ver[1].exists());
        assertTrue(ver[2].exists());
        assertTrue(ver[3].exists());
        assertTrue(ver[4].exists());

        fb.setArchiveVersions(1);
        assertTrue(((CopyOrMoveFileArchiver) fb.getFileArchiver()).maintain());
        assertFalse(ver[0].exists());
        assertFalse(ver[1].exists());
        assertFalse(ver[2].exists());
        assertFalse(ver[3].exists());
        assertTrue(ver[4].exists());
    }

    private void modLisaFile(File file, final FileInfo fInfo) {
        TestHelper.changeFile(file);
        scanFolder(getFolderAtLisa());

        final FileInfo scanned = getFolderAtLisa().getFile(fInfo);

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                for (FileInfo fi : getFolderAtBart().getKnownFiles()) {
                    if (fi.isVersionDateAndSizeIdentical(scanned)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
