package de.dal33t.powerfolder.disk;

import java.io.File;

/**
 * Class to consolidate the settings for creating a folder.
 * Used as constructor arg for the Folder class.
 *
 */
public class FolderSettings {

    /**
     * Base location of files in the folder.
     */
    private File localBaseDir;

    /**
     * The synchronization profile for the folder (manual, backup, etc)
     */
    private SyncProfile syncProfile;

    /**
     * Whether an invitation file should be created at the time the folder is constructed.
     */
    private boolean createInvitationFile;

    /**
     * Whether the folder move deleted items to the recycle bin.
     */
    private boolean useRecycleBin;

    /**
     * Whether this sould only be a preview of the folder.
     */
    private boolean previewOnly;

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
                          boolean useRecycleBin,
                          boolean previewOnly) {
        this.localBaseDir = localBaseDir;
        this.syncProfile = syncProfile;
        this.createInvitationFile = createInvitationFile;
        this.useRecycleBin = useRecycleBin;
        this.previewOnly = previewOnly;
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

    public void setSyncProfile(SyncProfile syncProfile) {
        this.syncProfile = syncProfile;
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public void setPreviewOnly(boolean previewOnly) {
        this.previewOnly = previewOnly;
    }
}
