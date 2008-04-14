package de.dal33t.powerfolder.event;

/**
 * Fired by Folder if the status of the Folder changes
 * <P>
 * TODO: Add events for local file deletion
 * <P>
 * TODO: Add event for folder db maitenance cleanup #798
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface FolderListener extends CoreListener {

    /**
     * The statistics gets calculated delayed after a folder changed or the
     * remote contents changed
     * 
     * @param folderEvent
     */
    void statisticsCalculated(FolderEvent folderEvent);

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

    /**
     * Fired when a single file has been changed. e.g. after scan or after
     * download.
     * 
     * @param folderEvent
     */
    void fileChanged(FolderEvent folderEvent);

    /**
     * Fired when files got physically deleted. e.g. through remote deletion
     * sync or user deleted files locally via GUI. DOES NOT get fired when scan
     * detects remove files.
     * 
     * @param folderEvent
     */
    void filesDeleted(FolderEvent folderEvent);
}
