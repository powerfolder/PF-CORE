/* $Id: RequestDownload.java,v 1.3 2004/10/04 00:41:11 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Request to start download a file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class RequestDownload extends Message {
    private static final long serialVersionUID = 100L;

    public FileInfo file;
    public long startOffset;

    public RequestDownload() {
        // Serialisation constructor
    }

    /**
     * Start a complete new download
     */
    public RequestDownload(FileInfo file) {
        this(file, 0);
    }

    /**
     * Requests file download, starting at offset
     * 
     * @param file
     * @param startOffset
     */
    public RequestDownload(FileInfo file, long startOffset) {
        super();
        this.file = file;
        this.startOffset = startOffset;
    }

    public String toString() {
        return "Request to download: " + file + ", starting at " + startOffset;
    }
}