package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;

/** Event fired if something changed in The PowerFolder Recycle Bin */
public class RecycleBinEvent extends EventObject {
    /** The file that was moved from or to the recycle bin */
    private FileInfo file;

    /**
     * Create a recycle Bin Event
     * 
     * @param recycleBin
     *            the source of the event
     * @param file
     *            the file that was moved from or to the recycle bin
     */
    public RecycleBinEvent(RecycleBin recycleBin, FileInfo file) {
        super(recycleBin);
        this.file = file;
    }

    /**
     * the file that was moved from or to the recycle bin
     * 
     * @return the file that was moved from or to the recycle bin
     */
    public FileInfo getFile() {
        return file;
    }

}
