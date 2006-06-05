/* $Id: TransferManagerEvent.java,v 1.3 2005/06/08 11:47:40 totmacherr Exp $
 */
package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;

/**
 * Event fired by TransferManager
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class TransferManagerEvent extends EventObject {
    private Download download;
    private FileInfo file;
    private Upload upload;

    public TransferManagerEvent(TransferManager source, Download download) {
        super(source);
        this.download = download;
        this.file = download.getFile();
    }

    public TransferManagerEvent(TransferManager source, Download download,
        FileInfo file)
    {
        super(source);
        this.download = download;
        this.file = file;
    }

    public TransferManagerEvent(TransferManager source, Upload upload) {
        super(source);
        this.upload = upload;
        this.file = upload.getFile();
    }

    public Download getDownload() {
        return download;
    }

    /**
     * Returns the affected file of upload/download event
     * 
     * @return
     */
    public FileInfo getFile() {
        if (file != null) {
            return file;
        } else if (download != null) {
            return download.getFile();
        } else if (upload != null) {
            return upload.getFile();
        }
        // Unable to resolve
        return null;
    }

    public Upload getUpload() {
        return upload;
    }
}