package de.dal33t.powerfolder.event;

import java.io.File;
import java.util.EventObject;

import de.dal33t.powerfolder.disk.RecycleBin;

public class RecycleBinConfirmEvent extends EventObject{
    private File targetFile;
    private File sourceFile;
    public RecycleBinConfirmEvent(RecycleBin source, File sourceFile, File targetFile) {
        super(source);
        this.targetFile = targetFile;
        this.sourceFile = sourceFile;
    }
    
    public File getSourceFile() {
        return sourceFile;
        
    }
    
    public File getTargetFile() {
        return targetFile;
        
    }
}