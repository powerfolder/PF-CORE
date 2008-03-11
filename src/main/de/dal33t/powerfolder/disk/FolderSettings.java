package de.dal33t.powerfolder.disk;

import java.io.File;

/**
 * Class to consolidate the settings for creating a folder.
 * Used as constructor arg for the Folder class.
 */
public class FolderSettings implements Cloneable {

    public static final String FOLDER_SETTINGS_PREFIX = "folder.";
    public static final String FOLDER_SETTINGS_ID = ".id";
    public static final String FOLDER_SETTINGS_PREVIEW = ".preview";
    public static final String FOLDER_SETTINGS_SECRET = ".secret";
    public static final String FOLDER_SETTINGS_DONT_RECYCLE = ".dontuserecyclebin";
    public static final String FOLDER_SETTINGS_SYNC_PROFILE = ".syncprofile";
    public static final String FOLDER_SETTINGS_DIR = ".dir";
    public static final String FOLDER_SETTINGS_LAST_LOCAL = ".last-localbase";    

    /**
     * Base location of files in the folder.
     */
    private File localBaseDir;

    /** Base location to remember in a preview folder for when it is converted
     * to a normal folder  */
    private File conversionLocalBaseDir;

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
     * @param previewOnly
     * @param realLocalBaseDir
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

    public void setLocalBaseDir(File localBaseDir) {
        this.localBaseDir = localBaseDir;
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

    public File getConversionLocalBaseDir() {
        return conversionLocalBaseDir;
    }

    public void setConversionLocalBaseDir(File conversionLocalBaseDir) {
        this.conversionLocalBaseDir = conversionLocalBaseDir;
    }
    
    public Object clone() {
        FolderSettings fs = new FolderSettings(localBaseDir, syncProfile,
                createInvitationFile, useRecycleBin, previewOnly);
        fs.conversionLocalBaseDir = conversionLocalBaseDir;
        return fs;
    }
}
