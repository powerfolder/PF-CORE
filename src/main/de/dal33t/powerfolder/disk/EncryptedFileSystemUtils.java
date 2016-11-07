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
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.CryptoFileSystemUris;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;

/**
 * Helper class for working with the encrypted FileSystem from Cryptomator.
 */

public class EncryptedFileSystemUtils {

    public static Path initCryptoFS(Controller controller, Path encDir) throws IOException {
        try {
            URI encFolderUri = CryptoFileSystemUris.createUri(encDir);
            encDir = FileSystems.getFileSystem(encFolderUri).provider().getPath(encFolderUri);
            if (!Files.exists(encDir)){
                Files.createDirectories(encDir);
            }
            return encDir;
        } catch (FileSystemNotFoundException e){
            FileSystem cryptoFS = initCryptoFileSystem(controller, encDir);
            encDir = cryptoFS.getPath(encDir.toString());
            if (!Files.exists(encDir)){
                Files.createDirectories(encDir);
            }
            return encDir;
        }
    }

    public static boolean isEncryptionActivated(Controller controller){
        Reject.ifNull(controller, "controller");
        return ConfigurationEntry.ENCRYPTED_STORAGE.getValueBoolean(controller);
    }

    public static boolean isEncryptedPath(Path path){
        Reject.ifNull(path, "Path");
        return isEncryptedPath(path.toString());
    }

    public static boolean isEncryptedPath(String path){
        Reject.ifNull(path, "Path");
        return path.contains(Constants.FOLDER_ENCRYPTION_SUFFIX);
    }

    public static boolean isCryptoPathInstance(Path path){
        Reject.ifNull(path, "Path");
        return path.getFileSystem().provider() instanceof CryptoFileSystemProvider;
    }

    public static void setEncryptionPassphrase(Controller controller){
        Reject.ifNull(controller, "controller");
        if (!ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.hasValue(controller)) {
            ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.setValue(controller,
                    IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId());
            controller.saveConfig();
        }
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