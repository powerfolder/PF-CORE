/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.problem.FileConflictProblem;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the correct updating of files.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileUpdateTest extends TwoControllerTestCase {
    private static final String TEST_FILENAME = "TestFile.bin";
    private static final byte[] SMALLER_FILE_CONTENTS = "Changed/smaller"
        .getBytes();
    private static final byte[] LONG_FILE_CONTENTS = "Some test file with long contents"
        .getBytes();

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.HOST_FILES);
    }


    private static FileInfo syncedFileInfo(Folder folder, FileInfo expected) {
        FileInfo local = folder.getFile(expected);
        if (local == null || local.isDeleted()
            || !local.isVersionDateAndSizeIdentical(expected))
        {
            return null;
        }
        return local.inSyncWithDisk(local.getDiskFile(folder.getController()
            .getFolderRepository())) ? local : null;
    }

    private static boolean knowsAllFilesOf(Folder source, Folder target,
        int expectedItems)
    {
        Collection<FileInfo> files = source.getKnownFiles();
        if (files.size() != expectedItems) {
            return false;
        }
        for (FileInfo fileInfo : files) {
            if (syncedFileInfo(target, fileInfo) == null) {
                return false;
            }
        }
        return true;
    }

    private void triggerFileRequestingOnBothSides() {
        getContollerBart().getFolderRepository().getFileRequestor()
            .triggerFileRequesting(getFolderAtBart().getInfo());
        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting(getFolderAtLisa().getInfo());
    }

    private void waitForFoldersInSync(int secondsTimeout,
        final int expectedItems)
    {
        TestHelper.waitForCondition(secondsTimeout, new ConditionWithMessage() {
            public boolean reached() {
                triggerFileRequestingOnBothSides();
                return knowsAllFilesOf(getFolderAtBart(), getFolderAtLisa(),
                    expectedItems)
                    && knowsAllFilesOf(getFolderAtLisa(), getFolderAtBart(),
                        expectedItems);
            }

            public String message() {
                return "Expected " + expectedItems
                    + " identical items at both sides. Bart: "
                    + getFolderAtBart().getKnownFiles() + ". Lisa: "
                    + getFolderAtLisa().getKnownFiles();
            }
        });
    }

    private void waitForFileListOf(final Folder folder,
        final Controller remote, final FileInfo fileInfo)
    {
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            private Member remoteMember() {
                for (Member member : folder.getMembersAsCollection()) {
                    if (!member.isMySelf()
                        && member.getId().equals(remote.getMySelf().getId()))
                    {
                        return member;
                    }
                }
                return null;
            }

            public boolean reached() {
                Member member = remoteMember();
                return member != null
                    && member.hasCompleteFileListFor(folder.getInfo())
                    && member.getFile(fileInfo) != null;
            }

            public String message() {
                return "No complete file list of "
                    + remote.getMySelf().getNick() + " containing " + fileInfo
                    + " at " + folder.getController().getMySelf().getNick();
            }
        });
    }

    private static final class DownloadRequestCounter
        extends TransferManagerAdapter
    {
        private final AtomicInteger requested = new AtomicInteger();

        @Override
        public void downloadRequested(TransferManagerEvent event) {
            requested.incrementAndGet();
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

        int requested() {
            return requested.get();
        }
    }

    /**
     * Test the initial sync of two files with same name and place but different
     * modification dates.
     * <p>
     * Ticket #345
     */
    @Test
    public void testInitalSync() throws IOException {
        TestHelper.waitMilliSeconds(500);
        Path fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), "TestInitalFile.bin",
            "A older version of the file".getBytes());
        // File @ Bart was modified one days before (=newer)
        Files.setLastModifiedTime(
            fileAtBart,
            FileTime.fromMillis(System.currentTimeMillis() - 1000 * 60 * 60
                * 24 * 1));

        Path fileAtLisa = TestHelper.createTestFile(getFolderAtLisa()
            .getLocalBase(), "TestInitalFile.bin",
            "My newest version of the file".getBytes());
        // File @ Lisa was modified three days before (=older)
        Files.setLastModifiedTime(
            fileAtLisa,
            FileTime.fromMillis(System.currentTimeMillis() - 1000 * 60 * 60
                * 24 * 3));

        // Let them scan the new content
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());
        final FileInfo scannedAtBart = getFolderAtBart().getKnownFiles()
            .iterator().next();
        final FileInfo scannedAtLisa = getFolderAtLisa().getKnownFiles()
            .iterator().next();
        waitForFileListOf(getFolderAtLisa(), getContollerBart(),
            scannedAtBart);
        waitForFileListOf(getFolderAtBart(), getContollerLisa(),
            scannedAtLisa);
        getFolderAtBart().maintainFolderDB(0);
        getFolderAtLisa().maintainFolderDB(0);

        assertFileMatch(fileAtBart, getFolderAtBart().getKnownFiles()
            .iterator().next(), getContollerBart());
        assertFileMatch(fileAtLisa, getFolderAtLisa().getKnownFiles()
            .iterator().next(), getContollerLisa());

        // Now let them sync with auto-download
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        // Let the copy
        TestHelper.waitForCondition(70, new ConditionWithMessage() {
            public boolean reached() {
                triggerFileRequestingOnBothSides();
                FileInfo atBart = getFolderAtBart().getFile(scannedAtBart);
                return atBart != null && atBart.getVersion() >= 1
                    && syncedFileInfo(getFolderAtLisa(), atBart) != null;
            }

            public String message() {
                return "File at bart: " + getFolderAtBart().getKnownFiles()
                    + ". File at lisa: " + getFolderAtLisa().getKnownFiles();
            }
        });

        // Test barts file (=newer)
        FileInfo fileInfoAtBart = getFolderAtBart().getFile(
            FileInfoFactory.lookupInstance(getFolderAtBart(), fileAtBart));
        assertEquals(1, fileInfoAtBart.getVersion());
        assertEquals(fileAtBart.getFileName().toString(), fileInfoAtBart.getFilenameOnly());
        assertEquals(Files.size(fileAtBart), fileInfoAtBart.getSize());
        assertEquals(Files.getLastModifiedTime(fileAtLisa).toMillis(),
            fileInfoAtBart.getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            fileInfoAtBart.getModifiedBy());

        // Test lisas file (=should be override by barts newer file)
        FileInfo fileInfoAtLisa = FileInfoFactory.lookupInstance(
            getFolderAtLisa(), fileAtLisa);
        fileInfoAtLisa = fileInfoAtLisa.getLocalFileInfo(getContollerLisa()
            .getFolderRepository());
        assertFileMatch(fileAtLisa, fileInfoAtLisa, getContollerLisa());
        assertTrue(fileInfoAtLisa.inSyncWithDisk(fileAtLisa));
        assertEquals(fileAtBart.getFileName().toString(), fileInfoAtLisa.getFilenameOnly());
        assertEquals(Files.size(fileAtBart), fileInfoAtLisa.getSize());
        assertEquals(Files.getLastModifiedTime(fileAtLisa).toMillis(),
            fileInfoAtLisa.getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            fileInfoAtLisa.getModifiedBy());
    }

    /**
     * Tests the when the internal db is out of sync with the disk. Ticket #387
     */
    @Test
    public void testFileChangedOnDisk() throws IOException {
        Path fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), TEST_FILENAME, LONG_FILE_CONTENTS);
        // Scan the file
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());

        // Change the file on disk. make it shorter.
        fileAtBart = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), TEST_FILENAME,
            SMALLER_FILE_CONTENTS);
        // Now the DB of Barts folder is out of sync with the disk!
        // = disk
        assertEquals(SMALLER_FILE_CONTENTS.length, Files.size(fileAtBart));
        // = db
        assertEquals(LONG_FILE_CONTENTS.length, getFolderAtBart()
            .getKnownFiles().iterator().next().getSize());
        assertNotSame(Files.size(fileAtBart), getFolderAtBart().getKnownFiles()
            .iterator().next().getSize());

        final DownloadRequestCounter downloadsAtLisa = new DownloadRequestCounter();
        getContollerLisa().getTransferManager().addListener(downloadsAtLisa);

        // Change sync profile = auto download.
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        // Abort of upload should have been sent to lisa = NO download.
        TestHelper.waitForCondition(70, new ConditionWithMessage() {
            public boolean reached() {
                getContollerLisa().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting(getFolderAtLisa().getInfo());
                return downloadsAtLisa.requested() > 0
                    && getContollerLisa().getTransferManager()
                        .countNumberOfDownloads(getFolderAtLisa()) == 0;
            }

            public String message() {
                return "Downloads requested at lisa: "
                    + downloadsAtLisa.requested() + ", still pending: "
                    + getContollerLisa().getTransferManager()
                        .getActiveDownloads() + ", incoming at lisa: "
                    + getFolderAtLisa().getIncomingFiles();
            }
        });
        assertEquals( 0, getContollerLisa()
            .getTransferManager().countNumberOfDownloads(getFolderAtLisa()),"Lisa has a stuck download");

        // Now trigger barts folders mainteance, detect new file.
        getContollerBart().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(1000);
        // and trigger Lisas transfer check, detect broken download
        getContollerLisa().getTransferManager().triggerTransfersCheck();
        TestHelper.waitMilliSeconds(5000);

        // Download stays forever
        assertEquals( 0, getContollerLisa()
            .getTransferManager().countNumberOfDownloads(getFolderAtLisa()),"Lisa has a stuck download");
    }

    @Test
    public void testFileConflict() {
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        final Path fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), TEST_FILENAME, LONG_FILE_CONTENTS);
        // Scan the file
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());

        final FileInfo initialAtBart = getFolderAtBart().getKnownFiles()
            .iterator().next();
        final Path fileAtLisa = initialAtBart
            .getDiskFile(getContollerLisa().getFolderRepository());
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                triggerFileRequestingOnBothSides();
                return syncedFileInfo(getFolderAtLisa(), initialAtBart) != null
                    && getContollerBart().getTransferManager()
                        .getActiveUploads().isEmpty();
            }

            public String message() {
                return "Lisa did not receive " + initialAtBart.toDetailString()
                    + ". Known at lisa: " + getFolderAtLisa().getKnownFiles();
            }
        });

        disconnectBartAndLisa();

        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileAtBart);
        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileAtLisa);

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        FileInfo fInfoAtBart = getFolderAtBart().getKnownFiles().iterator()
            .next();
        FileInfo fInfoAtLisa = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        assertEquals(
            1, fInfoAtBart.getVersion(),
            "Expected version at bart 1. Got: " + fInfoAtBart.toDetailString());
        assertEquals(
            1, fInfoAtLisa.getVersion(),
            "Expected version at lisa 1. Got: " + fInfoAtLisa.toDetailString());
        assertFalse( fInfoAtBart
            .getModifiedDate().equals(fInfoAtLisa.getModifiedDate()),"Date of conflicting file same");
        assertTrue(
            fInfoAtBart.getModifiedDate().getTime() < fInfoAtLisa
                .getModifiedDate().getTime(),"Date of conflicting file problem");
        assertTrue(
            fInfoAtLisa.isNewerThan(fInfoAtBart),
            "File @ lisa is not newer than on bart. Lisa: "
                + fInfoAtLisa.toDetailString() + ". Bart: "
                + fInfoAtBart.toDetailString());
        assertFalse(
            fInfoAtLisa.getSize() == fInfoAtBart.getSize(),"File size mismatch. Lisa: " + fInfoAtLisa.toDetailString()
            + ". Bart: " + fInfoAtBart.toDetailString());

        // Now we have a conflict: SAME file version, but different modification
        // dates and sizes. In this scenario LISAs file wins

        connectBartAndLisa();
        // Let them sync.
        final FileInfo conflictWinner = fInfoAtLisa;
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                triggerFileRequestingOnBothSides();
                return syncedFileInfo(getFolderAtBart(), conflictWinner) != null
                    && getContollerBart().getTransferManager()
                        .getActiveDownloads().isEmpty()
                    && getContollerLisa().getTransferManager()
                        .getActiveDownloads().isEmpty();
            }

            public String message() {
                return "Bart did not take over Lisas conflict winner "
                    + conflictWinner.toDetailString() + ". Known at bart: "
                    + getFolderAtBart().getKnownFiles();
            }
        });
        // Bart should not longer raise problems
        assertEquals(
            0, getFolderAtBart().getProblems().size(),
            "Expected problems at bart: 0. Got: "
                + getFolderAtBart().getProblems());
        assertEquals(
            0, getFolderAtLisa().getProblems().size(),
            "Expected problems at lisa: 0. Got: "
                + getFolderAtLisa().getProblems());
        assertTrue(
            getFolderAtBart().getFileArchiver()
                .hasArchivedFileInfo(fInfoAtBart),"Old file not in archive @ bart: " + fInfoAtBart);
        FileInfo fInfoArchivedAtBart = getFolderAtBart().getFileArchiver()
            .getArchivedFilesInfos(fInfoAtBart).get(0);
        assertEquals(fInfoAtBart, fInfoArchivedAtBart);
        // HMMM is this good? File might be overwritten on the next update
        assertEquals(fInfoAtBart.getVersion(), fInfoArchivedAtBart.getVersion());
        assertEquals(fInfoAtBart.getModifiedBy(),
            fInfoArchivedAtBart.getModifiedBy());
        assertEquals(fInfoAtBart.getModifiedDate(),
            fInfoArchivedAtBart.getModifiedDate());

        // The old copy should have been distributed.
        waitForFoldersInSync(20, 2);

        // Second case: version is lower, but last modification date if newer
        disconnectBartAndLisa();

        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());
        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());
        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());
        fInfoAtBart = fInfoAtBart
            .getLocalFileInfo(getContollerBart().getFolderRepository());
        assertEquals(
            4, fInfoAtBart.getVersion(),
            "Expected version at bart 4. Got: " + fInfoAtBart.toDetailString());

        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileAtLisa);
        scanFolder(getFolderAtLisa());
        fInfoAtLisa = fInfoAtLisa
            .getLocalFileInfo(getContollerLisa().getFolderRepository());
        assertEquals(
            2, fInfoAtLisa.getVersion(),
            "Expected version at lisa 2. Got: " + fInfoAtLisa.toDetailString());

        boolean conflict = fInfoAtLisa.getVersion() == fInfoAtBart.getVersion()
            && fInfoAtBart.isNewerThan(fInfoAtLisa);
        conflict |= fInfoAtLisa.getVersion() <= fInfoAtBart.getVersion()
            && DateUtil.isNewerFileDateCrossPlattform(
                fInfoAtLisa.getModifiedDate(), fInfoAtBart.getModifiedDate());
        assertTrue( conflict,"Barts: " + fInfoAtBart.toDetailString() + " Lisas: "
            + fInfoAtLisa.toDetailString());

        connectBartAndLisa();
        // The old copy should have been distributed.
        waitForFoldersInSync(20, 3);
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getProblems().isEmpty();
            }

            public String message() {
                return "Problems at lisa: " + getFolderAtLisa().getProblems();
            }
        });
        assertEquals(
                0, getFolderAtLisa().getProblems().size(),
                "Expected problems at lisa: 0. Got: "
                        + getFolderAtLisa().getProblems());
        assertTrue(
            getFolderAtLisa().getFileArchiver()
                .hasArchivedFileInfo(fInfoAtLisa),"Old file not in archive @ lisa: " + fInfoAtLisa);
        FileInfo fInfoArchivedAtLisa = getFolderAtLisa().getFileArchiver()
            .getArchivedFilesInfos(fInfoAtLisa).get(0);
        assertEquals(fInfoAtBart, fInfoArchivedAtLisa);
        // HMMM is this good? File might be overwritten on the next update
        assertEquals(fInfoAtLisa.getVersion(), fInfoArchivedAtLisa.getVersion());
        assertEquals(fInfoAtLisa.getModifiedBy(),
            fInfoArchivedAtLisa.getModifiedBy());
        assertEquals(fInfoAtLisa.getModifiedDate(),
            fInfoArchivedAtLisa.getModifiedDate());
    }

    @Test
    public void testManyUpdatesWhileTransfer() {
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        final Path fileAtBart = TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 50000000);
        final FileInfo fileInfo = FileInfoFactory.lookupInstance(getFolderAtBart(), fileAtBart);
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(70, new ConditionWithMessage() {
            @Override
            public String message() {
                return "File at lisa: " + getFolderAtLisa().getFile(fileInfo)
                    + ". Known files at lisa: "
                    + getFolderAtLisa().getKnownFiles();
            }

            public boolean reached() {
                return getFolderAtLisa().getFile(fileInfo) != null;
            }
        });
        for (int i = 0; i < 10; i++) {
            TestHelper.changeFile(fileAtBart,
                5000000 + (long) (Math.random() * 10000));
            TestHelper.waitMilliSeconds(100);
            scanFolder(getFolderAtBart());
        }
        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            @Override
            public String message() {
                Path fileAtLisa = getFolderAtLisa().getKnownFiles().iterator()
                        .next()
                        .getDiskFile(getContollerLisa().getFolderRepository());
                try {
                    return "size(Lisa):" + Files.size(fileAtLisa) + " size(Bart): " + Files.size(fileAtBart)
                            + " lastmod(Lisa): " + Files.getLastModifiedTime(fileAtLisa) + " lastmod(Bart): " + Files.getLastModifiedTime(fileAtBart);
                } catch (IOException e) {
                    return e.getMessage();
                }
            }

            public boolean reached() {
                Path fileAtLisa = getFolderAtLisa().getKnownFiles().iterator()
                        .next()
                        .getDiskFile(getContollerLisa().getFolderRepository());
                try {
                    return Files.size(fileAtLisa) == Files.size(fileAtBart)
                            && DateUtil.equalsFileDateCrossPlattform(Files.getLastModifiedTime(fileAtBart).toMillis(),
                            Files.getLastModifiedTime(fileAtLisa).toMillis());
                } catch (IOException ioe) {
                    return true;
                }
            }
        });
        TestHelper.waitForCondition(10, new ConditionWithMessage() {

            @Override
            public boolean reached() {
                FileInfo fInfoAtBart = getFolderAtBart().getFile(fileInfo);
                FileInfo fIfnoAtLisa = getFolderAtLisa().getFile(fileInfo);
                return fInfoAtBart.getVersion() == fIfnoAtLisa.getVersion() &&
                        fInfoAtBart.getSize() == fIfnoAtLisa.getSize() &&
                        DateUtil.equalsFileDateCrossPlattform(fInfoAtBart.getModifiedDate(), fIfnoAtLisa.getModifiedDate());
            }

            @Override
            public String message() {
                return "";
            }
        });
    }

    // PFC-2758
    @Test
    public void testIdenticalDateAndSizeHandling() throws IOException {
        if (!FileInfo.IGNORE_CASE) {
            return;
        }
        assumeTrue( FileInfo.IGNORE_CASE,"Test only supported on systems which do ignore character case in filenames, e.g. Windows");
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        disconnectBartAndLisa();

        // 1) Create v=2 testfile at bart
        Path fileBart = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "Test.txt", new byte[0]);
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileBart);
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileBart);
        scanFolder(getFolderAtBart());

        assertEquals(2,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());
        assertEquals(1, getFolderAtBart().getKnownItemCount());

        // 2) Create v=1 testfile at lisa (different name case)
        TestHelper.waitMilliSeconds(2100);
        Path fileLisa = TestHelper.createTestFile(
            getFolderAtLisa().getLocalBase(), "test.txt", new byte[0]);
        scanFolder(getFolderAtLisa());

        TestHelper.waitMilliSeconds(2100);
        TestHelper.changeFile(fileLisa, Files.size(fileBart));
        Files.setLastModifiedTime(fileLisa,
            Files.getLastModifiedTime(fileBart));
        scanFolder(getFolderAtLisa());

        assertEquals(1,
            getFolderAtLisa().getKnownFiles().iterator().next().getVersion());
        assertEquals(1, getFolderAtLisa().getKnownItemCount());

        // 3) Connect Lisa and Bart and sync
        connectBartAndLisa();
        TestHelper.waitMilliSeconds(1000);

        assertEquals( 1,
            getFolderAtBart().getKnownItemCount(),"Bart has too many files");
        assertEquals(
            2,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion(),
            "Version at bart wrong: " + getFolderAtBart().getKnownFiles()
                .iterator().next().toDetailString());

        assertEquals( 1,
            getFolderAtLisa().getKnownItemCount(),"Lisa has too many files");
        assertEquals(
            2,
            getFolderAtLisa().getKnownFiles().iterator().next().getVersion(),
            "Version at lisa wrong: " + getFolderAtLisa().getKnownFiles()
                .iterator().next().toDetailString());
    }
}
