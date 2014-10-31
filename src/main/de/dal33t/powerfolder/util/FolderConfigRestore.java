package de.dal33t.powerfolder.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

public class FolderConfigRestore {
    private static final Logger LOG = Logger
        .getLogger(FolderConfigRestore.class.getName());

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Properties config = new Properties();
        Path searchBaseDir = Paths.get(args[0]);
        DirectoryStream<Path> folderDirs = Files.newDirectoryStream(searchBaseDir);
        for (Path folderDir : folderDirs) {
            Path sysDir = folderDir.resolve(
                Constants.POWERFOLDER_SYSTEM_SUBDIR);
            LOG.info("Processing directory: " + folderDir);
            if (Files.exists(sysDir)) {
                try {
                    // Folder directly under PowerFolders/
                    restoreFolderConfig(folderDir, config);
                } catch (Exception e) {
                    LOG.warning("Problem with " + folderDir + ". " + e);
                }
            } else if (Files.isDirectory(folderDir)) {
                try (DirectoryStream<Path> folderDirs2 = Files.newDirectoryStream(folderDir)) {
                    // PowerFolders/username/foldername
                    // Try harder. Subdirs:
                    for (Path folderDir2 : folderDirs2) {
                        if (Files.isDirectory(folderDir2)) {
                            try {
                                restoreFolderConfig(folderDir2, config);
                            } catch (Exception e) {
                                LOG.warning("Problem with " + folderDir2 + ". "
                                    + e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warning("Problem with " + folderDir + ". " + e);
                }
            }
        }
        PropertiesUtil.saveConfig(Paths.get("PowerFolder_restored.config"),
            config, "");
    }

    private static void restoreFolderConfig(Path baseDir, Properties config) {
        FolderInfo foInfo;
        baseDir = PathUtils.removeInvalidFilenameChars(baseDir);
        try {
            foInfo = readFolderInfo(baseDir);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to read folder info from " + baseDir
                + ". " + e);
            return;
        }
        if (foInfo == null) {
            LOG.severe("Unable to read folderinfo from " + baseDir);
            return;
        }
        FolderSettings foSettings = new FolderSettings(baseDir,
            SyncProfile.AUTOMATIC_SYNCHRONIZATION, 0);
        foSettings.set(foInfo, config);
        LOG.info("Restored folder " + foInfo.getName() + " @ " + baseDir);
    }

    private static FolderInfo readFolderInfo(Path baseDir) throws IOException,
        ClassNotFoundException
    {
        Path sysDir = baseDir.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR);
        if (Files.notExists(baseDir)) {
            throw new FileNotFoundException("No folder found at " + sysDir);
        }
        Path dbFile = sysDir.resolve(Constants.DB_FILENAME);
        // load files and scan in
        ObjectInputStream in = new ObjectInputStream(
            Files.newInputStream(dbFile));
        FileInfo[] files = (FileInfo[]) in.readObject();
        // LOG.info("Got " + files.length + " files in " + baseDir);
        if (files.length <= 0) {
            return null;
        }
        return files[0].getFolderInfo();
    }

}
