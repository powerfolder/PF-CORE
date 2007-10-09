package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class MirrorFolderTest extends FiveControllerTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(tryToConnectSimpsons());
        joinTestFolder(SyncProfile.SYNCHRONIZE_PCS);
    }

    public void testRandomSyncOperations() {
        performRandomOperations(100, 70, 0, getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public String message() {
                return "Downloads not completed: Homer "
                    + getContollerHomer().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Marge "
                    + getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Lisa "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size()
                    + ", Maggie "
                    + getContollerMaggie().getTransferManager()
                        .getCompletedDownloadsCollection().size();
            }

            public boolean reached() {
                return getContollerHomer().getTransferManager()
                    .getCompletedDownloadsCollection().size() == 100
                    && getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size() == 100
                    && getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection().size() == 100
                    && getContollerMarge().getTransferManager()
                        .getCompletedDownloadsCollection().size() == 100;
            }
        });
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

//        // Step 3) Change operations
//        performRandomOperations(0, 50, 0, getFolderAtBart().getLocalBase());
//        scanFolder(getFolderAtBart());

        //TestHelper.waitMilliSeconds(60000);
//        TestHelper.waitForCondition(60, new ConditionWithMessage() {
//            public String message() {
//                return "Downloads not completed: Homer "
//                    + getContollerHomer().getTransferManager()
//                        .getCompletedDownloadsCollection().size()
//                    + ", Marge "
//                    + getContollerMarge().getTransferManager()
//                        .getCompletedDownloadsCollection().size()
//                    + ", Lisa "
//                    + getContollerLisa().getTransferManager()
//                        .getCompletedDownloadsCollection().size()
//                    + ", Maggie "
//                    + getContollerMaggie().getTransferManager()
//                        .getCompletedDownloadsCollection().size();
//            }
//
//            public boolean reached() {
//                return getContollerHomer().getTransferManager()
//                    .getCompletedDownloadsCollection().size() == 150
//                    && getContollerMarge().getTransferManager()
//                        .getCompletedDownloadsCollection().size() == 150
//                    && getContollerLisa().getTransferManager()
//                        .getCompletedDownloadsCollection().size() == 150
//                    && getContollerMarge().getTransferManager()
//                        .getCompletedDownloadsCollection().size() == 100;
//            }
//        });

     //   assertIdenticalTestFolder();
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
        int size1 = 0;
        for (File file : dir1.listFiles()) {
            size1 += file.length();
        }
        int size2 = 0;
        for (File file : dir2.listFiles()) {
            size2 += file.length();
        }
        assertEquals(size1, size2);
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
            File[] files = dir.listFiles();
            if (files.length == 0) {
                return;
            }
            File file = files[index % files.length];
            if (file.isFile()) {
                TestHelper.changeFile(file);
                return;
            }
        }
    }

    private static class RemoveFileOperation implements Runnable {
        private File dir;

        private RemoveFileOperation(File dir) {
            super();
            this.dir = dir;
        }

        public void run() {
            File[] files = dir.listFiles();
            if (files.length == 0) {
                return;
            }
            while (true) {
                File file = files[(int) (Math.random() * files.length)];
                if (file.isFile()) {
                    file.delete();
                    return;
                }
            }
        }
    }
}
