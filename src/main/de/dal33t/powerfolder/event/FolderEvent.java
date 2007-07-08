package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;

public class FolderEvent extends EventObject {
    private ScanResult scanResult;

    public FolderEvent(Folder source) {
        super(source);
    }
    
    public FolderEvent(Folder source, ScanResult sr) {
        super(source);
        this.scanResult = sr;
    }

    public Folder getFolder() {
        return (Folder) getSource();
    }
    
    public ScanResult getScanResult() {
        return scanResult;
    }
}
