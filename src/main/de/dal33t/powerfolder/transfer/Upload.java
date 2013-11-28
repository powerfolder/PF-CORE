/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: Upload.java 18906 2012-05-17 02:21:56Z sprajc $
 */
package de.dal33t.powerfolder.transfer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.FileChunkExt;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.ReplyFilePartsRecord;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.StartUpload;
import de.dal33t.powerfolder.message.StartUploadExt;
import de.dal33t.powerfolder.message.StopUpload;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.ProgressListener;
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

    private boolean aborted;
    private transient Queue<Message> pendingRequests = new LinkedList<Message>();
    protected transient RandomAccessFile raf;
    protected transient InputStream in;
    private long inpos;
    private String debugState;

    /**
     * Constructs a new uploads, package protected, can only be called by
     * transfermanager
     * 
     * @param manager
     * @param member
     * @param dl
     */
    Upload(TransferManager manager, Member member, RequestDownload dl) {
        super(manager, (FileInfo) ((dl == null) ? null : dl.file), member);
        if (dl == null) {
            throw new NullPointerException("Download request is null");
        }
        if (dl.file == null) {
            throw new NullPointerException("File is null");
        }
        setStartOffset(dl.startOffset);
        aborted = false;
        debugState = "initialized";
    }

    private void enqueueMessage(Message m) {
        try {
            synchronized (pendingRequests) {
                if (pendingRequests.size() >= getTransferManager()
                    .getMaxRequestsQueued() * 5)
                {
                    throw new TransferException("Too many requests queued: "
                        + pendingRequests.size() + ", maximum: "
                        + getTransferManager().getMaxRequestsQueued() * 5);
                }
                pendingRequests.add(m);
                pendingRequests.notifyAll();
            }
        } catch (TransferException e) {
            logSevere("TransferException", e);
            getTransferManager().uploadBroken(this,
                TransferProblem.TRANSFER_EXCEPTION, e.getMessage());
        }
    }

    public void enqueuePartRequest(RequestPart pr) {
        Reject.ifNull(pr, "Message is null");

        // If the download was aborted
        if (aborted || !isStarted()) {
            return;
        }

        // Requests for different files on the same transfer connection are not
        // supported currently
        if (!pr.getFile().isVersionDateAndSizeIdentical(getFile())
            || pr.getRange().getLength() <= 0)
        {
            logSevere("Received invalid part request!");
            getTransferManager().uploadBroken(this,
                TransferProblem.INVALID_PART);
            return;
        }
        if (pr.getRange().getLength() > getTransferManager()
            .getMaxFileChunkSize())
        {
            logWarning("Got request for a range bigger then my max filechunk size ("
                + pr.getRange()
                + "): "
                + pr.getRange().getLength()
                + " on "
                + getFile().toDetailString());
        }
        state.setProgress(pr.getProgress());
        enqueueMessage(pr);
    }

    public void receivedFilePartsRecordRequest(RequestFilePartsRecord r) {
        Reject.ifNull(r, "Record is null");

        if (isFine()) {
            logFine("Received request for a parts record: " + r);
        }
        // If the download was aborted
        if (aborted || !isStarted()) {
            return;
        }
        if (getFile().getSize() < Constants.MIN_SIZE_FOR_PARTTRANSFERS) {
            logWarning("Remote side requested invalid PartsRecordRequest!");

            getTransferManager().uploadBroken(this,
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

    /**
     * Starts the upload in a own thread using the give transfer manager
     */
    synchronized void start() {
        if (isStarted()) {
            logWarning("Upload already started. " + this);
            return;
        }
        if (isAborted() || isBroken()) {
            logWarning("Upload already broken/aborted. " + this);
            return;
        }

        debugState = "Starting";
        // Mark upload as started
        setStarted();

        Runnable uploadPerfomer = new Runnable() {
            public void run() {
                try {
                    if (isAborted() || isBroken()) {
                        throw new TransferException(
                            "Upload broken/aborted while starting. "
                                + Upload.this);
                    }
                    
                    debugState = "Opening file";
                    boolean useInputStream = true;
                    Folder f = getFile().getFolder(
                        getController().getFolderRepository());
                    if (f.getLocalBase().getFileSystem().provider().getScheme()
                        .equals("file"))
                    {
                        useInputStream = false;
                        try {
                            raf = new RandomAccessFile(getFile().getDiskFile(
                                getController().getFolderRepository()).toFile(), "r");
                        } catch (FileNotFoundException e) {
                            useInputStream = true;
                        }
                    }
                    if (useInputStream) {
                        try {
                            in = Files.newInputStream(getFile().getDiskFile(
                                getController().getFolderRepository()));
                            inpos = 0;
                        } catch (FileNotFoundException e) {
                            throw new TransferException(e);
                        } catch (IOException ioe) {
                            throw new TransferException(ioe);
                        }
                    }
                    if (isAborted() || isBroken()) {
                        throw new TransferException(
                            "Upload broken/aborted while starting. "
                                + Upload.this);
                    }

                    // If our partner supports requests, let him request. This
                    // is required for swarming to work.
                    if (isFiner()) {
                        logFiner("Both clients support partial transfers!");
                    }
                    debugState = "Sending StartUpload";
                    try {
                        if (getPartner().getProtocolVersion() >= 102) {
                            getPartner().sendMessage(
                                new StartUploadExt(getFile()));
                        } else {
                            getPartner()
                                .sendMessage(new StartUpload(getFile()));
                        }

                    } catch (ConnectionException e) {
                        throw new TransferException(e);
                    }
                    debugState = "Waiting for requests";
                    if (waitForRequests(Constants.UPLOAD_REQUEST_TIMEOUT)) {
                        if (isFiner()) {
                            logFiner("Checking for parts request.");
                        }

                        debugState = "Checking for FPR request.";

                        // Check if the first request is for a
                        // FilePartsRecord
                        if (checkForFilePartsRecordRequest()) {
                            debugState = "Waiting for remote matching";
                            state.setState(TransferState.REMOTEMATCHING);
                            logFiner("Waiting for initial part requests!");
                            waitForRequests(Constants.UPLOAD_REMOTEHASHING_PART_REQUEST_TIMEOUT);
                        }
                        debugState = "Starting to send parts";
                        if (isFine()) {
                            logFine("Started " + this);
                        }
                        long startTime = System.currentTimeMillis();

                        // FIXME: It shouldn't be possible to loop endlessly
                        // This fixme has to solved somewhere else partly
                        // since
                        // it's like:
                        // "How long do we allow to upload to some party" -
                        // which can't be decided here.
                        while (sendPart()) {
                        }
                        long took = System.currentTimeMillis() - startTime;
                        getTransferManager().logTransfer(false, took,
                            getFile(), getPartner());
                    }
                    closeIO();
                    if (!isBroken() && !aborted) {
                        getTransferManager().setCompleted(Upload.this);
                    }
                } catch (TransferException e) {
                    closeIO();
                    // Loggable.logWarningStatic(Upload.class, "Upload broken: "
                    // + Upload.this, e);
                    getTransferManager().uploadBroken(Upload.this,
                        TransferProblem.TRANSFER_EXCEPTION, e.getMessage());
                } finally {
                    closeIO();
                    debugState = "DONE";
                }
            }

            public String toString() {
                return "Upload " + getFile().toDetailString() + " to "
                    + getPartner().getNick();
            }
        };

        // Perfom upload in threadpool
        getTransferManager().perfomUpload(uploadPerfomer);
    }
    
    private synchronized void closeIO() {
        if (in != null) {
            try {
                in.close();
                in = null;
            } catch (IOException e) {
                logSevere("IOException", e);
            }
        }
        if (raf != null) {
            try {
                if (isFiner()) {
                    logFiner("Closing raf for "
                        + getFile().toDetailString());
                }
                raf.close();
                raf = null;
            } catch (IOException e) {
                logSevere("IOException", e);
            }
        }
    }

    protected boolean checkForFilePartsRecordRequest() throws TransferException
    {
        RequestFilePartsRecord r = null;
        synchronized (pendingRequests) {
            if (pendingRequests.isEmpty()) {
                logWarning("Cancelled message too fast");
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
        try {
            checkLastModificationDate(fi,
                fi.getDiskFile(getController().getFolderRepository()));
            FilePartsRecord fpr;
            state.setState(TransferState.FILEHASHING);
            fpr = getTransferManager().getFileRecordManager().retrieveRecord(
                fi, new ProgressListener() {
                    public void progressReached(double percentageReached) {
                        state.setProgress(percentageReached);
                    }

                });
            getPartner().sendMessagesAsynchron(
                new ReplyFilePartsRecord(fi, fpr));
            state.setState(TransferState.UPLOADING);
        } catch (FileNotFoundException e) {
            logWarning("FileNotFoundException", e);
            getTransferManager().uploadBroken(Upload.this,
                TransferProblem.FILE_NOT_FOUND_EXCEPTION, e.getMessage());
        } catch (IOException e) {
            logWarning("IOException", e);
            getTransferManager().uploadBroken(Upload.this,
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
        if (isAborted() || isBroken()) {
            return false;
        }
        state.setState(TransferState.UPLOADING);
        RequestPart pr = null;
        long waitTime = Constants.UPLOAD_REMOTEHASHING_PART_REQUEST_TIMEOUT;
        synchronized (pendingRequests) {
            while (pendingRequests.isEmpty() && !isBroken() && !isAborted()) {
                try {
                    pendingRequests.wait(waitTime);
                    waitTime = Constants.UPLOAD_REQUEST_TIMEOUT;
                } catch (InterruptedException e) {
                    logWarning("Interrupted on " + this + ". " + e);
                    logFiner(e);
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

            if (isAborted() || isBroken()) {
                return false;
            }
        }
        Path f = pr.getFile()
            .getDiskFile(getController().getFolderRepository());
        try {
            byte[] data = new byte[(int) pr.getRange().getLength()];
            long startOffset = pr.getRange().getStart();
            if (raf != null) {
                raf.seek(startOffset);
            } else if (in != null) {
                long skip = startOffset - inpos;
                if (skip >= 0) {
                    inpos += in.skip(skip);
                } else {
                    try {
                        try {
                            in.close();
                        } catch (Exception e) {
                            logWarning(e.toString());
                        }
                        in = Files.newInputStream(getFile().getDiskFile(
                            getController().getFolderRepository()));
                        in.skip(startOffset);
                        inpos = startOffset;
                    } catch (FileNotFoundException e) {
                        throw new TransferException(e);
                    }
                }

            }
            int pos = 0;
            while (pos < data.length) {
                int read;
                int readLen = data.length - pos;
                if (raf != null) {
                    read = raf.read(data, pos, readLen);
                } else if (in != null) {
                    read = in.read(data, pos, readLen);
                    inpos += read;
                } else {
                    throw new TransferException("I/O already closed");
                }
                if (read < 0) {
                    logWarning("Requested part exceeds filesize!");
                    throw new TransferException(
                        "Requested part exceeds filesize!");
                }
                pos += read;
            }
            FileChunk chunk;
            if (getPartner().getProtocolVersion() >= 104) {
                chunk = new FileChunkExt(pr.getFile(),
                    pr.getRange().getStart(), data);
            } else {
                chunk = new FileChunk(pr.getFile(), pr.getRange().getStart(),
                    data);
            }

            getPartner().sendMessage(chunk);
            getCounter().chunkTransferred(chunk);
            getTransferManager().getUploadCounter().chunkTransferred(chunk);

            // FIXME: Below this check is done every 15 seconds - maybe restrict
            // this test here too
            checkLastModificationDate(pr.getFile(), f);

        } catch (FileNotFoundException e) {
            logSevere("FileNotFoundException", e);
            throw new TransferException(e);
        } catch (IOException e) {
            logSevere("IOException", e);
            throw new TransferException(e);
        } catch (ConnectionException e) {
            logWarning("Connectiopn problem while uploading. " + e.toString());
            if (isFiner()) {
                logFiner("ConnectionException", e);
            }
            throw new TransferException(e);
        }
        return true;
    }

    protected boolean waitForRequests(long requestTimeoutMS) {
        if (isBroken() || aborted) {
            return false;
        }
        synchronized (pendingRequests) {
            if (!pendingRequests.isEmpty()) {
                return true;
            }
            try {
                pendingRequests.wait(requestTimeoutMS);
            } catch (InterruptedException e) {
                logFine("InterruptedException. " + e);
            }
        }
        return !isBroken() && !aborted && !pendingRequests.isEmpty();
    }

    /**
     * Aborts this dl if currently transferrings
     */
    synchronized void abort() {
        logFiner("Upload aborted: " + this);
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
        closeIO();
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
            logWarning("Upload broken because not enqued @ partner: queedAtPartner: "
                + stillQueuedAtPartner()
                + ", folder: "
                + getFile().getFolder(getController().getFolderRepository())
                + ", diskfile: "
                + getFile().getDiskFile(getController().getFolderRepository())
                + ", last contime: " + getPartner().getLastConnectTime());
        }

        Path diskFile = getFile().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null || Files.notExists(diskFile)) {
            logWarning("Upload broken because diskfile is not available, folder: "
                + getFile().getFolder(getController().getFolderRepository())
                + ", diskfile: "
                + diskFile
                + ", last contime: "
                + getPartner().getLastConnectTime());
            return true;
        }

        return !stillQueuedAtPartner();
    }

    /*
     * General
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
        String msg = "Upload: State: " + debugState + ", TransferState: "
            + state.getState() + " " + getFile().toDetailString() + " to '"
            + getPartner().getNick() + "'";
        if (getPartner().isOnLAN()) {
            msg += " (local-net)";
        }
        return msg;
    }

    private void checkLastModificationDate(FileInfo theFile, Path f)
        throws TransferException, IOException
    {
        assert theFile != null;
        assert f != null;

        boolean lastModificationDataMismatch = !DateUtil
            .equalsFileDateCrossPlattform(Files.getLastModifiedTime(f)
                .toMillis(), theFile.getModifiedDate().getTime());
        if (lastModificationDataMismatch) {
            Folder folder = theFile.getFolder(getController()
                .getFolderRepository());
            if (folder.scanAllowedNow()) {
                folder.scanChangedFile(theFile);
            }
            // folder.recommendScanOnNextMaintenance();
            throw new TransferException("Last modification date mismatch. '"
                + f.toAbsolutePath().toString()
                + "': expected "
                + Convert.convertToGlobalPrecision(theFile.getModifiedDate()
                    .getTime()) + ", actual "
                + Convert.convertToGlobalPrecision(Files.getLastModifiedTime(f).toMillis()));
        }
    }
}