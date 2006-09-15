package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;

public class FileNameProblemEvent extends EventObject {
    private ScanResult scanResult;
    public FileNameProblemEvent(Folder folder, ScanResult scanResult) {
        super(folder);
        this.scanResult = scanResult;
    }
    
    public ScanResult getScanResult() {
        return scanResult;
    }  
    
    public Folder getFolder() {
        return (Folder)getSource();
    }
}
