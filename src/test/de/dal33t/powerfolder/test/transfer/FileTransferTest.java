/* $Id: FolderJoinTest.java,v 1.2 2006/04/16 23:01:52 totmacherr Exp $
 */
package de.dal33t.powerfolder.test.transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper.Condition;

/**
 * Tests file transfer between nodes.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FileTransferTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        // Join on testfolder
        setupTestFolder(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
    }

    public void testSmallFileCopy() throws IOException {
        File testFileBart = new File(getFolderAtBart().getLocalBase(),
            "TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFileBart);
        byte[] testContent = "This is the contenent of the testfile".getBytes();
        fOut.write(testContent);
        fOut.close();

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        assertEquals(1, getFolderAtBart().getFilesCount());

        // Give them time to copy
        TestHelper.waitMilliSeconds(2000);

        // Test ;)
        assertEquals(1, getFolderAtLisa().getFilesCount());

        File testFileLisa = new File(getFolderAtLisa().getLocalBase(),
            "TestFile.txt");
        assertEquals(testContent.length, testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());
    }

    public void testFileUpdate() throws IOException {
        // First copy file
        testSmallFileCopy();

        File testFile1 = new File(getFolderAtBart().getLocalBase()
            + "/TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFile1, true);
        fOut.write("-> Next content<-".getBytes());
        fOut.close();

        // Readin file content
        FileInputStream fIn = new FileInputStream(testFile1);
        byte[] content1 = new byte[fIn.available()];
        fIn.read(content1);
        fIn.close();

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        // Give them time to copy
        TestHelper.waitMilliSeconds(5000);

        // Test ;)
        assertEquals(1, getFolderAtLisa().getFilesCount());
        FileInfo testFileInfo2 = getFolderAtLisa().getFiles()[0];
        assertEquals(testFile1.length(), testFileInfo2.getSize());

        // Read content
        File testFile2 = testFileInfo2.getDiskFile(getContollerLisa()
            .getFolderRepository());
        fIn = new FileInputStream(testFile2);
        byte[] conten2 = new byte[fIn.available()];
        fIn.read(conten2);
        fIn.close();

        // Check version
        assertEquals(1, testFileInfo2.getVersion());

        // Check content
        assertEquals(new String(content1), new String(conten2));
    }

    public void testEmptyFileCopy() throws IOException, InterruptedException {
        // Register listeners
        MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        File testFile1 = new File(getFolderAtBart().getLocalBase()
            + "/TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFile1);
        fOut.write(new byte[]{});
        fOut.close();
        assertTrue(testFile1.exists());

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadCompleted >= 1;
            }
        });

        // Check correct event fireing
        assertEquals(1, tm1Listener.uploadRequested);
        assertEquals(1, tm1Listener.uploadStarted);
        assertEquals(1, tm1Listener.uploadCompleted);

        // Check correct event fireing
        assertEquals(1, tm2Listener.downloadRequested);
        assertEquals(1, tm2Listener.downloadQueued);
        assertEquals(1, tm2Listener.downloadStarted);
        assertEquals(1, tm2Listener.downloadCompleted);
        assertEquals(0, tm2Listener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(1, getFolderAtLisa().getFilesCount());
        // 2 physical files (1 + 1 system dir)
        assertEquals(2, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        // Clear completed downloads
        getContollerLisa().getTransferManager().clearCompletedDownloads();
        // give time for event firering
        Thread.sleep(500);
        assertEquals(1, tm2Listener.downloadsCompletedRemoved);
    }

    /**
     * Tests the copy of a big file. approx. 10 megs.
     */
    public void testBigFileCopy() {
        // Register listeners
        MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        // 1Meg testfile
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 1000000);

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadCompleted >= 1;
            }
        });

        // Check correct event fireing
        assertEquals(1, tm1Listener.uploadRequested);
        assertEquals(1, tm1Listener.uploadStarted);
        assertEquals(1, tm1Listener.uploadCompleted);

        // Check correct event fireing
        assertEquals(1, tm2Listener.downloadRequested);
        assertEquals(1, tm2Listener.downloadQueued);
        assertEquals(1, tm2Listener.downloadStarted);
        assertEquals(1, tm2Listener.downloadCompleted);
        assertEquals(0, tm2Listener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(1, getFolderAtLisa().getFilesCount());
        // 2 physical files (1 + 1 system dir)
        assertEquals(2, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        // Clear completed downloads
        getContollerLisa().getTransferManager().clearCompletedDownloads();
        // give time for event firering
        TestHelper.waitMilliSeconds(500);
        assertEquals(1, tm2Listener.downloadsCompletedRemoved);
    }

    public void testMultipleFileCopy() {
        // Register listeners
        MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }

        // Let him scan the new content
        getFolderAtBart().forceScanOnNextMaintenance();
        getFolderAtBart().maintain();

        assertEquals(nFiles, getFolderAtBart().getFilesCount());

        // Wait for copy (timeout 50)
        TestHelper.waitForCondition(120, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadCompleted >= nFiles;
            }
        });

        // Check correct event fireing
        assertEquals(nFiles, tm1Listener.uploadRequested);
        assertEquals(nFiles, tm1Listener.uploadStarted);
        assertEquals(nFiles, tm1Listener.uploadCompleted);

        // Check correct event fireing
        assertEquals(nFiles, tm2Listener.downloadRequested);
        assertEquals(nFiles, tm2Listener.downloadQueued);
        assertEquals(nFiles, tm2Listener.downloadStarted);
        assertEquals(nFiles, tm2Listener.downloadCompleted);
        assertEquals(0, tm2Listener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getFilesCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        // Clear completed downloads
        getContollerLisa().getTransferManager().clearCompletedDownloads();

        TestHelper.waitMilliSeconds(500);
        assertEquals(nFiles, tm2Listener.downloadsCompletedRemoved);
    }

    /**
     * For checking the correct events.
     */
    private class MyTransferManagerListener implements TransferManagerListener {
        public int downloadRequested;
        public int downloadQueued;
        public int pendingDownloadEnqued;
        public int downloadStarted;
        public int downloadBroken;
        public int downloadAborted;
        public int downloadCompleted;
        public int downloadsCompletedRemoved;

        public int uploadRequested;
        public int uploadStarted;
        public int uploadBroken;
        public int uploadAborted;
        public int uploadCompleted;

        public void downloadRequested(TransferManagerEvent event) {
            downloadRequested++;
        }

        public void downloadQueued(TransferManagerEvent event) {
            downloadQueued++;
        }

        public void downloadStarted(TransferManagerEvent event) {
            downloadStarted++;
        }

        public void downloadAborted(TransferManagerEvent event) {
            downloadAborted++;
        }

        public void downloadBroken(TransferManagerEvent event) {
            downloadBroken++;
        }

        public void downloadCompleted(TransferManagerEvent event) {
            downloadCompleted++;
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            downloadsCompletedRemoved++;
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            pendingDownloadEnqued++;
        }

        public void uploadRequested(TransferManagerEvent event) {
            uploadRequested++;

        }

        public void uploadStarted(TransferManagerEvent event) {
            uploadStarted++;

        }

        public void uploadAborted(TransferManagerEvent event) {
            uploadAborted++;
        }

        public void uploadBroken(TransferManagerEvent event) {
            uploadAborted++;
        }

        public void uploadCompleted(TransferManagerEvent event) {
            uploadCompleted++;
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
