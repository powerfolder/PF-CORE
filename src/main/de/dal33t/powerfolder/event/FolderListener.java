package de.dal33t.powerfolder.event;


public interface FolderListener extends CoreListener {
    public void statisticsCalculated(FolderEvent folderEvent);
    public void folderChanged(FolderEvent folderEvent);   
    public void syncProfileChanged(FolderEvent folderEvent);
    public void remoteContentsChanged(FolderEvent folderEvent);
}
