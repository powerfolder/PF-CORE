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
import de.dal33t.powerfolder.ui.information.uploads.UploadsTableModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.logging.LoggingManager;

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

    @Override
    protected void setUp() throws Exception {
        // Diagnostics for sporadic hangs of this class on CI (PFC-3573): log which method runs and what happens.
        // If a test runs longer than 8 minutes, dump all thread stacks. To a FILE in test-reports (uploaded as CI
        // artifact) - NOT to System.out: the hang also freezes stdout (blocked PrintStream lock), and the ant fork
        // timeout kill discards buffered output.
        System.out.println(">>> UploadsTableModelTest#" + getName());
        stackDumpTimer = new java.util.Timer("StackDumpWatchdog", true);
        stackDumpTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                StringBuilder dump = new StringBuilder("=== STACK DUMP - " + getName() + " runs > 8 minutes ===\n");
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
                    java.nio.file.Files.write(dir.resolve("threaddump-UploadsTableModelTest-" + getName() + ".txt"),
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

    @Override
    protected void tearDown() throws Exception {
        if (stackDumpTimer != null) {
            stackDumpTimer.cancel();
        }
        super.tearDown();
    }

    public void testSingleFileUpload() {
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        // Copy
        TestHelper.waitMilliSeconds(1500);

        // No upload in tablemodel
        assertEquals(0, bartModel.getRowCount());

        // Check correct events from model
        assertEquals(bartModelListener.events.toString(), 4,
            bartModelListener.events.size());
        assertTrue(bartModelListener.events.get(0)
            .getType() == TableModelEvent.INSERT); // Upload
        // Requested
        assertTrue(bartModelListener.events.get(1)
            .getType() == TableModelEvent.UPDATE); // Upload
        // started
        assertTrue(bartModelListener.events.get(2)
            .getType() == TableModelEvent.UPDATE); // Upload
        // completed
        assertTrue(bartModelListener.events.get(3)
            .getType() == TableModelEvent.DELETE); // Completed
        // upload
        // removed
    }

    /**
     * This tests UPLOADS_AUTO_CLEANUP ConfigurationEntry. By default this is
     * true. Setting to FALSE stops completed uploads being removed.
     */
    public void testSingleFileUploadNoAutoCleanup() {
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
            .setValue(getContollerBart(), Integer.MAX_VALUE);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        // Copy
        TestHelper.waitMilliSeconds(1500);

        // One (complete) upload in tablemodel
        assertEquals(1, bartModel.getRowCount());

        // Check correct events from model
        assertEquals(bartModelListener.events.toString(), 3,
            bartModelListener.events.size());
        assertTrue(bartModelListener.events.get(0)
            .getType() == TableModelEvent.INSERT); // Upload
        // Requested
        assertTrue(bartModelListener.events.get(1)
            .getType() == TableModelEvent.UPDATE); // Upload
        // started
        assertTrue(bartModelListener.events.get(2)
            .getType() == TableModelEvent.UPDATE); // Upload
        // completed
    }

    public void testRunningUpload() {
        // Throttle so the upload stays observable as running; 20MB at 2MB/s = ~10 seconds
        getContollerBart().getTransferManager().setUploadCPSForLAN(2 * 1024 * 1024);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 20 * 1024 * 1024);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public String message() {
                return "bartModel.getRowCount()=" + bartModel.getRowCount();
            }

            public boolean reached() {
                getContollerLisa().getFolderRepository().getFileRequestor().triggerFileRequesting();
                return bartModel.getRowCount() > 0;
            }
        });

        // 1 active uploads
        assertEquals(1, bartModel.getRowCount());

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }

            public String message() {
                return "Bart rowcount:" + bartModel.getRowCount();
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount());

        TestHelper.waitForEmptyEDT();
    }

    public void testAbortUpload() {
        // Throttle lisas download so the upload is still running when it gets aborted. Setting the
        // ConfigurationEntry directly (as before) never took effect - the TransferManager applies limits
        // via updateSpeedLimits() only.
        getContollerLisa().getTransferManager().setDownloadCPSForLAN(1024 * 1024);

        assertEquals(0, bartModelListener.events.size());
        // Create a 20 megs file. At 1MB/s the transfer runs ~20 seconds - the abort happens mid-transfer.
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 20 * 1024 * 1024);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().size() == 1
                    && getContollerLisa().getTransferManager()
                        .countActiveDownloads() == 1;
            }
        });
        TestHelper.waitForEmptyEDT();

        assertEquals(1, bartModel.getRowCount());
        // Upload requested + started
        assertEquals(2, bartModelListener.events.size());

        // Abort thru change of sync profile
        getFolderAtLisa().setSyncProfile(SyncProfile.HOST_FILES);

        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });
        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Wait for EDT
        TestHelper.waitForEmptyEDT();

        // no active upload
        assertEquals(0, bartModel.getRowCount());
        // Check correct events from model
        assertEquals(bartModelListener.events.toString(), 3, bartModelListener.events.size());
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

    public void testDisconnectWhileUploadMultiple() throws Exception {
        for (int i = 0; i < 10; i++) {
            testDisconnectWhileUpload();
            tearDown();
            setUp();
        }
    }

    public void testDisconnectWhileUpload() {
        getContollerBart().getTransferManager().setUploadCPSForLAN(40000);

        // Create a 10 megs file. At ~40KB/s the upload is guaranteed to be still running when disconnecting.
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10 * 1024 * 1024);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countActiveUploads() > 0 && bartModel.getRowCount() >= 1
                    && bartModelListener.events.size() >= 2;
            }

            @Override
            public String message() {
                return "Bart rowcount: " + bartModel.getRowCount()
                    + ". Bart events: " + bartModelListener.events.size() +
                        ". Bart active uploads: " + getContollerBart().getTransferManager().countActiveUploads();
            }
        });

        // Problem can occur: Transfer completes too quick. Uploads table model
        // is then empty!
        assertEquals("Rowcount mismatch", 1, bartModel.getRowCount());
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
        assertEquals("Event count mismatch", 2,
            bartModelListener.events.size());

        disconnectBartAndLisa();
        TestHelper.waitForCondition(30, () -> getContollerBart().getTransferManager()
            .countLiveUploads() == 0);

        // Give EDT time
        TestHelper.waitForEmptyEDT();

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return bartModel.getRowCount() == 0
                    && bartModelListener.events.size() == 3;
            }

            @Override
            public String message() {
                return "Bart rowcount: " + bartModel.getRowCount()
                    + ". Bart events: " + bartModelListener.events.size();
            }
        });

        // no active upload
        assertEquals("Rowcount mismatch", 0, bartModel.getRowCount());
        // Upload requested, started, aborted
        assertEquals("Event count mismatch", 3,
            bartModelListener.events.size());
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
