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

    private FileSystem fileSystem;
    private Path encryptedDestination;

    public void testStartFolderRepository() {

        //getController().getFolderRepository().start();

        // Setup a test folder.
        //setupTestFolder(SyncProfile.HOST_FILES);
    }

    public void testEncryptedPathOperations() throws IOException {


        /**
         * TO-DO Montag:
         *  1. copy, move, delete Test in cryptoTest einbauen.
         *  2. MultiThreading Test in cryptoTest einbauen.
         */

        encryptedDestination = Paths.get("/home/jw/test/test1");
        fileSystem = initFileSystem("78f639876f298793AFAG!!%%12%...ö22öppP");
        encryptedDestination = fileSystem.getPath(encryptedDestination.toString());

        Path encFileFrom = encryptedDestination.resolve("foobar.txt");

        Path encFileTo = encryptedDestination.resolve("foobar2.txt");

        Files.copy(encFileFrom, encFileTo);

        Files.walk(encryptedDestination)
                .forEach(p -> System.out.println(p));

    }

    private FileSystem initFileSystem(String password) throws IOException {
        return CryptoFileSystemProvider.newFileSystem(
                encryptedDestination,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase(password)
                        .build());
    }





}