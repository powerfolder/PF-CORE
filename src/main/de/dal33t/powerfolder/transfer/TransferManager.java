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
 * $Id: TransferManager.java 21079 2013-03-15 02:10:37Z sprajc $
 */
package de.dal33t.powerfolder.transfer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoKey;
import de.dal33t.powerfolder.light.FileInfoKey.Type;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.AbortUpload;
import de.dal33t.powerfolder.message.DownloadQueued;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.TransferStatus;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.transfer.swarm.FileRecordProvider;
import de.dal33t.powerfolder.transfer.swarm.VolatileFileRecordProvider;
import de.dal33t.powerfolder.util.Filter;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.NamedThreadFactory;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Validate;
import de.dal33t.powerfolder.util.Visitor;
import de.dal33t.powerfolder.util.WrapperExecutorService;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;

/**
 * Transfer manager for downloading/uploading files
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.92 $
 */
public class TransferManager extends PFComponent {

    /**
     * The maximum size of a chunk transferred at once of older, prev 3.1.7/4.0
     * versions.
     */
    public static final int OLD_MAX_CHUNK_SIZE = 32 * 1024;
    public static final int OLD_MAX_REQUESTS_QUEUED = 20;
    public static final long PARTIAL_TRANSFER_DELAY = 5000; // Five seconds
    public static final long ONE_DAY = 24L * 3600 * 1000; // One day in ms
    public static final long SIX_HOURS = 6L * 3600 * 1000; // 6 hours

    private static final DecimalFormat CPS_FORMAT = new DecimalFormat(
        "#,###,###,###.##");

    private volatile boolean started;

    private Thread myThread;
    /** Uploads that are waiting to start */
    private final List<Upload> queuedUploads;
    /** currently uploading */
    private final List<Upload> activeUploads;
    /** The list of completed download */
    private final List<Upload> completedUploads;
    /** currenly downloading */
    private final ConcurrentMap<FileInfo, DownloadManager> dlManagers;
    /**
     * The # of active and queued downloads of this node. Cached value. Only
     * used for performance optimization
     */
    private final ConcurrentMap<Member, Integer> downloadsCount;
    /** A set of pending files, which should be downloaded */
    private final List<Download> pendingDownloads;
    /** The list of completed download */
    private final ConcurrentMap<FileInfoKey, DownloadManager> completedDownloads;

    /** The trigger, where transfermanager waits on */
    private final Object waitTrigger = new Object();
    private boolean transferCheckTriggered;
    /**
     * To lock the transfer checker. Lock this to make sure no transfer checks
     * are executed untill the lock is released.
     */
    // private ReentrantLock downloadsLock = new ReentrantLock();
    /**
     * To lock the transfer checker. Lock this to make sure no transfer checks
     * are executed untill the lock is released.
     */
    private final Lock uploadsLock = new ReentrantLock();
    private final Object downloadRequestLock = new Object();

    private FileRecordProvider fileRecordProvider;

    /** Threadpool for Upload Threads */
    private ExecutorService threadPool;

    /** the counter for uploads (effecitve) */
    private final TransferCounter uploadCounter;
    /** the counter for downloads (effecitve) */
    private final TransferCounter downloadCounter;

    /** the counter for up traffic (real) */
    private final TransferCounter totalUploadTrafficCounter;
    /** the counter for download traffic (real) */
    private final TransferCounter totalDownloadTrafficCounter;

    /** Provides bandwidth for the transfers */
    private final BandwidthProvider bandwidthProvider;

    /** Input limiter, currently shared between all LAN connections */
    private final BandwidthLimiter sharedLANInputHandler;
    /** Input limiter, currently shared between all WAN connections */
    private final BandwidthLimiter sharedWANInputHandler;
    /** Output limiter, currently shared between all LAN connections */
    private final BandwidthLimiter sharedLANOutputHandler;
    /** Output limiter, currently shared between all WAN connections */
    private final BandwidthLimiter sharedWANOutputHandler;

    private final TransferManagerListener listenerSupport;

    private DownloadManagerFactory downloadManagerFactory = MultiSourceDownloadManager.factory;

    private BandwidthStatsRecorder statsRecorder;

    private final AtomicBoolean recalculatingAutomaticRates = new AtomicBoolean();

    public TransferManager(Controller controller) {
        super(controller);
        started = false;
        queuedUploads = new CopyOnWriteArrayList<Upload>();
        activeUploads = new CopyOnWriteArrayList<Upload>();
        completedUploads = new CopyOnWriteArrayList<Upload>();
        dlManagers = Util.createConcurrentHashMap();
        pendingDownloads = new CopyOnWriteArrayList<Download>();
        completedDownloads = Util.createConcurrentHashMap();
        downloadsCount = Util.createConcurrentHashMap();
        uploadCounter = new TransferCounter();
        downloadCounter = new TransferCounter();
        totalUploadTrafficCounter = new TransferCounter();
        totalDownloadTrafficCounter = new TransferCounter();

        // Create listener support
        listenerSupport = ListenerSupportFactory
            .createListenerSupport(TransferManagerListener.class);

        bandwidthProvider = new BandwidthProvider();

        statsRecorder = new BandwidthStatsRecorder(getController());
        bandwidthProvider.addBandwidthStatListener(statsRecorder);

        sharedWANOutputHandler = BandwidthLimiter.WAN_OUTPUT_BANDWIDTH_LIMITER;
        sharedWANInputHandler = BandwidthLimiter.WAN_INPUT_BANDWIDTH_LIMITER;
        sharedLANOutputHandler = BandwidthLimiter.LAN_OUTPUT_BANDWIDTH_LIMITER;
        sharedLANInputHandler = BandwidthLimiter.LAN_INPUT_BANDWIDTH_LIMITER;

        checkConfigCPS(ConfigurationEntry.UPLOAD_LIMIT_WAN, 0);
        checkConfigCPS(ConfigurationEntry.DOWNLOAD_LIMIT_WAN, 0);
        checkConfigCPS(ConfigurationEntry.UPLOAD_LIMIT_LAN, 0);
        checkConfigCPS(ConfigurationEntry.DOWNLOAD_LIMIT_LAN, 0);

        setUploadCPSForWAN(getConfigCPS(ConfigurationEntry.UPLOAD_LIMIT_WAN));
        setDownloadCPSForWAN(getConfigCPS(ConfigurationEntry.DOWNLOAD_LIMIT_WAN));
        setUploadCPSForLAN(getConfigCPS(ConfigurationEntry.UPLOAD_LIMIT_LAN));
        setDownloadCPSForLAN(getConfigCPS(ConfigurationEntry.DOWNLOAD_LIMIT_LAN));

        if (getMaxFileChunkSize() > OLD_MAX_CHUNK_SIZE) {
            logWarning("Max filechunk size set to "
                + Format.formatBytes(getMaxFileChunkSize())
                + ". Can only communicate with clients"
                + " running version 3.1.7 or higher.");
        }
        if (getMaxRequestsQueued() > OLD_MAX_REQUESTS_QUEUED) {
            logWarning("Max request queue size set to "
                + getMaxRequestsQueued()
                + ". Can only communicate with clients"
                + " running version 3.1.7 or higher.");
        }
    }

    /**
     * Checks if the configration entry exists, and if not sets it to a given
     * value.
     * 
     * @param entry
     * @param cpsArg
     */
    private void checkConfigCPS(ConfigurationEntry entry, long cpsArg) {
        String cps = entry.getValue(getController());
        if (cps == null) {
            entry.setValue(getController(), Long.toString(cpsArg / 1024));
        }
    }

    private long getConfigCPS(ConfigurationEntry entry) {
        String cps = entry.getValue(getController());
        long maxCps = 0;
        if (cps != null) {
            try {
                maxCps = (long) Double.parseDouble(cps) * 1024;
                if (maxCps < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                logWarning("Illegal value for KByte." + entry + " '" + cps
                    + '\'');
            }
        }
        return maxCps;
    }

    // General ****************************************************************

    public void printStats() {
        long total = totalDownloadTrafficCounter.getBytesTransferred()
            + totalUploadTrafficCounter.getBytesTransferred();
        long payload = downloadCounter.getBytesTransferred()
            + uploadCounter.getBytesTransferred();
        long overhead = total - payload;
        logInfo("Total: " + Format.formatBytes(total) + ", Payload: "
            + Format.formatBytes(payload) + ". Overhead: " + overhead * 100
            / payload + '%');
    }

    /**
     * Starts the transfermanager thread
     */
    public void start() {
        if (!ConfigurationEntry.TRANSFER_MANAGER_ENABLED
            .getValueBoolean(getController()))
        {
            logWarning("Not starting TransferManager. disabled by config");
            return;
        }
        fileRecordProvider = new VolatileFileRecordProvider(getController());

        bandwidthProvider.start();

        threadPool = new WrapperExecutorService(
            Executors.newCachedThreadPool(new NamedThreadFactory("TMThread-")));

        myThread = new Thread(new TransferChecker(), "Transfer manager");
        myThread.start();

        // Load all pending downloads
        loadDownloads();

        // Do an initial clean.
        cleanupOldTransfers();

        getController().scheduleAndRepeat(new PartialTransferStatsUpdater(),
            PARTIAL_TRANSFER_DELAY, PARTIAL_TRANSFER_DELAY);

        getController().scheduleAndRepeat(new TransferCleaner(), SIX_HOURS,
            SIX_HOURS);

        started = true;
        logFine("Started");
    }

    /**
     * This method cleans up old uploads and downloads. Only cleans up if the
     * UPLOADS_AUTO_CLEANUP / DOWNLOAD_AUTO_CLEANUP is true and the transfer is
     * older than AUTO_CLEANUP_FREQUENCY in days.
     */
    private void cleanupOldTransfers() {
        int rawUploadCleanupFrequency = ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
            .getValueInt(getController());
        int trueUploadCleanupFrequency;
        if (rawUploadCleanupFrequency <= 4) {
            trueUploadCleanupFrequency = Constants.CLEANUP_VALUES[rawUploadCleanupFrequency];
        } else {
            trueUploadCleanupFrequency = Integer.MAX_VALUE;

        }
        for (Upload completedUpload : completedUploads) {
            long numberOfDays = calcDays(completedUpload.getCompletedDate());
            if (numberOfDays >= trueUploadCleanupFrequency) {
                logInfo("Auto-cleaning up upload '"
                    + completedUpload.getFile().getRelativeName() + "' (days="
                    + numberOfDays + ')');
                clearCompletedUpload(completedUpload);
            }
        }

        int rawDownloadCleanupFrequency = ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY
            .getValueInt(getController());
        int trueDownloadCleanupFrequency;
        if (rawDownloadCleanupFrequency <= 4) {
            trueDownloadCleanupFrequency = Constants.CLEANUP_VALUES[rawDownloadCleanupFrequency];
        } else {
            trueDownloadCleanupFrequency = Integer.MAX_VALUE;

        }
        int n = 0;
        for (DownloadManager completedDownload : completedDownloads.values()) {
            long numberOfDays = calcDays(completedDownload.getCompletedDate());
            if (completedDownload.getCompletedDate() == null && isSevere()) {
                logFine("Completed download misses completed date: "
                    + completedDownload.getCompletedDate());
            }
            if (numberOfDays >= trueDownloadCleanupFrequency) {
                if (isFiner()) {
                    logFiner("Auto-cleaning up download '"
                        + completedDownload.getFileInfo().getRelativeName()
                        + "' (days=" + numberOfDays + ')');
                }
                clearCompletedDownload(completedDownload);
                n++;
            }
        }
        if (n > 0) {
            logFine("Cleaned up " + n + " completed downloads");
        }
    }

    /**
     * Returns the number of days between two dates.
     * 
     * @param completedDate
     * @return
     */
    private static long calcDays(Date completedDate) {
        if (completedDate == null) {
            return -1;
        }
        Date now = new Date();
        long diff = now.getTime() - completedDate.getTime();
        return diff / ONE_DAY;
    }

    /**
     * Shuts down the transfer manager
     */
    public void shutdown() {
        // Remove listners, not bothering them by boring shutdown events
        // ListenerSupportFactory.removeAllListeners(listenerSupport);

        // shutdown on thread
        if (myThread != null) {
            myThread.interrupt();
        }

        if (threadPool != null) {
            threadPool.shutdownNow();
        }

        // shutdown active uploads
        for (Upload upload : activeUploads) {
            upload.abort();
            upload.shutdown();
        }

        bandwidthProvider.shutdown();

        if (started) {
            storeDownloads();
        }

        // abort / shutdown active downloads
        // done after storeDownloads(), so they are restored!
        for (DownloadManager man : dlManagers.values()) {
            synchronized (man) {
                man.setBroken(TransferProblem.BROKEN_DOWNLOAD, "shutdown");
            }
        }

        if (fileRecordProvider != null) {
            fileRecordProvider.shutdown();
            fileRecordProvider = null;
        }

        statsRecorder.persistStats();

        started = false;
        logFine("Stopped");
    }

    /**
     * Prune stats older than a month.
     */
    public void pruneStats() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        Date date = cal.getTime();
        statsRecorder.pruneStats(date);
    }

