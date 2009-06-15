package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

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

        TestHelper.changeFile(tl);
        scanFolder(fl);

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return fb.getKnowFilesAsArray()[0].getVersion() > 0;
            }
        });

        assertTrue(eBart.exists());
        assertFalse(eLisa.exists());
    }
}
