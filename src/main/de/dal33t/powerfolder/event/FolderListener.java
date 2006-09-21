package de.dal33t.powerfolder.event;

/** Fired by Folder if the status of the Folder changes */
public interface FolderListener extends CoreListener {
    /**
     * The statistics gets calculated delayed after a folder changed or the
     * remote contents changed
     */
    public void statisticsCalculated(FolderEvent folderEvent);

    /** Contents on disk of the folder have changed */
    public void folderChanged(FolderEvent folderEvent);

    /** The synchronization profile changed */
    public void syncProfileChanged(FolderEvent folderEvent);

    /**
     * The remote contents of a Folder changed (e.g. a file was added remote or
     * deleted)
     */
    public void remoteContentsChanged(FolderEvent folderEvent);
}
