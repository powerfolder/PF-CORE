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
import java.util.concurrent.locks.ReentrantLock;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.FileCheckWorker;
import de.dal33t.powerfolder.util.FileUtils;
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
    private enum InternalState {
        WAITING_FOR_SOURCE, WAITING_FOR_UPLOAD_READY, WAITING_FOR_FILEPARTSRECORD,

        COMPLETED, BROKEN, ABORTED,

        /**
         * Receive chunks in linear fashion from one source only
         */
        PASSIVE_DOWNLOAD,

        /**
         * Request chunks
         */
        ACTIVE_DOWNLOAD, MATCHING_AND_COPYING, CHECKING_FILE_VALIDITY;
    }

    protected FilePartsState filePartsState;

    protected FilePartsRecord remotePartRecord;
    
    /**
     * Hold this while changing states.
     * If you want to call "dangerous" methods while holding this lock, 
     * use post(...) to call them.
     * All methods that may themselves post "events" are dangerous. 
     * For example methods that will send requests or aborts don't need to
     * locked (since you don't know when it gets sent anyways), so use
     * post on them.
     */
    private ReentrantLock lock = new ReentrantLock();

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

        this.fileInfo = file;
        this.automatic = automatic;

        this.controller = controller;

        init();
    }

    public void abort() {
        // illegalState("abort");
        switch (state) {
            case ABORTED :
            case BROKEN :
            case COMPLETED :
                break;
            default :
                setAborted(false);
                break;
        }
    }

    public void abortAndCleanup() {
        // illegalState("abortAndCleanup");
        switch (state) {
            case ABORTED :
            case BROKEN :
            case COMPLETED :
                break;
            default :
                setAborted(true);
                break;
        }
    }

    public void addSource(Download download) {
        Reject.ifNull(download, "Download is null!");
        Reject.ifFalse(download.isCompleted()
            || allowsSourceFor(download.getPartner()),
            "Illegal addSource() call!!");
        try {
            if (addSource0(download)) {
                setCompleted();
            }
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public void broken(String reason) {
        setBroken(TransferProblem.BROKEN_DOWNLOAD, reason);
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
        lock.lock();
        try {
            if (counter == null) {
                counter = new TransferCounter(0, fileInfo.getSize());
            }
            return counter;
        } finally {
            lock.unlock();
        }
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
                tempFile = new File(getMetaDataBaseDir(),
                    "(incomplete) " + getFileID());
            } catch (IOException e) {
                log().error(e);
                return null;
            }
        }
        return tempFile;
    }

    /**
     * Returns the base directory for transfer specific meta data.
     * If the directory doesn't exist, it's created.
     * @return the base directory
     * @throws IOException if the directory couldn't be created
     */
    private File getMetaDataBaseDir() throws IOException {
        File baseDir = new File(getFileInfo().getFolder(
            getController().getFolderRepository()).getSystemSubDir(),
            "transfers");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IOException("Couldn't create base directory for transfer meta data!");
        }
        return baseDir;
    }

    public boolean isBroken() {
        return state == InternalState.BROKEN;
    }

    public boolean isCompleted() {
        return state == InternalState.COMPLETED;
    }

    public boolean isDone() {
        switch (state) {
            case ABORTED :
            case BROKEN :
            case COMPLETED :
                return true;
        }
        return false;
    }

    public boolean isRequestedAutomatic() {
        return automatic;
    }

    public boolean isShutDown() {
        return shutdown;
    }

    public boolean isStarted() {
        return started;
    }

    public void readyForRequests(Download download) {
        Reject.ifNull(download, "Download is null!!!");
        try {
            readyForRequests0(download);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        } catch (AssertionError e) {
            log().error(e);
            throw e;
        }
    }

    public void receivedChunk(Download download, FileChunk chunk) {
        Reject.noNullElements(download, chunk);
        assert chunk.file.isCompletelyIdentical(getFileInfo());
        try {
            if (receivedChunk0(download, chunk)) {
                setCompleted();
            }
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public void receivedFilePartsRecord(Download download,
        FilePartsRecord record)
    {
        Reject.noNullElements(download, record);
        try {
            receivedFilePartsRecord0(download, record);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    public void removeSource(Download download) {
        Reject.ifNull(download, "Download is null!");
        try {
            removeSource0(download);
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "; state= " + state
            + " file=" + getFileInfo() + "; tempFileRAF: " + tempRAF
            + "; tempFile: " + getTempFile() + "; broken: " + isBroken()
            + "; completed: " + isCompleted() + "; aborted: " + isAborted();
    }

    protected abstract void addSourceImpl(Download source);

    protected boolean checkCompleted() {
        assert !lock.isHeldByCurrentThread();

        setTransferState(TransferState.VERIFYING);
        // log().debug("Verifying file hash for " + this);
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
            log().warn("Checking FAILED");
            counter = new TransferCounter(0, fileInfo.getSize());
            filePartsState.setPartState(Range.getRangeByLength(0,
                filePartsState.getFileLength()), PartState.NEEDED);

            return false;
        } catch (NoSuchAlgorithmException e) {
            // If this error occurs, no downloads will ever succeed.
            log().error(e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log().verbose(e);
        } catch (Exception e) {
            log().error(e);
            setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
        }
        return false;
    }

    protected File getFile() {
        return fileInfo.getDiskFile(getController().getFolderRepository());
    }

    protected void init() throws IOException {
        assert fileInfo != null;

        if (getTempFile() == null) {
            throw new IOException("Couldn't create a temporary file for "
                + fileInfo);
        }

        // If it's an old download, don't create a temporary file
        if (isCompleted()) {
            return;
        }

        // This has to happen here since "completed" is valid
        assert !isDone() : "File broken/aborted before init!";
        assert getTempFile().getParentFile().exists() : "Missing PowerFolder system directory";
        
        // Create temp-file directory structure if necessary
//        if (!getTempFile().getParentFile().exists()) {
//            if (!getTempFile().getParentFile().mkdirs()) {
//                throw new FileNotFoundException(
//                    "Couldn't create parent directory!");
//            }
//        }

        lock.lock();
        try {
            loadMetaData();
        } finally {
            lock.unlock();
        }

        tempRAF = new RandomAccessFile(getTempFile(), "rw");
    }

    protected boolean isNeedingFilePartsRecord() {
        return !isCompleted() && remotePartRecord == null
            && fileInfo.getSize() >= Constants.MIN_SIZE_FOR_PARTTRANSFERS
            && fileInfo.diskFileExists(getController());
    }

    protected abstract boolean isUsingPartRequests();

    protected void matchAndCopyData() throws BrokenDownloadException,
        InterruptedException
    {
        try {
            File src = getFile();

            setTransferState(TransferState.MATCHING);
            Callable<List<MatchInfo>> mInfoWorker = new MatchResultWorker(
                remotePartRecord, src)
            {

                @Override
                protected void setProgress(int percent) {
                    setTransferState(percent / 100.0);
                }
            };
            List<MatchInfo> mInfoRes = null;
            mInfoRes = mInfoWorker.call();

            // log().debug("Records: " + record.getInfos().length);
            log().debug(
                "Matches: " + mInfoRes.size() + " which are "
                    + (remotePartRecord.getPartLength() * mInfoRes.size())
                    + " bytes (bit less maybe).");

            setTransferState(TransferState.COPYING);
            Callable<FilePartsState> pStateWorker = new MatchCopyWorker(src,
                getTempFile(), remotePartRecord, mInfoRes)
            {
                @Override
                protected void setProgress(int percent) {
                    setTransferState(percent / 100.0);
                }
            };
            FilePartsState state = pStateWorker.call();
            if (state.getFileLength() != fileInfo.getSize()) {
                // Concurrent file modification
                throw new BrokenDownloadException();
            }

            lock.lock();
            try {
                setFilePartsState(state);
                counter = new TransferCounter(filePartsState.countPartStates(
                    filePartsState.getRange(), PartState.AVAILABLE), fileInfo
                    .getSize());
            } finally {
                lock.unlock();
            }

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

    protected abstract void sendPartRequests() throws BrokenDownloadException;

    protected void setAutomatic(boolean auto) {
        automatic = auto;
    }

    protected void setBroken(final TransferProblem problem, final String message)
    {
        assert !lock.isHeldByCurrentThread();
        lock.lock();
        try {
            if (isBroken()) {
                return;
            }
            log().debug("Download broken: " + fileInfo);
            setState(InternalState.BROKEN);
            shutdown();
        } finally {
            lock.unlock();
        }

        for (Download d : getSources()) {
            getController().getTransferManager().setBroken(d, problem, message);
        }

        getController().getTransferManager().setBroken(
            AbstractDownloadManager.this, problem, message);
    }

    protected void setCompleted() {
        assert !lock.isHeldByCurrentThread();
        assert !isCompleted();

        lock.lock();
        try {
            // Might be broken/aborted in the mean time
            if (isDone()) {
                return;
            }

            log().debug("Completed download of " + fileInfo + ".");

            setState(InternalState.COMPLETED);
            shutdown();
            deleteMetaData();
        } finally {
            lock.unlock();
        }
        // Should be called without locking:
        // It's more "event" style and prevents deadlocks
        getController().getTransferManager().setCompleted(
            AbstractDownloadManager.this);

        for (Download d : getSources()) {
            getController().getTransferManager().setCompleted(d);
        }
    }

    protected void setFilePartsState(FilePartsState state) {
        assert lock.isHeldByCurrentThread();
        assert filePartsState == null;

        filePartsState = state;
    }

    protected void setStarted() {
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
    protected void shutdown() {
        assert lock.isHeldByCurrentThread();
        assert isDone();
        if (isShutDown()) {
            return;
        }

        assert tempRAF != null;

        shutdown = true;
        // log().debug("Shutting down " + fileInfo);
        try {
            if (worker != null) {
                worker.interrupt();
            }
            // log().error("Closing temp file!");
            tempRAF.close();
            tempRAF = null;

            if (isBroken()) {
                saveMetaData();
            } else {
                deleteMetaData();
            }
        } catch (IOException e) {
            log().error(e);
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

    protected void startActiveDownload() throws BrokenDownloadException {
        assert !getSources().isEmpty();
        assert !isDone();
        assert Util.usePartRequests(getController(), getSources().iterator()
            .next().getPartner());
        assert lock.isHeldByCurrentThread();

        setState(InternalState.ACTIVE_DOWNLOAD);
        validateDownload();

        log().debug("Requesting parts");
        post(new Runnable() {
            public void run() {
                try {
                    sendPartRequests();
                } catch (BrokenDownloadException e) {
                    setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
                }
            }
        });
    }

    protected void storeFileChunk(Download download, FileChunk chunk) {
        assert download != null;
        assert chunk != null;
        assert getSources().contains(download) : "Invalid source!";
        assert lock.isHeldByCurrentThread();

        setStarted();

        try {
            tempRAF.seek(chunk.offset);
            tempRAF.write(chunk.data);
        } catch (IOException e) {
            log().error(e);
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

    protected void updateTempFile() {
        assert getTempFile() != null && getTempFile().exists();

        // log().debug("Updating tempfile modification date to: " +
        // getFileInfo().getModifiedDate());
        if (!getTempFile().setLastModified(
            getFileInfo().getModifiedDate().getTime()))
        {
            log().error(
                "Failed to update modification date! Detail:"
                    + this);
        }
    }

    /**
     * @return true, if the download was completed by this call
     */
    private boolean addSource0(final Download download)
        throws BrokenDownloadException
    {
        lock.lock();
        try {
            // This should be true because the addSource() caller should be
            // locking the calls
            assert download.isCompleted()
                || allowsSourceFor(download.getPartner()) : "Illegal addSource() call!!";

            validateDownload();
            switch (state) {
                case BROKEN:
                    // The connection could've broken while some other code tries to add sources
                    break;
                case MATCHING_AND_COPYING :
                case CHECKING_FILE_VALIDITY :
                    addSourceImpl(download);

                    post(new Runnable() {
                        public void run() {
                            // Maybe rework the code so this request will be sent if we
                            // really need data
                            download.request(0);
                        }
                    });
                    break;
                case ACTIVE_DOWNLOAD :
                    addSourceImpl(download);
                    // We can use offset 0 here since the transfer will use
                    // requests
                    // anyways
                    post(new Runnable() {
                        public void run() {
                            download.request(0);
                        }
                    });
                    break;
                case WAITING_FOR_UPLOAD_READY :
                case WAITING_FOR_FILEPARTSRECORD :
                    addSourceImpl(download);
                    // We can use offset 0 here since the transfer will use
                    // requests
                    // anyways
                    post(new Runnable() {
                        public void run() {
                            download.request(0);
                        }
                    });
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

                    long offset = 0;
                    if (filePartsState != null) {
                        if (filePartsState.isCompleted()) {
                            return true;
                        }
                        Range range = filePartsState
                            .findFirstPart(PartState.NEEDED);
                        if (range != null) {
                            offset = range.getStart();
                        } else {
                            assert filePartsState.isCompleted()
                                || filePartsState
                                    .findFirstPart(PartState.PENDING) != null;
                        }
                    }
                    if (isUsingPartRequests()) {
                        setState(InternalState.WAITING_FOR_UPLOAD_READY);
                    } else {
                        setState(InternalState.PASSIVE_DOWNLOAD);
                        if (filePartsState == null) {
                            setFilePartsState(new FilePartsState(fileInfo
                                .getSize()));
                        }
                    }
                    post(new Runnable() {
                        public void run() {
                            download.request(0);
                        }
                    });
                    break;
                case COMPLETED :
                    assert download.isCompleted();
                    addSourceImpl(download);
                    break;
                default :
                    illegalState("addSource");
                    break;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    private void post(Runnable runnable) {
        getController().getThreadPool().execute(runnable);
    }

    /**
     * 
     */
    private void checkFileValidity() {
        assert state == InternalState.ACTIVE_DOWNLOAD
            || state == InternalState.WAITING_FOR_UPLOAD_READY && filePartsState.getFileLength() == 0
            || state == InternalState.MATCHING_AND_COPYING : "Invalid state: "
            + state;

        setState(InternalState.CHECKING_FILE_VALIDITY);
        worker = new Thread(new Runnable() {
            public void run() {
                if (checkCompleted()) {
                    setCompleted();
                } else {
                    setBroken(TransferProblem.MD5_ERROR, "File hash mismatch!");
                }
            }
        }, "Downloadmanager file checker");
        worker.start();
    }

    private void deleteMetaData() {
        // log().warn("deleteMetaData()");
        if (getMetaFile() != null && getMetaFile().exists()
            && !getMetaFile().delete())
        {
            log().error("Couldn't delete meta data file!");
        }
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
                log().error(e);
                return null;
            }
        }
//        metaFile = new File(diskFile.getParentFile(),
//            FileUtils.DOWNLOAD_META_FILE + diskFile.getName());
        return metaFile;
    }

    /**
     * @param id
     * @return
     * @throws Error
     */
    private String getFileID() throws Error {
        if (fileID == null) {
            try {
                fileID = new String(Util.encodeHex(
                    Util.md5(getFileInfo().getName().getBytes("ISO-8859-1"))));
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
        }
        return fileID;
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
            log()
                .warn(
                    "Couldn't delete old temporary file, some other process could be using it! Trying to set it's length to 0.");
            RandomAccessFile f = new RandomAccessFile(getTempFile(), "rw");
            f.setLength(0);
            f.close();
        }
    }

    private void loadMetaData() throws IOException {
        // log().warn("loadMetaData()");

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
            log()
                .info(
                    "Resuming download - already got "
                        + filePartsState.countPartStates(filePartsState
                            .getRange(), PartState.AVAILABLE) + " of "
                        + getFileInfo().getSize());
        }
    }

    private void protocolStateError(final Download cause, String operation)
        throws BrokenDownloadException
    {
        String msg = "PROTOCOL ERROR caused by " + cause + ": " + operation
            + " not allowed in state " + state;
        msg += " " + cause.getPartner().isSupportingPartTransfers();
        msg += " " + Util.useDeltaSync(getController(), cause.getPartner())
            + " " + Util.useSwarming(getController(), cause.getPartner());
        log().warn(msg);

        throw new BrokenDownloadException(msg);
    }

    private void readyForRequests0(final Download download)
        throws BrokenDownloadException
    {
        lock.lock();
        try {
            validateDownload();
            switch (state) {
                case CHECKING_FILE_VALIDITY :
                case MATCHING_AND_COPYING :
                    // Do nothing, action will be taken after the worker is done
                    break;
                case ACTIVE_DOWNLOAD :
                    post(new Runnable() {
                        public void run() {
                            try {
                                sendPartRequests();
                            } catch (BrokenDownloadException e) {
                                setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
                            }
                        }
                    });
                    break;
                case WAITING_FOR_FILEPARTSRECORD :
                    // Maybe request from different source
                    post(new Runnable() {
                        public void run() {
                            requestFilePartsRecord(download);
                        }
                    });
                    break;
                case WAITING_FOR_UPLOAD_READY :
                    if (isNeedingFilePartsRecord()
                        && Util.useDeltaSync(getController(), download
                            .getPartner()))
                    {
                        setState(InternalState.WAITING_FOR_FILEPARTSRECORD);
                        post(new Runnable() {
                            public void run() {
                                requestFilePartsRecord(download);
                            }
                        });
                    } else {
                        if (filePartsState == null) {
                            setFilePartsState(new FilePartsState(fileInfo
                                .getSize()));
                        }
                        if (filePartsState.isCompleted()) {
                            log().debug("Not requesting anything, seems to be a zero file: " + fileInfo);
                            checkFileValidity();
                        } else {
                            log().debug("Not requesting record for this download.");
                            startActiveDownload();
                        }
                    }
                    break;
                case BROKEN :
                case ABORTED :
                    post(new Runnable() {
                        public void run() {
                            download.abort();
                        }
                    });
                    break;
                default :
                    protocolStateError(download, "readyForRequests");
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return true, if the download was completed by this chunk
     */
    private boolean receivedChunk0(final Download download, FileChunk chunk)
        throws BrokenDownloadException
    {
        lock.lock();
        try {
            validateDownload();
            switch (state) {
                case ABORTED :
                case BROKEN :
                    log().debug(
                        "Aborted download of " + fileInfo
                            + " received chunk from " + download);
                    post(new Runnable() {
                        public void run() {
                            download.abort();
                        }
                    });
                    break;
                case PASSIVE_DOWNLOAD :
                    storeFileChunk(download, chunk);
                    if (filePartsState.isCompleted()) {
                        return true;
                    }
                    break;
                case ACTIVE_DOWNLOAD :
                    storeFileChunk(download, chunk);
                    if (filePartsState.isCompleted()) {
                        checkFileValidity();
                    } else {
                        post(new Runnable() {
                            public void run() {
                                try {
                                    sendPartRequests();
                                } catch (BrokenDownloadException e) {
                                    setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
                                }
                            }
                        });
                    }
                    break;
                default :
                    protocolStateError(download, "receivedChunk");
                    break;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    private void receivedFilePartsRecord0(Download download,
        final FilePartsRecord record) throws BrokenDownloadException
    {
        lock.lock();
        try {
            validateDownload();
            switch (state) {
                case WAITING_FOR_FILEPARTSRECORD :
                    log().debug("Matching and copying...");
                    setState(InternalState.MATCHING_AND_COPYING);
                    remotePartRecord = record;
                    worker = new Thread(new Runnable() {
                        public void run() {
                            try {
                                matchAndCopyData();
                                lock.lock();
                                try {
                                    if (filePartsState.isCompleted()) {
                                        checkFileValidity();
                                    } else {
                                        if (getSources().isEmpty()) {
                                            throw new BrokenDownloadException("Out of sources");
                                        }
                                        if (!isDone()) {
                                            startActiveDownload();
                                        }
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            } catch (BrokenDownloadException e) {
                                setBroken(TransferProblem.IO_EXCEPTION, e
                                    .toString());
                            } catch (InterruptedException e) {
                                // TODO Maybe it should be setBroken
                                // setBroken(TransferProblem.GENERAL_EXCEPTION,
                                // e.toString());
                            }
                        }
                    }, "Downloadmanager matching and copying");
                    worker.start();
                    break;
                default :
                    protocolStateError(download, "receivedFilePartsRecord");
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeSource0(final Download download)
        throws BrokenDownloadException
    {
        lock.lock();
        try {
            validateDownload();
            switch (state) {
                case WAITING_FOR_FILEPARTSRECORD :
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
                case PASSIVE_DOWNLOAD :
                    removeSourceImpl(download);
                    throw new BrokenDownloadException(
                        "Broken single-source download!");
                case ACTIVE_DOWNLOAD :
                    removeSourceImpl(download);
                    if (hasSources()) {
                        post(new Runnable() {
                            public void run() {
                                    try {
                                        sendPartRequests();
                                    } catch (BrokenDownloadException e) {
                                        setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
                                    }
                                }
                        });
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
        } finally {
            lock.unlock();
        }
    }

    private void saveMetaData() throws IOException {
        assert state != InternalState.COMPLETED;
        assert lock.isHeldByCurrentThread();

        // log().warn("saveMetaData()");
        File mf = getMetaFile();
        if (mf == null && !isCompleted()) {
            if (!getTempFile().delete()) {
                log().error("saveMetaData(): Couldn't delete temp file!");
            }
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
        assert !lock.isHeldByCurrentThread();
        assert !isAborted();

        lock.lock();
        try {
            // Might have been completed/broken
            if (isDone()) {
                return;
            }
            log().debug("Download aborted: " + fileInfo);

            setState(InternalState.ABORTED);
            shutdown();

            if (cleanup) {
                try {
                    killTempFile();
                } catch (FileNotFoundException e) {
                    log().error(e);
                } catch (IOException e) {
                    log().error(e);
                }
                deleteMetaData();
            }
        } finally {
            lock.unlock();
        }

        // This code shouldn't be locked
        for (Download d : getSources()) {
            d.abort();
        }

        getController().getTransferManager().downloadAborted(
            AbstractDownloadManager.this);
    }

    private void setState(InternalState newState) {
        assert lock.isHeldByCurrentThread();

        assert newState != InternalState.PASSIVE_DOWNLOAD
            || !getSources().isEmpty();

        //        log().warn("STATE " + newState + " for " + fileInfo);
        state = newState;
    }

    private void validateDownload() throws BrokenDownloadException {
        if (filePartsState != null) {
            if (filePartsState.getFileLength() != fileInfo.getSize()) {
                throw new BrokenDownloadException(
                    "Concurrent file modification");
            }
        }
        if (remotePartRecord != null) {
            if (remotePartRecord.getFileLength() != fileInfo.getSize()) {
                throw new BrokenDownloadException(
                    "Concurrent file modification");
            }
        }
    }
}