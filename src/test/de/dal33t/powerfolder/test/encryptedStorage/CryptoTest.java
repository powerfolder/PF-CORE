package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cryptomator cryptolib and cryptofs JUnit tests.
 *
 * @author Jan Wiegmann <wiegmann@powerfolder.com>
 */

public class CryptoTest {

    private FileSystem fileSystem1;
    private FileSystem fileSystem2;

    private Path encryptedDestination1;
    private Path encryptedDestination2;

    @Before
    public void setUp() throws Exception {

        encryptedDestination1 = TestHelper.getTestDir().resolve("encryptedDestination");
        Files.createDirectories(encryptedDestination1);

        encryptedDestination2 = TestHelper.getTestDir().resolve("encryptedDestination2");
        Files.createDirectories(encryptedDestination2);

        String passphrase = IdGenerator.makeId();

        /**
         * BREAKPOINT bitte vor dem fileSystem1 setzen und durchsteppen.
         */

        fileSystem1 = initFileSystem(encryptedDestination1, passphrase);
        fileSystem2 = initFileSystem(encryptedDestination2, passphrase);

    }

    private static FileSystem initFileSystem(Path encDir, String password) throws IOException {

        return CryptoFileSystemProvider.newFileSystem(
                encDir,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase(password)
                        .build());
    }

    @Test
    public void encryptSingleFile() throws IOException {
        // Mandatory
    }
}


