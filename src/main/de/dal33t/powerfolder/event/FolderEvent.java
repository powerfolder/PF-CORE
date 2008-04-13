package de.dal33t.powerfolder.event;

import java.util.Collection;
import java.util.EventObject;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.util.Reject;

/**
 * Event about changes in the folder, mostly file changes. Exception: Stats
 * calculated event.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FolderEvent extends EventObject {
    /**
     * In case the local folder has been scanned.
     */
    private ScanResult scanResult;

    /**
     * Changes from the remote side. new list
     */
    private FileList fileList;

    /**
     * Changes from the remote side. new list
     */
    private FolderFilesChanged changeList;

    /**
     * The single file that has been freshly scanned
     */
    private FileInfo scannedFileInfo;

    /**
     * The locally deleted files
     */
    private Collection<FileInfo> deletedFileInfos;

    /**
     * The new syncprofile of the folder.
     */
    private SyncProfile newSyncProfile;

    public FolderEvent(Folder source) {
        super(source);
    }

    public FolderEvent(Folder source, ScanResult sr) {
        super(source);
        Reject.ifNull(sr, "ScanResult is null");
        this.scanResult = sr;
    }

    public FolderEvent(Folder source, FileList fileList) {
        super(source);
        Reject.ifNull(fileList, "Filelist is null");
        this.fileList = fileList;
    }

    public FolderEvent(Folder source, FolderFilesChanged changeList) {
        super(source);
        Reject.ifNull(changeList, "ChangeList is null");
        this.changeList = changeList;
    }

    public FolderEvent(Folder source, FileInfo fileInfo) {
        super(source);
        Reject.ifNull(fileInfo, "FileInfo is null");
        this.scannedFileInfo = fileInfo;
    }

    public FolderEvent(Folder source, Collection<FileInfo> fileInfos) {
        super(source);
        Reject.ifNull(fileInfos, "DeletedFileInfos is null");
        this.deletedFileInfos = fileInfos;
    }

    public FolderEvent(Folder source, SyncProfile profile) {
        super(source);
        Reject.ifNull(profile, "New sync profile is null");
        this.newSyncProfile = profile;
    }

    public Folder getFolder() {
        return (Folder) getSource();
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public FileList getFileList() {
        return fileList;
    }

    public FolderFilesChanged getChangeList() {
        return changeList;
    }

    public FileInfo getScannedFileInfo() {
        return scannedFileInfo;
    }

    public Collection<FileInfo> getDeletedFileInfos() {
        return deletedFileInfos;
    }

    public SyncProfile getNewSyncProfile() {
        return newSyncProfile;
    }
}
