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
import de.dal33t.powerfolder.util.Reject;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.CryptoFileSystemUris;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class for working with the encrypted FileSystem from Cryptomator.
 */

public class EncryptedFileSystemUtils {

    public static Path getEncryptedFileSystem(Controller controller, Path incDir) throws IOException {
        Reject.ifNull(controller, "Controller");
        Reject.ifNull(incDir, "Path");
        if (incDir.getFileSystem().provider() instanceof CryptoFileSystemProvider){
            return incDir;
        } else {
            try {
                URI encFolderUri = CryptoFileSystemUris.createUri(incDir, "/encDir");
                incDir = FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);
                return incDir;
            } catch (FileSystemNotFoundException e) {
                FileSystem cryptoFS = initCryptoFileSystem(controller, incDir);
                Path encDir = cryptoFS.getPath("/encDir");
                if (!Files.exists(encDir)) {
                    Files.createDirectories(encDir);
                }
                return encDir;
            }
        }
    }

    public static boolean isEncryptionActivated(Controller controller){
        Reject.ifNull(controller, "controller");
        return ConfigurationEntry.ENCRYPTED_STORAGE.getValueBoolean(controller);
    }

    public static boolean isVaultPath(String path){
        return path.contains(Constants.FOLDER_ENCRYPTION_SUFFIX);
    }

    public static boolean isCryptoInstance(Path path){
        return path.getFileSystem().provider() instanceof CryptoFileSystemProvider;
    }

    public static Path getPhysicalStorageLocation(Path path) {
        CryptoFileSystem fs = (CryptoFileSystem) path.getFileSystem();
        if (fs instanceof CryptoFileSystem) {
            return fs.getPathToVault();
        } else {
            throw new IllegalArgumentException("FileSystem from " + path  + " is not a CryptoFileSystem");
        }
    }

    /**
     * This method returns a CryptoPath to a given String, if a CryptoPath instance exists for this String.
     * IMPORTANT: The given String MUST be an absolute path to the vault of an CryptoFileSystem!
     * @param pathToVault
     * @return CryptoPath for the given String, if an active CryptoPath instance for this String is available.
     */

    public static Path getCryptoPath(String pathToVault) {
        Reject.ifNull(pathToVault, "Path");
        Path cryptoPath = Paths.get(pathToVault);
        try {
            URI encFolderUri = CryptoFileSystemUris.createUri(cryptoPath, "/encDir");
            cryptoPath = FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);
        } catch (FileSystemNotFoundException e){
            // This could happen.
        }
        return cryptoPath;
    }

    public static void setEncryptionPassphrase(Controller controller){
        Reject.ifNull(controller, "Controller");
        if (!ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.hasValue(controller)) {
            ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.setValue(controller,
                    IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId());
            controller.saveConfig();
        }
    }

    public static boolean checkJCEinstalled() throws NoSuchPaddingException, NoSuchAlgorithmException {
        int keyLength = Cipher.getInstance("AES/CBC/PKCS5Padding").getMaxAllowedKeyLength("AES");
        if (keyLength == 128) {
            return false;
        } else {
            return true;
        }
    }

    // Internal helper ********************************************************

    private static FileSystem initCryptoFileSystem(Controller controller, Path encDir) throws IOException {

         FileSystem fs = CryptoFileSystemProvider.newFileSystem(
                encDir,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase(ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.getValue(controller))
                        .build());

        return fs;
    }

}