    /**
     * for debug
     * 
     * @param suspended
     */
    public void setSuspendFireEvents(boolean suspended) {
        ListenerSupportFactory.setSuspended(listenerSupport, suspended);
        logFine("setSuspendFireEvents: " + suspended);
    }

    /**
     * Triggers the workingn checker thread.
     */
    public void triggerTransfersCheck() {
        synchronized (waitTrigger) {
            transferCheckTriggered = true;
            waitTrigger.notifyAll();
        }
    }

    public BandwidthProvider getBandwidthProvider() {
        return bandwidthProvider;
    }

    public BandwidthLimiter getOutputLimiter(ConnectionHandler handler) {
        if (handler.isOnLAN()) {
            return sharedLANOutputHandler;
        }
        return sharedWANOutputHandler;
    }

    public BandwidthLimiter getInputLimiter(ConnectionHandler handler) {
        if (handler.isOnLAN()) {
            return sharedLANInputHandler;
        }
        return sharedWANInputHandler;
    }

    /**
     * Returns the transfer status by calculating it new
     * 
     * @return the current status
     */
    public TransferStatus getStatus() {
        TransferStatus transferStatus = new TransferStatus();

        // Upload status
        transferStatus.activeUploads = activeUploads.size();
        transferStatus.queuedUploads = queuedUploads.size();
        transferStatus.maxUploadCPS = getUploadCPSForWAN();
        transferStatus.currentUploadCPS = (long) uploadCounter
            .calculateCurrentCPS();
        transferStatus.uploadedBytesTotal = uploadCounter.getBytesTransferred();

        // Download status
        for (DownloadManager man : dlManagers.values()) {
            for (Download download : man.getSources()) {
                if (download.isStarted()) {
                    transferStatus.activeDownloads++;
                } else {
                    transferStatus.queuedDownloads++;
                }
            }
        }

        transferStatus.maxDownloads = Integer.MAX_VALUE;
        transferStatus.maxDownloadCPS = Double.MAX_VALUE;
        transferStatus.currentDownloadCPS = (long) downloadCounter
            .calculateCurrentCPS();
        transferStatus.downloadedBytesTotal = downloadCounter
            .getBytesTransferred();

        return transferStatus;
    }

    // General Transfer callback methods **************************************

    /**
     * Returns the MultiSourceDownload, that's managing the given info.
     * 
     * @param info
     * @return
     */
    private DownloadManager getDownloadManagerFor(FileInfo info) {
        Validate.notNull(info);
        DownloadManager man = dlManagers.get(info);
        if (man != null
            && man.getFileInfo().isVersionDateAndSizeIdentical(info))
        {
            return man;
        }
        return null;
    }

    /**
     * Sets an transfer as started
     * 
     * @param transfer
     *            the transfer
     */
    void setStarted(Transfer transfer) {
        if (transfer instanceof Upload) {
            uploadsLock.lock();
            try {
                queuedUploads.remove(transfer);
                activeUploads.add((Upload) transfer);
            } finally {
                uploadsLock.unlock();
            }

            // Fire event
            fireUploadStarted(new TransferManagerEvent(this, (Upload) transfer));
        } else if (transfer instanceof Download) {
            // Fire event
            fireDownloadStarted(new TransferManagerEvent(this,
                (Download) transfer));
        }

        if (isFine()) {
            logFine("Started: " + transfer);
        }
    }

    /**
     * Callback to inform, that a download has been enqued at the remote ide
     * 
     * @param download
     *            the download request
     * @param member
     */
    public void downloadQueued(Download download, Member member) {
        Reject.noNullElements(download, member);
        fireDownloadQueued(new TransferManagerEvent(this, download));
    }

    /**
     * Sets a transfer as broken, removes from queues
     * 
     * @param upload
     *            the upload
     * @param transferProblem
     *            the problem that broke the transfer
     */
    void uploadBroken(Upload upload, TransferProblem transferProblem) {
        uploadBroken(upload, transferProblem, null);
    }

    void downloadBroken(Download download, TransferProblem problem,
        String problemInfo)
    {
        logWarning("Download broken: " + download + ' '
            + (problem == null ? "" : problem) + ": " + problemInfo);

        download.setTransferProblem(problem);
        download.setProblemInformation(problemInfo);

        removeDownload(download);

        // Fire event
        fireDownloadBroken(new TransferManagerEvent(this, download, problem,
            problemInfo));
    }

    /**
     * Sets a transfer as broken, removes from queues
     * 
     * @param upload
     *            the upload
     * @param transferProblem
     *            the problem that broke the transfer
     * @param problemInformation
     *            specific information about the problem
     */
    void uploadBroken(Upload upload, TransferProblem transferProblem,
        String problemInformation)
    {
        // Ensure shutdown
        upload.shutdown();
        logWarning("Upload broken: " + upload + ' '
            + (transferProblem == null ? "" : transferProblem) + ": "
            + problemInformation);
        uploadsLock.lock();
        boolean transferFound = false;
        try {
            transferFound = queuedUploads.remove(upload);
            transferFound = transferFound || activeUploads.remove(upload);
        } finally {
            uploadsLock.unlock();
        }

        // Tell remote peer if possible
        if (upload.getPartner().isCompletelyConnected()) {
            if (isFine()) {
                logFine("Sending abort upload of " + upload);
            }
            upload.getPartner().sendMessagesAsynchron(
                new AbortUpload(upload.getFile()));
        }

        // Fire event
        if (transferFound) {
            fireUploadBroken(new TransferManagerEvent(this, upload));
        }

        // Now trigger, to check uploads/downloads to start
        triggerTransfersCheck();
    }

    /**
     * Breaks all transfers on that folder, usually on folder remove
     * 
     * @param foInfo
     */
    public void breakTransfers(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folderinfo is null");
        // Search for uls to break
        if (!queuedUploads.isEmpty()) {
            for (Upload upload : queuedUploads) {
                if (foInfo.equals(upload.getFile().getFolderInfo())) {
                    uploadBroken(upload, TransferProblem.FOLDER_REMOVED,
                        foInfo.name);
                }
            }
        }

        if (!activeUploads.isEmpty()) {
            for (Upload upload : activeUploads) {
                if (foInfo.equals(upload.getFile().getFolderInfo())) {
                    uploadBroken(upload, TransferProblem.FOLDER_REMOVED,
                        foInfo.name);
                }
            }
        }

        for (DownloadManager man : dlManagers.values()) {
            if (foInfo.equals(man.getFileInfo().getFolderInfo())) {
                man.setBroken(TransferProblem.FOLDER_REMOVED, foInfo.name);
            }
        }
    }

    /**
     * Breaks all transfers from and to that node. Usually done on disconnect
     * 
     * @param node
     */
    public void breakTransfers(Member node) {
        // Search for uls to break
        if (!queuedUploads.isEmpty()) {
            for (Upload upload : queuedUploads) {
                if (node.equals(upload.getPartner())) {
                    uploadBroken(upload, TransferProblem.NODE_DISCONNECTED,
                        node.getNick());
                }
            }
        }

        if (!activeUploads.isEmpty()) {
            for (Upload upload : activeUploads) {
                if (node.equals(upload.getPartner())) {
                    uploadBroken(upload, TransferProblem.NODE_DISCONNECTED,
                        node.getNick());
                }
            }
        }

        for (DownloadManager man : dlManagers.values()) {
            for (Download download : man.getSources()) {
                if (node.equals(download.getPartner())) {
                    download.setBroken(TransferProblem.NODE_DISCONNECTED,
                        node.getNick());
                }
            }
        }
    }

    /**
     * Breaks all transfers on the file. Usually when the file is going to be
     * changed. TODO: Transfers have been ABORTED instead of BROKEN before.
     * 
     * @param fInfo
     */
    public void breakTransfers(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo is null");
        // Search for uls to break
        if (!queuedUploads.isEmpty()) {
            for (Upload upload : queuedUploads) {
                if (fInfo.equals(upload.getFile())) {
                    uploadBroken(upload, TransferProblem.FILE_CHANGED,
                        fInfo.getRelativeName());
                }
            }
        }

        if (!activeUploads.isEmpty()) {
            for (Upload upload : activeUploads) {
                if (fInfo.equals(upload.getFile())) {
                    upload.abort();
                    uploadBroken(upload, TransferProblem.FILE_CHANGED,
                        fInfo.getRelativeName());
                }
            }
        }

        for (DownloadManager man : dlManagers.values()) {
            if (fInfo.equals(man.getFileInfo())) {
                man.setBroken(TransferProblem.FILE_CHANGED,
                    fInfo.getRelativeName());
            }
        }
    }

    void setCompleted(final DownloadManager dlManager) {
        assert dlManager.isDone();

        final FileInfo fInfo = dlManager.getFileInfo();
        // Inform other folder member of added file
        final Folder folder = fInfo.getFolder(getController()
            .getFolderRepository());
        if (folder != null) {
            // scan in new downloaded file
            // TODO React on failed scan?
            // TODO PREVENT further uploads of this file unless it's "there"
            // Search for active uploads of the file and break them
            boolean abortedUL;
            int tries = 0;
            do {
                abortedUL = abortUploadsOf(fInfo);
                tries++;
                if (abortedUL) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
            } while (abortedUL);

            assert getActiveUploads(fInfo).isEmpty();

            if (folder.scanDownloadFile(fInfo, dlManager.getTempFile())) {
                if (StringUtils.isNotBlank(folder.getDownloadScript())) {
                    Runnable scriptRunner = new Runnable() {
                        public void run() {
                            executeDownloadScript(fInfo, folder, dlManager);
                        }
                    };
                    doWork(scriptRunner);
                }
            } else {
                logWarning("Scanning of completed file failed: "
                    + fInfo.toDetailString() + " at " + dlManager.getTempFile());
                dlManager.setBroken(
                    TransferProblem.FILE_CHANGED,
                    "Scanning of completed file failed: "
                        + fInfo.toDetailString());
                return;
            }
        }
        completedDownloads.put(new FileInfoKey(fInfo, Type.VERSION_DATE_SIZE),
            dlManager);
        for (Download d : dlManager.getSources()) {
            d.setCompleted();
        }
        removeDownloadManager(dlManager);

        // Auto cleanup of Downloads
        boolean autoClean = dlManager.getFileInfo().getFolderInfo()
            .isMetaFolder();
        autoClean = autoClean
            || ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY
                .getValueInt(getController()) == 0;
        if (autoClean) {
            if (isFiner()) {
                logFiner("Auto-cleaned " + dlManager.getSources());
            }
            clearCompletedDownload(dlManager);
        }

        if (fInfo.getFolderInfo().isMetaFolder()) {
            MetaFolderDataHandler mfdh = new MetaFolderDataHandler(
                getController());
            mfdh.handleMetaFolderFileInfo(fInfo);
        }
    }

