package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.IdGenerator;
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

    private static FileSystem initCryptoFileSystem(Controller controller, Path encDir) throws IOException {

        return CryptoFileSystemProvider.newFileSystem(
                encDir,
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withPassphrase(ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.getValue(controller))
                        .build());
    }

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

        return ConfigurationEntry.ENCRYPTED_STORAGE.getValueBoolean(controller);
    }

    public static boolean isEncryptedPath (Path path){

        return path.toString().endsWith(Constants.FOLDER_ENCRYPTION_SUFFIX);
    }

    public static boolean isCryptoPathInstance(Path path){

        return path.getFileSystem().provider() instanceof CryptoFileSystemProvider;
    }

    public static void setEncryptionPassphrase(Controller controller){

        if (!ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.hasValue(controller)) {
            ConfigurationEntry.ENCRYPTED_STORAGE_PASSPHRASE.setValue(controller,
                    IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId() + IdGenerator.makeId());
            controller.saveConfig();
        }
    }

}
