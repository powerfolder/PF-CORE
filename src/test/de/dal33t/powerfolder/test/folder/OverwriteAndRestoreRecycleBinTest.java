package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.event.RecycleBinConfirmEvent;
import de.dal33t.powerfolder.event.RecycleBinConfirmationHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;

public class OverwriteAndRestoreRecycleBinTest extends TwoControllerTestCase {
    private static final String BASEDIR1 = "build/test/ControllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/ControllerLisa/testFolder";

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("OverwriteAndRestoreRecycleBin.setUp()");
        super.setUp();
        makeFriends();

    }

    /**
     * Test the overwrite of file (due to sync) results in copy of old one in
     * RecycleBin. After that the file is restored.
     */
    public void testOverwriteToRecycleAndRestore() {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        joinFolder(testFolder, new File(BASEDIR1), new File(BASEDIR2));
        final Folder folderAtBart = getContollerBart().getFolderRepository()
            .getFolder(testFolder);
        final Folder folderAtLisa = getContollerLisa().getFolderRepository()
            .getFolder(testFolder);
        folderAtBart.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        folderAtLisa.setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        final File testFileBart = TestHelper.createRandomFile(folderAtBart
            .getLocalBase());
        
        folderAtBart.forceScanOnNextMaintenance();
        folderAtBart.maintain();
        
        final FileInfo fInfoBart = folderAtBart.getFiles()[0];

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return folderAtLisa.getFilesCount() >= 1;
            }
        });
        assertEquals(1, folderAtLisa.getFilesCount());
        final FileInfo fInfoLisa = folderAtLisa.getFiles()[0];
        final File testFileLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());

        assertTrue(fInfoLisa.completelyIdentical(fInfoBart));
        assertEquals(testFileBart.length(), testFileLisa.length());

        // overwrite file at Bart
        TestHelper.createTestFile(folderAtBart.getLocalBase(), testFileBart
            .getName(), new byte[]{6, 5, 6, 7});
        folderAtBart.forceScanOnNextMaintenance();
        folderAtBart.maintain();
        
        TestHelper.waitMilliSeconds(500);

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return fInfoLisa.completelyIdentical(fInfoBart)
                    && (testFileBart.length() == testFileLisa.length());
            }
        });

        RecycleBin binAtLisa = getContollerLisa().getRecycleBin();
        assertTrue(binAtLisa.isInRecycleBin(fInfoLisa));
        assertEquals(1, binAtLisa.countAllRecycledFiles());

        FileInfo infoAtLisa = binAtLisa.getAllRecycledFiles().get(0);

        // add NO reply to overwrite question on restore
        binAtLisa
            .setRecycleBinConfirmationHandler(new RecycleBinConfirmationHandlerNo());
        assertFalse(binAtLisa.restoreFromRecycleBin(infoAtLisa));
        // file should still be in recycle bin
        assertEquals(1, binAtLisa.countAllRecycledFiles());

        // add Yes reply to overwrite question on restore
        binAtLisa
            .setRecycleBinConfirmationHandler(new RecycleBinConfirmationHandlerYes());
        assertTrue(binAtLisa.restoreFromRecycleBin(infoAtLisa));
        assertEquals(0, binAtLisa.countAllRecycledFiles());
        TestHelper.waitMilliSeconds(500);

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return fInfoLisa.completelyIdentical(fInfoBart)
                    && (testFileBart.length() == testFileLisa.length());
            }
        });
    }

    class RecycleBinConfirmationHandlerNo implements
        RecycleBinConfirmationHandler
    {
        public boolean confirmOverwriteOnRestore(
            RecycleBinConfirmEvent recycleBinConfirmEvent)
        {
            return false;
        }
    }

    class RecycleBinConfirmationHandlerYes implements
        RecycleBinConfirmationHandler
    {
        public boolean confirmOverwriteOnRestore(
            RecycleBinConfirmEvent recycleBinConfirmEvent)
        {
            return true;
        }
    }
}
