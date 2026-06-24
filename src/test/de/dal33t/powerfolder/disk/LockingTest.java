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
 */
package de.dal33t.powerfolder.disk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.event.LockingEvent;
import de.dal33t.powerfolder.event.LockingListener;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class LockingTest extends TwoControllerTestCase {
    private Locking lockingBart;
    private LoggingLockingListener lockingListenerBart;
    private Locking lockingLisa;
    private LoggingLockingListener lockingListenerLisa;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestFolderContents();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        lockingBart = getContollerBart().getFolderRepository().getLocking();
        lockingListenerBart = new LoggingLockingListener();
        lockingBart.addListener(lockingListenerBart);
        lockingLisa = getContollerLisa().getFolderRepository().getLocking();
        lockingListenerLisa = new LoggingLockingListener();
        lockingLisa.addListener(lockingListenerLisa);
    }

    public void testLockUnlockLocal() {
        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1);
        scanFolder(getFolderAtBart());

        // Prepare and check
        FileInfo testFInfo = FileInfoFactory.lookupInstance(getFolderAtBart(),
            testFile);
        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));
        assertEquals(0, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.unlocked.size());

        // Lock
        assertTrue(lockingBart.lock(testFInfo));

        // Test
        assertTrue(lockingBart.isLocked(testFInfo));
        Lock lock = lockingBart.getLock(testFInfo);
        assertNotNull(lock);
        assertEquals(testFInfo, lock.getFileInfo());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            lock.getMemberInfo());
        assertEquals(getContollerBart().getOSClient().getAccountInfo(),
            lock.getAccountInfo());

        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.unlocked.size());

        // Unlock
        assertTrue(lockingBart.unlock(testFInfo));

        // Test
        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));

        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(1, lockingListenerBart.unlocked.size());
    }

    public void testLockUnlockRemote() {
        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1);
        scanFolder(getFolderAtBart());

        // Prepare and check
        final FileInfo testFInfo = FileInfoFactory.lookupInstance(
            getFolderAtBart(), testFile);

        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));
        assertEquals(0, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.unlocked.size());

        assertFalse(lockingLisa.isLocked(testFInfo));
        assertNull(lockingLisa.getLock(testFInfo));
        assertTrue(lockingLisa.unlock(testFInfo));
        assertEquals(0, lockingListenerLisa.locked.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());

        // Lock
        assertTrue(lockingBart.lock(testFInfo));
        // Wait
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return lockingLisa.isLocked(testFInfo)
                    && lockingListenerLisa.locked.size() == 1;
            }

            @Override
            public String message() {
                return "File wasn't locked at lisa: " + testFInfo;
            }
        });

        // Test
        assertTrue(lockingLisa.isLocked(testFInfo));
        Lock lock = lockingLisa.getLock(testFInfo);
        assertNotNull(lock);
        assertEquals(testFInfo, lock.getFileInfo());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            lock.getMemberInfo());
        assertEquals(getContollerBart().getOSClient().getAccountInfo(),
            lock.getAccountInfo());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.unlocked.size());

        // Unlock
        assertTrue(lockingLisa.unlock(testFInfo));
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return !lockingBart.isLocked(testFInfo)
                    && lockingListenerBart.unlocked.size() == 1;
            }

            @Override
            public String message() {
                return "File wasn't unlocked at bart: " + testFInfo
                    + ". locked now? " + lockingBart.isLocked(testFInfo);
            }
        });
        
        // Test
        assertFalse(lockingLisa.isLocked(testFInfo));
        assertNull(lockingLisa.getLock(testFInfo));
        assertTrue(lockingLisa.unlock(testFInfo));
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(1, lockingListenerLisa.unlocked.size());

        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));
        assertEquals(1, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
    }
    
    public void testAutoLockMSOffice() throws IOException {
        lockingListenerLisa.locked.clear();
        lockingListenerLisa.unlocked.clear();
        lockingListenerLisa.forbidden.clear();
        lockingListenerBart.locked.clear();
        lockingListenerBart.unlocked.clear();
        lockingListenerBart.forbidden.clear();

        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1);
        final FileInfo testFInfo = FileInfoFactory.lookupInstance(
            getFolderAtBart(), testFile);
        TestHelper.scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return !getContollerBart().getTransferManager()
                    .getCompletedUploadsCollection().isEmpty()
                    && !getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().isEmpty();
            }

            @Override
            public String message() {
                return "Bart: Completed uploads: "
                    + getContollerBart().getTransferManager()
                        .getCompletedUploadsCollection()
                    + ". Lisa: Completed downloads: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection();
            }
        });

        assertTrue(Files.exists(getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName())));
        TestHelper.waitMilliSeconds(500);

        Path officeLockFile = TestHelper.createRandomFile(getFolderAtLisa()
            .getLocalBase(), Constants.MS_OFFICE_FILENAME_PREFIX
            + testFile.getFileName().toString().substring(2));

        TestHelper.scanFolder(getFolderAtLisa());
        TestHelper.waitMilliSeconds(500);

        assertEquals(0, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(0, lockingListenerLisa.forbidden.size());

        // PFC-2705: Now remove the lock file
        Files.delete(officeLockFile);
        TestHelper.scanFolder(getFolderAtLisa());
        TestHelper.waitMilliSeconds(500);

        assertEquals(0, lockingListenerLisa.forbidden.size());
        assertEquals(1, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(1, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());

        assertFalse(testFInfo.isLocked(getContollerLisa()));
        assertFalse(testFInfo.isLocked(getContollerBart()));
    }

    public void testAutoLockForbiddenMSOffice() {
        lockingListenerLisa.locked.clear();
        lockingListenerLisa.unlocked.clear();
        lockingListenerLisa.forbidden.clear();
        lockingListenerBart.locked.clear();
        lockingListenerBart.unlocked.clear();
        lockingListenerBart.forbidden.clear();

        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1);
        final FileInfo testFInfo = FileInfoFactory.lookupInstance(
            getFolderAtBart(), testFile);

        TestHelper.scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return !getContollerBart().getTransferManager()
                    .getCompletedUploadsCollection().isEmpty()
                    && !getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().isEmpty();
            }

            @Override
            public String message() {
                return "Bart: Completed uploads: "
                    + getContollerBart().getTransferManager()
                        .getCompletedUploadsCollection()
                    + ". Lisa: Completed downloads: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection();
            }
        });

        assertTrue(Files.exists(getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName())));

        lockingBart.lock(testFInfo);

        TestHelper.waitMilliSeconds(500);

        assertEquals(0, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(0, lockingListenerLisa.forbidden.size());

        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase(),
            Constants.MS_OFFICE_FILENAME_PREFIX
                + testFile.getFileName().toString());

        TestHelper.scanFolder(getFolderAtLisa());
        TestHelper.waitMilliSeconds(500);

        assertEquals(0, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(1, lockingListenerLisa.forbidden.size());
    }

    public void testAutoLockForbiddenOpenOffice() {
        lockingListenerLisa.locked.clear();
        lockingListenerLisa.unlocked.clear();
        lockingListenerLisa.forbidden.clear();
        lockingListenerBart.locked.clear();
        lockingListenerBart.unlocked.clear();
        lockingListenerBart.forbidden.clear();

        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1);
        final FileInfo testFInfo = FileInfoFactory.lookupInstance(
            getFolderAtBart(), testFile);

        TestHelper.scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return 1 == getFolderAtLisa().getIncomingFiles().size();
            }

            @Override
            public String message() {
                return "There are "
                    + getFolderAtLisa().getIncomingFiles().size()
                    + " incoming files. Should be 1";
            }
        });

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return 0 == getFolderAtLisa().getIncomingFiles().size();
            }

            @Override
            public String message() {
                return "There are "
                    + getFolderAtLisa().getIncomingFiles().size()
                    + " incoming files. Should be 0";
            }
        });

        assertTrue(Files.exists(getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName())));

        lockingBart.lock(testFInfo);

        TestHelper.waitMilliSeconds(500);

        assertEquals(0, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(0, lockingListenerLisa.forbidden.size());

        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase(),
            Constants.LIBRE_OFFICE_FILENAME_PREFIX
                + testFile.getFileName().toString());

        TestHelper.scanFolder(getFolderAtLisa());
        TestHelper.waitMilliSeconds(500);

        assertEquals(0, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(1, lockingListenerLisa.forbidden.size());
    }

    public void testLockUnlockMultiple() {
        for (int i = 0; i < 25; i++) {
            lockingListenerBart.locked.clear();
            lockingListenerBart.unlocked.clear();
            lockingListenerBart.forbidden.clear();
            testLockUnlockLocal();

            TestHelper.waitMilliSeconds(50);

            lockingListenerBart.locked.clear();
            lockingListenerBart.unlocked.clear();
            lockingListenerBart.forbidden.clear();
            lockingListenerLisa.locked.clear();
            lockingListenerLisa.unlocked.clear();
            lockingListenerLisa.forbidden.clear();
            testLockUnlockRemote();
        }
    }
    
    /**
     * PFS-1922/FYK-543-88331
     */
    public void testBrokenLockfile() {
        Path testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1);
        scanFolder(getFolderAtBart());

        // Prepare and check
        FileInfo testFInfo = FileInfoFactory.lookupInstance(getFolderAtBart(),
            testFile);
        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));
        assertEquals(0, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.unlocked.size());

        // Lock
        assertTrue(lockingBart.lock(testFInfo));
        
        Path lockFile = getLockFile(testFInfo);
        TestHelper.changeFile(lockFile, 0);

        // Test
        assertTrue(lockingBart.isLocked(testFInfo));
        Lock lock = lockingBart.getLock(testFInfo);
        assertNull(lock);

        // Unlock
        assertTrue(lockingBart.unlock(testFInfo));

        // Test
        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));

        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(1, lockingListenerBart.unlocked.size());
    }
    
    public void testLockUnlockSubFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "projects/team/shared";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        Path testFile = TestHelper.createRandomFile(sharedPath, "locked.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);
        assertNotNull(subFolder);
        assertTrue(subFolder.isSubFolder());

        FileInfo fileInTop = topFolder.getFileInfo(testFile);
        assertNotNull(fileInTop);
        assertEquals(subDir + "/locked.txt", fileInTop.getRelativeName());

        FileInfo fileInSub = FileInfoFactory.mapToSubFolder(fileInTop,
            subFolder.getInfo());
        assertNotNull(fileInSub);
        assertEquals("locked.txt", fileInSub.getRelativeName());

        // Not locked initially
        assertFalse(lockingBart.isLocked(fileInSub));
        assertNull(lockingBart.getLock(fileInSub));
        assertTrue(lockingBart.unlock(fileInSub));

        // Lock through the subfolder view
        assertTrue(lockingBart.lock(fileInSub));

        // The lock is stored at the top folder scoped to the subdir, so it is
        // visible both through the subfolder view and the top-folder view.
        assertTrue(lockingBart.isLocked(fileInSub));
        assertTrue(lockingBart.isLocked(fileInTop));
        assertNotNull(lockingBart.getLock(fileInSub));
        assertEquals(1, lockingListenerBart.locked.size());

        // The physical lock file lives in the TOP folder's meta-folder.
        Path topLockFile = getLockFile(fileInTop);
        assertNotNull(topLockFile);
        assertTrue("Lock file must be created in the top folder meta-folder: "
            + topLockFile, Files.exists(topLockFile));

        // Unlock through the subfolder view removes the top-scoped lock
        assertTrue(lockingBart.unlock(fileInSub));
        assertFalse(lockingBart.isLocked(fileInSub));
        assertFalse(lockingBart.isLocked(fileInTop));
        assertNull(lockingBart.getLock(fileInSub));
        assertFalse(Files.exists(topLockFile));
        assertEquals(1, lockingListenerBart.unlocked.size());
    }

    public void testSubFolderLockFileIsScopedToSubdirPath() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "data/reports/monthly";

        Path root = topFolder.getPhysicalDir();
        Path sharedPath = Files.createDirectories(root.resolve(subDir));
        Path nestedDir = Files.createDirectories(sharedPath.resolve("2024/q1"));
        Path testFile = TestHelper.createRandomFile(nestedDir, "stmt.txt");
        TestHelper.scanFolder(topFolder);

        DirectoryInfo dirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(dirInfo);
        assertNotNull(subFolder);

        FileInfo fileInTop = topFolder.getFileInfo(testFile);
        FileInfo fileInSub = FileInfoFactory.mapToSubFolder(fileInTop,
            subFolder.getInfo());
        assertEquals("2024/q1/stmt.txt", fileInSub.getRelativeName());

        assertTrue(lockingBart.lock(fileInSub));

        // The lock file must be located under the top folder's meta-folder
        // locks dir at the full subdir-scoped path.
        Folder topMeta = getContollerBart().getFolderRepository()
            .getMetaFolder(topFolder.getInfo());
        assertNotNull(topMeta);
        Path locksDir = topMeta.getLocalBase().resolve(
            Folder.METAFOLDER_LOCKS_DIR);
        Path expected = locksDir.resolve(FileInfoFactory.encodeIllegalChars(
            subDir + "/2024/q1/stmt.txt" + ".lck"));
        assertTrue("Expected subdir-scoped lock file: " + expected,
            Files.exists(expected));

        assertTrue(lockingBart.unlock(fileInSub));
        assertFalse(Files.exists(expected));
    }

    private Path getLockFile(FileInfo fInfo) {
        Folder metaFolder = getContollerBart().getFolderRepository()
            .getMetaFolder(fInfo.getFolderInfo());
        if (metaFolder == null) {
            return null;
        }
        Path baseDir = metaFolder.getLocalBase().resolve(
            Folder.METAFOLDER_LOCKS_DIR);
        return baseDir.resolve(FileInfoFactory.encodeIllegalChars(fInfo
            .getRelativeName() + ".lck"));
    }

    private class LoggingLockingListener implements LockingListener {
        List<LockingEvent> locked = new LinkedList<>();
        List<LockingEvent> unlocked = new LinkedList<>();
        List<LockingEvent> forbidden = new LinkedList<>();

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public void locked(LockingEvent event) {
            locked.add(event);

        }

        @Override
        public void unlocked(LockingEvent event) {
            unlocked.add(event);
        }

        @Override
        public void autoLockForbidden(LockingEvent event) {
            forbidden.add(event);
        }
    }
}
