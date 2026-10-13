/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.TestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
* EncryptedFileSystemUtils Test.
*
* @author <a href="mailto:krickl@powerfolder.com>Maximilian Krickl</a>
* @version 1.0
*/

public class EncryptedFileSystemUtilsTest {

    private Path vaultPath;
    private Controller controller;
    private Properties config;

    @BeforeEach
    public void before() throws IOException {
        controller = mock(Controller.class);
        config = new Properties();

        when(controller.getConfig()).thenReturn(config);

        // Cleanup
        TestHelper.cleanTestDir();
        PathUtils.recursiveDelete(Controller.getMiscFilesLocation().resolve(
                "build"));

        Path testDir = TestHelper.getTestDir();
        if (testDir == null) {
            fail();
        }
        vaultPath = testDir.resolve("Folder.crypto");
        if (Files.notExists(vaultPath)) {
            Files.createDirectories(vaultPath);
        }

        EncryptedFileSystemUtils.setEncryptionPassphrase(controller);

        assertTrue(PathUtils.isEmptyDir(vaultPath));
    }

    @AfterEach
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
    public void isInitializationRequiredWithVaultPath() throws IOException {
        assertTrue(EncryptedFileSystemUtils.isInitializationRequired(vaultPath));
    }

    @Test
    public void isInitializationRequiredMissingRootDir() throws IOException {
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
    public void isInitializationRequiredMissingKeyFiles() throws IOException {
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
    public void isInitializationRequiredMissingEverything() throws IOException {
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
