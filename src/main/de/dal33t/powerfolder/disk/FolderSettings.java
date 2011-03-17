/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id$
 */
package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.UserDirectories;
import de.dal33t.powerfolder.util.ui.UserDirectory;

/**
 * Class to consolidate the settings for creating a folder. Used as constructor
 * arg for the Folder class.
 */
public class FolderSettings {

    public static final Logger LOG = Logger.getLogger(FolderSettings.class
        .getName());
    public static final String FOLDER_SETTINGS_PREFIX_V4 = "f.";
    public static final String FOLDER_SETTINGS_ID = ".id";
    public static final String FOLDER_SETTINGS_PREVIEW = ".preview";
    public static final String FOLDER_SETTINGS_SYNC_PROFILE = ".syncprofile";
    public static final String FOLDER_SETTINGS_DIR = ".dir";
    public static final String FOLDER_SETTINGS_COMMIT_DIR = ".commit-dir";
    public static final String FOLDER_SETTINGS_DOWNLOAD_SCRIPT = ".dlscript";
    public static final String FOLDER_SETTINGS_NAME = ".name";
    public static final String FOLDER_SETTINGS_ARCHIVE = ".archive";
    public static final String FOLDER_SETTINGS_VERSIONS = ".versions";
    public static final String FOLDER_SETTINGS_SYNC_PATTERNS = ".sync-patterns";

    public static final String FOLDER_ID_GENERATE = "$generate";
    public static final String FOLDER_ID_FROM_ACCOUNT = "$fromAccount";

    /**
     * Field list for backup taget pre #777. Used to convert to new backup
     * target for #787.
     */
    private static final String PRE_777_BACKUP_TARGET_FIELD_LIST = "true,true,true,true,0,false,12,0,m";
    private static final String PRE_2040_AUTOMATIC_SYNCHRONIZATION_FIELD_LIST = "true,true,true,true,1,false,12,0,m,"
        + Translation
            .getTranslation("transfer_mode.automatic_synchronization.name");
    private static final String PRE_2040_AUTOMATIC_SYNCHRONIZATION_10MIN_FIELD_LIST = "true,true,true,true,10,false,12,0,m,"
        + Translation
            .getTranslation("transfer_mode.automatic_synchronization_10min.name");
    private static final String PRE_2074_BACKUP_SOURCE_5MIN_FIELD_LIST = "false,false,false,false,5,false,12,0,m,"
        + Translation.getTranslation("transfer_mode.backup_source_5min.name")
        + ",false";
    private static final String PRE_2074_BACKUP_SOURCE_HOUR_FIELD_LIST = "false,false,false,false,60,false,12,0,m,"
        + Translation.getTranslation("transfer_mode.backup_source_hour.name")
        + ",false";

    /**
     * Original value from config.
     */
    private String localBaseDirStr;

    /**
     * Physical location of files in the folder.
     */
    private final File localBaseDir;

    /**
     * #2056: The directory to commit/mirror the whole folder to when in reaches
     * 100% sync.
     */
    private final File commitDir;

    /**
     * The synchronization profile for the folder (manual, backup, etc)
     */
    private final SyncProfile syncProfile;

    /**
     * Whether an invitation file should be created at the time the folder is
     * constructed.
     */
    private final boolean createInvitationFile;

    /**
     * How to archive local files
     */
    private final ArchiveMode archiveMode;

    /**
     * The number of archive versions to keep
     */
    private final int versions;

    /**
     * Whether this sould only be a preview of the folder.
     */
    private final boolean previewOnly;

    /**
     * #1538: Script that gets executed after a download has been completed
     * successfully.
     */
    private final String downloadScript;

    private final boolean syncPatterns;

    /**
     * Loaded from that config entry id.
     */
    private String configEntryId;

    /**
     * Constructor. Creates a new FolderSettings object.
     * 
     * @param localBaseDir
     * @param syncProfile
     * @param createInvitationFile
     * @param archiveMode
     * @param previewOnly
     * @param downloadScript
     * @param versions
     * @param syncPatterns
     */
    public FolderSettings(File localBaseDir, SyncProfile syncProfile,
        boolean createInvitationFile, ArchiveMode archiveMode,
        boolean previewOnly, String downloadScript, int versions,
        boolean syncPatterns)
    {
        this(localBaseDir, syncProfile, createInvitationFile, archiveMode,
            previewOnly, downloadScript, versions, syncPatterns, null);
    }

    /**
     * Constructor. Creates a new FolderSettings object.
     * 
     * @param localBaseDir
     * @param syncProfile
     * @param createInvitationFile
     * @param archiveMode
     * @param previewOnly
     * @param downloadScript
     * @param versions
     * @param syncPatterns
     * @param commitDir
     */
    public FolderSettings(File localBaseDir, SyncProfile syncProfile,
        boolean createInvitationFile, ArchiveMode archiveMode,
        boolean previewOnly, String downloadScript, int versions,
        boolean syncPatterns, File commitDir)
    {

        Reject.ifNull(localBaseDir, "Local base dir required");
        Reject.ifNull(syncProfile, "Sync profile required");
        this.localBaseDir = localBaseDir;
        this.commitDir = commitDir;
        this.syncProfile = syncProfile;
        this.createInvitationFile = createInvitationFile;
        this.archiveMode = archiveMode;
        this.previewOnly = previewOnly;
        this.downloadScript = downloadScript;
        this.versions = versions;
        this.syncPatterns = syncPatterns;
        // Generate a unique entry id for config file.
        this.configEntryId = new String(Util.encodeHex(Util.md5(IdGenerator
            .makeIdBytes())));
    }

