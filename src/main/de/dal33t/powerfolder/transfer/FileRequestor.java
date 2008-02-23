package de.dal33t.powerfolder.transfer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.FileUtils;

/**
 * The filerequestor handles all stuff about requesting new downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.18 $
 */
public class FileRequestor extends PFComponent {
    private Thread myThread;
    private final Queue<Folder> folderQueue;

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
        if (folder == null) {
            log().warn("Folder not joined, not requesting files: " + foInfo);
        }
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

        Collection<FileInfo> incomingFiles = folder
            .getIncomingFiles(requestFromOthers);
        TransferManager tm = getController().getTransferManager();
        for (FileInfo fInfo : incomingFiles) {
            if (fInfo.isDeleted() || tm.isDownloadingActive(fInfo)
                || tm.isDownloadingPending(fInfo))
            {
                // Already downloading/file is deleted
                continue;
            }

            // Try to source non-existent files locally (somwhere in folder or recycle bin).
            if (!fInfo.diskFileExists(getController())) {
                if (sourceLocally(fInfo)) {
                    return;
                }
            }

            // Arrange for a download.
            boolean download = requestFromOthers ||
                    requestFromFriends && fInfo.getModifiedBy()
                            .getNode(getController()).isFriend();

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
                    "folder (" + folder.getName() + ") not on auto donwload");
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

        Collection<FileInfo> incomingFiles = folder.getIncomingFiles(folder
            .getSyncProfile().isAutoDownloadFromOthers());
        TransferManager tm = getController().getTransferManager();
        for (FileInfo fInfo : incomingFiles) {
            if (fInfo.isDeleted() || tm.isDownloadingActive(fInfo)
                || tm.isDownloadingPending(fInfo))
            {
                // Already downloading/file is deleted
                continue;
            }

            // Try to source non-existent files locally
            // (somewhere in folder or recycle bin).
            if (!fInfo.diskFileExists(getController())) {
                if (sourceLocally(fInfo)) {
                    return;
                }
            }

            // Arrange for a download.
            boolean download = folder.getSyncProfile().isAutoDownloadFromOthers() ||
                    folder.getSyncProfile().isAutoDownloadFromFriends() &&
                            fInfo.getModifiedBy().getNode(getController()).isFriend();

            if (download) {
                tm.downloadNewestVersion(fInfo, true);
            }
        }
    }

    /**
     * Try to find this file locally.
     * Look in this folder (other subdirectory perhaps) and in recycle bin.
     *
     * @param fileInfo details of file to find.
     * @return
     */
    private boolean sourceLocally(FileInfo fileInfo) {

        String md5 = fileInfo.getMD5();
        if (md5.length() == 0) {
            // No md5? Can not find a match.
            return false;
        }

        // Try to find in folder, in another directory perhaps.
        FolderRepository folderRepository = getController().getFolderRepository();
        File diskFile = fileInfo.getDiskFile(folderRepository);
        File localBase = fileInfo.getFolder(folderRepository).getLocalBase();

        // Search the localBase for the file
        File identicalFile = findIdenticalFile(localBase, diskFile, md5);
        if (identicalFile != null) {
            // Found it! Now do a file copy.
            try {
                FileUtils.copyFile(identicalFile, diskFile);
                if (logVerbose) {
                    getLogger().verbose("Locally copied " +
                            identicalFile.getAbsolutePath()
                            + " to " + diskFile.getAbsolutePath());
                }

                // Let the TransferManager know what happened.
                getController().getTransferManager().localCopy(fileInfo);
                return true;
            } catch (IOException e) {
                getLogger().error("Problem copying file", e);
            }
        }
        return false;
    }

    /**
     * Find another file with the same name and with same md5.
     *
     * @param currentDirectory where to search
     * @param targetFile file to find
     * @param targetMD5 md5 of file to find
     * @return a matching file, or null
     */
    private static File findIdenticalFile(File currentDirectory, File targetFile,
                                          String targetMD5) {
        File[] files = currentDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                // Recursive search
                File f = findIdenticalFile(file, targetFile, targetMD5);
                if (f != null) {
                    return f;
                }
            } else {

                // Check name
                if (targetFile.getName().equals(file.getName())) {

                    // Check path - do not find self!
                    if (!targetFile.getAbsolutePath().equals(file
                            .getAbsolutePath())) {

                        // Check MD5
                        String md5 = FileUtils.calculateMD5(file);
                        if (md5.equals(targetMD5)) {

                            // Success!
                            return file;
                        }
                    }
                }
            }
        }
        return null;
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
        public void run() {
            triggerFileRequesting();
        }
    }
}