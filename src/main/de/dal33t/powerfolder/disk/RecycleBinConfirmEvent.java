package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.EventObject;

public class RecycleBinConfirmEvent extends EventObject{
    File targetFile;
    File sourceFile;
    RecycleBinConfirmEvent(RecycleBin source, File sourceFile, File targetFile) {
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