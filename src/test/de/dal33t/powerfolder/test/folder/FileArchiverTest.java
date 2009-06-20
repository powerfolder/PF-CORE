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

        FileInfo fib = fb.getKnowFilesAsArray()[0];

        FileArchiver fa = ArchiveMode.FULL_BACKUP.getInstance(fb);
        try {
            fa.archive(fib, tb, false);
        } catch (IOException e) {
            fail(e.toString());
        }

        File expected = new File(fb.getSystemSubDir(), "archive");
        expected = new File(expected, fib.getName() + "_K_" + fib.getVersion());
        assertTrue(expected.exists());
    }

    public void testBackupOnDownload() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveMode(ArchiveMode.FULL_BACKUP);

        Folder fl = getFolderAtLisa();
        File tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return fb.getKnowFilesAsArray().length > 0;
            }
        });

        FileInfo fib = fb.getKnowFilesAsArray()[0];
        File eBart = new File(fb.getSystemSubDir(), "archive");
        eBart = new File(eBart, fib.getName() + "_K_" + fib.getVersion());
        File eLisa = new File(fl.getSystemSubDir(), "archive");
        eLisa = new File(eLisa, fib.getName() + "_K_" + fib.getVersion());

        modLisaFile(tl, fib);

        assertTrue(eBart.exists());
        assertFalse(eLisa.exists());
    }

    public void testLimitedVersions() {
        final Folder fb = getFolderAtBart();
        fb.setArchiveMode(ArchiveMode.FULL_BACKUP);
        ((CopyOrMoveFileArchiver) fb.getFileArchiver()).setVersionsPerFile(3);

        Folder fl = getFolderAtLisa();
        File tl = TestHelper.createRandomFile(fl.getLocalBase(), 1024);

        scanFolder(fl);

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return fb.getKnowFilesAsArray().length > 0;
            }
        });
        FileInfo fib = fb.getKnowFilesAsArray()[0];

        for (int i = 0; i < 4; i++) {
            modLisaFile(tl, fib);
        }

        File ver[] = new File[4];
        File archdir = new File(fb.getSystemSubDir(), "archive");
        for (int i = 0; i < ver.length; i++) {
            ver[i] = new File(archdir, fib.getName() + "_K_" + i);
        }

        assertFalse(ver[0].exists());
        assertTrue(ver[1].exists());

        modLisaFile(tl, fib);
        assertFalse(ver[1].exists());
        assertTrue(ver[2].exists());

        ((CopyOrMoveFileArchiver) fb.getFileArchiver()).setVersionsPerFile(5);
        modLisaFile(tl, fib);
        assertTrue(ver[2].exists());

        modLisaFile(tl, fib);
        assertTrue(ver[2].exists());

        modLisaFile(tl, fib);
        assertFalse(ver[2].exists());
        assertTrue(ver[3].exists());
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
