/* $Id: FolderJoinTest.java,v 1.2 2006/04/16 23:01:52 totmacherr Exp $
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.junit.transfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.junit.TwoControllerTestCase;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Tests if both instance join the same folder by folder id
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FileTransferTest extends TwoControllerTestCase {

    private static final String BASEDIR1 = "build/test/controller1/testFolder";
    private static final String BASEDIR2 = "build/test/controller2/testFolder";

    private Folder folder1;
    private Folder folder2;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);

        folder1 = getContoller1().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR1));

        folder2 = getContoller2().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR2));

        // Give them time to join
        Thread.sleep(500);

        checkFolderJoined();
    }

    /**
     * Helper to check that controllers have join all folders
     */
    private void checkFolderJoined() {
        assertEquals(2, folder1.getMembersCount());
        assertEquals(2, folder2.getMembersCount());
    }

    public void testSmallFileCopy() throws IOException, InterruptedException {
        // Set both folders to auto download
        folder1.setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        folder2.setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        FileOutputStream fOut = new FileOutputStream(folder1.getLocalBase()
            .getAbsoluteFile()
            + "/TestFile.txt");
        fOut.write("This is the contenent of the testfile".getBytes());
        fOut.close();

        // Let him scan the new content
        folder1.forceNextScan();
        folder1.scan();

        // Give them time to copy
        Thread.sleep(500);

        // Test ;)
        assertEquals(1, folder2.getFilesCount());

        // No active downloads?
        assertEquals(0, getContoller2().getTransferManager()
            .getActiveDownloadCount());
    }

    public void testFileUpdate() throws IOException, InterruptedException {
        // Set both folders to auto download
        folder1.setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        folder2.setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        // First copy file
        testSmallFileCopy();

        File testFile1 = new File(folder1.getLocalBase() + "/TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFile1, true);
        fOut.write("-> Next content<-".getBytes());
        fOut.close();

        // Let him scan the new content
        folder1.forceNextScan();
        folder1.scan();

        // Give them time to copy
        Thread.sleep(1000);

        // Test ;)
        assertEquals(1, folder2.getFilesCount());
        FileInfo testFileInfo2 = folder2.getFiles()[0];
        assertEquals(testFile1.length(), testFileInfo2.getSize());

        // Check version
        assertEquals(1, testFileInfo2.getVersion());
    }

    public void testEmptyFileCopy() throws IOException, InterruptedException {
        // Set both folders to auto download
        folder1.setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        folder2.setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        
        // Register listeners
        MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContoller1().getTransferManager().addListener(tm1Listener);
        MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContoller2().getTransferManager().addListener(tm2Listener);

        File testFile1 = new File(folder1.getLocalBase() + "/TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFile1);
        fOut.write(new byte[]{});
        fOut.close();
        assertTrue(testFile1.exists());

        // Let him scan the new content
        folder1.forceNextScan();
        folder1.scan();

        // Give them time to copy
        Thread.sleep(500);

        // Check correct event fireing
        assertTrue(tm1Listener.uploadRequested);
        assertTrue(tm1Listener.uploadStarted);
        assertTrue(tm1Listener.uploadCompleted);

        // Check correct event fireing
        assertTrue(tm2Listener.downloadRequested);
        assertTrue(tm2Listener.downloadQueued);
        assertTrue(tm2Listener.downloadStarted);
        assertTrue(tm2Listener.downloadCompleted);

        // Test ;)
        assertEquals(1, folder2.getFilesCount());

        // No active downloads?
        assertEquals(0, getContoller2().getTransferManager()
            .getActiveDownloadCount());
    }

    /**
     * For checking the correct events.
     */
    private class MyTransferManagerListener implements TransferManagerListener {
        public boolean downloadRequested;
        public boolean downloadQueued;
        public boolean downloadStarted;
        public boolean downloadBroken;
        public boolean downloadAborted;
        public boolean downloadCompleted;

        public boolean uploadRequested;
        public boolean uploadStarted;
        public boolean uploadBroken;
        public boolean uploadAborted;
        public boolean uploadCompleted;

        public void downloadRequested(TransferManagerEvent event) {
            downloadRequested = true;
        }

        public void downloadQueued(TransferManagerEvent event) {
            downloadQueued = true;

        }

        public void downloadStarted(TransferManagerEvent event) {
            downloadStarted = true;
        }

        public void downloadAborted(TransferManagerEvent event) {
            downloadAborted = true;
        }

        public void downloadBroken(TransferManagerEvent event) {
            downloadBroken = true;
        }

        public void downloadCompleted(TransferManagerEvent event) {
            downloadCompleted = true;
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            // TODO Auto-generated method stub

        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            // TODO Auto-generated method stub

        }

        public void uploadRequested(TransferManagerEvent event) {
            uploadRequested = true;

        }

        public void uploadStarted(TransferManagerEvent event) {
            uploadStarted = true;

        }

        public void uploadAborted(TransferManagerEvent event) {
            uploadAborted = true;

        }

        public void uploadBroken(TransferManagerEvent event) {
            uploadAborted = true;
        }

        public void uploadCompleted(TransferManagerEvent event) {
            uploadCompleted = true;
        }

    }
}
