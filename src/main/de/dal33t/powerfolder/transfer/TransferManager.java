/* $Id: TransferManager.java,v 1.92 2006/04/30 14:24:17 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.util.*;

/**
 * Transfer manager for downloading/uploading files
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.92 $
 */
public class TransferManager extends PFComponent implements Runnable {
    // The maximum size of a chunk transferred at once
    public static int MAX_CHUNK_SIZE = 32 * 1024;
    // Timeout of donwload in 2 min
    public static long DOWNLOAD_REQUEST_TIMEOUT_MS = 60 * 1000;

    private static DecimalFormat CPS_FORMAT = new DecimalFormat(
        "#,###,###,###.##");

    private boolean started;

    private Thread myThread;
    private List<Upload> queuedUploads;
    private List<Upload> activeUploads;
    private Map<FileInfo, Download> downloads;

    // A set of pending files, which should be downloaded
    private List<Download> pendingDownloads;
    // The list of completed download
    private List<Download> completedDownloads;

    // The trigger, where transfermanager waits on
    private Object waitTrigger = new Object();

    // The currently calculated transferstatus
    private TransferStatus transferStatus;

    // initialize with defaults
    private int allowedUploads;

    // the counter for uploads
    private TransferCounter uploadCounter;
    private TransferCounter downloadCounter;

    // Provides bandwidth for the transfers
    private BandwidthProvider bandwidthProvider;
    private BandwidthLimiter sharedWANOutputHandler;

    // Input limiter, currently shared between all WAN/LAN connections
    private BandwidthLimiter sharedInputHandler;

    private BandwidthLimiter sharedLANOutputHandler;

    private TransferManagerListener listenerSupport;

