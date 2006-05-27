package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Message to indicate that the download should be aborted
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class AbortDownload extends Message {
    private static final long serialVersionUID = 100L;

    public FileInfo file;

    public AbortDownload(FileInfo file) {
        this.file = file;
    }

    public String toString() {
        return "Abort download of: " + file;
    }
}