    private ReentrantLock scriptLock = new ReentrantLock();

    /**
     * #1538
     * <p>
     * http://www.powerfolder.com/wiki/Script_execution
     * 
     * @param fInfo
     * @param folder
     */
    private void executeDownloadScript(FileInfo fInfo, Folder folder,
        DownloadManager dlManager)
    {
        Reject
            .ifBlank(folder.getDownloadScript(), "Download script is not set");
        Path dlFile = fInfo.getDiskFile(getController().getFolderRepository());
        String command = folder.getDownloadScript();
        command = command.replace("$file", dlFile.toAbsolutePath().toString());
        command = command.replace("$path", dlFile.getParent().toString());
        command = command.replace("$folderpath", folder.getLocalBase()
            .toAbsolutePath().toString());

        StringBuilder sourcesStr = new StringBuilder();
        for (Download source : dlManager.getSources()) {
            Member p = source.getPartner();
            if (p != null) {
                sourcesStr.append(p.getNick());
            }
            sourcesStr.append(',');
        }
        if (sourcesStr.length() > 0) {
            // Cut last ,
            sourcesStr.setLength(sourcesStr.length() - 1);
        }
        command = command.replace("$sources", sourcesStr);

        try {
            scriptLock.lock();
            logInfo("Begin executing command: " + command);
            final Process p = Runtime.getRuntime().exec(command);
            // Auto-kill after 20 seconds
            getController().schedule(new Runnable() {
                public void run() {
                    p.destroy();
                }
            }, 20000L);
            byte[] out = StreamUtils.readIntoByteArray(p.getInputStream());
            String output = new String(out);
            byte[] err = StreamUtils.readIntoByteArray(p.getErrorStream());
            String error = new String(err);
            int res = p.waitFor();
            logInfo("Executed command finished (exit value: " + res + "): "
                + command + " | stdout: " + output + ", stderr: " + error);
        } catch (IOException e) {
            logSevere(
                "Unable to execute script after download. '"
                    + folder.getDownloadScript() + "' file: " + dlFile
                    + ", command: " + command + ". " + e, e);
        } catch (InterruptedException e) {
            logSevere("Abnormal termination of script after download. '"
                + folder.getDownloadScript() + "' file: " + dlFile
                + ", command: " + command + ". " + e, e);
        } finally {
            scriptLock.unlock();
        }
    }

    private boolean abortUploadsOf(FileInfo fInfo) {
        uploadsLock.lock();
        boolean abortedUL = false;
        try {
            for (Upload u : activeUploads) {
                if (u.getFile().equals(fInfo)) {
                    abortedUL = abortUpload(fInfo, u.getPartner()) || abortedUL;
                }
            }
            List<Upload> remove = new LinkedList<Upload>();
            for (Upload u : queuedUploads) {
                if (u.getFile().equals(fInfo)) {
                    abortedUL = true;
                    remove.add(u);
                }
            }
            queuedUploads.removeAll(remove);
        } finally {
            uploadsLock.unlock();
        }
        return abortedUL;
    }

    /**
     * Returns all uploads of the given file.
     * 
     * @param info
     * @return
     */
    private List<Upload> getActiveUploads(FileInfo info) {
        List<Upload> ups = new ArrayList<Upload>();
        for (Upload u : activeUploads) {
            if (u.getFile().equals(info)) {
                ups.add(u);
            }
        }
        return ups;
    }

