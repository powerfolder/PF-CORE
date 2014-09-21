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
 * $Id$
 */
package de.dal33t.powerfolder.transfer;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.AbortDownload;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestDownloadExt;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.RequestPartExt;
import de.dal33t.powerfolder.message.StopUpload;
import de.dal33t.powerfolder.message.StopUploadExt;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Validate;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * Download class, containing file and member.<BR>
 * Serializable for remembering completed Downloads in DownLoadTableModel.
 * <P>
 * Attention: All synchronized method are only allowed to be called by
 * DownloadManager
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class Download extends Transfer {

    private static final long serialVersionUID = 100L;

    private Date lastTouch;
    private final boolean automatic;
    private boolean queued;
    private boolean markedBroken;

    private Queue<RequestPart> pendingRequests = new ConcurrentLinkedQueue<RequestPart>();

    private transient DownloadManager dlManager;

    /** for serialisation */
    public Download() {
        automatic = false;
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
        this.markedBroken = false;
    }

    /**
     * Re-initialized the Transfer with the TransferManager. Use this only if
     * you are know what you are doing .
     *
     * @param aTransferManager
     *            the transfermanager
     */
    void init(TransferManager aTransferManager) {
        super.init(aTransferManager);
        queued = false;
        markedBroken = false;
    }

    /**
     * @return if this download was automatically requested
     */
    public boolean isRequestedAutomatic() {
        return automatic;
    }

    public synchronized void setDownloadManager(DownloadManager handler) {
        Reject.ifNull(handler, "Handler is null!");
        if (!handler.getFileInfo().isVersionDateAndSizeIdentical(getFile())) {
            throw new IllegalArgumentException("Fileinfos mismatch. expected "
                + getFile().toDetailString() + ", got "
                + handler.getFileInfo().toDetailString());
        }
        if (this.dlManager != null) {
            throw new IllegalStateException("DownloadManager already set!");
        }
        this.dlManager = handler;
    }

    public DownloadManager getDownloadManager() {
        return dlManager;
    }

    /**
     * Called when the partner supports part-transfers and is ready to upload
     *
     * @param fileInfo
     *            the fileInfo the remote side uses.
     */
    public void uploadStarted(FileInfo fileInfo) {
        checkFileInfo(fileInfo);
        lastTouch.setTime(System.currentTimeMillis());

        // Maybe remove this check?
        if (isStarted()) {
            if (isWarning()) {
                logWarning("Aborting. Received multiple upload start messages: "
                    + this);
            }
            abort();
            return;
        }

        if (isFiner()) {
            logFiner("Uploader supports partial transfers for "
                + fileInfo.toDetailString());
        }
        setStarted();
        dlManager.readyForRequests(Download.this);
    }

    /**
     * Requests a FPR from the remote side.
     */
    void requestFilePartsRecord() {
        assert Util.useDeltaSync(getController(), this) : "Requesting FilePartsRecord from a client that doesn't support that!";
        requestCheckState();

        getPartner().sendMessagesAsynchron(
            new RequestFilePartsRecord(getFile()));
    }

    /**
     * Invoked when a record for this download was received.
     *
     * @param fileInfo
     *            the fileInfo the remote side uses.
     * @param record
     *            the record received.
     */
    public void receivedFilePartsRecord(FileInfo fileInfo,
        final FilePartsRecord record)
    {
        Reject.ifNull(record, "Record is null");
        checkFileInfo(fileInfo);

        lastTouch.setTime(System.currentTimeMillis());
        if (isFine()) {
            logFine("Received parts record for " + fileInfo.toDetailString()
                + ": " + record);
        }
        dlManager.filePartsRecordReceived(Download.this, record);
    }

    /**
     * Requests a single part from the remote peer.
     *
     * @param range
     * @return
     * @throws BrokenDownloadException
     */
    boolean requestPart(Range range) throws BrokenDownloadException {
        Validate.notNull(range);
        requestCheckState();

        RequestPart rp;
        if (pendingRequests.size() >= getTransferManager()
            .getMaxRequestsQueued())
        {
            if (isFiner()) {
                logFiner("X Skipping request. Already got too many pending requests: " + range);
            }
            return false;
        }
        if (isFiner()) {
            logFiner("X requestPart: " + range);
        }
        try {
            if (getPartner().getProtocolVersion() >= 109) {
                rp = new RequestPartExt(getFile(), range, Math.max(0,
                    state.getProgress()));
            } else {
                rp = new RequestPart(getFile(), range, Math.max(0,
                    state.getProgress()));
            }
        } catch (IllegalArgumentException e) {
            // I need to do this because FileInfos are NOT immutable...
            if (isWarning()) {
                logWarning("Concurrent file change while requesting:" + e);
            }
            throw new BrokenDownloadException(
                "Concurrent file change while requesting: " + e);
        }
        pendingRequests.add(rp);
        getPartner().sendMessagesAsynchron(rp);
        return true;
    }

    public Collection<RequestPart> getPendingRequests() {
        return Collections.unmodifiableCollection(pendingRequests);
    }

    /**
     * Adds a chunk to the download
     *
     * @param chunk
     * @return true if the chunk was successfully appended to the download file.
     */
    public boolean addChunk(final FileChunk chunk) {
        Reject.ifNull(chunk, "Chunk is null");
        checkFileInfo(chunk.file);

        getTransferManager().chunkAdded(Download.this, chunk);

        if (isBroken()) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, "isBroken()");
            return false;
        }

        // donwload begins to start
        if (!isStarted()) {
            if (Util.useSwarming(getController(), getPartner())) {
                // Old passive downloads=Started with the first file chunk
                setStarted();
            } else {
                if (isWarning()) {
                    logWarning("Ignoring file chunk for non-started " + this);
                }
                return false;
            }
        }
        lastTouch.setTime(System.currentTimeMillis());

        // Remove pending requests for the received chunk since
        // the manager below might want to request new parts.
        Range range = Range.getRangeByLength(chunk.offset, chunk.data.length);

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

        getCounter().chunkTransferred(chunk);

        dlManager.chunkReceived(Download.this, chunk);
        return true;
    }

    /**
     * Requests this download from the partner.
     *
     * @param startOffset
     */
    void request(long startOffset) {
        Reject.ifTrue(startOffset < 0 || startOffset >= getFile().getSize(),
            "Invalid startOffset: " + startOffset);
        requestCheckState();

        if (isFiner()) {
            logFiner("request(" + startOffset + "): "
                + getFile().toDetailString());
        }
        if (getPartner().getProtocolVersion() >= 109) {
            getPartner().sendMessagesAsynchron(
                new RequestDownloadExt(getFile(), startOffset));
        } else {
            getPartner().sendMessagesAsynchron(
                new RequestDownload(getFile(), startOffset));
        }
    }

    /**
     * Requests to abort this dl
     */
    void abort() {
        abort(true);
    }

    /**
     * @param informRemote
     *            if a <code>AbortDownload</code> message should be sent to the
     *            partner
     */
    void abort(boolean informRemote) {
        shutdown();
        if (getPartner() != null && getPartner().isCompletelyConnected()
            && informRemote)
        {
            getPartner().sendMessageAsynchron(new AbortDownload(getFile()));
        }
        if (getDownloadManager() == null) {
            logSevere(this + " has no DownloadManager! (abort before start?)");
            // For Pending downloads without download manager
            getController().getTransferManager().downloadAborted(Download.this);
            return;
        }
        getController().getTransferManager().downloadAborted(Download.this);
    }

    /**
     * This download is queued at the remote side
     *
     * @param fInfo
     *            the fileinfo
     */
    public void setQueued(FileInfo fInfo) {
        Reject.ifNull(fInfo, "fInfo is null!");
        checkFileInfo(fInfo);
        if (isFiner()) {
            logFiner("DL queued by remote side: " + this);
        }
        queued = true;
        getTransferManager().downloadQueued(Download.this, getPartner());
    }

    @Override
    void setCompleted() {
        super.setCompleted();
        if (getPartner().getProtocolVersion() >= 109) {
            getPartner().sendMessagesAsynchron(new StopUploadExt(getFile()));
        } else {
            getPartner().sendMessagesAsynchron(new StopUpload(getFile()));
        }

        getTransferManager().setCompleted(Download.this);
    }

    /**
     * @return if this is a pending download
     */
    public boolean isPending() {
        return !isCompleted() && getPartner() == null;
    }

    /**
     * Sets the download to a broken state.
     *
     * @param problem
     * @param message
     */
    void setBroken(final TransferProblem problem, final String message) {
        synchronized (this) {
            // Prevent setBroken from being called more than once on a
            // single download
            if (markedBroken) {
                if (isFiner()) {
                    logFiner("Not breaking already marked broken download");
                }
                return;
            }
            markedBroken = true;
        }
        Member p = getPartner();
        if (p != null && p.isCompletelyConnected()) {
            p.sendMessageAsynchron(new AbortDownload(getFile()));
        }
        shutdown();
        getTransferManager().downloadBroken(Download.this, problem, message);
    }

    private long lastBrokenCheck;
    private boolean brokenCache;

    /**
     * @return if this download is broken. timed out or has no connection
     *         anymore or (on blacklist in folder and isRequestedAutomatic)
     */
    public boolean isBroken() {
        if (markedBroken) {
            return true;
        }
        if (super.isBroken()) {
            return true;
        }
        if (System.currentTimeMillis() - lastBrokenCheck < 1000) {
            // Check every second only
            return brokenCache;
        }

        lastBrokenCheck = System.currentTimeMillis();

        // timeout is, when dl is not enqued at remote side,
        // and has timeout. Don't check timeout during filehasing of UPLOADER
        // and DOWNLOADER (#1829)
        if (stateCanTimeout()) {
            boolean timedOut = System.currentTimeMillis()
                - Constants.DOWNLOAD_REQUEST_TIMEOUT_LIMIT > lastTouch
                .getTime() && !queued;
            if (timedOut) {
                if (isFine()) {
                    logFine("Break cause: Timeout. "
                        + getFile().toDetailString());
                }
                brokenCache = true;
                return true;
            }
        }
        // Check queueing at remote side
        boolean isQueuedAtPartner = stillQueuedAtPartner();
        if (!isQueuedAtPartner) {
            if (isFine()) {
                logFine("Break cause: not queued.");
            }
            brokenCache = true;
            return true;
        }

        // Not done here, may cause too much CPU.
        // #2532: Done in TransferManager.checkActiveTranfersForExcludes();
        // Folder folder = getFile().getFolder(
        // getController().getFolderRepository());
        // boolean onBlacklist =
        // folder.getDiskItemFilter().isExcluded(getFile());
        // if (onBlacklist) {
        // if (isFine()) {
        // logFine("Break cause: Excluded from sync.");
        // }
        // return true;
        // }

        /*
         * Wrong place to check, since we could actually want to load an old
         * version!
         */
        // True, but re-added check because of #1326
        // Check if newer file is available. boolean
        boolean newerFileAvailable = getFile().isNewerAvailable(
            getController().getFolderRepository());
        if (newerFileAvailable) {
            if (isFine()) {
                logFine("Break cause: Newer version available. "
                    + getFile().toDetailString());
            }
            brokenCache = true;
            return true;
        }

        brokenCache = false;
        return false;
    }

    private boolean stateCanTimeout() {
        TransferState state = getTransferState();
        return state != TransferState.FILERECORD_REQUEST
            && state != TransferState.VERIFYING
            && state != TransferState.MATCHING;
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

    // General ****************************************************************

    @Override
    public String toString() {
        String msg = "Download: " + getFile().toDetailString();
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

    // Checks ****************************************************************
    private void requestCheckState() {
        if (dlManager == null) {
            throw new IllegalStateException("DownloadManager not set!");
        }
    }

    private void checkFileInfo(FileInfo fileInfo) {
        Reject.ifFalse(fileInfo.isVersionDateAndSizeIdentical(getFile()),
            "FileInfo mismatch!");
    }
}