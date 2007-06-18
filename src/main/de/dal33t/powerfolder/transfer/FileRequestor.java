package de.dal33t.powerfolder.transfer;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * The filerequestor handles all stuff about requesting new downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.18 $
 */
public class FileRequestor extends PFComponent {
    private Thread myThread;
    private Queue<Folder> folderQueue;

    public FileRequestor(Controller controller) {
        super(controller);
        folderQueue = new ConcurrentLinkedQueue<Folder>();
    }

    /**
     * Starts the file requestor
     */
    public void start() {
        myThread = new Thread(new Worker(), "FileRequestor");
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();
        log().debug("Started");

        long waitTime = getController().getWaitTime() * 12;
        getController()
            .scheduleAndRepeat(new PeriodicalTriggerTask(), waitTime);
    }

    /**
     * Triggers the worker to request new files on the given folder.
     * 
     * @param foInfo
     *            the folder to request files on
     */
    public void triggerFileRequesting(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder is null");

        Folder folder = foInfo.getFolder(getController());
        if (folderQueue.contains(folder)) {
            return;
        }
        folderQueue.offer(folder);
        synchronized (folderQueue) {
            folderQueue.notifyAll();
        }
    }

    /**
     * Triggers to request missing files on all folders.
     * 
     * @see #triggerFileRequesting(FolderInfo) for single folder file requesting
     *      (=lower CPU usage)
     */
    public void triggerFileRequesting() {
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (folderQueue.contains(folder)) {
                continue;
            }

            folderQueue.offer(folder);
        }
        synchronized (folderQueue) {
            folderQueue.notifyAll();
        }
    }

    /**
     * Stops file requsting
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }
        synchronized (folderQueue) {
            folderQueue.notifyAll();
        }
        log().debug("Stopped");
    }

    /**
     * Checks all new received filelists from member and downloads unknown/new
     * files, force the settings.
     * <p>
     * FIXME: Does requestFromFriends work?
     * 
     * @param folder
     * @param requestFromFriends
     * @param requestFromOthers
     * @param autoDownload
     */
    public void requestMissingFiles(Folder folder, boolean requestFromFriends,
        boolean requestFromOthers, boolean autoDownload)
    {
        // Dont request files until has own database
        if (!folder.hasOwnDatabase()) {
            return;
        }

        List<FileInfo> expectedFiles = folder
            .getIncomingFiles(requestFromOthers);
        TransferManager tm = getController().getTransferManager();
        for (FileInfo fInfo : expectedFiles) {
            if (fInfo.isDeleted() || tm.isDownloadingActive(fInfo)
                || tm.isDownloadingPending(fInfo))
            {
                // Already downloading/file is deleted
                continue;
            }
            boolean download = requestFromOthers
                || (requestFromFriends && fInfo.getModifiedBy().getNode(
                    getController()).isFriend());

            if (download) {
                tm.downloadNewestVersion(fInfo, autoDownload);
            }
        }
    }

    /**
     * Requests missing files for autodownload. May not request any files if
     * folder is not in auto download sync profile. Checks the syncprofile for
     * each file. Sysncprofile may change in the meantime.
     * 
     * @param folder
     *            the folder to request missing files on.
     */
    private void requestMissingFilesForAutodownload(Folder folder) {
        if (!folder.getSyncProfile().isAutodownload()) {
            if (logEnabled) {
                log().debug(
                    "folder (" + folder.getName() + ")not on auto donwload");
            }
            return;
        }
        if (logVerbose) {
            log().verbose(
                "Requesting files (autodownload), has own DB? "
                    + folder.hasOwnDatabase());
        }

        // Dont request files until has own database
        if (!folder.hasOwnDatabase()) {
            log().debug("not requesting because no own database");
            return;
        }

        List<FileInfo> expectedFiles = folder.getIncomingFiles(folder
            .getSyncProfile().isAutoDownloadFromOthers());
        TransferManager tm = getController().getTransferManager();
        for (FileInfo fInfo : expectedFiles) {
            if (fInfo.isDeleted() || tm.isDownloadingActive(fInfo)
                || tm.isDownloadingPending(fInfo))
            {
                // Already downloading/file is deleted
                continue;
            }
            boolean download = folder.getSyncProfile()
                .isAutoDownloadFromOthers()
                || (folder.getSyncProfile().isAutoDownloadFromFriends() && fInfo
                    .getModifiedBy().getNode(getController()).isFriend());

            if (download) {
                tm.downloadNewestVersion(fInfo, true);
            }
        }
    }

    /**
     * Requests periodically new files from the folders
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.18 $
     */
    private class Worker implements Runnable {
        public void run() {
            while (!myThread.isInterrupted()) {
                if (folderQueue.isEmpty()) {
                    synchronized (folderQueue) {
                        try {
                            folderQueue.wait();
                        } catch (InterruptedException e) {
                            log().debug("Stopped");
                            log().verbose(e);
                            break;
                        }
                    }
                }

                int nFolders = folderQueue.size();
                log().info(
                    "Start requesting files for " + nFolders + " folder(s)");
                long start = System.currentTimeMillis();
                for (Iterator<Folder> it = folderQueue.iterator(); it.hasNext();)
                {
                    requestMissingFilesForAutodownload(it.next());
                    it.remove();
                }
                if (logVerbose) {
                    long took = System.currentTimeMillis() - start;
                    log().verbose(
                        "Requesting files for " + nFolders + " folder(s) took "
                            + took + "ms.");
                }

                try {
                    // Sleep a bit to avoid spamming
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log().debug("Stopped");
                    log().verbose(e);
                    break;
                }
            }
        }
    }

    /**
     * Periodically triggers the file requesting on all folders.
     */
    private final class PeriodicalTriggerTask extends TimerTask {
        @Override
        public void run()
        {
            triggerFileRequesting();
        }
    }
}