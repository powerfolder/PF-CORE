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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import de.dal33t.powerfolder.transfer.swarm.TransferUtil;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Validate;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * Download class, containing file and member.<BR>
 * Serializable for remembering completed Downloads in DownLoadTableModel.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class Download extends Transfer {

    private static final Logger log = Logger
        .getLogger(Download.class.getName());
    private static final long serialVersionUID = 100L;
    public static final int MAX_REQUESTS_QUEUED = 15;

    private Date lastTouch;
    private boolean automatic;
    private boolean queued;

    private Queue<RequestPart> pendingRequests = new LinkedList<RequestPart>();

    private transient DownloadManager handler;

    /**
     * Indicates that this download is broken.
     */
    private boolean markedBroken;

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
        super(tm, (FileInfo) file.clone(), null);
        // from can be null
        this.lastTouch = new Date();
        this.automatic = automatic;
        this.queued = false;
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

    public void setDownloadManager(DownloadManager handler) {
        Reject.ifNull(handler, "Handler is null!");
        Reject.ifFalse(handler.getFileInfo().isCompletelyIdentical(getFile()),
            "Fileinfos mismatch. expected " + getFile().toDetailString()
                + ", got " + handler.getFileInfo().toDetailString());
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
     */
    public void uploadStarted(FileInfo fileInfo) {
        checkFileInfo(fileInfo);
        lastTouch.setTime(System.currentTimeMillis());
        if (isStarted()) {
            logWarning("Received multiple upload start messages!");
            return;
        }

        logFiner("Uploader supports partial transfers.");
        setStarted();
        handler.readyForRequests(Download.this);
    }

    /**
     * Requests a FPR from the remote side.
     */
    public synchronized void requestFilePartsRecord() {
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
    public synchronized boolean requestPart(Range range)
        throws BrokenDownloadException
    {
        Validate.notNull(range);
        requestCheckState();

        RequestPart rp;
        synchronized (pendingRequests) {
            if (pendingRequests.size() >= MAX_REQUESTS_QUEUED) {
                return false;
            }

            try {
                rp = new RequestPart(getFile(), range, Math.max(0,
                    transferState.getProgress()));
            } catch (IllegalArgumentException e) {
                // I need to do this because FileInfos are NOT immutable...
                log.log(Level.WARNING,
                    "Concurrent file change while requesting:" + e);
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
            return new ArrayList<RequestPart>(pendingRequests);
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

        TransferUtil.invokeLater(new Runnable() {
            public void run() {
                getTransferManager().chunkAdded(Download.this, chunk);
            }
        });

        if (isBroken()) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, "isBroken()");
            return false;
        }

        // logFine("Received " + chunk);

        if (!isStarted()) {
            // donwload begins to start
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
    public synchronized void request(long startOffset) {
        Reject.ifTrue(startOffset < 0 || startOffset >= getFile().getSize(),
            "Invalid startOffset: " + startOffset);
        requestCheckState();

        getPartner().sendMessagesAsynchron(
            new RequestDownload(getFile(), startOffset));
    }

    /**
     * Requests to abort this dl
     */
    public synchronized void abort() {
        shutdown();
        if (getPartner() != null && getPartner().isCompleteyConnected()) {
            getPartner().sendMessageAsynchron(new AbortDownload(getFile()),
                null);
        }
        TransferUtil.invokeLater(new Runnable() {
            public void run() {
                getController().getTransferManager().downloadAborted(
                    Download.this);
            }
        });
    }

    /**
     * This download is queued at the remote side
     * @param fInfo 
     */
    public void setQueued(FileInfo fInfo) {
        Reject.ifNull(fInfo, "fInfo is null!");
        checkFileInfo(fInfo);
        log.finer("DL queued by remote side: " + this);
        queued = true;

        TransferUtil.invokeLater(new Runnable() {
            public void run() {
                getTransferManager()
                    .downloadQueued(Download.this, getPartner());
            }
        });
    }

    @Override
    synchronized void setCompleted() {
        super.setCompleted();
        getPartner().sendMessagesAsynchron(new StopUpload(getFile()));

        TransferUtil.invokeLater(new Runnable() {

            public void run() {
                getTransferManager().setCompleted(Download.this);
            }

        });
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
     * Sets the download to a broken state.
     * 
     * @param problem
     * @param message
     */
    public synchronized void setBroken(final TransferProblem problem,
        final String message)
    {
        // Prevent setBroken from being called more than once on a single
        // download
        if (markedBroken) {
            return;
        }
        markedBroken = true;
        Member p = getPartner();
        if (p != null && p.isCompleteyConnected()) {
            p.sendMessageAsynchron(new AbortDownload(getFile()), null);
        }
        shutdown();
        TransferUtil.invokeLater(new Runnable() {
            public void run() {
                getTransferManager().downloadbroken(Download.this, problem,
                    message);
            }
        });
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
        // and has timeout
        boolean timedOut = System.currentTimeMillis()
            - Constants.DOWNLOAD_REQUEST_TIMEOUT_LIMIT > lastTouch.getTime()
            && !queued;
        if (timedOut) {
            logWarning("Break cause: Timeout.");
            return true;
        }
        // Check queueing at remote side
        boolean isQueuedAtPartner = stillQueuedAtPartner();
        if (!isQueuedAtPartner) {
            logWarning("Break cause: not queued.");
            return true;
        }
        // check blacklist
        Folder folder = getFile().getFolder(
            getController().getFolderRepository());
        boolean onBlacklist = folder.getDiskItemFilter().isExcluded(getFile());
        if (onBlacklist) {
            logWarning("Break cause: On blacklist.");
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
            logWarning("Break cause: Newer version available. "
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
        Reject.ifFalse(fileInfo.isCompletelyIdentical(getFile()),
            "FileInfo mismatch!");
    }
}