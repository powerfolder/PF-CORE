package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.DiskItem;

import java.util.Date;

/**
 * Holds FileInfo for a file plus whether the file is new (recently downloaded).
 */
public class DirectoryTableFileBean implements DiskItem {

    private FileInfo fileInfo;
    private boolean recentlyDownloaded;

    public DirectoryTableFileBean(FileInfo fileInfo, boolean recentlyDownloaded) {
        this.fileInfo = fileInfo;
        this.recentlyDownloaded = recentlyDownloaded;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean isRecentlyDownloaded() {
        return recentlyDownloaded;
    }

    public String getExtension() {
        return fileInfo.getExtension();
    }

    public FolderInfo getFolderInfo() {
        return fileInfo.getFolderInfo();
    }

    public String getLowerCaseName() {
        return fileInfo.getLowerCaseName();
    }

    public MemberInfo getModifiedBy() {
        return fileInfo.getModifiedBy();
    }

    public Date getModifiedDate() {
        return fileInfo.getModifiedDate();
    }

    public String getName() {
        return fileInfo.getName();
    }

    public long getSize() {
        return fileInfo.getSize();
    }
}
