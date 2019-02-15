package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.*;

/**
* EncryptedFileSystemUtils Test.
*
* @author <a href="mailto:krickl@powerfolder.com>Maximilian Krickl</a>
* @version 1.0
*/

public class EncryptedFileSystemUtilsTest {

    private Path vaultPath;
    private Mockery mockery = new Mockery();
    private Controller controller;
    private Properties config;

    @Before
    public void before() throws IOException {
        mockery.setImposteriser(ClassImposteriser.INSTANCE);
        controller = mockery.mock(Controller.class);
        config = new Properties();

        mockery.checking(new Expectations() {{
            allowing(controller).getConfig(); will(returnValue(config));
            allowing(controller).saveConfig();
        }});

        // Cleanup
        TestHelper.cleanTestDir();
        PathUtils.recursiveDelete(Controller.getMiscFilesLocation().resolve(
                "build"));

        vaultPath = TestHelper.getTestDir().resolve("Folder.crypto");
        if (Files.notExists(vaultPath)) {
            Files.createDirectories(vaultPath);
        }

        EncryptedFileSystemUtils.setEncryptionPassphrase(controller);

        assertTrue(PathUtils.isEmptyDir(vaultPath));
    }

    @After
    public void after() throws IOException {
        try {
            EncryptedFileSystemUtils.getCryptoPath(vaultPath).getFileSystem().close();
        } catch (FileSystemNotFoundException fsnfe) {
            System.out.println("No FileSystem to close. " + fsnfe + ". This might be right.");
        }
    }

    @Test
    public void endsWithEncryptionSuffix() {
        // absolute paths
        assertTrue(EncryptedFileSystemUtils.endsWithEncryptionSuffix(Paths.get("/folder/base/dir/folderName.crypto")));
        assertFalse(EncryptedFileSystemUtils.endsWithEncryptionSuffix(Paths.get("/folder/base/dir/folderName.crypto/should/only/end/with/extension")));
        // relative paths
        assertTrue(EncryptedFileSystemUtils.endsWithEncryptionSuffix(Paths.get("base/dir/folderName.crypto")));
        assertFalse(EncryptedFileSystemUtils.endsWithEncryptionSuffix(Paths.get("base/dir/folderName.crypto/should/only/end/with/extension")));
    }

    @Test
    public void isEmptyCryptoContainerRootDir() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller,
            vaultPath);

        assertFalse(EncryptedFileSystemUtils.isEmptyCryptoContainerRootDir(vaultPath));
        assertTrue(EncryptedFileSystemUtils.isEmptyCryptoContainerRootDir(cryptoPath));
    }

    @Test
    public void getEncryptedFileSystem() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller,
            vaultPath);

        assertTrue(Files.exists(cryptoPath));
        assertNotEquals(vaultPath, cryptoPath);
        assertTrue(EncryptedFileSystemUtils.isEmptyCryptoContainerRootDir(cryptoPath));
    }

    @Test
    public void getCryptoPath() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller,
            vaultPath);

        Path otherCryptoPath = EncryptedFileSystemUtils.getCryptoPath(vaultPath);

        assertEquals(cryptoPath.getFileName().toString(), "encDir");
        assertEquals(otherCryptoPath.getFileName().toString(), "encDir");
        assertEquals(cryptoPath, otherCryptoPath);
    }

    @Test
    public void isCryptoInstance() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller, vaultPath);

        assertTrue(EncryptedFileSystemUtils.isCryptoInstance(cryptoPath));
        assertFalse(EncryptedFileSystemUtils.isCryptoInstance(vaultPath));
    }

    @Test
    public void getPhysicalStorageLocation() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller, vaultPath);
        Path vault = EncryptedFileSystemUtils.getPhysicalStorageLocation(cryptoPath);

        assertEquals(vaultPath, vault);

        try {
            EncryptedFileSystemUtils.getPhysicalStorageLocation(vaultPath);
            fail();
        } catch (IllegalArgumentException iae) {
            // NOP -- expected
        }
    }

    @Test
    public void isInitializuationRequiredWithVaultPath() throws IOException {
        assertTrue(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));
    }

    @Test
    public void isInitializuationRequiredMissingRootDir() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils
            .getEncryptedFileSystem(controller, vaultPath);

        // preconditions
        assertFalse(EncryptedFileSystemUtils.isInitializationRequired(cryptoPath));
        assertFalse(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));

        // delete root dir
        PathUtils.recursiveDelete(vaultPath.resolve(EncryptedFileSystemUtils.DEFAULT_ENCRYPTED_ROOT_DIR));

        // test
        assertTrue(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));
    }

    @Test
    public void isInitializuationRequiredMissingKeyFiles() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller, vaultPath);

        // preconditions
        assertFalse(EncryptedFileSystemUtils.isInitializationRequired(cryptoPath));
        assertFalse(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));

        // delete key files
        Files.delete(vaultPath.resolve(EncryptedFileSystemUtils.DEFAULT_MASTERKEY_FILENAME));
        Files.delete(vaultPath.resolve(EncryptedFileSystemUtils.DEFAULT_MASTERKEY_BACKUP_FILENAME));

        // test
        assertTrue(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));
    }

    @Test
    public void isInitializuationRequiredMissingEverything() throws IOException {
        Path cryptoPath = EncryptedFileSystemUtils.getEncryptedFileSystem(controller, vaultPath);

        // preconditions
        assertFalse(EncryptedFileSystemUtils.isInitializationRequired(cryptoPath));
        assertFalse(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));

        // delete root dir
        PathUtils.recursiveDelete(vaultPath.resolve(EncryptedFileSystemUtils.DEFAULT_ENCRYPTED_ROOT_DIR));

        // delete key files
        Files.delete(vaultPath.resolve(EncryptedFileSystemUtils.DEFAULT_MASTERKEY_FILENAME));
        Files.delete(vaultPath.resolve(EncryptedFileSystemUtils.DEFAULT_MASTERKEY_BACKUP_FILENAME));

        // test
        assertTrue(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));
    }

    @Test
    public void isEncryptionActivated() {
        // not testing: If the controller does not correctly return config settings... :shrug:
    }

    @Test
    public void setEncryptedPassphrase() {
        // not testing: If the controller does not correctly return config settings... :shrug:
    }
} 
