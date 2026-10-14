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
 *
 */
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(tryToConnectSimpsons());
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
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
        assertEquals( 1, getFolderAtBart()
            .getKnownItemCount(),"Barts know item count: "
            + getFolderAtBart().getKnownItemCount());
        DirectoryInfo infoBart = getFolderAtBart().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirBart, infoBart, getContollerBart());
        assertEquals(0, infoBart.getVersion());
        assertEquals( 0,
            getFolderAtBart().getIncomingFiles().size(),getFolderAtBart().getIncomingFiles().toString());

        assertEquals( 1, getFolderAtLisa()
            .getKnownItemCount(),"Lisas knonwn item count: "
            + getFolderAtLisa().getKnownItemCount());
        DirectoryInfo infoLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());
        assertEquals(0, infoLisa.getVersion());
        assertEquals(0, getFolderAtLisa().getIncomingFiles().size());
    }

    @Test
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

    @Test
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

        assertTrue( createdDirs > 10,"Created dirs: " + createdDirs);
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
            assertTrue( directoryInfo.isDeleted(),
                "Dir not detected as deleted: "
                    + directoryInfo.toDetailString());
            assertEquals(1, directoryInfo.getVersion());
        }

        // Wait for delete
        waitForEmptyFolder(getFolderAtBart());
        assertFilesAndDirs(getFolderAtBart(), createdDirs, createdFiles.size(),
            0);
        dirs = getFolderAtBart().getKnownDirectories();
        for (DirectoryInfo directoryInfo : dirs) {
            assertTrue( directoryInfo.isDeleted(),
                "Dir not detected as deleted: "
                    + directoryInfo.toDetailString());
            Path diskFile = directoryInfo.getDiskFile(getContollerBart()
                .getFolderRepository());
            assertFalse(
                Files.exists(diskFile),diskFile + " info " + directoryInfo.toDetailString());
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

    @Test
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

    @Test
    public void testScanDirectory() {
        Path dirBart = getFolderAtBart().getLocalBase().resolve("testDir");
        try {
            Files.createDirectory(dirBart);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        FileInfo dirInfo = FileInfoFactory.newFile(getFolderAtBart(), dirBart, null, null, null, null, true, null);
        getFolderAtBart().scanDirectory(dirInfo, dirBart);
        assertFilesAndDirs(getFolderAtBart(), 1, 0);
        assertFilesAndDirs(getFolderAtLisa(), 1, 0);
    }

    private void assertFilesAndDirs(final Folder folder,
        final int expectedDirs, final int expectedFiles)
    {
        assertFilesAndDirs(folder, expectedDirs, expectedFiles, expectedDirs);
    }

    private void assertFilesAndDirs(final Folder folder,
        final int expectedDirs, final int expectedFiles,
        final int expectedActualSubdirs)
    {
        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            public String message() {
                List<String> mismatching = dirsNotMatchingDisk(folder);
                return folder + " known dirs: "
                    + folder.getKnownDirectories().size() + " (expected "
                    + expectedDirs + "), known files: "
                    + folder.getKnownFiles().size() + " (expected "
                    + expectedFiles + "), subdirs on disk: "
                    + countSubDirs(folder.getLocalBase()) + " (expected "
                    + expectedActualSubdirs
                    + "), directories not matching disk: "
                    + mismatching.size()
                    + (mismatching.isEmpty() ? "" : ", e.g. "
                        + mismatching.get(0));
            }

            public boolean reached() {
                return folder.getKnownDirectories().size() == expectedDirs
                    && folder.getKnownFiles().size() == expectedFiles
                    && dirsNotMatchingDisk(folder).isEmpty()
                    && countSubDirs(folder.getLocalBase()) == expectedActualSubdirs;
            }
        });
        int subdirs = countSubDirs(folder.getLocalBase());
        assertEquals(expectedActualSubdirs, subdirs);
        assertEquals(expectedDirs + expectedFiles, folder.getKnownItemCount());
        assertEquals(expectedDirs, folder.getKnownDirectories().size());
        assertEquals(expectedFiles, folder.getKnownFiles().size());
    }

    private static List<String> dirsNotMatchingDisk(Folder folder) {
        List<String> mismatching = new ArrayList<String>();
        for (DirectoryInfo info : folder.getKnownDirectories()) {
            Path dir = info.getDiskFile(folder.getController()
                .getFolderRepository());
            if (info.isDeleted() == Files.isDirectory(dir)) {
                mismatching.add(info.toDetailString());
            }
        }
        return mismatching;
    }

    private void waitForSyncedDir(int seconds, final Folder folder,
        final Path dir, final int version, final boolean deleted)
    {
        TestHelper.waitForCondition(seconds, new ConditionWithMessage() {
            public boolean reached() {
                FileInfo info = folder.getFile(FileInfoFactory
                    .lookupInstance(folder, dir));
                return info != null && info.isDeleted() == deleted
                    && info.getVersion() == version
                    && (deleted
                        ? Files.notExists(dir)
                        : Files.isDirectory(dir));
            }

            public String message() {
                return "Expected " + dir + " at " + folder + " as version "
                    + version + (deleted ? ", deleted" : ", existing")
                    + ". On disk: " + Files.exists(dir) + ". In database: "
                    + folder.getFile(FileInfoFactory.lookupInstance(folder,
                        dir));
            }
        });
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
    @Test
    public void testUnableToDeleteDirectory() throws IOException {
        getFolderAtLisa().getFolderWatcher().setIngoreAll(true);
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
        waitForSyncedDir(5, getFolderAtLisa(), dirLisa, 0, false);
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
        assertTrue( dirInfoAtBart.isDeleted(),dirInfoAtBart.toDetailString());
        assertEquals( 1,
            dirInfoAtBart.getVersion(),dirInfoAtBart.toDetailString());
        assertDirMatch(dirBart, dirInfoAtBart, getContollerBart());

        final Member bartAtLisa = getContollerLisa().getNodeManager()
            .getNode(getContollerBart().getMySelf().getId());
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public String message() {
                return "Deletion of " + dirInfoAtBart.toDetailString()
                    + " has not reached lisa, who sees: "
                    + bartAtLisa.getFile(dirInfoAtBart);
            }

            public boolean reached() {
                FileInfo dirAtBartSeenByLisa = bartAtLisa.getFile(dirInfoAtBart);
                return dirAtBartSeenByLisa != null
                    && dirAtBartSeenByLisa.isDeleted()
                    && dirAtBartSeenByLisa.getVersion() == dirInfoAtBart.getVersion();
            }
        });
        getFolderAtLisa().syncRemoteDeletedFiles(Collections.singleton(bartAtLisa), false);
        assertTrue(Files.isDirectory(dirLisa), "Dir at lisa not existing: " + dirLisa);

        FileInfo dirInfoAtLisa = getFolderAtLisa().getKnownDirectories()
            .iterator().next();
        assertEquals( 0,
            dirInfoAtLisa.getVersion(),dirInfoAtLisa.toDetailString());
        assertFalse( dirInfoAtLisa.isDeleted(),dirInfoAtLisa.toDetailString());
        assertDirMatch(dirLisa, dirInfoAtLisa, getContollerLisa());
    }

    @Test
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
        waitForSyncedDir(20, getFolderAtLisa(), dirLisa, 0, false);
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
        assertEquals( 1,
            getFolderAtLisa().getKnownItemCount(),getFolderAtLisa().getKnownFiles().toString());
        infoLisa = getFolderAtLisa().getKnownDirectories().iterator().next();
        assertTrue( infoLisa.isDeleted(),
            "Dir should have been detected as deleted: "
                + infoLisa.toDetailString());
        assertEquals( 1, infoLisa.getVersion(),infoLisa.toDetailString());
        assertDirMatch(dirLisa, infoLisa, getContollerLisa());

        // Restore at Homer
        final Path dirHomer = getFolderAtHomer().getLocalBase().resolve(
            "testDir");
        waitForSyncedDir(20, getFolderAtHomer(), dirHomer, 1, true);
        assertFalse(Files.exists(dirHomer));
        DirectoryInfo infoHomer = getFolderAtHomer().getKnownDirectories()
            .iterator().next();
        assertDirMatch(dirHomer, infoHomer, getContollerHomer());
        assertEquals( 1, infoHomer.getVersion(),infoHomer.toDetailString());
    }
}
