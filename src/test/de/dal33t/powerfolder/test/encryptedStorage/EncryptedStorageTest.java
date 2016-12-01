package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.EncryptedFileSystemUtils;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;

/**
 * JUnit Test for file encryption with cryptomator lib, cryptofs and PowerFolder.
 *
 * @author Jan Wiegmann <wiegmann@powerfolder.com>
 * @since <pre>Aug 23, 2016</pre>
 */

public class EncryptedStorageTest extends ControllerTestCase {

    private Folder testFolder1;
    private Path testFolder2;

    @Override
    public void setUp() throws Exception {

        super.setUp();

        // Setup encrypted folder.
        setupEncryptedFolder();
    }

    public void setupEncryptedFolder() {

        // Setup a encrypted test testFolder1.
        getController().setPaused(true);
        EncryptedFileSystemUtils.setEncryptionPassphrase(super.getController());
        ConfigurationEntry.ENCRYPTED_STORAGE.setValue(super.getController(), true);
        setupEncryptedTestFolder(SyncProfile.HOST_FILES);
        testFolder1 = getFolder();

        Path localBase = testFolder1.getLocalBase();

        assertTrue(EncryptedFileSystemUtils.isCryptoInstance(localBase));

        // Create a test.txt file
        try {

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
            BufferedWriter writer = Files.newBufferedWriter(testFile, Charset.forName("UTF-8"));

            writer.write("This is the test text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf" +
                    "\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh klh8iesyt");

            writer.flush();

            writer = Files.newBufferedWriter(testFile2, Charset.forName("UTF-8"));
            writer.write("This is the test2 text.\n\nl;fjk sdl;fkjs dfljkdsf ljds flsfjd lsjdf lsfjdoi;ureffd dshf" +
                    "\nhjfkluhgfidgh kdfghdsi8yt ribnv.,jbnfd kljhfdlkghes98o jkkfdgh osdjft");

        } catch (IOException e) {
            e.printStackTrace();
        }

        testFolder2 = Paths.get(testFolder1.getLocalBase().toAbsolutePath().toString().replace(".crypto", "2.crypto"));

    }

    public void testMoveEncryptedFolder() throws IOException {

        FolderRepository repository = getController().getFolderRepository();

        try {

            System.out.println("Before moving:");

            Files.walk(testFolder1.getLocalBase())
                    .forEach(p -> System.out.println(p));

            Path oldLocalBase = testFolder1.getLocalBase();

            testFolder1 = repository.moveLocalFolder(testFolder1, testFolder2);

            scanFolder(testFolder1);

            System.out.println("After moving:");

            Files.walk(testFolder1.getLocalBase())
                    .forEach(p -> System.out.println(p));

            // The testFolder should have the test files plus 2 subdirs
            assertEquals(4, testFolder1.getKnownItemCount());

            // Sub dir should contain one file; test2.txt
            Files.walk(testFolder2)
                    .filter(p -> p.getFileName().toString().equals("sub") && Files.isDirectory(p))
                    .forEach(p -> assertEquals(1, PathUtils.getNumberOfSiblings(p)));

            // Since moveFolder method is NOT removing the old directory, this has to be true:
            assertTrue("Old location still existing!:  " + oldLocalBase, Files.exists(oldLocalBase));

        } catch (IOException e) {
            //fail(e);
            e.printStackTrace();
        }

    }

    public void testDeleteEncryptedFolder() throws IOException {

        boolean isDirectoryEmpty = false;

        FolderRepository repo = getController().getFolderRepository();

        repo.removeFolder(testFolder1, true);

        try {
            Files.list(testFolder1.getLocalBase());
        } catch (NoSuchFileException nsf){
            isDirectoryEmpty = true;
        }

        assertTrue(isDirectoryEmpty);

    }

    public void testInitCryptoFileSystem() throws IOException {

        Path locaBase = null;
        Path tempLocalBase = testFolder1.getLocalBase();

        try {
            locaBase = EncryptedFileSystemUtils.getEncryptedFileSystem(getController(), testFolder1.getLocalBase());
            fail("Must not succeed!");
        } catch (FileAlreadyExistsException e) {
            // Expected.
        }

        FolderRepository repository = getController().getFolderRepository();

        repository.removeFolder(testFolder1, false, false);

        locaBase = EncryptedFileSystemUtils.getEncryptedFileSystem(getController(), tempLocalBase);

        assertEquals(tempLocalBase.toString(), locaBase.toString());

        Path testPath = Paths.get("/home/jw/PowerFolders/admin/encr1.crypto");
        PathUtils.recursiveDelete(testPath);
        Files.createDirectories(testPath);

        locaBase = EncryptedFileSystemUtils.getEncryptedFileSystem(getController(), testPath);
        assertEquals(testPath.toString(), locaBase.toString());

        locaBase = EncryptedFileSystemUtils.getEncryptedFileSystem(getController(), testPath);
        assertEquals(testPath.toString(), locaBase.toString());


    }

}