package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.CryptoFileSystemUri;
import org.junit.Test;
import org.junit.Before;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;

import static de.dal33t.powerfolder.disk.EncryptedFileSystemUtils.getCryptoPath;
import static de.dal33t.powerfolder.disk.EncryptedFileSystemUtils.isCryptoInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 
* EncryptedFileSystemUtils Test.
* 
* @author <a href="mailto:wiegmann@powerfolder.com>Jan Wiegmann</a>
* @since <pre>Dec 1, 2016</pre> 
* @version 1.0 
*/

public class EncryptedFileSystemUtilsTest {

    protected static Path UNENCRYPTED_TESTDIR;
    protected static Path ENCRYPTED_TESTFOLDER;

@Before
public void before() throws Exception {

    // Cleanup
    TestHelper.cleanTestDir();
    PathUtils.recursiveDelete(Controller.getMiscFilesLocation().resolve(
            "build"));

    // Get dir to encrypt.
    UNENCRYPTED_TESTDIR= TestHelper.getTestDir().resolve("dirToEncrypt" + Constants.FOLDER_ENCRYPTION_SUFFIX);
    Files.createDirectories(UNENCRYPTED_TESTDIR);

    // Create encrypted container.
    ENCRYPTED_TESTFOLDER = getEncryptedFileSystem(UNENCRYPTED_TESTDIR);
}

/**
 * Method: endsWithEncryptionSuffix(String path)
*/

@Test
public void testIsPhysicalStorageLocation() throws Exception {
    assertTrue(UNENCRYPTED_TESTDIR.toString().contains(Constants.FOLDER_ENCRYPTION_SUFFIX));
} 

/**
* Method: isCryptoInstance(Path path)
*/

@Test
public void testIsCryptoInstance() throws Exception {
    assertTrue(ENCRYPTED_TESTFOLDER.getFileSystem().provider() instanceof CryptoFileSystemProvider);
} 

/**
* Method: getPhysicalStorageLocation(Path path)
*/

@Test
public void testGetPhysicalStorageLocation() throws Exception {
    CryptoFileSystem fs = (CryptoFileSystem) ENCRYPTED_TESTFOLDER.getFileSystem();
    assertTrue(UNENCRYPTED_TESTDIR.equals(fs.getPathToVault()));
} 

/**
* Method: getCryptoPath(String pathToVault)
*/

@Test
public void testGetCryptoPathString() throws Exception {
    String UNENCRYPTED_TESTDIR_STRING = UNENCRYPTED_TESTDIR.toString();
    Path cryptoPath = getCryptoPath(UNENCRYPTED_TESTDIR_STRING);
    assertTrue(cryptoPath.equals(ENCRYPTED_TESTFOLDER));
} 

/**
* Method: getCryptoPath(Path path)
*/

@Test
public void testGetCryptoPath() throws Exception {
    URI encFolderUri = CryptoFileSystemUri.create(UNENCRYPTED_TESTDIR, Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR);
    Path ENCRYPTED_TESTFOLDER_2 = FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);
    assertTrue(ENCRYPTED_TESTFOLDER.equals(ENCRYPTED_TESTFOLDER_2));
}

    /**
     * Method: getCryptoPath(Path path)
     */

    @Test
    public void testIsCryptoContainerRootDir() throws Exception {
        URI encFolderUri = CryptoFileSystemUri.create(UNENCRYPTED_TESTDIR, Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR);
        Path ENCRYPTED_TESTFOLDER_2 = FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);
        assertTrue(EncryptedFileSystemUtils.isCryptoContainerEmptyRootDir(ENCRYPTED_TESTFOLDER_2));
    }

/**
* Method: checkJCEinstalled()
*/

@Test
public void testCheckJCEinstalled() throws Exception {
    int keyLength = Cipher.getInstance("AES/CBC/PKCS5Padding").getMaxAllowedKeyLength("AES");
    assertFalse(keyLength == 128);
    assertTrue(keyLength > 128);
}

    // Internal helper ********************************************************

    private static FileSystem initCryptoFileSystem(Path encDir) throws IOException {
        FileSystem fs = CryptoFileSystemProvider.newFileSystem(
                encDir,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase("foobar")
                        .build());
        return fs;
    }

    public static Path getEncryptedFileSystem(Path incDir) throws IOException {
        Reject.ifNull(incDir, "Path");
        if (isCryptoInstance(incDir)){
            return incDir;
        } else {
            try {
                return getCryptoPath(incDir);
            } catch (FileSystemNotFoundException e) {
                FileSystem cryptoFS = initCryptoFileSystem(incDir);
                Path encDir = cryptoFS.getPath(Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR);
                if (!Files.exists(encDir)) {
                    Files.createDirectories(encDir);
                }
                return encDir;
            }
        }
    }

} 
