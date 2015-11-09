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
package de.dal33t.powerfolder.test.transfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.problem.FileConflictProblem;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.HOST_FILES);
    }

    /**
     * Test the initial sync of two files with same name and place but different
     * modification dates.
     * <p>
     * Ticket #345
     */
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
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownItemCount() == 1
                    && getFolderAtLisa().getKnownItemCount() == 1;
            }
        });
        // W8 4 filelist
        TestHelper.waitMilliSeconds(2000);
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
        TestHelper.waitForCondition(70, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() == 1;
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

        // Change sync profile = auto download.
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        // Abort of upload should have been sent to lisa = NO download.
        TestHelper.waitForCondition(70, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countNumberOfDownloads(getFolderAtLisa()) == 0;
            }
        });
        assertEquals("Lisa has a stuck download", 0, getContollerLisa()
            .getTransferManager().countNumberOfDownloads(getFolderAtLisa()));

        // Now trigger barts folders mainteance, detect new file.
        getContollerBart().getFolderRepository().triggerMaintenance();
        TestHelper.waitMilliSeconds(1000);
        // and trigger Lisas transfer check, detect broken download
        getContollerLisa().getTransferManager().triggerTransfersCheck();
        TestHelper.waitMilliSeconds(5000);

        // Download stays forever
        assertEquals("Lisa has a stuck download", 0, getContollerLisa()
            .getTransferManager().countNumberOfDownloads(getFolderAtLisa()));
    }

    public void testFileConflict() {
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        final Path fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), TEST_FILENAME, LONG_FILE_CONTENTS);
        // Scan the file
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());

        final Path fileAtLisa = getFolderAtBart().getKnownFiles().iterator()
            .next().getDiskFile(getContollerLisa().getFolderRepository());
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return Files.exists(fileAtLisa);
            }

            public String message() {
                return "File at lisa does not exists: " + fileAtLisa;
            }
        });

        disconnectBartAndLisa();

        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtBart);
        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtLisa);

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        FileInfo fInfoAtBart = getFolderAtBart().getKnownFiles().iterator()
            .next();
        FileInfo fInfoAtLisa = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        assertEquals(
            "Expected version at bart 1. Got: " + fInfoAtBart.toDetailString(),
            1, fInfoAtBart.getVersion());
        assertEquals(
            "Expected version at lisa 1. Got: " + fInfoAtLisa.toDetailString(),
            1, fInfoAtLisa.getVersion());
        assertFalse("Date of conflicting file same", fInfoAtBart
            .getModifiedDate().equals(fInfoAtLisa.getModifiedDate()));
        assertTrue("Date of conflicting file problem",
            fInfoAtBart.getModifiedDate().getTime() < fInfoAtLisa
                .getModifiedDate().getTime());
        assertTrue(
            "File @ lisa is not newer than on bart. Lisa: "
                + fInfoAtLisa.toDetailString() + ". Bart: "
                + fInfoAtBart.toDetailString(),
            fInfoAtLisa.isNewerThan(fInfoAtBart));
        assertFalse("File size mismatch. Lisa: " + fInfoAtLisa.toDetailString()
            + ". Bart: " + fInfoAtBart.toDetailString(),
            fInfoAtLisa.getSize() == fInfoAtBart.getSize());

        // Now we have a conflict: SAME file version, but different modification
        // dates and sizes. In this scenario LISAs file wins

        connectBartAndLisa();
        // Let them sync.
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                try {
                    return Files.size(fileAtLisa) == Files.size(fileAtBart);
                } catch (IOException ioe) {
                    return true;
                }
            }

            public String message() {
                try {
                    return "Lisa file.length: " + Files.size(fileAtLisa)
                        + ". Bart file.length: " + Files.size(fileAtBart);
                } catch (IOException ioe) {
                    return "Could not retreive the file sizes";
                }
            }
        });
        // Now Bart should have detected an conflict.
        assertEquals(
            "Expected problems at bart: 1. Got: "
                + getFolderAtBart().getProblems(),
            1, getFolderAtBart().getProblems().size());
        assertEquals(
            "Expected problems at lisa: 0. Got: "
                + getFolderAtLisa().getProblems(),
            0, getFolderAtLisa().getProblems().size());
        Problem p = getFolderAtBart().getProblems().iterator().next();
        assertTrue("No conflicts detected: " + getFolderAtBart().getProblems(),
            p instanceof FileConflictProblem);
        FileConflictProblem cp = (FileConflictProblem) p;
        assertEquals(fInfoAtLisa, cp.getFileInfo());
        assertTrue("Old file not in archive @ bart: " + fInfoAtBart,
            getFolderAtBart().getFileArchiver()
                .hasArchivedFileInfo(fInfoAtBart));
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
        TestHelper.waitForCondition(70, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtBart().getKnownItemCount() == 1
                    && getFolderAtLisa().getKnownItemCount() == 1;
            }

            public String message() {
                return "Bart.getKnownItemCount: "
                    + getFolderAtBart().getKnownItemCount()
                    + ". Lisa.Bart.getKnownItemCount: "
                    + getFolderAtLisa().getKnownItemCount();
            }
        });

        // Second case: version is lower, but last modification date if newer
        disconnectBartAndLisa();
        getFolderAtBart().removeProblem(cp);

        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());
        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());
        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtBart);
        scanFolder(getFolderAtBart());
        fInfoAtBart = fInfoAtBart
            .getLocalFileInfo(getContollerBart().getFolderRepository());
        assertEquals(
            "Expected version at bart 4. Got: " + fInfoAtBart.toDetailString(),
            4, fInfoAtBart.getVersion());

        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtLisa);
        scanFolder(getFolderAtLisa());
        fInfoAtLisa = fInfoAtLisa
            .getLocalFileInfo(getContollerLisa().getFolderRepository());
        assertEquals(
            "Expected version at lisa 2. Got: " + fInfoAtLisa.toDetailString(),
            2, fInfoAtLisa.getVersion());

        boolean conflict = fInfoAtLisa.getVersion() == fInfoAtBart.getVersion()
            && fInfoAtBart.isNewerThan(fInfoAtLisa);
        conflict |= fInfoAtLisa.getVersion() <= fInfoAtBart.getVersion()
            && DateUtil.isNewerFileDateCrossPlattform(
                fInfoAtLisa.getModifiedDate(), fInfoAtBart.getModifiedDate());
        assertTrue("Barts: " + fInfoAtBart.toDetailString() + " Lisas: "
            + fInfoAtLisa.toDetailString(), conflict);

        connectBartAndLisa();
        // The old copy should have been distributed.
        TestHelper.waitForCondition(70, new ConditionWithMessage() {

            public boolean reached() {
                // Hack: #2557
                getContollerBart().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting();
                getContollerLisa().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting();
                return getFolderAtBart().getKnownItemCount() == 1
                    && getFolderAtLisa().getKnownItemCount() == 1
                    && !getFolderAtLisa().getProblems().isEmpty();
            }

            public String message() {
                return "Bart.getKnownItemCount: "
                    + getFolderAtBart().getKnownItemCount()
                    + ". Lisa.Bart.getKnownItemCount: "
                    + getFolderAtLisa().getKnownItemCount()
                    + " Problems at lisa: " + getFolderAtLisa().getProblems();
            }
        });
        p = getFolderAtLisa().getProblems().iterator().next();
        assertTrue("No conflicts detected: " + getFolderAtLisa().getProblems(),
            p instanceof FileConflictProblem);
        cp = (FileConflictProblem) p;
        assertEquals(fInfoAtLisa, cp.getFileInfo());

        assertTrue("Old file not in archive @ lisa: " + fInfoAtLisa,
            getFolderAtLisa().getFileArchiver()
                .hasArchivedFileInfo(fInfoAtLisa));
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

    public void testManyUpdatesWhileTransfer() {
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        final Path fileAtBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 5000000);
        TestHelper.waitForCondition(70, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFiles().size() > 0;
            }
        });
        LoggingManager.setConsoleLogging(Level.FINER);
        for (int i = 0; i < 40; i++) {
            TestHelper.changeFile(fileAtBart,
                5000000 + (long) (Math.random() * 10000));
            TestHelper.waitMilliSeconds(400);
            scanFolder(getFolderAtBart());
        }
        TestHelper.waitForCondition(70, new Condition() {
            public boolean reached() {
                Path fileAtLisa = getFolderAtLisa().getKnownFiles().iterator()
                    .next()
                    .getDiskFile(getContollerLisa().getFolderRepository());
                try {
                    return Files.size(fileAtLisa) == Files.size(fileAtBart)
                        && Files.getLastModifiedTime(fileAtBart).equals(
                            Files.getLastModifiedTime(fileAtLisa));
                } catch (IOException ioe) {
                    return true;
                }
            }
        });
    }

    // PFC-2758
    public void testIdenticalDateAndSizeHandling() throws IOException {
        if (!FileInfo.IGNORE_CASE) {
            fail("FileInfo.IGNORE_CASE=" + FileInfo.IGNORE_CASE
                + ". Unable to test");
            return;
        }
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        disconnectBartAndLisa();

        // 1) Create v=2 testfile at bart
        Path fileBart = TestHelper.createTestFile(
            getFolderAtBart().getLocalBase(), "Test.txt", new byte[0]);
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(2500);
        TestHelper.changeFile(fileBart);
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(2500);
        TestHelper.changeFile(fileBart);
        scanFolder(getFolderAtBart());

        assertEquals(2,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());
        assertEquals(1, getFolderAtBart().getKnownItemCount());

        // 2) Create v=1 testfile at lisa (different name case)
        TestHelper.waitMilliSeconds(2500);
        Path fileLisa = TestHelper.createTestFile(
            getFolderAtLisa().getLocalBase(), "test.txt", new byte[0]);
        scanFolder(getFolderAtLisa());

        TestHelper.waitMilliSeconds(2500);
        TestHelper.changeFile(fileLisa, Files.size(fileBart));
        Files.setLastModifiedTime(fileLisa,
            Files.getLastModifiedTime(fileBart));
        scanFolder(getFolderAtLisa());

        assertEquals(1,
            getFolderAtLisa().getKnownFiles().iterator().next().getVersion());
        assertEquals(1, getFolderAtLisa().getKnownItemCount());

        // 3) Connect Lisa and Bart and sync
        connectBartAndLisa();
        TestHelper.waitMilliSeconds(5000);

        assertEquals("Bart has too many files", 1,
            getFolderAtBart().getKnownItemCount());
        assertEquals(
            "Version at bart wrong: " + getFolderAtBart().getKnownFiles()
                .iterator().next().toDetailString(),
            2,
            getFolderAtBart().getKnownFiles().iterator().next().getVersion());

        assertEquals("Lisa has too many files", 1,
            getFolderAtLisa().getKnownItemCount());
        assertEquals(
            "Version at lisa wrong: " + getFolderAtLisa().getKnownFiles()
                .iterator().next().toDetailString(),
            2,
            getFolderAtLisa().getKnownFiles().iterator().next().getVersion());
    }
}
