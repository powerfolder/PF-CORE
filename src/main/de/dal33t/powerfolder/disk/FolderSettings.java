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
 * $Id: FolderSettings.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.disk;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;
import de.dal33t.powerfolder.util.Util;

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
    public static final String FOLDER_SETTINGS_VERSIONS = ".versions";
    public static final String FOLDER_SETTINGS_SYNC_PATTERNS = ".sync-patterns";
    public static final String FOLDER_SETTINGS_SYNC_WARN_SECONDS = ".sync-warn-seconds";

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
    private final Path localBaseDir;

    /**
     * #2056: The directory to commit/mirror the whole folder to when in reaches
     * 100% sync.
     */
    private final Path commitDir;

    /**
     * The synchronization profile for the folder (manual, backup, etc)
     */
    private final SyncProfile syncProfile;

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
     * #2265: Extend monitoring. negative value = disabled, 0 = use default,
     * positive value = warn if folder is X seconds out of sync.
     */
    private final int syncWarnSeconds;

    /**
     * Loaded from that config entry id.
     */
    private String configEntryId;

    /**
     * Constructor. Creates a new FolderSettings object. NON preview, NO
     * download script.
     * 
     * @param localBaseDir
     * @param syncProfile
     * @param createInvitationFile
     * @param versions
     */
    public FolderSettings(Path localBaseDir, SyncProfile syncProfile,
        boolean createInvitationFile, int versions)
    {
        this(localBaseDir, syncProfile, createInvitationFile,
            false, null, versions, true);
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
     */
    public FolderSettings(Path localBaseDir, SyncProfile syncProfile,
        boolean createInvitationFile,
        boolean previewOnly, String downloadScript, int versions,
        boolean syncPatterns)
    {
        this(localBaseDir, syncProfile, createInvitationFile,
            previewOnly, downloadScript, versions, syncPatterns, null, 0);
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
     * @param syncWarnSeconds
     */
    public FolderSettings(Path localBaseDir, SyncProfile syncProfile,
        boolean createInvitationFile,
        boolean previewOnly, String downloadScript, int versions,
        boolean syncPatterns, Path commitDir, int syncWarnSeconds)
    {
        Reject.ifNull(localBaseDir, "Local base dir required");
        Reject.ifNull(syncProfile, "Sync profile required");
        this.localBaseDir = localBaseDir;
        this.commitDir = commitDir;
        this.syncProfile = syncProfile;
        this.previewOnly = previewOnly;
        this.downloadScript = downloadScript;
        this.versions = versions;
        this.syncPatterns = syncPatterns;
        this.syncWarnSeconds = syncWarnSeconds;
        // Generate a unique entry id for config file.
        this.configEntryId = new String(Util.encodeHex(Util.md5(IdGenerator
            .makeIdBytes())));
    }

    // /////////////
    // Accessors //
    // /////////////

    public Path getLocalBaseDir() {
        return localBaseDir;
    }

    public Path getCommitDir() {
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
        return false;
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

    public int getSyncWarnSeconds() {
        return syncWarnSeconds;
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
        int defaultVersions = ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
            .getValueInt(c);
        String defSyncProfile = ConfigurationEntry.DEFAULT_TRANSFER_MODE
            .getValue(c);
        return load(c.getConfig(), entryId, defaultVersions, defSyncProfile,
            true);
    }

    public static FolderSettings load(Properties properties, String entryId,
        int fallbackDefaultVersions, String fallbackDefaultProfile,
        boolean verify)
    {
        Reject.ifNull(properties, "Config");
        Reject.ifBlank(entryId, "Entry Id");
        String folderDirStr = properties.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + entryId + FOLDER_SETTINGS_DIR);

        Path folderDir = translateFolderDir(folderDirStr, verify);
        if (folderDir == null) {
            return null;
        }

        Path commitDir = null;
        String commitDirStr = properties.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + entryId + FOLDER_SETTINGS_COMMIT_DIR);
        if (StringUtils.isNotBlank(commitDirStr)) {
            commitDir = translateFolderDir(commitDirStr, verify);
        }

        String syncProfConfig = properties
            .getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_SYNC_PROFILE);

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
        } else if (StringUtils.isBlank(syncProfConfig)) {
            // Take default:
            syncProfile = SyncProfile
                .getSyncProfileByFieldList(fallbackDefaultProfile);
        } else {
            // Load profile from field list.
            syncProfile = SyncProfile.getSyncProfileByFieldList(syncProfConfig);
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

        String syncWarnSecSetting = properties
            .getProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_SYNC_WARN_SECONDS);
        int syncWarnSeconds = 0;
        if (StringUtils.isNotBlank(syncWarnSecSetting)) {
            try {
                syncWarnSeconds = Integer.parseInt(syncWarnSecSetting);
            } catch (Exception e) {
                LOG.warning("Unable to parse sync warning settings: "
                    + syncWarnSecSetting + ". Using default.");
            }
        }

        FolderSettings settings = new FolderSettings(folderDir, syncProfile,
            false, preview, dlScript, versions, syncPatterns,
            commitDir, syncWarnSeconds);
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
            baseDir = localBaseDir.toAbsolutePath().toString();
        }
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_DIR, baseDir);
        String commitDirStr = commitDir != null
            ? commitDir.toAbsolutePath().toString()
            : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_COMMIT_DIR, commitDirStr);
        // Save sync profiles as internal configuration for custom profiles.
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_SYNC_PROFILE, syncProfile.getFieldList());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_VERSIONS, String.valueOf(versions));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_PREVIEW, String.valueOf(isPreviewOnly()));
        String dlScript = getDownloadScript() != null
            ? getDownloadScript()
            : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_DOWNLOAD_SCRIPT, dlScript);

        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
            + FOLDER_SETTINGS_SYNC_PATTERNS, String.valueOf(syncPatterns));
        if (syncWarnSeconds > 0) {
            config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_SYNC_WARN_SECONDS,
                String.valueOf(syncWarnSeconds));
        } else {
            config.remove(FOLDER_SETTINGS_PREFIX_V4 + entryId
                + FOLDER_SETTINGS_SYNC_WARN_SECONDS);
        }
    }

    private static Path translateFolderDir(String str, boolean verify) {
        if (str == null) {
            return null;
        }
        if (!str.contains("$")) {
            // No placeholders found
            try {
                return Paths.get(URI.create(str));
            } catch (IllegalArgumentException iae) {
                return Paths.get(str);
            }
        }
        String res = str;
        try {
            Map<String, UserDirectory> dirs = UserDirectories
                .getUserDirectories();
            LOG.fine("Local placeholder directories: " + dirs);
            for (UserDirectory dir : dirs.values()) {
                if (StringUtils.isBlank(dir.getPlaceholder())) {
                    continue;
                }
                if (res.contains(dir.getPlaceholder())) {
                    res = res.replace(dir.getPlaceholder(), dir.getDirectory()
                        .toAbsolutePath().toString());
                }
            }
        } catch (Exception e) {
            LOG.warning("Unable to translate directory path: " + str + ". " + e);
        }
        if (verify) {
            if (res.contains("$user.dir.") || res.contains("$apps.dir.")) {
                LOG.warning("Local directory for placeholders not found: "
                    + res);
                return null;
            }
            if (res.contains("$")) {
                LOG.warning("Directory path may still contain placeholders: "
                    + res);
            }
        }

        try {
            return Paths.get(URI.create(res));
        } catch (IllegalArgumentException iae) {
            return Paths.get(res);
        }
    }
}
