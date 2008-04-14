package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.transfer.Transfer.State;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.FileCheckWorker;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TransferCounter;
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
public abstract class AbstractDownloadManager extends Loggable implements
    DownloadManager
{
    private enum InternalState {
        ACTIVE, COMPLETED, BROKEN, ABORTED;
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

    private final FileInfo fileInfo;
    private State transferState = new State();
    private boolean automatic;
    private Controller controller;
    private RandomAccessFile tempFile = null;

    private InternalState state = InternalState.ACTIVE;

    private boolean shutdown;
    private boolean started;

    public AbstractDownloadManager(Controller controller, FileInfo file,
        boolean automatic) throws IOException
    {
        Reject.noNullElements(controller, file);

        this.fileInfo = file;
        this.automatic = automatic;

        init(controller);
    }

    public synchronized void abort() {
        if (isDone()) {
            return;
        }
        setAborted();

        for (Download d : getDownloads()) {
            d.abort();
        }
        shutdown();
        getController().getTransferManager().downloadAborted(this);

    }

    public synchronized void abortAndCleanup() {
        if (isDone()) {
            return;
        }
        abort();

        if (!getTempFile().delete()) {
            log().error("Failed to delete temporary file:" + getTempFile());
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
                return true;
        }
        return shutdown;
    }

    public synchronized boolean isRequestedAutomatic() {
        return automatic;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public synchronized void readyForRequests(Download download) {
        Reject.ifNull(download, "Download is null!!!");
        if (isDone()) {
            log()
                .info(
                    "Got ready download, but manager is done. Aborting "
                        + download);
            download.abort();
            return;
        }
        if (isNeedingFilePartsRecord()) {
            requestFilePartsRecord(download);
        }

        sendPartRequests();
    }

    public synchronized void receivedChunk(Download download, FileChunk chunk)
        throws IOException
    {
        Reject.noNullElements(download, chunk);
        // log().debug("Received " + chunk + " from " + download);

        if (isDone()) {
            return;
        }

        if (filePartsState == null) {
            log().warn(
                "Not ready to receive data, but received " + chunk + " from "
                    + download + " detail:" + Debug.detailedObjectState(this));
            return;
        }

        setStarted();

        tempFile.seek(chunk.offset);
        tempFile.write(chunk.data);

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

        // log().debug("Remaining: " + (fileInfo.getSize() - avs) + " " +
        // filePartsState.isCompleted());

        // Maybe we're done already ?
        if (filePartsState.isCompleted()) {
            log().debug(
                "Download of " + fileInfo
                    + " completed, awaiting verification.");
            checkCompleted();
            return;
        }

        // Finally request more if needed.
        sendPartRequests();
    }

    public synchronized void receivedFilePartsRecord(Download download,
        final FilePartsRecord record)
    {
        Reject.noNullElements(download, record);
        if (isDone()) {
            return;
        }
        setStarted();

        log().debug("Received FilePartsRecord.");
        if (remotePartRecord != null) {
            log().warn("Received unrequested FilePartsRecord from " + download);
            if (!remotePartRecord.equals(record)) {
                log().error("The new and the old record differ!!");
            }
        }
        remotePartRecord = record;
        getController().getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    File src = getFile();

                    setTransferState(TransferState.MATCHING);
                    Callable<List<MatchInfo>> mInfoWorker = new MatchResultWorker(
                        record, src)
                    {

                        @Override
                        protected void setProgress(int percent) {
                            setTransferState(percent / 100.0);
                        }
                    };
                    List<MatchInfo> mInfoRes = null;
                    try {
                        mInfoRes = mInfoWorker.call();
                    } catch (Throwable t) {
                        log().error(t);
                    }

                    // log().debug("Records: " + record.getInfos().length);
                    log().debug(
                        "Matches: " + mInfoRes.size() + " which are "
                            + (record.getPartLength() * mInfoRes.size())
                            + " bytes (bit less maybe).");

                    setTransferState(TransferState.COPYING);
                    Callable<FilePartsState> pStateWorker = new MatchCopyWorker(
                        src, getTempFile(), record, mInfoRes)
                    {
                        @Override
                        protected void setProgress(int percent) {
                            setTransferState(percent / 100.0);
                        }
                    };
                    FilePartsState state = pStateWorker.call();
                    synchronized (AbstractDownloadManager.this) {
                        filePartsState = state;
                        counter = new TransferCounter(filePartsState
                            .countPartStates(filePartsState.getRange(),
                                PartState.AVAILABLE), fileInfo.getSize());

                        if (filePartsState.isCompleted()) {
                            log().debug(
                                "Download completed (no change detected): "
                                    + this);
                            checkCompleted();
                        } else {
                            sendPartRequests();
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    log().error("SHA Digest not found. Fatal error", e);
                    throw new RuntimeException(
                        "SHA Digest not found. Fatal error", e);
                } catch (FileNotFoundException e) {
                    log().error(e);
                    setBroken(TransferProblem.FILE_NOT_FOUND_EXCEPTION, e
                        .getMessage());
                } catch (IOException e) {
                    log().error(e);
                    setBroken(TransferProblem.IO_EXCEPTION, e.getMessage());
                } catch (Exception e) {
                    log().error(e);
                    setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
                }
            }
        });
    }

    /**
     * Releases resources not required anymore
     */
    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        filePartsState = null;
        // TODO: Actually the remote record shouldn't be dropped since if
        // somebody wants to download the file from us
        // we could just send it, instead of recalculating it!! (So it should be
        // stored "somewhere" - like in the
        // folders database or so)
        remotePartRecord = null;
        try {
            if (tempFile != null) {
                tempFile.close();
                tempFile = null;
            }
        } catch (IOException e) {
            log().error(e);
        }
        updateTempFile();
    }

    @Override
    public String toString() {
        return "[" + getClass().getName() + "; file=" + getFileInfo()
            + "; tempFileRAF: " + tempFile + "; tempFile: " + getTempFile()
            + "; broken: " + isBroken() + "; completed: " + isCompleted()
            + "; aborted: " + isAborted() + "; shutdown: " + shutdown;
    }

    protected void checkCompleted() {
        log().debug("Checking for completed " + fileInfo);
        if (isDone()) {
            return;
        }
        setTransferState(TransferState.VERIFYING);
        log().debug("Verifying file hash.");
        getController().getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Callable<Boolean> fileChecker = null;
                    synchronized (AbstractDownloadManager.this) {
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
                    }
                    // If we don't have a record, the file is assumed to be
                    // "valid"
                    if (fileChecker == null || fileChecker.call()) {
                        synchronized (AbstractDownloadManager.this) {
                            if (!isDone()) {
                                setCompleted();
                            }
                        }
                    } else {
                        synchronized (AbstractDownloadManager.this) {
                            filePartsState.setPartState(Range.getRangeByLength(
                                0, filePartsState.getFileLength()),
                                PartState.NEEDED);
                        }
                        sendPartRequests();
                        counter = new TransferCounter(0, fileInfo.getSize());
                    }
                } catch (NoSuchAlgorithmException e) {
                    // If this error occurs, no downloads will ever succeed.
                    log().error(e);
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    log().error(e);
                    setBroken(TransferProblem.GENERAL_EXCEPTION, e.getMessage());
                } finally {
                    log().debug("DONE - Validating file hash.");
                }
            }
        });
    }

    protected abstract Collection<Download> getDownloads();

    protected File getFile() {
        return fileInfo.getDiskFile(getController().getFolderRepository());
    }

    protected void init(Controller controller) throws IOException {
        Reject.ifNull(controller, "Controller is null");
        this.controller = controller;

        // Check for valid values!
        Reject.ifNull(fileInfo, "fileInfo is null");
        Reject.ifNull(getTempFile(), "tempFile is null");

        // If it's an old download, don't create a temporary file
        if (isCompleted()) {
            return;
        }

        if (isDone()) {
            throw new IllegalStateException("File done before init!");
        }

        // Create temp-file directory structure if necessary
        if (!getTempFile().getParentFile().exists()) {
            if (!getTempFile().getParentFile().mkdirs()) {
                throw new FileNotFoundException(
                    "Couldn't create parent directory!");
            }
        }

        tempFile = new RandomAccessFile(getTempFile(), "rw");

        if (!isNeedingFilePartsRecord()) {
            tempFile.setLength(0);

            log().verbose(
                "Won't send FPR request: Minimum requirements not fulfilled!");
            filePartsState = new FilePartsState(fileInfo.getSize());
        }

        loadFilePartsState();
    }

    protected boolean isNeedingFilePartsRecord() {
        return !isCompleted() && remotePartRecord == null
            && fileInfo.getSize() >= Constants.MIN_SIZE_FOR_PARTTRANSFERS
            && fileInfo.diskFileExists(getController());
    }

    protected abstract void requestFilePartsRecord(Download download);

    protected abstract void sendPartRequests();

    protected synchronized void setAutomatic(boolean auto) {
        automatic = auto;
    }

    protected synchronized void setBroken(TransferProblem problem,
        String message)
    {
        if (isDone()) {
            log().warn("Multiple calls to setBroken()");
            return;
        }
        state = InternalState.BROKEN;

        for (Download d : getDownloads()) {
            getController().getTransferManager().setBroken(d, problem, message);
        }

        getController().getTransferManager().setBroken(this, problem, message);
        shutdown();
    }

    protected synchronized void setCompleted() {
        if (isDone()) {
            throw new IllegalStateException("Already done");
        }
        state = InternalState.COMPLETED;

        shutdown();

        getController().getTransferManager().setCompleted(this);

        for (Download d : getDownloads()) {
            getController().getTransferManager().setCompleted(d);
        }
    }

    protected synchronized void setCompleteOnLoad() {
        state = InternalState.COMPLETED;
    }

    protected void setStarted() {
        started = true;
    }

    protected void setTransferState(double progress) {
        transferState.setProgress(progress);
        for (Download d : getDownloads()) {
            d.transferState.setProgress(progress);
        }
    }

    protected void setTransferState(TransferState state) {
        if (transferState.getState() == state) {
            return;
        }
        transferState.setState(state);
        transferState.setProgress(0);
        for (Download d : getDownloads()) {
            d.transferState.setState(state);
            d.transferState.setProgress(0);
        }
    }

    protected void setTransferState(TransferState state, double progress) {
        transferState.setState(state);
        transferState.setProgress(progress);
        for (Download d : getDownloads()) {
            d.transferState.setState(state);
            d.transferState.setProgress(progress);
        }
    }

    protected void updateTempFile() {
        if (tempFile == null) {
            return;
        }

        try {
            tempFile.close();
        } catch (IOException e) {
            log().error(e);
        }
        // log().debug("Updating tempfile modification date to: " +
        // getFileInfo().getModifiedDate());
        if (!getTempFile().setLastModified(
            getFileInfo().getModifiedDate().getTime()))
        {
            log().error(
                "Failed to update modification date! Detail:"
                    + Debug.detailedObjectState(this));
        }

        try {
            tempFile = new RandomAccessFile(getTempFile(), "rw");
        } catch (FileNotFoundException e) {
            setBroken(TransferProblem.FILE_NOT_FOUND_EXCEPTION, e.toString());
            return;
        }
    }

    private boolean isAborted() {
        return state == InternalState.ABORTED;
    }

    private void loadFilePartsState() throws IOException {
        // TODO: I'm deleting any previous progress here since
        // we don't store the parts state anywhere. Therefore no assumption can
        // be made
        // on what has already been downloaded and what is garbage.
        // Note that the old code didn't do this, even when using delta sync.
        // That was
        // actually wrong, but didn't show up since the parts where received "in
        // order".
        // But with swarming there might be holes!

        tempFile.setLength(0);
    }

    private void setAborted() {
        state = InternalState.ABORTED;
    }
}