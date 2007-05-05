/* $Id$
 */
package de.dal33t.powerfolder.test.transfer;

import java.io.File;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.Condition;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;

/**
 * Tests the correct updating of files.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileUpdateTest extends TwoControllerTestCase {
    private static final String TEST_FILENAME = "TestFile.bin";
    private static final byte[] SMALLER_FILE_CONTENTS = "Changed/smaller"
        .getBytes();
    private static final byte[] LONG_FILE_CONTENTS = "Some test file with long contents"
        .getBytes();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    /**
     * Test the inital sync of two files with same name and place but diffrent
     * modification dates.
     * <p>
     * Ticket #345
     */
    public void testInitalSync() {
        File fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), "TestInitalFile.bin",
            "A older version of the file".getBytes());
        // File @ Bart was modified one days before (=newer)
        fileAtBart.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60
            * 24 * 1);

        File fileAtLisa = TestHelper.createTestFile(getFolderAtLisa()
            .getLocalBase(), "TestInitalFile.bin",
            "My newest version of the file".getBytes());
        // File @ Lisa was modified three days before (=older)
        fileAtLisa.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60
            * 24 * 3);

        // Let them scan the new content
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFilesCount() == 1
                    && getFolderAtLisa().getKnownFilesCount() == 1;
            }
        });

        assertFileMatch(fileAtBart, getFolderAtBart().getKnownFiles()[0],
            getContollerBart());
        assertFileMatch(fileAtLisa, getFolderAtLisa().getKnownFiles()[0],
            getContollerLisa());

        // Now let them sync with auto-download
        getFolderAtBart().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        // Let the copy
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() == 1;
            }
        });

        // Test barts file (=newer)
        FileInfo fileInfoAtBart = getFolderAtBart().getKnownFiles()[0];
        assertEquals(0, fileInfoAtBart.getVersion());
        assertEquals(fileAtBart.getName(), fileInfoAtBart.getFilenameOnly());
        assertEquals(fileAtBart.length(), fileInfoAtBart.getSize());
        assertEquals(fileAtBart.lastModified(), fileInfoAtBart
            .getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(), fileInfoAtBart
            .getModifiedBy());

        // Test lisas file (=should be override by barts newer file)
        FileInfo fileInfoAtLisa = getFolderAtLisa().getKnownFiles()[0];
        assertFileMatch(fileAtLisa, getFolderAtLisa().getKnownFiles()[0],
            getContollerLisa());
        assertTrue(fileInfoAtLisa.inSyncWithDisk(fileAtLisa));
        assertEquals(fileAtBart.getName(), fileInfoAtLisa.getFilenameOnly());
        assertEquals(fileAtBart.length(), fileInfoAtLisa.getSize());
        assertEquals(fileAtBart.lastModified(), fileInfoAtLisa
            .getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(), fileInfoAtLisa
            .getModifiedBy());
    }

    /**
     * Tests the when the internal db is out of sync with the disk. Ticket #387
     */
    public void testFileChangedOnDisk() {
        File fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), TEST_FILENAME, LONG_FILE_CONTENTS);
        // Scan the file
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownFilesCount());

        // Change the file on disk. make it shorter.
        fileAtBart = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), TEST_FILENAME,
            SMALLER_FILE_CONTENTS);
        // Now the DB of Barts folder is out of sync with the disk!
        // = disk
        assertEquals(SMALLER_FILE_CONTENTS.length, fileAtBart.length());
        // = db
        assertEquals(LONG_FILE_CONTENTS.length, getFolderAtBart().getKnownFiles()[0]
            .getSize());

        assertNotSame(fileAtBart.length(), getFolderAtBart().getKnownFiles()[0]
            .getSize());

        // Wait for copy
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        // Wait that Lisa has a stuck dl
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countNumberOfDownloads(getFolderAtLisa()) > 0;
            }
        });

        // Now trigger barts folders mainteance, detect new file.
        getContollerBart().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(1000);
        // and trigger Lisas transfer check, detect broken download
        getContollerLisa().getTransferManager().triggerTransfersCheck();
        TestHelper.waitMilliSeconds(1000);

        // Download stays forever
        assertEquals("Lisa has a stuck download", 0, getContollerLisa()
            .getTransferManager().countNumberOfDownloads(getFolderAtLisa()));
    }
}
