/* $Id: TransferStatus.java,v 1.7 2005/10/28 21:20:22 schaatser Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.Date;

import de.dal33t.powerfolder.util.Format;

/**
 * Information about peers transferstatus
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class TransferStatus extends Message {
    private static final long serialVersionUID = 100L;

    public Date time;

    public int activeUploads;
    public int queuedUploads;
    public int maxUploads;
    public double maxUploadCPS;
    public long currentUploadCPS;
    public long uploadedBytesTotal;

    public int activeDownloads;
    public int queuedDownloads;
    public int maxDownloads;
    public double maxDownloadCPS;
    public long currentDownloadCPS;
    public long downloadedBytesTotal;

    public TransferStatus() {
        time = new Date();
    }

    /**
     * Answers if there are avaible upload slots
     * 
     * @return
     */
    public int getFreeUploadSlots() {
        return maxUploads - activeUploads;
    }

    /**
     * Answers the available upload bandwidth.
     * <p>
     * TODO: Calculate this with the "real" maximum upload cps
     * 
     * @return
     */
    public long getAvailbleUploadCPS() {
        return Math.max((long) maxUploadCPS - currentUploadCPS, 0);
    }

    public String toString() {
        return "Transfer status: DLs ( " + activeDownloads + " / "
            + queuedDownloads + " ) (" + Format.formatBytes(currentDownloadCPS)
            + "/s), ULs ( " + activeUploads + " / " + queuedUploads + " ) ("
            + Format.formatBytes(currentUploadCPS) + "/s, max: "
            + Format.formatBytes((long) maxUploadCPS) + "/s)";
    }
}