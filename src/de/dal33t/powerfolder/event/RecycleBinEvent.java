package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;

public class RecycleBinEvent extends EventObject {
    private FileInfo file;
    
        public RecycleBinEvent(RecycleBin recycleBin, FileInfo file) {
        super(recycleBin); 
        this.file = file;
    }
    
    public FileInfo getFile() {
        return file;
    }
    
}
