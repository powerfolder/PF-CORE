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
* $Id$
*/
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.apache.commons.io.filefilter.FileFilterUtils;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class MirrorFolderTest extends FiveControllerTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(tryToConnectSimpsons());
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    public void testRandomSyncOperationsMultiple() throws Exception {
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
}
