package de.dal33t.powerfolder.test.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.test.Condition;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.test.folder.ScanFolderTest;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.transfer.UploadsTableModel;

/**
 * Tests the upload table model.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UploadsTableModelTest extends TwoControllerTestCase {

    private UploadsTableModel bartModel;
    private MyUploadTableModelListener bartModelListener;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        bartModelListener = new MyUploadTableModelListener();
        bartModel = new UploadsTableModel(getContollerBart()
            .getTransferManager(), false);
        bartModel.addTableModelListener(bartModelListener);
    }

    public void testSingleFileUpload() {
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());
        
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
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10000000);
        scanFolder(getFolderAtBart());

        // wait for 1 active upload
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length >= 1;
            }
        });
        TestHelper.waitForEmptyEDT();

        // Fake another request of the file
        FileInfo testFile = getFolderAtBart().getKnownFiles()[0];
        Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getId());
        assertTrue(bartAtLisa.isCompleteyConnected());
        bartAtLisa.sendMessage(new RequestDownload(testFile));

        TestHelper.waitMilliSeconds(1000);
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 0
                    && getContollerBart().getTransferManager()
                        .getQueuedUploads().length == 0;
            }
        });

        // Model should be empty
        assertEquals(0, bartModel.getRowCount());

        Download dl = getContollerLisa().getTransferManager()
            .getActiveDownload(testFile);
        if (dl != null) {
            dl.abortAndCleanup();
        }
    }

    public void testRunningUpload() {
        // Create a 10 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10000000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(2, new Condition() {
            public boolean reached() {
                return bartModel.getRowCount() > 0;
            }
        });

        // 1 active uploads
        assertEquals(1, bartModel.getRowCount());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // no active upload
        assertEquals(0, bartModel.getRowCount());

        TestHelper.waitForEmptyEDT();
    }

    public void testAbortUpload() {
        assertEquals(0, bartModelListener.events.size());
        // Create a 20 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 20000000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 1;
            }
        });
        TestHelper.waitForEmptyEDT();

        assertEquals(1, bartModel.getRowCount());
        // Upload requested + started
        assertEquals(2, bartModelListener.events.size());

        // Abort
        Download download = getContollerLisa().getTransferManager()
            .getActiveDownloads()[0];
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        download.abort();

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

    public void testDisconnectWhileUpload() {
        // Create a 10 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10000000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return bartModel.getRowCount() > 0;
            }
        });
        TestHelper.waitForEmptyEDT();
        disconnectBartAndLisa();

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return bartModel.getRowCount() == 0;
            }
        });

        // Give EDT time
        TestHelper.waitForEmptyEDT();

        // no active upload
        assertEquals(0, bartModel.getRowCount());
        // Upload queued, started, aborted
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
            // System.err.println("Got event: " + e.getType() + " row: "
            // + e.getFirstRow() + "-" + e.getLastRow());
            events.add(e);
        }
    }
}
