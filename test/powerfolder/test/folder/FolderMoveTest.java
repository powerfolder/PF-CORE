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
import java.nio.file.*;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * This test checks that a testFolder can be moved for one location to another. It
 * moves a file from testFolder to testFolder2. Tests functionality found in the
 * HomeTab class.
 */
public class FolderMoveTest extends ControllerTestCase {

    private Folder testFolder;

    private Path testFolder2;
    private Path testFolder3;

    /**
     * Creates test.txt and sub/test2.txt file2 to move.
     *
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {

        super.setUp();

        // Test for encrypted Folder or "normal" Folder. Default value: false for normal folder.
        prepareFolderMove(false);

    }

    private void prepareFolderMove(boolean isEncryptedFolder) throws Exception {

        // Setup a test testFolder; delete previous tests.
        getController().setPaused(true);

        if (isEncryptedFolder) {
            ConfigurationEntry.ENCRYPTED_STORAGE.setValue(super.getController(), true);
            setupEncryptedTestFolder(SyncProfile.HOST_FILES);
        } else {
            setupTestFolder(SyncProfile.HOST_FILES);
        }

        testFolder = getFolder();
        Path localBase = testFolder.getLocalBase();

        // Create a test.txt file
        Path testFile = localBase.resolve("test.txt");
        Files.deleteIfExists(testFile);
        Files.createFile(testFile);

        // Create a text2.txt file in the 'sub' testFolder.
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

        scanFolder(testFolder);

        if (isEncryptedFolder){
            testFolder2 = Paths.get(testFolder.getLocalBase().toAbsolutePath().toString().replace(".crypto", "2.crypto"));
            testFolder3 = Paths.get(localBase.toAbsolutePath().toString().replace(".crypto", "3.crypto"));
        } else {
            testFolder2 = Paths.get(testFolder.getLocalBase().toAbsolutePath().toString() + "2");
            testFolder3 = Paths.get(localBase.toAbsolutePath().toString() + "3");
        }

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

        FolderRepository repository = getController().getFolderRepository();

        try {

            System.out.println("Before moving:");

            Files.walk(testFolder.getLocalBase())
                    .forEach(p -> System.out.println(p));

            Path oldLocalBase = testFolder.getLocalBase();

            testFolder = repository.moveLocalFolder(testFolder, testFolder2);

            scanFolder(testFolder);

            System.out.println("After moving:");

            Files.walk(testFolder.getLocalBase())
                    .forEach(p -> System.out.println(p));

            // The testFolder should have the test files plus 2 subdirs
            assertEquals(4, testFolder.getKnownItemCount());

            // Sub dir should contain one file; test2.txt
            Files.walk(testFolder2)
                    .filter(p -> p.getFileName().toString().equals("sub") && Files.isDirectory(p))
                    .forEach(p -> assertEquals(1, PathUtils.getNumberOfSiblings(p)));

            // PFS-2227: moveLocalFolder should actually move all contents on filesystem:
            assertTrue("Old location still existing!:  " + oldLocalBase, Files.notExists(oldLocalBase));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Tests that a valid move is passed by canMoveFiles. Test move goes okay.
     * Tests old dir emptied.
     */
    public void testFolderMoveToEmptyDir() {

        FolderRepository repository = getController().getFolderRepository();

        testFolder2 = PathUtils.createEmptyDirectory(testFolder2);

        try {

            System.out.println("Before moving:");

            Files.walk(testFolder.getLocalBase())
                    .forEach(p -> System.out.println(p));

            Path oldLocalBase = testFolder.getLocalBase();

            testFolder = repository.moveLocalFolder(testFolder, testFolder2);

            scanFolder(testFolder);

            System.out.println("After moving:");

            Files.walk(testFolder.getLocalBase())
                    .forEach(p -> System.out.println(p));

            // The testFolder should have the test files plus 2 subdirs
            assertEquals(4, testFolder.getKnownItemCount());

            // Sub dir should contain one file; test2.txt
            Files.walk(testFolder2)
                    .filter(p -> p.getFileName().toString().equals("sub") && Files.isDirectory(p))
                    .forEach(p -> assertEquals(1, PathUtils.getNumberOfSiblings(p)));

            // PFS-2227: moveLocalFolder should actually move all contents on filesystem:
            assertTrue("Old location still existing!:  " + oldLocalBase, Files.notExists(oldLocalBase));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
