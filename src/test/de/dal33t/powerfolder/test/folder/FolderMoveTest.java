package de.dal33t.powerfolder.test.folder;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This test checks that a folder can be moved for one location to another.
 * It moves a file from testFolder to testFolder2.
 * Tests functionality found in the HomeTab class.
 */
public class FolderMoveTest extends ControllerTestCase {

    /**
     * Creates test.txt and sub/test2.txt file2 to move.
     * @throws Exception
     */
    public void setUp() throws Exception {

        super.setUp();

        // Setup a test folder; delete previous tests.
        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD, true);
        File localBase = getFolder().getLocalBase();

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



        // Wrtie a test files.
        FileWriter writer = new FileWriter(testFile);
        writer.write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        writer = new FileWriter(testFile2);
        writer.write("This is the test2 text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh osdjft");
        writer.close();
        scanFolder(getFolder());

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

    /**
     * Tests move to non-empty dir is stopped.
     * Tests move to non-existant dir okay.
     * Tests move to subDir stopped.
     */
    public void testCanMove() {

        File localBase = getFolder().getLocalBase();
        File testFolder3 = new File(localBase.getAbsolutePath() + '3');
        File testFolder4 = new File(localBase.getAbsolutePath() + '4');

        // Move to non-empty dir (fails - 1)
        int checkOne = FileUtils.canMoveFiles(localBase, testFolder3);
        assertEquals(1, checkOne);

        // Move to non-existant dir (okay)
        int checkTwo = FileUtils.canMoveFiles(localBase, testFolder4);
        assertEquals(0, checkTwo);

        // Move to subDir (fails - 2)
        int checkThree = FileUtils.canMoveFiles(localBase, new File(localBase, "emptySub"));
        assertEquals(2, checkThree);
    }

    /**
     * Tests that a valid move is passed by canMoveFiles.
     * Test move goes okay.
     * Tests old dir emptied.
     */
    public void testFolderMove() {

        // Create new directories
        // .../ControllerBart/testFolder2
        File testFolder2 = new File(getFolder().getLocalBase().getAbsolutePath() + '2');
        FolderRepository repository = getController().getFolderRepository();

        // Remove original folder from the folder repository.
        repository.removeFolder(getFolder());

        try {
            // Create new folder
            FolderSettings folderSettings =
                    new FolderSettings(testFolder2,
                            getFolder().getSyncProfile(),
                            false, getFolder().isUseRecycleBin());

            // Simulate tests done in HomeTab to check the folder can be moved.
            File oldLocalBase = getFolder().getLocalBase();

            int preTest = FileUtils.canMoveFiles(oldLocalBase, testFolder2);
            assertEquals(0, preTest);

            // Move contents
            repository.createFolder(getFolder().getInfo(), folderSettings);

            // Move the folder.
            FileUtils.moveFiles(oldLocalBase, testFolder2);

            // The folder should have the test files.
            assertEquals(2, getFolder().getKnownFilesCount());

            // The new location should contain the
            // 1) .PowerFolder dir, 2) the test file, 3) sub dir and 4) emptySub dir.
            assertEquals(4, testFolder2.listFiles().length);

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
            assertTrue(!oldLocalBase.exists());

        } catch (FolderException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
