/* $Id$
 */
package de.dal33t.powerfolder.test.transfer;

import java.io.File;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper.Condition;

/**
 * Tests the correct updating of files.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileUpdateTest extends TwoControllerTestCase {
    
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
        File fileAtBart = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "TestInitalFile.bin",
            "A older version of the file".getBytes());
        // File @ Bart was modified one days before (=newer)
        fileAtBart.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60
            * 24 * 1);

        File fileAtLisa = TestHelper.createTestFile(
            getFolderAtLisa().getLocalBase(), "TestInitalFile.bin",
            "My newest version of the file".getBytes());
        // File @ Lisa was modified three days before (=older)
        fileAtLisa.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60
            * 24 * 3);

        // Let them scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        getFolderAtLisa().forceScanOnNextMaintenance();
        getFolderAtLisa().maintain();
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getFilesCount() == 1
                    && getFolderAtLisa().getFilesCount() == 1;

            }
        });

        // Now let them sync with auto-download
        getFolderAtBart().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        // Let the copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() == 1;

            }
        });

        // Test barts file (=newer)
        FileInfo fileInfoAtBart = getFolderAtBart().getFiles()[0];
        assertEquals(fileAtBart.getName(), fileInfoAtBart.getFilenameOnly());
        assertEquals(fileAtBart.length(), fileInfoAtBart.getSize());
        assertEquals(fileAtBart.lastModified(), fileInfoAtBart
            .getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(), fileInfoAtBart
            .getModifiedBy());

        // Test lisas file (=should be override by barts newer file)
        FileInfo fileInfoAtLisa = getFolderAtLisa().getFiles()[0];
        assertEquals(fileAtBart.getName(), fileInfoAtLisa.getFilenameOnly());
        assertEquals(fileAtBart.length(), fileInfoAtLisa.getSize());
        assertEquals(fileAtBart.lastModified(), fileInfoAtLisa
            .getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(), fileInfoAtLisa
            .getModifiedBy());
    }

}
