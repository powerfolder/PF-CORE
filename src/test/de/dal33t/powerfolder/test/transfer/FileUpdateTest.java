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

import java.io.File;
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
    public void testInitalSync() {
        TestHelper.waitMilliSeconds(500);
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
                return getFolderAtBart().getKnownItemCount() == 1
                    && getFolderAtLisa().getKnownItemCount() == 1;
            }
        });
        // W8 4 filelist
        TestHelper.waitMilliSeconds(500);
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
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() == 1;
            }
        });

        // Test barts file (=newer)
        FileInfo fileInfoAtBart = getFolderAtBart().getFile(
            FileInfoFactory.lookupInstance(getFolderAtBart(), fileAtBart));
        assertEquals(1, fileInfoAtBart.getVersion());
        assertEquals(fileAtBart.getName(), fileInfoAtBart.getFilenameOnly());
        assertEquals(fileAtBart.length(), fileInfoAtBart.getSize());
        assertEquals(fileAtBart.lastModified(), fileInfoAtBart
            .getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            fileInfoAtBart.getModifiedBy());

        // Test lisas file (=should be override by barts newer file)
        FileInfo fileInfoAtLisa = FileInfoFactory.lookupInstance(
            getFolderAtLisa(), fileAtLisa);
        fileInfoAtLisa = fileInfoAtLisa.getLocalFileInfo(getContollerLisa()
            .getFolderRepository());
        assertFileMatch(fileAtLisa, fileInfoAtLisa, getContollerLisa());
        assertTrue(fileInfoAtLisa.inSyncWithDisk(fileAtLisa));
        assertEquals(fileAtBart.getName(), fileInfoAtLisa.getFilenameOnly());
        assertEquals(fileAtBart.length(), fileInfoAtLisa.getSize());
        assertEquals(fileAtBart.lastModified(), fileInfoAtLisa
            .getModifiedDate().getTime());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            fileInfoAtLisa.getModifiedBy());
    }

    /**
     * Tests the when the internal db is out of sync with the disk. Ticket #387
     */
    public void testFileChangedOnDisk() {
        File fileAtBart = TestHelper.createTestFile(getFolderAtBart()
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
        assertEquals(SMALLER_FILE_CONTENTS.length, fileAtBart.length());
        // = db
        assertEquals(LONG_FILE_CONTENTS.length, getFolderAtBart()
            .getKnownFiles().iterator().next().getSize());
        assertNotSame(fileAtBart.length(), getFolderAtBart().getKnownFiles()
            .iterator().next().getSize());

        // Change sync profile = auto download.
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        // Abort of upload should have been sent to lisa = NO download.
        TestHelper.waitForCondition(10, new Condition() {
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

        final File fileAtBart = TestHelper.createTestFile(getFolderAtBart()
            .getLocalBase(), TEST_FILENAME, LONG_FILE_CONTENTS);
        // Scan the file
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());

        final File fileAtLisa = getFolderAtBart().getKnownFiles().iterator()
            .next().getDiskFile(getContollerLisa().getFolderRepository());
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return fileAtLisa.exists();
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
        assertEquals(1, fInfoAtBart.getVersion());
        assertEquals(1, fInfoAtLisa.getVersion());
        assertFalse("Date of conflicting file same", fInfoAtBart
            .getModifiedDate().equals(fInfoAtLisa.getModifiedDate()));
        assertTrue("Date of conflicting file problem", fInfoAtBart
            .getModifiedDate().getTime() < fInfoAtLisa.getModifiedDate()
            .getTime());
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
                return fileAtLisa.length() == fileAtBart.length();
            }

            public String message() {
                return "Lisa file.length: " + fileAtLisa.length()
                    + ". Bart file.length: " + fileAtBart.length();
            }
        });
        // Now Bart should have detected an conflict.
        assertEquals(1, getFolderAtBart().getProblems().size());
        assertEquals(0, getFolderAtLisa().getProblems().size());
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
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
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
        fInfoAtBart = fInfoAtBart.getLocalFileInfo(getContollerBart()
            .getFolderRepository());
        assertEquals(4, fInfoAtBart.getVersion());

        TestHelper.waitMilliSeconds(3500);
        TestHelper.changeFile(fileAtLisa);
        scanFolder(getFolderAtLisa());
        fInfoAtLisa = fInfoAtLisa.getLocalFileInfo(getContollerLisa()
            .getFolderRepository());
        assertEquals(2, fInfoAtLisa.getVersion());

        boolean conflict = fInfoAtLisa.getVersion() == fInfoAtBart.getVersion()
            && fInfoAtBart.isNewerThan(fInfoAtLisa);
        conflict |= fInfoAtLisa.getVersion() <= fInfoAtBart.getVersion()
            && DateUtil.isNewerFileDateCrossPlattform(
                fInfoAtLisa.getModifiedDate(), fInfoAtBart.getModifiedDate());
        assertTrue("Barts: " + fInfoAtBart.toDetailString() + " Lisas: "
            + fInfoAtLisa.toDetailString(), conflict);

        connectBartAndLisa();
        // The old copy should have been distributed.
        TestHelper.waitForCondition(10, new ConditionWithMessage() {

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

        final File fileAtBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 5000000);
        TestHelper.waitForCondition(10, new Condition() {
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
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                File fileAtLisa = getFolderAtLisa().getKnownFiles().iterator()
                    .next()
                    .getDiskFile(getContollerLisa().getFolderRepository());
                return fileAtLisa.length() == fileAtBart.length()
                    && fileAtBart.lastModified() == fileAtLisa.lastModified();
            }
        });
    }
}
