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
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.filefilter.FileFilterUtils;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.EqualsCondition;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class MirrorFolderTest extends FiveControllerTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(tryToConnectSimpsons());
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    public void xtestRandomSyncOperationsMultiple() throws Exception {
        for (int i = 0; i < 10; i++) {
            testRandomSyncOperations();
            tearDown();
            setUp();
        }
    }

    public void testRandomSyncOperations() {
        performRandomOperations(100, 70, 0, getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        waitForCompletedDownloads(100, 0, 100, 100, 100);
        assertIdenticalTestFolder();

        // Step 2) Remove operations
        performRandomOperations(0, 0, 30, getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public String message() {
                return "Delete sync not completed. Files in folder: Homer "
                    + getFolderAtHomer().getLocalBase().list().length
                    + ", Marge "
                    + getFolderAtMarge().getLocalBase().list().length
                    + ", Lisa "
                    + getFolderAtLisa().getLocalBase().list().length
                    + ", Maggie "
                    + getFolderAtMaggie().getLocalBase().list().length;
            }

            public boolean reached() {
                return getFolderAtHomer().getLocalBase().list().length == 71
                    && getFolderAtMarge().getLocalBase().list().length == 71
                    && getFolderAtLisa().getLocalBase().list().length == 71
                    && getFolderAtMaggie().getLocalBase().list().length == 71;
            }
        });

        assertIdenticalTestFolder();
        clearCompletedDownloads();

        // Step 3) Change operations
        performRandomOperations(0, 50, 0, getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        waitForCompletedDownloads(50, 0, 50, 50, 50);
        assertIdenticalTestFolder();
    }

    public void testMixedCaseSubdirs2() {
        // Emulate Windows.
        // FileInfo.IGNORE_CASE = true;
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        getFolderAtHomer().setSyncProfile(SyncProfile.NO_SYNC);
        getFolderAtMarge().setSyncProfile(SyncProfile.NO_SYNC);
        getFolderAtMaggie().setSyncProfile(SyncProfile.NO_SYNC);
        disconnectAll();

        File testDirBart = new File(getFolderAtBart().getLocalBase(), "testdir");
        assertTrue(testDirBart.mkdirs());
        scanFolder(getFolderAtBart());

        File testDirLisa = new File(getFolderAtLisa().getLocalBase(), "TESTDIR");
        assertTrue(testDirLisa.mkdirs());

        connectAll();
        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        TestHelper.waitForCondition(10, new ConditionWithMessage() {

            public boolean reached() {
                return getFolderAtLisa().getKnownDirectories().size() == 1;
            }

            public String message() {
                return "Items at lisa: "
                    + getFolderAtLisa().getKnownDirectories();
            }
        });
        DirectoryInfo testDirInfoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertEquals("testdir", testDirInfoLisa.getRelativeName());
        // null = IN SYNC
        assertNull(testDirInfoLisa.syncFromDiskIfRequired(getFolderAtLisa(),
            testDirLisa));
    }

    /**
     * TRAC #1960
     */
    public void testNoDbAfterDirectorySync() {
        getContollerBart().getTransferManager().setUploadCPSForLAN(1000);
        final Folder foLisa = getFolderAtLisa();
        assertTrue(foLisa.hasOwnDatabase());
        assertTrue(getFolderAtLisa().hasOwnDatabase());

        for (int i = 0; i < 500; i++) {
            new File(getFolderAtBart().getLocalBase(), "testdir-" + i).mkdirs();
        }

        // 20 MB testfile
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 2000000);
        getFolderAtBart().getFolderWatcher().setIngoreAll(false);

        scanFolder(getFolderAtBart());

        Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getInfo());
        TestHelper.waitForCondition(30, new EqualsCondition() {
            public Object expected() {
                return 500;
            }

            public Object actual() {
                return foLisa.getKnownDirectories().size();
            }
        });
        assertEquals(1, getFolderAtLisa().getIncomingFiles().size());
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() >= 1;
            }

            public String message() {
                return "Downloads at lisa: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });
        bartAtLisa.shutdown();
        // Only dirs. No files
        assertEquals(500, foLisa.getKnownItemCount());
        getContollerLisa().getFolderRepository().removeFolder(foLisa, false);

        // DB Must have been stored
        assertTrue(
            "Database file was NOT saved at Lisa although directory have been synced",
            new File(foLisa.getSystemSubDir(), Constants.DB_FILENAME).exists());
    }

    public void testMixedCaseSubdirs() throws IOException {
        // Emulate Windows.
        // FileInfo.IGNORE_CASE = true;
        if (!OSUtil.isWindowsSystem()) {
            return;
        }
        getFolderAtHomer().setSyncProfile(SyncProfile.NO_SYNC);
        getFolderAtMarge().setSyncProfile(SyncProfile.NO_SYNC);
        getFolderAtMaggie().setSyncProfile(SyncProfile.NO_SYNC);

        MyTransferManagerListener bartListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartListener);
        File fileAtBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), "subdirectory/sourcedir/Testfile.txt");
        scanFolder(getFolderAtBart());
        File fileAtLisa = new File(getFolderAtLisa().getLocalBase(),
            "subdirectory/Sourcedir/Testfile.txt");
        FileUtils.copyFile(fileAtBart, fileAtLisa);
        fileAtLisa.setLastModified(fileAtBart.lastModified());
        scanFolder(getFolderAtLisa());
        connectSimpsons();

        getContollerBart().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();
        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();
        TestHelper.waitMilliSeconds(500);

        assertEquals("" + getFolderAtBart().getIncomingFiles(), 0,
            getFolderAtBart().getIncomingFiles().size());
        assertEquals(0, getFolderAtLisa().getIncomingFiles().size());
        assertEquals(0, bartListener.uploadRequested);
        assertEquals(0, bartListener.uploadStarted);
        assertEquals(0, bartListener.uploadCompleted);
        assertEquals(0, bartListener.uploadAborted);
        assertEquals(0, bartListener.uploadBroken);
        assertEquals(0, bartListener.downloadRequested);
        assertEquals(0, bartListener.downloadQueued);
        assertEquals(0, bartListener.downloadStarted);
        assertEquals(0, bartListener.downloadCompleted);
        assertEquals(0, bartListener.downloadAborted);
        assertEquals(0, bartListener.downloadBroken);
        assertEquals(0, bartListener.downloadsCompletedRemoved);

        assertEquals(0, getContollerBart().getTransferManager()
            .getCompletedDownloadsCollection().size());
        assertEquals(0, getContollerLisa().getTransferManager()
            .getCompletedDownloadsCollection().size());

        assertEquals(3, getFolderAtBart().getKnownItemCount());
        assertEquals(3, getFolderAtLisa().getKnownItemCount());
        FileInfo fBart = getFolderAtBart().getKnownFiles().iterator().next();
        FileInfo fLisa = getFolderAtLisa().getKnownFiles().iterator().next();

        assertEquals(0, fBart.getVersion());
        assertFalse(fBart.isDeleted());
        assertFileMatch(fileAtBart, fBart, getContollerBart());

        assertEquals(0, fLisa.getVersion());
        assertFalse(fLisa.isDeleted());
        assertFileMatch(fileAtLisa, fLisa, getContollerLisa());
    }

    private void assertIdenticalTestFolder() {
        assertIdenticalContent(getFolderAtBart().getLocalBase(),
            getFolderAtHomer().getLocalBase());
        assertIdenticalContent(getFolderAtBart().getLocalBase(),
            getFolderAtMarge().getLocalBase());
        assertIdenticalContent(getFolderAtBart().getLocalBase(),
            getFolderAtLisa().getLocalBase());
        assertIdenticalContent(getFolderAtBart().getLocalBase(),
            getFolderAtMaggie().getLocalBase());
    }

    private static void assertIdenticalContent(File dir1, File dir2) {
        assertEquals(dir1.listFiles().length, dir2.listFiles().length);
        String[] files1 = dir1.list();
        Arrays.sort(files1);
        String[] files2 = dir2.list();
        Arrays.sort(files2);

        for (int i = 0; i < files1.length; i++) {

            File file1 = new File(dir1, files1[i]);
            File file2 = new File(dir2, files2[i]);
            if (file1.isDirectory() && file2.isDirectory()) {
                // Skip
                continue;
            }
            assertEquals("File lenght mismatch: " + file1.getAbsolutePath(),
                file1.length(), file2.length());
        }
        //        
        // int size1 = 0;
        // for (File file : ) {
        // size1 += file.length();
        // }
        // int size2 = 0;
        // for (File file : dir2.listFiles()) {
        // size2 += file.length();
        // }
        // assertEquals(Arrays.asList(dir1.list()) + " <-> "
        // + Arrays.asList(dir2.list()), size1, size2);
    }

    private static void performRandomOperations(int nAdded, int nChanged,
        int nRemoved, File dir)
    {
        for (int i = 0; i < nAdded; i++) {
            new AddFileOperation(dir).run();
        }
        for (int i = 0; i < nChanged; i++) {
            new ChangeFileOperation(dir, i).run();
        }
        for (int i = 0; i < nRemoved; i++) {
            new RemoveFileOperation(dir).run();
        }
    }

    private static class AddFileOperation implements Runnable {
        private File dir;

        private AddFileOperation(File dir) {
            super();
            this.dir = dir;
        }

        public void run() {
            TestHelper.createRandomFile(dir);
        }
    }

    private static class ChangeFileOperation implements Runnable {
        private File dir;
        private int index;

        private ChangeFileOperation(File dir, int index) {
            super();
            this.dir = dir;
            this.index = index;
        }

        public void run() {
            File[] files = dir.listFiles((FileFilter) FileFilterUtils
                .fileFileFilter());
            if (files.length == 0) {
                return;
            }
            File file = files[index % files.length];
            TestHelper.changeFile(file);
        }
    }

    private static class RemoveFileOperation implements Runnable {
        private File dir;

        private RemoveFileOperation(File dir) {
            super();
            this.dir = dir;
        }

        public void run() {
            File[] files = dir.listFiles((FileFilter) FileFilterUtils
                .fileFileFilter());
            if (files.length == 0) {
                return;
            }
            File file = files[(int) (Math.random() * files.length)];
            file.delete();
        }
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

        public int uploadsCompletedRemoved;
        public int uploadRequested;
        public int uploadStarted;
        public int uploadBroken;
        public int uploadAborted;
        public int uploadCompleted;

        private TransferManagerEvent lastEvent;

        public List<FileInfo> uploadsRequested = new ArrayList<FileInfo>();
        public List<FileInfo> downloadsRequested = new ArrayList<FileInfo>();
        private final boolean failOnSecondRequest;

        public MyTransferManagerListener(boolean failOnSecondRequest) {
            this.failOnSecondRequest = failOnSecondRequest;
        }

        public MyTransferManagerListener() {
            failOnSecondRequest = false;
        }

        public synchronized void downloadRequested(TransferManagerEvent event) {
            downloadRequested++;
            if (downloadsRequested.contains(event.getFile())) {
                if (failOnSecondRequest) {
                    fail("Second download request for "
                        + event.getFile().toDetailString());
                } else {
                    System.err.println("Second download request for "
                        + event.getFile().toDetailString());
                }
            }
            downloadsRequested.add(event.getFile());
        }

        public synchronized void downloadQueued(TransferManagerEvent event) {
            downloadQueued++;
            lastEvent = event;
        }

        public synchronized void downloadStarted(TransferManagerEvent event) {
            downloadStarted++;
            lastEvent = event;
        }

        public synchronized void downloadAborted(TransferManagerEvent event) {
            downloadAborted++;
        }

        public synchronized void downloadBroken(TransferManagerEvent event) {
            downloadBroken++;
            lastEvent = event;
        }

        public synchronized void downloadCompleted(TransferManagerEvent event) {
            downloadCompleted++;
            lastEvent = event;
        }

        public synchronized void completedDownloadRemoved(
            TransferManagerEvent event)
        {
            downloadsCompletedRemoved++;
            lastEvent = event;
        }

        public synchronized void pendingDownloadEnqueued(
                TransferManagerEvent event)
        {
            pendingDownloadEnqued++;
            lastEvent = event;
        }

        public synchronized void uploadRequested(TransferManagerEvent event) {
            uploadRequested++;
            lastEvent = event;

            if (uploadsRequested.contains(event.getFile())) {
                System.err.println("Second upload request for "
                    + event.getFile().toDetailString());
            }
            uploadsRequested.add(event.getFile());
        }

        public synchronized void uploadStarted(TransferManagerEvent event) {
            uploadStarted++;
            lastEvent = event;
        }

        public synchronized void uploadAborted(TransferManagerEvent event) {
            uploadAborted++;
            lastEvent = event;
        }

        public synchronized void uploadBroken(TransferManagerEvent event) {
            uploadAborted++;
            lastEvent = event;
        }

        public synchronized void uploadCompleted(TransferManagerEvent event) {
            uploadCompleted++;
            lastEvent = event;
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

        public synchronized void completedUploadRemoved(
            TransferManagerEvent event)
        {
            uploadsCompletedRemoved++;
            lastEvent = event;
        }

    }
}
