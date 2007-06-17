package de.dal33t.powerfolder.event;

/** interface to implement to receive events from the FolderRepository */
public interface FolderRepositoryListener extends CoreListener {
    /**
     * Fired by the FolderRepository if a Folder is removed from the list of
     * "joined Folders"
     */
    public void folderRemoved(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository if a Folder is added to the list of "joined
     * Folders"
     */
    public void folderCreated(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository when the scans are started
     */
    public void maintenanceStarted(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository when the scans are finished
     */
    public void maintenanceFinished(FolderRepositoryEvent e);
}
