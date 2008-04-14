package de.dal33t.powerfolder.event;

/**
 * Convinience adataper for <code>FolderListener</code>
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public abstract class FolderAdapter implements FolderListener {

    public void remoteContentsChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void scanResultCommited(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void fileChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void statisticsCalculated(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void syncProfileChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void filesDeleted(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public abstract boolean fireInEventDispathThread();
}
