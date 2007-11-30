/* $Id: Download.java,v 1.30 2006/04/30 14:24:18 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.StopUpload;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState;
import de.dal33t.powerfolder.util.delta.MatchInfo;
import de.dal33t.powerfolder.util.delta.PartInfoMatcher;
import de.dal33t.powerfolder.util.delta.RollingAdler32;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

/**
 * Download class, containing file and member.<BR>
 * Serializable for remembering completed Downloads in DownLoadTableModel.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class Download extends Transfer {
    private static final long serialVersionUID = 100L;
    public final static int MAX_REQUESTS_QUEUED = 15;

    private Date lastTouch;
    private boolean automatic;
    private boolean queued;
    private boolean completed;
    private boolean tempFileError;

    private FilePartsState filePartsState;
    private FilePartsRecord remotePartRecord;
    private Queue<RequestPart> pendingRequests = new LinkedList<RequestPart>();

    private long initialAvailableCount = -1;

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

        invalidateFilePartsState();
        File tempFile = getTempFile();
        if (tempFile != null && tempFile.exists()) {
            String reason = "";
            // Compare with global file date precision, because of
            // different precisions on different filesystems (e.g. FAT32 only
            // supports second near values)
            if (file.getSize() > tempFile.length()
                && Util.equalsFileDateCrossPlattform(file.getModifiedDate()
                    .getTime(), tempFile.lastModified()))
            {
                // Set offset only if file matches exactly
                setStartOffset(tempFile.length());
            } else {
                if (!Util.equalsFileDateCrossPlattform(file.getModifiedDate()
                    .getTime(), tempFile.lastModified()))
                {
                    reason = ": Modified date of tempfile ("
                        + new Date(Convert.convertToGlobalPrecision(tempFile
                            .lastModified()))
                        + ") does not match with file ("
                        + new Date(Convert.convertToGlobalPrecision(file
                            .getModifiedDate().getTime())) + ")";
                }
                // Otherwise delete tempfile an start at 0
                tempFile.delete();
                setStartOffset(0);
            }
            log().warn(
                "Tempfile exists for " + file + ", tempFile: " + tempFile
                    + ", " + (tempFile.exists() ? "using it" : "removed") + " "
                    + reason);
        }
    }

    /**
     * Re-initalized the Transfer with the TransferManager. Use this only if you
     * are know what you are doing .
     * 
     * @param aTransferManager
     *            the transfermanager
     */
    public void init(TransferManager aTransferManager) {
        super.init(aTransferManager);
        queued = false;
    }

    /**
     * @return if this download was automatically requested
     */
    public boolean isRequestedAutomatic() {
        return automatic;
    }

    /**
     * Called when the partner supports part-transfers and is ready to upload
     */
    public void uploadStarted() {
        log().info(
            "Uploader supports partial transfers, sending record-request.");
        // Start by requesting a FilePartsRecord if minimum requirements are
        // fulfilled.
        if (getFile().getSize() >= Constants.MIN_SIZE_FOR_PARTTRANSFERS
            && getFile().diskFileExists(getController()))
        {
            transferState.setState(TransferState.FILERECORD_REQUEST);
            getPartner().sendMessagesAsynchron(
                new RequestFilePartsRecord(getFile()));
        } else {
            log().verbose(
                "Didn't send request: Minimum requirements not fulfilled!");
            getFilePartsState().setPartState(
                Range.getRangeByLength(0, getFilePartsState().getFileLength()),
                PartState.NEEDED);
            requestParts();
        }
    }

    public void receivedFilePartsRecord(final FilePartsRecord record) {
        log().info("Received parts record");
        this.remotePartRecord = record;
        getController().getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    transferState.setState(TransferState.MATCHING);
                    log().debug("Processing FilePartsRecord.");
                    PartInfoMatcher matcher = new PartInfoMatcher(
                        new RollingAdler32(record.getPartLength()),
                        MessageDigest.getInstance("SHA-256"));
                    File dfile = getFile().getDiskFile(
                        getController().getFolderRepository());
                    if (dfile != null && dfile.exists()) {
                        // Update state
                        final double dFileSize = dfile.length();
                        matcher.getProcessedBytes().addValueChangeListener(
                            new PropertyChangeListener() {
                                public void propertyChange(
                                    PropertyChangeEvent evt)
                                {
                                    transferState.setProgress((Long) evt
                                        .getNewValue()
                                        / dFileSize);
                                }
                            });

                        RandomAccessFile traf = null;
                        FileInputStream in = null;
                        try {
                            in = new FileInputStream(dfile);
                            // log().info("Trying to find matches in " +
                            // dfile.length() + " vs " +
                            // record.getFileLength());
                            List<MatchInfo> mis = matcher.matchParts(in, record
                                .getInfos());
                            // log().info("Found " + mis.size() + " matches
                            // which won't have to be downloaded.");
                            in.close();
                            traf = new RandomAccessFile(dfile, "rw");
                            if (raf == null) {
                                raf = new RandomAccessFile(getTempFile(), "rw");
                            }
                            long mInfoCount = 0, mInfoLength = mis.size();
                            transferState.setState(TransferState.COPYING);
                            for (MatchInfo m : mis) {
                                transferState.setProgress(((double) mInfoCount)
                                    / mInfoLength);
                                mInfoCount++;

                                traf.seek(m.getMatchedPosition());
                                long pos = m.getMatchedPart().getIndex()
                                    * record.getPartLength();
                                raf.seek(pos);
                                byte[] data = new byte[8192];
                                int read;
                                int rem = (int) Math.min(
                                    record.getPartLength(), record
                                        .getFileLength()
                                        - pos);
                                // Mark copied parts as already available
                                getFilePartsState().setPartState(
                                    Range.getRangeByLength(pos, rem),
                                    PartState.AVAILABLE);
                                // log().info("From " + m.getMatchedPosition() +
                                // " to " + pos);
                                while (rem > 0
                                    && (read = traf.read(data, 0, Math.min(rem,
                                        data.length))) > 0)
                                {
                                    raf.write(data, 0, read);
                                    rem -= read;
                                }
                            }
                        } finally {
                            // Close and remove pointer so the code below works
                            // as expected.
                            // raf.close();
                            // raf = null;
                            if (traf != null) {
                                traf.close();
                            }
                            if (in != null) {
                                in.close();
                            }
                        }
                    }
                    log().info("Starting to request parts - NOW");
                    if (getFilePartsState().isCompleted()) {
                        completed = true;
                        log().debug(
                            "Download completed (no change detected): " + this);
                        checkCompleted();
                    } else {
                        requestParts();
                    }
                } catch (NoSuchAlgorithmException e) {
                    log().error("SHA Digest not found. Fatal error", e);
                    throw new RuntimeException(
                        "SHA Digest not found. Fatal error", e);
                } catch (FileNotFoundException e) {
                    log().error(e);
                    getController().getTransferManager().setBroken(
                        Download.this,
                        TransferProblem.FILE_NOT_FOUND_EXCEPTION,
                        e.getMessage());
                } catch (IOException e) {
                    log().error(e);
                    getController().getTransferManager().setBroken(
                        Download.this, TransferProblem.IO_EXCEPTION,
                        e.getMessage());
                } finally {
                    log().debug("DONE - Processing FilePartsRecord.");
                }
            }
        });
    }

    protected synchronized void requestParts() {
        if (getState() != TransferState.DOWNLOADING) {
            transferState.setState(TransferState.DOWNLOADING);
            transferState.setProgress(0);
        }
        synchronized (pendingRequests) {
            if (pendingRequests.size() >= MAX_REQUESTS_QUEUED) {
                log().warn("Pending request queue shouldn't be full!");
                return;
            }
        }
        while (true) {
            Range range;
            RequestPart rp;
            synchronized (pendingRequests) {
                if (pendingRequests.size() >= MAX_REQUESTS_QUEUED) {
                    return;
                }
                FilePartsState state = getFilePartsState();
                range = state.findFirstPart(PartState.NEEDED);
                if (range == null) {
                    // File completed, or only pending requests left
                    return;
                }
                range = Range.getRangeByLength(range.getStart(), Math.min(
                    TransferManager.MAX_CHUNK_SIZE, range.getLength()));
                state.setPartState(range, PartState.PENDING);
                rp = new RequestPart(getFile(), range);
                pendingRequests.add(rp);
            }
            getPartner().sendMessagesAsynchron(rp);
        }
    }

    /**
     * Adds a chunk to the download
     * 
     * @param chunk
     * @return true if the chunk was successfully appended to the download file.
     */
    public synchronized boolean addChunk(FileChunk chunk) {
        Reject.ifNull(chunk, "Chunk is null");
        if (isBroken()) {
            return false;
        }
        if (!isStarted()) {
            // donwload begins to start
            setStarted();
        }
        lastTouch.setTime(System.currentTimeMillis());

        // check tempfile
        File tempFile = getTempFile();

        // create subdirs
        File subdirs = tempFile.getParentFile();
        if (!subdirs.exists()) {
            // TODO check if works else give warning because of invalid
            // directory name
            // and move to blacklist
            subdirs.mkdirs();

            log().verbose("Subdirectory created: " + subdirs);
        }

        /**
         * This block can't work as expected since chunks with offset 0 can
         * occur on differencing
         */
        if ((!ConfigurationEntry.TRANSFER_SUPPORTS_PARTTRANSFERS
            .getValueBoolean(getController()) || !getPartner()
            .isSupportingPartTransfers())
            && tempFile.exists() && chunk.offset == 0)
        {
            // download started from offset 0 new, remove file,
            // "erase and rewind" ;)
            if (!tempFile.delete()) {
                log().error(
                    "Unable to removed broken tempfile for download: "
                        + tempFile.getAbsolutePath());
                tempFileError = true;
                getController().getTransferManager().setBroken(this,
                    TransferProblem.TEMP_FILE_DELETE);
                return false;
            }
        }

        if (!tempFile.exists()) {
            try {
                // TODO check if works else give warning because of invalid
                // filename or diskfull?
                // and move to blacklist
                tempFile.createNewFile();
            } catch (IOException e) {
                log().error(
                    "Unable to create/open tempfile for donwload: "
                        + tempFile.getAbsolutePath() + ". " + e.getMessage());
                tempFileError = true;
                getController().getTransferManager().setBroken(this,
                    TransferProblem.TEMP_FILE_OPEN, e.getMessage());
                return false;
            }
        }

        // log().warn("Tempfile exists ? " + tempFile.exists() + ", " +
        // tempFile.getAbsolutePath());

        if (!tempFile.canWrite()) {
            log().error(
                "Unable to write to tempfile for donwload: "
                    + tempFile.getAbsolutePath());
            tempFileError = true;
            getController().getTransferManager().setBroken(this,
                TransferProblem.TEMP_FILE_WRITE);
            return false;
        }

        try {
            if (raf == null) {
                raf = new RandomAccessFile(tempFile, "rw");
            }
            // check chunk validity
            if (chunk.offset < 0 || chunk.offset > getFile().getSize()
                || chunk.data == null
                || (chunk.data.length + chunk.offset > getFile().getSize())
            /* || chunk.offset != raf.length() */)
            {

                String reason = "unknown";

                if (chunk.offset < 0 || chunk.offset > getFile().getSize()) {
                    reason = "Illegal offset " + chunk.offset;
                }

                if (chunk.data == null) {
                    reason = "Chunk data null";
                } else {
                    if (chunk.data.length + chunk.offset > getFile().getSize())
                    {
                        reason = "Chunk exceeds filesize";
                    }

                    if (chunk.offset != raf.length()) {
                        reason = "Offset does not matches the current tempfile size. offset: "
                            + chunk.offset + ", filesize: " + tempFile.length();
                    }
                }
                log().error(
                    "Received illegal chunk. " + chunk + ". Reason: " + reason);
                // Abort dl
                getController().getTransferManager().setBroken(this,
                    TransferProblem.ILLEGAL_CHUNK, reason);
                return false;
            }

            // add bytes to transferred status
            getCounter().chunkTransferred(chunk);
            FolderStatistic stat = getFile().getFolder(
                getController().getFolderRepository()).getStatistic();
            if (stat != null) {
                stat.getDownloadCounter().chunkTransferred(chunk);
            }

            raf.seek(chunk.offset);
            raf.write(chunk.data);

            Range range = Range.getRangeByLength(chunk.offset,
                chunk.data.length);
            FilePartsState state = getFilePartsState();
            state.setPartState(range, PartState.AVAILABLE);
            long avs = state.countPartStates(state.getRange(),
                PartState.AVAILABLE);
            if (initialAvailableCount < 0 || initialAvailableCount > avs) {
                initialAvailableCount = avs;
            }
            // Note: This is an estimate which will be the same for all
            // downloads on the same file
            // TODO: For swarming downloads the whole thing should be
            // refactored!! (Like adding a strategy which manages downloads)
            transferState.setState(TransferState.DOWNLOADING);
            transferState.setProgress(((double) (avs - initialAvailableCount))
                / getFile().getSize());

            synchronized (pendingRequests) {
                // Maybe the sender merged requests from us, so check all
                // requests
                for (Iterator<RequestPart> ip = pendingRequests.iterator(); ip
                    .hasNext();)
                {
                    RequestPart p = ip.next();
                    if (p.getRange().contains(range)) {
                        ip.remove();
                    }
                }

            }
            if (usePartialTransfers()) {
                requestParts();
            }
        } catch (IOException e) {
            // TODO: Disk full warning ?
            log().error(
                "Error while writing to tempfile for donwload: "
                    + tempFile.getAbsolutePath() + ". " + e.getMessage());
            log().verbose(e);
            tempFileError = true;
            getController().getTransferManager().setBroken(this,
                TransferProblem.IO_EXCEPTION, e.getMessage());
            return false;
        }

        if (!completed) {
            // completed = chunk.data.length + chunk.offset ==
            // getFile().getSize();
            // getFile().getFilePartsState().debugOutput(log());
            completed = getFilePartsState().isCompleted();
            if (completed) {
                // Finish download
                log().debug("Download completed: " + this);
                checkCompleted();
            }
        }

        return true;
    }

    /**
     * TODO: This is not the way it should be when using swarming because every
     * Download might do a hash check, whereas it's always the same file and
     * thus only one should do it: the last one. This also could be fixed by
     * adding a "strategy" class which manages the downloads for one file.
     */
    private void checkCompleted() {
        if (usePartialTransfers() && remotePartRecord != null) {
            getController().getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        transferState.setState(TransferState.VERIFYING);
                        log().debug("Verifying file hash.");
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] data = new byte[8192];
                        long rem = raf.length();
                        long len = raf.length();
                        raf.seek(0);
                        while (rem > 0) {
                            int read = raf.read(data);
                            md.update(data, 0, read);
                            rem -= read;
                            transferState.setProgress(1.0 - ((double) rem)
                                / len);
                        }
                        if (Arrays.equals(md.digest(), remotePartRecord
                            .getFileDigest()))
                        {
                            // Inform transfer manager
                            log().info("Successfully checked file hash!");
                            getTransferManager().setCompleted(Download.this);
                            // Use remote part record, prevent local rehashing.
                            file.setFilePartsRecord(remotePartRecord);
                        } else {
                            // MD5 sum mismatch
                            log().error("MD5-Hash error:");
                            // Note: This will result in a download of the
                            // complete file in case of a hash error.
                            // Although some parts might have already been
                            // transfered before it's still a good
                            // idea to do this:
                            // If the md5 hash is broken either the file itself
                            // or the transfer has a problem, so
                            // it's better to download everything again
                            getFilePartsState().setPartState(
                                Range.getRangeByLength(0, getFilePartsState()
                                    .getFileLength()), PartState.NEEDED);
                            completed = false;
                            requestParts();

                            // getController().getTransferManager().setBroken(Download.this,
                            // TransferProblem.MD5_ERROR);
                        }
                    } catch (IOException e) {
                        log().error(e);
                        tempFileError = true;
                        getController().getTransferManager().setBroken(
                            Download.this, TransferProblem.IO_EXCEPTION,
                            e.getMessage());
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("MD5 not found, fatal", e);
                    } finally {
                        log().debug("DONE - Validating file hash.");
                    }
                    // Inform transfer manager
                    getTransferManager().setCompleted(Download.this);
                }
            });
        } else {
            // Inform transfer manager
            getTransferManager().setCompleted(this);
        }
    }

    private boolean usePartialTransfers() {
        return getPartner().isSupportingPartTransfers()
            && ConfigurationEntry.TRANSFER_SUPPORTS_PARTTRANSFERS
                .getValueBoolean(getController());
    }

    private FilePartsState getFilePartsState() {
        if (filePartsState == null) {
            filePartsState = new FilePartsState(getFile().getSize());
        }
        return filePartsState;
    }

    private void invalidateFilePartsState() {
        filePartsState = null;
    }

    /**
     * @return the tempfile for this download
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
     * 
     * @return true if the download was requests, false if this download has no
     *         content (size = 0)
     */
    boolean request(Member from) {
        if (from == null) {
            throw new NullPointerException("From is null");
        }
        // Set partner
        setPartner(from);

        Range range = getFilePartsState().findFirstPart(PartState.NEEDED);
        if (range != null) {
            getPartner().sendMessageAsynchron(
                new RequestDownload(getFile(), range.getStart()), null);
            return true;
        }
        return false;
    }

    /**
     * Completes a zero size download by creating simply the file.
     */
    void completeZeroSizeDownload() {
        // Empty file, don't waste bandwidth and just create an empty temp
        // file if necessary.
        try {
            // Create parent directory!
            File subdirs = getTempFile().getParentFile();
            if (!subdirs.exists()) {
                // TODO check if works else give warning because of invalid
                // directory name and move to blacklist
                subdirs.mkdirs();
                log().verbose("Subdirectory created: " + subdirs);
            }
            getTempFile().createNewFile();
            getTransferManager().setCompleted(this);
        } catch (IOException e) {
            log().error(e);
            getTransferManager().setBroken(this, TransferProblem.IO_EXCEPTION,
                e.getMessage());
        }
    }

    /**
     * Requests to abort this dl
     */
    public void abort() {
        getController().getTransferManager().abortDownload(this);
    }

    @Override
    void shutdown() {
        super.shutdown();
        synchronized (pendingRequests) {
            for (RequestPart pr : pendingRequests) {
                // Set requested ranges back to NEEDED. Actually pr.getFile()
                // should be the same as getFile() - but you never know ;)
                getFilePartsState().setPartState(pr.getRange(),
                    PartState.NEEDED);
            }
            pendingRequests.clear();
        }

        // Set lastmodified of file.
        File tempFile = getTempFile();
        if (tempFile != null && tempFile.exists()) {
            tempFile.setLastModified(getFile().getModifiedDate().getTime());
        }
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
     * @return if this transfer has already started
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

    @Override
    void setCompleted() {
        if (usePartialTransfers()) {
            getPartner().sendMessagesAsynchron(new StopUpload(getFile()));
        }
        transferState.setState(TransferState.DONE);
        super.setCompleted();
    }

    /**
     * @return if this is a pending download
     */
    public boolean isPending() {
        if (isCompleted()) {
            // not pending when completed
            return false;
        }
        return getPartner() == null || isBroken();
    }

    /**
     * @return if this download is broken. timed out or has no connection
     *         anymore or (on blacklist in folder and isRequestedAutomatic)
     */
    public boolean isBroken() {
        if (super.isBroken()) {
            return true;
        }
        if (tempFileError) {
            log().warn("Abort cause: Tempfile error.");
            return true;
        }
        // timeout is, when dl is not enqued at remote side,
        // and has timeout
        boolean timedOut = ((System.currentTimeMillis() - Constants.DOWNLOAD_REQUEST_TIMEOUT_LIMIT) > lastTouch
            .getTime())
            && !this.queued;
        if (timedOut) {
            log().warn("Abort cause: Timeout.");
            return true;
        }
        // Check queueing at remote side
        boolean isQueuedAtPartner = stillQueuedAtPartner();
        if (!isQueuedAtPartner) {
            log().warn("Abort cause: not queued.");
            return true;
        }
        // check blacklist
        if (isRequestedAutomatic()) {
            Folder folder = getFile().getFolder(
                getController().getFolderRepository());
            boolean onBlacklist = folder.getBlacklist().isIgnored(getFile());
            if (onBlacklist) {
                log().warn("Abort cause: On blacklist.");
                return true;
            }

            // Check if newer file is available.
            boolean newerFileAvailable = getFile().isNewerAvailable(
                getController().getFolderRepository());
            if (newerFileAvailable) {
                log().warn("Abort cause: Newer version available.");
                return true;
            }
        }

        return false;
    }

    /**
     * @return if this download is completed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * @return if this download is queued
     */
    public boolean isQueued() {
        return queued && !isBroken();
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

    @Override
    protected void setStartOffset(long startOffset) {
        super.setStartOffset(startOffset);
        getFilePartsState().setPartState(
            Range.getRangeByLength(0, startOffset), PartState.AVAILABLE);
    }

    // General ****************************************************************

    @Override
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