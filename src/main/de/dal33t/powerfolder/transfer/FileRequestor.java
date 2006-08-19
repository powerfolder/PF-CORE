package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * The filerequestor handles all stuff about requesting new downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.18 $
 */
public class FileRequestor extends PFComponent {
    private Thread myThread;
    private Object requestTrigger = new Object();

    public FileRequestor(Controller controller) {
        super(controller);
    }

    /**
     * Starts the file requestor
     */
    public void start() {
        myThread = new Thread(new PeriodicalRequestor(), "FileRequestor");
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();
        log().debug("Started");
    }

    /**
     * Triggers to request missing files on folders
     */
    public void triggerFileRequesting() {
        synchronized (requestTrigger) {
            requestTrigger.notifyAll();
        }
    }

    /**
     * Stops file requsting
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }

        log().debug("Stopped");
    }

    /**
     * Requests periodically new files from the folders
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.18 $
     */
    private class PeriodicalRequestor implements Runnable {
        public void run() {
            long waitTime = getController().getWaitTime() * 4;

            while (!myThread.isInterrupted()) { 
                log().debug(getController().getMySelf().getNick() + " start requesting");
                FolderInfo[] folders = getController().getFolderRepository()
                    .getJoinedFolderInfos();
                for (FolderInfo folderInfo : folders) {
                    Folder folder = getController().getFolderRepository()
                        .getFolder(folderInfo);
                    // Download new files on folder if autodownload is wanted
                    if (folder != null) { // maybe null during shutdown
                        requestMissingFilesForAutodownload(folder);
                    }
                }
                
                try {
                    synchronized (requestTrigger) {
                        // use waiter, will quit faster
                        requestTrigger.wait(waitTime);                        
                    }

                    // Sleep a bit to avoid spamming
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    log().verbose(e);
                    break;
                }
            }
        }
    }

    /**
     * Checks all new received filelists from member and downloads unknown/new
     * files, force the settings.
     * <p>
     * FIXME: Does requestFromFriends work?
     */
    public void requestMissingFiles(Folder folder, boolean requestFromFriends,
        boolean requestFromOthers, boolean autoDownload)
    {
        // Dont request files until has own database
        if (!folder.hasOwnDatabase()) {
            return;
        }

        FileInfo[] expectedFiles = folder.getExpecedFiles(requestFromOthers);
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
     */
    public void requestMissingFilesForAutodownload(Folder folder) {
        
        if (!folder.getSyncProfile().isAutodownload()) {            
            return;
        }
        if (logVerbose) {
            log().verbose(getController().getMySelf().getNick() + " Requesting files (autodownload), has own DB? " + folder.hasOwnDatabase());
        }
        
        // Dont request files until has own database
        if (!folder.hasOwnDatabase()) {
            return;
        }

        FileInfo[] expectedFiles = folder.getExpecedFiles(folder
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

}