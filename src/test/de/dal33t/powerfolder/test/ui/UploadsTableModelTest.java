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
package de.dal33t.powerfolder.test.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.information.uploads.UploadsTableModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the upload table model.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UploadsTableModelTest extends TwoControllerTestCase {

    private UploadsTableModel bartModel;
    private MyUploadTableModelListener bartModelListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);

        bartModelListener = new MyUploadTableModelListener();
        bartModel = new UploadsTableModel(new TransferManagerModel(
            getContollerBart().getTransferManager()));
        bartModel.addTableModelListener(bartModelListener);
        bartModel.setPeriodicUpdate(false);

        // Instant cleanup
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY.setValue(
            getContollerBart(), Integer.MAX_VALUE);
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY.setValue(
            getContollerBart(), "0");

    }

    public void testSingleFileUpload() {
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        // Copy
        TestHelper.waitMilliSeconds(1500);

        // No upload in tablemodel
        assertEquals(0, bartModel.getRowCount());

        // Check correct events from model
        assertEquals(bartModelListener.events.toString(), 4,
            bartModelListener.events.size());
        assertTrue(bartModelListener.events.get(0).getType() == TableModelEvent.INSERT); // Upload
        // Requested
        assertTrue(bartModelListener.events.get(1).getType() == TableModelEvent.UPDATE); // Upload
        // started
        assertTrue(bartModelListener.events.get(2).getType() == TableModelEvent.UPDATE); // Upload
        // completed
        assertTrue(bartModelListener.events.get(3).getType() == TableModelEvent.DELETE); // Completed
        // upload
        // removed
    }

    /**
     * This tests UPLOADS_AUTO_CLEANUP ConfigurationEntry. By default this is
     * true. Setting to FALSE stops completed uploads being removed.
     */
    public void testSingleFileUploadNoAutoCleanup() {
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY.setValue(
            getContollerBart(), Integer.MAX_VALUE);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        // Copy
        TestHelper.waitMilliSeconds(1500);

        // One (complete) upload in tablemodel
        assertEquals(1, bartModel.getRowCount());

        // Check correct events from model
        assertEquals(bartModelListener.events.toString(), 3,
            bartModelListener.events.size());
        assertTrue(bartModelListener.events.get(0).getType() == TableModelEvent.INSERT); // Upload
        // Requested
        assertTrue(bartModelListener.events.get(1).getType() == TableModelEvent.UPDATE); // Upload
        // started
        assertTrue(bartModelListener.events.get(2).getType() == TableModelEvent.UPDATE); // Upload
        // completed
    }

    public void testRunningUpload() {
        // Create a 10 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10000000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
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
        ConfigurationEntry.DOWNLOAD_LIMIT_LAN.setValue(getContollerLisa(),
            "1000");

        assertEquals(0, bartModelListener.events.size());
        // Create a 20 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 20000000);
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
        assertEquals(3, bartModelListener.events.size());
        // Upload requested
        assertTrue(bartModelListener.events.get(0).getType() == TableModelEvent.INSERT);
        // Upload started
        assertTrue(bartModelListener.events.get(1).getType() == TableModelEvent.UPDATE);
        // Upload aborted
        assertTrue(bartModelListener.events.get(2).getType() == TableModelEvent.DELETE);

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
        getContollerLisa().getTransferManager().setUploadCPSForWAN(40000);

        // Create a 30 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 30000000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countActiveUploads() > 0 && bartModel.getRowCount() >= 1;
            }
        });

        // Problem can occur: Transfer completes too quick. Uploads table model
        // is then empty!
        assertEquals(1, bartModel.getRowCount());
        // Requested and Started
        assertEquals(2, bartModelListener.events.size());
        // Upload requested
        assertTrue(bartModelListener.events.get(0).getType() == TableModelEvent.INSERT);
        // Upload started
        assertTrue(bartModelListener.events.get(1).getType() == TableModelEvent.UPDATE);

        disconnectBartAndLisa();
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countLiveUploads() == 0;
            }
        });

        // Give EDT time
        TestHelper.waitForEmptyEDT();

        // no active upload
        assertEquals(0, bartModel.getRowCount());
        // Upload requested, started, aborted
        assertEquals(3, bartModelListener.events.size());
        // Upload requested
        assertTrue(bartModelListener.events.get(0).getType() == TableModelEvent.INSERT);
        // Upload started
        assertTrue(bartModelListener.events.get(1).getType() == TableModelEvent.UPDATE);
        // Upload aborted
        assertTrue(bartModelListener.events.get(2).getType() == TableModelEvent.DELETE);

        TestHelper.waitForEmptyEDT();
    }

    private class MyUploadTableModelListener implements TableModelListener {
        public List<TableModelEvent> events = new ArrayList<TableModelEvent>();

        public void tableChanged(TableModelEvent e) {
            System.err.println("Got event: " + e.getType() + " row: "
                + e.getFirstRow() + "-" + e.getLastRow());
            events.add(e);
        }
    }
}
