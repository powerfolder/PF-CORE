package de.dal33t.powerfolder.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
        File searchBaseDir = new File(args[0]);
        for (File folderDir : searchBaseDir.listFiles()) {
            File sysDir = new File(folderDir,
                Constants.POWERFOLDER_SYSTEM_SUBDIR);
            LOG.info("Processing directory: " + folderDir);
            if (sysDir.exists()) {
                try {
                    // Folder directly under PowerFolders/
                    restoreFolderConfig(folderDir, config);
                } catch (Exception e) {
                    LOG.warning("Problem with " + folderDir + ". " + e);
                }
            } else if (folderDir.isDirectory()) {
                try {
                    // PowerFolders/username/foldername
                    // Try harder. Subdirs:
                    for (File folderDir2 : folderDir.listFiles()) {
                        if (folderDir2.isDirectory()) {
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
        PropertiesUtil.saveConfig(new File("PowerFolder_restored.config"),
            config, "");
    }

    private static void restoreFolderConfig(File baseDir, Properties config) {
        FolderInfo foInfo;
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
            SyncProfile.AUTOMATIC_SYNCHRONIZATION, false, 0);
        foSettings.set(foInfo, config);
        LOG.info("Restored folder " + foInfo.getName() + " @ " + baseDir);
    }

    private static FolderInfo readFolderInfo(File baseDir) throws IOException,
        ClassNotFoundException
    {
        File sysDir = new File(baseDir, Constants.POWERFOLDER_SYSTEM_SUBDIR);
        if (!baseDir.exists()) {
            throw new FileNotFoundException("No folder found at " + sysDir);
        }
        File dbFile = new File(sysDir, Constants.DB_FILENAME);
        // load files and scan in
        InputStream fIn = new BufferedInputStream(new FileInputStream(dbFile));
        ObjectInputStream in = new ObjectInputStream(fIn);
        FileInfo[] files = (FileInfo[]) in.readObject();
        // LOG.info("Got " + files.length + " files in " + baseDir);
        if (files.length <= 0) {
            return null;
        }
        return files[0].getFolderInfo();
    }

}
