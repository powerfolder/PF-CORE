package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;

/**
 * Test for FolderStatistic.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderStatisticTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    public void testOneFile() {
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());
        forceStatsCalc(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getStatistic().getTotalFilesCount());
        assertEquals(testFile.length(), getFolderAtBart().getStatistic()
            .getTotalSize());

    }

    public void testMultipleFiles() {
        long totalSize = 0;
        for (int i = 0; i < 50; i++) {
            File testFile = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase());
            totalSize += testFile.length();
        }

        scanFolder(getFolderAtBart());
        forceStatsCalc(getFolderAtBart());
        assertEquals(50, getFolderAtBart().getStatistic().getTotalFilesCount());
        assertEquals(totalSize, getFolderAtBart().getStatistic()
            .getTotalSize());
    }

    private static final void forceStatsCalc(Folder folder) {
        folder.getStatistic().calculate0();
    }
}
