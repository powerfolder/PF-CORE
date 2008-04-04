package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the correct synchronization of file deletions.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @version $Revision: 1.5 $
 */
public class DeletionSyncTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        System.out.println("DeletionSyncTest.setUp()");
        super.setUp();
        connectBartAndLisa();
        // Note: Don't make friends, SYNC_PC profile should sync even if PCs are
        // not friends.
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    public void xtestMultipleDeleteAndRestore() throws Exception {
        for (int i = 0; i < 40; i++) {
            testDeleteAndRestore();
            tearDown();
            setUp();
        }
    }

    /**
     * TRAC #394
     */
    public void testDeleteAndRestore() {
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        final Member lisaAtBart = getContollerBart().getNodeManager().getNode(
            getContollerLisa().getMySelf().getInfo());

        // Create a file with version = 1
        final File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());
        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        assertEquals(0, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        TestHelper.changeFile(testFileBart);
        TestHelper.waitMilliSeconds(50);
        scanFolder(getFolderAtBart());

        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        assertEquals(1, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());

        // Let Lisa download the file via auto-dl and broadcast the change to
        // bart
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 1
                    && lisaAtBart.getLastFileListAsCollection(
                        getFolderAtLisa().getInfo()).size() >= 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFiles().iterator().next()
            .getVersion());

        // Now delete the file @ bart. This should NOT be mirrored to Lisa! (She
        // has only auto-dl, no deletion sync)
        assertTrue(testFileBart.exists());
        assertTrue(testFileBart.canWrite());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return testFileBart.delete();
            }
        });
        scanFolder(getFolderAtBart());
        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        assertEquals(2, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());

        // @ Lisa, still the "old" version (=1).
        File testFileLisa = getFolderAtLisa().getKnownFiles().iterator().next()
            .getDiskFile(getContollerLisa().getFolderRepository());
        assertEquals(1, getFolderAtLisa().getKnownFiles().iterator().next()
            .getVersion());
        assertFileMatch(testFileLisa, getFolderAtLisa().getKnownFiles()
            .iterator().next(), getContollerLisa());

        // Now let Bart re-download the file! -> Manually triggerd
        FileInfo testfInfoBart = getFolderAtBart().getKnownFiles().iterator()
            .next();
        assertTrue(""
            + lisaAtBart.getLastFileListAsCollection(getFolderAtBart()
                .getInfo()), lisaAtBart.hasFile(testfInfoBart));
        assertTrue(lisaAtBart.isCompleteyConnected());
        List<Member> sources = getContollerBart().getTransferManager()
            .getSourcesFor(testfInfoBart);
        assertNotNull(sources);
        assertEquals(1, sources.size());
        // assertEquals(1, getFolderAtBart().getConnectedMembers()[0]
        // .getFile(testfInfoBart).getVersion());
        DownloadManager source = getContollerBart().getTransferManager()
            .downloadNewestVersion(
                getFolderAtBart().getKnownFiles().iterator().next());
        assertNotNull("Download source is null", source);

        TestHelper.waitMilliSeconds(200);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtBart().getKnownFiles().iterator().next()
                    .getVersion();
            }
        });

        // Check the file.
        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        // TODO: Discuss: The downloaded version should be 3 (?).
        // Version 3 of the file = restored.
        assertEquals(1, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testSingleFileDeleteSync() {
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());

        FileInfo fInfoBart = getFolderAtBart().getKnownFiles().iterator()
            .next();

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        File testFileLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());

        assertTrue(fInfoLisa.isCompletelyIdentical(fInfoBart));
        assertEquals(testFileBart.length(), testFileLisa.length());

        // Now delete the file at lisa
        assertTrue(testFileLisa.delete());
        scanFolder(getFolderAtLisa());

        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        assertEquals(1, getFolderAtLisa().getKnownFiles().iterator().next()
            .getVersion());
        assertTrue(getFolderAtLisa().getKnownFiles().iterator().next()
            .isDeleted());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFiles().iterator().next()
                    .isDeleted();
            }
        });

        assertEquals(1, getFolderAtBart().getKnownFilesCount());
        assertEquals(1, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isDeleted());

        // Assume only 1 file (=PowerFolder system dir)
        assertEquals(1, getFolderAtBart().getLocalBase().list().length);
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testMultipleFileDeleteSync() {
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());

        // Copy
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= nFiles;
            }
        });
        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());

        // Now delete the file at lisa
        FileInfo[] fInfosLisa = getFolderAtLisa().getKnowFilesAsArray();
        for (int i = 0; i < fInfosLisa.length; i++) {
            assertTrue(fInfosLisa[i].getDiskFile(
                getContollerLisa().getFolderRepository()).delete());
        }
        scanFolder(getFolderAtLisa());

        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());
        fInfosLisa = getFolderAtLisa().getKnowFilesAsArray();
        for (int i = 0; i < fInfosLisa.length; i++) {
            assertEquals(1, fInfosLisa[i].getVersion());
            assertTrue(fInfosLisa[i].isDeleted());
        }

        // Wait to sync the deletions
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnowFilesAsArray()[nFiles - 1]
                    .isDeleted();
            }
        });
        TestHelper.waitMilliSeconds(500);

        // Test the correct deletions state at bart
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());
        FileInfo[] fInfosBart = getFolderAtBart().getKnowFilesAsArray();
        for (int i = 0; i < fInfosBart.length; i++) {
            assertTrue(fInfosBart[i].isDeleted());
            assertEquals(1, fInfosBart[i].getVersion());
        }

        // Assume only 1 file (=PowerFolder system dir)
        assertEquals(1, getFolderAtBart().getLocalBase().list().length);

    }

    /**
     * Complex scenario to test the the correct deletion synchronization.
     * <p>
     * Related tickets: #9
     */
    public void testDeletionSyncScenario() {
        // file "host" and "client"
        getFolderAtBart().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        getFolderAtLisa().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        File file1 = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "/TestFile.txt",
            "This are the contents of the testfile".getBytes());
        File file2 = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "/TestFile2.txt",
            "This are the contents  of the 2nd testfile".getBytes());
        File file3 = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "/sub/sub/TestFile3.txt",
            "This are the contents of the 3nd testfile".getBytes());

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals(3, getFolderAtBart().getKnownFilesCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 3;
            }
        });

        // Test ;)
        assertEquals(3, getFolderAtLisa().getKnownFilesCount());

        // Version should be the 0 for new files
        for (FileInfo fileInfo : getFolderAtBart().getKnowFilesAsArray()) {
            assertEquals(0, fileInfo.getVersion());
        }

        // Version should be the 0 for new files
        for (FileInfo fileInfo : getFolderAtLisa().getKnowFilesAsArray()) {
            assertEquals(0, fileInfo.getVersion());
        }

        assertEquals(
            getContollerBart().getRecycleBin().countAllRecycledFiles(), 0);
        assertEquals(
            getContollerLisa().getRecycleBin().countAllRecycledFiles(), 0);

        assertTrue(file1.delete());
        assertTrue(file2.delete());
        assertTrue(file3.delete());

        assertFalse(file1.exists());
        assertFalse(file2.exists());
        assertFalse(file3.exists());

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        // all 3 must be deleted
        FileInfo[] folder1Files = getFolderAtBart().getKnowFilesAsArray();
        for (FileInfo fileInfo : folder1Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
        }

        // Give them time to remote deletion
        TestHelper.waitMilliSeconds(3000);

        // all 3 must be deleted remote
        FileInfo[] folder2Files = getFolderAtLisa().getKnowFilesAsArray();
        for (FileInfo fileInfo : folder2Files) {
            assertTrue(fileInfo.isDeleted());
            assertEquals(1, fileInfo.getVersion());
            File file = getFolderAtLisa().getDiskFile(fileInfo);
            assertFalse(file.exists());
        }
        assertEquals(
            getContollerLisa().getRecycleBin().countAllRecycledFiles(), 3);

        // switch profiles
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        getFolderAtBart().setSyncProfile(SyncProfile.SYNCHRONIZE_PCS);

        RecycleBin recycleBin = getContollerLisa().getRecycleBin();
        List<FileInfo> deletedFilesAtLisa = getContollerLisa().getRecycleBin()
            .getAllRecycledFiles();
        for (FileInfo deletedFileAtLisa : deletedFilesAtLisa) {
            recycleBin.restoreFromRecycleBin(deletedFileAtLisa);
        }

        // all 3 must not be deleted at folder2
        for (FileInfo fileAtLisa : getFolderAtLisa().getKnowFilesAsArray()) {
            assertFalse(fileAtLisa.isDeleted());
            assertEquals(2, fileAtLisa.getVersion());
            assertEquals(getContollerLisa().getMySelf().getInfo(), fileAtLisa
                .getModifiedBy());
            File file = getFolderAtLisa().getDiskFile(fileAtLisa);
            assertTrue(file.exists());
            assertEquals(fileAtLisa.getSize(), file.length());
            assertEquals(fileAtLisa.getModifiedDate().getTime(), file
                .lastModified());
        }

        getContollerBart().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Give them time to undelete sync (means downloading;)
        TestHelper.waitMilliSeconds(3000);

        // all 3 must not be deleted anymore at folder1
        for (FileInfo fileInfo : getFolderAtBart().getKnowFilesAsArray()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
            assertTrue(fileInfo.getDiskFile(
                getContollerBart().getFolderRepository()).exists());
        }

        for (FileInfo fileInfo : getFolderAtLisa().getKnowFilesAsArray()) {
            assertEquals(2, fileInfo.getVersion());
            assertFalse(fileInfo.isDeleted());
            assertTrue(fileInfo.getDiskFile(
                getContollerLisa().getFolderRepository()).exists());
        }
    }

    /**
     * EVIL: #666
     */
    public void testDeleteCustomProfile() {
        getFolderAtBart().setSyncProfile(
            new SyncProfile(false, false, true, true, 60));
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        final Member lisaAtBart = getContollerBart().getNodeManager().getNode(
            getContollerLisa().getMySelf().getInfo());

        // Create a file with version = 1
        final File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());
        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        assertEquals(0, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        TestHelper.changeFile(testFileBart);
        TestHelper.waitMilliSeconds(50);
        scanFolder(getFolderAtBart());

        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        assertEquals(1, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());

        // Let Lisa download the file via auto-dl and broadcast the change to
        // bart
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 1
                    && lisaAtBart.getLastFileListAsCollection(
                        getFolderAtLisa().getInfo()).size() >= 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFiles().iterator().next()
            .getVersion());

        // Now delete the file @ lisa.
        final File testFileLisa = getFolderAtLisa().getKnownFiles().iterator()
            .next().getDiskFile(getContollerLisa().getFolderRepository());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return testFileLisa.delete();
            }
        });
        scanFolder(getFolderAtLisa());

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return !testFileBart.exists();
            }
        });
        assertFalse(testFileBart.exists());
        assertEquals(2, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
    }
}
