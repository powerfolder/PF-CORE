/* $Id: Upload.java,v 1.13 2006/01/30 00:51:18 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.ReplyFilePartsRecord;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.StartUpload;
import de.dal33t.powerfolder.message.StopUpload;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * Simple class for a scheduled Upload
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
@SuppressWarnings("serial")
public class Upload extends Transfer {
    public final static int MAX_REQUESTS_QUEUED = 20;

    private boolean aborted;
    private transient Queue<Message> pendingRequests = new LinkedList<Message>();

    protected transient RandomAccessFile raf;

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

    private void enqueueMessage(Message m) {
        try {
            synchronized (pendingRequests) {
                if (pendingRequests.size() >= MAX_REQUESTS_QUEUED) {
                    throw new TransferException("Too many requests");
                }
                pendingRequests.add(m);
                pendingRequests.notifyAll();
            }
        } catch (TransferException e) {
            log().error(e);
            getTransferManager().setBroken(this,
                TransferProblem.TRANSFER_EXCEPTION, e.getMessage());
        }
    }

    public void enqueuePartRequest(RequestPart pr) {
        Reject.ifNull(pr, "Message is null");
        
        // If the download was aborted
        if (aborted || !isStarted()) {
            return;
        }

        if (!Util.usePartRequests(getController(), this.getPartner())) {
            log().warn("Downloader sent a PartRequest (Protocol violation). Aborting.");
            getTransferManager().setBroken(this, TransferProblem.TRANSFER_EXCEPTION);
            return;
        }
        // Requests for different files on the same transfer connection are not
        // supported currently
        if (!pr.getFile().isCompletelyIdentical(getFile())
            || pr.getRange().getLength() > TransferManager.MAX_CHUNK_SIZE
            || pr.getRange().getLength() <= 0)
        {
            log().error("Received invalid part request!");
            getTransferManager().setBroken(this, TransferProblem.INVALID_PART);
            return;
        }
        transferState.setProgress(pr.getProgress());
        enqueueMessage(pr);
    }

    public void receivedFilePartsRecordRequest(RequestFilePartsRecord r) {
        Reject.ifNull(r, "Record is null");
        
        log().info("Received request for a parts record.");
        // If the download was aborted
        if (aborted || !isStarted()) {
            return;
        }
        if (getFile().getSize() < Constants.MIN_SIZE_FOR_PARTTRANSFERS) {
            log().warn("Remote side requested invalid PartsRecordRequest!");

            getTransferManager().setBroken(this,
                TransferProblem.GENERAL_EXCEPTION,
                "Remote side requested invalid PartsRecordRequest!");
            return;
        }
        enqueueMessage(r);
    }

    public void stopUploadRequest(StopUpload su) {
        Reject.ifNull(su, "Message is null");
        
        synchronized (pendingRequests) {
            pendingRequests.clear();
            pendingRequests.add(su);
            pendingRequests.notifyAll();
        }
    }

    public void cancelPartRequest(RequestPart pr) {
        Reject.ifNull(pr, "Message is null");
        
        synchronized (pendingRequests) {
            pendingRequests.remove(pr);
            pendingRequests.notifyAll();
        }
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

        Runnable uploadPerfomer = new Runnable() {
            public void run() {
                try {
                    try {
                        raf = new RandomAccessFile(getFile()
                            .getDiskFile(
                                getController().getFolderRepository()),
                            "r");
                    } catch (FileNotFoundException e) {
                        throw new TransferException(e);
                    }
                    
                    
                    // If our partner supports requests, let him request. This is required for swarming to work.
                    if (Util.usePartRequests(getController(), getPartner())) {

                        if (logVerbose) {
                            log().verbose(
                                "Both clients support partial transfers!");
                        }
                        try {
                            getPartner()
                                .sendMessage(new StartUpload(getFile()));
                        } catch (ConnectionException e) {
                            throw new TransferException(e);
                        }
                        if (waitForRequests()) {
                            log().info("Checking for parts request.");
    
                            // Check if the first request is for a FilePartsRecord
                            if (checkForFilePartsRecordRequest()) {
                                transferState
                                    .setState(TransferState.REMOTEMATCHING);
                                log().verbose("Waiting for initial part requests!");
                                waitForRequests();
                            }
                            log().info("Upload started " + this);
                            long startTime = System.currentTimeMillis();
    
                            // FIXME: It shouldn't be possible to loop endlessly
                            // This fixme has to solved somewhere else partly since
                            // it's like:
                            // "How long do we allow to upload to some party" -
                            // which can't be decided here.
                            while (sendPart()) {
                            }
                            long took = System.currentTimeMillis() - startTime;
                            getTransferManager().logTransfer(false, took,
                                getFile(), getPartner());
                        }
                    } else {
                        transferState.setState(TransferState.UPLOADING);
                        sendChunks();
                    }
                    getTransferManager().setCompleted(Upload.this);
                } catch (TransferException e) {
                    // log().warn("Upload broken: " + Upload.this, e);
                    getTransferManager().setBroken(Upload.this,
                        TransferProblem.TRANSFER_EXCEPTION, e.getMessage());
                } finally {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        log().error(e);
                    }
                }
            }

            public String toString() {
                return "Upload " + getFile().getFilenameOnly() + " to "
                    + getPartner().getNick();
            }
        };

        // Perfom upload in threadpool
        getTransferManager().perfomUpload(uploadPerfomer);
    }

    protected boolean checkForFilePartsRecordRequest() throws TransferException
    {
        RequestFilePartsRecord r = null;
        synchronized (pendingRequests) {
            if (pendingRequests.isEmpty()) {
                log().warn("Cancelled message too fast");
                return false;
            }
            if (pendingRequests.peek() instanceof RequestFilePartsRecord) {
                r = (RequestFilePartsRecord) pendingRequests.remove();
            }
        }
        if (r == null) {
            return false;
        }
        final FileInfo fi = r.getFile();
        FilePartsRecord fpr;
        try {
            transferState.setState(TransferState.FILEHASHING);
            fpr = fi.getFilePartsRecord(getController().getFolderRepository(),
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        transferState.setProgress(((double) (Long) evt
                            .getNewValue())
                            / fi.getSize());
                    }
                });
            getPartner().sendMessagesAsynchron(
                new ReplyFilePartsRecord(fi, fpr));
            transferState.setState(TransferState.UPLOADING);
        } catch (FileNotFoundException e) {
            log().error(e);
            getTransferManager().setBroken(Upload.this,
                TransferProblem.FILE_NOT_FOUND_EXCEPTION, e.getMessage());
        } catch (IOException e) {
            log().error(e);
            getTransferManager().setBroken(Upload.this,
                TransferProblem.IO_EXCEPTION, e.getMessage());
        }
        return true;
    }

    /**
     * Sends one requested part.
     * 
     * @return false if the upload should stop, true otherwise
     * @throws TransferException
     */
    private boolean sendPart() throws TransferException {
        if (getPartner() == null) {
            throw new NullPointerException("Upload member is null");
        }
        if (getFile() == null) {
            throw new NullPointerException("Upload file is null");
        }

        if (isAborted()) {
            throw new TransferException("Upload aborted: " + this);
        }
        if (isBroken()) {
            throw new TransferException("Upload broken: " + this);
        }
        transferState.setState(TransferState.UPLOADING);
        RequestPart pr = null;
        synchronized (pendingRequests) {
            while (pendingRequests.isEmpty() && !isBroken() && !isAborted()) {
                try {
                    pendingRequests.wait(Constants.UPLOAD_PART_REQUEST_TIMEOUT);
                } catch (InterruptedException e) {
                    log().error(e);
                    throw new TransferException(e);
                }
            }
            // If it's still empty we either got a StopUpload, or we got
            // interrupted or it got aborted in which case we just drop out.
            // Also the timeout could be the cause in which case this also is
            // the end of the upload.
            if (pendingRequests.isEmpty()) {
                return false;
            }
            if (pendingRequests.peek() instanceof StopUpload) {
                pendingRequests.remove();
                return false;
            }
            pr = (RequestPart) pendingRequests.remove();
            
            if (isBroken()) {
                return false;
            }
        }
        try {

            // TODO: Maybe cache the file
            File f = pr.getFile().getDiskFile(
                getController().getFolderRepository());

            byte[] data = new byte[(int) pr.getRange().getLength()];
            raf.seek(pr.getRange().getStart());
            int pos = 0;
            while (pos < data.length) {
                int read = raf.read(data, pos, data.length - pos);
                if (read < 0) {
                    log().warn("Requested part exceeds filesize!");
                    throw new TransferException(
                        "Requested part exceeds filesize!");
                }
                pos += read;
            }
            FileChunk chunk = new FileChunk(pr.getFile(), pr.getRange()
                .getStart(), data);
            getPartner().sendMessage(chunk);
            getCounter().chunkTransferred(chunk);
            getTransferManager().getUploadCounter().chunkTransferred(chunk);

            // FIXME: Below this check is done every 15 seconds - maybe restrict
            // this test here too
            checkLastModificationDate(pr.getFile(), f);

        } catch (FileNotFoundException e) {
            log().error(e);
            throw new TransferException(e);
        } catch (IOException e) {
            log().error(e);
            throw new TransferException(e);
        } catch (ConnectionException e) {
            log().warn("Connectiopn problem while uploading", e);
            throw new TransferException(e);
        }
        return true;
    }

    protected boolean waitForRequests() {
        if (isBroken() || aborted) {
            return false;
        }
        synchronized (pendingRequests) {
            if (!pendingRequests.isEmpty()) {
                return true;
            }
            try {
                while (pendingRequests.isEmpty() && !isBroken() && !aborted) {
                    pendingRequests.wait();
                }
            } catch (InterruptedException e) {
                log().error(e);
            }
        }
        return !pendingRequests.isEmpty();
    }

    /**
     * Aborts this dl if currently transferrings
     */
    synchronized void abort() {
        log().verbose("Upload aborted: " + this);
        aborted = true;

        stopUploads();
    }

    /**
     * Shuts down this upload if currently active
     */
    void shutdown() {
        super.shutdown();
        // "Forget" all requests from the client
        stopUploads();
    }

    private void stopUploads() {
        synchronized (pendingRequests) {
            pendingRequests.clear();
            // Notify any remaining waiter
            pendingRequests.notifyAll();
        }
    }

    public boolean isAborted() {
        return aborted;
    }

    /**
     * @return if this upload is broken
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
        String msg = getFile().toDetailString() + " to '" + getPartner().getNick()
            + "'";
        if (getPartner().isOnLAN()) {
            msg += " (local-net)";
        }
        return msg;
    }

    /**
     * Transfers a file to a member.
     * 
     * @throws TransferException
     *             if something unexepected occoured.
     */
    private void sendChunks() throws TransferException {
        assert getPartner() != null : "Upload member is null";
        assert getFile() != null : "Upload file is null";

        if (isBroken()) {
            throw new TransferException("Upload broken: " + this);
        }

        Member member = getPartner();
        Date lastFileCheck;
        FileInfo theFile = getFile();

        // connection still alive ?
        if (!member.isConnected()) {
            throw new TransferException("Upload broken, member disconnected: "
                + this);
        }

        // TODO: check if member is in folder of file
        File f = theFile.getDiskFile(getController().getFolderRepository());
        if (f == null) {
            throw new TransferException(this + ": Myself not longer on "
                + theFile.getFolderInfo());
        }
        if (!f.exists()) {
            throw new TransferException(theFile
                + " not found, download canceled. '" + f.getAbsolutePath()
                + "'");
        }
        if (!f.canRead()) {
            throw new TransferException("Cannot read file. '"
                + f.getAbsolutePath() + "'");
        }
        checkLastModificationDate(theFile, f);
        lastFileCheck = new Date();

        log().info(
            "Upload started " + this + " starting at " + getStartOffset());
        long startTime = System.currentTimeMillis();

        try {
            if (f.length() == 0) {
                // Handle files with zero size.
                // Just send one empty FileChunk
                FileChunk chunk = new FileChunk(theFile, 0, new byte[]{});
                member.sendMessage(chunk);
            } else {
                // Handle usual files. size > 0

                // Chunk size
                int chunkSize = member.isOnLAN()
                    ? TransferManager.MAX_CHUNK_SIZE
                    : (int) getTransferManager().getAllowedUploadCPSForWAN();
                if (chunkSize == 0) {
                    chunkSize = TransferManager.MAX_CHUNK_SIZE;
                }
                // Keep care of maximum chunk size
                chunkSize = Math.min(chunkSize, TransferManager.MAX_CHUNK_SIZE);

                if (chunkSize <= 0) {
                    log().error("Illegal chunk size: " + chunkSize);
                }

                // InputStream fin = new BufferedInputStream(
                // new FileInputStream(f));
                // starting at offset
                // fin.skip(upload.getStartOffset());
                long offset = getStartOffset();

                byte[] buffer = new byte[chunkSize];
                int read;
                do {
                    if (isAborted()) {
                        throw new TransferException("Upload aborted: " + this);
                    }
                    if (isBroken()) {
                        throw new TransferException("Upload broken: " + this);
                    }

                    raf.seek(offset);
                    read = raf.read(buffer);
                    // read = fin.read(buffer);
                    if (read < 0) {
                        // stop ul
                        break;
                    }

                    byte[] data;

                    if (read == buffer.length) {
                        // Take buffer unchanged as data
                        data = buffer;
                    } else {
                        // We have read less bytes then our buffer, copy
                        // data
                        data = new byte[read];
                        System.arraycopy(buffer, 0, data, 0, read);
                    }

                    FileChunk chunk = new FileChunk(theFile, offset, data);
                    offset += data.length;

                    member.sendMessage(chunk);
                    getCounter().chunkTransferred(chunk);
                    getTransferManager().getUploadCounter().chunkTransferred(
                        chunk);
                    transferState.setProgress(getCounter()
                        .calculateCompletionPercentage() / 100);

                    // Check file every 15 seconds
                    if (lastFileCheck.before(new Date(System
                        .currentTimeMillis() - 15 * 1000)))
                    {
                        if (logVerbose) {
                            log().verbose(
                                "Checking uploading file: "
                                    + theFile.toDetailString());
                        }
                        checkLastModificationDate(theFile, f);
                        lastFileCheck = new Date();
                    }

                    if (logVerbose) {
                        // log().verbose(
                        // "Chunk, "
                        // + Format.NUMBER_FORMATS.format(chunkSize)
                        // + " bytes, uploaded in "
                        // + (System.currentTimeMillis() - start)
                        // + "ms to " + member.getNick());
                    }
                } while (read > 0);

                // fin.close();
            }

            long took = System.currentTimeMillis() - startTime;
            getTransferManager().logTransfer(false, took, theFile, member);

            // upload completed successfully
        } catch (FileNotFoundException e) {
            throw new TransferException("File not found to upload. " + theFile,
                e);
        } catch (IOException e) {
            throw new TransferException("Problem reading file. " + theFile, e);
        } catch (ConnectionException e) {
            throw new TransferException(
                "Connection problem to " + getPartner(), e);
        }
    }

    private void checkLastModificationDate(FileInfo theFile, File f)
        throws TransferException
    {
        assert theFile != null;
        assert f != null;
        
        boolean lastModificationDataMismatch = !Util
            .equalsFileDateCrossPlattform(f.lastModified(), theFile
                .getModifiedDate().getTime());
        if (lastModificationDataMismatch) {
            Folder folder = theFile.getFolder(getController()
                .getFolderRepository());
            folder.recommendScanOnNextMaintenance();
            throw new TransferException("Last modification date mismatch. '"
                + f.getAbsolutePath()
                + "': expected "
                + Convert.convertToGlobalPrecision(theFile.getModifiedDate()
                    .getTime()) + ", actual "
                + Convert.convertToGlobalPrecision(f.lastModified()));
        }
    }
}