    /**
     * Coounts the number of active uploads for a folder.
     * 
     * @param folder
     * @return the # of activate uploads
     */
    public int countActiveUploads(Folder folder) {
        int count = 0;
        for (Upload activeUpload : activeUploads) {
            if (activeUpload.getFile().getFolderInfo().equals(folder.getInfo()))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Callback method to indicate that a transfer is completed
     * 
     * @param transfer
     */
    void setCompleted(Transfer transfer) {

        FileInfo fileInfo = transfer.getFile();
        if (transfer instanceof Download) {
            // Fire event
            fireDownloadCompleted(new TransferManagerEvent(this,
                (Download) transfer));

            downloadsCount.remove(transfer.getPartner());
            int nDlFromNode = countActiveAndQueuedDownloads(transfer
                .getPartner());
            boolean requestMoreFiles = nDlFromNode == 0;
            if (!requestMoreFiles) {
                // Hmm maybe end of transfer queue is near (25% or less filled),
                // request if yes!
                if (transfer.getPartner().isOnLAN()) {
                    requestMoreFiles = nDlFromNode <= Constants.MAX_DLS_FROM_LAN_MEMBER / 4;
                } else {
                    requestMoreFiles = nDlFromNode <= Constants.MAX_DLS_FROM_INET_MEMBER / 4;
                }
            }

            if (requestMoreFiles) {
                // Trigger filerequestor
                getController().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting(fileInfo.getFolderInfo());
            } else {
                if (isFiner()) {
                    logFiner("Not triggering file requestor. " + nDlFromNode
                        + " more dls from " + transfer.getPartner());
                }
            }

        } else if (transfer instanceof Upload) {
            transfer.setCompleted();

            uploadsLock.lock();
            boolean transferFound = false;
            try {
                transferFound = queuedUploads.remove(transfer);
                transferFound = activeUploads.remove(transfer) || transferFound;
                completedUploads.add((Upload) transfer);
            } finally {
                uploadsLock.unlock();
            }

            if (transferFound) {
                // Fire event
                fireUploadCompleted(new TransferManagerEvent(this,
                    (Upload) transfer));
            }

            // Auto cleanup of uploads
            boolean autoClean = transfer.getFile().getFolderInfo()
                .isMetaFolder();
            autoClean = autoClean
                || ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
                    .getValueInt(getController()) == 0;
            if (autoClean) {
                if (isFiner()) {
                    logFiner("Auto-cleaned " + transfer);
                }
                clearCompletedUpload((Upload) transfer);
            }
        }

        // Now trigger, to start next transfer
        triggerTransfersCheck();

        if (isFiner()) {
            logFiner("Completed: " + transfer);
        }
    }

    // Upload management ******************************************************

    /**
     * This method is called after any change associated with bandwidth. I.e.:
     * upload limits, paused mode
     */
    public void updateSpeedLimits() {
        // Any setting that is "unlimited" will stay unlimited!
        bandwidthProvider.setLimitBPS(sharedLANOutputHandler,
            getUploadCPSForLAN());
        bandwidthProvider.setLimitBPS(sharedWANOutputHandler,
            getUploadCPSForWAN());
        bandwidthProvider.setLimitBPS(sharedLANInputHandler,
            getDownloadCPSForLAN());
        bandwidthProvider.setLimitBPS(sharedWANInputHandler,
            getDownloadCPSForWAN());
    }

    /**
     * Sets the selected upload bandwidth usage in CPS
     * 
     * @param allowedCPS
     */
    public void setUploadCPSForWAN(long allowedCPS) {

        ConfigurationEntry.UPLOAD_LIMIT_WAN.setValue(getController(),
            String.valueOf(allowedCPS / 1024));

        updateSpeedLimits();

        logInfo("Upload limit: "
            + Format.formatBytesShort(getUploadCPSForWAN()) + "/s");
    }

    /**
     * Returns the upload WAN rate.
     * 
     * @return the selected upload rate (internet) in CPS
     */
    public long getUploadCPSForWAN() {
        return Integer.parseInt(ConfigurationEntry.UPLOAD_LIMIT_WAN
            .getValue(getController())) * 1024;
    }

    /**
     * Sets the selected download bandwidth usage in CPS
     * 
     * @param allowedCPS
     */
    public void setDownloadCPSForWAN(long allowedCPS) {

        ConfigurationEntry.DOWNLOAD_LIMIT_WAN.setValue(getController(),
            String.valueOf(allowedCPS / 1024));

        updateSpeedLimits();

        logInfo("Download limit: "
            + Format.formatBytesShort(getDownloadCPSForWAN()) + "/s");
    }

    /**
     * Returns the download WAN rate.
     * 
     * @return the selected upload rate (internet) in CPS
     */
    public long getDownloadCPSForWAN() {
        return ConfigurationEntry.DOWNLOAD_LIMIT_WAN
            .getValueInt(getController()) * 1024;
    }

    /**
     * Sets the maximum upload bandwidth usage in CPS for LAN
     * 
     * @param allowedCPS
     */
    public void setUploadCPSForLAN(long allowedCPS) {

        ConfigurationEntry.UPLOAD_LIMIT_LAN.setValue(getController(),
            String.valueOf(allowedCPS / 1024));

        updateSpeedLimits();

        logInfo("LAN Upload limit: "
            + Format.formatBytesShort(getUploadCPSForLAN()) + "/s");
    }

    /**
     * @return the upload rate (LAN) in CPS
     */
    public long getUploadCPSForLAN() {
        return ConfigurationEntry.UPLOAD_LIMIT_LAN.getValueInt(getController()) * 1024;
    }

    /**
     * Sets the maximum upload bandwidth usage in CPS for LAN
     * 
     * @param allowedCPS
     */
    public void setDownloadCPSForLAN(long allowedCPS) {

        ConfigurationEntry.DOWNLOAD_LIMIT_LAN.setValue(getController(),
            String.valueOf(allowedCPS / 1024));

        updateSpeedLimits();

        logInfo("LAN Download limit: "
            + Format.formatBytesShort(getDownloadCPSForLAN()) + "/s");
    }

    /**
     * @return the download rate (LAN) in CPS
     */
    public long getDownloadCPSForLAN() {
        return Integer.parseInt(ConfigurationEntry.DOWNLOAD_LIMIT_LAN
            .getValue(getController())) * 1024;
    }

    /**
     * @see ConfigurationEntry#TRANSFERS_MAX_FILE_CHUNK_SIZE
     * @return the maximum size of a {@link FileChunk}
     */
    int getMaxFileChunkSize() {
        return ConfigurationEntry.TRANSFERS_MAX_FILE_CHUNK_SIZE
            .getValueInt(getController());
    }

    /**
     * @see ConfigurationEntry#TRANSFERS_MAX_REQUESTS_QUEUED
     * @return
     */
    int getMaxRequestsQueued() {
        return ConfigurationEntry.TRANSFERS_MAX_REQUESTS_QUEUED
            .getValueInt(getController());
    }

    /**
     * @return the counter for upload speed
     */
    public TransferCounter getUploadCounter() {
        return uploadCounter;
    }

    /**
     * @return the counter for total upload traffic (real)
     */
    public TransferCounter getTotalUploadTrafficCounter() {
        return totalUploadTrafficCounter;
    }

    /**
     * Queues a upload into the upload queue. Breaks all former upload requests
     * for the file from that member. Will not be queued if file not exists or
     * is deleted in the meantime or if the connection with the requestor is
     * lost.
     * 
     * @param from
     * @param dl
     * @return the enqued upload, or null if not queued.
     */
    public Upload queueUpload(final Member from, RequestDownload dl) {
        if (isFine()) {
            logFine("Received download request from " + from + ": " + dl);
        }
        if (dl == null || dl.file == null) {
            throw new NullPointerException("Downloadrequest/File is null");
        }
        // Never upload db files !!
        if (Constants.DB_FILENAME.equalsIgnoreCase(dl.file.getRelativeName())
            || Constants.DB_BACKUP_FILENAME.equalsIgnoreCase(dl.file
                .getRelativeName()))
        {
            logSevere(from.getNick()
                + " has illegally requested to download a folder database file");
            return null;
        }
        Folder folder = dl.file
            .getFolder(getController().getFolderRepository());
        if (folder == null) {
            logWarning("Received illegal download request from "
                + from.getNick() + ". Not longer on folder "
                + dl.file.getFolderInfo());
            return null;
        }
        if (!folder.hasReadPermission(from)) {
            logWarning("No Read permission: " + from + " on " + folder);
            return null;
        }

        if (dlManagers.containsKey(dl.file)) {
            logWarning("Not queuing upload, active download of the file is in progress.");
            return null;
        }

        FolderRepository repo = getController().getFolderRepository();
        Path diskFile = dl.file.getDiskFile(repo);
        boolean fileInSyncWithDisk = diskFile != null
            && dl.file.inSyncWithDisk(diskFile);
        if (!fileInSyncWithDisk) {
            if (diskFile == null) {
                return null;
            }
            if (isWarning()) {
                try {
                    logWarning("File not in sync with disk: '"
                        + dl.file.toDetailString() + "', disk file at "
                        + Files.getLastModifiedTime(diskFile).toMillis());
                } catch (IOException ioe) {
                    logSevere("Could not access modification time of file "
                        + diskFile.toAbsolutePath().toString());
                }
            }

            // This should free up an otherwise waiting for download partner
            if (folder.scanAllowedNow()) {
                folder.scanChangedFile(dl.file);
            }
            // folder.recommendScanOnNextMaintenance();
            return null;
        }

        FileInfo localFile = dl.file.getLocalFileInfo(repo);
        boolean fileInSyncWithDb = localFile != null
            && localFile.isVersionDateAndSizeIdentical(dl.file);
        if (!fileInSyncWithDb) {
            logWarning("File not in sync with db: '" + dl.file.toDetailString()
                + "', but I have "
                + (localFile != null ? localFile.toDetailString() : ""));
            return null;
        }

        final Upload upload = new Upload(this, from, dl);
        if (upload.isBroken()) { // connection lost
            // Check if this download is broken
            return null;
        }

        // Check if we have a old upload to break
        uploadsLock.lock();
        Upload oldUpload = null;
        try {
            int oldUploadIndex = activeUploads.indexOf(upload);
            if (oldUploadIndex >= 0) {
                oldUpload = activeUploads.get(oldUploadIndex);
                activeUploads.remove(oldUploadIndex);
            }
            oldUploadIndex = queuedUploads.indexOf(upload);
            if (oldUploadIndex >= 0) {
                if (oldUpload != null) {
                    // Should never happen
                    throw new IllegalStateException(
                        "Found illegal upload. is in list of queued AND active uploads: "
                            + oldUpload);
                }
                oldUpload = queuedUploads.get(oldUploadIndex);
                queuedUploads.remove(oldUploadIndex);
            }

            logFine("Queued: " + upload + ", startOffset: " + dl.startOffset
                + ", to: " + from);
            queuedUploads.add(upload);
        } finally {
            uploadsLock.unlock();
        }

        // Fire as requested
        fireUploadRequested(new TransferManagerEvent(this, upload));

        if (oldUpload != null) {
            logWarning("Received already known download request for " + dl.file
                + " from " + from.getNick() + ", overwriting old request");
            // Stop former upload request
            oldUpload.abort();
            oldUpload.shutdown();
            uploadBroken(oldUpload, TransferProblem.OLD_UPLOAD, from.getNick());
        }

        // Trigger working thread on upload enqueued
        triggerTransfersCheck();

        // Wait 500ms to let the transfers check grab the new download
        getController().schedule(new Runnable() {
            public void run() {
                // If upload is queued.
                if (!upload.isStarted() && !upload.isCompleted()
                    && !upload.isBroken() && !upload.isAborted())
                {
                    from.sendMessageAsynchron(new DownloadQueued(upload
                        .getFile()));
                } else if (isFiner()) {
                    logFiner("Optimization. Did not send DownloadQueued message for "
                        + upload.getFile() + " to " + upload.getPartner());
                }
            }
        }, 500L);

        return upload;
    }

    /**
     * Aborts a upload
     * 
     * @param fInfo
     *            the file to upload
     * @param to
     *            the member where is file is going to
     * @return true if the upload was actually aborted.
     */
    public boolean abortUpload(FileInfo fInfo, Member to) {
        if (fInfo == null) {
            throw new NullPointerException(
                "Unable to abort upload, file is null");
        }
        if (to == null) {
            throw new NullPointerException(
                "Unable to abort upload, to-member is null");
        }

        Upload abortedUpload = null;
        for (Upload upload : queuedUploads) {
            if (upload.getFile().isVersionDateAndSizeIdentical(fInfo)
                && to.equals(upload.getPartner()))
            {
                // Remove upload from queue
                uploadsLock.lock();
                try {
                    queuedUploads.remove(upload);
                } finally {
                    uploadsLock.unlock();
                }
                upload.abort();
                abortedUpload = upload;
            }
        }

        for (Upload upload : activeUploads) {
            if (upload.getFile().isVersionDateAndSizeIdentical(fInfo)
                && to.equals(upload.getPartner()))
            {
                // Remove upload from queue
                uploadsLock.lock();
                try {
                    activeUploads.remove(upload);
                } finally {
                    uploadsLock.unlock();
                }
                upload.abort();
                abortedUpload = upload;
            }
        }

        if (abortedUpload != null) {
            fireUploadAborted(new TransferManagerEvent(this, abortedUpload));
            // Trigger check
            triggerTransfersCheck();
            return true;
        } else {
            if (isFine()) {
                logFine("Upload to abort not found: " + fInfo + " to " + to);
            }
            return false;
        }
    }

    /**
     * Returns the {@link FileRecordProvider} managing the
     * {@link FilePartsRecord}s for the various uploads/downloads.
     * 
     * @return
     */
    public FileRecordProvider getFileRecordManager() {
        return fileRecordProvider;
    }

    /**
     * Perfoms a upload in the tranfsermanagers threadpool.
     * 
     * @param uploadPerformer
     */
    void perfomUpload(Runnable uploadPerformer) {
        threadPool.submit(uploadPerformer);
    }

    /**
     * Generic stuff to do
     * 
     * @param worker
     */
    void doWork(Runnable worker) {
        threadPool.submit(worker);
    }

    /**
     * @return the number of currently actively transferring uploads.
     */
    public int countActiveUploads() {
        return activeUploads.size();
    }

    /**
     * @return the currently active uploads
     */
    public Collection<Upload> getActiveUploads() {
        return Collections.unmodifiableCollection(activeUploads);
    }

    /**
     * @return an unmodifiable collection reffering to the internal completed
     *         upload list. May change after returned.
     */
    public List<Upload> getCompletedUploadsCollection() {
        return Collections.unmodifiableList(completedUploads);
    }

    /**
     * @return the number of queued uploads.
     */
    public int countQueuedUploads() {
        return queuedUploads.size();
    }

    /**
     * @return the total number of queued / active uploads
     */
    public int countLiveUploads() {
        return activeUploads.size() + queuedUploads.size();
    }

    /**
     * @return the total number of uploads
     */
    public int countAllUploads() {
        return activeUploads.size() + queuedUploads.size()
            + completedUploads.size();
    }

    /**
     * @param folder
     *            the folder.
     * @return the number of uploads on the folder
     */
    public int countUploadsOn(Folder folder) {
        int nUploads = 0;
        for (Upload upload : activeUploads) {
            if (upload.getFile().getFolderInfo().equals(folder.getInfo())) {
                nUploads++;
            }
        }
        for (Upload upload : queuedUploads) {
            if (upload.getFile().getFolderInfo().equals(folder.getInfo())) {
                nUploads++;
            }
        }
        return nUploads;
    }

    /**
     * Answers all queued uploads
     * 
     * @return Array of all queued upload
     */
    public Collection<Upload> getQueuedUploads() {
        return Collections.unmodifiableCollection(queuedUploads);

    }

    /**
     * Answers, if we are uploading to this member
     * 
     * @param member
     *            the receiver
     * @return true if we are uploading to that member
     */
    public boolean uploadingTo(Member member) {
        return uploadingToSize(member) >= 0;
    }

    /**
     * @param member
     *            the receiver
     * @return the total size of bytes of the files currently upload to that
     *         member. Or -1 if not uploading to that member.
     */
    public long uploadingToSize(Member member) {
        long size = 0;
        boolean uploading = false;
        for (Upload upload : activeUploads) {
            if (member.equals(upload.getPartner())) {
                size += upload.getFile().getSize();
                uploading = true;
            }
        }
        if (!uploading) {
            return -1;
        }
        return size;
    }

    // Download management ***************************************************

    /**
     * Be sure to hold downloadsLock when calling this method!
     */
    private void removeDownload(Download download) {
        DownloadManager man = download.getDownloadManager();
        if (man == null) {
            return;
        }
        synchronized (man) {
            downloadsCount.remove(download.getPartner());
            if (man.hasSource(download)) {
                try {
                    man.removeSource(download);
                } catch (Exception e) {
                    logSevere("Unable to remove download: " + download, e);
                }
                if (!man.hasSources()) {
                    if (man.isDone()) {
                        logFine("No further sources in that manager, Not removing it because it's already done");
                    } else {
                        logFine("No further sources, removing " + man);
                        man.setBroken(TransferProblem.BROKEN_DOWNLOAD,
                            "Out of sources for download");
                    }
                }
            }
        }
    }

    /**
     * @return the download counter
     */
    public TransferCounter getDownloadCounter() {
        return downloadCounter;
    }

    /**
     * @return the download traffic counter (real)
     */
    public TransferCounter getTotalDownloadTrafficCounter() {
        return totalDownloadTrafficCounter;
    }

    /**
     * Addds a file for download if source is not known. Download will be
     * started when a source is found.
     * 
     * @param download
     * @return true if succeeded
     */
    public boolean enquePendingDownload(Download download) {
        Validate.notNull(download);

        FileInfo fInfo = download.getFile();
        if (download.isRequestedAutomatic()) {
            logFiner("Not adding pending download, is a auto-dl: " + fInfo);
            return false;
        }

        if (!getController().getFolderRepository().hasJoinedFolder(
            fInfo.getFolderInfo()))
        {
            logWarning("Not adding pending download, not on folder: " + fInfo);
            return false;
        }

        Folder folder = fInfo.getFolder(getController().getFolderRepository());
        FileInfo localFile = folder != null ? folder.getFile(fInfo) : null;
        if (localFile != null && fInfo.isVersionDateAndSizeIdentical(localFile))
        {
            logWarning("Not adding pending download, already have: " + fInfo);
            return false;
        }

        boolean contained = true;

        synchronized (pendingDownloads) {
            if (!pendingDownloads.contains(download)
                && !dlManagers.containsKey(download.getFile()))
            {
                contained = false;
                pendingDownloads.add(0, download);
            }
        }

        if (!contained) {
            logFine("Pending download added for: " + fInfo);
            firePendingDownloadEnqueud(new TransferManagerEvent(this, download));
        }
        return true;
    }

    public DownloadManagerFactory getDownloadManagerFactory() {
        return downloadManagerFactory;
    }

    public void setDownloadManagerFactory(
        DownloadManagerFactory downloadManagerFactory)
    {
        this.downloadManagerFactory = downloadManagerFactory;
    }

    /**
     * Requests to download the newest version of the file. Returns null if
     * download wasn't enqued at a node. Download is then pending
     * 
     * @param fInfo
     * @return the member where the dl was requested at, or null if no source
     *         available (DL was NOT started)
     */
    public DownloadManager downloadNewestVersion(FileInfo fInfo) {
        return downloadNewestVersion(fInfo, false);
    }

    /**
     * Requests to download the newest version of the file. Returns null if
     * download wasn't enqued at a node. Download is then pending or blacklisted
     * (for autodownload)
     * 
     * @param fInfo
     * @param automatic
     *            if this download was requested automatically
     * @return the member where the dl was requested at, or null if no source
     *         available (DL was NOT started) and null if blacklisted
     */
    public DownloadManager downloadNewestVersion(FileInfo fInfo,
        boolean automatic)
    {
        synchronized (downloadRequestLock) {
            Folder folder = fInfo.getFolder(getController()
                .getFolderRepository());
            if (folder == null) {
                // on shutdown folder maybe null here
                return null;
            }
            if (!started) {
                return null;
            }
            // FIXME Does not actually CHECK the base dir, but takes result of
            // last
            // scan. Possible problem on Mac/Linux: Unmounted path might exist.
            if (folder.isDeviceDisconnected()) {
                return null;
            }

            // return null if in blacklist on automatic download
            if (folder.getDiskItemFilter().isExcluded(fInfo)) {
                return null;
            }

            if (!fInfo.isValid()) {
                return null;
            }

            // Now walk through all sources and get the best one
            // Member bestSource = null;
            FileInfo newestVersionFile = fInfo.getNewestVersion(getController()
                .getFolderRepository());
            FileInfo localFile = folder.getFile(fInfo);
            FileInfo fileToDl = newestVersionFile != null
                ? newestVersionFile
                : fInfo;

            // Check if we have the file already downloaded in the meantime.
            // Or we have this file actual on disk but not in own db yet.
            if (localFile != null && !fileToDl.isNewerThan(localFile)) {
                if (isFiner()) {
                    logFiner("NOT requesting download, already has latest file in own db: "
                        + fInfo.toDetailString());
                }
                return null;
            } else if (fileToDl.inSyncWithDisk(fInfo
                .getDiskFile(getController().getFolderRepository())))
            {
                if (isFiner()) {
                    logFiner("NOT requesting download, file seems already to exists on disk: "
                        + fInfo.toDetailString());
                }
                // Disabled: Causes bottleneck on many transfers
                // DB seems to be out of sync. Recommend scan
                // Folder f = fInfo.getFolder(getController()
                // .getFolderRepository());
                // if (f != null) {
                // f.recommendScanOnNextMaintenance();
                // }
                return null;
            }

            List<Member> sources = getSourcesWithFreeUploadCapacity(fInfo);

            Collection<Member> bestSources = null;
            for (Member source : sources) {
                FileInfo remoteFile = source.getFile(fInfo);
                if (remoteFile == null) {
                    continue;
                }
                // Skip "wrong" sources
                if (!fileToDl.isVersionDateAndSizeIdentical(remoteFile)) {
                    continue;
                }
                if (bestSources == null) {
                    bestSources = new LinkedList<Member>();
                }
                bestSources.add(source);
            }

            if (bestSources != null) {
                for (Member bestSource : bestSources) {
                    Download download = new Download(this, fileToDl, automatic);
                    if (isFiner()) {
                        logFiner("Best source for " + fInfo + " is "
                            + bestSource);
                    }
                    if (localFile != null
                        && localFile.getModifiedDate().after(
                            fileToDl.getModifiedDate())
                        && !localFile.isDeleted())
                    {
                        if (isFine()) {
                            logFine("Requesting older file: "
                                + fileToDl.toDetailString() + ", local: "
                                + localFile.toDetailString()
                                + ", localIsNewer: "
                                + localFile.isNewerThan(fileToDl));
                        }
                    }
                    if (fileToDl.isNewerAvailable(getController()
                        .getFolderRepository()))
                    {
                        logFine("Downloading old version while newer is available: "
                            + localFile);
                    }
                    requestDownload(download, bestSource);
                }
            }

            if (bestSources == null && !automatic) {
                // Okay enque as pending download if was manually requested
                enquePendingDownload(new Download(this, fileToDl, automatic));
                return null;
            }
            return getActiveDownload(fInfo);
        }
    }

    /**
     * Requests the download from that member
     * 
     * @param download
     * @param from
     */
    private void requestDownload(Download download, Member from) {
        FileInfo fInfo = download.getFile();
        // Lock/Disable transfer checker
        DownloadManager man;
        synchronized (dlManagers) {
            Download dl = getActiveDownload(from, fInfo);
            if (dl != null) {
                if (isFiner()) {
                    logFiner("Already downloading " + fInfo.toDetailString()
                        + " from " + from);
                }
                return;
            }
            if (fInfo.isVersionDateAndSizeIdentical(fInfo.getFolder(
                getController().getFolderRepository()).getFile(fInfo)))
            {
                // Skip exact same version etc.
                if (isFiner()) {
                    logFiner("Not requesting download, already have latest file version: "
                        + fInfo.toDetailString());
                }
                return;
            }

            man = dlManagers.get(fInfo);

            if (man == null
                || !fInfo.isVersionDateAndSizeIdentical(man.getFileInfo()))
            {
                if (man != null) {
                    if (!man.isDone()) {
                        logWarning("Aborting download. Got active download of different file version: "
                            + man);
                        man.abortAndCleanup();
                    }
                    return;
                }

                try {
                    man = downloadManagerFactory
                        .createDownloadManager(getController(), fInfo,
                            download.isRequestedAutomatic());
                    man.init(false);
                } catch (IOException e) {
                    // Something gone badly wrong
                    logSevere("IOException", e);
                    return;
                }
                if (dlManagers.put(fInfo, man) != null) {
                    throw new AssertionError("Found old manager!");
                }
            }
        }

        if (abortUploadsOf(fInfo)) {
            logFine("Aborted uploads of file to be downloaded: "
                + fInfo.toDetailString());
        }

        boolean dlWasRequested = false;
        synchronized (man) {
            if (fInfo.isVersionDateAndSizeIdentical(fInfo.getFolder(
                getController().getFolderRepository()).getFile(fInfo)))
            {
                // Skip exact same version etc.
                logInfo("Aborting download, already have latest file version: "
                    + fInfo.toDetailString());
                man.abort();
                return;
            }
            if (man.getSourceFor(from) == null && !man.isDone()
                && man.canAddSource(from))
            {
                if (isFine()) {
                    logFine("Requesting " + fInfo.toDetailString() + " from "
                        + from);
                }

                if (fInfo.isNewerThan(man.getFileInfo())) {
                    logSevere("Requested newer download: " + download
                        + " than " + man);
                }
                pendingDownloads.remove(download);
                downloadsCount.remove(from);
                download.setPartner(from);
                download.setDownloadManager(man);
                dlWasRequested = man.addSource(download);
            }
        }

        if (dlWasRequested) {
            if (isFiner()) {
                logFiner("File really was requested!"
                    + download.getFile().toDetailString());
            }
            // Fire event
            fireDownloadRequested(new TransferManagerEvent(this, download));
        }
    }

    /**
     * Returns only sources which are connected and have "exactly" the given
     * FileInfo version.
     * 
     * @param fInfo
     * @return
     */
    public Collection<Member> getSourcesForVersion(FileInfo fInfo) {
        Reject.notNull(fInfo, "fInfo");

        Folder folder = fInfo.getFolder(getController().getFolderRepository());
        if (folder == null) {
            throw new NullPointerException("Folder not joined of file: "
                + fInfo);
        }
        List<Member> sources = null;
        for (Member node : folder.getMembersAsCollection()) {
            FileInfo rInfo = node.getFile(fInfo);
            if (node.isCompletelyConnected() && !node.isMySelf()
                && fInfo.isVersionDateAndSizeIdentical(rInfo))
            {
                // node is connected and has file
                if (sources == null) {
                    sources = new ArrayList<Member>();
                }
                sources.add(node);
            }
        }
        if (sources == null) {
            sources = Collections.emptyList();
        }
        return sources;
    }

    // TODO Does all this "sources" management really belong to the
    // TransferManager?

    /**
     * Finds the sources for the file. Returns only sources which are connected
     * The members are sorted in order of best source.
     * <p>
     * WARNING: The result contains only sources having the same file versions.
     * 
     * @param fInfo
     * @return the list of members, where the file is available
     */
    public List<Member> getSourcesFor(FileInfo fInfo) {
        return getSourcesFor0(fInfo, false, true);
    }

    /**
     * Finds the sources for the file. Returns only sources which are connected
     * The members are sorted in order of best source.
     * <p>
     * WARNING: Result contains sources with any file versions.
     * 
     * @param fInfo
     * @return the list of members, where the file is available
     */
    public List<Member> getSourcesForAnyVersion(FileInfo fInfo) {
        return getSourcesFor0(fInfo, false, false);
    }

    private List<Member> getSourcesWithFreeUploadCapacity(FileInfo fInfo) {
        return getSourcesFor0(fInfo, true, true);
    }

    /**
     * Finds the sources for the file. Returns only sources which are connected
     * The members are sorted in order of best source.
     * <p>
     * WARNING: Versions of files are ignored
     * 
     * @param fInfo
     * @param withUploadCapacityOnly
     *            if only those sources should be considered, that have free
     *            upload capacity.
     * @param onlyIdenticalVersion
     *            return sources that have identical file versions.
     * @return the list of members, where the file is available
     */
    private List<Member> getSourcesFor0(FileInfo fInfo,
        boolean withUploadCapacityOnly, boolean onlyIdenticalVersion)
    {
        Reject.ifNull(fInfo, "File is null");
        Folder folder = fInfo.getFolder(getController().getFolderRepository());
        if (folder == null) {
            throw new NullPointerException("Folder not joined of file: "
                + fInfo);
        }

        // List<Member> nodes = getController().getNodeManager()
        // .getNodeWithFileListFrom(fInfo.getFolderInfo());
        List<Member> sources = null;
        // List<Member> sources = new ArrayList<Member>(nodes.size());
        for (Member node : folder.getMembersAsCollection()) {
            FileInfo fRemoteInfo = node.getFile(fInfo);
            if (node.isCompletelyConnected() && !node.isMySelf()
                && fRemoteInfo != null)
            {
                if (withUploadCapacityOnly && !hasUploadCapacity(node)) {
                    continue;
                }
                if (onlyIdenticalVersion
                    && fInfo.getVersion() != fRemoteInfo.getVersion())
                {
                    continue;
                }

                // node is connected and has file
                if (sources == null) {
                    sources = new ArrayList<Member>();
                }
                sources.add(node);
            }
        }
        if (sources == null) {
            return Collections.emptyList();
        }
        // Sort by the best upload availibility
        Collections.shuffle(sources);
        Collections.sort(sources, new ReverseComparator<Member>(
            MemberComparator.BY_UPLOAD_AVAILIBILITY));

        return sources;
    }

    /**
     * Aborts all automatically enqueued download of a folder.
     * 
     * @param folder
     *            the folder to break downloads on
     */
    public void abortAllAutodownloads(Folder folder) {
        int aborted = 0;
        for (DownloadManager dl : getActiveDownloads()) {
            boolean fromFolder = folder.getInfo().equals(
                dl.getFileInfo().getFolderInfo());
            if (fromFolder && dl.isRequestedAutomatic()) {
                // Abort
                dl.abort();
                aborted++;
            }
        }
        logFine("Aborted " + aborted + " downloads on " + folder);
    }

    /**
     * Aborts all automatically enqueued download of a folder.
     * 
     * @param folder
     *            the folder to break downloads on
     */
    public void abortDownloads(Visitor<DownloadManager> vistor) {
        int aborted = 0;
        for (DownloadManager dl : getActiveDownloads()) {
            if (vistor.visit(dl)) {
                dl.abortAndCleanup();
                aborted++;
            }
        }
        if (aborted > 0 && isFine()) {
            logFine("Aborted " + aborted + " downloads");
        }
    }

    void downloadManagerAborted(DownloadManager manager) {
        logWarning("Aborted download: " + manager);
        removeDownloadManager(manager);
    }

    void downloadManagerBroken(DownloadManager manager,
        TransferProblem problem, String message)
    {
        removeDownloadManager(manager);
        if (!manager.isRequestedAutomatic()) {
            enquePendingDownload(new Download(this, manager.getFileInfo(),
                manager.isRequestedAutomatic()));
        }
    }

    /**
     * @param manager
     */
    private void removeDownloadManager(DownloadManager manager) {
        assert manager.isDone() : "Manager to remove is NOT done!";
        dlManagers.remove(manager.getFileInfo(), manager);
    }

    /**
     * Aborts an download. Gets removed compeletly.
     */
    void downloadAborted(Download download) {
        pendingDownloads.remove(download);
        removeDownload(download);
        // Fire event
        fireDownloadAborted(new TransferManagerEvent(this, download));
    }

    /**
     * abort a download, only if the downloading partner is the same
     * 
     * @param fileInfo
     * @param from
     */
    public void abortDownload(FileInfo fileInfo, Member from) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        Reject.ifNull(from, "From is null");
        Download download = getActiveDownload(from, fileInfo);
        if (download != null) {
            assert download.getPartner().equals(from);
            if (isFiner()) {
                logFiner("downloading changed file, aborting it! "
                    + fileInfo.toDetailString() + ' ' + from);
            }
            download.abort(false);
        } else {
            for (Download pendingDL : pendingDownloads) {
                if (pendingDL.getFile().equals(fileInfo)
                    && pendingDL.getPartner() != null
                    && pendingDL.getPartner().equals(from))
                {
                    if (isFiner()) {
                        logFiner("Aborting pending download! "
                            + fileInfo.toDetailString() + ' ' + from);
                    }
                    pendingDL.abort(false);
                }
            }
        }
    }

    /**
     * Clears a completed downloads
     */
    public void clearCompletedDownload(DownloadManager dlMan) {
        if (completedDownloads.remove(new FileInfoKey(dlMan.getFileInfo(),
            Type.VERSION_DATE_SIZE)) == null)
        {
            logSevere("Completed download manager not found: " + dlMan);
        }
        for (Download download : dlMan.getSources()) {
            fireCompletedDownloadRemoved(new TransferManagerEvent(this,
                download));
        }
    }

    /**
     * Clears a completed uploads
     */
    public void clearCompletedUpload(Upload upload) {
        if (completedUploads.remove(upload)) {
            fireCompletedUploadRemoved(new TransferManagerEvent(this, upload));
        }
    }

    /**
     * Invoked by Download, if a new chunk was received
     * 
     * @param d
     * @param chunk
     */
    public void chunkAdded(Download d, FileChunk chunk) {
        Reject.noNullElements(d, chunk);
        downloadCounter.chunkTransferred(chunk);
    }

    /**
     * @param fInfo
     * @return true if that file gets downloaded
     */
    public boolean isDownloadingActive(FileInfo fInfo) {
        return dlManagers.containsKey(fInfo);
    }

    /**
     * @param fInfo
     * @return true if the file is enqued as pending download
     */
    public boolean isDownloadingPending(FileInfo fInfo) {
        Reject.ifNull(fInfo, "File is null");
        for (Download d : pendingDownloads) {
            if (d.getFile().equals(fInfo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param fInfo
     * @return true if that file is uploading, or queued
     */
    public boolean isUploading(FileInfo fInfo) {
        for (Upload upload : activeUploads) {
            if (upload.getFile() == fInfo) {
                return true;
            }
        }
        for (Upload upload : queuedUploads) {
            if (upload.getFile() == fInfo) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param fInfo
     * @param internet
     * @return if there is an active upload for the given file.
     */
    public boolean isUploadActive(FileInfo fInfo, boolean internet) {
        for (Upload upload : activeUploads) {
            if (internet && upload.getPartner().isOnLAN()) {
                continue;
            }
            if (upload.getFile().equals(fInfo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the upload for the given file to the given member
     * 
     * @param to
     *            the member which the upload transfers to
     * @param fInfo
     *            the file which is transfered
     * @return the Upload or null if there is none
     */
    public Upload getUpload(Member to, FileInfo fInfo) {
        for (Upload u : activeUploads) {
            if (u.getFile().isVersionDateAndSizeIdentical(fInfo)
                && u.getPartner().equals(to))
            {
                return u;
            }
        }
        for (Upload u : queuedUploads) {
            if (u.getFile().isVersionDateAndSizeIdentical(fInfo)
                && u.getPartner().equals(to))
            {
                return u;
            }
        }
        return null;
    }

    /**
     * @param from
     * @param fInfo
     * @return The download of the file that has been completed from this
     *         member.
     */
    public Download getCompletedDownload(Member from, FileInfo fInfo) {
        DownloadManager man = getCompletedDownload(fInfo);
        if (man == null) {
            return null;
        }
        Download d = man.getSourceFor(from);
        if (d == null) {
            return null;
        }
        assert d.getPartner().equals(from);
        return d;
    }

    /**
     * @param fInfo
     * @return the download manager containing the completed file, otherwise
     *         null
     */
    public DownloadManager getCompletedDownload(FileInfo fInfo) {
        return completedDownloads.get(new FileInfoKey(fInfo,
            Type.VERSION_DATE_SIZE));
    }

    public Download getActiveDownload(Member from, FileInfo fInfo) {
        DownloadManager man = getDownloadManagerFor(fInfo);
        if (man == null) {
            return null;
        }
        Download d = man.getSourceFor(from);
        if (d == null) {
            return null;
        }
        assert d.getPartner().equals(from);
        return d;
    }

    /**
     * @param fInfo
     * @return the active download for a file
     */
    public DownloadManager getActiveDownload(FileInfo fInfo) {
        return getDownloadManagerFor(fInfo);
    }

    /**
     * @param fileInfo
     * @return the pending download for the file
     */
    public Download getPendingDownload(FileInfo fileInfo) {
        for (Download pendingDl : pendingDownloads) {
            if (fileInfo.equals(pendingDl.getFile())) {
                return pendingDl;
            }
        }
        return null;
    }

    /**
     * @param folder
     *            the folder
     * @return the number of downloads (active & queued) from on a folder.
     */
    public int countNumberOfDownloads(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        int n = 0;

        for (DownloadManager man : dlManagers.values()) {
            for (Download download : man.getSources()) {
                if (download.getFile().getFolderInfo().equals(folder.getInfo()))
                {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * @param from
     * @return the number of downloads (active & queued) from a member
     */
    public int getNumberOfDownloadsFrom(Member from) {
        if (from == null) {
            throw new NullPointerException("From is null");
        }
        int n = 0;
        for (DownloadManager man : dlManagers.values()) {
            if (man.getSourceFor(from) != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * @return the internal unodifiable collection of pending downloads.
     */
    public Collection<Download> getPendingDownloads() {
        return Collections.unmodifiableCollection(pendingDownloads);
    }

    /**
     * @return unodifiable instance to the internal active downloads collection.
     */
    public Collection<DownloadManager> getActiveDownloads() {
        return Collections.unmodifiableCollection(dlManagers.values());
    }

    /**
     * @return the number of completed downloads
     */
    public int countCompletedDownloads() {
        return completedDownloads.size();
    }

    public int countCompletedDownloads(Folder folder) {
        int count = 0;
        for (DownloadManager completedDownload : completedDownloads.values()) {
            Folder f = completedDownload.getFileInfo().getFolder(
                getController().getFolderRepository());
            if (f != null && f.getInfo().equals(folder.getInfo())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the number of active downloads for a folder.
     * 
     * @param folder
     * @return
     */
    public int countActiveDownloads(Folder folder) {
        int count = 0;
        for (DownloadManager activeDownload : dlManagers.values()) {
            Folder f = activeDownload.getFileInfo().getFolder(
                getController().getFolderRepository());
            if (f != null && f.getInfo().equals(folder.getInfo())) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return an unmodifiable collection reffering to the internal completed
     *         downloads list. May change after returned.
     */
    public Collection<DownloadManager> getCompletedDownloadsCollection() {
        return Collections.unmodifiableCollection(completedDownloads.values());
    }

    /**
     * @param filter
     * @return modifiable, filtered list of completed downloads (not sorted)
     */
    public List<DownloadManager> getCompletedDownloadsCollection(
        Filter<DownloadManager> filter)
    {
        Reject.ifNull(filter, "Filter");
        if (completedDownloads.isEmpty()) {
            return Collections.emptyList();
        }
        List<DownloadManager> dms = new ArrayList<DownloadManager>();
        for (DownloadManager dm : completedDownloads.values()) {
            if (filter.accept(dm)) {
                dms.add(dm);
            }
        }
        return dms;
    }

    /**
     * @param fInfo
     * @return true if the file was recently downloaded (in the list of
     *         completed downloads);
     */
    public boolean isCompletedDownload(FileInfo fInfo) {
        return completedDownloads.get(new FileInfoKey(fInfo,
            Type.VERSION_DATE_SIZE)) != null;
    }

    /**
     * @return the number of all downloads
     */
    public int countActiveDownloads() {
        return dlManagers.size();
    }

    /**
     * @return the number of total downloads (queued, active, pending and
     *         completed)
     */
    public int countTotalDownloads() {
        return countActiveDownloads() + pendingDownloads.size()
            + completedDownloads.size();
    }

    /**
     * @param node
     * @return true if the remote node has free upload capacity.
     */
    public boolean hasUploadCapacity(Member node) {
        int nDownloadFrom = countActiveAndQueuedDownloads(node);
        int maxAllowedDls = node.isOnLAN()
            ? Constants.MAX_DLS_FROM_LAN_MEMBER
            : Constants.MAX_DLS_FROM_INET_MEMBER;
        return nDownloadFrom < maxAllowedDls;
    }

    /**
     * Counts the number of downloads from this node.
     * 
     * @param node
     *            the node to check
     * @return Number of active or enqued downloads to that node
     */
    private int countActiveAndQueuedDownloads(Member node) {
        Integer cached = downloadsCount.get(node);
        if (cached != null) {
            // cacheHit++;
            // if (cacheHit % 1000 == 0) {
            // logWarning(
            // "countActiveAndQueuedDownloads cache hit count: "
            // + cacheHit);
            // }
            return cached;
        }
        int nDownloadsFrom = 0;
        for (DownloadManager man : dlManagers.values()) {
            for (Download download : man.getSources()) {
                if (download.getPartner().equals(node)
                    && !download.isCompleted() && !download.isBroken())
                {
                    nDownloadsFrom++;
                }
            }
        }
        downloadsCount.put(node, nDownloadsFrom);
        return nDownloadsFrom;
    }

    /**
     * Answers if we have active downloads from that member (not used)
     * 
     * @param from
     * @return
     */
    /*
     * private boolean hasActiveDownloadsFrom(Member from) { synchronized
     * (downloads) { for (Iterator it = downloads.values().iterator();
     * it.hasNext();) { Download download = (Download) it.next(); if
     * (download.isStarted() && download.getFrom().equals(from)) { return true;
     * } } } return false; }
     */

    /**
     * Loads all pending downloads and enqueus them for re-download
     */
    private void loadDownloads() {
        Path transferFile = Controller.getMiscFilesLocation().resolve(
            getController().getConfigName() + ".transfers");
        if (Files.notExists(transferFile)) {
            logFine("No downloads to restore, "
                + transferFile.toAbsolutePath() + " does not exists");
            return;
        }
        try (ObjectInputStream oIn = new ObjectInputStream(new FileInputStream(
            transferFile.toAbsolutePath().toString()))) {
            List<?> storedDownloads = (List<?>) oIn.readObject();
            // Reverse to restore in right order
            Collections.reverse(storedDownloads);

            if (storedDownloads.size() > 10000) {
                logWarning("Got many completed downloads ("
                    + storedDownloads.size() + "). Cleanup is recommended");
            }
            // #1705: Speed up of start
            Map<FileInfo, List<DownloadManager>> tempMap = new HashMap<FileInfo, List<DownloadManager>>();
            for (Object storedDownload : storedDownloads) {
                Download download = (Download) storedDownload;

                // Initalize after deserialisation
                download.init(this);
                if (download.isCompleted()) {
                    List<DownloadManager> dlms = tempMap
                        .get(download.getFile());
                    if (dlms == null) {
                        dlms = new ArrayList<DownloadManager>(1);
                        tempMap.put(download.getFile(), dlms);
                    }

                    DownloadManager man = null;
                    for (DownloadManager dlm : dlms) {
                        if (dlm.getFileInfo().isVersionDateAndSizeIdentical(
                            download.getFile()))
                        {
                            man = dlm;
                            break;
                        }
                    }

                    if (man == null) {
                        man = downloadManagerFactory.createDownloadManager(
                            getController(), download.getFile(),
                            download.isRequestedAutomatic());
                        man.init(true);
                        completedDownloads.put(
                            new FileInfoKey(man.getFileInfo(),
                                Type.VERSION_DATE_SIZE), man);
                        // For faster loading
                        dlms.add(man);
                    }
                    // FIXME The UI shouldn't access Downloads directly anyway,
                    // there should be a representation suitable for that.
                    if (download.getPartner() != null) {
                        download.setDownloadManager(man);
                        if (man.canAddSource(download.getPartner())) {
                            man.addSource(download);
                        }
                    }
                } else if (download.isPending()) {
                    enquePendingDownload(download);
                }
            }

            logFine("Loaded " + storedDownloads.size() + " downloads");
        } catch (IOException e) {
            logSevere("Unable to load pending downloads", e);
            try {
                Files.delete(transferFile);
            } catch (IOException ioe) {
                logSevere("Unable to delete transfer file!");
            }
        } catch (ClassNotFoundException e) {
            logSevere("Unable to load pending downloads", e);
            try {
                Files.delete(transferFile);
            } catch (IOException ioe) {
                logSevere("Unable to delete pending downloads file!");
            }
        } catch (ClassCastException e) {
            logSevere("Unable to load pending downloads", e);
            try {
                Files.delete(transferFile);
            } catch (IOException ioe) {
                logSevere("Unable to delete pending downloads file!");
            }
        }
    }

    /**
     * Stores all downloads to disk
     */
    public void storeDownloads() {
        // Store pending downloads
        try {
            // Collect all download infos
            List<Download> storedDownloads = new LinkedList<Download>(
                pendingDownloads);
            int nPending = countActiveDownloads();
            int nCompleted = completedDownloads.size();
            for (DownloadManager man : dlManagers.values()) {
                storedDownloads.add(new Download(this, man.getFileInfo(), man
                    .isRequestedAutomatic()));
            }

            for (DownloadManager man : completedDownloads.values()) {
                storedDownloads.addAll(man.getSources());
            }

            logFiner("Storing " + storedDownloads.size() + " downloads ("
                + nPending + " pending, " + nCompleted + " completed)");
            Path transferFile = Controller.getMiscFilesLocation().resolve(
                getController().getConfigName() + ".transfers");
            // for testing we should support getConfigName() with subdirs
            if (Files.notExists(transferFile.getParent())) {
                try {
                    Files.createDirectories(transferFile.getParent());
                } catch (IOException ioe) {
                    logSevere("Failed to mkdir misc directory!");
                }
            }
            ObjectOutputStream oOut = new ObjectOutputStream(
                Files.newOutputStream(transferFile));
            oOut.writeObject(storedDownloads);
            oOut.close();
        } catch (IOException e) {
            logSevere("Unable to store pending downloads", e);
        }
    }

    /**
     * Removes completed download for a fileInfo.
     * 
     * @param fileInfo
     */
    public void clearCompletedDownload(FileInfo fileInfo) {
        FileInfoKey key = new FileInfoKey(fileInfo, Type.VERSION_DATE_SIZE);
        DownloadManager dlManager = completedDownloads.remove(key);
        if (dlManager == null) {
            return;
        }
        for (Download download : dlManager.getSources()) {
            fireCompletedDownloadRemoved(new TransferManagerEvent(this,
                download));
        }
    }

    public Set<CoalescedBandwidthStat> getBandwidthStats() {
        return statsRecorder.getBandwidthStats();
    }

    // Worker code ************************************************************

    /**
     * The core maintenance thread of transfermanager.
     */
    private class TransferChecker implements Runnable {
        public void run() {
            long waitTime = Controller.getWaitTime() * 2;
            int count = 0;

            while (!Thread.currentThread().isInterrupted()) {
                // Check uploads/downloads every 10 seconds
                if (isFiner()) {
                    logFiner("Checking uploads/downloads");
                }

                if (getController().isPaused()) {
                    logFine("Paused.");
                } else {

                    // Check queued uploads
                    checkQueuedUploads();

                    // Check pending downloads
                    checkPendingDownloads();

                    // Checking downloads
                    checkDownloads();

                    // log upload / donwloads
                    if (count % 2 == 0) { // @todo huh? why % 2 ?
                        if (isFine()) {
                            logFine("Transfers: "
                                + countActiveDownloads()
                                + " download(s), "
                                + Format.formatDecimal(getDownloadCounter()
                                    .calculateCurrentKBS())
                                + " KByte/s, "
                                + activeUploads.size()
                                + " active upload(s), "
                                + queuedUploads.size()
                                + " in queue, "
                                + Format.formatDecimal(getUploadCounter()
                                    .calculateCurrentKBS()) + " KByte/s");
                        }
                    }

                    count++;

                }

                // wait a bit to next work
                try {
                    // Wait another 10ms to avoid spamming via trigger
                    Thread.sleep(10);

                    synchronized (waitTrigger) {
                        if (!transferCheckTriggered) {
                            waitTrigger.wait(waitTime);
                        }
                        transferCheckTriggered = false;
                    }

                } catch (InterruptedException e) {
                    // Break
                    break;
                }
            }
        }
    }

    public void abortAllDownloads() {
        for (DownloadManager downloadManager : dlManagers.values()) {
            downloadManager.abort();
        }
    }

    public void abortAllUploads() {
        for (Upload upload : activeUploads) {
            uploadBroken(upload, TransferProblem.PAUSED);
        }
        for (Upload upload : queuedUploads) {
            uploadBroken(upload, TransferProblem.PAUSED);
        }
    }

    public void checkActiveTranfersForExcludes() {
        for (DownloadManager dlManager : dlManagers.values()) {
            FileInfo fInfo = dlManager.getFileInfo();
            Folder folder = fInfo.getFolder(getController()
                .getFolderRepository());
            if (folder != null) {
                if (folder.getDiskItemFilter().isExcluded(fInfo)) {
                    logInfo("Aborting download, file is now excluded from sync: "
                        + fInfo);
                    breakTransfers(fInfo);
                }
            }
        }
        for (Upload upload : queuedUploads) {
            FileInfo fInfo = upload.getFile();
            Folder folder = fInfo.getFolder(getController()
                .getFolderRepository());
            if (folder != null) {
                if (folder.getDiskItemFilter().isExcluded(fInfo)) {
                    logInfo("Aborting upload, file is now excluded from sync: "
                        + fInfo);
                    breakTransfers(fInfo);
                }
            }
        }
        for (Upload upload : activeUploads) {
            FileInfo fInfo = upload.getFile();
            Folder folder = fInfo.getFolder(getController()
                .getFolderRepository());
            if (folder != null) {
                if (folder.getDiskItemFilter().isExcluded(fInfo)) {
                    logInfo("Aborting upload, file is now excluded from sync: "
                        + fInfo);
                    breakTransfers(fInfo);
                }
            }
        }
    }

    /**
     * Checks the queued or active downloads and breaks them if nesseary. Also
     * searches for additional sources of download.
     */
    private void checkDownloads() {
        if (isFiner()) {
            logFiner("Checking " + countActiveDownloads() + " download(s)");
        }

        for (DownloadManager man : dlManagers.values()) {
            try {
                downloadNewestVersion(man.getFileInfo(),
                    man.isRequestedAutomatic());
                for (Download download : man.getSources()) {
                    if (!download.isCompleted() && download.isBroken()) {
                        download.setBroken(TransferProblem.BROKEN_DOWNLOAD,
                            "isBroken()");
                    }
                }
            } catch (Exception e) {
                logSevere("Exception while cheking downloads. " + e, e);
            }
        }
    }

    /**
     * Checks the queued uploads and start / breaks them if nessesary.
     */
    private void checkQueuedUploads() {

        if (isFiner()) {
            logFiner("Checking " + queuedUploads.size() + " queued uploads");
        }

        int uploadsBroken = 0;
        int uploadsStarted = 0;
        for (Upload upload : queuedUploads) {
            try {
                if (upload.isBroken()) {
                    // Broken
                    uploadBroken(upload, TransferProblem.BROKEN_UPLOAD);
                    uploadsBroken++;
                } else {
                    boolean alreadyUploadingTo;
                    // The total size planned+current uploading to that node.
                    long totalPlannedSizeUploadingTo = uploadingToSize(upload
                        .getPartner());
                    if (totalPlannedSizeUploadingTo == -1) {
                        alreadyUploadingTo = false;
                        totalPlannedSizeUploadingTo = 0;
                    } else {
                        alreadyUploadingTo = true;
                    }
                    totalPlannedSizeUploadingTo += upload.getFile().getSize();
                    long maxSizeUpload = upload.getPartner().isOnLAN()
                        ? Constants.START_UPLOADS_TILL_PLANNED_SIZE_LAN
                        : Constants.START_UPLOADS_TILL_PLANNED_SIZE_INET;
                    if (!alreadyUploadingTo
                        || totalPlannedSizeUploadingTo <= maxSizeUpload)
                    {
                        // if (!alreadyUploadingTo) {
                        if (alreadyUploadingTo && isFiner()) {
                            logFiner("Starting another upload to "
                                + upload.getPartner().getNick()
                                + ". Total size to upload to: "
                                + Format
                                    .formatBytesShort(totalPlannedSizeUploadingTo));

                        }
                        // start the upload if we have free slots
                        // and not uploading to member currently
                        // or user is on local network

                        // TODO should check if this file is not sended (or is
                        // being send) to other user in the last minute or so to
                        // allow for disitributtion of that file by user that
                        // just received that file from us

                        // Enqueue upload to friends and lan members first

                        if (upload.isAborted()) {
                            logWarning("Not starting aborted: " + upload);
                        } else {
                            if (upload.getPartner().isOnLAN()
                                || !isUploadActive(upload.getFile(), true))
                            {
                                logFiner("Starting upload: " + upload);
                                upload.start();
                                uploadsStarted++;
                            } else {
                                // PFS-843
                                logInfo("Waiting with Internet upload of file "
                                    + upload.getFile()
                                    + ". Already upload to an Internet device");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logSevere("Exception while cheking queued uploads. " + e, e);
            }
        }

        if (isFiner()) {
            logFiner("Started " + uploadsStarted + " upload(s), "
                + uploadsBroken + " broken upload(s)");
        }
    }

    /**
     * Checks the pendings download. Restores them if a source is found
     */
    private void checkPendingDownloads() {
        if (isFiner()) {
            logFiner("Checking " + pendingDownloads.size()
                + " pending downloads");
        }

        // Checking pending downloads
        for (Download dl : pendingDownloads) {
            try {
                FileInfo fInfo = dl.getFile();
                boolean notDownloading = getDownloadManagerFor(fInfo) == null;
                if (notDownloading
                    && getController().getFolderRepository().hasJoinedFolder(
                        fInfo.getFolderInfo()))
                {
                    // MultiSourceDownload source = downloadNewestVersion(fInfo,
                    // download
                    // .isRequestedAutomatic());
                    DownloadManager source = downloadNewestVersion(fInfo,
                        dl.isRequestedAutomatic());
                    if (source != null) {
                        logFine("Pending download restored: " + fInfo
                            + " from " + source);
                        synchronized (pendingDownloads) {
                            pendingDownloads.remove(dl);
                        }
                    }
                } else if (dl.getDownloadManager() != null
                    && !dl.getDownloadManager().isDone())
                {
                    // Not joined folder, break pending dl
                    logWarning("Pending download removed: " + fInfo);
                    synchronized (pendingDownloads) {
                        pendingDownloads.remove(dl);
                    }
                }
            } catch (Exception e) {
                logSevere("Exception while cheking pending downloads. " + e, e);
            }
        }
    }

    /**
     * Recalculate the up/download bandwidth auto limit. Do this by testing
     * upload and download of 100KiB to the server.
     */
    public FutureTask<Object> getRecalculateAutomaticRate() {

        return new FutureTask<Object>(new Callable<Object>() {
            public Object call() throws Exception {
                if (recalculatingAutomaticRates.getAndSet(true)) {
                    // Only one at a time.
                    return null;
                }
                // Pause all transfers
                boolean wasPause = getController().isPaused();
                getController().setPaused(true);
                try {
                    // Get times.
                    Date startDate = new Date();
                    long downloadRate = 0;
                    long downloadSize = 1047552; // @todo, why 1023 * 1024
                                                 // bytes?
                    boolean downloadOk = false;

                    // downloadOk = countActiveDownloads() == 0
                    // && testAvailabilityDownload(downloadSize);

                    Date afterDownload = new Date();
                    // @todo please explain why / 4 ?
                    long uploadSize = 1047552 / 4;
                    boolean uploadOk = countActiveUploads() == 0
                        && testAvailabilityUpload(uploadSize);
                    Date afterUpload = new Date();

                    // Calculate time differences.
                    long downloadTime = afterDownload.getTime()
                        - startDate.getTime();
                    long uploadTime = afterUpload.getTime()
                        - afterDownload.getTime();
                    // logWarning("Test availability download time " +
                    // downloadTime);
                    // logWarning("Test availability upload time " +
                    // uploadTime);
                    // Calculate rates in KiB/s.#
                    if (downloadOk) {
                        downloadRate = downloadTime > 0 ? downloadSize * 1000
                            / downloadTime : 0;
                    }
                    long uploadRate = uploadTime > 0 ? uploadSize * 1000
                        / uploadTime : 0;
                    if (downloadOk) {
                        logFine("Test availability download rate "
                            + Format.formatBytesShort(downloadRate) + "/s");
                    }
                    if (uploadOk) {
                        logFine("Test availability upload rate "
                            + Format.formatBytesShort(uploadRate) + "/s");
                    }
                    // Update bandwidth provider with 90% of new rates.
                    // By experience: Measured rates usually lower than actual
                    // speed.
                    long modifiedDownloadRate = 90 * downloadRate / 100;
                    long modifiedUploadRate = 90 * uploadRate / 100;

                    logInfo("Speed test finished: Download "
                        + Format.formatBytesShort(downloadRate) + "/s, Upload "
                        + Format.formatBytesShort(uploadRate) + "/s");

                    // If the detected rate is too low the connection is
                    // possibly
                    // exhausted. Unlimt transfers? or keep the current rate
                    // unchanged?
                    if (uploadRate < 10240) {
                        uploadRate = 0;
                    }
                    if (downloadRate < 102400 || downloadRate < uploadRate) {
                        downloadRate = 0;
                    }

                    if (downloadOk) {
                        setDownloadCPSForWAN(modifiedDownloadRate);
                    } else {
                        // Set unlimited
                        setDownloadCPSForWAN(0);
                    }
                    if (uploadOk) {
                        setUploadCPSForWAN(modifiedUploadRate);
                    }

                } finally {
                    recalculatingAutomaticRates.set(false);
                    getController().setPaused(wasPause);
                }
                return null;
            }
        });
    }

    /**
     * Test upload rate by uploading 100 KiB to the server.
     * 
     * @return true if it succeeded.
     */
    private boolean testAvailabilityUpload(long size) {
        try {
            String path = getController().getOSClient().getWebURL()
                + "/testavailability?action=upload";
            URL url = new URL(path);

            URLConnection connection = url.openConnection();
            connection.setDoOutput(true); // This sets request method to POST.
            String boundary = "---------------------------313223033317673";
            connection.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8"));

                writer.println("--" + boundary);
                writer
                    .println("Content-Disposition: form-data; name=\"upload\"");
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println();
                String paramToSend = "action";
                writer.println(paramToSend);

                writer.println("--" + boundary);
                writer
                    .println("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"file.txt\"");
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println();

                for (int i = 0; i < size; i++) {
                    writer.write('X');
                }

                writer.println();
                writer.println("--" + boundary + "--");
                writer.println();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }

            // Connection is lazily executed whenever you request any status.
            int responseCode = ((HttpURLConnection) connection)
                .getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            logWarning("Test availability upload failed: " + e);
        }
        return false;
    }

    /**
     * Test download rate by downloading 100 KiB from the server.
     * 
     * @return true if it succeeded.
     */
    private boolean testAvailabilityDownload(long size) {
        BufferedReader reader = null;
        try {
            String path = getController().getOSClient().getWebURL()
                + "/testavailability?action=download&size=" + size;
            URL url = new URL(path);
            URLConnection connection = url.openConnection();
            reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
            while (reader.readLine() != null) {
            }
            return true;
        } catch (Exception e) {
            logWarning("Test availability download failed: " + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Dont care.
                }
            }
        }
        return false;
    }

    // Helper code ************************************************************

    /**
     * logs a up- or download with speed and time
     * 
     * @param download
     * @param took
     * @param fInfo
     * @param member
     */
    public void logTransfer(boolean download, long took, FileInfo fInfo,
        Member member)
    {

        String memberInfo = "";
        if (member != null) {
            memberInfo = (download ? " from " : " to ") + '\''
                + member.getNick() + '\'';
        }

        String cpsStr = "-";

        // printout rate only if dl last longer than 0,5 s
        if (took > 1000) {
            double cps = fInfo.getSize();
            cps /= 1024;
            cps /= took;
            cps *= 1000;
            synchronized (CPS_FORMAT) {
                cpsStr = CPS_FORMAT.format(cps);
            }
        }

        if (isInfo()) {
            String msg = (download ? "Download" : "Upload") + " completed: "
                + Format.formatDecimal(fInfo.getSize()) + " bytes in " + took
                / 1000 + "s (" + cpsStr + " KByte/s): " + fInfo + memberInfo;
            if (fInfo.getFolderInfo().isMetaFolder()) {
                logFine(msg);
            } else {
                logInfo(msg);
            }
        }
    }

    // Event/Listening code ***************************************************

    public void addListener(TransferManagerListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeListener(TransferManagerListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    private void fireUploadStarted(TransferManagerEvent event) {
        listenerSupport.uploadStarted(event);
    }

    private void fireUploadAborted(TransferManagerEvent event) {
        listenerSupport.uploadAborted(event);
    }

    private void fireUploadBroken(TransferManagerEvent event) {
        listenerSupport.uploadBroken(event);
    }

    private void fireUploadCompleted(TransferManagerEvent event) {
        listenerSupport.uploadCompleted(event);
    }

    private void fireUploadRequested(TransferManagerEvent event) {
        listenerSupport.uploadRequested(event);
    }

    private void fireDownloadAborted(TransferManagerEvent event) {
        listenerSupport.downloadAborted(event);
    }

    private void fireDownloadBroken(TransferManagerEvent event) {
        listenerSupport.downloadBroken(event);
    }

    private void fireDownloadCompleted(TransferManagerEvent event) {
        listenerSupport.downloadCompleted(event);
    }

    private void fireDownloadQueued(TransferManagerEvent event) {
        listenerSupport.downloadQueued(event);
    }

    private void fireDownloadRequested(TransferManagerEvent event) {
        listenerSupport.downloadRequested(event);
    }

    private void fireDownloadStarted(TransferManagerEvent event) {
        listenerSupport.downloadStarted(event);
    }

    private void fireCompletedDownloadRemoved(TransferManagerEvent event) {
        listenerSupport.completedDownloadRemoved(event);
    }

    private void fireCompletedUploadRemoved(TransferManagerEvent event) {
        listenerSupport.completedUploadRemoved(event);
    }

    private void firePendingDownloadEnqueud(TransferManagerEvent event) {
        listenerSupport.pendingDownloadEnqueued(event);
    }

    /**
     * This class regularly checks to see if any downloads or uploads are
     * active, and updates folder statistics with partial down/upload byte
     * count.
     */
    private class PartialTransferStatsUpdater extends TimerTask {
        @Override
        public void run() {
            FolderRepository folderRepository = getController()
                .getFolderRepository();
            for (FileInfo fileInfo : dlManagers.keySet()) {
                DownloadManager downloadManager = dlManagers.get(fileInfo);
                if (downloadManager != null) {
                    Folder folder = folderRepository.getFolder(fileInfo
                        .getFolderInfo());
                    if (folder == null) {
                        continue;
                    }
                    folder.getStatistic().putPartialSyncStat(fileInfo,
                        getController().getMySelf(),
                        downloadManager.getCounter().getBytesTransferred());
                }
            }
            for (Upload upload : activeUploads) {
                Folder folder = upload.getFile().getFolder(folderRepository);
                if (folder == null) {
                    continue;
                }
                folder.getStatistic().putPartialSyncStat(upload.getFile(),
                    upload.getPartner(),
                    upload.getCounter().getBytesTransferred());
            }
        }
    }

    private class TransferCleaner extends TimerTask {
        @Override
        public void run() {
            cleanupOldTransfers();
        }
    }
}
