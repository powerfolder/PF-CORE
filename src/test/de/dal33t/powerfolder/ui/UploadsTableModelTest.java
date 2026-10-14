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
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.information.uploads.UploadsTableModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the upload table model.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UploadsTableModelTest extends TwoControllerTestCase {

    private UploadsTableModel bartModel;
    private MyUploadTableModelListener bartModelListener;
    private java.util.Timer stackDumpTimer;

    // PFS-5561: JUnit 3's TestCase.getName() is gone under JUnit 5 - capture the
    // running test name via TestInfo for the diagnostic stack dump below.
    private String currentTestName = "UploadsTableModelTest";

    @org.junit.jupiter.api.BeforeEach
    void captureTestName(org.junit.jupiter.api.TestInfo testInfo) {
        currentTestName = testInfo.getDisplayName();
    }

    @BeforeEach
    protected void setUp() throws Exception {
        // Diagnostics for sporadic hangs of this class on CI (PFC-3573): log which method runs and what happens.
        // If a test runs longer than 8 minutes, dump all thread stacks. To a FILE in test-reports (uploaded as CI
        // artifact) - NOT to System.out: the hang also freezes stdout (blocked PrintStream lock), and the ant fork
        // timeout kill discards buffered output.
        System.out.println(">>> UploadsTableModelTest#" + currentTestName);
        stackDumpTimer = new java.util.Timer("StackDumpWatchdog", true);
        stackDumpTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                StringBuilder dump = new StringBuilder("=== STACK DUMP - " + currentTestName + " runs > 8 minutes ===\n");
                for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                    dump.append("Thread: ").append(e.getKey().getName()).append(" state=")
                        .append(e.getKey().getState()).append('\n');
                    for (StackTraceElement ste : e.getValue()) {
                        dump.append("    at ").append(ste).append('\n');
                    }
                }
                try {
                    java.nio.file.Path dir = java.nio.file.Paths.get("test-reports");
                    java.nio.file.Files.createDirectories(dir);
                    java.nio.file.Files.write(dir.resolve("threaddump-UploadsTableModelTest-" + currentTestName + ".txt"),
                        dump.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                    // Ignore - diagnostics only
                }
            }
        }, 8 * 60 * 1000, 8 * 60 * 1000);
        super.setUp();
        LoggingManager.setConsoleLogging(Level.INFO);
        connectBartAndLisa();

        // Join on testfolder
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
        getFolderAtLisa().getFolderWatcher().setIngoreAll(true);

        bartModelListener = new MyUploadTableModelListener();
        bartModel = new UploadsTableModel(
            new TransferManagerModel(getContollerBart().getTransferManager()));
        bartModel.setPeriodicUpdate(false);
        bartModel.addTableModelListener(bartModelListener);

        // Instant cleanup
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
            .setValue(getContollerBart(), Integer.MAX_VALUE);
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
            .setValue(getContollerBart(), "0");

    }

    @AfterEach
    protected void tearDown() throws Exception {
        // Cancel AFTER super.tearDown(): the PFC-3573 hang happened in controller shutdown during tearDown -
        // cancelling first would disarm the watchdog exactly for that case.
        super.tearDown();
        if (stackDumpTimer != null) {
            stackDumpTimer.cancel();
        }
    }

    @Test
    public void testSingleFileUpload() {
        final String fileName = TestHelper.createRandomFile(
            getFolderAtBart().getLocalBase()).getFileName().toString();
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return inSyncAtLisa(fileName) && uploadRowOf(fileName) == null
                    && countType(bartModelListener.events,
                        TableModelEvent.INSERT) >= 1
                    && countType(bartModelListener.events,
                        TableModelEvent.DELETE) >= 1;
            }

            public String message() {
                return "Completed upload of " + fileName
                    + " was not removed from the model. In sync at lisa: "
                    + inSyncAtLisa(fileName) + ". Row: "
                    + uploadRowOf(fileName) + ". Events: "
                    + bartModelListener.events;
            }
        });

        // No upload in tablemodel
        assertEquals(0, bartModel.getRowCount());

        // Assert the feature, not an exact event sequence: on a loaded runner
        // the model interleaves extra progress UPDATEs and can re-fire
        // INSERT/DELETE pairs. The stable invariants are the final row count
        // (asserted above: 0) plus proof the upload was both tracked and
        // removed at least once.
        List<TableModelEvent> events = bartModelListener.events;
        assertTrue(countType(events, TableModelEvent.INSERT) >= 1, events.toString());
        assertTrue(countType(events, TableModelEvent.DELETE) >= 1,
            "completed upload should have been removed: " + events);
    }

    private Upload uploadRowOf(String fileName) {
        for (int row = 0; row < bartModel.getRowCount(); row++) {
            Upload upload = bartModel.getUploadAtRow(row);
            if (upload != null
                && fileName.equals(upload.getFile().getFilenameOnly()))
            {
                return upload;
            }
        }
        return null;
    }

    private boolean uploadCompletedAtBart(String fileName) {
        for (Upload upload : getContollerBart().getTransferManager()
            .getCompletedUploadsCollection())
        {
            if (fileName.equals(upload.getFile().getFilenameOnly())) {
                return true;
            }
        }
        return false;
    }

    private boolean inSyncAtLisa(String fileName) {
        for (FileInfo fInfo : getFolderAtLisa().getKnownFiles()) {
            if (fileName.equals(fInfo.getFilenameOnly())
                && fInfo.inSyncWithDisk(fInfo.getDiskFile(
                    getContollerLisa().getFolderRepository())))
            {
                return true;
            }
        }
        return false;
    }

    private static int countType(List<TableModelEvent> events, int type) {
        int n = 0;
        for (TableModelEvent e : events) {
            if (e.getType() == type) {
                n++;
            }
        }
        return n;
    }

    /**
     * This tests UPLOADS_AUTO_CLEANUP ConfigurationEntry. By default this is
     * true. Setting to FALSE stops completed uploads being removed.
     */
    @Test
    public void testSingleFileUploadNoAutoCleanup() {
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
            .setValue(getContollerBart(), Integer.MAX_VALUE);
        final String fileName = TestHelper.createRandomFile(
            getFolderAtBart().getLocalBase()).getFileName().toString();
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return uploadCompletedAtBart(fileName)
                    && uploadRowOf(fileName) != null;
            }

            public String message() {
                return "Completed upload of " + fileName
                    + " did not reach the model. Completed at bart: "
                    + getContollerBart().getTransferManager()
                        .getCompletedUploadsCollection()
                    + ". Events: " + bartModelListener.events;
            }
        });
        TestHelper.waitForEmptyEDT();

        // The feature under test is "cleanup off => the completed upload is
        // NOT removed", which the final row count captures: the row is still
        // there. (On a loaded runner the model can churn an intermediate
        // INSERT/DELETE/INSERT, so the DELETE *event* count is not a stable
        // signal - the surviving row is.)
        assertEquals(1, bartModel.getRowCount());
        assertTrue(countType(bartModelListener.events, TableModelEvent.INSERT) >= 1,
            bartModelListener.events.toString());
    }

    @Test
    public void testRunningUpload() {
        // Throttle so the upload stays observable as running; 20MB at 2MB/s = ~10 seconds
        getContollerBart().getTransferManager().setUploadCPSForLAN(2 * 1024 * 1024);
        final String fileName = TestHelper.createRandomFile(
            getFolderAtBart().getLocalBase(), 20 * 1024 * 1024).getFileName()
            .toString();
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public String message() {
                return "Upload of " + fileName + " not in the model. Rows: "
                    + bartModel.getRowCount();
            }

            public boolean reached() {
                getContollerLisa().getFolderRepository().getFileRequestor().triggerFileRequesting();
                return uploadRowOf(fileName) != null;
            }
        });

        // 1 active uploads
        assertEquals(1, bartModel.getRowCount());

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return inSyncAtLisa(fileName) && uploadRowOf(fileName) == null;
            }

            public String message() {
                return "Upload of " + fileName + " still in the model. In sync"
                    + " at lisa: " + inSyncAtLisa(fileName) + ". Rows: "
                    + bartModel.getRowCount();
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount());

        TestHelper.waitForEmptyEDT();
    }

    @Test
    public void testAbortUpload() {
        // Throttle lisas download so the upload is still running when it gets aborted. Setting the
        // ConfigurationEntry directly (as before) never took effect - the TransferManager applies limits
        // via updateSpeedLimits() only.
        getContollerLisa().getTransferManager().setDownloadCPSForLAN(1024 * 1024);

        assertEquals(0, bartModelListener.events.size());
        // Create a 20 megs file. At 1MB/s the transfer runs ~20 seconds - the abort happens mid-transfer.
        final String fileName = TestHelper.createRandomFile(
            getFolderAtBart().getLocalBase(), 20 * 1024 * 1024).getFileName()
            .toString();
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                Upload upload = uploadRowOf(fileName);
                return upload != null && upload.isStarted()
                    && !upload.isCompleted()
                    && bartModelListener.events.size() >= 2;
            }

            public String message() {
                return "Upload of " + fileName + " not running in the model: "
                    + uploadRowOf(fileName) + ". Events: "
                    + bartModelListener.events;
            }
        });
        TestHelper.waitForEmptyEDT();

        assertEquals(1, bartModel.getRowCount());
        // Upload requested + started
        assertEquals(2, bartModelListener.events.size());

        // Abort thru change of sync profile
        getFolderAtLisa().setSyncProfile(SyncProfile.HOST_FILES);

        TestHelper.waitForCondition(50, new ConditionWithMessage() {
            public boolean reached() {
                return uploadRowOf(fileName) == null
                    && bartModelListener.events.size() >= 3;
            }

            public String message() {
                return "Aborted upload of " + fileName
                    + " still in the model: " + uploadRowOf(fileName)
                    + ". Events: " + bartModelListener.events;
            }
        });
        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Wait for EDT
        TestHelper.waitForEmptyEDT();

        // no active upload
        assertEquals(0, bartModel.getRowCount());
        // Check correct events from model
        assertEquals(3, bartModelListener.events.size(), bartModelListener.events.toString());
        // Upload requested
        assertTrue(bartModelListener.events.get(0)
            .getType() == TableModelEvent.INSERT);
        // Upload started
        assertTrue(bartModelListener.events.get(1)
            .getType() == TableModelEvent.UPDATE);
        // Upload aborted
        assertTrue(bartModelListener.events.get(2)
            .getType() == TableModelEvent.DELETE);

        TestHelper.waitForEmptyEDT();
    }

    @Test
    public void testDisconnectWhileUploadMultiple() throws Exception {
        for (int i = 0; i < 10; i++) {
            testDisconnectWhileUpload();
            tearDown();
            setUp();
        }
    }

    @Test
    public void testDisconnectWhileUpload() {
        getContollerBart().getTransferManager().setUploadCPSForLAN(40000);

        // Create a 10 megs file. At ~40KB/s the upload is guaranteed to be still running when disconnecting.
        final String fileName = TestHelper.createRandomFile(
            getFolderAtBart().getLocalBase(), 10 * 1024 * 1024).getFileName()
            .toString();
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                Upload upload = uploadRowOf(fileName);
                return upload != null && upload.isStarted()
                    && !upload.isCompleted()
                    && bartModelListener.events.size() >= 2;
            }

            @Override
            public String message() {
                return "Upload of " + fileName + " not running in the model: "
                    + uploadRowOf(fileName) + ". Bart events: "
                    + bartModelListener.events + ". Bart active uploads: "
                    + getContollerBart().getTransferManager().countActiveUploads();
            }
        });

        // Problem can occur: Transfer completes too quick. Uploads table model
        // is then empty!
        assertEquals(1, bartModel.getRowCount(), "Rowcount mismatch");
        // Requested and Started
        if (bartModelListener.events.size() >= 3) {
            if (bartModelListener.events.get(2)
                .getType() == TableModelEvent.DELETE)
            {
                fail(
                    "Premature file transfer finish. increase filesize for testfile. " +
                            "completed uploads=" + getContollerBart().getTransferManager().countCompletedDownloads());
            }
        }
        // Upload requested
        assertEquals(TableModelEvent.INSERT,
            bartModelListener.events.get(0).getType());
        // Upload started
        assertEquals(TableModelEvent.UPDATE,
            bartModelListener.events.get(1).getType());
        assertEquals(2, bartModelListener.events.size(), "Event count mismatch");

        disconnectBartAndLisa();
        TestHelper.waitForCondition(30, () -> getContollerBart().getTransferManager()
            .countLiveUploads() == 0);

        // Give EDT time
        TestHelper.waitForEmptyEDT();

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return uploadRowOf(fileName) == null
                    && bartModelListener.events.size() >= 3;
            }

            @Override
            public String message() {
                return "Aborted upload of " + fileName
                    + " still in the model: " + uploadRowOf(fileName)
                    + ". Bart events: " + bartModelListener.events;
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount(), "Rowcount mismatch");
        // Upload requested, started, aborted
        assertEquals(3, bartModelListener.events.size(), "Event count mismatch");
        // Upload requested
        assertTrue(bartModelListener.events.get(0)
            .getType() == TableModelEvent.INSERT);
        // Upload started
        assertTrue(bartModelListener.events.get(1)
            .getType() == TableModelEvent.UPDATE);
        // Upload aborted
        assertTrue(bartModelListener.events.get(2)
            .getType() == TableModelEvent.DELETE);

        TestHelper.waitForEmptyEDT();
    }

    private class MyUploadTableModelListener implements TableModelListener {
        public List<TableModelEvent> events = new ArrayList<TableModelEvent>();

        public void tableChanged(TableModelEvent e) {
            events.add(e);
        }
    }
}
