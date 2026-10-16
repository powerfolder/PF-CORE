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

 import de.dal33t.powerfolder.Constants;
 import de.dal33t.powerfolder.event.LockingEvent;
 import de.dal33t.powerfolder.event.LockingListener;
 import de.dal33t.powerfolder.light.DirectoryInfo;
 import de.dal33t.powerfolder.light.FileInfo;
 import de.dal33t.powerfolder.light.FileInfoFactory;
 import de.dal33t.powerfolder.util.test.ConditionWithMessage;
 import de.dal33t.powerfolder.util.test.TestHelper;
 import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import static org.junit.jupiter.api.Assertions.*;

 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.List;
 import java.util.concurrent.CopyOnWriteArrayList;

public class LockingTest extends TwoControllerTestCase {
    private Locking lockingBart;
    private LoggingLockingListener lockingListenerBart;
    private Locking lockingLisa;
    private LoggingLockingListener lockingListenerLisa;

    @BeforeEach
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

    @Test
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
        assertEquals(0, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.unlocked(testFInfo));

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

        assertEquals(1, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.unlocked(testFInfo));

        // Unlock
        assertTrue(lockingBart.unlock(testFInfo));

        // Test
        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));

        assertEquals(1, lockingListenerBart.locked(testFInfo));
        assertEquals(1, lockingListenerBart.unlocked(testFInfo));
    }

    @Test
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
        assertEquals(0, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.unlocked(testFInfo));

        assertFalse(lockingLisa.isLocked(testFInfo));
        assertNull(lockingLisa.getLock(testFInfo));
        assertTrue(lockingLisa.unlock(testFInfo));
        assertEquals(0, lockingListenerLisa.locked(testFInfo));
        assertEquals(0, lockingListenerLisa.unlocked(testFInfo));

        // Lock
        assertTrue(lockingBart.lock(testFInfo));
        waitForLocked(lockingLisa, lockingListenerLisa, testFInfo);

        // Test
        assertTrue(lockingLisa.isLocked(testFInfo));
        Lock lock = lockingLisa.getLock(testFInfo);
        assertNotNull(lock);
        assertEquals(testFInfo, lock.getFileInfo());
        assertEquals(getContollerBart().getMySelf().getInfo(),
            lock.getMemberInfo());
        assertEquals(getContollerBart().getOSClient().getAccountInfo(),
            lock.getAccountInfo());
        assertEquals(1, lockingListenerLisa.locked(testFInfo));
        assertEquals(0, lockingListenerLisa.unlocked(testFInfo));
        assertEquals(1, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.unlocked(testFInfo));

        // Unlock
        assertTrue(lockingLisa.unlock(testFInfo));
        waitForUnlocked(lockingBart, lockingListenerBart, testFInfo);

        // Test
        assertFalse(lockingLisa.isLocked(testFInfo));
        assertNull(lockingLisa.getLock(testFInfo));
        assertTrue(lockingLisa.unlock(testFInfo));
        assertEquals(1, lockingListenerLisa.locked(testFInfo));
        assertTrue(lockingListenerLisa.unlocked(testFInfo) >= 1,
            "No unlocked event at lisa");

        assertFalse(lockingBart.isLocked(testFInfo));
        assertNull(lockingBart.getLock(testFInfo));
        assertTrue(lockingBart.unlock(testFInfo));
        assertTrue(lockingListenerBart.unlocked(testFInfo) >= 1,
            "No unlocked event at bart");
        assertEquals(1, lockingListenerBart.locked(testFInfo));
    }

    private void waitForFileAtLisa(final FileInfo fInfo) {
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                FileInfo atLisa = getFolderAtLisa().getFile(fInfo);
                return atLisa != null
                    && atLisa.inSyncWithDisk(atLisa.getDiskFile(
                        getContollerLisa().getFolderRepository()));
            }

            @Override
            public String message() {
                return "File did not arrive at lisa: " + fInfo
                    + ". Known at lisa: " + getFolderAtLisa().getFile(fInfo)
                    + ". Incoming: " + getFolderAtLisa().getIncomingFiles();
            }
        });
    }

    private static void waitForLocked(final Locking locking,
        final LoggingLockingListener listener, final FileInfo fInfo)
    {
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return locking.isLocked(fInfo) && listener.locked(fInfo) >= 1;
            }

            @Override
            public String message() {
                return "File wasn't locked at " + locking.getController().getMySelf().getNick()
                    + ": " + fInfo + ". Lock file present? " + locking.isLocked(fInfo)
                    + ". Locked events: " + listener.locked;
            }
        });
    }

    private static void waitForUnlocked(final Locking locking,
        final LoggingLockingListener listener, final FileInfo fInfo)
    {
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return !locking.isLocked(fInfo) && listener.unlocked(fInfo) >= 1;
            }

            @Override
            public String message() {
                return "File wasn't unlocked at " + locking.getController().getMySelf().getNick()
                    + ": " + fInfo + ". Lock file present? " + locking.isLocked(fInfo)
                    + ". Unlocked events: " + listener.unlocked;
            }
        });
    }
    
    @Test
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

        waitForFileAtLisa(testFInfo);

        assertTrue(Files.exists(getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName())));
        TestHelper.waitMilliSeconds(500);

        Path officeLockFile = TestHelper.createRandomFile(getFolderAtLisa()
            .getLocalBase(), Constants.MS_OFFICE_FILENAME_PREFIX
            + testFile.getFileName().toString().substring(2));

        TestHelper.scanFolder(getFolderAtLisa());
        waitForLocked(lockingBart, lockingListenerBart, testFInfo);

        assertEquals(0, lockingListenerBart.unlocked(testFInfo));
        assertEquals(1, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.forbidden(testFInfo));
        assertEquals(0, lockingListenerLisa.unlocked(testFInfo));
        assertEquals(1, lockingListenerLisa.locked(testFInfo));
        assertEquals(0, lockingListenerLisa.forbidden(testFInfo));

        // PFC-2705: Now remove the lock file
        Files.delete(officeLockFile);
        TestHelper.scanFolder(getFolderAtLisa());
        waitForUnlocked(lockingBart, lockingListenerBart, testFInfo);

        assertEquals(0, lockingListenerLisa.forbidden(testFInfo));
        assertEquals(1, lockingListenerLisa.unlocked(testFInfo));
        assertEquals(1, lockingListenerLisa.locked(testFInfo));
        assertEquals(0, lockingListenerBart.forbidden(testFInfo));
        assertEquals(1, lockingListenerBart.unlocked(testFInfo));
        assertEquals(1, lockingListenerBart.locked(testFInfo));

        assertFalse(testFInfo.isLocked(getContollerLisa()));
        assertFalse(testFInfo.isLocked(getContollerBart()));
    }

    @Test
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

        waitForFileAtLisa(testFInfo);

        assertTrue(Files.exists(getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName())));

        lockingBart.lock(testFInfo);
        waitForLocked(lockingLisa, lockingListenerLisa, testFInfo);

        assertEquals(0, lockingListenerBart.unlocked(testFInfo));
        assertEquals(1, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.forbidden(testFInfo));
        assertEquals(0, lockingListenerLisa.unlocked(testFInfo));
        assertEquals(1, lockingListenerLisa.locked(testFInfo));
        assertEquals(0, lockingListenerLisa.forbidden(testFInfo));

        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase(),
            Constants.MS_OFFICE_FILENAME_PREFIX
                + testFile.getFileName().toString());

        TestHelper.scanFolder(getFolderAtLisa());

        assertEquals(0, lockingListenerBart.unlocked(testFInfo));
        assertEquals(1, lockingListenerBart.locked(testFInfo));
        assertEquals(0, lockingListenerBart.forbidden(testFInfo));
        assertEquals(0, lockingListenerLisa.unlocked(testFInfo));
        assertEquals(1, lockingListenerLisa.locked(testFInfo));
        assertEquals(1, lockingListenerLisa.forbidden(testFInfo));
    }

    @Test
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

        waitForFileAtLisa(testFInfo);

        assertTrue(Files.exists(getFolderAtLisa().getLocalBase().resolve(
            testFile.getFileName())));

        lockingBart.lock(testFInfo);

        waitForLocked(lockingLisa, lockingListenerLisa, testFInfo);

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

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return lockingListenerLisa.forbidden(testFInfo) >= 1;
            }

            @Override
            public String message() {
                return "Forbidden event did not arrive at lisa for " + testFInfo
                    + ". Events: " + lockingListenerLisa.forbidden;
            }
        });

        assertEquals(0, lockingListenerBart.unlocked.size());
        assertEquals(1, lockingListenerBart.locked.size());
        assertEquals(0, lockingListenerBart.forbidden.size());
        assertEquals(0, lockingListenerLisa.unlocked.size());
        assertEquals(1, lockingListenerLisa.locked.size());
        assertEquals(lockingListenerLisa.forbidden.size(),
            lockingListenerLisa.forbidden(testFInfo),
            "Lisa must only forbid the locked file " + testFInfo
                + ", but forbade: " + lockingListenerLisa.forbidden);
    }

    @Test
    public void testLockUnlockMultiple() {
        for (int i = 0; i < 25; i++) {
            testLockUnlockLocal();
            testLockUnlockRemote();
        }
    }
    
    /**
     * PFS-1922/FYK-543-88331
     */
    @Test
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
    
    @Test
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
        assertTrue(Files.exists(topLockFile),
            "Lock file must be created in the top folder meta-folder: "
            + topLockFile);

        // Unlock through the subfolder view removes the top-scoped lock
        assertTrue(lockingBart.unlock(fileInSub));
        assertFalse(lockingBart.isLocked(fileInSub));
        assertFalse(lockingBart.isLocked(fileInTop));
        assertNull(lockingBart.getLock(fileInSub));
        assertFalse(Files.exists(topLockFile));
        assertEquals(1, lockingListenerBart.unlocked.size());
    }

    @Test
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

        FileInfo fileInSub = subFolder.getFileInfo(testFile);
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
        assertTrue(Files.exists(expected),
            "Expected subdir-scoped lock file: " + expected);

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

    private static class LoggingLockingListener implements LockingListener {
        List<LockingEvent> locked = new CopyOnWriteArrayList<>();
        List<LockingEvent> unlocked = new CopyOnWriteArrayList<>();
        List<LockingEvent> forbidden = new CopyOnWriteArrayList<>();

        int locked(FileInfo fInfo) {
            return count(locked, fInfo);
        }

        int unlocked(FileInfo fInfo) {
            return count(unlocked, fInfo);
        }

        int forbidden(FileInfo fInfo) {
            return count(forbidden, fInfo);
        }

        private static int count(List<LockingEvent> events, FileInfo fInfo) {
            int n = 0;
            for (LockingEvent event : events) {
                if (fInfo.equals(event.getFileInfo())) {
                    n++;
                }
            }
            return n;
        }

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
