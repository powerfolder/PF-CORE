/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * TRAC #378
 * <p>
 * Sync 1
 * <p>
 * Delete
 * <p>
 * Structure with many subdirs
 * <p>
 * Many/random changes
 * <p>
 * 
 * @author sprajc
 */
public class DirectorySyncTest extends FiveControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(tryToConnectSimpsons());
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSyncDeepStrucutre() {
        int maxDepth = 4;
        int dirsPerDir = 7;
        int createdDirs = createSubDirs(getFolderAtBart().getLocalBase(),
            dirsPerDir, 1, maxDepth);

        assertTrue(createdDirs > 100);
        scanFolder(getFolderAtBart());
        assertDirs(getFolderAtBart(), createdDirs);
        assertDirs(getFolderAtHomer(), createdDirs);
        assertDirs(getFolderAtMarge(), createdDirs);
        assertDirs(getFolderAtLisa(), createdDirs);
        assertDirs(getFolderAtMaggie(), createdDirs);
    }

    private void assertDirs(final Folder folder, final int expectedDirs) {
        TestHelper.waitForCondition(160, new ConditionWithMessage() {
            public String message() {
                return folder + " known: " + folder.getKnownFilesCount()
                    + " expected: " + expectedDirs;
            }

            public boolean reached() {
                return folder.getKnownFilesCount() == expectedDirs;
            }
        });
        int subdirs = countSubDirs(folder.getLocalBase());
        // Minus system subdir.
        assertEquals(subdirs - 1, expectedDirs);
        assertEquals(expectedDirs, folder.getKnownFilesCount());
        assertEquals(expectedDirs, folder.getKnownDirectories().size());
        assertEquals(0, folder.getKnownFiles().size());
    }

    private static int countSubDirs(File baseDir) {

        File[] subdirs = baseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        int i = subdirs.length;
        for (int j = 0; j < subdirs.length; j++) {
            File file = subdirs[j];
            i += countSubDirs(file);
        }
        return i;
    }

    private static int createSubDirs(File baseDir, int nsubdirs, int depth,
        int maxdepth)
    {
        int created = 0;
        for (int i = 0; i < nsubdirs; i++) {
            File dir = FileUtils.createEmptyDirectory(baseDir, IdGenerator
                .makeId());
            created++;
            if (depth < maxdepth) {
                created += createSubDirs(dir, nsubdirs, depth + 1, maxdepth);
            }
        }
        return created;
    }

    public void testSyncSingleDir() {
        File dirBart = new File(getFolderAtBart().getLocalBase(), "testDir");
        assertTrue(dirBart.mkdir());
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownFilesCount());
        DirectoryInfo infoBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirBart, infoBart, getContollerBart());

        // Check remote syncs
        final File dirLisa = new File(getFolderAtLisa().getLocalBase(), dirBart
            .getName());
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public String message() {
                return "Dir at lisa not existing: " + dirLisa;
            }

            public boolean reached() {
                return dirLisa.exists() && dirLisa.isDirectory()
                    && getFolderAtLisa().getKnownFilesCount() == 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        DirectoryInfo infoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());

        LoggingManager.setConsoleLogging(Level.FINE);
        // Now delete at Lisa
        assertTrue(dirLisa.delete());
        scanFolder(getFolderAtLisa());
        assertEquals(getFolderAtLisa().getKnownFiles().toString(), 1,
            getFolderAtLisa().getKnownFilesCount());
        infoLisa = getFolderAtLisa().getKnownDirectories().iterator().next();
        assertTrue("Dir should have been detected as deleted: "
            + infoLisa.toDetailString(), infoLisa.isDeleted());
        assertEquals(infoLisa.toDetailString(), 1, infoLisa.getVersion());
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());

        // Restore at Homer
        final File dirHomer = new File(getFolderAtHomer().getLocalBase(),
            "testDir");
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public String message() {
                return "Dir at homer existing: " + dirLisa;
            }

            public boolean reached() {
                return !dirHomer.exists()
                    && getFolderAtHomer().getKnownFilesCount() == 1
                    && getFolderAtHomer().getKnownDirectories().iterator()
                        .next().isDeleted();
            }
        });
        assertFalse(dirHomer.exists());
        DirectoryInfo infoHomer = getFolderAtHomer().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirHomer, infoHomer, getContollerHomer());
        assertEquals(infoHomer.toDetailString(), 1, infoHomer.getVersion());
    }
}
