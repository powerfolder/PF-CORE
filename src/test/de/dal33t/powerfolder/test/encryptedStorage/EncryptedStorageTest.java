package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.EncryptedFileSystemUtils;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JUnit Test for file encryption with cryptomator lib, cryptofs and PowerFolder.
 *
 * @author Jan Wiegmann <wiegmann@powerfolder.com>
 * @since <pre>Aug 23, 2016</pre>
 */

public class EncryptedStorageTest extends ControllerTestCase {

    private Folder folder;
    private Path localBase;

    @Override
    public void setUp() throws Exception {

        super.setUp();

        // Activate storage encryption.
        ConfigurationEntry.ENCRYPTED_STORAGE.setValue(super.getController(), true);

    }

    public void testSetupEncryptedFolder() {

        // Setup a encrypted test folder.
        //getController().setPaused(true);
        setupEncryptedTestFolder(SyncProfile.HOST_FILES);
        folder = getFolder();
        localBase = folder.getLocalBase();

        assertTrue(EncryptedFileSystemUtils.isCryptoPathInstance(localBase));

    }

    public void testMoveEncryptedFolder() throws IOException {

        // Setup a encrypted test folder.
        testSetupEncryptedFolder();

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

        scanFolder(folder);

    }

}