package de.dal33t.powerfolder.util;

import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_ARCHIVE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DIR;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DOWNLOAD_SCRIPT;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_ID;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_NAME;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX_V4;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREVIEW;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_WHITELIST;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Folder;
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
            restoreFolderConfig(folderDir, config);
        }
        PropertiesUtil.saveConfig(new File("PowerFolder_restored.config"),
            config, "");
    }

    private static void restoreFolderConfig(File baseDir, Properties config) {
        FolderInfo foInfo;
        try {
            foInfo = readFolderInfo(baseDir);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while reading folderinfo from "
                + baseDir + ". " + e, e);
            return;
        }
        if (foInfo == null) {
            LOG.severe("Unable to read folderinfo from " + baseDir);
            return;
        }
        FolderSettings foSettings = new FolderSettings(baseDir,
            SyncProfile.BACKUP_TARGET_NO_CHANGE_DETECT, false,
            ArchiveMode.FULL_BACKUP);
        saveFolderConfig(foInfo, foSettings, config);
    }

    private static FolderInfo readFolderInfo(File baseDir) throws IOException,
        ClassNotFoundException
    {
        File sysDir = new File(baseDir, Constants.POWERFOLDER_SYSTEM_SUBDIR);
        File dbFile = new File(sysDir, Folder.DB_FILENAME);
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

    public static void saveFolderConfig(FolderInfo folderInfo,
        FolderSettings folderSettings, Properties config)
    {
        String md5 = new String(Util.encodeHex(Util.md5(folderInfo.id
            .getBytes())));
        // store folder in config
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_NAME, folderInfo.name);
        config
            .setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5 + ".recycle", "true");
        config
            .setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5 + FOLDER_SETTINGS_ID,
                folderInfo.id);
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_DIR, folderSettings.getLocalBaseDir()
            .getAbsolutePath());
        // Save sync profiles as internal configuration for custom profiles.
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_SYNC_PROFILE, folderSettings.getSyncProfile()
            .getFieldList());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_ARCHIVE, folderSettings.getArchiveMode().name());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_PREVIEW, String.valueOf(folderSettings
            .isPreviewOnly()));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_WHITELIST, String.valueOf(folderSettings
            .isWhitelist()));
        String dlScript = folderSettings.getDownloadScript() != null
            ? folderSettings.getDownloadScript()
            : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_DOWNLOAD_SCRIPT, dlScript);

    }
}
