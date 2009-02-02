/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
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
        joinTestFolder(SyncProfile.HOST_FILES);
    }

    public void testDeleteAndRestoreMultiple() throws Exception {
        for (int i = 0; i < 20; i++) {
            testDeleteAndRestore();
            tearDown();
            setUp();
        }
    }

    /**
     * TRAC #394
     */
    public void testDeleteAndRestore() {
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
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
        System.out.println(getFolderAtBart().getKnowFilesAsArray()[0]);

        DownloadManager source = getContollerBart().getTransferManager()
            .downloadNewestVersion(testfInfoBart);
        assertNull("Download source is not null", source);

        // Barts version is 2 (del) and Lisa has 1, so it shouldn't revert back

        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        TestHelper.waitMilliSeconds(200);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 2 == getFolderAtLisa().getKnownFiles().iterator().next()
                    .getVersion();
            }
        });

        // Check the file.
        assertFileMatch(testFileBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        // TODO: Discuss: The downloaded version should be 3 (?).
        // Version 3 of the file = restored.
        // As agreed on IRC, downloadNewestVersion shouldn't download an older
        // version even if a newer
        // one was deleted.
        assertEquals(2, getFolderAtBart().getKnownFiles().iterator().next()
            .getVersion());
        assertTrue(getFolderAtLisa().getKnownFiles().iterator().next()
            .isDeleted());
    }

    /**
     * Tests the synchronization of file deletions of one file.
     */
    public void testSingleFileDeleteSync() {
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

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
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

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
        getFolderAtBart().setSyncProfile(SyncProfile.HOST_FILES);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

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
                return getFolderAtLisa().getKnownFilesCount() >= 3
                    && getContollerBart().getTransferManager()
                        .getCompletedUploadsCollection().size() >= 3;
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

        assertEquals(0, getContollerBart().getRecycleBin()
            .countAllRecycledFiles());
        assertEquals(0, getContollerLisa().getRecycleBin()
            .countAllRecycledFiles());

        assertTrue("Unable to delete: " + file1, file1.delete());
        assertTrue("Unable to delete: " + file2, file2.delete());
        assertTrue("Unable to delete: " + file3, file3.delete());

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
        getFolderAtLisa().setSyncProfile(SyncProfile.HOST_FILES);

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

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Bart incoming: "
                    + getFolderAtBart().getIncomingFiles(true, true);
            }

            public boolean reached() {
                return getFolderAtBart().getIncomingFiles(true, true).size() == getFolderAtLisa()
                    .getKnownFilesCount();
            }
        });

        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getContollerBart().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Give them time to undelete sync (means downloading;)
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countCompletedDownloads() >= getFolderAtBart()
                    .getKnownFilesCount();
            }

            public String message() {
                return "Downloaded files: "
                    + getContollerBart().getTransferManager()
                        .countCompletedDownloads() + " known: "
                    + getFolderAtBart().getKnownFilesCount();
            }
        });

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
            SyncProfile.getSyncProfileByFieldList("false,false,true,true,60"));
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
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

    public void testDbNotInSyncDeletion() throws IOException {
        // Step 1) Create file
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        scanFolder(getFolderAtBart());
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        // 2) Delete and sync deletion
        testFile.delete();
        scanFolder(getFolderAtBart());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isDeleted());

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() == 1;
            }
        });
        assertTrue(getFolderAtLisa().getKnownFiles().iterator().next()
            .isDeleted());
        assertFileMatch(testFile, getFolderAtBart().getKnownFiles().iterator()
            .next(), getContollerBart());
        disconnectBartAndLisa();

        // Now bring PF into the problematic state
        testFile.createNewFile();
        TestHelper.changeFile(testFile);

        // Not scanned yet
        connectBartAndLisa();

        TestHelper.waitMilliSeconds(1000);
        assertTrue(testFile.exists());
    }
}
