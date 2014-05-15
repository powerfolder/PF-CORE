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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * This test checks that a folder can be moved for one location to another. It
 * moves a file from testFolder to testFolder2. Tests functionality found in the
 * HomeTab class.
 */
public class FolderMoveTest extends ControllerTestCase {
    private Folder folder;

    /**
     * Creates test.txt and sub/test2.txt file2 to move.
     *
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {

        super.setUp();

        // Setup a test folder; delete previous tests.
        getController().setPaused(true);
        setupTestFolder(SyncProfile.HOST_FILES);
        folder = getFolder();
        Path localBase = folder.getLocalBase();

        // Create a test.txt file
        Path testFile = localBase.resolve("test.txt");
        Files.deleteIfExists(testFile);
        Files.createFile(testFile);

        // Create a text2.txt file in the 'sub' folder.
        Path sub = localBase.resolve("sub");
        Files.createDirectory(sub);
        assertTrue(Files.exists(sub));

        Path testFile2 = sub.resolve("test2.txt");
        Files.deleteIfExists(testFile2);
        Files.createFile(testFile2);

        Path emptySub = localBase.resolve("emptySub");
        Files.createDirectory(emptySub);
        assertTrue(Files.exists(emptySub));

        // Write a test files.
        try (BufferedWriter writer = Files.newBufferedWriter(testFile, Charset.forName("UTF-8"))) {
            writer
                .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(testFile2, Charset.forName("UTF-8"))) {
            writer
                .write("This is the test2 text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh osdjft");
        }

        scanFolder(folder);

        Path testFolder3 = Paths.get(localBase.toAbsolutePath().toString() + "3");
        Files.createDirectory(testFolder3);
        assertTrue(Files.exists(testFolder3));

        Path dummyFile = testFolder3.resolve("dummy.txt");
        Files.deleteIfExists(dummyFile);
        Files.createFile(dummyFile);

        // Write a test files.
        try (BufferedWriter writer = Files.newBufferedWriter(dummyFile, Charset.forName("UTF-8"))) {
            writer.write("This is the dummy text.\n\n sdlkja hsdjfksd f90a-7s w t");
        }
    }

    public void testFolderMoveMultiple() throws Exception {
        for (int i = 0; i < 20; i++) {
            testFolderMove();
            tearDown();
            setUp();
        }
    }

    /**
     * Tests that a valid move is passed by canMoveFiles. Test move goes okay.
     * Tests old dir emptied.
     */
    public void testFolderMove() {

        // Create new directories
        // .../ControllerBart/testFolder2
        Path testFolder2 = Paths.get(
            folder.getLocalBase().toAbsolutePath().toString() + "2");
        FolderRepository repository = getController().getFolderRepository();

        // Remove original folder from the folder repository.
        repository.removeFolder(folder, false);

        // Simulate tests done in HomeTab to check the folder can be moved.
        Path oldLocalBase = folder.getLocalBase();
        try {
            // Move the contents.
            PathUtils.recursiveMove(oldLocalBase, testFolder2);

            // The new location should contain the
            // 1) .PowerFolder dir, 2) the test file, 3) sub dir and 4) emptySub
            // dir.
            assertEquals(4, PathUtils.getNumberOfSiblings(testFolder2));

            // Create new folder
            FolderSettings folderSettings = new FolderSettings(testFolder2,
                getFolder().getSyncProfile(), getFolder().getFileArchiver()
                    .getVersionsPerFile());

            // Move the folder
            folder = repository.createFolder(folder.getInfo(), folderSettings);

            scanFolder(folder);

            // The folder should have the test files plus 2 subdirs
            assertEquals(4, folder.getKnownItemCount());

            // Sub dir should contain one file; test2.txt
            boolean foundTest2 = false;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(testFolder2)) {
                for (Path file : stream) {
                    if (file.getFileName().toString().equals("sub") && Files.isDirectory(file)) {
                        assertEquals(1, PathUtils.getNumberOfSiblings(file));
                        foundTest2 = true;
                    }
                }
            }
            assertTrue(foundTest2);

            // The old location should be gone.
            assertFalse("Old location still existing!:  " + oldLocalBase,
                Files.exists(oldLocalBase));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
