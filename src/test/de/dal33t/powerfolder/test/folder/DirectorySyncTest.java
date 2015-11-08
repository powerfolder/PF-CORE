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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
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

    public void testInitialSync() throws IOException {
        disconnectAll();
        LoggingManager.setConsoleLogging(Level.WARNING);
        Path dirBart = getFolderAtBart().getLocalBase().resolve("testDir");
        try {
            Files.createDirectory(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        TestHelper.waitMilliSeconds(3000);
        final Path dirLisa = getFolderAtLisa().getLocalBase().resolve(
            dirBart.getFileName());
        try {
            Files.createDirectory(dirLisa);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        connectAll();
        assertEquals("Barts know item count: "
            + getFolderAtBart().getKnownItemCount(), 1, getFolderAtBart()
            .getKnownItemCount());
        DirectoryInfo infoBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirBart, infoBart, getContollerBart());
        assertEquals(0, infoBart.getVersion());
        assertEquals(getFolderAtBart().getIncomingFiles().toString(), 0,
            getFolderAtBart().getIncomingFiles().size());

        assertEquals("Lisas knonwn item count: "
            + getFolderAtLisa().getKnownItemCount(), 1, getFolderAtLisa()
            .getKnownItemCount());
        DirectoryInfo infoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());
        assertEquals(0, infoLisa.getVersion());
        assertEquals(0, getFolderAtLisa().getIncomingFiles().size());
    }

    public void testDisconnectedHighVersion() throws IOException {
        Path dirBart = getFolderAtBart().getLocalBase().resolve("testDir");
        try {
            Files.createDirectory(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        final Path dirLisa = getFolderAtLisa().getLocalBase().resolve(
            dirBart.getFileName());
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return Files.exists(dirLisa);
            }
        });
        DirectoryInfo dirInfoBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirBart, dirInfoBart, getContollerBart());
        assertEquals(0, dirInfoBart.getVersion());
        try {
            Files.delete(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        scanFolder(getFolderAtBart());
        dirInfoBart = getFolderAtBart().getKnownDirectories().iterator().next();
        assertDirMatch(dirBart, dirInfoBart, getContollerBart());
        assertEquals(1, dirInfoBart.getVersion());
        assertTrue(dirInfoBart.isDeleted());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return Files.notExists(dirLisa) && getFolderAtLisa()
                    .getKnownDirectories().iterator().next().isDeleted();
            }
        });
        DirectoryInfo dirInfoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirLisa, dirInfoLisa, getContollerLisa());
        assertEquals(1, dirInfoLisa.getVersion());
        assertTrue(dirInfoLisa.isDeleted());
        // Now disconnect

        disconnectAll();

        try {
            Files.createDirectory(dirBart);
            scanFolder(getFolderAtBart());
            Files.delete(dirBart);
            scanFolder(getFolderAtBart());
            Files.createDirectories(dirBart);
            scanFolder(getFolderAtBart());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }

        // Version 4 at bart. Version 1 at lisa
        dirInfoBart = getFolderAtBart().getKnownDirectories().iterator().next();
        assertDirMatch(dirBart, dirInfoBart, getContollerBart());
        assertEquals(4, dirInfoBart.getVersion());
        assertFalse(dirInfoBart.isDeleted());

        connectAll();
        // Now lisa should synced up to version 4
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return Files.exists(dirLisa);
            }
        });
        dirInfoLisa = getFolderAtLisa().getKnownDirectories().iterator().next();
        assertDirMatch(dirLisa, dirInfoLisa, getContollerLisa());
        assertEquals(4, dirInfoLisa.getVersion());
        assertFalse(dirInfoLisa.isDeleted());
    }

    public void testSyncMixedStrucutre() throws IOException {
        int maxDepth = 2;
        int dirsPerDir = 10;
        final List<Path> createdFiles = new ArrayList<Path>();
        int createdDirs = createSubDirs(getFolderAtBart().getLocalBase(),
            dirsPerDir, 1, maxDepth, new DirVisitor() {
                public void created(Path dir) {
                    createdFiles.add(TestHelper.createRandomFile(dir));
                }
            });

        assertTrue("Created dirs: " + createdDirs, createdDirs > 10);
        assertTrue(createdFiles.size() > 10);
        scanFolder(getFolderAtBart());
        assertFilesAndDirs(getFolderAtBart(), createdDirs, createdFiles.size());
        assertFilesAndDirs(getFolderAtHomer(), createdDirs, createdFiles.size());
        assertFilesAndDirs(getFolderAtMarge(), createdDirs, createdFiles.size());
        assertFilesAndDirs(getFolderAtLisa(), createdDirs, createdFiles.size());
        assertFilesAndDirs(getFolderAtMaggie(), createdDirs,
            createdFiles.size());

        // Now delete
        try (DirectoryStream<Path> stream = Files
            .newDirectoryStream(getFolderAtHomer().getLocalBase())) {
            for (Path path : stream) {
                if (Files.isDirectory(path)
                    && !getFolderAtHomer().isSystemSubDir(path))
                {
                    PathUtils.recursiveDelete(path);
                }
            }
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        // Leave 1 file to omitt disconnect detection under Linux
        scanFolder(getFolderAtHomer());
        assertFilesAndDirs(getFolderAtHomer(), createdDirs,
            createdFiles.size(), 0);
        Collection<DirectoryInfo> dirs = getFolderAtHomer()
            .getKnownDirectories();
        for (DirectoryInfo directoryInfo : dirs) {
            assertTrue(
                "Dir not detected as deleted: "
                    + directoryInfo.toDetailString(), directoryInfo.isDeleted());
            assertEquals(1, directoryInfo.getVersion());
        }

        // Wait for delete
        waitForEmptyFolder(getFolderAtBart());
        assertFilesAndDirs(getFolderAtBart(), createdDirs, createdFiles.size(),
            0);
        dirs = getFolderAtBart().getKnownDirectories();
        for (DirectoryInfo directoryInfo : dirs) {
            assertTrue(
                "Dir not detected as deleted: "
                    + directoryInfo.toDetailString(), directoryInfo.isDeleted());
            Path diskFile = directoryInfo.getDiskFile(getContollerBart()
                .getFolderRepository());
            assertFalse(diskFile + " info " + directoryInfo.toDetailString(),
                Files.exists(diskFile));
            assertEquals(1, directoryInfo.getVersion());
        }

        // Check others.
        waitForEmptyFolder(getFolderAtMarge());
        assertFilesAndDirs(getFolderAtMarge(), createdDirs,
            createdFiles.size(), 0);
        waitForEmptyFolder(getFolderAtLisa());
        assertFilesAndDirs(getFolderAtLisa(), createdDirs, createdFiles.size(),
            0);
        waitForEmptyFolder(getFolderAtMaggie());
        assertFilesAndDirs(getFolderAtMaggie(), createdDirs,
            createdFiles.size(), 0);
    }

    private void waitForEmptyFolder(final Folder folder) {
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    folder.getLocalBase(), new ExcludeSystemSubdirFilter())) {
                    for (Path p : stream) {
                        sb.append(p.toAbsolutePath().toString());
                    }
                } catch (IOException ioe) {
                    // Ignore.
                }
                sb.append("]");

                return "Folder not empty. Files in " + folder + ": "
                    + sb.toString();
            }

            public boolean reached() {
                return PathUtils.isEmptyDir(folder.getLocalBase(),
                    new ExcludeSystemSubdirFilter());
            }
        });
    }

    public void testSyncDeepStrucutre() {
        int maxDepth = 9;
        int dirsPerDir = 2;
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
        int createdDirs = createSubDirs(getFolderAtBart().getLocalBase(),
            dirsPerDir, 1, maxDepth, null);

        assertTrue(createdDirs > 100);
        scanFolder(getFolderAtBart());
        assertFilesAndDirs(getFolderAtBart(), createdDirs, 0);
        assertFilesAndDirs(getFolderAtHomer(), createdDirs, 0);
        assertFilesAndDirs(getFolderAtMarge(), createdDirs, 0);
        assertFilesAndDirs(getFolderAtLisa(), createdDirs, 0);
        assertFilesAndDirs(getFolderAtMaggie(), createdDirs, 0);
    }

    private void assertFilesAndDirs(final Folder folder,
        final int expectedDirs, final int expectedFiles)
    {
        assertFilesAndDirs(folder, expectedDirs, expectedFiles, expectedDirs);
    }

    private void assertFilesAndDirs(final Folder folder,
        final int expectedDirs, final int expectedFiles,
        int expectedActualSubdirs)
    {
        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            public String message() {
                return folder + " known: " + folder.getKnownItemCount()
                    + " expected dirs: " + expectedDirs + " expected file: "
                    + expectedFiles + " total: "
                    + (expectedDirs + expectedFiles);
            }

            public boolean reached() {
                return folder.getKnownItemCount() == expectedDirs
                    + expectedFiles;
            }
        });
        int subdirs = countSubDirs(folder.getLocalBase());
        assertEquals(expectedActualSubdirs, subdirs);
        assertEquals(expectedDirs + expectedFiles, folder.getKnownItemCount());
        assertEquals(expectedDirs, folder.getKnownDirectories().size());
        assertEquals(expectedFiles, folder.getKnownFiles().size());
    }

    private static int countSubDirs(Path baseDir) {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir,
            new Filter<Path>() {
                @Override
                public boolean accept(Path entry) throws IOException {
                    boolean accept = Files.isDirectory(entry)
                        && !entry.getFileName().startsWith(
                            Constants.POWERFOLDER_SYSTEM_SUBDIR);
                    return accept;
                }
            })) {
            int i = 0;
            for (Path p : stream) {
                i++;

                i += countSubDirs(p);
            }

            return i;
        } catch (IOException ioe) {
            return 0;
        }
    }

    private static int createSubDirs(Path baseDir, int nsubdirs, int depth,
        int maxdepth, DirVisitor visitor)
    {
        int created = 0;
        for (int i = 0; i < nsubdirs; i++) {
            Path dir = PathUtils.createEmptyDirectory(
                baseDir,
                PathUtils.removeInvalidFilenameChars(depth + "-"
                    + IdGenerator.makeId().substring(1, 5)));

            if (visitor != null) {
                visitor.created(dir);
            }
            created++;
            if (depth < maxdepth) {
                created += createSubDirs(dir, nsubdirs, depth + 1, maxdepth,
                    visitor);
            }
        }
        return created;
    }

    private final class ExcludeSystemSubdirFilter implements Filter<Path> {
        @Override
        public boolean accept(Path path) throws IOException {
            return !path.toString().contains(
                Constants.POWERFOLDER_SYSTEM_SUBDIR);
        }
    }

    private interface DirVisitor {
        void created(Path dir);
    }

    /**
     * TRAC #1854
     */
    public void testUnableToDeleteDirectory() throws IOException {
        Path dirBart = getFolderAtBart().getLocalBase().resolve("testDir");
        try {
            Files.createDirectory(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());
        DirectoryInfo infoBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirBart, infoBart, getContollerBart());
        // Check remote syncs
        final Path dirLisa = getFolderAtLisa().getLocalBase().resolve(
            dirBart.getFileName());
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public String message() {
                return "Dir at lisa not existing: " + dirLisa;
            }

            public boolean reached() {
                return Files.exists(dirLisa) && Files.isDirectory(dirLisa)
                    && getFolderAtLisa().getKnownItemCount() == 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownItemCount());
        DirectoryInfo infoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());

        // Create a random file at lisa
        TestHelper.createRandomFile(dirLisa);
        try {
            Files.delete(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }

        // Now sync to lisa. SHOULD STILL EXISTS!
        scanFolder(getFolderAtBart());
        FileInfo dirInfoAtBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertTrue(dirInfoAtBart.toDetailString(), dirInfoAtBart.isDeleted());
        assertEquals(dirInfoAtBart.toDetailString(), 1,
            dirInfoAtBart.getVersion());
        assertDirMatch(dirBart, dirInfoAtBart, getContollerBart());

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public String message() {
                return "Dir at lisa not existing: " + dirLisa;
            }

            public boolean reached() {
                return Files.exists(dirLisa) && Files.isDirectory(dirLisa);
            }
        });

        TestHelper.waitMilliSeconds(1000);

        FileInfo dirInfoAtLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        // SHOULD STILL be version 0 = could not sync version 1 (=deleted) from
        // bart.
        assertEquals(dirInfoAtLisa.toDetailString(), 0,
            dirInfoAtLisa.getVersion());
        assertFalse(dirInfoAtLisa.toDetailString(), dirInfoAtLisa.isDeleted());
        assertDirMatch(dirLisa, dirInfoAtLisa, getContollerLisa());
    }

    public void testSyncSingleDir() throws IOException {
        Path dirBart = getFolderAtBart().getLocalBase().resolve("testDir");
        try {
            Files.createDirectory(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());
        DirectoryInfo infoBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirBart, infoBart, getContollerBart());

        // Check remote syncs
        final Path dirLisa = getFolderAtLisa().getLocalBase().resolve(
            dirBart.getFileName());
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Dir at lisa not existing: " + dirLisa;
            }

            public boolean reached() {
                return Files.exists(dirLisa) && Files.isDirectory(dirLisa)
                    && getFolderAtLisa().getKnownItemCount() == 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownItemCount());
        DirectoryInfo infoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());
        assertEquals(0, getFolderAtLisa().getIncomingFiles().size());

        // Now delete at Lisa
        try {
            Files.delete(dirLisa);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        scanFolder(getFolderAtLisa());
        assertEquals(getFolderAtLisa().getKnownFiles().toString(), 1,
            getFolderAtLisa().getKnownItemCount());
        infoLisa = getFolderAtLisa().getKnownDirectories().iterator().next();
        assertTrue(
            "Dir should have been detected as deleted: "
                + infoLisa.toDetailString(), infoLisa.isDeleted());
        assertEquals(infoLisa.toDetailString(), 1, infoLisa.getVersion());
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());

        // Restore at Homer
        final Path dirHomer = getFolderAtHomer().getLocalBase().resolve(
            "testDir");
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public String message() {
                return "Dir at homer existing: " + dirLisa;
            }

            public boolean reached() {
                return Files.notExists(dirHomer)
                    && getFolderAtHomer().getKnownItemCount() == 1
                    && getFolderAtHomer().getKnownDirectories().iterator()
                        .next().isDeleted();
            }
        });
        assertFalse(Files.exists(dirHomer));
        DirectoryInfo infoHomer = getFolderAtHomer().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirHomer, infoHomer, getContollerHomer());
        assertEquals(infoHomer.toDetailString(), 1, infoHomer.getVersion());
    }
}
