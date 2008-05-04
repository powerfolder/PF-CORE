package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import de.dal33t.powerfolder.util.Debug;
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
        WAITING_FOR_SOURCE,
        WAITING_FOR_UPLOAD_READY,
        WAITING_FOR_FILEPARTSRECORD,
        
        COMPLETED, BROKEN, ABORTED, STOPPED, 
        
        /**
         * Receive chunks in linear fashion from one source only 
         */
        PASSIVE_DOWNLOAD, ACTIVE_DOWNLOAD, MATCHING_AND_COPYING, CHECKING_FILE_VALIDITY;
    }

    /**
     * Be sure to lock on "this" if you overwrite this variable
     */
    protected FilePartsState filePartsState;

    /**
     * Be sure to lock on "this" if you overwrite this variable
     */
    protected FilePartsRecord remotePartRecord;

    private volatile TransferCounter counter;
    private State transferState = new State();

    private final FileInfo fileInfo;
    private boolean automatic;
    private Controller controller;
    private RandomAccessFile tempFile = null;

    private InternalState state = InternalState.WAITING_FOR_SOURCE;

    private boolean started;
    
    private Thread worker;

    private int failedChecks;

    public AbstractDownloadManager(Controller controller, FileInfo file,
        boolean automatic) throws IOException
    {
        Reject.noNullElements(controller, file);

        this.fileInfo = file;
        this.automatic = automatic;

        this.controller = controller;
        
        init();
    }

    public synchronized void abort() {
//        illegalState("abort");
        switch (state) {
            case ABORTED:
            case BROKEN:
            case COMPLETED:
            break;
            default:
                setAborted(false);
                break;
        }
    }
    
    public synchronized void abortAndCleanup() {
//        illegalState("abortAndCleanup");
        switch (state) {
            case ABORTED:
            case BROKEN:
            case COMPLETED:
                break;
            default:
                setAborted(true);
            break;
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
    public File getTempFile() {
        File diskFile = getFileInfo().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        File tempFile = new File(diskFile.getParentFile(), "(incomplete) "
            + diskFile.getName());
        return tempFile;
    }

    private File getMetaFile() {
        File diskFile = getFileInfo().getDiskFile(
            getController().getFolderRepository());
        if (diskFile == null) {
            return null;
        }
        File metaFile = new File(diskFile.getParentFile(),
            FileUtils.DOWNLOAD_META_FILE + diskFile.getName());
        return metaFile;
    }

    public synchronized boolean isBroken() {
        return state == InternalState.BROKEN;
    }

    public synchronized boolean isCompleted() {
        return state == InternalState.COMPLETED;
    }

    public synchronized boolean isDone() {
        switch (state) {
            case ABORTED :
            case BROKEN :
            case COMPLETED :
            case STOPPED:
                return true;
        }
        return false;
    }

    public synchronized boolean isRequestedAutomatic() {
        return automatic;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public synchronized void readyForRequests(Download download) {
        Reject.ifNull(download, "Download is null!!!");
        try {
            validateDownload();
            switch (state) {
                case CHECKING_FILE_VALIDITY:
                case MATCHING_AND_COPYING:
                    // Do nothing, action will be taken after the worker is done
                    break;
                case ACTIVE_DOWNLOAD:
                    sendPartRequests();
                    break;
                case WAITING_FOR_FILEPARTSRECORD:
                    // Maybe request from different source
                    requestFilePartsRecord(download);
                    break;
                case WAITING_FOR_UPLOAD_READY:
                    if (isNeedingFilePartsRecord() && Util.useDeltaSync(getController(), download.getPartner())) {
                        requestFilePartsRecord(download);
                        setState(InternalState.WAITING_FOR_FILEPARTSRECORD);
                    } else {
                        if (filePartsState == null) {
                            setFilePartsState(new FilePartsState(fileInfo.getSize()));
                        }
                        log().debug("Not requesting record for this download.");
                        try {
                            startActiveDownload();
                        } catch (BrokenDownloadException e) {
                            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
                        }
                    }
                    break;
                case BROKEN:
                case ABORTED:
                    download.abort();
                    break;
                default:
                    protocolStateError(download, "readyForRequests");
                break;
            }
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    protected abstract boolean isUsingDeltaSync();

    private void protocolStateError(Download cause, String operation) {
        String msg = "PROTOCOL ERROR caused by " + cause + ": " + operation + " not allowed in state " + state; 
        log().warn(msg);
        getController().getTransferManager().setBroken(cause, TransferProblem.BROKEN_DOWNLOAD);
    }

    protected void storeFileChunk(Download download, FileChunk chunk) {
        assert download != null;
        assert chunk != null;
        if (!getSources().contains(download)) {
            log().warn("Received chunk from download which is not source: " + download);
            return;
        }
        
        setStarted();

        try {
            tempFile.seek(chunk.offset);
            tempFile.write(chunk.data);
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
    
    public synchronized void receivedChunk(Download download, FileChunk chunk) {
        Reject.noNullElements(download, chunk);
        try {
            validateDownload();
            switch (state) {
                case ABORTED:
                case BROKEN:
                    log().debug("Aborted download of " + fileInfo + " received chunk from " + download);
                    download.abort();
                    break;
                case PASSIVE_DOWNLOAD:
                    storeFileChunk(download, chunk);
                    if (filePartsState.isCompleted()) {
                        setCompleted();
                    }
                    break;
                case ACTIVE_DOWNLOAD:
                    storeFileChunk(download, chunk);
                    if (filePartsState.isCompleted()) {
                        checkFileValidity();
                    } else {
                        sendPartRequests();
                    }
                    break;
                default:
                    protocolStateError(download, "receivedChunk");
                break;
            }
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }

    /**
     * 
     */
    private void checkFileValidity() {
        assert !isDone();
        
        setState(InternalState.CHECKING_FILE_VALIDITY);
        worker = new Thread(new Runnable() {
            public void run() {
                if (checkCompleted()) {
                    synchronized (AbstractDownloadManager.this) {
                        // Might still have been broken
                        if (!isDone()) {
                            setCompleted();
                        }
                    }
                } else {
                    synchronized (AbstractDownloadManager.this) {
                        try {
                            startActiveDownload();
                        } catch (BrokenDownloadException e) {
                            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
                        }
                    }
                }
            }
        }, "Downloadmanager file checker");
        worker.start();
    }

    public synchronized void receivedFilePartsRecord(Download download,
        final FilePartsRecord record)
    {
        Reject.noNullElements(download, record);
        try {
            validateDownload();
            switch (state) {
                case WAITING_FOR_FILEPARTSRECORD:
                    log().debug("Matching and copying...");
                    setState(InternalState.MATCHING_AND_COPYING);
                    remotePartRecord = record;
                    worker = new Thread(new Runnable() {
                        public void run() {
                            try {
                                matchAndCopyData();
                                synchronized (AbstractDownloadManager.this) {
                                    if (filePartsState.isCompleted()) {
                                        checkFileValidity();
                                    } else {
                                        startActiveDownload();
                                    }
                                }
                            } catch (BrokenDownloadException e) {
                                synchronized (AbstractDownloadManager.this) {
                                    setBroken(TransferProblem.IO_EXCEPTION, e.toString());
                                }
                            } catch (InterruptedException e) {
//                            TODO Maybe it should be setBroken
//                            setBroken(TransferProblem.GENERAL_EXCEPTION, e.toString());
                            }
                        }
                    }, "Downloadmanager matching and copying");
                    worker.start();
                    break;
                default:
                    protocolStateError(download, "receivedFilePartsRecord");
                break;
            }
        } catch (BrokenDownloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void startActiveDownload() throws BrokenDownloadException {
        assert Util.usePartRequests(getController(), getSources().iterator().next().getPartner());
        
        setState(InternalState.ACTIVE_DOWNLOAD);
        validateDownload();
        
        log().debug("Requesting parts");
        sendPartRequests();
    }

    public synchronized void stop() {
        setState(InternalState.STOPPED);
        shutdown();
    }
    
    /**
     * Releases resources not required anymore
     */
    protected void shutdown() {
        assert Thread.holdsLock(this);
        assert isDone();

        if (isShutDown()) {
            return;
        }

        shutdown = true;
        log().debug("Shutting down " + fileInfo);
        try {
            if (worker != null) {
                worker.interrupt();
            }
            if (tempFile != null) {
                // log().error("Closing temp file!");
                tempFile.close();
                tempFile = null;
            }
            if (!isCompleted()) {
                saveMetaData();
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
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "; state= "
            + state + " file=" + getFileInfo() + "; tempFileRAF: "
            + tempFile + "; tempFile: " + getTempFile() + "; broken: "
            + isBroken() + "; completed: " + isCompleted() + "; aborted: "
            + isAborted();
    }

    protected boolean checkCompleted() {
        setTransferState(TransferState.VERIFYING);
//        log().debug("Verifying file hash for " + this);
        try {
            Callable<Boolean> fileChecker = null;
            if (remotePartRecord != null) {
                fileChecker = new FileCheckWorker(getTempFile(),
                    MessageDigest.getInstance("MD5"),
                    remotePartRecord.getFileDigest())
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
            log().warn(
                "Checking FAILED, file will be re-downloaded!");
            if (failedChecks > 1) {
                throw new BrokenDownloadException("Filecheck failed after redownloading " + failedChecks + " times, remote part record seems to be corrupted.");
            }
            failedChecks++;
            counter = new TransferCounter(0, fileInfo.getSize());
            filePartsState.setPartState(Range.getRangeByLength(
                0, filePartsState.getFileLength()),
                PartState.NEEDED);
            
            return false;
        } catch (NoSuchAlgorithmException e) {
            // If this error occurs, no downloads will ever succeed.
            log().error(e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log().verbose(e);
        } catch (Exception e) {
            log().error(e);
            synchronized (AbstractDownloadManager.this) {
                setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
            }
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

        if (isDone()) {
            throw new IllegalStateException("File broken/aborted before init!");
        }

        // Create temp-file directory structure if necessary
        if (!getTempFile().getParentFile().exists()) {
            if (!getTempFile().getParentFile().mkdirs()) {
                throw new FileNotFoundException(
                    "Couldn't create parent directory!");
            }
        }

        loadMetaData();

        tempFile = new RandomAccessFile(getTempFile(), "rw");
    }

    protected boolean isNeedingFilePartsRecord() {
        return !isCompleted() && remotePartRecord == null
            && fileInfo.getSize() >= Constants.MIN_SIZE_FOR_PARTTRANSFERS
            && fileInfo.diskFileExists(getController());
    }

    protected abstract void requestFilePartsRecord(Download download);

    protected abstract void sendPartRequests() throws BrokenDownloadException;

    protected synchronized void setAutomatic(boolean auto) {
        automatic = auto;
    }

    protected void setBroken(TransferProblem problem,
        String message)
    {
        if (isDone()) {
            return;
        }
        log().debug("Download broken: " + fileInfo);
        setState(InternalState.BROKEN);
        shutdown();
        
        for (Download d: getSources()) {
            getController().getTransferManager().setBroken(d, problem, message);
        }

        getController().getTransferManager().setBroken(this, problem, message);
    }

    protected void setCompleted() {
        log().debug("Completed download of " + fileInfo + ".");
        
        setState(InternalState.COMPLETED);
        shutdown();
        deleteMetaData();
        
        getController().getTransferManager().setCompleted(this);

        for (Download d: getSources()) {
            getController().getTransferManager().setCompleted(d);
        }
    }
    
    public void broken() {
        setBroken(TransferProblem.BROKEN_DOWNLOAD, "Broken by external code");
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

    protected void updateTempFile() {
        assert getTempFile() != null && getTempFile().exists();

        // log().debug("Updating tempfile modification date to: " +
        // getFileInfo().getModifiedDate());
        if (!getTempFile().setLastModified(
            getFileInfo().getModifiedDate().getTime()))
        {
            log().error(
                "Failed to update modification date! Detail:"
                    + Debug.detailedObjectState(this));
        }
    }

    private synchronized boolean isAborted() {
        return state == InternalState.ABORTED;
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
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
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

    /**
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void killTempFile() throws FileNotFoundException, IOException {
        if (getTempFile() != null && getTempFile().exists()
            && !getTempFile().delete())
        {
            log().warn("Couldn't delete old temporary file, some other process could be using it! Trying to set it's length to 0.");
            RandomAccessFile f = new RandomAccessFile(getTempFile(), "rw");
            f.setLength(0);
            f.close();
        }
    }

    private void deleteMetaData() {
        // log().warn("deleteMetaData()");
        if (getMetaFile() != null && getMetaFile().exists()
            && !getMetaFile().delete())
        {
            log().error("Couldn't delete meta data file!");
        }
    }

    private void saveMetaData() throws IOException {
        assert state != InternalState.COMPLETED;
        
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
        
        for (Download d: getSources()) {
            d.abort();
        }

        getController().getTransferManager().downloadAborted(this);
    }

    private boolean shutdown;
    protected synchronized void setFilePartsState(FilePartsState state) {
        assert filePartsState == null;

        filePartsState = state;
    }
    
    public synchronized void addSource(Download download) {
        Reject.ifNull(download, "Download is null!");
        Reject.ifFalse(download.isCompleted()
            || allowsSourceFor(download.getPartner()), "Illegal addSource() call!!");
        try {
            validateDownload();
            switch (state) {
                case MATCHING_AND_COPYING:
                case CHECKING_FILE_VALIDITY:
                    addSourceImpl(download);
                    // Maybe rework the code so this request will be sent if we really need data
                    download.request(0);
                    break;
                case ACTIVE_DOWNLOAD:
                    addSourceImpl(download);
                    // We can use offset 0 here since the transfer will use requests
                    // anyways
                    download.request(0);
                    sendPartRequests();
                    break;
                case WAITING_FOR_UPLOAD_READY:
                case WAITING_FOR_FILEPARTSRECORD:
                    addSourceImpl(download);
                    // We can use offset 0 here since the transfer will use requests
                    // anyways
                    download.request(0);
                    break;
                case WAITING_FOR_SOURCE:
                    addSourceImpl(download);
                    long offset = 0;
                    if (filePartsState != null) {
                        if (filePartsState.isCompleted()) {
                            // Completed already ?
                            setCompleted();
                            break;
                        }
                        Range range = filePartsState.findFirstPart(PartState.NEEDED);
                        if (range != null) {
                            offset = range.getStart();
                        } else {
                            if (filePartsState.isCompleted() ||
                                filePartsState.findFirstPart(PartState.PENDING) != null) {
                                log().error(new AssertionError("FILEPARTSSTATE ERROR!"));
                                throw new AssertionError("FILEPARTSSTATE ERROR!");
                            }
                        }
                    }
                    download.request(offset);
                    if (isUsingPartRequests()) {
                        setState(InternalState.WAITING_FOR_UPLOAD_READY);
                    } else {
                        setState(InternalState.PASSIVE_DOWNLOAD);
                        if (filePartsState == null) {
                            setFilePartsState(new FilePartsState(fileInfo.getSize()));
                        }
                    }
                    break;
                default:
                    illegalState("addSource");
                break;
            }
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }
    
    private void setState(InternalState newState) {
        assert Thread.holdsLock(this);

        switch (newState) {
            case PASSIVE_DOWNLOAD:
                if (getSources().isEmpty()) {
                    log().error(new AssertionError("old state: " + state + " - new state: " + newState + " not possible: no sources!"));
                    throw new AssertionError("old state: " + state + " - new state: " + newState + " not possible: no sources!");
                }
                break;
        }
//        log().warn("STATE " + newState + " for " + fileInfo);
        state = newState;
    }

    protected abstract boolean isUsingPartRequests();

    private void illegalState(String operation) {
        throw new IllegalStateException(operation + " not allowed in state " + state);
    }

    public synchronized void removeSource(Download download) {
        Reject.ifNull(download, "Download is null!");
        try {
            validateDownload();
            switch (state) {
                case WAITING_FOR_FILEPARTSRECORD:
                case WAITING_FOR_UPLOAD_READY:
                    removeSourceImpl(download);
                    if (!hasSources()) {
                        // If we're out of sources, wait for additional ones again
                        // Actually the TransferManager will break this transfer, but with 
                        // the following code this manager could also be reused.
                        setState(InternalState.WAITING_FOR_SOURCE);
                    }
                    break;
                case PASSIVE_DOWNLOAD:
                    removeSourceImpl(download);
                    setBroken(TransferProblem.BROKEN_DOWNLOAD, "Broken single-source download!");
                    break;
                case ACTIVE_DOWNLOAD:
                    removeSourceImpl(download);
                    if (hasSources()) {
                        sendPartRequests();
                    }
                    break;
                case MATCHING_AND_COPYING:
                case CHECKING_FILE_VALIDITY:
                case BROKEN:
                case ABORTED:
                    removeSourceImpl(download);
                    break;
                default:
                    illegalState("removeSource");
                break;
            }
        } catch (BrokenDownloadException e) {
            setBroken(TransferProblem.BROKEN_DOWNLOAD, e.toString());
        }
    }
    
    private void validateDownload() throws BrokenDownloadException {
        if (filePartsState != null) {
            if (filePartsState.getFileLength() != fileInfo.getSize()) {
                throw new BrokenDownloadException("Concurrent file modification");
            }
        }
        if (remotePartRecord != null) {
            if (remotePartRecord.getFileLength() != fileInfo.getSize()) {
                throw new BrokenDownloadException("Concurrent file modification");
            }
        }
    }

    protected abstract void removeSourceImpl(Download source);

    protected abstract void addSourceImpl(Download source);

    protected void matchAndCopyData() throws BrokenDownloadException, InterruptedException {
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
            Callable<FilePartsState> pStateWorker = new MatchCopyWorker(
                src, getTempFile(), remotePartRecord, mInfoRes)
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
            
            setFilePartsState(state);
            counter = new TransferCounter(filePartsState
                .countPartStates(filePartsState.getRange(),
                    PartState.AVAILABLE), fileInfo.getSize());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                "SHA Digest not found. Fatal error", e);
        } catch (FileNotFoundException e) {
            throw new BrokenDownloadException(TransferProblem.FILE_NOT_FOUND_EXCEPTION, e);
        } catch (IOException e) {
            throw new BrokenDownloadException(TransferProblem.IO_EXCEPTION, e);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new BrokenDownloadException(TransferProblem.GENERAL_EXCEPTION, e);
        }
    }

    public boolean isShutDown() {
        return shutdown;
    }
}