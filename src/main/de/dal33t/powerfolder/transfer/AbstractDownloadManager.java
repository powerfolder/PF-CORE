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
 * $Id: AbstractDownloadManager.java 18206 2012-02-29 02:52:40Z tot $
 */
package de.dal33t.powerfolder.transfer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.Base64;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProgressListener;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;
import de.dal33t.powerfolder.util.delta.MatchCopyWorker;
import de.dal33t.powerfolder.util.delta.MatchInfo;
import de.dal33t.powerfolder.util.delta.MatchResultWorker;

/**
 * Shared implementation of download managers. This class leaves details on what
 * to request from whom to the implementing class.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public abstract class AbstractDownloadManager extends PFComponent implements
    DownloadManager
{

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
        ACTIVE_DOWNLOAD, MATCHING_AND_COPYING, CHECKING_FILE_VALIDITY, PASSIVE_DOWNLOAD;

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

    /**
     * Only set on init(boolean).
     */
    private volatile InternalState state = null;

    private volatile boolean automatic;
    private volatile boolean started;
    private volatile boolean shutdown;

    private Path metaFile;

    private String fileID;

    private Path tempFile;

    private final TransferManager tm;

    private Path metaDataBaseDir;

    public AbstractDownloadManager(Controller controller, FileInfo file,
        boolean automatic)
    {
        Reject.noNullElements(controller, file);

        this.fileInfo = file;
        this.automatic = automatic;

        this.controller = controller;

        tm = controller.getTransferManager();
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

    public synchronized boolean addSource(Download download) {
        validateDownload(download);
        Reject.ifFalse(
            download.isCompleted() || canAddSource(download.getPartner()),
            "Illegal addSource() call!!");
        return addSource0(download);
    }

    public synchronized void chunkReceived(Download download, FileChunk chunk) {
        Reject.noNullElements(download, chunk);
        validateDownload(download);
        assert chunk.file.isVersionDateAndSizeIdentical(getFileInfo());
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
     * @return the transfer counter
     */
    public synchronized TransferCounter getCounter() {
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
    public synchronized Path getTempFile() {
        Path diskFile = getFileInfo().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        if (tempFile == null) {
            try {
                tempFile = getMetaDataBaseDir().resolve("(incomplete) "
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

    public boolean isStarted() {
        return started;
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

    @Override
    public String toString() {
        String tInfo = tempFile == null ? "n/a" : tempFile.toString();
        return "[" + getClass().getSimpleName() + "; state= " + state
            + " file=" + getFileInfo() + "; tempFileRAF: " + tempRAF
            + "; tempFile: " + tInfo + "; broken: " + isBroken()
            + "; completed: " + isCompleted() + "; aborted: " + isAborted()
            + "; partsState: " + filePartsState;
    }

    protected abstract void addSourceImpl(Download source);

    protected boolean checkCompleted() {

        setTransferState(TransferState.VERIFYING);
        // logFine("Verifying file hash for " + this);
        try {
            FilePartsRecord thisRemotePartRecord = remotePartRecord;
            byte[] tempFileHash = null;
            if (thisRemotePartRecord != null) {
                tempFileHash = PathUtils.digest(getTempFile(),
                    MessageDigest.getInstance("MD5"), new ProgressListener() {
                        public void progressReached(double percentageReached) {
                            setTransferState(percentageReached / 100.0);
                        }

                    });
            }
            // If we don't have a record, no hashing was performed and the file
            // is assumed to be "valid"
            if (tempFileHash == null
                || Arrays.equals(thisRemotePartRecord.getFileDigest(),
                    tempFileHash))
            {
                return true;
            }
            logFine("Checksum test FAILED on " + fileInfo.toDetailString()
                + ". MD5 found: " + Base64.encodeBytes(tempFileHash)
                + " expected: "
                + Base64.encodeBytes(thisRemotePartRecord.getFileDigest()));
            counter = new TransferCounter(0, fileInfo.getSize());
            // filePartsState.setPartState(Range.getRangeByLength(0,
            // filePartsState.getFileLength()), PartState.NEEDED);
            filePartsState = null;
            // Maybe part record was bogus.
            remotePartRecord = null;

            return false;
        } catch (NoSuchAlgorithmException e) {
            // If this error occurs, no downloads will ever succeed.
            logSevere("NoSuchAlgorithmException", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logSevere("Exception", e);
            setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
        }
        return false;
    }

    protected Path getFile() {
        return fileInfo.getDiskFile(getController().getFolderRepository());
    }

    /**
     * Call this after construction. Otherwise download might not have tempfile
     * ready. Does not prepare tempfile if completed
     * 
     * @param completed
     *            if this download is already completed.
     * @throws IOException
     */
    public synchronized void init(boolean completed) throws IOException {
        assert fileInfo != null;
        if (completed) {
            setTransferState(TransferState.DONE, 1);
            state = InternalState.COMPLETED;
        } else {
            setTransferState(TransferState.NONE);
            state = InternalState.WAITING_FOR_SOURCE;
        }

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
        assert Files.exists(getTempFile().getParent()) : "Missing PowerFolder system directory";

        // Create temp-file directory structure if necessary
        // if (!getTempFile().getParentFile().exists()) {
        // if (!getTempFile().getParentFile().mkdirs()) {
        // throw new FileNotFoundException(
        // "Couldn't create parent directory!");
        // }
        // }

        loadMetaData();

        if (isFiner()) {
            logFiner("Init tempfile at " + getTempFile());
        }
        tempRAF = new RandomAccessFile(getTempFile().toFile(), "rw");
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
            Path src = getFile();

            setTransferState(TransferState.MATCHING);
            ProgressListener transferObs = new ProgressListener() {
                public void progressReached(double percentageReached) {
                    setTransferState(percentageReached);
                }
            };
            Callable<List<MatchInfo>> mInfoWorker = new MatchResultWorker(
                remotePartRecord, src, transferObs);
            List<MatchInfo> mInfoRes = null;
            mInfoRes = mInfoWorker.call();

            // logFine("Records: " + record.getInfos().length);
            if (isFine()) {
                logFine("Matches: "
                    + mInfoRes.size()
                    + " which are "
                    + Format.formatBytes(remotePartRecord.getPartLength()
                        * mInfoRes.size()) + " bytes (bit less maybe) on "
                    + fileInfo.toDetailString());
            }
            setTransferState(TransferState.COPYING);
            Callable<FilePartsState> pStateWorker = new MatchCopyWorker(src,
                getTempFile(), remotePartRecord, mInfoRes, transferObs);
            FilePartsState calcedState = pStateWorker.call();
            if (calcedState.getFileLength() != fileInfo.getSize()) {
                // Concurrent file modification
                throw new BrokenDownloadException();
            }
            setFilePartsState(calcedState);
            counter = new TransferCounter(filePartsState.countPartStates(
                filePartsState.getRange(), PartState.AVAILABLE),
                fileInfo.getSize());

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

    protected synchronized void setAutomatic(boolean auto) {
        automatic = auto;
    }

    public synchronized void setBroken(final TransferProblem problem,
        final String message)
    {
        if (isBroken()) {
            return;
        }
        if (isFine()) {
            logFine("Download broken: " + fileInfo.toDetailString()
                + ". Problem: " + problem + ": " + message);
        }
        setState(InternalState.BROKEN);
        shutdown();

        if (problem.equals(TransferProblem.MD5_ERROR)) {
            try {
                deleteTempFile();
            } catch (IOException e) {
                logSevere("Unable to remove tempfile on MD5_ERROR: "
                    + getTempFile().toString() + ". " + e, e);
            }
        } else {
            try {
                if (getTempFile() != null && Files.exists(getTempFile())
                    && Files.size(getTempFile()) == 0)
                {
                    if (isFiner()) {
                        logFiner("Deleting tempfile with size 0.");
                    }
                    Files.delete(getTempFile());
                }
            } catch (IOException ioe) {
                logWarning("Failed to delete temp file: "
                    + getTempFile().toAbsolutePath().toString());
            }
        }
        final Download sources[] = getSources().toArray(new Download[0]);
        for (Download d : sources) {
            d.setBroken(problem, message);
        }
        tm.downloadManagerBroken(AbstractDownloadManager.this, problem, message);
    }

    protected synchronized void setCompleted() {
        assert !isCompleted();

        // Might be broken/aborted in the mean time
        if (isDone()) {
            return;
        }

        if (isInfo()) {
            if (fileInfo.getFolderInfo().isMetaFolder()) {
                logFine("Download completed: " + fileInfo.toDetailString());    
            } else {
                logInfo("Download completed: " + fileInfo.toDetailString());
            }
        }

        setTransferState(TransferState.DONE, 1);
        setState(InternalState.COMPLETED);
        shutdown();
        deleteMetaData();
        tm.setCompleted(AbstractDownloadManager.this);
    }

    protected synchronized void setFilePartsState(FilePartsState state) {
        assert filePartsState == null;

        if (filePartsState != null) {
            logSevere("FilePartsState should've been null, but was "
                + filePartsState + " on " + fileInfo.toDetailString());
            Debug.dumpCurrentStackTrace();
        }

        filePartsState = state;
    }

    protected void setStarted() {
        started = true;
    }

    protected void setTransferState(double progress) {
        transferState.setProgress(progress);
        for (Download d : getSources()) {
            d.state.setProgress(progress);
        }
    }

    protected void setTransferState(TransferState state) {
        if (transferState.getState() == state) {
            return;
        }
        transferState.setState(state);
        transferState.setProgress(0);
        for (Download d : getSources()) {
            d.state.setState(state);
            d.state.setProgress(0);
        }
    }

    protected void setTransferState(TransferState state, double progress) {
        transferState.setState(state);
        transferState.setProgress(progress);
        for (Download d : getSources()) {
            d.state.setState(state);
            d.state.setProgress(progress);
        }
    }

    /**
     * Releases resources not required anymore
     */
    protected synchronized void shutdown() {
        assert isDone();
        if (shutdown) {
            return;
        }

        assert tempRAF != null;

        shutdown = true;
        if (isFiner()) {
            logFine("Shutting down " + fileInfo.toDetailString());
        }
        try {
            if (isFiner()) {
                logFiner("Closing temp file: " + getTempFile() + " of "
                    + getFile());
            }
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
        assert !isCompleted() && !isAborted() || !hasMetaFile();
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
        setTransferState(TransferState.DOWNLOADING, fileInfo.getSize() > 0
            ? (double) avs / fileInfo.getSize()
            : 1);

        // add bytes to transferred status
        FolderStatistic stat = fileInfo.getFolder(
            getController().getFolderRepository()).getStatistic();
        if (stat != null) {
            stat.getDownloadCounter().chunkTransferred(chunk);
        }
    }

    /**
     * @return true, if the download was actually requested from the source.
     */
    private boolean addSource0(final Download download) {
        // This should be true because the addSource() caller should be
        // locking the calls
        assert download.isCompleted() || canAddSource(download.getPartner()) : "Illegal addSource() call!!";

        switch (state) {
            case BROKEN :
                // The connection could've broken while some other code
                // tries to add sources
                download.setBroken(TransferProblem.BROKEN_DOWNLOAD,
                    "Manager already broken!");
                return false;
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
                return true;
            case WAITING_FOR_SOURCE :
                // Required for complete on load downloads
                if (download.isCompleted()) {
                    setState(InternalState.COMPLETED);
                }

                addSourceImpl(download);

                if (isDone()) {
                    return false;
                }

                // Zero sized files hack: Shouldn't request anything
                if (getFileInfo().getSize() == 0) {
                    setCompleted();
                    return false;
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
                return true;
            case COMPLETED :
                addSourceImpl(download);
                if (!download.isCompleted() && !download.isBroken()) {
                    download.setCompleted();
                }
                return false;
            default :
                illegalState("addSource");
                return false;
        }
    }

    private void validateDownload(Download download) {
        Reject.ifNull(download, "Download is null!");
        if (!download.getFile().isVersionDateAndSizeIdentical(getFileInfo())) {
            throw new IllegalArgumentException("Download FileInfo differs: "
                + download.getFile().toDetailString() + " vs mine: "
                + getFileInfo().toDetailString());
        }
    }

    private synchronized void checkFileValidity() {
        assert state == InternalState.ACTIVE_DOWNLOAD
            || state == InternalState.WAITING_FOR_UPLOAD_READY
            && filePartsState.getFileLength() == 0
            || state == InternalState.MATCHING_AND_COPYING : "Invalid state: "
            + state;

        setState(InternalState.CHECKING_FILE_VALIDITY);
        tm.doWork(new Runnable() {
            public void run() {
                if (checkCompleted()) {
                    setCompleted();
                } else {
                    setBroken(TransferProblem.MD5_ERROR, "File hash mismatch");
                }
            }
        });
    }

    private void deleteMetaData() {
        if (getMetaFile() != null) {
            try {
                Files.deleteIfExists(getMetaFile());
            } catch (IOException ioe) {
                if (isSevere() && Files.exists(getMetaFile())) {
                    logSevere("Couldn't delete meta data file!");
                }
            }
        }
    }

    /**
     * @param id
     * @return
     * @throws Error
     */
    private String getFileID() throws Error {
        if (fileID == null) {
            fileID = new String(Util.encodeHex(Util.md5(getFileInfo()
                .getRelativeName().getBytes(Convert.UTF8))));
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
    private Path getMetaDataBaseDir() throws IOException {
        if (metaDataBaseDir != null) {
            return metaDataBaseDir;
        }

        // FIXME: Implement
        boolean workAroundTrueZIP = getFileInfo().getFolder(
            getController().getFolderRepository()).isEncrypted();
        if (workAroundTrueZIP) {
            metaDataBaseDir = Paths.get(System.getProperty("tmp.dir"),
                "transfers").toAbsolutePath();
        } else {
            metaDataBaseDir = getFileInfo()
                .getFolder(getController().getFolderRepository())
                .getSystemSubDir().resolve("transfers").toAbsolutePath();
        }
        if (Files.notExists(metaDataBaseDir)) {
            try {
                Files.createDirectories(metaDataBaseDir);
            } catch (IOException ioe) {
                throw new IOException(
                    "Couldn't create base directory for transfer meta data!");
            }
        }
        return metaDataBaseDir;
    }

    private boolean hasMetaFile() {
        return getMetaFile() != null && Files.exists(getMetaFile());
    }

    private Path getMetaFile() {
        if (metaFile != null) {
            return metaFile;
        }
        Path diskFile = getFileInfo().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        try {
            metaFile = getMetaDataBaseDir().resolve(
                PathUtils.DOWNLOAD_META_FILE + getFileID()).toAbsolutePath();
            return metaFile;
        } catch (IOException e) {
            logSevere("IOException", e);
            return null;
        }
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
    private void deleteTempFile() throws IOException {
        boolean exists = getTempFile() != null && Files.exists(getTempFile());
        if (exists && isFine()) {
            logFine("killTempFile: " + getTempFile() + ", size: "
                + Files.size(getTempFile()));
        }
        try {
            Files.deleteIfExists(getTempFile());
        } catch (IOException e) {
            if (isWarning()) {
                logWarning("Couldn't delete old temporary file, some other process could be using it! Trying to set it's length to 0. for file: "
                    + getFileInfo().toDetailString());
            }
            RandomAccessFile f = new RandomAccessFile(getTempFile().toFile(), "rw");
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
            || Files.notExists(getTempFile())
            || !DateUtil.equalsFileDateCrossPlattform(fileInfo
                .getModifiedDate().getTime(), Files.getLastModifiedTime(getTempFile()).toMillis()))
        {
            // If something's wrong with the tempfile, kill the meta data file
            // if it exists
            deleteMetaData();
            deleteTempFile();
            return;
        }

        Path mf = getMetaFile();
        if (mf == null || Files.notExists(mf)) {
            deleteTempFile();
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(mf))) {
            FileInfo fi = (FileInfo) in.readObject();
            if (fi.isVersionDateAndSizeIdentical(fileInfo)) {
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
                deleteTempFile();
                deleteMetaData();
            }
        } catch (Exception e) {
            remotePartRecord = null;
            filePartsState = null;
            deleteMetaData();
        }

        if (filePartsState != null) {
            if (isInfo()) {
                logInfo("Resuming download - already got "
                    + filePartsState.countPartStates(filePartsState.getRange(),
                        PartState.AVAILABLE) + " of " + getFileInfo().getSize());
            }
        }
    }

    private void protocolStateError(final Download cause, String operation)
        throws BrokenDownloadException
    {
        String msg = "PROTOCOL ERROR caused by " + cause + ": " + operation
            + " not allowed in state " + state;
        msg += " use DS: " + Util.useDeltaSync(getController(), cause)
            + " use Swarm: "
            + Util.useSwarming(getController(), cause.getPartner());
        logWarning(msg);

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
                    && Util.useDeltaSync(getController(), download))
                {
                    setState(InternalState.WAITING_FOR_FILEPARTSRECORD);
                    requestFilePartsRecord(download);
                } else {
                    if (filePartsState == null) {
                        setFilePartsState(new FilePartsState(fileInfo.getSize()));
                    }
                    if (filePartsState.isCompleted()) {
                        if (isFine()) {
                            logFine("Not requesting anything, seems to be a zero file: "
                                + fileInfo);
                        }
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

    private void receivedChunk0(final Download download, FileChunk chunk)
        throws BrokenDownloadException
    {
        switch (state) {
            case ABORTED :
            case BROKEN :
                if (isFine()) {
                    logFine("Aborted download of " + fileInfo
                        + " received chunk from " + download);
                }
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
            case PASSIVE_DOWNLOAD :
                storeFileChunk(download, chunk);
                if (filePartsState.isCompleted()) {
                    setCompleted();
                }
                break;
            case WAITING_FOR_UPLOAD_READY :
                setState(InternalState.PASSIVE_DOWNLOAD);
                setFilePartsState(new FilePartsState(fileInfo.getSize()));
                filePartsState.setPartState(filePartsState.getRange(),
                    PartState.NEEDED);
                receivedChunk0(download, chunk);
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
                if (isFine()) {
                    logFine("Matching and copying..."
                        + fileInfo.toDetailString());
                }
                setState(InternalState.MATCHING_AND_COPYING);
                remotePartRecord = record;

                tm.doWork(new Runnable() {
                    public void run() {
                        try {
                            matchAndCopyData();
                            if (isDone()) {
                                return;
                            }
                            if (filePartsState.isCompleted()) {
                                checkFileValidity();
                            } else {
                                // Protect empty check
                                synchronized (AbstractDownloadManager.this) {
                                    if (getSources().isEmpty()) {
                                        throw new BrokenDownloadException(
                                            "Out of sources");
                                    }
                                    if (!isDone()) {
                                        try {
                                            startActiveDownload();
                                        } catch (BrokenDownloadException e) {
                                            setBroken(
                                                TransferProblem.IO_EXCEPTION,
                                                e.toString());
                                        }
                                    }
                                }
                            }
                        } catch (final BrokenDownloadException e) {
                            setBroken(TransferProblem.IO_EXCEPTION,
                                e.toString());
                        } catch (InterruptedException e) {
                            logFiner("InterruptedException", e);
                        }
                    }
                });
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
            case PASSIVE_DOWNLOAD :
                removeSourceImpl(download);

                if (!isDone()) {
                    setBroken(TransferProblem.BROKEN_DOWNLOAD, "Source lost.");
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
        Path mf = getMetaFile();
        if (mf == null && !isCompleted()) {
            deleteTempFile();
            return;
        }

        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(mf))) {
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
        }
    }

    private void setAborted(boolean cleanup) {
        assert !isAborted();
        assert Thread.holdsLock(this);

        // Might have been completed/broken
        if (isDone()) {
            return;
        }
        if (isFine()) {
            logFine("Download aborted: " + fileInfo);
        }

        setState(InternalState.ABORTED);
        shutdown();

        if (cleanup) {
            try {
                deleteTempFile();
            } catch (FileNotFoundException e) {
                logSevere("FileNotFoundException", e);
            } catch (IOException e) {
                logSevere("IOException", e);
            }
            deleteMetaData();
        }
        // Prevent ConcurrentModificiation Exceptions
        final Download sources[] = getSources().toArray(new Download[0]);
        for (Download d : sources) {
            d.abort();
        }
        tm.downloadManagerAborted(AbstractDownloadManager.this);
    }

    private void setState(InternalState newState) {

        assert Thread.holdsLock(this);

        if (newState == InternalState.WAITING_FOR_UPLOAD_READY) {
            assert filePartsState == null || !filePartsState.isCompleted();
        }

        if (isFiner()) {
            logFiner("State change to " + newState + ": "
                + getFileInfo().toDetailString());
        }

        state = newState;
    }

    private void updateTempFile() {
        if (getTempFile() != null) {
            try {
                Files.setLastModifiedTime(getTempFile(), FileTime
                    .fromMillis(getFileInfo().getModifiedDate().getTime()));
                return;
            } catch (IOException ioe) {
                logSevere("Failed to update modification date! Detail:" + this);
                // print message
            }

        }
    }
}
