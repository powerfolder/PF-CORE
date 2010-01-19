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
 * $Id$
 */
package de.dal33t.powerfolder.transfer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FileHistory.Conflict;
import de.dal33t.powerfolder.message.FileHistoryReply;
import de.dal33t.powerfolder.message.FileHistoryRequest;
import de.dal33t.powerfolder.util.ProblemUtil;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.Reject;

/**
 * The filerequestor handles all stuff about requesting new downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.18 $
 */
public class FileRequestor extends PFComponent {
    private Thread myThread;
    private final Queue<Folder> folderQueue;
    private final Queue<FileInfo> pendingRequests;

    public FileRequestor(Controller controller) {
        super(controller);
        folderQueue = new ConcurrentLinkedQueue<Folder>();
        pendingRequests = new ConcurrentLinkedQueue<FileInfo>();
    }

    /**
     * Starts the file requestor
     */
    public void start() {
        myThread = new Thread(new Worker(), "FileRequestor");
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();

        logFine("Started");

        long waitTime = Controller.getWaitTime() * 12;
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
            logWarning("Folder not joined, not requesting files: " + foInfo);
            return;
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
        ProfilingEntry pe = Profiling.start();
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            if (folderQueue.contains(folder)) {
                continue;
            }
            folderQueue.offer(folder);
        }
        Profiling.end(pe, 100);
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
        logFine("Stopped");
    }

    /**
     * Checks all new received filelists from member and downloads unknown/new
     * files, force the settings.
     * <p>
     * FIXME: Does requestFromFriends work?
     * 
     * @param folder
     * @param autoDownload
     */
    public void requestMissingFiles(Folder folder, boolean autoDownload) {
        // Dont request files until has own database
        if (!folder.hasOwnDatabase()) {
            return;
        }

        // TODO: Detect conflicts. Raise problem.
        Collection<FileInfo> incomingFiles = folder.getIncomingFiles(false);
        retrieveNewestVersions(folder, incomingFiles, autoDownload);
    }

    /**
     * Requests missing files for autodownload. May not request any files if
     * folder is not in auto download sync profile. Checks the syncprofile for
     * each file. Sysncprofile may change in the meantime.
     * 
     * @param folder
     *            the folder to request missing files on.
     */
    private void requestMissingFilesForAutodownload(final Folder folder) {
        if (!folder.getSyncProfile().isAutodownload()) {
            if (isFiner()) {
                logFiner("Skipping " + folder + ". not on auto donwload");
            }
            return;
        }
        if (isFiner()) {
            logFiner("Requesting files (autodownload), has own DB? "
                + folder.hasOwnDatabase());
        }

        // Dont request files until has own database
        if (!folder.hasOwnDatabase()) {
            logFine("Not requesting files. No own database for " + folder);
            return;
        }
        if (folder.getConnectedMembersCount() == 0) {
            if (isFiner()) {
                logFiner("Not requesting files. No member connected on "
                    + folder);
            }
            return;
        }
        if (!folder.hasUploadCapacity()) {
            if (isFiner()) {
                logFiner("Not requesting files. Members of folder don't have upload capacity "
                    + folder);
            }
            return;
        }
        Collection<FileInfo> incomingFiles = folder.getIncomingFiles(false);
        if (incomingFiles.isEmpty()) {
            if (isFiner()) {
                logFiner("Not requesting files. No incoming files " + folder);
            }
            return;
        }

        retrieveNewestVersions(folder, incomingFiles, true);
    }

    /**
     * Utility method that will retrieve (download or make directory) the newest
     * version of all given files provided that the given filter accepts a file.
     * 
     * @param folder
     * @param fInfos
     * @param autoDownload
     */
    private void retrieveNewestVersions(Folder folder,
        Collection<FileInfo> fInfos, boolean autoDownload)
    {
        TransferManager tm = getController().getTransferManager();
        List<FileInfo> filesToDownload = new ArrayList<FileInfo>(fInfos.size());
        for (FileInfo fInfo : fInfos) {
            if (myThread.isInterrupted()) {
                return;
            }
            if (fInfo.isDeleted()) {
                // Dont retrieve deleted. done in a different place:
                // Folder.syncRemoteDeletions.
                continue;
            }
            if (fInfo.isFile()) {
                if (tm.isDownloadingActive(fInfo)
                    || tm.isDownloadingPending(fInfo))
                {
                    // Already downloading/file
                    continue;
                }
            }

            if (fInfo.isFile()) {
                filesToDownload.add(fInfo);
            } else if (fInfo.isDiretory()) {
                createDirectory((DirectoryInfo) fInfo);
            }
        }
        if (filesToDownload.isEmpty()) {
            // Quit here.
            return;
        }
        Collections.sort(filesToDownload, folder.getTransferPriorities()
            .getComparator());
        for (FileInfo fInfo : filesToDownload) {
            FileInfo newestVersion = fInfo.getNewestVersion(getController()
                .getFolderRepository());
            if (newestVersion == null) {
                logWarning("Unable to download. Newest version not found: "
                    + fInfo.toDetailString());
                continue;
            }
            prepareDownload(newestVersion, autoDownload);
        }
    }

    private void createDirectory(DirectoryInfo dirInfo) {
        if (isFine()) {
            logFine("Creating/Taking over direcory: "
                + dirInfo.toDetailString());
        }
        File dirFile = dirInfo.getDiskFile(getController()
            .getFolderRepository());
        Folder folder = dirInfo
            .getFolder(getController().getFolderRepository());
        if (folder == null || dirFile == null) {
            logWarning("Unable to created directory. not longer on folder: "
                + dirInfo.toDetailString());
            return;
        }
        folder.scanDirectory(dirInfo, dirFile);
    }

    private void prepareDownload(FileInfo fInfo, boolean autoDownload) {
        TransferManager tm = getController().getTransferManager();
        if (autoDownload
            && fInfo.getLocalFileInfo(getController().getFolderRepository()) != null)
        {
            if (pendingRequests.contains(fInfo)) {
                return;
            }
            // FIXME Currently only support for automatically requested files,
            // additional features require some rewriting of the whole requests
            // thing.
            Collection<Member> sources = tm.getSourcesForVersion(fInfo
                .getNewestVersion(getController().getFolderRepository()));
            Member fhReq = null;
            Member oldSource = null;
            for (Member m : sources) {
                if (m.getIdentity().isSupportingFileHistoryRequests()) {
                    // TODO Send History request!
                    fhReq = m;
                    break;
                } else {
                    oldSource = m;
                }
            }

            // No source at all?
            if (fhReq == null && oldSource == null) {
                // This is autoDownlad so just drop out
                return;
            }

            if (fhReq == null && oldSource != null) {
                if (!ProblemUtil.resolveNoFileHistorySupport(fInfo
                    .getFolder(getController().getFolderRepository()), fInfo,
                    oldSource))
                {
                    return;
                }
            }

            if (fhReq != null) {
                pendingRequests.add(fInfo);
                fhReq.sendMessageAsynchron(new FileHistoryRequest(fInfo), null);
                return;
            }
        }
        tm.downloadNewestVersion(fInfo, autoDownload);
    }

    /**
     * Called if a FileHistory was received.
     * 
     * @param fhReply
     */
    public void receivedFileHistory(final FileHistoryReply fhReply) {
        Reject.notNull(fhReply, "fhReply");
        if (!pendingRequests.remove(fhReply.getRequestFileInfo())) {
            logWarning("Received FileHistory for unrequested FileInfo "
                + fhReply.getRequestFileInfo());
            return;
        }
        getController().getIOProvider().startIO(new Runnable() {
            public void run() {
                checkForConflict(fhReply);
            }
        });
    }

    private void checkForConflict(FileHistoryReply fhRepl) {
        FileInfo fi = fhRepl.getRequestFileInfo();
        FileHistory fh = fhRepl.getFileHistory();
        if (fh == null) {
            logFine("Remote client claims not to have a history for " + fi
                + ", not downloading!");
            logFine("That was a lie, since there are no FileHistories I'll download it anyways!");
            // FIXME But it should not download the file and
            // abort instead if FileHistories come available!
            getController().getTransferManager()
                .downloadNewestVersion(fi, true);
        } else {
            FileHistory localHistory = fi.getFolder(
                getController().getFolderRepository()).getDAO().getFileHistory(
                fi);
            if (localHistory == null) {
                logSevere("Local FileHistory missing for " + fi
                    + ", not downloading!");
            } else {
                Conflict conflict = localHistory.getConflictWith(fh);
                if (conflict != null) {
                    if (ProblemUtil.resolveConflict(conflict)) {
                        // The code currently only supports
                        // autoDownloads!
                        getController().getTransferManager()
                            .downloadNewestVersion(fi, true);
                    }
                } else {
                    // The code currently only supports
                    // autoDownloads!
                    getController().getTransferManager().downloadNewestVersion(
                        fi, true);
                }
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
                try {
                    if (folderQueue.isEmpty()) {
                        synchronized (folderQueue) {
                            folderQueue.wait();
                        }
                    }

                    int nFolders = folderQueue.size();
                    logInfo("Start requesting files for " + nFolders
                        + " folder(s)");
                    long start = System.currentTimeMillis();
                    for (Iterator<Folder> it = folderQueue.iterator(); it
                        .hasNext();)
                    {
                        try {
                            requestMissingFilesForAutodownload(it.next());
                        } catch (RuntimeException e) {
                            logSevere("RuntimeException: " + e.toString(), e);
                        }
                        it.remove();
                        // Give CPU a bit time.
                        Thread.sleep(2);
                    }
                    if (isFiner()) {
                        long took = System.currentTimeMillis() - start;
                        logFiner("Requesting files for " + nFolders
                            + " folder(s) took " + took + "ms.");
                    }

                    // Sleep a bit to avoid spamming
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    logFine("Stopped");
                    logFiner(e);
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