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
import java.util.LinkedList;
import java.util.Queue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.AbortDownload;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.StopUpload;
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

    private Queue<RequestPart> pendingRequests = new LinkedList<RequestPart>();

    private transient DownloadManager handler;

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
        super(tm, (FileInfo) file, null);
        // from can be null
        this.lastTouch = new Date();
        this.automatic = automatic;
        this.queued = false;
        this.markedBroken = false;
    }

    /**
     * Re-initalized the Transfer with the TransferManager. Use this only if you
     * are know what you are doing .
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
        Reject.ifFalse(handler.getFileInfo().isVersionDateAndSizeIdentical(
            getFile()), "Fileinfos mismatch. expected "
            + getFile().toDetailString() + ", got "
            + handler.getFileInfo().toDetailString());
        if (this.handler != null) {
            throw new IllegalStateException("DownloadManager already set!");
        }
        this.handler = handler;
    }

    public DownloadManager getDownloadManager() {
        return handler;
    }

    /**
     * Called when the partner supports part-transfers and is ready to upload
     * 
     * @param fileInfo
     *            the fileInfo the remote side uses.
     * @param usedFileInfo
     */
    public void uploadStarted(FileInfo fileInfo) {
        checkFileInfo(fileInfo);
        lastTouch.setTime(System.currentTimeMillis());
        if (isStarted()) {
            logWarning("Received multiple upload start messages: "
                + fileInfo.toDetailString());
            return;
        }

        logFiner("Uploader supports partial transfers for "
            + fileInfo.toDetailString());
        setStarted();
        handler.readyForRequests(Download.this);
    }

    /**
     * Requests a FPR from the remote side.
     */
    void requestFilePartsRecord() {
        assert Util.useDeltaSync(getController(), getPartner()) : "Requesting FilePartsRecord from a client that doesn't support that!";
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
        logInfo("Received parts record");
        handler.filePartsRecordReceived(Download.this, record);
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
        synchronized (pendingRequests) {
            if (pendingRequests.size() >= getTransferManager()
                .getMaxRequestsQueued())
            {
                return false;
            }

            try {
                rp = new RequestPart(getFile(), range, Math.max(0,
                    transferState.getProgress()));
            } catch (IllegalArgumentException e) {
                // I need to do this because FileInfos are NOT immutable...
                logWarning("Concurrent file change while requesting:" + e);
                throw new BrokenDownloadException(
                    "Concurrent file change while requesting: " + e);
            }
            pendingRequests.add(rp);
        }
        getPartner().sendMessagesAsynchron(rp);
        return true;
    }

    public Collection<RequestPart> getPendingRequests() {
        synchronized (pendingRequests) {
            return Collections.unmodifiableCollection(pendingRequests);
        }
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
            setStarted();
        }
        lastTouch.setTime(System.currentTimeMillis());

        // Remove pending requests for the received chunk since
        // the manager below might want to request new parts.
        Range range = Range.getRangeByLength(chunk.offset, chunk.data.length);
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

        getCounter().chunkTransferred(chunk);

        handler.chunkReceived(Download.this, chunk);
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
        getPartner().sendMessagesAsynchron(
            new RequestDownload(getFile(), startOffset));
    }

    /**
     * Requests to abort this dl
     */
    public void abort() {
        abort(true);
    }

    /**
     * @param informRemote
     *            if a <code>AbortDownload</code> message should be sent to the
     *            partner
     */
    void abort(boolean informRemote) {
        shutdown();
        if (getPartner() != null && getPartner().isCompleteyConnected()
            && informRemote)
        {
            getPartner().sendMessageAsynchron(new AbortDownload(getFile()),
                null);
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
     */
    public void setQueued(FileInfo fInfo) {
        Reject.ifNull(fInfo, "fInfo is null!");
        checkFileInfo(fInfo);
        logFiner("DL queued by remote side: " + this);
        queued = true;
        getTransferManager().downloadQueued(Download.this, getPartner());
    }

    @Override
    void setCompleted() {
        super.setCompleted();
        getPartner().sendMessagesAsynchron(new StopUpload(getFile()));
        getTransferManager().setCompleted(Download.this);
    }

    /**
     * @return if this is a pending download
     */
    public boolean isPending() {
        if (isCompleted()) {
            // not pending when completed
            return false;
        }
        return getPartner() == null;
    }

    /**
     * Sets the download to a broken state.
     * 
     * @param problem
     * @param message
     */
    public void setBroken(final TransferProblem problem, final String message) {
        synchronized (this) {
            // Prevent setBroken from being called more than once on a
            // single
            // download
            if (markedBroken) {
                if (isFiner()) {
                    logFiner("Not breaking already marked broken download");
                }
                return;
            }
            markedBroken = true;
        }
        Member p = getPartner();
        if (p != null && p.isCompleteyConnected()) {
            p.sendMessageAsynchron(new AbortDownload(getFile()), null);
        }
        shutdown();
        getTransferManager().downloadbroken(Download.this, problem, message);
    }

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
        // timeout is, when dl is not enqued at remote side,
        // and has timeout. Don't check timeout during filehasing of UPLOADER
        if (getState() != TransferState.FILERECORD_REQUEST) {
            boolean timedOut = System.currentTimeMillis()
                - Constants.DOWNLOAD_REQUEST_TIMEOUT_LIMIT > lastTouch
                .getTime()
                && !queued;
            if (timedOut) {
                logFine("Break cause: Timeout.");
                return true;
            }
        }
        // Check queueing at remote side
        boolean isQueuedAtPartner = stillQueuedAtPartner();
        if (!isQueuedAtPartner) {
            logFine("Break cause: not queued.");
            return true;
        }
        // check blacklist
        Folder folder = getFile().getFolder(
            getController().getFolderRepository());
        boolean onBlacklist = folder.getDiskItemFilter().isExcluded(getFile());
        if (onBlacklist) {
            logFine("Break cause: On blacklist.");
            return true;
        }

        /*
         * Wrong place to check, since we could actually want to load an old
         * version!
         */
        // True, but re-added check because of #1326
        // Check if newer file is available. boolean
        boolean newerFileAvailable = getFile().isNewerAvailable(
            getController().getFolderRepository());
        if (newerFileAvailable) {
            logFine("Break cause: Newer version available. "
                + getFile().toDetailString());
            return true;
        }

        return false;
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

    // Checks ****************************************************************
    private void requestCheckState() {
        if (handler == null) {
            throw new IllegalStateException("DownloadManager not set!");
        }
    }

    private void checkFileInfo(FileInfo fileInfo) {
        Reject.ifFalse(fileInfo.isVersionDateAndSizeIdentical(getFile()),
            "FileInfo mismatch!");
    }
}