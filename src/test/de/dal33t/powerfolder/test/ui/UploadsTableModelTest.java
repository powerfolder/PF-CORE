package de.dal33t.powerfolder.test.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.ui.transfer.UploadsTableModel;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

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
    protected void setUp() throws Exception {
        super.setUp();
        getContollerLisa().setSilentMode(true);
        getContollerBart().setSilentMode(true);
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        bartModelListener = new MyUploadTableModelListener();
        bartModel = new UploadsTableModel(new TransferManagerModel(
            getContollerBart().getTransferManager(), new NavTreeModel(
                getContollerBart())), false);
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

    public void testMultipleDRU() throws Exception {
        for (int i = 0; i < 10; i++) {
            testDuplicateRequestedUpload();
            tearDown();
            setUp();
        }
    }

    public void testDuplicateRequestedUpload() throws ConnectionException {
        // Create a 30 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 30000000);
        scanFolder(getFolderAtBart());

        // wait for 1 active upload
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length >= 1;
            }
        });

        // Fake another request of the file
        FileInfo testFile = getFolderAtBart().getKnownFiles().iterator().next();
        Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getId());
        assertTrue(bartAtLisa.isCompleteyConnected());
        bartAtLisa.sendMessage(new RequestDownload(testFile));

        // Should not be a problem
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 1
                    && getContollerBart().getTransferManager()
                        .getQueuedUploads().length == 0;
            }

            public String message() {
                return "Bart active upload: "
                    + getContollerBart().getTransferManager()
                        .getActiveUploads().length;
            }
        });

        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 0
                    && getContollerBart().getTransferManager()
                        .getQueuedUploads().length == 0;
            }

            public String message() {
                return "Bart active upload: "
                    + getContollerBart().getTransferManager()
                        .getActiveUploads().length;
            }
        });

        // Model should be empty
        assertEquals(0, bartModel.getRowCount());
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
        ConfigurationEntry.DOWNLOADLIMIT_LAN.setValue(getContollerLisa(), "1000");

        assertEquals(0, bartModelListener.events.size());
        // Create a 20 megs file
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 20000000);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .getActiveUploads().length == 1 &&
                    getContollerLisa().getTransferManager()
                    .getActiveDownloadCount() == 1;
            }
        });
        TestHelper.waitForEmptyEDT();

        assertEquals(1, bartModel.getRowCount());
        // Upload requested + started
        assertEquals(2, bartModelListener.events.size());

        // Abort
        DownloadManager download = getContollerLisa().getTransferManager()
            .getActiveDownloads().iterator().next();
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
