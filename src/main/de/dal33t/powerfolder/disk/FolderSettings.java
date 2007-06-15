package de.dal33t.powerfolder.disk;

import java.io.File;

public class FolderSettings {

    private File localBaseDir;
    private SyncProfile syncProfile;
    private boolean createInvitationFile;
    private boolean useRecycleBin;

    public FolderSettings(File localBaseDir, SyncProfile syncProfile, boolean createInvitationFile, boolean useRecycleBin) {
        this.localBaseDir = localBaseDir;
        this.syncProfile = syncProfile;
        this.createInvitationFile = createInvitationFile;
        this.useRecycleBin = useRecycleBin;
    }

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
}
