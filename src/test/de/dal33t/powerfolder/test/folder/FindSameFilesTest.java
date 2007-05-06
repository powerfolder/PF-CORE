package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.FileUtils;

/**
 * Tests the finding of same files.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FindSameFilesTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    /**
     * Tests the adapting of the filelist from remote. Should find the same file
     * from remote peer.
     * 
     * @throws IOException
     */
    public void testFilelistAdapt() throws IOException {
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), "TestFile.txt");

        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        // File should be found. version: 0
        TestHelper.changeFile(testFile);
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        // File changed. version: 1
        TestHelper.changeFile(testFile);
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        // File changed. version: 2
        TestHelper.changeFile(testFile);
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();
        // File changed. version: 3
        assertEquals(3, getFolderAtBart().getKnownFiles()[0].getVersion());

        // File gets copied to lisa.
        File testFileCopy = new File(getFolderAtLisa().getLocalBase(),
            "TestFile.txt");
        FileUtils.copyFile(testFile, testFileCopy);

        // somehow the copie process is not complete sometimes what results in
        // different filesizes!
        TestHelper.waitMilliSeconds(1000);
        
        // Let lisa scan it.
        getFolderAtLisa().forceScanOnNextMaintenance();
        getFolderAtLisa().maintain();

        // List should have detected the file from bart as the same!
        assertEquals(0, getFolderAtLisa().getIncomingFiles(true).size());

        // File modifications should be adapted from Bart, because same file!
        assertEquals(3, getFolderAtLisa().getKnownFiles()[0].getVersion());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            getFolderAtLisa().getKnownFiles()[0].getModifiedBy());
    }

}
