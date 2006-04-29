/* $Id: Upload.java,v 1.13 2006/01/30 00:51:18 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.io.File;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.util.Util;

/**
 * Simple class for a scheduled Upload
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class Upload extends Transfer {
    private Thread myThread;
    private boolean aborted;
    private boolean completed = false;

    /**
     * Constructs a new uploads, package protected, can only be called by
     * transfermanager
     * 
     * @param manager
     * @param file
     * @param member
     */
    Upload(TransferManager manager, Member member, RequestDownload dl) {
        super(manager, (dl == null) ? null : dl.file, member);
        if (dl == null) {
            throw new NullPointerException("Download request is null");
        }
        if (dl.file == null) {
            throw new NullPointerException("File is null");
        }
        setStartOffset(dl.startOffset);
        aborted = false;
    }

    /**
     * Starts the upload in a own thread using the give transfer manager
     * 
     * @param tm
     */
    synchronized void start() {
        if (isStarted()) {
            log().warn("Upload already started. " + this);
            return;
        }

        // Mark upload as started
        setStarted();

        myThread = new Thread("Upload " + getFile().getFilenameOnly() + " to "
            + getPartner().getNick())
        {
            public void run() {
                // perfom upload
                completed = getTransferManager().transfer(Upload.this);

                // Now inform transfer manager
                if (completed) {
                    getTransferManager().setCompleted(Upload.this);
                } else {
                    getTransferManager().setBroken(Upload.this);
                }
            }
        };

        myThread.setName("Upload " + this.toString());
        myThread.start();
    }

    /** @return true if transfer is completed */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Aborts this dl if currently transferrings
     */
    synchronized void abort() {
        log().warn("Upload aborted: " + this);
        aborted = true;
    }

    /**
     * Shuts down this upload if currently active
     */
    void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }
    }

    public boolean isAborted() {
        return aborted;
    }

    /**
     * Broken upload if client disconnected
     * 
     * @return
     */
    public boolean isBroken() {
        if (super.isBroken()) {
            return true;
        }

        if (!stillQueuedAtPartner()) {
            log().warn(
                "Upload broken because not enqued @ partner: queedAtPartner: "
                    + stillQueuedAtPartner()
                    + ", folder: "
                    + getFile()
                        .getFolder(getController().getFolderRepository())
                    + ", diskfile: "
                    + getFile().getDiskFile(
                        getController().getFolderRepository())
                    + ", last contime: " + getPartner().getLastConnectTime());
        }

        File diskFile = getFile().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null || !diskFile.exists()) {
            log().warn(
                "Upload broken because diskfile is not available, folder: "
                    + getFile()
                        .getFolder(getController().getFolderRepository())
                    + ", diskfile: " + diskFile + ", last contime: "
                    + getPartner().getLastConnectTime());
            return true;
        }

        return !stillQueuedAtPartner();
    }

    /*
     * General ****************************************************************
     */

    public int hashCode() {
        int hash = 0;
        if (getFile() != null) {
            hash += getFile().hashCode();
        }
        if (getPartner() != null) {
            hash += getPartner().hashCode();
        }
        return hash;
    }

    public boolean equals(Object o) {
        if (o instanceof Upload) {
            Upload other = (Upload) o;
            return Util.equals(this.getFile(), other.getFile())
                && Util.equals(this.getPartner(), other.getPartner());
        }

        return false;
    }

    public String toString() {
        String msg = getFile().toString() + " to '" + getPartner().getNick()
            + "'";
        if (getPartner().isOnLAN()) {
            msg += " (local-net)";
        }
        return msg;
    }
}