    public TransferManager(Controller controller) {
        super(controller);
        this.started = false;
        this.queuedUploads = Collections
            .synchronizedList(new LinkedList<Upload>());
        this.activeUploads = Collections
            .synchronizedList(new LinkedList<Upload>());
        this.downloads = new ConcurrentHashMap<FileInfo, Download>();
        this.pendingDownloads = Collections
            .synchronizedList(new LinkedList<Download>());
        this.completedDownloads = Collections
            .synchronizedList(new LinkedList<Download>());
        this.uploadCounter = new TransferCounter();
        this.downloadCounter = new TransferCounter();
        // Create listener support
        this.listenerSupport = (TransferManagerListener) ListenerSupportFactory
            .createListenerSupport(TransferManagerListener.class);

        // maximum concurrent uploads
        allowedUploads = ConfigurationEntry.UPLOADS_MAX_CONCURRENT.getValueInt(
            getController()).intValue();
        if (allowedUploads <= 0) {
            throw new NumberFormatException("Illegal value for max uploads: "
                + allowedUploads);
        }

        // parse max upload cps
        String cps = ConfigurationEntry.UPLOADLIMIT_WAN
            .getValue(getController());
        long maxCps = 0;
        if (cps != null) {
            try {
                maxCps = (long) Double.parseDouble(cps) * 1024;
                if (maxCps < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                log().warn(
                    "Illegal value for KByte upload limit. '" + cps + "'");
            }
        }

        bandwidthProvider = new BandwidthProvider();

        sharedWANOutputHandler = new BandwidthLimiter();
        bandwidthProvider.setLimitBPS(sharedWANOutputHandler, maxCps);
        sharedInputHandler = new BandwidthLimiter();

        // set ul limit
        setAllowedUploadCPSForWAN(maxCps);

        // parse max upload cps
        cps = ConfigurationEntry.UPLOADLIMIT_LAN.getValue(getController());
        maxCps = 0;
        if (cps != null) {
            try {
                maxCps = (long) Double.parseDouble(cps) * 1024;
                if (maxCps < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                log().warn(
                    "Illegal value for KByte upload limit. '" + cps + "'");
            }
        }

        sharedLANOutputHandler = new BandwidthLimiter();
        bandwidthProvider.setLimitBPS(sharedLANOutputHandler, maxCps);

        // set ul limit
        setAllowedUploadCPSForLAN(maxCps);
    }

    // General ****************************************************************

    /**
     * Starts the transfermanager thread
     */
    public void start() {
        myThread = new Thread(this, "Transfer manager");
        myThread.start();

        // Load all pending downloads
        loadDownloads();

        started = true;
        log().debug("Started");
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

        // shutdown active downloads
        Upload[] uploads = getActiveUploads();
        for (int i = 0; i < uploads.length; i++) {
            // abort && shutdown uploads
            uploads[i].abort();
            uploads[i].shutdown();
        }

        if (started) {
            storeDownloads();
        }

        started = false;
        log().debug("Stopped");
    }

    /** for debug */
    public void setSuspendFireEvents(boolean suspended) {
        ListenerSupportFactory.setSuspended(listenerSupport, suspended);
        log().debug("setSuspendFireEvents: " + suspended);
    }

    /**
     * Triggers the working thread
     */
    private void triggerTransfersCheck() {
        // log().verbose("Triggering transfers check");
        synchronized (waitTrigger) {
            waitTrigger.notifyAll();
        }
    }

    public BandwidthProvider getBandwidthProvider() {
        return bandwidthProvider;
    }

    public BandwidthLimiter getOutputLimiter(ConnectionHandler handler) {
        if (handler.isOnLAN())
            return sharedLANOutputHandler;
        return sharedWANOutputHandler;
    }

    public BandwidthLimiter getInputLimiter(ConnectionHandler handler) {
        if (handler.isOnLAN())
            return new BandwidthLimiter();
        return sharedInputHandler;
    }

    /**
     * Returns the transferstatus by calculating it new
     * 
     * @return
     */
    public TransferStatus getStatus() {
        transferStatus = new TransferStatus();

        // Upload status
        transferStatus.activeUploads = activeUploads.size();
        transferStatus.queuedUploads = queuedUploads.size();

        transferStatus.maxUploads = getAllowedUploads();
        transferStatus.maxUploadCPS = getAllowedUploadCPSForWAN();
        transferStatus.currentUploadCPS = (long) uploadCounter
            .calculateCurrentCPS();
        transferStatus.uploadedBytesTotal = uploadCounter.getBytesTransferred();

        // Download status
        synchronized (downloads) {
            for (Iterator it = downloads.values().iterator(); it.hasNext();) {
                Download download = (Download) it.next();
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
     * Sets an transfer as started
     * 
     * @param transfer
     *            the transfer
     */
    void setStarted(Transfer transfer) {
        if (transfer instanceof Upload) {
            synchronized (queuedUploads) {
                synchronized (activeUploads) {
                    queuedUploads.remove(transfer);
                    activeUploads.add((Upload) transfer);
                }
            }

            // Fire event
            fireUploadStarted(new TransferManagerEvent(this, (Upload) transfer));
        } else if (transfer instanceof Download) {
            // Fire event
            fireDownloadStarted(new TransferManagerEvent(this,
                (Download) transfer));
        }

        log().debug("Transfer started: " + transfer);
    }

    /**
     * Callback to inform, that a download has been enqued at the remote ide
     * 
     * @param download
     *            the download
     */
    public void setQueued(DownloadQueued dlQueuedRequest) {
        Download dl = downloads.get(dlQueuedRequest.file);
        if (dl != null) {
            // set this dl as queued
            dl.setQueued();
            // Fire
            fireDownloadQueued(new TransferManagerEvent(this, dl));
        }
    }

    /**
     * Sets a transfer as broken, removes from queues
     * 
     * @param tranfer
     *            the transfer
     */
    void setBroken(Transfer transfer) {
        boolean transferFound = false;
        if (transfer instanceof Download) {
            log().warn("Download broken: " + transfer);
            transferFound = downloads.remove(transfer.getFile()) != null;
            // Add to pending downloads
            Download dl = (Download) transfer;

            if (!dl.isRequestedAutomatic()) {
                enquePendingDownload(dl);
            }

            // Fire event
            if (transferFound) {
                fireDownloadBroken(new TransferManagerEvent(this, dl));
            }
        } else if (transfer instanceof Upload) {
            log().warn("Upload broken: " + transfer);
            transferFound = queuedUploads.remove(transfer);
            transferFound = activeUploads.remove(transfer) || transferFound;

            // Fire event
            if (transferFound) {
                fireUploadBroken(new TransferManagerEvent(this,
                    (Upload) transfer));
            }
        }
    }

    /**
     * Breaks all transfers from and to that node. Usually done on disconnect
     * 
     * @param node
     */
    public void breakTransfers(Member node) {
        List transfersToBreak = new LinkedList();

        if (!downloads.isEmpty()) {
            // Search for dls to break
            synchronized (downloads) {
                for (Iterator it = downloads.values().iterator(); it.hasNext();)
                {
                    Download download = (Download) it.next();
                    if (download.getPartner().equals(node)) {
                        transfersToBreak.add(download);
                    }
                }
            }
        }

        // Search for uls to break
        if (!queuedUploads.isEmpty()) {
            synchronized (queuedUploads) {
                for (Iterator it = queuedUploads.iterator(); it.hasNext();) {
                    Upload upload = (Upload) it.next();
                    if (upload.getPartner().equals(node)) {
                        transfersToBreak.add(upload);
                    }
                }
            }
        }
        if (!activeUploads.isEmpty()) {
            synchronized (activeUploads) {
                for (Iterator it = activeUploads.iterator(); it.hasNext();) {
                    Upload upload = (Upload) it.next();
                    if (upload.getPartner().equals(node)) {
                        transfersToBreak.add(upload);
                    }
                }
            }
        }

        if (!transfersToBreak.isEmpty()) {
            // Now break these Transfers
            for (Iterator it = transfersToBreak.iterator(); it.hasNext();) {
                Transfer transfer = (Transfer) it.next();
                setBroken(transfer);
            }
        }
    }

    /**
     * Callback method to indicate that a transfer is completed
     * 
     * @param transfer
     */
    void setCompleted(Transfer transfer) {
        boolean transferFound = false;

        transfer.setCompleted();

        if (transfer instanceof Download) {
            Download download = (Download) transfer;
            transferFound = downloads.containsKey(transfer.getFile());

            if (!transferFound) {
                return;
            }

            FileInfo fInfo = transfer.getFile();
            // Inform other folder member of added file
            Folder folder = fInfo.getFolder(getController()
                .getFolderRepository());
            if (folder != null) {
                // scan in new downloaded file
                folder.scanDownloadFile(fInfo, download.getTempFile());
            }

            // Actually remove from active downloads list
            downloads.remove(transfer.getFile());
            // Add to list of completed downloads
            completedDownloads.add((Download) transfer);

            // Fire event
            fireDownloadCompleted(new TransferManagerEvent(this,
                (Download) transfer));

            // Trigger filerequestor
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting();

            // Autostart torrents
            File diskFile = fInfo.getDiskFile(getController()
                .getFolderRepository());

            boolean isLeechFile = diskFile != null
                && fInfo.getFilenameOnly().endsWith(".torrent");
            // Autostart bittorento!
            if (folder.getSyncProfile().isAutostartLeechPrograms()
                && isLeechFile)
            {
                log().warn("Auto starting: " + diskFile.getAbsolutePath());
                try {
                    FileUtils.executeFile(diskFile);
                } catch (IOException e) {
                    log().error(e);
                    // unableToStart(fInfo, ex);
                }
            }
        } else if (transfer instanceof Upload) {
            transferFound = queuedUploads.remove(transfer);
            transferFound = activeUploads.remove(transfer) || transferFound;

            if (transferFound) {
                // Fire event
                fireUploadCompleted(new TransferManagerEvent(this,
                    (Upload) transfer));
            }
        }

        log().debug("Transfer completed: " + transfer);

        if (transfer instanceof Upload) {
            // Now trigger, to start next upload
            triggerTransfersCheck();
        }
    }

    // Upload management ******************************************************

    /**
     * This method is called after any change associated with bandwidth. I.e.:
     * upload limits, silent mode
     */
    public void updateSpeedLimits() {
        int throttle = 100;

        if (getController().isSilentMode()) {
            try {
                throttle = Integer
                    .parseInt(ConfigurationEntry.UPLOADLIMIT_SILENTMODE_THROTTLE
                        .getValue(getController()));
                if (throttle < 0) {
                    throttle = 0;
                } else if (throttle > 100) {
                    throttle = 100;
                }
            } catch (NumberFormatException nfe) {
                log().debug(nfe);
            }
        }

        // Any setting that is "unlimited" will stay unlimited!
        bandwidthProvider.setLimitBPS(sharedLANOutputHandler,
            getAllowedUploadCPSForLAN() * throttle / 100);
        bandwidthProvider.setLimitBPS(sharedWANOutputHandler,
            getAllowedUploadCPSForWAN() * throttle / 100);
    }

    /**
     * Sets the maximum upload bandwidth usage in CPS
     * 
     * @param maxAllowedUploadCPS
     */
    public void setAllowedUploadCPSForWAN(long allowedCPS) {
        if (allowedCPS != 0 && allowedCPS < 3 * 1024
            && !getController().isVerbose())
        {
            log().warn("Setting upload limit to a minimum of 3 KB/s");
            allowedCPS = 3 * 1024;
        }

        // Store in config
        ConfigurationEntry.UPLOADLIMIT_WAN.setValue(getController(), ""
            + (allowedCPS / 1024));

        updateSpeedLimits();

        log().info(
            "Upload limit: " + allowedUploads + " allowed, at maximum rate of "
                + (getAllowedUploadCPSForWAN() / 1024) + " KByte/s");
    }

    /**
     * Answers the allowed upload rate
     * 
     * @return
     */
    public long getAllowedUploadCPSForWAN() {
        try {
            return Integer.parseInt(ConfigurationEntry.UPLOADLIMIT_WAN
                .getValue(getController())) * 1024;
        } catch (NumberFormatException e) {
            log().error("No valid uploadlimit:", e);
        }
        return -1;
    }

    /**
     * Sets the maximum upload bandwidth usage in CPS for LAN
     * 
     * @param maxAllowedUploadCPS
     */
    public void setAllowedUploadCPSForLAN(long allowedCPS) {
        if (allowedCPS != 0 && allowedCPS < 3 * 1024
            && !getController().isVerbose())
        {
            log().warn("Setting upload limit to a minimum of 3 KB/s");
            allowedCPS = 3 * 1024;
        }
        // Store in config
        ConfigurationEntry.UPLOADLIMIT_LAN.setValue(getController(), ""
            + (allowedCPS / 1024));

        updateSpeedLimits();

        log().info(
            "LAN Upload limit: " + allowedUploads
                + " allowed, at maximum rate of "
                + (getAllowedUploadCPSForLAN() / 1024) + " KByte/s");
    }

    /**
     * Answers the allowed upload rate for LAN
     * 
     * @return
     */
    public long getAllowedUploadCPSForLAN() {
        try {
            return Integer.parseInt(ConfigurationEntry.UPLOADLIMIT_LAN
                .getValue(getController())) * 1024;
        } catch (NumberFormatException e) {
            log().error("No valid lan uploadlimit:", e);
        }
        return -1;
    }

    /**
     * Checks if the manager has free upload slots
     * 
     * @return
     */
    private boolean hasFreeUploadSlots() {
        return activeUploads.size() < allowedUploads;
    }

    /**
     * Answer the maximum number of allowed uploads
     * 
     * @return
     */
    public int getAllowedUploads() {
        return allowedUploads;
    }

    /**
     * Returns the counter for upload speed
     * 
     * @return
     */
    public TransferCounter getUploadCounter() {
        return uploadCounter;
    }

    /**
     * Queues a upload into the upload queue. Breaks all former upload requests
     * for the file from that member.
     * 
     * @param file
     * @param member
     * @return the enqued upload
     */
    public Upload queueUpload(Member from, RequestDownload dl) {
        if (dl == null || dl.file == null) {
            throw new NullPointerException("Downloadrequest/File is null");
        }
        // Never upload db files !!
        if (Folder.DB_FILENAME.equalsIgnoreCase(dl.file.getName())
            || Folder.DB_BACKUP_FILENAME.equalsIgnoreCase(dl.file.getName()))
        {
            log()
                .error(
                    from.getNick()
                        + " has illegally requested to download a folder database file");
            return null;
        }

        Upload oldUpload = null;
        Upload upload = new Upload(this, from, dl);
        if (upload.isBroken()) {
            // Check if this download is broken
            return null;
        }
        
        // Check if we have a old upload to break
        synchronized (activeUploads) {
            synchronized (queuedUploads) {
                int oldUploadIndex = activeUploads.indexOf(upload);
                if (oldUploadIndex >= 0) {
                    oldUpload = activeUploads.get(oldUploadIndex);
                    activeUploads.remove(oldUploadIndex);
                }
                
                oldUploadIndex = queuedUploads.indexOf(upload);
                if (oldUploadIndex >= 0) {
                    if (oldUpload != null) {
                        // Should never happen
                        throw new IllegalStateException("Found illegal upload. is in list of queued AND active uploads: "
                            + oldUpload);
                    }
                    oldUpload = queuedUploads.get(oldUploadIndex);
                    queuedUploads.remove(oldUploadIndex);
                }
            }
        }

        if (oldUpload != null) {
            log().warn(
                "Received already known download request for " + dl.file
                    + " from " + from.getNick() + ", overwriting old request");
            // Stop former upload request
            oldUpload.abort();
            oldUpload.shutdown();
            setBroken(oldUpload);
        }
        
        synchronized (queuedUploads) {
            log().debug(
                "Upload enqueud: " + dl.file + ", startOffset: "
                    + dl.startOffset + ", to: " + from);
            queuedUploads.add(upload);
        }

        // If upload is not started, tell peer
        if (!upload.isStarted()) {
            from.sendMessageAsynchron(new DownloadQueued(upload.getFile()),
                null);
        }

        if (!upload.isBroken()) {
            fireUploadRequested(new TransferManagerEvent(this, upload));
        }

        // Trigger working thread
        triggerTransfersCheck();

        return upload;
    }

    /**
     * Aborts a upload
     * 
     * @param fInfo
     *            the file to upload
     * @param to
     *            the member where is file is going to
     */
    public void abortUpload(FileInfo fInfo, Member to) {
        if (fInfo == null) {
            throw new NullPointerException(
                "Unable to abort upload, file is null");
        }
        if (to == null) {
            throw new NullPointerException(
                "Unable to abort upload, to-member is null");
        }

        Upload abortedUpload = null;
        synchronized (queuedUploads) {
            for (Iterator it = queuedUploads.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                if (upload.getFile().equals(fInfo)
                    && to.equals(upload.getPartner()))
                {
                    // Remove upload from queue
                    it.remove();
                    upload.abort();
                    abortedUpload = upload;
                }
            }
        }

        synchronized (activeUploads) {
            for (Iterator it = activeUploads.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                if (upload.getFile().equals(fInfo)
                    && to.equals(upload.getPartner()))
                {
                    // Remove upload from queue
                    it.remove();
                    upload.abort();
                    abortedUpload = upload;
                }
            }
        }

        if (abortedUpload != null) {
            fireUploadAborted(new TransferManagerEvent(this, abortedUpload));

            // Trigger check
            triggerTransfersCheck();
        }
    }

    /**
     * Returns the currently active uploads
     * 
     * @return
     */
    public Upload[] getActiveUploads() {
        synchronized (activeUploads) {
            Upload[] ulArray = new Upload[activeUploads.size()];
            activeUploads.toArray(ulArray);
            return ulArray;
        }
    }

    /**
     * Answers the total number of uploads
     * 
     * @return
     */
    public int countUploads() {
        return activeUploads.size() + queuedUploads.size();
    }

    /**
     * Answers all queued uploads
     * 
     * @return Array of all queued upload
     */
    public Upload[] getQueuedUploads() {
        synchronized (queuedUploads) {
            Upload[] ulArray = new Upload[queuedUploads.size()];
            queuedUploads.toArray(ulArray);
            return ulArray;
        }
    }

    /**
     * Answers, if we are uploading to this member
     * 
     * @param to
     * @return
     */
    public boolean uploadingTo(Member member) {
        synchronized (activeUploads) {
            for (Iterator it = activeUploads.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                if (member.equals(upload.getPartner())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Download management ***************************************************

    /**
     * Returns the download counter
     * 
     * @return
     */
    public TransferCounter getDownloadCounter() {
        return downloadCounter;
    }

    /**
     * Addds a file for download if source is not known. Download will be
     * started when a source is found.
     * 
     * @param fInfo
     * @return true if succeeded
     */
    public boolean enquePendingDownload(Download download) {
        // Check if still on folder
        FileInfo fInfo = download.getFile();

        if (download.isRequestedAutomatic()) {
            log().warn("Not adding pending download, is a auto-dl: " + fInfo);
            return false;
        }

        if (!getController().getFolderRepository().hasJoinedFolder(
            fInfo.getFolderInfo()))
        {
            log().warn("Not adding pending download, not on folder: " + fInfo);
            return false;
        }

        Folder folder = fInfo.getFolder(getController().getFolderRepository());
        FileInfo localFile = folder != null ? folder.getFile(fInfo) : null;
        if (fInfo != null && localFile != null
            && fInfo.completelyIdentical(localFile))
        {
            log().warn("Not adding pending download, already have: " + fInfo);
            return false;
        }

        boolean contained;
        // Remove partner
        download.setPartner(null);

        // Add to pending list
        synchronized (pendingDownloads) {
            contained = pendingDownloads.remove(download);
            pendingDownloads.add(0, download);
        }
        if (!contained) {
            log().warn("Pending download added: " + download);
            firePendingDownloadEnqueud(new TransferManagerEvent(this, download));
        }
        return true;
    }

    /**
     * Requests to download the newest version of the file. Returns null if
     * download wasn't enqued at a node. Download is then pending
     * 
     * @param fInfo
     * @return the member where the dl was requested at, or null if no source
     *         available (DL was NOT started)
     */
    public Member downloadNewestVersion(FileInfo fInfo) {
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
    public Member downloadNewestVersion(FileInfo fInfo, boolean automatic) {
        Folder folder = fInfo.getFolder(getController().getFolderRepository());
        if (folder == null) {
            // on shutdown folder maybe null here
            return null;
        }

        // return null if in blacklist on automatic download
        if (automatic && folder.isInBlacklist(fInfo)) {
            return null;
        }

        if (isDownloadingActive(fInfo)) {
            // if already downloading, return member
            Download download = getActiveDownload(fInfo);
            return download.getPartner();
        }

        if (!getController().getFolderRepository().hasJoinedFolder(
            fInfo.getFolderInfo()))
        {
            return null;
        }

        // only if we have joined the folder
        Member[] sources = getSourcesFor(fInfo);
        // log().verbose("Got " + sources.length + " sources for " + fInfo);

        // Now walk through all sources and get the best one
        Member bestSource = null;
        FileInfo newestVersionFile = null;
        // ap<>
        Map<Member, Integer> downloadCountList = countNodesDownloads();

        // Get best source (=newest version & best connection)
        // FIXME: Causes trouble when we have one node with the highest file
        // version
        // but no more allowed DLS. This algo may download a lower fileversion
        // from diffrent node
        for (int i = 0; i < sources.length; i++) {
            Member source = sources[i];
            FileInfo remoteFile = source.getFile(fInfo);

            if (remoteFile == null) {
                continue;
            }

            int nDownloadFrom = 0;
            if (downloadCountList.containsKey(source)) {
                nDownloadFrom = downloadCountList.get(source).intValue();
            }
            int maxAllowedDls = source.isOnLAN()
                ? Constants.MAX_DLS_FROM_LAN_MEMBER
                : Constants.MAX_DLS_FROM_INET_MEMBER;
            if (nDownloadFrom >= maxAllowedDls) {
                // No more dl from this node allowed, skip
                // log().warn("No more download allowed from " + source);
                continue;
            }

            if (newestVersionFile == null || bestSource == null) {
                // Initalize
                newestVersionFile = sources[i].getFile(fInfo);
                bestSource = source;
            }

            // Found a newer version
            if (remoteFile.isNewerThan(newestVersionFile)) {
                newestVersionFile = remoteFile;
                bestSource = source;
            }
        }

        Download download = new Download(this, fInfo, automatic);

        if (bestSource != null) {
            requestDownload(download, bestSource);
            return bestSource;
        }

        if (!automatic) {
            // Okay enque as pending download if was manually requested
            enquePendingDownload(download);
        }
        return null;
    }

    /**
     * Requests the download from that member
     * 
     * @param download
     * @param from
     */
    private void requestDownload(Download download, Member from) {
        FileInfo fInfo = download.getFile();

        synchronized (downloads) {
            Download oldDownload = downloads.get(fInfo);
            if (oldDownload == null) {
                downloads.put(fInfo, download);
            } else {
                log().warn(
                    "Not adding download. Already havin one: " + oldDownload);
                return;
            }
        }

        // Remove from pending downloads
        pendingDownloads.remove(download);

        if (logEnabled) {
            log().debug(
                "Requesting " + fInfo.toDetailString() + " from " + from);
        }
        download.request(from);

        if (!download.isBroken()) {
            // Fire event
            fireDownloadRequested(new TransferManagerEvent(this, download));
        }
    }

    /**
     * Finds the sources for the file. Returns only sources which are connected
     * The members are sorted in order of best source.
     * <p>
     * WARNING: Versions of files are ingnored
     * 
     * @param fInfo
     * @return the list of members, where the file is available
     */
    public Member[] getSourcesFor(FileInfo fInfo) {
        if (fInfo == null) {
            throw new NullPointerException("File is null");
        }

        Member[] nodes = getController().getNodeManager().getNodes();
        List sources = new ArrayList(nodes.length);
        for (int i = 0; i < nodes.length; i++) {
            Member node = nodes[i];
            if (node.isCompleteyConnected() && node.hasFile(fInfo)) {
                // node is connected and has file
                sources.add(node);
            }
        }
        /*
         * log().verbose( "Got these sources for dl " + fInfo + " (" +
         * sources.size() + "): " + sources);
         */

        Collections.shuffle(sources);
        Member[] srces = new Member[sources.size()];
        sources.toArray(srces);

        // Sort by the best upload availibility
        Arrays.sort(srces, new ReverseComparator(
            MemberComparator.BY_UPLOAD_AVAILIBILITY));

        return srces;
    }

    /**
     * Aborts all automatically enqueued download of a folder. FIXME: Currently
     * aborts all downloads on folder
     * 
     * @param from
     *            the remote node
     */
    public void abortAllAutodownloads(Folder folder) {
        Download[] downloadsArr = getActiveDownloads();
        int aborted = 0;
        for (int i = 0; i < downloadsArr.length; i++) {
            Download dl = downloadsArr[i];
            boolean fromFolder = folder.getInfo().equals(
                dl.getFile().getFolderInfo());
            if (fromFolder && dl.isRequestedAutomatic()) {
                // Abort
                dl.abort();
                aborted++;
            }
        }

        log().warn("Aborted " + aborted + " downloads on " + folder);
    }

    /**
     * Aborts an download. Gets removed compeletly
     * 
     * @return if abort was sucessfully deliverd
     */
    void abortDownload(Download download) {
        FileInfo fInfo = download.getFile();
        Member from = download.getPartner();
        if (from != null && from.isCompleteyConnected()) {
            from.sendMessageAsynchron(new AbortDownload(fInfo), null);
        }
        // Send abort command
        log().debug("Aborting download: " + download);

        downloads.remove(fInfo);
        pendingDownloads.remove(download);

        // Fire event
        fireDownloadAborted(new TransferManagerEvent(this, download));
    }

    /**
     * Clears the list of completed downloads
     */
    public void clearCompletedDownloads() {
        Download[] completedDls = getCompletedDownloads();
        completedDownloads.clear();
        for (int i = 0; i < completedDls.length; i++) {
            fireCompletedDownloadRemoved(new TransferManagerEvent(this,
                completedDls[i]));
        }
    }

    /**
     * Called by member, always a new filechunk is received
     * 
     * @param chunk
     */
    public void chunkReceived(FileChunk chunk, Member from) {
        if (chunk == null) {
            throw new NullPointerException("Chunk is null");
        }
        if (from == null) {
            throw new NullPointerException("Member is null");
        }
        FileInfo file = chunk.file;
        if (file == null) {
            throw new NullPointerException(
                "Fileinfo is null from received chunk");
        }

        Download download = downloads.get(file);
        if (download == null) {
            log().warn(
                "Received download, which has not been requested, ignoring: "
                    + file + " Chunk: Offset:" + chunk.offset + " Length: "
                    + chunk.data.length);

            // Abort dl
            // abortDownload(file, from);
            return;
        }

        // add chunk to DL
        download.addChunk(chunk);
        // Add to calculator
        downloadCounter.chunkTransferred(chunk);
    }

    /**
     * Checks, if that file is downloading
     * 
     * @param fInfo
     * @return
     */
    public boolean isDownloadingActive(FileInfo fInfo) {
        return downloads.containsKey(fInfo);
    }

    /**
     * Answers if the file is enqued as pending download
     * 
     * @param fInfo
     * @return
     */
    public boolean isDownloadingPending(FileInfo fInfo) {
        return pendingDownloads.contains(fInfo);
    }

    /**
     * Checks, if that file is uploading
     * 
     * @param fInfo
     * @return
     */
    public boolean isUploading(FileInfo fInfo) {
        synchronized (activeUploads) {
            for (Iterator it = activeUploads.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                if (upload.getFile() == fInfo) {
                    return true;
                }
            }
        }
        synchronized (queuedUploads) {
            for (Iterator it = queuedUploads.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                if (upload.getFile() == fInfo) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the active download for a file
     * 
     * @param fInfo
     * @return
     */
    public Download getActiveDownload(FileInfo fInfo) {
        return downloads.get(fInfo);
    }

    /**
     * Returns the pending download for the file
     * 
     * @param fileInfo
     * @return
     */
    public Download getPendingDownload(FileInfo fileInfo) {
        synchronized (pendingDownloads) {
            for (Download pendingDl : pendingDownloads) {
                if (fileInfo.equals(pendingDl.getFile())) {
                    return pendingDl;
                }
            }
        }
        return null;
    }

    /**
     * Answers the number of downloads (active & queued) from on a folder.
     * <p>
     * TODO: Count also number of uploads?
     * 
     * @param folder
     *            the folder
     * @return
     */
    public int countNumberOfDownloads(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        int n = 0;
        synchronized (downloads) {
            for (Iterator it = downloads.values().iterator(); it.hasNext();) {
                Download download = (Download) it.next();
                if (download.getFile().getFolderInfo().equals(folder.getInfo()))
                {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Answers the number of downloads (active & queued) from a member TODO:
     * Make this more efficent, runs O(n)
     * 
     * @param from
     * @return
     */
    public int getNumberOfDownloadsFrom(Member from) {
        if (from == null) {
            throw new NullPointerException("From is null");
        }
        int n = 0;
        synchronized (downloads) {
            for (Iterator it = downloads.values().iterator(); it.hasNext();) {
                Download download = (Download) it.next();
                if (from.equals(download.getPartner())) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the currently pending downloads
     * 
     * @return
     */
    public Download[] getPendingDownloads() {
        synchronized (pendingDownloads) {
            Download[] dlArray = new Download[pendingDownloads.size()];
            pendingDownloads.toArray(dlArray);
            return dlArray;
        }
    }

    /**
     * Returns all downloads
     * 
     * @return
     */
    public Download[] getActiveDownloads() {
        synchronized (downloads) {
            Download[] dlArray = new Download[downloads.size()];
            downloads.values().toArray(dlArray);
            return dlArray;
        }
    }

    /**
     * Returns the number of completed downloads
     * 
     * @return
     */
    public int countCompletedDownloads() {
        return completedDownloads.size();
    }

    /**
     * Returns the list of completed downloads
     * 
     * @return
     */
    public Download[] getCompletedDownloads() {
        synchronized (completedDownloads) {
            Download[] dlArray = new Download[completedDownloads.size()];
            completedDownloads.toArray(dlArray);
            return dlArray;
        }
    }

    /**
     * Answers the number of all downloads
     * 
     * @return
     */
    public int getActiveDownloadCount() {
        return downloads.size();
    }

    /**
     * Returns the number of total downloads (queued, active and pending)
     * 
     * @return
     */
    public int getTotalDownloadCount() {
        return getActiveDownloadCount() + pendingDownloads.size()
            + completedDownloads.size();
    }

    /**
     * Counts the number of downloads grouped by Node.
     * <p>
     * contains:
     * <p>
     * Member -> Number of active or enqued downloads to that node
     * 
     * @return Member -> Number of active or enqued downloads to that node
     */
    private Map<Member, Integer> countNodesDownloads() {
        Map<Member, Integer> countList = new HashMap<Member, Integer>();
        synchronized (downloads) {
            for (Download download : downloads.values()) {
                int nDownloadsFrom = 0;
                if (countList.containsKey(download.getPartner())) {
                    nDownloadsFrom = countList.get(download.getPartner())
                        .intValue();
                }

                nDownloadsFrom++;
                countList.put(download.getPartner(), Integer
                    .valueOf(nDownloadsFrom));
            }
        }
        return countList;
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
     * (download.isStarted() && download.getFrom().equals(from)) { return true; } } }
     * return false; }
     */

    /**
     * Loads all pending downloads and enqueus them for re-download
     */
    private void loadDownloads() {
        File transferFile = new File(Controller.getMiscFilesLocation(),
            getController().getConfigName() + ".transfers");
        if (!transferFile.exists()) {
            log().warn(
                "No downloads to restore, " + transferFile.getAbsolutePath()
                    + " does not exists");
            return;
        }
        try {
            FileInputStream fIn = new FileInputStream(transferFile);
            ObjectInputStream oIn = new ObjectInputStream(fIn);
            List storedDownloads = (List) oIn.readObject();
            oIn.close();
            // Reverse to restore in right order
            Collections.reverse(storedDownloads);

            for (Iterator it = storedDownloads.iterator(); it.hasNext();) {
                Download download = (Download) it.next();
                // Initalize after dezerialisation
                download.init(this);
                if (download.isCompleted()) {
                    completedDownloads.add(download);
                } else if (download.isPending()) {
                    enquePendingDownload(download);
                }
            }

            log().warn("Loaded " + storedDownloads.size() + " downloads");
        } catch (IOException e) {
            log().error("Unable to load pending downloads", e);
            transferFile.delete();
        } catch (ClassNotFoundException e) {
            log().error("Unable to load pending downloads", e);
            transferFile.delete();
        } catch (ClassCastException e) {
            log().error("Unable to load pending downloads", e);
            transferFile.delete();
        }
    }

    /**
     * Stores all pending downloads to disk
     */
    private void storeDownloads() {
        // Store pending downloads
        try {
            // Collect all download infos
            List<Download> storedDownloads = new ArrayList(pendingDownloads);
            int nPending = downloads.size();
            int nCompleted = completedDownloads.size();
            synchronized (downloads) {
                storedDownloads.addAll(downloads.values());
            }
            synchronized (completedDownloads) {
                storedDownloads.addAll(completedDownloads);
            }

            log().verbose(
                "Storing " + storedDownloads.size() + " downloads (" + nPending
                    + " pending, " + nCompleted + " completed)");
            File transferFile = new File(Controller.getMiscFilesLocation(),
                getController().getConfigName() + ".transfers");
            // for testing we should support getConfigName() with subdirs
            new File(transferFile.getParent()).mkdirs();
            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
                transferFile));
            ObjectOutputStream oOut = new ObjectOutputStream(fOut);
            oOut.writeObject(storedDownloads);
            oOut.close();
        } catch (IOException e) {
            log().error("Unable to store pending downloads", e);
        }
    }

    // Worker code ************************************************************

    public void run() {
        long waitTime = getController().getWaitTime() * 2;
        int count = 0;
        List uploadsToStart = new LinkedList();
        List uploadsToStartNodes = new LinkedList();
        List uploadsToBreak = new LinkedList();
        List downloadsToBreak = new LinkedList();

        while (!Thread.currentThread().isInterrupted()) {
            // Check uploads/downloads every 10 seconds
            if (logVerbose) {
                log().verbose("Checking uploads/downloads");
            }

            // Check uploads to start
            uploadsToStart.clear();
            uploadsToStartNodes.clear();
            uploadsToBreak.clear();

            if (logVerbose) {
                log().verbose("Checking queued uploads");
            }
            synchronized (queuedUploads) {
                for (Iterator it = queuedUploads.iterator(); it.hasNext();) {
                    Upload upload = (Upload) it.next();

                    if (upload.isBroken()) {
                        // add to break dls
                        uploadsToBreak.add(upload);
                    } else if ((hasFreeUploadSlots() || upload.getPartner()
                        .isOnLAN())
                        && !uploadingTo(upload.getPartner())
                        && !uploadsToStartNodes.contains(upload.getPartner()))
                    {
                        // start the upload if we have free slots
                        // and not uploading to member currently
                        // or user is on local network

                        // TODO should check if this file is not sended (or is
                        // being send) to other user in the last minute or so to
                        // allow for disitributtion of that file by user that
                        // just received that file from us

                        // Enqueue upload to friends and lan members first
                        int uploadIndex = upload.getPartner().isFriend()
                            ? 0
                            : uploadsToStart.size();
                        log().verbose(
                            "Starting upload at queue index: " + uploadIndex);
                        uploadsToStart.add(uploadIndex, upload);
                        // Do not start another upload to that node
                        // I implemented this wrong
                        // I want to allow a start of 2 more uploads to a
                        // partner on lan.
                        // if (upload.getPartner().isOnLAN()) {
                        // if (lanUploadCount++==2) {
                        // uploadsToStartNodes.add(upload.getPartner());
                        // }
                        // } else {
                        // add to list of nodes to indicate are already
                        // uploading to this partner
                        uploadsToStartNodes.add(upload.getPartner());
                        // }
                    }
                }
            }

            if (logVerbose) {
                log().verbose(
                    "Starting " + uploadsToStart.size() + " Uploads, "
                        + uploadsToBreak.size() + " are getting broken");
            }

            // Start uploads
            for (Iterator it = uploadsToStart.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                upload.start();
            }

            for (Iterator it = uploadsToBreak.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                // Set broken
                setBroken(upload);
            }

            // Check pending downloads
            checkPendingDownloads();

            // Checking downloads
            downloadsToBreak.clear();
            int activeDownloads = 0;
            int sleepingDownloads = 0;

            if (logVerbose) {
                log().verbose("Checking downloads");
            }
            synchronized (downloads) {
                for (Iterator it = downloads.values().iterator(); it.hasNext();)
                {
                    Download download = (Download) it.next();
                    if (download.isBroken()) {
                        // Set broken
                        downloadsToBreak.add(download);
                    } else if (download.isStarted()) {
                        activeDownloads++;
                    } else {
                        sleepingDownloads++;
                    }
                }
            }
            if (logVerbose) {
                log().verbose(
                    "Breaking " + downloadsToBreak.size() + " downloads");
            }

            // Now set broken downloads
            for (Iterator it = downloadsToBreak.iterator(); it.hasNext();) {
                Download download = (Download) it.next();
                setBroken(download);
            }

            // log upload / donwloads
            if (count % 2 == 0) {
                log().debug(
                    "Transfers: "
                        + activeDownloads
                        + " download(s), "
                        + sleepingDownloads
                        + " in queue, "
                        + activeUploads.size()
                        + " upload(s), "
                        + queuedUploads.size()
                        + " in queue, "
                        + Format.NUMBER_FORMATS.format(getUploadCounter()
                            .calculateCurrentKBS()) + " KByte/s");
            }

            count++;

            // wait a bit to next work
            try {
                synchronized (waitTrigger) {
                    waitTrigger.wait(waitTime);
                }

                // Wait another 100ms to avoid spamming via trigger
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Break
                break;
            }
        }
    }

    /**
     * Checks the pendings download. Restores them if a source is found
     */
    private void checkPendingDownloads() {
        if (logVerbose) {
            log().verbose(
                "Checking pending downloads (" + pendingDownloads.size() + ")");
        }

        // Checking pending downloads
        List<Download> pendingDownloadsCopy;
        synchronized (pendingDownloads) {
            pendingDownloadsCopy = new ArrayList<Download>(pendingDownloads);
        }

        // Reverse
        // Collections.reverse(pendingDownloadsCopy);

        for (Iterator it = pendingDownloadsCopy.iterator(); it.hasNext();) {
            Download download = (Download) it.next();
            FileInfo fInfo = download.getFile();
            boolean notDownloading = getActiveDownload(fInfo) == null;
            if (notDownloading
                && getController().getFolderRepository().hasJoinedFolder(
                    fInfo.getFolderInfo()))
            {
                Member source = downloadNewestVersion(fInfo, download
                    .isRequestedAutomatic());
                if (source != null) {
                    log().debug(
                        "Pending download restored: " + download + " from "
                            + source);
                    pendingDownloads.remove(download);
                }
            } else {
                // Not joined folder, break pending dl
                log().warn("Pending download removed: " + download);
                pendingDownloads.remove(download);
            }
        }
    }

    // Helper code ************************************************************

    /**
     * logs a up- or download with speed and time
     * 
     * @param type
     * @param took
     * @param fInfo
     * @param member
     */
    public void logTransfer(boolean download, long took, FileInfo fInfo,
        Member member)
    {

        String memberInfo = "";
        if (member != null) {
            memberInfo = ((download) ? " from " : " to ") + "'"
                + member.getNick() + "'";
        }

        String cpsStr = "-";

        // printout rate only if dl last longer than 0,5 s
        if (took > 1000) {
            double cps = fInfo.getSize();
            cps = cps / 1024;
            cps = cps / took;
            cps = cps * 1000;
            cpsStr = CPS_FORMAT.format(cps);
        }

        log().info(
            (download ? "Download" : "Upload") + " completed: "
                + Format.NUMBER_FORMATS.format(fInfo.getSize()) + " bytes in "
                + (took / 1000) + "s (" + cpsStr + " KByte/s): " + fInfo
                + memberInfo);
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

    private void firePendingDownloadEnqueud(TransferManagerEvent event) {
        listenerSupport.pendingDownloadEnqueud(event);
    }
}
