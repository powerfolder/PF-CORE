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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
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
                    + getFolderAtHomer().getLocalBase().toFile().list().length
                    + ", Marge "
                    + getFolderAtMarge().getLocalBase().toFile().list().length
                    + ", Lisa "
                    + getFolderAtLisa().getLocalBase().toFile().list().length
                    + ", Maggie "
                    + getFolderAtMaggie().getLocalBase().toFile().list().length;
            }

            public boolean reached() {
                return getFolderAtHomer().getLocalBase().toFile().list().length == 71
                    && getFolderAtMarge().getLocalBase().toFile().list().length == 71
                    && getFolderAtLisa().getLocalBase().toFile().list().length == 71
                    && getFolderAtMaggie().getLocalBase().toFile().list().length == 71;
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

        Path testDirBart = getFolderAtBart().getLocalBase().resolve("testdir");
        try {
            Files.createDirectory(testDirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        scanFolder(getFolderAtBart());

        Path testDirLisa = getFolderAtLisa().getLocalBase().resolve("TESTDIR");
        try {
            Files.createDirectory(testDirLisa);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }

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
            try {
                Files.createDirectory(getFolderAtBart().getLocalBase().resolve(
                    "testdir-" + i));
            } catch (IOException ioe) {
                fail(ioe.getMessage());
            }
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
            Files.exists(foLisa.getSystemSubDir().resolve(Constants.DB_FILENAME)));
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
        Path fileAtBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), "subdirectory/sourcedir/Testfile.txt");
        scanFolder(getFolderAtBart());
        Path fileAtLisa = getFolderAtLisa().getLocalBase().resolve(
            "subdirectory/Sourcedir/Testfile.txt");
        Files.copy(fileAtBart, fileAtLisa);
        Files.setLastModifiedTime(fileAtLisa, Files.getLastModifiedTime(fileAtBart));
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

    private static void assertIdenticalContent(Path dir1, Path dir2) {
        int size1 = 0, size2 = 0;
        List<Path> entries1 = new ArrayList<Path>();
        List<Path> entries2 = new ArrayList<Path>();
        try (DirectoryStream<Path> stream1 = Files.newDirectoryStream(dir1);
            DirectoryStream<Path> stream2 = Files.newDirectoryStream(dir2))
        {
            for (Path path : stream1) {
                entries1.add(path);
                size1++;
            }
            for (Path path : stream2) {
                entries2.add(path);
                size2++;
            }
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        assertEquals(size1, size2);
        Collections.sort(entries1);
        Collections.sort(entries2);

        for (int i = 0; i < entries1.size(); i++) {
            Path path1 = entries1.get(i);
            Path path2 = entries2.get(i);
            if (Files.isDirectory(path1) && Files.isDirectory(path2)) {
                // Skip
                continue;
            }

            try {
                assertEquals("File length mismatch: " + path1.toAbsolutePath(),
                    Files.size(path1), Files.size(path2));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        int nRemoved, Path dir)
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
        private Path dir;

        private AddFileOperation(Path dir) {
            super();
            this.dir = dir;
        }

        public void run() {
            TestHelper.createRandomFile(dir);
        }
    }

    private static class ChangeFileOperation implements Runnable {
        private Path dir;
        private int index;

        private ChangeFileOperation(Path dir, int index) {
            super();
            this.dir = dir;
            this.index = index;
        }

        public void run() {
            Filter<Path> filter = new Filter<Path>() {
                @Override
                public boolean accept(Path entry) {
                    return Files.isRegularFile(entry);
                }
            };

            List<Path> fList = new ArrayList<Path>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, filter)) {
                for (Path file : files) {
                    fList.add(file);
                }
                Path file = fList.get(index % fList.size());
                TestHelper.changeFile(file);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    private static class RemoveFileOperation implements Runnable {
        private Path dir;

        private RemoveFileOperation(Path dir) {
            super();
            this.dir = dir;
        }

        public void run() {
            Filter<Path> filter = new Filter<Path>() {
                @Override
                public boolean accept(Path entry) {
                    return Files.isRegularFile(entry);
                }
            };

            List<Path> fList = new ArrayList<Path>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, filter)) {
                for (Path file : files) {
                    fList.add(file);
                }
                Path file = fList.get((int) (Math.random() * fList.size()));
                Files.delete(file);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
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
