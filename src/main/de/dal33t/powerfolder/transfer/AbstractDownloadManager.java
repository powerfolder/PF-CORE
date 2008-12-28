/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.transfer.swarm.TransferUtil;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.FileCheckWorker;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.ProgressObserver;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState;
import de.dal33t.powerfolder.util.delta.MatchCopyWorker;
import de.dal33t.powerfolder.util.delta.MatchInfo;
import de.dal33t.powerfolder.util.delta.MatchResultWorker;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

/**
 * Shared implementation of download managers. This class leaves details on what
 * to request from whom to the implementing class.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public abstract class AbstractDownloadManager extends PFComponent implements
    DownloadManager
{

    private static final Logger log = Logger
        .getLogger(AbstractDownloadManager.class.getName());

    private enum InternalState {
        WAITING_FOR_SOURCE, WAITING_FOR_UPLOAD_READY, WAITING_FOR_FILEPARTSRECORD,

        COMPLETED {
            @Override
            public boolean isDone() {
                return true;
            }
        },
        BROKEN {
            @Override
            public boolean isDone() {
                return true;
            }
        },
        ABORTED {
            @Override
            public boolean isDone() {
                return true;
            }

        },

        /**
         * Request chunks
         */
        ACTIVE_DOWNLOAD, MATCHING_AND_COPYING, CHECKING_FILE_VALIDITY;

        public boolean isDone() {
            return false;
        }
    }

    protected FilePartsState filePartsState;

    protected FilePartsRecord remotePartRecord;

    private volatile TransferCounter counter;
    private State transferState = new State();

    private final FileInfo fileInfo;
    private Controller controller;
    private RandomAccessFile tempRAF = null;

    private volatile InternalState state = InternalState.WAITING_FOR_SOURCE;

    private volatile boolean automatic;
    private volatile boolean started;

    private Thread worker;

    private boolean shutdown;

    private File metaFile;

    private String fileID;

    private File tempFile;

    public AbstractDownloadManager(Controller controller, FileInfo file,
        boolean automatic) throws IOException
    {
        Reject.noNullElements(controller, file);

        this.fileInfo = (FileInfo) file.clone();
        this.automatic = automatic;

        this.controller = controller;

        init();
    }

    public synchronized void abort() {
        // illegalState("abort");
        switch (state) {
            case ABORTED :
                illegalState("abort()");
                break;
            case BROKEN :
            case COMPLETED :
                break;
            default :
                setAborted(false);
                break;
        }
    }

    public synchronized void abortAndCleanup() {
        // illegalState("abortAndCleanup");
        switch (state) {
            case ABORTED :
                illegalState("abortAndCleanup()");
                break;
            case BROKEN :
            case COMPLETED :
                break;
            default :
                setAborted(true);
                break;
        }
    }

    public synchronized void addSource(Download download) {
        validateDownload(download);
        Reject.ifFalse(download.isCompleted()
            || canAddSource(download.getPartner()),
            "Illegal addSource() call!!");
        try {
            addSource0(download);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public synchronized void chunkReceived(Download download, FileChunk chunk) {
        Reject.noNullElements(download, chunk);
        validateDownload(download);
        assert chunk.file.isCompletelyIdentical(getFileInfo());
        try {
            receivedChunk0(download, chunk);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public synchronized void filePartsRecordReceived(Download download,
        FilePartsRecord record)
    {
        Reject.noNullElements(download, record);
        validateDownload(download);
        try {
            receivedFilePartsRecord0(download, record);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public Controller getController() {
        return controller;
    }

    /**
     * Returns the transfer counter
     * 
     * @return
     */
    public TransferCounter getCounter() {
        if (counter == null) {
            counter = new TransferCounter(0, fileInfo.getSize());
        }
        return counter;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public State getState() {
        return transferState;
    }

    /**
     * @return the tempfile for this download
     */
    public File getTempFile() {
        File diskFile = getFileInfo().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        if (tempFile == null) {
            try {
                tempFile = new File(getMetaDataBaseDir(), "(incomplete) "
                    + getFileID());
            } catch (IOException e) {
                logSevere("IOException", e);
                return null;
            }
        }
        return tempFile;
    }

    public boolean isBroken() {
        return state == InternalState.BROKEN;
    }

    public boolean isCompleted() {
        return state == InternalState.COMPLETED;
    }

    public boolean isDone() {
        return state.isDone();
    }

    public boolean isRequestedAutomatic() {
        return automatic;
    }

    public synchronized boolean isShutDown() {
        return shutdown;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    private void post(Runnable runnable) {
        try {
            TransferUtil.invokeLater(runnable);
        } catch (RejectedExecutionException e) {
            log.log(Level.FINE, "RejectedExecutionException", e);
        }
    }

    public synchronized void readyForRequests(Download download) {
        validateDownload(download);
        try {
            readyForRequests0(download);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        } catch (AssertionError e) {
            logSevere("AssertionError", e);
            throw e;
        }
    }

    public synchronized void removeSource(Download download) {
        validateDownload(download);
        try {
            removeSource0(download);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public synchronized void setBroken(String reason) {
        setBroken(TransferProblem.BROKEN_DOWNLOAD, reason);
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "; state= " + state
            + " file=" + getFileInfo() + "; tempFileRAF: " + tempRAF
            + "; tempFile: " + getTempFile() + "; broken: " + isBroken()
            + "; completed: " + isCompleted() + "; aborted: " + isAborted()
            + "; partsState: " + filePartsState;
    }

    protected abstract void addSourceImpl(Download source);

    protected boolean checkCompleted() throws InterruptedException {

        setTransferState(TransferState.VERIFYING);
        // logFine("Verifying file hash for " + this);
        try {
            Callable<Boolean> fileChecker = null;
            if (remotePartRecord != null) {
                fileChecker = new FileCheckWorker(getTempFile(), MessageDigest
                    .getInstance("MD5"), remotePartRecord.getFileDigest())
                {
                    @Override
                    protected void setProgress(int percent) {
                        setTransferState(percent / 100.0);
                    }
                };
            }
            // If we don't have a record, the file is assumed to be
            // "valid"
            if (fileChecker == null || fileChecker.call()) {
                return true;
            }
            logWarning("Checking FAILED");
            counter = new TransferCounter(0, fileInfo.getSize());
            filePartsState.setPartState(Range.getRangeByLength(0,
                filePartsState.getFileLength()), PartState.NEEDED);
            // Maybe part record was bogus.
            remotePartRecord = null;

            return false;
        } catch (NoSuchAlgorithmException e) {
            // If this error occurs, no downloads will ever succeed.
            logSevere("NoSuchAlgorithmException", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            logSevere("Exception", e);
            setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
        }
        return false;
    }

    protected File getFile() {
        return fileInfo.getDiskFile(getController().getFolderRepository());
    }

    protected void init() throws IOException {
        assert fileInfo != null;

        // If it's an old download, don't create a temporary file
        if (isCompleted()) {
            return;
        }

        if (getTempFile() == null) {
            throw new IOException("Couldn't create a temporary file for "
                + fileInfo);
        }

        // This has to happen here since "completed" is valid
        assert !isDone() : "File broken/aborted before init!";
        assert getTempFile().getParentFile().exists() : "Missing PowerFolder system directory";

        // Create temp-file directory structure if necessary
        // if (!getTempFile().getParentFile().exists()) {
        // if (!getTempFile().getParentFile().mkdirs()) {
        // throw new FileNotFoundException(
        // "Couldn't create parent directory!");
        // }
        // }

        loadMetaData();

        tempRAF = new RandomAccessFile(getTempFile(), "rw");
    }

    protected boolean isNeedingFilePartsRecord() {
        return !isCompleted() && remotePartRecord == null
            && fileInfo.getSize() >= Constants.MIN_SIZE_FOR_PARTTRANSFERS
            && fileInfo.diskFileExists(getController());
    }

    protected void matchAndCopyData() throws BrokenDownloadException,
        InterruptedException
    {
        try {
            File src = getFile();

            setTransferState(TransferState.MATCHING);
            ProgressObserver transferObs = new ProgressObserver() {

                public void progressed(double percent) {
                    setTransferState(percent);
                }

            };
            Callable<List<MatchInfo>> mInfoWorker = new MatchResultWorker(
                remotePartRecord, src, transferObs);
            List<MatchInfo> mInfoRes = null;
            mInfoRes = mInfoWorker.call();

            // logFine("Records: " + record.getInfos().length);
            log.fine("Matches: " + mInfoRes.size() + " which are "
                + (remotePartRecord.getPartLength() * mInfoRes.size())
                + " bytes (bit less maybe).");

            setTransferState(TransferState.COPYING);
            Callable<FilePartsState> pStateWorker = new MatchCopyWorker(src,
                getTempFile(), remotePartRecord, mInfoRes, transferObs);
            FilePartsState state = pStateWorker.call();
            if (state.getFileLength() != fileInfo.getSize()) {
                // Concurrent file modification
                throw new BrokenDownloadException();
            }
            setFilePartsState(state);
            counter = new TransferCounter(filePartsState.countPartStates(
                filePartsState.getRange(), PartState.AVAILABLE), fileInfo
                .getSize());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA Digest not found. Fatal error", e);
        } catch (FileNotFoundException e) {
            throw new BrokenDownloadException(
                TransferProblem.FILE_NOT_FOUND_EXCEPTION, e);
        } catch (IOException e) {
            throw new BrokenDownloadException(TransferProblem.IO_EXCEPTION, e);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new BrokenDownloadException(
                TransferProblem.GENERAL_EXCEPTION, e);
        }
    }

    protected abstract void removeSourceImpl(Download source);

    protected abstract void requestFilePartsRecord(Download download);

    /**
     * Be careful with the implementation of this method, it's called with
     * internal locks in place. Reason: This method will access filepartsstate,
     * which is also accessed in here. TODO: Find a "cleaner" way so this method
     * doesn't need to be locked.
     * 
     * @throws BrokenDownloadException
     */
    protected abstract void sendPartRequests() throws BrokenDownloadException;

    protected void setAutomatic(boolean auto) {
        automatic = auto;
    }

    protected synchronized void setBroken(final TransferProblem problem,
        final String message)
    {
        if (isBroken()) {
            return;
        }
        log.fine("Download broken: " + fileInfo + ", problem: " + problem
            + ", message: " + message);
        setState(InternalState.BROKEN);
        shutdown();

        if (getTempFile() != null && getTempFile().exists()
            && getTempFile().length() == 0)
        {
            if (isFiner()) {
                logFiner("Deletin tempfile with size 0.");
            }
            if (!getTempFile().delete()) {
                logWarning("Failed to delete temp file: "
                    + getTempFile().getAbsolutePath());
            }
        }
        for (Download d : getSources()) {
            d.setBroken(problem, message);
        }
        post(new Runnable() {
            public void run() {
                getController().getTransferManager().downloadManagerBroken(
                    AbstractDownloadManager.this, problem, message);
            }
        });
    }

    protected synchronized void setCompleted() {
        assert !isCompleted();

        // Might be broken/aborted in the mean time
        if (isDone()) {
            return;
        }

        log.fine("Completed download of " + fileInfo + ".");

        setState(InternalState.COMPLETED);
        shutdown();
        deleteMetaData();

        // Should be called without locking:
        // It's more "event" style and prevents deadlocks
        post(new Runnable() {
            public void run() {
                getController().getTransferManager().setCompleted(
                    AbstractDownloadManager.this);
            }
        });
        for (Download d : getSources()) {
            d.setCompleted();
        }
    }

    protected synchronized void setFilePartsState(FilePartsState state) {
        assert filePartsState == null;

        if (filePartsState != null) {
            log.severe("FilePartsState should've been null, but was "
                + filePartsState);
            Debug.dumpCurrentStackTrace();
        }

        filePartsState = state;
    }

    protected synchronized void setStarted() {
        started = true;
    }

    protected void setTransferState(double progress) {
        transferState.setProgress(progress);
        for (Download d : getSources()) {
            d.transferState.setProgress(progress);
        }
    }

    protected void setTransferState(TransferState state) {
        if (transferState.getState() == state) {
            return;
        }
        transferState.setState(state);
        transferState.setProgress(0);
        for (Download d : getSources()) {
            d.transferState.setState(state);
            d.transferState.setProgress(0);
        }
    }

    protected void setTransferState(TransferState state, double progress) {
        transferState.setState(state);
        transferState.setProgress(progress);
        for (Download d : getSources()) {
            d.transferState.setState(state);
            d.transferState.setProgress(progress);
        }
    }

    /**
     * Releases resources not required anymore
     */
    protected synchronized void shutdown() {
        assert isDone();
        if (isShutDown()) {
            return;
        }

        assert tempRAF != null;

        shutdown = true;
        // logFine("Shutting down " + fileInfo);
        try {
            if (worker != null && worker.isAlive()
                && worker != Thread.currentThread())
            {
                worker.interrupt();
                try {
                    worker.join(2000);
                } catch (InterruptedException e) {
                    log.finer("InterruptedException:" + e);
                }
                if (worker.isAlive()) {
                    logSevere("Couldn't stop worker thread: " + worker);
                    logSevere("Worker Stack:"
                        + Debug.getStackTrace(worker.getStackTrace()));
                    logSevere("My stack:"
                        + Debug.getStackTrace(Thread.currentThread()
                            .getStackTrace()));
                }
            }
            // logSevere("Closing temp file!");
            tempRAF.close();
            tempRAF = null;

            if (isBroken()) {
                saveMetaData();
            } else {
                deleteMetaData();
            }
        } catch (IOException e) {
            logSevere("IOException", e);
        }
        // FIXME: Uncomment to save resources
        // setFilePartsState(null);
        // TODO: Actually the remote record shouldn't be dropped since if
        // somebody wants to download the file from us
        // we could just send it, instead of recalculating it!! (So it should be
        // stored "somewhere" - like in the
        // folders database or so)
        remotePartRecord = null;
        updateTempFile();

        assert tempRAF == null;
        assert !isCompleted() && !isAborted() || !getMetaFile().exists();
    }

    protected synchronized void startActiveDownload()
        throws BrokenDownloadException
    {
        assert !getSources().isEmpty();
        assert !isDone();

        setStarted();
        setState(InternalState.ACTIVE_DOWNLOAD);
        if (isFiner()) {
            logFiner("Requesting parts");
        }
        sendPartRequests();
    }

    protected synchronized void storeFileChunk(Download download,
        FileChunk chunk)
    {
        assert download != null;
        assert chunk != null;
        assert getSources().contains(download) : "Invalid source!";

        setStarted();

        try {
            tempRAF.seek(chunk.offset);
            tempRAF.write(chunk.data);
        } catch (IOException e) {
            logSevere("IOException", e);
            setBroken(TransferProblem.IO_EXCEPTION,
                "Couldn't write to tempfile!");
            return;
        }

        getCounter().chunkTransferred(chunk);

        Range range = Range.getRangeByLength(chunk.offset, chunk.data.length);
        filePartsState.setPartState(range, PartState.AVAILABLE);

        long avs = filePartsState.countPartStates(filePartsState.getRange(),
            PartState.AVAILABLE);
        setTransferState(TransferState.DOWNLOADING, (double) avs
            / fileInfo.getSize());

        // add bytes to transferred status
        FolderStatistic stat = fileInfo.getFolder(
            getController().getFolderRepository()).getStatistic();
        if (stat != null) {
            stat.getDownloadCounter().chunkTransferred(chunk);
        }
    }

    /**
     * @return true, if the download was completed by this call
     */
    private void addSource0(final Download download)
        throws BrokenDownloadException
    {
        validateDownload(download);
        // This should be true because the addSource() caller should be
        // locking the calls
        assert download.isCompleted() || canAddSource(download.getPartner()) : "Illegal addSource() call!!";

        switch (state) {
            case BROKEN :
                // The connection could've broken while some other code
                // tries to add sources
                download.setBroken(TransferProblem.BROKEN_DOWNLOAD,
                    "Manager already broken!");
                break;
            case MATCHING_AND_COPYING :
            case CHECKING_FILE_VALIDITY :
            case ACTIVE_DOWNLOAD :
            case WAITING_FOR_UPLOAD_READY :
            case WAITING_FOR_FILEPARTSRECORD :
                addSourceImpl(download);

                // Maybe rework the code so this request will be
                // sent if we
                // really need data
                download.request(0);
                break;
            case WAITING_FOR_SOURCE :
                // Required for complete on load downloads
                if (download.isCompleted()) {
                    setState(InternalState.COMPLETED);
                }

                addSourceImpl(download);

                if (isDone()) {
                    break;
                }

                // Zero sized files hack: Shouldn't request anything
                if (getFileInfo().getSize() == 0) {
                    setCompleted();
                    return;
                }

                long _offset = 0;
                if (filePartsState != null) {
                    assert !filePartsState.isCompleted();

                    Range range = filePartsState
                        .findFirstPart(PartState.NEEDED);
                    if (range != null) {
                        _offset = range.getStart();
                    } else {
                        assert filePartsState.isCompleted()
                            || filePartsState.findFirstPart(PartState.PENDING) != null;
                    }
                }
                setState(InternalState.WAITING_FOR_UPLOAD_READY);
                download.request(_offset);
                break;
            case COMPLETED :
                addSourceImpl(download);
                if (!download.isCompleted() && !download.isBroken()) {
                    download.setCompleted();
                }
                break;
            default :
                illegalState("addSource");
                break;
        }
    }

    private void validateDownload(Download download) {
        Reject.ifNull(download, "Download is null!");
        Reject.ifTrue(!download.getFile().isCompletelyIdentical(getFileInfo()),
            "Download FileInfo differs: " + download.getFile().toDetailString()
                + " vs mine: " + getFileInfo().toDetailString());
    }

    /**
     *
     */
    private synchronized void checkFileValidity() {
        assert state == InternalState.ACTIVE_DOWNLOAD
            || state == InternalState.WAITING_FOR_UPLOAD_READY
            && filePartsState.getFileLength() == 0
            || state == InternalState.MATCHING_AND_COPYING : "Invalid state: "
            + state;
        assert worker == null || Thread.currentThread() == worker
            || !worker.isAlive() : worker;

        setState(InternalState.CHECKING_FILE_VALIDITY);
        worker = new Thread(new Runnable() {
            public void run() {
                try {
                    if (checkCompleted()) {
                        // To prevent locks
                        post(new Runnable() {
                            public void run() {
                                setCompleted();
                            }
                        });
                    } else {
                        // To prevent locks
                        post(new Runnable() {
                            public void run() {
                                for (Download d : getSources()) {
                                    logSevere("Source: " + d);
                                }
                                setBroken(TransferProblem.MD5_ERROR,
                                    "File hash mismatch!");
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    log.finer("InterruptedException:" + e);
                }
            }
        }, "Downloadmanager file checker");
        worker.start();
        if (Thread.interrupted()) {
            worker.interrupt();
        }
    }

    private void deleteMetaData() {
        // logWarning("deleteMetaData()");
        if (getMetaFile() != null && getMetaFile().exists()
            && !getMetaFile().delete())
        {
            logSevere("Couldn't delete meta data file!");
        }
    }

    /**
     * @param id
     * @return
     * @throws Error
     */
    private String getFileID() throws Error {
        if (fileID == null) {
            try {
                fileID = new String(Util.encodeHex(Util.md5(getFileInfo()
                    .getName().getBytes("UTF8"))));
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
        }
        return fileID;
    }

    /**
     * Returns the base directory for transfer specific meta data. If the
     * directory doesn't exist, it's created.
     * 
     * @return the base directory
     * @throws IOException
     *             if the directory couldn't be created
     */
    private File getMetaDataBaseDir() throws IOException {
        File baseDir = new File(getFileInfo().getFolder(
            getController().getFolderRepository()).getSystemSubDir(),
            "transfers");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IOException(
                "Couldn't create base directory for transfer meta data!");
        }
        return baseDir;
    }

    private File getMetaFile() {
        File diskFile = getFileInfo().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        if (metaFile == null) {
            try {
                metaFile = new File(getMetaDataBaseDir(),
                    FileUtils.DOWNLOAD_META_FILE + getFileID());
            } catch (IOException e) {
                logSevere("IOException", e);
                return null;
            }
        }
        // metaFile = new File(diskFile.getParentFile(),
        // FileUtils.DOWNLOAD_META_FILE + diskFile.getName());
        return metaFile;
    }

    private void illegalState(String operation) {
        throw new IllegalStateException(operation + " not allowed in state "
            + state);
    }

    private boolean isAborted() {
        return state == InternalState.ABORTED;
    }

    /**
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void killTempFile() throws FileNotFoundException, IOException {
        if (getTempFile() != null && getTempFile().exists()
            && !getTempFile().delete())
        {
            log
                .warning("Couldn't delete old temporary file, some other process could be using it! Trying to set it's length to 0.");
            RandomAccessFile f = new RandomAccessFile(getTempFile(), "rw");
            try {
                f.setLength(0);
            } finally {
                f.close();
            }
        }
    }

    private void loadMetaData() throws IOException {
        // logWarning("loadMetaData()");

        if (getTempFile() == null
            || !getTempFile().exists()
            || !Util.equalsFileDateCrossPlattform(fileInfo.getModifiedDate()
                .getTime(), getTempFile().lastModified()))
        {
            // If something's wrong with the tempfile, kill the meta data file
            // if it exists
            deleteMetaData();
            killTempFile();
            return;
        }

        File mf = getMetaFile();
        if (mf == null || !mf.exists()) {
            killTempFile();
            return;
        }

        ObjectInputStream in = new ObjectInputStream(new FileInputStream(mf));
        try {
            FileInfo fi = (FileInfo) in.readObject();
            if (fi.isCompletelyIdentical(fileInfo)) {
                List<?> content = (List<?>) in.readObject();
                for (Object o : content) {
                    if (o.getClass() == FilePartsState.class) {
                        setFilePartsState((FilePartsState) o);
                    } else if (o.getClass() == FilePartsRecord.class) {
                        remotePartRecord = (FilePartsRecord) o;
                    }
                }
            } else {
                in.close();
                killTempFile();
                deleteMetaData();
            }
        } catch (ClassCastException e) {
            remotePartRecord = null;
            filePartsState = null;
            in.close();
            deleteMetaData();
        } catch (ClassNotFoundException e) {
            remotePartRecord = null;
            filePartsState = null;
            in.close();
            deleteMetaData();
        } finally {
            in.close();
        }

        if (filePartsState != null) {
            logInfo("Resuming download - already got "
                + filePartsState.countPartStates(filePartsState.getRange(),
                    PartState.AVAILABLE) + " of " + getFileInfo().getSize());
        }
    }

    private void protocolStateError(final Download cause, String operation)
        throws BrokenDownloadException
    {
        String msg = "PROTOCOL ERROR caused by " + cause + ": " + operation
            + " not allowed in state " + state;
        msg += " use DS: "
            + Util.useDeltaSync(getController(), cause.getPartner())
            + " use Swarm: "
            + Util.useSwarming(getController(), cause.getPartner());
        log.warning(msg);

        throw new BrokenDownloadException(msg);
    }

    private void readyForRequests0(final Download download)
        throws BrokenDownloadException
    {
        switch (state) {
            case CHECKING_FILE_VALIDITY :
            case MATCHING_AND_COPYING :
                // Do nothing, action will be taken after the worker is done
                break;
            case ACTIVE_DOWNLOAD :
                sendPartRequests();
                break;
            case WAITING_FOR_FILEPARTSRECORD :
                // Maybe request from different source
                requestFilePartsRecord(download);
                break;
            case WAITING_FOR_UPLOAD_READY :
                if (isNeedingFilePartsRecord()
                    && Util
                        .useDeltaSync(getController(), download.getPartner()))
                {
                    setState(InternalState.WAITING_FOR_FILEPARTSRECORD);
                    requestFilePartsRecord(download);
                } else {
                    if (filePartsState == null) {
                        setFilePartsState(new FilePartsState(fileInfo.getSize()));
                    }
                    if (filePartsState.isCompleted()) {
                        logFine("Not requesting anything, seems to be a zero file: "
                            + fileInfo);
                        checkFileValidity();
                    } else {
                        if (isFiner()) {
                            logFiner("Not requesting record for this download.");
                        }
                        startActiveDownload();
                    }
                }
                break;
            case COMPLETED :
            case BROKEN :
            case ABORTED :
                download.abort();
                break;
            default :
                protocolStateError(download, "readyForRequests");
                break;
        }
    }

    /**
     * @return true, if the download was completed by this chunk
     */
    private void receivedChunk0(final Download download, FileChunk chunk)
        throws BrokenDownloadException
    {
        switch (state) {
            case ABORTED :
            case BROKEN :
                log.fine("Aborted download of " + fileInfo
                    + " received chunk from " + download);
                download.abort();
                break;
            case ACTIVE_DOWNLOAD :
                storeFileChunk(download, chunk);
                if (filePartsState.isCompleted()) {
                    checkFileValidity();
                } else {
                    sendPartRequests();
                }
                break;
            default :
                protocolStateError(download, "receivedChunk");
                break;
        }
    }

    private void receivedFilePartsRecord0(Download download,
        final FilePartsRecord record) throws BrokenDownloadException
    {
        switch (state) {
            case WAITING_FOR_FILEPARTSRECORD :
                assert worker == null || !worker.isAlive();
                log.fine("Matching and copying...");
                setState(InternalState.MATCHING_AND_COPYING);
                remotePartRecord = record;
                worker = new Thread(new Runnable() {
                    public void run() {
                        try {
                            matchAndCopyData();
                            if (isDone()) {
                                return;
                            }
                            if (filePartsState.isCompleted()) {
                                checkFileValidity();
                            } else {
                                if (getSources().isEmpty()) {
                                    throw new BrokenDownloadException(
                                        "Out of sources");
                                }
                                if (!isDone()) {
                                    post(new Runnable() {

                                        public void run() {
                                            try {
                                                startActiveDownload();
                                            } catch (BrokenDownloadException e)
                                            {
                                                setBroken(
                                                    TransferProblem.IO_EXCEPTION,
                                                    e.toString());
                                            }
                                        }

                                    });
                                }
                            }
                        } catch (final BrokenDownloadException e) {
                            post(new Runnable() {
                                public void run() {
                                    setBroken(TransferProblem.IO_EXCEPTION, e
                                        .toString());
                                }
                            });
                        } catch (InterruptedException e) {
                            log.finer("InterruptedException: " + e);
                        }
                    }
                }, "Downloadmanager matching and copying");
                worker.start();
                break;
            default :
                protocolStateError(download, "receivedFilePartsRecord");
                break;
        }
    }

    private void removeSource0(final Download download)
        throws BrokenDownloadException
    {
        switch (state) {
            case WAITING_FOR_FILEPARTSRECORD :
                removeSourceImpl(download);
                requestFilePartsRecord(null);
                break;
            case WAITING_FOR_UPLOAD_READY :
                removeSourceImpl(download);
                if (!hasSources()) {
                    // If we're out of sources, wait for additional ones
                    // again
                    // Actually the TransferManager will break this
                    // transfer, but with
                    // the following code this manager could also be reused.
                    setState(InternalState.WAITING_FOR_SOURCE);
                }
                break;
            case ACTIVE_DOWNLOAD :
                removeSourceImpl(download);
                if (hasSources()) {
                    sendPartRequests();
                }
                break;
            case MATCHING_AND_COPYING :
            case CHECKING_FILE_VALIDITY :
            case BROKEN :
            case ABORTED :
            case COMPLETED : // If a remote side sends an abort this can
                // happen
                removeSourceImpl(download);
                break;
            default :
                illegalState("removeSource");
                break;
        }
    }

    private void saveMetaData() throws IOException {
        assert state != InternalState.COMPLETED;

        // logWarning("saveMetaData()");
        File mf = getMetaFile();
        if (mf == null && !isCompleted()) {
            killTempFile();
            return;
        }

        ObjectOutputStream out = new ObjectOutputStream(
            new FileOutputStream(mf));
        try {
            out.writeObject(fileInfo);
            List<Object> list = new LinkedList<Object>();
            if (filePartsState != null) {
                filePartsState.purgePending();
                list.add(filePartsState);
            }
            if (remotePartRecord != null) {
                list.add(remotePartRecord);
            }
            out.writeObject(list);
        } finally {
            out.close();
        }
    }

    private void setAborted(boolean cleanup) {
        assert !isAborted();

        // Might have been completed/broken
        if (isDone()) {
            return;
        }
        log.fine("Download aborted: " + fileInfo);

        setState(InternalState.ABORTED);
        shutdown();

        if (cleanup) {
            try {
                killTempFile();
            } catch (FileNotFoundException e) {
                logSevere("FileNotFoundException", e);
            } catch (IOException e) {
                logSevere("IOException", e);
            }
            deleteMetaData();
        }

        for (Download d : getSources()) {
            d.abort();
        }

        post(new Runnable() {
            public void run() {
                getController().getTransferManager().downloadManagerAborted(
                    AbstractDownloadManager.this);
            }
        });
    }

    private void setState(InternalState newState) {

        assert Thread.holdsLock(this);

        if (newState == InternalState.WAITING_FOR_UPLOAD_READY) {
            assert filePartsState == null || !filePartsState.isCompleted();
        }

        // logWarning("STATE " + newState + " for " + fileInfo);
        state = newState;
    }

    private void updateTempFile() {
        assert getTempFile() != null && getTempFile().exists();

        // logFine("Updating tempfile modification date to: " +
        // getFileInfo().getModifiedDate());
        if (getTempFile() == null
            || !getTempFile().setLastModified(
                getFileInfo().getModifiedDate().getTime()))
        {
            logSevere("Failed to update modification date! Detail:" + this);
        }
    }
}