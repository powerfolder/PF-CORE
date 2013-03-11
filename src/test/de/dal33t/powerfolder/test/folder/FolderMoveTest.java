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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
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
        setupTestFolder(SyncProfile.HOST_FILES, ArchiveMode.FULL_BACKUP);
        folder = getFolder();
        File localBase = folder.getLocalBase();

        // Create a test.txt file
        File testFile = new File(localBase, "test.txt");
        if (testFile.exists()) {
            testFile.delete();
        }
        assertTrue(testFile.createNewFile());

        // Create a text2.txt file in the 'sub' folder.
        File sub = new File(localBase, "sub");
        sub.mkdir();
        assertTrue(sub.exists());

        File testFile2 = new File(sub, "test2.txt");
        if (testFile2.exists()) {
            testFile2.delete();
        }
        assertTrue(testFile2.createNewFile());

        File emptySub = new File(localBase, "emptySub");
        emptySub.mkdir();
        assertTrue(emptySub.exists());

        // Write a test files.
        FileWriter writer = new FileWriter(testFile);
        writer
            .write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        writer = new FileWriter(testFile2);
        writer
            .write("This is the test2 text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh osdjft");
        writer.close();
        scanFolder(folder);

        File testFolder3 = new File(localBase.getAbsolutePath() + '3');
        testFolder3.mkdir();
        assertTrue(testFolder3.exists());

        File dummyFile = new File(testFolder3, "dummy.txt");
        if (dummyFile.exists()) {
            dummyFile.delete();
        }
        assertTrue(dummyFile.createNewFile());

        // Write a test files.
        writer = new FileWriter(dummyFile);
        writer.write("This is the dummy text.\n\n sdlkja hsdjfksd f90a-7s w t");
        writer.close();
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
        File testFolder2 = new File(
            folder.getLocalBase().getAbsolutePath() + '2');
        FolderRepository repository = getController().getFolderRepository();

        // Remove original folder from the folder repository.
        repository.removeFolder(folder, false);

        // Simulate tests done in HomeTab to check the folder can be moved.
        File oldLocalBase = folder.getLocalBase();
        try {
            // Move the contents.
            FileUtils.recursiveMove(oldLocalBase, testFolder2);

            // The new location should contain the
            // 1) .PowerFolder dir, 2) the test file, 3) sub dir and 4) emptySub
            // dir.
            assertEquals(4, testFolder2.listFiles().length);

            // Create new folder
            FolderSettings folderSettings = new FolderSettings(testFolder2,
                getFolder().getSyncProfile(), false, getFolder()
                    .getFileArchiver().getArchiveMode(), getFolder()
                            .getFileArchiver().getVersionsPerFile());

            // Move the folder
            folder = repository.createFolder(folder.getInfo(), folderSettings);

            scanFolder(folder);

            // The folder should have the test files plus 2 subdirs
            assertEquals(4, folder.getKnownItemCount());

            // Sub dir should contain one file; test2.txt
            boolean foundTest2 = false;
            for (File file : testFolder2.listFiles()) {
                if (file.getName().equals("sub") && file.isDirectory()) {
                    assertEquals(1, file.listFiles().length);
                    foundTest2 = true;
                }
            }
            assertTrue(foundTest2);

            // The old location should be gone.
            assertFalse("Old location still existing!:  " + oldLocalBase,
                oldLocalBase.exists());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