    /**
     * Constructor. Creates a new FolderSettings object. NON preview, NO
     * download script.
     * 
     * @param localBaseDir
     * @param syncProfile
     * @param createInvitationFile
     * @param versions
     */
    public FolderSettings(File localBaseDir, SyncProfile syncProfile,
        boolean createInvitationFile, ArchiveMode archiveMode, int versions)
    {
        this(localBaseDir, syncProfile, createInvitationFile, archiveMode,
            false, null, versions, true);
    }

    // /////////////
    // Accessors //
    // /////////////

    public File getLocalBaseDir() {
        return localBaseDir;
    }

    public File getCommitDir() {
        return commitDir;
    }

    /**
     * @return the original config entry basedir. May contain placeholder
     */
    public String getLocalBaseDirString() {
        return localBaseDirStr;
    }

    public SyncProfile getSyncProfile() {
        return syncProfile;
    }

    public boolean isCreateInvitationFile() {
        return createInvitationFile;
    }

    public ArchiveMode getArchiveMode() {
        return archiveMode;
    }

    public int getVersions() {
        return versions;
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public String getDownloadScript() {
        return downloadScript;
    }

    public boolean isSyncPatterns() {
        return syncPatterns;
    }

    /**
     * @return the entry id used to get/set {@link FolderSettings} to config.
     *         e.g. FOLDER_SETTINGS_PREFIX_V4 + entryId + FOLDER_SETTINGS_DIR
     */
    public String getConfigEntryId() {
        return configEntryId;
    }

    /**
     * @param properties
     * @param entryId
     * @return
     */
    public static String loadFolderName(Properties properties, String entryId) {
        Reject.ifBlank(entryId, "EntryId");
        return properties.getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_NAME);
    }

    /**
     * @param properties
     *            the config properties
     * @return all entry ids as set.
     */
    public static Set<String> loadEntryIds(Properties properties) {
        // Find all folder names.
        Set<String> entryIds = new TreeSet<String>();
        for (@SuppressWarnings("unchecked")
        Enumeration<String> en = (Enumeration<String>) properties
            .propertyNames(); en.hasMoreElements();)
        {
            String propName = en.nextElement();
            // Look for a f.<entryId>.XXXX
            if (propName.startsWith(FOLDER_SETTINGS_PREFIX_V4)) {
                int firstDot = propName.indexOf('.');
                int secondDot = propName.indexOf('.', firstDot + 1);

                if (firstDot > 0 && secondDot > 0
                    && secondDot < propName.length())
                {
                    String entryId = propName
                        .substring(firstDot + 1, secondDot);
                    entryIds.add(entryId);
                }
            }
        }
        return entryIds;
    }

    /**
     * Expect something like 'f.c70001efd21928644ee14e327aa94724' or
     * 'f.TEST-Contacts' to remove config entries beginning with these.
     */
    public static void removeEntries(Properties p, String entryId) {
        for (@SuppressWarnings("unchecked")
        Enumeration<String> en = (Enumeration<String>) p.propertyNames(); en
            .hasMoreElements();)
        {
            String propName = en.nextElement();

            // Add a dot to prefix, like 'f.TEST-Contacts.', to prevent it
            // from also deleting things like 'f.TEST.XXXXX'.
            if (propName.startsWith(FOLDER_SETTINGS_PREFIX_V4 + entryId + '.'))
            {
                p.remove(propName);
            }
        }
    }

    public static FolderSettings load(Controller c, String entryId) {
        int defaultVersions = ConfigurationEntry.DEFAULT_ARCHIVE_VERIONS
            .getValueInt(c);
        return load(c.getConfig(), entryId, defaultVersions);
    }

    public static FolderSettings load(Properties properties, String entryId,
        int fallbackDefaultVersions)
    {
        Reject.ifNull(properties, "Config");
        Reject.ifBlank(entryId, "Entry Id");
        String folderDirStr = properties.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + entryId + FOLDER_SETTINGS_DIR);

        File folderDir = translateFolderDir(folderDirStr);
        if (folderDir == null) {
            return null;
        }

