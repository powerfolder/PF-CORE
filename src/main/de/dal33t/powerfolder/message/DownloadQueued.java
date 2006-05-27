/* $Id: DownloadQueued.java,v 1.3 2004/10/04 00:41:11 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Tells the peer, that the download of file was enqueued
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class DownloadQueued extends Message {
    private static final long serialVersionUID = 100L;

    public FileInfo file;

    public DownloadQueued() {
        // Serialisation constructor
    }

    /**
     * 
     */
    public DownloadQueued(FileInfo file) {
        super();
        this.file = file;
    }

    public String toString() {
        return "Download queued: " + file;
    }
}