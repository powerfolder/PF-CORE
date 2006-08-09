package de.dal33t.powerfolder.event;

/** implement when you want to receive events from the PowerFolder Recycle Bin. */
public interface RecycleBinListener {
    /** A file was added to the recycle bin */
    public void fileAdded(RecycleBinEvent e);

    /**
     * A file was removed from the recycle bin, this means either permanently
     * deleted, moved to the system recycle bin or restored.
     */
    public void fileRemoved(RecycleBinEvent e);

    /**
     * A file was updated in the recycle bin, this happens when the same file is
     * overwritten with a new file
     */
    public void fileUpdated(RecycleBinEvent e);
}
