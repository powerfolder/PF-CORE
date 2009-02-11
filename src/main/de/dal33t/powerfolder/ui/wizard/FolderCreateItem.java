package de.dal33t.powerfolder.ui.wizard;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.Reject;

import java.io.File;

/**
 * This class encapsulates requirements to create a folder in the Wizard flow.
 * Used where creating multiple folders.
 */
public class FolderCreateItem {

    private File localBase;
    private FolderInfo folderInfo;
    private SyncProfile syncProfile;

    public FolderCreateItem(File localBase) {
        this.localBase = localBase;
    }

    public File getLocalBase() {
        return localBase;
    }

    public void setLocalBase(File localBase) {
        Reject.ifNull(localBase, "Local base cannot be set null");
        this.localBase = localBase;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public void setFolderInfo(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
    }

    public SyncProfile getSyncProfile() {
        return syncProfile;
    }

    public void setSyncProfile(SyncProfile syncProfile) {
        this.syncProfile = syncProfile;
    }
}