        File commitDir = null;
        String commitDirStr = properties.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + entryId + FOLDER_SETTINGS_COMMIT_DIR);
        if (StringUtils.isNotBlank(commitDirStr)) {
            commitDir = translateFolderDir(commitDirStr);
        }

        String syncProfConfig = properties
            .getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_SYNC_PROFILE);

        // Migration for #603
        if ("autodownload_friends".equals(syncProfConfig)) {
            syncProfConfig = SyncProfile.AUTO_DOWNLOAD_FRIENDS.getFieldList();
        }

        SyncProfile syncProfile;
        if (PRE_777_BACKUP_TARGET_FIELD_LIST.equals(syncProfConfig)) {
            // Migration for #787 (backup target timeBetweenScans changed
            // from 0 to 60).
            syncProfile = SyncProfile.BACKUP_TARGET;
        } else if (PRE_2040_AUTOMATIC_SYNCHRONIZATION_FIELD_LIST
            .equals(syncProfConfig)
            || PRE_2040_AUTOMATIC_SYNCHRONIZATION_10MIN_FIELD_LIST
                .equals(syncProfConfig))
        {
            // Migration for #2040 (new auto sync uses JNotify).
            syncProfile = SyncProfile.AUTOMATIC_SYNCHRONIZATION;
        } else if (PRE_2074_BACKUP_SOURCE_5MIN_FIELD_LIST
            .equals(syncProfConfig)
            || PRE_2074_BACKUP_SOURCE_HOUR_FIELD_LIST.equals(syncProfConfig))
        {
            // Migration for #2074 (new backup source uses JNotify).
            syncProfile = SyncProfile.BACKUP_SOURCE;
        } else {
            // Load profile from field list.
            syncProfile = SyncProfile.getSyncProfileByFieldList(syncProfConfig);
        }

        String archiveSetting = properties
            .getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_ARCHIVE);
        ArchiveMode archiveMode;
        try {
            if (archiveSetting != null) {
                archiveMode = ArchiveMode.valueOf(archiveSetting);
            } else {
                LOG.log(Level.WARNING, "ArchiveMode not set: " + archiveSetting
                    + ", falling back to DEFAULT: " + fallbackDefaultVersions);
                archiveMode = ArchiveMode.FULL_BACKUP;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unsupported ArchiveMode: " + archiveSetting
                + ", falling back to DEFAULT: " + fallbackDefaultVersions);
            LOG.log(Level.FINE, e.toString(), e);
            archiveMode = ArchiveMode.FULL_BACKUP;
        }

        String ver = properties.getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_VERSIONS);
        int versions;
        if (ver != null && ver.length() > 0) {
            versions = Integer.valueOf(ver);
        } else {
            // Take default as fallback.
            versions = fallbackDefaultVersions;
            LOG.fine("Unable to find archive settings for " + entryId
                + ". Using default: " + versions);
        }

        String previewSetting = properties
            .getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_PREVIEW);
        boolean preview = previewSetting != null
            && "true".equalsIgnoreCase(previewSetting);

        String dlScript = properties.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + entryId + FOLDER_SETTINGS_DOWNLOAD_SCRIPT);

        String syncPatternsSetting = properties
            .getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_SYNC_PATTERNS);
        // Default syncPatterns to true.
        boolean syncPatterns = syncPatternsSetting == null
            || "true".equalsIgnoreCase(syncPatternsSetting);
        FolderSettings settings = new FolderSettings(folderDir, syncProfile,
            false, archiveMode, preview, dlScript, versions, syncPatterns,
            commitDir);
        settings.configEntryId = entryId;
        settings.localBaseDirStr = folderDirStr;
        return settings;
    }

    public void set(FolderInfo folderInfo, Properties config) {
        String entryId = this.configEntryId;

        if (StringUtils.isBlank(entryId)) {
            // Use md5 of folder id as entry id in config...
            entryId = new String(Util.encodeHex(Util.md5(folderInfo.id
                .getBytes())));
        }

        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_NAME, folderInfo.name);
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_ID, folderInfo.id);
        String baseDir = localBaseDirStr;
        if (StringUtils.isBlank(baseDir)) {
            baseDir = getLocalBaseDir().getAbsolutePath();
        }
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_DIR, baseDir);
        String commitDir = getCommitDir() != null ? getCommitDir()
            .getAbsolutePath() : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_COMMIT_DIR, commitDir);
        // Save sync profiles as internal configuration for custom profiles.
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_SYNC_PROFILE, getSyncProfile().getFieldList());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_ARCHIVE, getArchiveMode().name());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_VERSIONS, String.valueOf(getVersions()));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_PREVIEW, String.valueOf(isPreviewOnly()));
        String dlScript = getDownloadScript() != null
            ? getDownloadScript()
            : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_DOWNLOAD_SCRIPT, dlScript);
    }

    private static File translateFolderDir(String str) {
        if (str == null) {
            return null;
        }
        if (!str.contains("$")) {
            // No placeholders found
            return new File(str);
        }
        String res = str;
        try {
            Map<String, UserDirectory> dirs = UserDirectories
                .getUserDirectories();
            for (UserDirectory dir : dirs.values()) {
                if (StringUtils.isBlank(dir.getPlaceholder())) {
                    continue;
                }
                if (res.contains(dir.getPlaceholder())) {
                    res = res.replace(dir.getPlaceholder(), dir.getDirectory()
                        .getAbsolutePath());
                }
            }
        } catch (Exception e) {
            LOG.warning("Unable to translate directory path: " + str + ". " + e);
        }
        if (res.contains("$")) {
            LOG.severe("Directory path still contains placeholders: " + res);
        }
        return new File(res);
    }
}
