/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.transfer.UploadsTableModel;

/**
 * Tests the upload table model.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UploadsTableModelTest extends TwoControllerTestCase {
    private static final String BASEDIR1 = "build/test/ControllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/ControllerLisa/testFolder";

    private Folder folderBart;
    private UploadsTableModel bartModel;
    private MyUploadTableModelListener bartModelListener;

    @Override
    protected void setUp() throws Exception
    {
        System.out.println("FileTransferTest.setUp()");
        super.setUp();

        bartModelListener = new MyUploadTableModelListener();
        bartModel = new UploadsTableModel(getContollerBart()
            .getTransferManager());
        bartModel.addTableModelListener(bartModelListener);

        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);
        joinFolder(testFolder, new File(BASEDIR1), new File(BASEDIR2),
            SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        folderBart = getContollerBart().getFolderRepository().getFolder(
            testFolder);
        getContollerLisa().getFolderRepository().getFolder(testFolder);
    }

    public void testSingleFileUpload() {
        TestHelper.createRandomFile(folderBart.getLocalBase());
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        
        // Copy
        TestHelper.waitMilliSeconds(1500);

        // No upload in tablemodel
        assertEquals(0, bartModel.getRowCount());

        // Check correct events from model
        assertEquals(bartModelListener.events.toString(), 3,
            bartModelListener.events.size());
        assertTrue(bartModelListener.events.get(0).getType() == TableModelEvent.INSERT);
        assertTrue(bartModelListener.events.get(1).getType() == TableModelEvent.UPDATE);
        assertTrue(bartModelListener.events.get(2).getType() == TableModelEvent.DELETE);
    }

    public void testDuplicateRequestedUpload() throws ConnectionException {
        // Create a 10 megs file
        TestHelper.createRandomFile(folderBart.getLocalBase(), 10000000);

        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        
        // wait for 1 active upload
        TestHelper.waitForCondition(2, new TestHelper.Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length >= 1;
            }
        });
        TestHelper.waitMilliSeconds(500);

        // Fake another request of the file
        FileInfo testFile = folderBart.getFiles()[0];
        Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getId());
        assertTrue(bartAtLisa.isCompleteyConnected());
        bartAtLisa.sendMessage(new RequestDownload(testFile));

        TestHelper.waitMilliSeconds(1000);
        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 0
                    && getContollerBart().getTransferManager()
                        .getQueuedUploads().length == 0;
            }
        });

        // Model should be empty
        assertEquals(0, bartModel.getRowCount());

        Download dl = getContollerLisa().getTransferManager().getActiveDownload(testFile);
        if (dl != null) {
            dl.abortAndCleanup();
        }
    }

    public void testRunningUpload() {
        // Create a 10 megs file
        TestHelper.createRandomFile(folderBart.getLocalBase(), 10000000);
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        
        TestHelper.waitForCondition(2, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() > 0;
            }
        });

        // 1 active uploads
        assertEquals(1, bartModel.getRowCount());

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount());

        TestHelper.waitMilliSeconds(1000);
    }

    public void testAbortUpload() {
        // Create a 10 megs file
        TestHelper.createRandomFile(folderBart.getLocalBase(), 10000000);
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        
        TestHelper.waitForCondition(2, new TestHelper.Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 1;
            }
        });
        TestHelper.waitMilliSeconds(200);
        
        assertEquals(1, bartModel.getRowCount());
        // Upload requested + enqueud
        assertEquals(2, bartModelListener.events.size());

        // Abort
        Download download = getContollerLisa().getTransferManager()
            .getActiveDownloads()[0];
        download.abort();

        TestHelper.waitForCondition(50, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // Wait for EDT
        TestHelper.waitMilliSeconds(500);
        
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
        
        TestHelper.waitMilliSeconds(500);
    }

    public void testDisconnectWhileUpload() {
        // Create a 10 megs file
        TestHelper.createRandomFile(folderBart.getLocalBase(), 10000000);
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();
        
        TestHelper.waitForCondition(2, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() > 0;
            }
        });
        TestHelper.waitMilliSeconds(200);

        Member bartOnLisa = getContollerLisa().getNodeManager()
            .getConnectedNodes().get(0);

        // Disconnect
        bartOnLisa.shutdown();

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // Give EDT time
        TestHelper.waitMilliSeconds(500);
        
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

        TestHelper.waitMilliSeconds(500);
    }

    private class MyUploadTableModelListener implements TableModelListener {
        public List<TableModelEvent> events = new ArrayList<TableModelEvent>();

        public void tableChanged(TableModelEvent e) {
            events.add(e);
        }
    }
}
