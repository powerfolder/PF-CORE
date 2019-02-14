/*
 * Copyright 2004 - 2016 Christian Sprajc. All rights reserved.
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
 *
 * $Id: Controller.java 21251 2013-03-19 01:46:23Z sprajc $
 */

package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.CryptoFileSystemUri;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class for working with the encrypted FileSystem from Cryptomator.
 * @author <a href="mailto:wiegmann@powerfolder.com>Jan Wiegmann</a>
 */

public class EncryptedFileSystemUtils {

    static final String DEFAULT_MASTERKEY_FILENAME = "masterkey.cryptomator";
    static final String DEFAULT_MASTERKEY_BACKUP_FILENAME = "masterkey.cryptomator.bkup";
    static final String DEFAULT_ENCRYPTED_ROOT_DIR = "d";

    /**
     * Checks if an existing CryptoFileSystem exists for the given path, if not create a new one.
     *
     * @param controller The controller
     * @param vaultPath must (!) be an UnixPath!
     * @return root directory of an CryptoFileSystem as CryptoPath.
     * @throws IOException If deleting the old directories or creating the new directories failed
     */

    public static Path getEncryptedFileSystem(@NotNull Controller controller, @NotNull Path vaultPath) throws IOException {
        if (isCryptoInstance(vaultPath)) {
            Path encDir = vaultPath.getFileSystem().getPath(Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR);
            if (Files.notExists(encDir)) {
                Files.createDirectories(encDir);
            }

            return encDir;
        } else {
            try {
                return getCryptoPath(vaultPath);
            } catch (FileSystemNotFoundException e) {

                verifyEncryptedVault(vaultPath);

                FileSystem cryptoFS = initCryptoFileSystem(controller, vaultPath);
                Path encDir = cryptoFS.getPath(Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR);

                if (Files.notExists(encDir)) {
                    Files.createDirectories(encDir);
                }

                return encDir;
            }
        }
    }

    /**
     * Storage locations for new encrypted folders are completely empty. After a server restart we reconstruct
     * every encrypted container - if the storage location is NOT empty and NO cryptomator masterkey
     * files OR no encrypted files at all are available -> delete everything, recreate the the storage location
     * and initialize a new CryptoFileSystem.
     *
     * This method decides whether a new CryptoFileSystem should be created because certain files are missing for a
     * reconstruction of an existing CryptoFileSystem.
     *
     * @param vaultPath The path where the encrypted container is located or will be (re-)created.
     *
     * @return {@code True} if a complete new CryptoFileSystem creation is necessary, {@code false} if a reconstruction is to be made.
     * @throws IOException If deleting the old directories or creating the new directories failed
     */

    static boolean verifyEncryptedVault(Path vaultPath) throws IOException {

        if (isCryptoInstance(vaultPath)) {
            return false;
        }

        if (!PathUtils.isEmptyDir(vaultPath)
            &&
            (Files.notExists(vaultPath.resolve(DEFAULT_MASTERKEY_FILENAME))
                && Files.notExists(vaultPath.resolve(DEFAULT_MASTERKEY_BACKUP_FILENAME))
                || Files.notExists(vaultPath.resolve(DEFAULT_ENCRYPTED_ROOT_DIR))
            ))
        {
            PathUtils.recursiveDeleteVisitor(vaultPath);
            Files.createDirectories(vaultPath);
            return true;
        }

        return false;
    }

    /**
     * Checks if storage encryption is activated on this server.
     *
     * @param controller The controller
     * @return {@code True} if storage encryption is activated.
     */

    public static boolean isEncryptionActivated(@NotNull Controller controller){
        return ConfigurationEntry.ENCRYPTED_STORAGE.getValueBoolean(controller);
    }

    /**
     * Checks if the given UnixPath is a path to an encrypted folder.
     *
     * @param path must (!) be an UnixPath!
     * @return {@code True} if the path ends with the keyword ".crypto".
     */

    public static boolean endsWithEncryptionSuffix(@NotNull Path path) {
        return path.getFileName().toString().endsWith(Constants.FOLDER_ENCRYPTION_SUFFIX);
    }

    /**
     * Checks if the given path belongs to an CryptoFileSystem.
     *
     * @param path must (!) be an CryptoPath!
     * @return {@code True} if the given path has a CryptoFileSystem.
     */

    public static boolean isCryptoInstance(@NotNull Path path){
        return path.getFileSystem() instanceof CryptoFileSystem;
    }

    /**
     * Get the physical storage location of an CryptoFileSystem over the given CryptoPath.
     *
     * @param path must (!) be an CryptoPath!
     *
     * @return path leading to the physical data from the CryptoFileSystem.
     *
     * @throws IllegalArgumentException If the {@code path} is not a {@link org.cryptomator.cryptofs.CryptoPath} and therefore does not contain a {@link CryptoFileSystem}
     */

    @NotNull
    public static Path getPhysicalStorageLocation(@NotNull Path path) {
        if (!(path.getFileSystem() instanceof CryptoFileSystem)) {
            throw new IllegalArgumentException("FileSystem from " + path  + " is not a CryptoFileSystem");
        }

        CryptoFileSystem fs = (CryptoFileSystem) path.getFileSystem();

        if (fs != null) {
            return fs.getPathToVault();
        } else {
            throw new IllegalArgumentException("FileSystem from " + path  + " is not a CryptoFileSystem");
        }
    }

    /**
     * Returns the CryptoPath to the internal base directory of the crypto container specified by {@code vaultPath}.
     *
     * @param vaultPath Location of the crypto container
     * @return The base directory within the specified crypto container.
     */

    @NotNull
    public static Path getCryptoPath(@NotNull Path vaultPath) {
        URI encFolderUri = CryptoFileSystemUri.create(vaultPath, Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR);
        return FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);
    }

    /**
     * Sets the passphrase to encrypt the masterkeys from encrypted folders on this server.
     * Stores the configuration file.
     *
     * @param controller The controller
     */

    public static void setEncryptionPassphrase(@NotNull Controller controller){
        if (!ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.hasValue(controller)) {
            ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.setValue(controller,
                    IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId());
            controller.saveConfig();
        }
    }

    /**
     * Checks if {@code path} is a CryptoPath and is the base directory
     * within the crypto container and is empty.
     *
     * @param path the path to check
     *
     * @return {@code True} if the path is a crypto path, the root directory
     * within the crypto container and does NOT contain files.
     */

    public static boolean isEmptyCryptoContainerRootDir(@NotNull Path path) {
        return isCryptoInstance(path)
            && path.startsWith(Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR)
            && PathUtils.isEmptyDir(path);
    }

    /**
     * Checks if the Java Cryptography Extension (JCE) is installed on this host.
     * JCE is mandatory to support AES 256-bit encryption.
     *
     * @return {@code True} if a maximum allowed key length of AES is greater than 128 bit.
     * @throws NoSuchAlgorithmException If the 'AES' Algorithm was not found
     */

    public static boolean checkJCEinstalled() throws NoSuchAlgorithmException {
        return Cipher.getMaxAllowedKeyLength("AES") >= 128;
    }

    // Internal helper ********************************************************

    private static FileSystem initCryptoFileSystem(Controller controller, Path encDir) throws IOException {

        return CryptoFileSystemProvider.newFileSystem(
               encDir,
               CryptoFileSystemProperties.cryptoFileSystemProperties()
                       .withPassphrase(ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.getValue(controller))
                       .build());
    }
}