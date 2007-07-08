package de.dal33t.powerfolder.event;

/** Fired by Folder if the status of the Folder changes */
public interface FolderListener extends CoreListener {

    /**
     * The statistics gets calculated delayed after a folder changed or the
     * remote contents changed
     * 
     * @param folderEvent
     */
    void statisticsCalculated(FolderEvent folderEvent);

    /**
     * Contents on disk of the folder have changed
     * 
     * @param folderEvent
     */
    void folderChanged(FolderEvent folderEvent);

    /**
     * The synchronization profile changed
     * 
     * @param folderEvent
     */
    void syncProfileChanged(FolderEvent folderEvent);

    /**
     * The remote contents of a Folder changed (e.g. a file was added remote or
     * deleted)
     * 
     * @param folderEvent
     */
    void remoteContentsChanged(FolderEvent folderEvent);

    /**
     * Fired when a scanresult was commited (=scan processed finished)
     * 
     * @param folderEvent
     */
    void scanResultCommited(FolderEvent folderEvent);
}
