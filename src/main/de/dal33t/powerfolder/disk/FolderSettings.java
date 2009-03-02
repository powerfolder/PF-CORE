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

import de.dal33t.powerfolder.util.Reject;

import java.io.File;

/**
 * Class to consolidate the settings for creating a folder.
 * Used as constructor arg for the Folder class.
 */
public class FolderSettings {

    public static final String FOLDER_SETTINGS_PREFIX_V3 = "folder.";
    public static final String FOLDER_SETTINGS_PREFIX_V4 = "f.";
    public static final String FOLDER_SETTINGS_ID = ".id";
    public static final String FOLDER_SETTINGS_PREVIEW = ".preview";
    public static final String FOLDER_SETTINGS_DONT_RECYCLE = ".dontuserecyclebin";
    public static final String FOLDER_SETTINGS_SYNC_PROFILE = ".syncprofile";
    public static final String FOLDER_SETTINGS_DIR = ".dir";
    public static final String FOLDER_SETTINGS_LAST_LOCAL = ".last-localbase";
    public static final String FOLDER_SETTINGS_WHITELIST = ".whitelist";
    public static final String FOLDER_SETTINGS_NAME = ".name"; // V4 only
    public static final String FOLDER_SETTINGS_RECYCLE = ".recycle"; // V4 only

    /**
     * Base location of files in the folder.
     */
    private final File localBaseDir;

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
     * Whether the folder move deleted items to the recycle bin.
     */
    private final boolean useRecycleBin;

    /**
     * Whether this sould only be a preview of the folder.
     */
    private boolean previewOnly;

    /**
     * Whether the folder is a black- or white-list.
     */
    private boolean whitelist;

    /**
     * Constructor. Creates a new FolderSettings object.
     * @param localBaseDir
     * @param syncProfile
     * @param createInvitationFile
     * @param useRecycleBin
     * @param previewOnly
     * @param whitelist
     */
    public FolderSettings(File localBaseDir,
                          SyncProfile syncProfile,
                          boolean createInvitationFile,
                          boolean useRecycleBin,
                          boolean previewOnly,
                          boolean whitelist) {
        Reject.ifNull(localBaseDir, "Local base dir required");
        Reject.ifNull(syncProfile, "Sync profile required");
        this.localBaseDir = localBaseDir;
        this.syncProfile = syncProfile;
        this.createInvitationFile = createInvitationFile;
        this.useRecycleBin = useRecycleBin;
        this.previewOnly = previewOnly;
        this.whitelist = whitelist;
    }
    
    /**
     * Constructor. Creates a new FolderSettings object.
     * @param localBaseDir
     * @param syncProfile
     * @param createInvitationFile
     * @param useRecycleBin
     */
    public FolderSettings(File localBaseDir,
                          SyncProfile syncProfile,
                          boolean createInvitationFile,
                          boolean useRecycleBin) {
       this(localBaseDir, syncProfile, createInvitationFile, useRecycleBin, false, false);
    }

    ///////////////
    // Accessors //
    ///////////////

    public File getLocalBaseDir() {
        return localBaseDir;
    }

    public SyncProfile getSyncProfile() {
        return syncProfile;
    }

    public boolean isCreateInvitationFile() {
        return createInvitationFile;
    }

    public boolean isUseRecycleBin() {
        return useRecycleBin;
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public boolean isWhitelist() {
        return whitelist;
    }
}
