package de.dal33t.powerfolder.disk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.event.LockingEvent;
import de.dal33t.powerfolder.event.LockingListener;
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
                return "File wasn't unlocked at bart: " + testFInfo;
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
        for (int i = 0; i < 100; i++) {
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
