/* $Id: Download.java,v 1.30 2006/04/30 14:24:18 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.io.*;
import java.util.Date;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.util.Util;

/**
 * Download class, containing file and member.<BR>
 * Serializable for remembering completed Downloads in DownLoadTableModel.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class Download extends Transfer {
    private static final long serialVersionUID = 100L;

    private Date lastTouch;
    private boolean automatic;
    private boolean queued;
    private boolean completed;
    private boolean tempFileError;

    /** for serialisation */
    public Download() {

    }

    /**
     * Constuctor for download, package protected, can only be created by
     * transfer manager
     * <p>
     * Downloads start in pending state. Move to requested by calling
     * <code>request(Member)</code>
     */
    Download(TransferManager tm, FileInfo file, boolean automatic) {
        super(tm, file, null);
        // from can be null
        this.lastTouch = new Date();
        this.automatic = automatic;
        this.queued = false;
        this.completed = false;
        this.tempFileError = false;

        File tempFile = getTempFile();
        if (tempFile != null && tempFile.exists()) {
            String reason = "";
            // Compare with global file date precision, because of
            // different precisions on different filesystems (e.g. FAT32 only
            // supports second near values)
            if (file.getSize() > tempFile.length()
                && Util.convertToGlobalPrecision(file.getModifiedDate()
                    .getTime()) == Util.convertToGlobalPrecision(tempFile
                    .lastModified()))
            {
                // Set offset only if file matches exactly
                setStartOffset(tempFile.length());
            } else {
                if (file.getModifiedDate().getTime() != tempFile.lastModified())
                {
                    reason = ": Modified date of tempfile ("
                        + new Date(Util.convertToGlobalPrecision(tempFile
                            .lastModified()))
                        + ") does not match with file ("
                        + new Date(Util.convertToGlobalPrecision(file
                            .getModifiedDate().getTime())) + ")";
                }
                // Otherwise delete tempfile an start at 0
                tempFile.delete();
                setStartOffset(0);
            }
            log().verbose(
                "Tempfile exists for " + file + ", tempFile: " + tempFile
                    + ", " + (tempFile.exists() ? "using it" : "removed") + " "
                    + reason);
        }

        if (file.getSize() == 0) {
            // Null files are directly completed
            log().verbose("Completing 0 size file: " + file);
            completed = true;
            try {
                getTempFile().createNewFile();
            } catch (IOException e) {
                log().error(
                    "Unable to complete download, tempfile error. " + this);
                log().verbose(e);
                tempFileError = true;
            }
            finish();
        }
    }
    
    /**
     * Re-initalized the Transfer with the TransferManager. Use this only if you
     * are know what you are doing .
     * 
     * @param transferManager
     */
    public void init(TransferManager aTransferManager) {
        super.init(aTransferManager);
        queued = false;
    }

    /**
     * Answers if this download was automatically requested
     * 
     * @return
     */
    public boolean isRequestedAutomatic() {
        return automatic;
    }

    /**
     * Adds a chunk to the download
     * 
     * @param chunk
     */
    public synchronized void addChunk(FileChunk chunk) {
        if (chunk == null) {
            return;
        }

        if (!isStarted()) {
            // donwload begins to start
            setStarted();
        }
        lastTouch.setTime(System.currentTimeMillis());

        if (super.isBroken()) {
            return;
        }

        // check tempfile
        File tempFile = getTempFile();

        // create subdirs
        File subdirs = tempFile.getParentFile();
        if (!subdirs.exists()) {
            //TODO check if works else give warning because of invalid directory name
            // and move to blacklist
            subdirs.mkdirs();
            
            log().verbose("Subdirectory created: " + subdirs);
        }

        if (tempFile.exists() && chunk.offset == 0) {
            // download started from offset 0 new, remove file,
            // "erase and rewind" ;)
            if (!tempFile.delete()) {
                log().error(
                    "Unable to removed broken tempfile for download: "
                        + tempFile.getAbsolutePath());
                tempFileError = true;
                return;
            }
        }

        if (!tempFile.exists()) {
            try {
                // TODO check if works else give warning because of invalid filename or diskfull?
                // and move to blacklist
                tempFile.createNewFile();
            } catch (IOException e) {
                log().error(
                    "Unable to create/open tempfile for donwload: "
                        + tempFile.getAbsolutePath() + ". " + e.getMessage());
                tempFileError = true;
                return;
            }
        }

        // log().warn("Tempfile exists ? " + tempFile.exists() + ", " +
        // tempFile.getAbsolutePath());

        if (!tempFile.canWrite()) {
            log().error(
                "Unable to write to tempfile for donwload: "
                    + tempFile.getAbsolutePath());
            tempFileError = true;
            return;
        }

        // check chunk validity
        if (chunk.offset < 0 || chunk.offset > getFile().getSize()
            || chunk.data == null
            || (chunk.data.length + chunk.offset > getFile().getSize())
            || chunk.offset != tempFile.length())
        {

            String reason = "unknown";

            if (chunk.offset < 0 || chunk.offset > getFile().getSize()) {
                reason = "Illegal offset " + chunk.offset;
            }

            if (chunk.data == null) {
                reason = "Chunk data null";
            }

            if (chunk.data.length + chunk.offset > getFile().getSize()) {
                reason = "Chunk exceeds filesize";
            }

            if (chunk.offset != tempFile.length()) {
                reason = "Offset does not matches the current tempfile size. offset: "
                    + chunk.offset + ", filesize: " + tempFile.length();
            }

            log().error(
                "Received illegal chunk. " + chunk + ". Reason: " + reason);
            // Abort dl
            abort();
            return;
        }

        try {
            // add bytes to transferred status
            getCounter().chunkTransferred(chunk);
            // FIXME: Parse offset/not expect linar download
            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
                tempFile, true));
            fOut.write(chunk.data);
            fOut.close();
            // Set lastmodified date of file info
            /*
             * log().warn( "Setting lastmodified of tempfile for: " +
             * getFile().toDetailString());
             */
            tempFile.setLastModified(getFile().getModifiedDate().getTime());
            log().verbose(
                "Wrote " + chunk.data.length + " bytes to tempfile "
                    + tempFile.getAbsolutePath());
        } catch (IOException e) {
            // TODO: Disk full warning ?
            log().error(
                "Error while writing to tempfile for donwload: "
                    + tempFile.getAbsolutePath() + ". " + e.getMessage());
            log().verbose(e);
            tempFileError = true;
            return;
        }

        // FIXME: currently the trigger to stop dl is
        // the arrival of a chunk which matches exactly to
        // the last chunk of the file
        if (!completed) {
            completed = chunk.data.length + chunk.offset == getFile().getSize();
            if (completed) {
                // Finish download
                finish();
            }
        }
    }

    /**
     * Finishes the download
     */
    private void finish() {
        log().debug("Download completed: " + this);

        // Inform transfer manager
        getTransferManager().setCompleted(this);
    }

    /**
     * Returns the tempfile for this download
     * 
     * @return
     */
    File getTempFile() {
        File diskFile = getFile().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        File tempFile = new File(diskFile.getParentFile(), "(incomplete) "
            + diskFile.getName());
        return tempFile;
    }

    /**
     * Requestst the download from the remote member resumes download, if
     * tempfile exists
     */
    void request(Member from) {
        if (from == null) {
            throw new NullPointerException("From is null");
        }
        // Set partner
        setPartner(from);
        getPartner().sendMessageAsynchron(
            new RequestDownload(getFile(), getStartOffset()), null);
    }

    /**
     * Requests to abort this dl
     */
    public void abort() {
        getController().getTransferManager().abortDownload(this);
    }

    /**
     * Requests to abort this dl and removes any tempfile
     */
    public void abortAndCleanup() {
        // Abort dl
        abort();

        // delete tempfile
        File tempFile = getTempFile();
        tempFile.delete();
    }
    
    /**
     * Answers if this transfer has already started
     * 
     * @return
     */
    @Override
    public boolean isStarted() {
        return !isPending() && super.isStarted();
    }

    /**
     * This download is queued at the remote side
     */
    public void setQueued() {
        log().verbose("DL queued by remote side: " + this);
        queued = true;
    }

    /**
     * Sets the dl as started, removes placeholder file if exists
     * 
     * @see de.dal33t.powerfolder.transfer.Transfer#setStarted()
     */
    protected void setStarted() {
        super.setStarted();

        // Now try to delete placeholder file if exists
        File diskFile = getFile().getDiskFile(
            getController().getFolderRepository());
        if (diskFile != null) {
            File placeHolderFile = Util.getPlaceHolderFile(diskFile);
            if (placeHolderFile.exists()) {
                log().verbose("Removing placeholder file for " + getFile());
                placeHolderFile.delete();
            }
        }
    }
    
    /**
     * Answers if this is a pending download
     */
    public boolean isPending() {
        if (isCompleted()) {
            // not pending when completed
            return false;
        }
        return getPartner() == null || isBroken();
    }

    /**
     * Answers if this download is broken. timed out or has no connection
     * anymore or (on blacklist in folder and isRequestedAutomatic)
     * 
     * @return
     */
    public boolean isBroken() {
        if (super.isBroken()) {
            return true;
        }
        // timeout is, when dl is not enqued at remote side,
        // and has timeout
        boolean timedOut = ((System.currentTimeMillis() - TransferManager.DOWNLOAD_REQUEST_TIMEOUT_MS) > lastTouch
            .getTime())
            && !this.queued;
           
        boolean isQueuedAtPartner = true;
        if (!timedOut || tempFileError) {
            isQueuedAtPartner = stillQueuedAtPartner();
        }

        //check blacklist
        Folder folder = getController().getFolderRepository().getFolder(getFile().getFolderInfo());        
        boolean onBlacklist = folder.isInBlacklist(getFile());  
        
        // timeout or peer has dl not queued or problem with our tempfile
        // or partner is not longer a friend of ours or (onBlackList and automatic)
        return timedOut || !isQueuedAtPartner || tempFileError || (onBlacklist && isRequestedAutomatic());
    }

    /**
     * @return
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * @return
     */
    public boolean isQueued() {
        return !isBroken() && queued;
    }

    /*
     * General
     */

    public int hashCode() {
        int hash = 37;
        if (getFile() != null) {
            hash += getFile().hashCode();
        }
        return hash;
    }

    public boolean equals(Object o) {
        if (o instanceof Download) {
            Download other = (Download) o;
            return Util.equals(this.getFile(), other.getFile());
        }

        return false;
    }

    public String toString() {
        String msg = getFile().toDetailString();
        if (getPartner() != null) {
            msg += " from '" + getPartner().getNick() + "'";
            if (getPartner().isOnLAN()) {
                msg += " (local-net)";
            }
        } else {
            msg += " (pending)";
        }
        return msg;
    }
}