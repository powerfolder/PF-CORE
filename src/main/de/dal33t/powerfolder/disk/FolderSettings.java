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
import java.util.Properties;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * Class to consolidate the settings for creating a folder. Used as constructor
 * arg for the Folder class.
 */
public class FolderSettings {

    public static final String FOLDER_SETTINGS_PREFIX_V3 = "folder.";
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

    /**
     * Base location of files in the folder.
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

    public void set(FolderInfo folderInfo, Properties config) {
        String md5 = new String(Util.encodeHex(Util.md5(folderInfo.id
            .getBytes())));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_NAME, folderInfo.name);
        config
            .setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5 + FOLDER_SETTINGS_ID,
                folderInfo.id);
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_DIR, getLocalBaseDir().getAbsolutePath());
        String commitDir = getCommitDir() != null ? getCommitDir()
            .getAbsolutePath() : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_COMMIT_DIR, commitDir);
        // Save sync profiles as internal configuration for custom profiles.
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_SYNC_PROFILE, getSyncProfile().getFieldList());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_ARCHIVE, getArchiveMode().name());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_VERSIONS, String.valueOf(getVersions()));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_PREVIEW, String.valueOf(isPreviewOnly()));
        String dlScript = getDownloadScript() != null
            ? getDownloadScript()
            : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_DOWNLOAD_SCRIPT, dlScript);
    }

}
