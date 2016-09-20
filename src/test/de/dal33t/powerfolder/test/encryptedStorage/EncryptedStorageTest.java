package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.disk.SyncProfile;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JUnit Test for file encryption with cryptomator lib and cryptofs.
 *
 * @author Jan Wiegmann <wiegmann@de.dal33t.powerfolder.test.encryptedStorage.com>
 * @since <pre>Aug 23, 2016</pre>
 */

public class EncryptedStorageTest extends ControllerTestCase {

    public void testStartFolderRepository() {

        getController().getFolderRepository().start();

        // Setup a test folder.
        setupTestFolder(SyncProfile.HOST_FILES);
    }

}