package de.dal33t.powerfolder.event;

public interface RecycleBinListener {
    public void fileAdded(RecycleBinEvent e);
    public void fileRemoved(RecycleBinEvent e);
}
