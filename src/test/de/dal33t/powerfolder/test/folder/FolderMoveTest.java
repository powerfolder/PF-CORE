package de.dal33t.powerfolder.test.folder;

import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.util.FileUtils;

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
        File localbase = getFolder().getLocalBase();

        // Create a test.txt file
        File testFile = new File(localbase, "test.txt");
        if (testFile.exists()) {
            testFile.delete();
        }
        assertTrue(testFile.createNewFile());

        // Create a text2.txt file in the 'sub' folder.
        File sub = new File(localbase, "sub");
        sub.mkdir();
        assertTrue(sub.exists());

        File testFile2 = new File(sub, "test2.txt");
        if (testFile2.exists()) {
            testFile2.delete();
        }
        assertTrue(testFile2.createNewFile());

        // Wrtie a test files.
        FileWriter writer = new FileWriter(testFile);
        writer.write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");
        writer.close();
        writer = new FileWriter(testFile2);
        writer.write("This is the test2 text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh osdjft");
        writer.close();
        scanFolder(getFolder());
    }

    /**
     * Tests the folder move.
     * The code of the text should be as per HomeTab.MyFolderMoveWorker.construct().
     */
    public void testFoldermove() {

        // Create new directory
        // .../ControllerBart/testFolder2
        File newLocalBase = new File(getFolder().getLocalBase().getAbsolutePath() + '2');
        FolderRepository repository = getController().getFolderRepository();

        // Remove original folder from the folder repository.
        repository.removeFolder(getFolder());

        try {
            // Create new folder
            FolderSettings folderSettings =
                    new FolderSettings(newLocalBase,
                            getFolder().getSyncProfile(),
                            false, getFolder().isUseRecycleBin());
            repository.createFolder(getFolder().getInfo(), folderSettings);

            // Move contents
            File oldLocalBase = getFolder().getLocalBase();
            FileUtils.moveFiles(oldLocalBase, newLocalBase);

            // The folder should have the test files.
            assertEquals(2, getFolder().getKnownFilesCount());

            // The new location should contain the
            // 1) .PowerFolder dir, 2) the test file and 3) sub dir.
            assertEquals(3, newLocalBase.listFiles().length);

            // Sub dir should contain one file; test2.txt
            boolean foundTest2 = false;
            for (File file : newLocalBase.listFiles()) {
                if (file.getName().equals("sub") && file.isDirectory()) {
                    assertEquals(1, file.listFiles().length);
                    foundTest2 = true;
                }
            }
            assertTrue(foundTest2);

            // The old location should only contain the .PowerFolder dir.
            assertEquals(1, oldLocalBase.listFiles().length);

        } catch (FolderException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
