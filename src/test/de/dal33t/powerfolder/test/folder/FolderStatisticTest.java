package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
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
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, testFile.length());

        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        TestHelper.changeFile(testFile, 500);
        scanFolder(getFolderAtHomer());
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtMarge());
        scanFolder(getFolderAtLisa());
        scanFolder(getFolderAtMaggie());
        // Give the members time broadcast changes
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(500);
        assertTotalSyncPercentage(20);
        assertIncomingFiles(1, 0, 1, 1, 1);
        assertMemberSizesActual(1000, 500, 1000, 1000, 1000);
        assertSyncPercentages(0, 100, 0, 0, 0);

        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        waitForCompletedDownloads(2, 0, 2, 2, 2);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, testFile.length());
    }

    /**
     * Tests the sync percentage with one file that gets updated
     */
    public void testOneFileSameVersion() {
        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());

        File testFileLisa = TestHelper.createTestFile(getFolderAtLisa()
            .getLocalBase(), testFileBart.getName(), "TEST CONTENT".getBytes());
        testFileLisa.setLastModified(System.currentTimeMillis() + 1000 * 20);
        scanFolder(getFolderAtLisa());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(12);
        assertTotalSyncPercentage(20);
        assertMemberSizesActual(0, 1000, 0, 12, 0);
        assertMemberSizesInSync(0, 0, 0, 12, 0);
        assertSyncPercentages(0, 0, 0, 100, 0);

        // Bring them in sync
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        waitForCompletedDownloads(1, 1, 1, 0, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 12);
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
        waitForFileListOnTestFolder();
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
        assertMemberSizesActual(0, 1000, 0, 1750, 0);

        assertEquals(1, getFolderAtBart().getStatistic().getTotalFilesCount());
        assertEquals(1, getFolderAtLisa().getStatistic().getTotalFilesCount());

        assertEquals(20.0, getFolderAtBart().getStatistic()
            .getTotalSyncPercentage());
        assertEquals(20.0, getFolderAtLisa().getStatistic()
            .getTotalSyncPercentage());

        // Bring them in sync
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, testFile.length());
    }

    public void testMultipleFiles() {
        int nFiles = 10;
        int totalSize = 0;
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < nFiles; i++) {
            File f = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase(), 100);
            files.add(f);
            totalSize += f.length();
        }
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        scanFolder(getFolderAtBart());
        waitForCompletedDownloads(nFiles, 0, nFiles, nFiles, nFiles);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(nFiles, totalSize);

        // Change folder and let sync
        for (File file : files) {
            TestHelper.changeFile(file, 200);
        }
        scanFolder(getFolderAtBart());
        int dls = nFiles * 2;
        waitForCompletedDownloads(dls, 0, dls, dls, dls);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(nFiles, totalSize * 2);
        
        // Bart

        // assertTotalFileCount(nFiles);
        // assertTotalSize(totalSize);
        // assertIncomingFiles(0, 0, 0, 0, 0);
        // assertMemberSizesActual(totalSize, totalSize, totalSize, totalSize,
        // totalSize);
        // assertMemberSizesInSync(totalSize, totalSize, totalSize, totalSize,
        // totalSize);
        // assertTotalSyncPercentage(100);
        // assertSyncPercentages(100, 100, 100, 100, 100);
    }

    public void testIncomingFiles() {
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(testFile.length());
        assertTotalSyncPercentage(20);
        assertMemberSizesActual(0, 1000, 0, 0, 0);
        assertSyncPercentages(0, 100, 0, 0, 0);
        assertIncomingFiles(1, 0, 1, 1, 1);

        // Bring them in sync
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, testFile.length());
    }

    public void testDeletedFiles() {
        // 1) Sync ONE file to all simpsons
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        File testFile1 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1000);
        scanFolder(getFolderAtBart());
        assertEquals(0, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        assertEquals(1000, getFolderAtBart().getKnownFiles().iterator().next()
            .getSize());
        assertFalse(getFolderAtBart().getKnownFiles().iterator().next()
            .isDeleted());
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 1000);

        // 2) Switch to manual sync and delete the file at Bart
        setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        testFile1.delete();
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        assertEquals(0, getFolderAtBart().getKnownFiles().iterator().next()
            .getSize());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isDeleted());

        // 3) Create a SECOND file at Bart. File does not get sync (Still
        // MANUAL DOWNLOAD)
        File testFile2 = TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 500);
        scanFolder(getFolderAtBart());
        assertEquals(2, getFolderAtBart().getKnownFilesCount());
        waitForFileListOnTestFolder();
        forceStatsCals();

        assertTotalFileCount(1);
        assertTotalSize(500);
        assertTotalSyncPercentage(20);
        assertMemberSizesInSync(0, 500, 0, 0, 0);
        // They have still the old file and not the new one.
        assertMemberSizesActual(1000, 500, 1000, 1000, 1000);
        assertSyncPercentages(0, 100, 0, 0, 0);
        assertIncomingFiles(1, 0, 1, 1, 1);

        // 4) Let them sync all changes. 1 deleted file and 1 existing file
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        waitForCompletedDownloads(2, 0, 2, 2, 2);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 500);
    }
    
    public void testAutodownload() {
        // 1) Sync ONE file to all simpsons
        setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        File testFile1 = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 500);
        scanFolder(getFolderAtBart());
        waitForCompletedDownloads(1, 0, 1, 1, 1);
        waitForFileListOnTestFolder();
        forceStatsCals();
        assertAllInSync(1, 500);

        // 2) Set to auto-download and delete the file.
        setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        testFile1.delete();
        scanFolder(getFolderAtBart());
        waitForFileListOnTestFolder();
        forceStatsCals();
        
        assertTotalFileCount(0);
        assertTotalSize(0);
        // 0 total files = 100% sync
        assertTotalSyncPercentage(100);
        assertMemberSizesInSync(0, 0, 0, 0, 0);
        // They have still the old file and not the new one.
        assertMemberSizesActual(500, 0, 500, 500, 500);
        assertSyncPercentages(100, 100, 100, 100, 100);
        // No incoming files! Newest is DELETED @ bart!
        assertIncomingFiles(0, 0, 0, 0, 0);
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
        getFolderAtMarge().getBlacklist().removePattern(fD.getName());
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
     */
    public void xtestMultipleFiles() {
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

        assertMemberSizesActual(totalSize, totalSize, 0, totalSize, 0);
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

        assertMemberSizesActual(testFile.length(), testFile.length(), testFile
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

    private void assertMemberSizesInSync(long homer, long bart, long marge,
        long lisa, long maggie)
    {
        assertMemberSizesInSync(getFolderAtHomer(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtBart(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtMarge(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtLisa(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesInSync(getFolderAtMaggie(), homer, bart, marge, lisa,
            maggie);
    }

    private void assertMemberSizesInSync(Folder folder, long homer, long bart,
        long marge, long lisa, long maggie)
    {
        assertEquals(homer, folder.getStatistic().getSizeInSync(
            getContollerHomer().getMySelf()));
        assertEquals(bart, folder.getStatistic().getSizeInSync(
            getContollerBart().getMySelf()));
        assertEquals(marge, folder.getStatistic().getSizeInSync(
            getContollerMarge().getMySelf()));
        assertEquals(lisa, folder.getStatistic().getSizeInSync(
            getContollerLisa().getMySelf()));
        assertEquals(maggie, folder.getStatistic().getSizeInSync(
            getContollerMaggie().getMySelf()));
    }

    private void assertMemberSizesActual(long homer, long bart, long marge,
        long lisa, long maggie)
    {
        assertMemberSizesActual(getFolderAtHomer(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtBart(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtMarge(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtLisa(), homer, bart, marge, lisa,
            maggie);
        assertMemberSizesActual(getFolderAtMaggie(), homer, bart, marge, lisa,
            maggie);
    }

    private void assertMemberSizesActual(Folder folder, long homer, long bart,
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

    private void waitForFileListOnTestFolder() {
        long start = System.currentTimeMillis();
        waitForFilelistReceived(getFolderAtHomer());
        waitForFilelistReceived(getFolderAtBart());
        waitForFilelistReceived(getFolderAtMarge());
        waitForFilelistReceived(getFolderAtLisa());
        waitForFilelistReceived(getFolderAtMaggie());
        long took = System.currentTimeMillis() - start;
        // System.err.println("took " + took);
        // .waitMilliSeconds(500);
    }

    /**
     * Waits till all Simpsons received the filelist of the given folder.
     * 
     * @param folder
     */
    private void waitForFilelistReceived(final Folder folder) {
        TestHelper.waitForCondition(10, new ConditionWithMessage() {

            public String message() {
                return "not identical!";
            }

            public boolean reached() {
                Collection<FileInfo> filesLocal = folder.getKnownFiles();
                MemberInfo source = folder.getController().getMySelf()
                    .getInfo();

                Collection<FileInfo> filesOnHomer = getFileList(
                    getContollerHomer(), folder.getInfo(), source);
                Collection<FileInfo> filesOnBart = getFileList(
                    getContollerBart(), folder.getInfo(), source);
                Collection<FileInfo> filesOnMarge = getFileList(
                    getContollerMarge(), folder.getInfo(), source);
                Collection<FileInfo> filesOnLisa = getFileList(
                    getContollerLisa(), folder.getInfo(), source);
                Collection<FileInfo> filesOnMaggie = getFileList(
                    getContollerMaggie(), folder.getInfo(), source);

                return identicalFileList(filesLocal, filesOnHomer)
                    && identicalFileList(filesLocal, filesOnBart)
                    && identicalFileList(filesLocal, filesOnMarge)
                    && identicalFileList(filesLocal, filesOnLisa)
                    && identicalFileList(filesLocal, filesOnMaggie);
            }
        });
    }

    private Collection<FileInfo> getFileList(Controller controller,
        FolderInfo foInfo, MemberInfo source)
    {
        Folder folder = controller.getFolderRepository().getFolder(foInfo);
        return folder.getFilesAsCollection(source.getNode(controller));
    }

    private boolean identicalFileList(Collection<FileInfo> filesA,
        Collection<FileInfo> filesB)
    {
        if (filesA == null || filesB == null) {
            return false;
        }
        if (filesA.size() != filesB.size()) {
            return false;
        }
        List<FileInfo> listA = new ArrayList<FileInfo>(filesA);
        List<FileInfo> listB = new ArrayList<FileInfo>(filesB);
        for (FileInfo fileB : listB) {
            int atA = listA.indexOf(fileB);
            if (atA < 0) {
                return false;
            }
            FileInfo fileA = listA.get(atA);
            if (!fileA.isCompletelyIdentical(fileB)) {
                return false;
            }
        }
        return true;
    }

    private void waitForCompletedDownloads(final int h, final int b,
        final int mar, final int l, final int mag)
    {
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerHomer().getTransferManager()
                    .getCompletedDownloadsCollection().size() == h
                    && getContollerBart().getTransferManager()
                        .getCompletedDownloadsCollection().size() == b
                    && getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size() == mar
                    && getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size() == l
                    && getContollerMaggie().getTransferManager()
                        .getCompletedDownloadsCollection().size() == mag;
            }

            public String message() {
                return "Completed downloads. Homer: "
                    + getContollerHomer().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Bart: "
                    + getContollerBart().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Marge: "
                    + getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Lisa: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Maggie: "
                    + getContollerMaggie().getTransferManager()
                        .getCompletedDownloadsCollection().size();
            }
        });
    }

    private void assertAllInSync(int totalFiles, long totalSize) {
        assertTotalFileCount(totalFiles);
        assertTotalSize(totalSize);
        assertTotalSyncPercentage(100);
        assertMemberSizesInSync(totalSize, totalSize, totalSize, totalSize,
            totalSize);
        assertMemberSizesActual(totalSize, totalSize, totalSize, totalSize,
            totalSize);
        assertSyncPercentages(100, 100, 100, 100, 100);
        assertIncomingFiles(0, 0, 0, 0, 0);
    }
}
