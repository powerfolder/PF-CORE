package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for FolderStatistic.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderStatisticTest extends FiveControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(tryToConnectSimpsons());
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    /**
     * Tests the sync percentage with one file that gets updated
     */
    public void testOneFile() {
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtHomer().getKnownFilesCount() == 1
                    && getFolderAtBart().getKnownFilesCount() == 1
                    && getFolderAtMarge().getKnownFilesCount() == 1
                    && getFolderAtLisa().getKnownFilesCount() == 1
                    && getFolderAtMaggie().getKnownFilesCount() == 1;
            }
        });
        // Give the members time broadcast changes
        TestHelper.waitMilliSeconds(500);

        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(testFile.length());
        assertTotalSyncPercentage(100);
        assertMemberSizes(1000, 1000, 1000, 1000, 1000);
        assertSyncPercentages(100, 100, 100, 100, 100);

        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        TestHelper.changeFile(testFile, 500);
        scanFolder(getFolderAtHomer());
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtMarge());
        scanFolder(getFolderAtLisa());
        scanFolder(getFolderAtMaggie());
        // Give the members time broadcast changes
        TestHelper.waitMilliSeconds(500);
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(500);
        assertTotalSyncPercentage(20);
        assertIncomingFiles(1, 0, 1, 1, 1);
        assertMemberSizes(1000, 500, 1000, 1000, 1000);
        assertSyncPercentages(0, 100, 0, 0, 0);
    }

    public void testInitialSync() throws IOException {
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        File testFileAtLisa = new File(getFolderAtLisa().getLocalBase(),
            testFile.getName());
        FileUtils.copyFile(testFile, testFileAtLisa);
        TestHelper.changeFile(testFileAtLisa, 1750);
        testFileAtLisa.setLastModified(testFile.lastModified() - 1000L * 60);

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        // Give the members time broadcast changes
        // TODO Find a better way
        TestHelper.waitMilliSeconds(1000);

        forceStatsCals();

        assertEquals(testFile.length(), getFolderAtBart().getStatistic()
            .getTotalSize());
        assertEquals(testFile.length(), getFolderAtLisa().getStatistic()
            .getTotalSize());

        assertEquals(0, getFolderAtBart().getStatistic()
            .getIncomingFilesCount());
        assertEquals(1, getFolderAtLisa().getStatistic()
            .getIncomingFilesCount());
        assertSyncPercentages(0, 100, 0, 0, 0);
        assertMemberSizes(0, 1000, 0, 1750, 0);

        assertEquals(1, getFolderAtBart().getStatistic().getTotalFilesCount());
        assertEquals(1, getFolderAtLisa().getStatistic().getTotalFilesCount());

        assertEquals(20.0, getFolderAtBart().getStatistic()
            .getTotalSyncPercentage());
        assertEquals(20.0, getFolderAtLisa().getStatistic()
            .getTotalSyncPercentage());
    }

    public void xtestScenario() {
        // Step 1) Distribute file A
        File fA = TestHelper.createRandomFile(
            getFolderAtHomer().getLocalBase(), 100);
        scanFolder(getFolderAtHomer());

        getFolderAtBart().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtMarge().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFilesCount() == 1
                    && getFolderAtMarge().getKnownFilesCount() == 1;
            }
        });
        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        // ----------------------------

        // Step 2) Distribute file B, delete at all members
        File fb = TestHelper.createRandomFile(
            getFolderAtHomer().getLocalBase(), 2);
        scanFolder(getFolderAtHomer());

        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtMarge().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtHomer().getKnownFilesCount() == 2
                    && getFolderAtBart().getKnownFilesCount() == 2
                    && getFolderAtMarge().getKnownFilesCount() == 2;
            }
        });
        assertTrue(fb.delete());
        scanFolder(getFolderAtHomer());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtHomer().getLocalBase().list().length == 3
                    && getFolderAtBart().getLocalBase().list().length == 2
                    && getFolderAtMarge().getLocalBase().list().length == 2;
            }
        });
        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        // ----------------------------

        // Step 3) Copy file D to Bart, Delete at Homer
        getFolderAtBart().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        File fD = TestHelper.createRandomFile(
            getFolderAtHomer().getLocalBase(), 500);
        scanFolder(getFolderAtHomer());
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFiles().size() == 3;
            }
        });
        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        assertTrue(fD.delete());
        scanFolder(getFolderAtHomer());
        // ----------------------------

        // Step 4) Copy file C to Marge
        getFolderAtMarge().getBlacklist().addPattern(fD.getName());
        getFolderAtMarge().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        File fC = TestHelper.createRandomFile(
            getFolderAtHomer().getLocalBase(), 360);
        scanFolder(getFolderAtHomer());
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return getFolderAtMarge().getKnownFiles().size() == 3;
            }
        });
        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        getFolderAtMarge().removePattern(fD.getName());
        // ----------------------------

        // Step 5) Calc stats
        forceStatsCals();

        assertEquals(3, getFolderAtHomer().getStatistic().getTotalFilesCount());
        assertEquals(3, getFolderAtBart().getStatistic().getTotalFilesCount());
        assertEquals(3, getFolderAtMarge().getStatistic().getTotalFilesCount());

        assertEquals(100 + 360 + 500, getFolderAtHomer().getStatistic()
            .getTotalSize());
        assertEquals(100 + 360 + 500, getFolderAtMarge().getStatistic()
            .getTotalSize());
        assertEquals(2, getFolderAtBart().getKnownFilesCount());
        assertEquals(100 + 360 + 500, getFolderAtBart().getStatistic()
            .getTotalSize());

        //        
        // assertEquals(testFile.length(), getFolderAtHomer().getStatistic()
        // .getTotalSize());
        // assertEquals(testFile.length(), getFolderAtBart().getStatistic()
        // .getTotalSize());
        // assertEquals(testFile.length(), getFolderAtMarge().getStatistic()
        // .getTotalSize());
        // assertEquals(testFile.length(), getFolderAtLisa().getStatistic()
        // .getTotalSize());
        // assertEquals(testFile.length(), getFolderAtMaggie().getStatistic()
        // .getTotalSize());
    }

    /**
     * Test the sync calculation with multiple files.
     * 
     * @throws IOException
     */
    public void xtestMultipleFiles() throws IOException {
        final int nFiles = 50;
        long totalSize = 0;
        for (int i = 0; i < nFiles; i++) {
            File testFile = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase());
            totalSize += testFile.length();
        }

        scanFolder(getFolderAtBart());
        getFolderAtHomer().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        connectSimpsons();
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtHomer().getKnownFilesCount() == nFiles
                    && getFolderAtLisa().getKnownFilesCount() == nFiles;
            }
        });
        // Give the members time broadcast changes
        TestHelper.waitMilliSeconds(1000);
        forceStatsCals();

        assertTotalFileCount(nFiles);
        assertTotalSize(totalSize);
        assertTotalSyncPercentage(60);

        assertMemberSizes(totalSize, totalSize, 0, totalSize, 0);
        assertSyncPercentages(100, 100, 0, 100, 0);

        // FileUtils.recursiveDelete(getFolderAtBart().getLocalBase());
        for (File file : getFolderAtBart().getLocalBase().listFiles()) {
            if (file.isFile()) {
                file.delete();
            }
        }
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        getFolderAtMarge().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtMaggie().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        scanFolder(getFolderAtBart());
        
        // Give the members time broadcast changes
        TestHelper.waitMilliSeconds(10000);
        scanFolder(getFolderAtBart());
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(testFile.length());
        assertSyncPercentages(100, 100, 100, 100, 100);
        assertTotalSyncPercentage(100);

        assertMemberSizes(testFile.length(), testFile.length(), testFile
            .length(), testFile.length(), testFile.length());
       
    }

    private final void forceStatsCals() {
        forceStatsCalc(getFolderAtHomer());
        forceStatsCalc(getFolderAtBart());
        forceStatsCalc(getFolderAtMarge());
        forceStatsCalc(getFolderAtLisa());
        forceStatsCalc(getFolderAtMaggie());
    }

    private static final void forceStatsCalc(Folder folder) {
        folder.getStatistic().calculate0();
    }

    private void assertMemberSizes(long homer, long bart, long marge,
        long lisa, long maggie)
    {
        assertMemberSizes(getFolderAtHomer(), homer, bart, marge, lisa, maggie);
        assertMemberSizes(getFolderAtBart(), homer, bart, marge, lisa, maggie);
        assertMemberSizes(getFolderAtMarge(), homer, bart, marge, lisa, maggie);
        assertMemberSizes(getFolderAtLisa(), homer, bart, marge, lisa, maggie);
        assertMemberSizes(getFolderAtMaggie(), homer, bart, marge, lisa, maggie);
    }

    private void assertMemberSizes(Folder folder, long homer, long bart,
        long marge, long lisa, long maggie)
    {
        assertEquals(homer, folder.getStatistic().getSize(
            getContollerHomer().getMySelf()));
        assertEquals(bart, folder.getStatistic().getSize(
            getContollerBart().getMySelf()));
        assertEquals(marge, folder.getStatistic().getSize(
            getContollerMarge().getMySelf()));
        assertEquals(lisa, folder.getStatistic().getSize(
            getContollerLisa().getMySelf()));
        assertEquals(maggie, folder.getStatistic().getSize(
            getContollerMaggie().getMySelf()));
    }

    private void assertSyncPercentages(double homer, double bart, double marge,
        double lisa, double maggie)
    {
        assertSyncPercentages(getFolderAtHomer(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtBart(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtMarge(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtLisa(), homer, bart, marge, lisa,
            maggie);
        assertSyncPercentages(getFolderAtMaggie(), homer, bart, marge, lisa,
            maggie);
    }

    private void assertSyncPercentages(Folder folder, double homer,
        double bart, double marge, double lisa, double maggie)
    {
        assertEquals(homer, folder.getStatistic().getSyncPercentage(
            getContollerHomer().getMySelf()));
        assertEquals(bart, folder.getStatistic().getSyncPercentage(
            getContollerBart().getMySelf()));
        assertEquals(marge, folder.getStatistic().getSyncPercentage(
            getContollerMarge().getMySelf()));
        assertEquals(lisa, folder.getStatistic().getSyncPercentage(
            getContollerLisa().getMySelf()));
        assertEquals(maggie, folder.getStatistic().getSyncPercentage(
            getContollerMaggie().getMySelf()));
    }

    private void assertTotalSize(long totalSize) {
        assertEquals(totalSize, getFolderAtHomer().getStatistic()
            .getTotalSize());
        assertEquals(totalSize, getFolderAtBart().getStatistic().getTotalSize());
        assertEquals(totalSize, getFolderAtMarge().getStatistic()
            .getTotalSize());
        assertEquals(totalSize, getFolderAtLisa().getStatistic().getTotalSize());
        assertEquals(totalSize, getFolderAtMaggie().getStatistic()
            .getTotalSize());
    }

    private void assertTotalFileCount(int nFiles) {
        assertEquals(nFiles, getFolderAtHomer().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtBart().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtMarge().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtLisa().getStatistic()
            .getTotalFilesCount());
        assertEquals(nFiles, getFolderAtMaggie().getStatistic()
            .getTotalFilesCount());
    }

    private void assertTotalSyncPercentage(double totalSync) {
        assertEquals(totalSync, getFolderAtHomer().getStatistic()
            .getTotalSyncPercentage());
        assertEquals(totalSync, getFolderAtBart().getStatistic()
            .getTotalSyncPercentage());
        assertEquals(totalSync, getFolderAtMarge().getStatistic()
            .getTotalSyncPercentage());
        assertEquals(totalSync, getFolderAtLisa().getStatistic()
            .getTotalSyncPercentage());
        assertEquals(totalSync, getFolderAtMaggie().getStatistic()
            .getTotalSyncPercentage());
    }

    private void assertIncomingFiles(int homer, int bart, int marge, int lisa,
        int maggie)
    {
        assertEquals(homer, getFolderAtHomer().getStatistic()
            .getIncomingFilesCount());
        assertEquals(bart, getFolderAtBart().getStatistic()
            .getIncomingFilesCount());
        assertEquals(marge, getFolderAtMarge().getStatistic()
            .getIncomingFilesCount());
        assertEquals(lisa, getFolderAtLisa().getStatistic()
            .getIncomingFilesCount());
        assertEquals(maggie, getFolderAtMaggie().getStatistic()
            .getIncomingFilesCount());
    }
}
