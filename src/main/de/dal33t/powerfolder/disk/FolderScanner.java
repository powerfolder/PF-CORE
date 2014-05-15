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
 * $Id: FolderScanner.java 18828 2012-05-10 01:24:49Z tot $
 */
package de.dal33t.powerfolder.disk;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.ScanResult.ResultState;
import de.dal33t.powerfolder.disk.problem.FilenameProblemHelper;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * Disk Scanner for a folder. It compares the curent database of files agains
 * the ones availeble on disk and produces a ScanResult. MultiThreading is used,
 * for each subfolder of the root a DirectoryCrawler is used with a maximum of
 * MAX_CRAWLERS.<BR>
 * On succes the resultState of ScanResult is ScanResult.ResultState.SCANNED.<BR>
 * If the user aborted the scan (by selecting paused mode) the resultState =
 * ScanResult.ResultState.USER_ABORT.<BR>
 * If during scanning files dare deleted when scanning, the whole folder is
 * deleted or in practice the harddisk fails the resultState is
 * ScanResult.ResultState.HARDWARE_FAILURE. <BR>
 * usage:<BR>
 * <code>
 * ScanResult result = folderScannner.scanFolder(folder);
 * </code>
 */
public class FolderScanner extends PFComponent {
    /** The folder that is being scanned */
    private Folder currentScanningFolder;
    private ScanResult currentScanResult;

    /**
     * This is the list of knownfiles, if a file is found on disk the file is
     * removed from this list. The files that are left in this list after
     * scanning are deleted from disk.
     */
    private Map<String, FileInfo> remaining = Util.createConcurrentHashMap();

    /** DirectoryCrawler threads that are idle */
    private final List<DirectoryCrawler> directoryCrawlersPool = new CopyOnWriteArrayList<DirectoryCrawler>();

    /** Where crawling DirectoryCrawlers are */
    private final List<DirectoryCrawler> activeDirectoryCrawlers = new CopyOnWriteArrayList<DirectoryCrawler>();
    /**
     * Maximum number of DirectoryCrawlers after test of a big folder this seams
     * the optimum number.
     */
    private int maxCrawlers = 3;

    /**
     * The files which could not be scanned
     */
    private List<Path> unableToScanFiles = new CopyOnWriteArrayList<Path>();

    /**
     * Because of multi threading we use a flag to indicate a failed besides
     * returning false
     */
    private volatile boolean failure = false;

    /**
     * when set to true the scanning process will be aborted and the resultState
     * of the scan will be ScanResult.ResultState.USER_ABORT
     */
    private volatile boolean abort = false;

    /**
     * The semaphore to acquire = means this thread got the folder scan now.
     */
    private Semaphore threadOwnership;

    /**
     * Do not use this constructor, this should only be done by the Folder
     * Repositoty, to get the folder scanner call:
     * folderRepository.getFolderScanner()
     *
     * @param controller
     *            the controller that holds this folder.
     */
    FolderScanner(Controller controller) {
        super(controller);
        threadOwnership = new Semaphore(1);
        maxCrawlers = ConfigurationEntry.FOLDER_SCANNER_MAX_CRAWLERS
            .getValueInt(getController());
    }

    /**
     * Starts the folder scanner, creates MAX_CRAWLERS number of
     * DirectoryCrawler
     */
    public void start() {
        // start directoryCrawlers
        for (int i = 0; i < maxCrawlers; ++i) {
            DirectoryCrawler directoryCrawler = new DirectoryCrawler();
            Thread thread = new Thread(directoryCrawler,
                "FolderScanner.DirectoryCrawler #" + i);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
            directoryCrawlersPool.add(directoryCrawler);
        }
        currentScanResult = new ScanResult(true);
    }

    /**
     * sets aborted to true (user probably closed the program), and shutsdown
     * the DirectoryCrawlers
     */
    public void shutdown() {
        abort = true;
        synchronized (directoryCrawlersPool) {
            for (DirectoryCrawler directoryCrawler : directoryCrawlersPool) {
                directoryCrawler.shutdown();
            }
            for (DirectoryCrawler directoryCrawler : activeDirectoryCrawlers) {
                directoryCrawler.shutdown();
            }
        }
        // waitForCrawlersToStop();
    }

    public Folder getCurrentScanningFolder() {
        return currentScanningFolder;
    }

    /**
     * Abort scanning. when called the scanning process will be aborted and the
     * resultState of the scan will be ScanResult.ResultState.USER_ABORT
     *
     * @return true if abort has been initiated, false if not currently scanning
     */
    public boolean abortScan() {
        if (currentScanningFolder != null) {
            abort = true;
            return true;
        }
        return false;
    }

    /**
     * Scans a folder. See class description for explaining.
     *
     * @param folder
     *            The folder to scan.
     * @return a ScanResult the scan result.
     */
    public synchronized ScanResult scanFolder(Folder folder) {
        Reject.ifNull(folder, "folder cannot be null");

        if (!threadOwnership.tryAcquire()) {
            return new ScanResult(ScanResult.ResultState.BUSY);
        }

        try {
            currentScanningFolder = folder;
            if (isFiner()) {
                logFiner("Scan of folder: " + folder.getName() + " start");
            }
            long started = System.currentTimeMillis();
            // Debug.dumpThreadStacks();

            Path base = currentScanningFolder.getLocalBase();
            remaining.clear();
            for (FileInfo fInfo : currentScanningFolder.getKnownFiles()) {
                remaining.put(fInfo.getRelativeName(), fInfo);
            }
            for (FileInfo fInfo : currentScanningFolder.getKnownDirectories()) {
                remaining.put(fInfo.getRelativeName(), fInfo);
            }
            if (!scan(base) || failure) {
                // if false there was an IOError
                reset();
                return new ScanResult(ScanResult.ResultState.HARDWARE_FAILURE);
            }
            if (abort) {
                reset();
                return new ScanResult(ScanResult.ResultState.USER_ABORT);
            }
            // from , to
            tryFindMovementsInCurrentScan();
            tryFindProblemsInCurrentScan();

            // Remove the files that where unable to read.

            int n = unableToScanFiles.size();
            for (int i = 0; i < n; i++) {
                Path file = unableToScanFiles.get(i);
                FileInfo fInfo = FileInfoFactory.lookupInstance(
                    currentScanningFolder, file);
                remaining.remove(fInfo.getRelativeName());
                // TRAC #523
                if (Files.isDirectory(file)) {
                    String dirPath = file.toAbsolutePath().toString().replace(
                        file.getFileSystem().getSeparator(), "/");
                    // Is a directory. Remove all from remaining that are in
                    // that
                    // dir.
                    logFiner("Checking unreadable folder for files that were not scanned: "
                        + dirPath);
                    for (Iterator<FileInfo> it = remaining.values().iterator(); it
                        .hasNext();)
                    {
                        FileInfo fInfo2 = it.next();
                        String locationInFolder = fInfo2
                            .getLowerCaseFilenameOnly();
                        if (dirPath.endsWith(locationInFolder)) {
                            logWarning("Found file in unreadable folder. Unable to scan: "
                                + fInfo2);
                            it.remove();
                            unableToScanFiles.add(fInfo2
                                .getDiskFile(getController()
                                    .getFolderRepository()));
                        }
                    }
                }
            }

            if (isWarning()) {
                if (unableToScanFiles.isEmpty()) {
                    logFiner("Unable to scan " + unableToScanFiles.size()
                        + " file(s)");
                } else {
                    logWarning("Unable to scan " + unableToScanFiles.size()
                        + " file(s)");
                }
            }
            // Remaining files = deleted! But only if they are not already
            // flagged
            // as deleted or if the could not be scanned
            for (Iterator<FileInfo> it = remaining.values().iterator(); it
                .hasNext();)
            {
                FileInfo fInfo = it.next();
                if (fInfo.isDeleted()) {
                    // This file was already flagged as deleted,
                    // = not a freshly deleted file
                    it.remove();
                } else {
                    logFine("Deleted file detected: " + fInfo.toDetailString());
                }
            }

            // Build scanresult

            for (FileInfo fileInfo : remaining.values()) {
                // Do not perform FileInfo.syncFromDiskIfRequired
                // This would leave to extra I/O for all files that had been
                // deleted in the past on every scan.
                FileInfo deletedFileInfo = FileInfoFactory
                    .deletedFile(fileInfo, getController().getMySelf()
                        .getInfo(), new Date());
                currentScanResult.deletedFiles.add(deletedFileInfo);
            }

            // result.setMovedFiles(moved);
            // result.setProblemFiles(problemFiles);
            // result.setRestoredFiles(restoredFiles);
            // currentScanResult.totalFilesCount = totalFilesCount;
            // result.setResultState(ScanResult.ResultState.SCANNED);

            // prepare for next scan
            ScanResult myResult = currentScanResult;
            reset();
            if (isWarning()) {
                long took = System.currentTimeMillis() - started;
                if (currentScanResult.getResultState() == ResultState.SCANNED
                    || took > 1000L * 60 * 5)
                {
                    logFiner("Scan of folder " + folder.getName() + " done in "
                        + took + "ms. Result: "
                        + currentScanResult.getResultState());
                } else {
                    logWarning("Scan of folder " + folder.getName()
                        + " done in " + took + "ms. Result: "
                        + currentScanResult.getResultState());
                }
            }
            return myResult;
        } finally {
            // Not longer scanning
            currentScanningFolder = null;
            // Remove ownership for this thread
            threadOwnership.release();
        }
    }

    /** after scanning the state of this scanning should be reset */
    private void reset() {
        // Ensure gracful stop
        waitForCrawlersToStop();
        abort = false;
        failure = false;
        // changedFiles.clear();
        // newFiles.clear();
        // allFiles.clear();
        // restoredFiles.clear();
        unableToScanFiles.clear();
        // totalFilesCount = 0;
        currentScanResult = new ScanResult(true);
    }

    private void waitForCrawlersToStop() {
        while (!activeDirectoryCrawlers.isEmpty()) {
            logFine("Waiting for " + activeDirectoryCrawlers.size()
                + " crawlers to stop");
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Produces a list of FilenameProblems per FileInfo that has problems.
     * Public for testing
     *
     * @param files
     */
    private void tryFindProblemsInCurrentScan() {
        if (!PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(getController()))
        {
            return;
        }
        tryToFindProblemsInCurrentScan(currentScanResult.getChangedFiles());
        tryToFindProblemsInCurrentScan(currentScanResult.getRestoredFiles());
        tryToFindProblemsInCurrentScan(currentScanResult.getNewFiles());
    }

    private void tryToFindProblemsInCurrentScan(Collection<FileInfo> files)
    {
        for (FileInfo fileInfo : files) {
            List<Problem> problemList = null;
            if (FilenameProblemHelper.hasProblems(fileInfo)) {
                if (problemList == null) {
                    problemList = new ArrayList<Problem>();
                }
                problemList.addAll(FilenameProblemHelper.getProblems(
                    getController(), fileInfo));

            }
            if (problemList != null) {
                currentScanResult.putFileProblems(fileInfo, problemList);
            }
        }
    }

    /**
     * Scans folder from the local base folder as root
     *
     * @param folderBase
     *            The file root of the folder to scan from.
     * @returns true on success, false on failure (hardware not found?)
     */
    private boolean scan(Path folderBase) {
        if (folderBase == null) {
            failure = true;
            return false;
        }

        try (DirectoryStream<Path> stream = Files
            .newDirectoryStream(folderBase)) {

            if (stream == null) {
                failure = true;
                return false;
            }

            Iterator<Path> it = stream.iterator();

            if (it == null) {
                failure = true;
                return false;
            }

            while (it.hasNext()) {
                Path path = it.next();

                if (failure) {
                    return false;
                }
                if (abort) {
                    break;
                }
                if (Files.exists(path) && Files.isRegularFile(path)) {
                    if (PathUtils.isScannable(path, currentScanningFolder)) {
                        if (!scanFile(path, "")) {
                            failure = true;
                            return false;
                        }
                    }
                } else if (Files.isDirectory(path)) {
                    if (!PathUtils.isScannable(path, currentScanningFolder)
                        || currentScanningFolder.isSystemSubDir(path))
                    {
                        continue;
                    }
                    while (directoryCrawlersPool.isEmpty()) {
                        synchronized (this) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    synchronized (directoryCrawlersPool) {
                        DirectoryCrawler crawler = directoryCrawlersPool
                            .remove(0);
                        activeDirectoryCrawlers.add(crawler);
                        crawler.scan(path);
                    }
                } else {
                    boolean deviceDisconnected = currentScanningFolder
                        .checkIfDeviceDisconnected();
                    logWarning("Unable to scan file: " + path.toAbsolutePath()
                        + ". Folder device disconnected? " + deviceDisconnected);
                    if (deviceDisconnected) {
                        // Hardware not longer available? BREAK scan!
                        failure = true;
                        return false;
                    }
                    unableToScanFiles.add(path);
                }
            }
        } catch (IOException ioe) {
            return false;
        }

        while (!isReady()) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                logFiner(e);
                return false;
            }
        }
        return true;
    }

    /** @return true if all directory Crawler are idle. */
    private boolean isReady() {
        boolean ready;
        synchronized (directoryCrawlersPool) {
            ready = activeDirectoryCrawlers.isEmpty()
                && directoryCrawlersPool.size() == maxCrawlers;
        }
        return ready;
    }

    /**
     * if a file is in the knownFilesNotOnDisk list and in the newlyFoundFiles
     * list with the same size and modification date the file is for 99% sure
     * moved. Map<from , to>
     */
    private void tryFindMovementsInCurrentScan() {
        if (Feature.CORRECT_MOVEMENT_DETECTION.isDisabled()) {
            return;
        }
        for (FileInfo deletedFile : remaining.values()) {
            long size = deletedFile.getSize();
            long modificationDate = deletedFile.getModifiedDate().getTime();
            for (FileInfo newFile : currentScanResult.newFiles) {
                if (newFile.getSize() == size
                    && newFile.getModifiedDate().getTime() == modificationDate)
                {
                    // possible movement detected
                    if (isFine()) {
                        logFine("Movement from: " + deletedFile + " to: "
                            + newFile);
                    }
                    currentScanResult.movedFiles.put(deletedFile, newFile);
                }
            }
        }
    }

    /**
     * scans a single file.
     *
     * @param fileToScan
     *            the disk file to examine.
     * @param currentDirName
     *            The location the use when creating a FileInfo. This is that
     *            same for each file in the same directory and so not neccesary
     *            to "calculate" this per file.
     * @return true on success and false on IOError (disk failure or file
     *         removed in the meantime)
     */
    private boolean scanFile(Path fileToScan, String currentDirName) {
        Reject.ifNull(currentScanningFolder,
            "currentScanningFolder must not be null");
        currentScanResult.incrementTotalFilesCount();
        String filename;
        if (currentDirName.length() == 0) {
            filename = fileToScan.getFileName().toString();
        } else {
            filename = currentDirName + '/' + fileToScan.getFileName().toString();
        }
        return scanDiskItem(fileToScan, FileInfoFactory.decodeIllegalChars(filename), false);
    }

    /**
     * scans a single directory.
     *
     * @param dirToScan
     *            the disk directory to examine.
     * @param currentDirName
     *            The location the use when creating a FileInfo. This is that
     *            same for each file in the same directory and so not neccesary
     *            to "calculate" this per file.
     * @return true on success and false on IOError (disk failure or file
     *         removed in the meantime)
     */
    private boolean scanDirectory(Path dirToScan, String currentDirName) {
        Reject.ifNull(currentScanningFolder,
            "currentScanningFolder must not be null");
        if (isFiner()) {
            logFiner("Scanning subdir " + dirToScan + " / " + currentDirName);
        }
        currentScanResult.incrementTotalFilesCount();
        return scanDiskItem(dirToScan, FileInfoFactory.decodeIllegalChars(currentDirName), true);
    }

    /**
     * scans a single file.
     *
     * @param fileToScan
     *            the disk file to examine.
     * @param currentDirName
     *            The location the use when creating a FileInfo. This is that
     *            same for each file in the same directory and so not neccesary
     *            to "calculate" this per file.
     * @return true on success and false on IOError (disk failure or file
     *         removed in the meantime)
     */
    private boolean scanDiskItem(Path fileToScan, String filename,
        boolean directory)
    {
        Reject.ifNull(currentScanningFolder,
            "currentScanningFolder must not be null");

        // #1531 / #1804
        FileInfo exists = remaining.remove(filename);
        if (exists == null && FileInfo.IGNORE_CASE) {
            // Try harder, same file with the
            for (FileInfo otherFInfo : remaining.values()) {
                if (otherFInfo.getRelativeName().equalsIgnoreCase(filename)) {
                    if (isFiner()) {
                        logFiner("Found local diskfile with diffrent name-case in db. file: "
                            + fileToScan.toAbsolutePath().toString()
                            + ", dbFile: "
                            + otherFInfo.toDetailString());
                    }
                    // if (fInfo.getRelativeName().equals(
                    // otherFInfo.getRelativeName())
                    // && !fInfo.equals(otherFInfo))
                    // {
                    // throw new RuntimeException(
                    // "Bad failure: FileInfos not equal. "
                    // + fInfo.toDetailString() + " and "
                    // + otherFInfo.toDetailString()
                    // + " Probably FolderInfo objects are not equal?");
                    // }
                    remaining.remove(otherFInfo.getRelativeName());
                    exists = otherFInfo;
                }
            }
        }
        try {
            if (exists != null) {// file was known
                if (exists.isDeleted()) {
                    // file restored
                    FileInfo restoredFile = exists.syncFromDiskIfRequired(
                        currentScanningFolder, fileToScan);
                    if (restoredFile != null) {
                        if (isInfo()) {
                            logInfo("Restored detected: "
                                + exists.toDetailString() + ". On disk: size: "
                                + Files.size(fileToScan) + ", lastMod: "
                                + Files.getLastModifiedTime(fileToScan));
                        }
                        currentScanResult.restoredFiles.add(restoredFile);
                    }
                } else {
                    FileInfo changedFile = exists.syncFromDiskIfRequired(
                        currentScanningFolder, fileToScan);
                    if (changedFile != null) {
                        if (isInfo()
                            && currentScanningFolder.getDiskItemFilter()
                                .isRetained(changedFile))
                        {
                            logInfo("Change detected: "
                                + exists.toDetailString() + ". On disk: size: "
                                + Files.size(fileToScan) + ", lastMod: "
                                + Files.getLastModifiedTime(fileToScan));
                        }
                        currentScanResult.changedFiles.add(changedFile);
                    }
                }
            } else {
                // file is new
                FileInfo info = FileInfoFactory.newFile(currentScanningFolder,
                    fileToScan, getController().getMySelf().getInfo(),
                    directory);
                currentScanResult.newFiles.add(info);
                if (isFiner()) {
                    logFiner("New found: " + info.toDetailString());
                }
            }
        } catch (Exception e) {
            logWarning("Unable to scan: " + fileToScan + ". " + e);
            unableToScanFiles.add(fileToScan);
        }
        return true;
    }

    /**
     * calculates the subdir of this file relative to the location of the folder
     */
    private static String getCurrentDirName(Folder folder, Path subFile) {
        String fileName = subFile.getFileName().toString();
        Path parent = subFile.getParent();
        Path folderBase = folder.getLocalBase();
        while (!folderBase.equals(parent)) {
            if (parent == null) {
                throw new NullPointerException(
                    "Local file seems not to be in a subdir of the local powerfolder copy");
            }
            fileName = parent.getFileName().toString() + '/' + fileName;
            parent = parent.getParent();
        }
        return fileName;
    }

    /** A Thread that scans a directory */
    private class DirectoryCrawler implements Runnable {
        private Path root;
        private boolean shutdown = false;

        private void scan(Path aRoot) {
            if (root != null) {
                throw new IllegalStateException(
                    "cannot scan 2 directories at once");
            }
            synchronized (this) {
                root = aRoot;
                notify();
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notify();
            }
        }

        public void run() {
            while (true) {
                try {
                    while (root == null) {
                        synchronized (this) {
                            if (root != null) {
                                // Make sure that we don't wait with root!
                                continue;
                            }
                            try {
                                wait();
                                if (shutdown) {
                                    return;
                                }
                            } catch (InterruptedException e) {
                                logFiner(e.getMessage());
                                return;
                            }
                        }
                    }
                    if (!scanDir(root)) {
                        // hardware failure
                        failure = true;
                    }
                    root = null;

                    synchronized (directoryCrawlersPool) {
                        activeDirectoryCrawlers.remove(this);
                        directoryCrawlersPool.add(this);
                    }

                } catch (RuntimeException e) {
                    logSevere("Folder scanner crashed @ " + root + ". " + e, e);
                    failure = true;
                } finally {
                    // scan of this directory is ready, notify FolderScanner we
                    // are ready for the next folder.
                    synchronized (FolderScanner.this) {
                        FolderScanner.this.notify();
                    }
                }
            }
        }

        /**
         * Scans a directory, will recurse into subdirectories
         *
         * @param dirToScan
         *            The directory to scan
         * @return true or succes or false is failed (harware failure or
         *         directory or file removed in the meantime)
         */
        private boolean scanDir(Path dirToScan) {
            Reject.ifNull(currentScanningFolder,
                "current scanning folder must not be null");
            String currentDirName = getCurrentDirName(currentScanningFolder,
                dirToScan);
            try {
                // Give CPU room to breath. Don't consume 100% CPU.
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            scanDirectory(dirToScan, currentDirName);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirToScan)) {
                Iterator<Path> it = stream.iterator();

                if (it == null) {
                    throw new IOException("Unable to access directory");
                }

                while (it.hasNext()) {
                    Path path = it.next();
                    if (failure) {
                        return false;
                    }
                    if (abort) {
                        break;
                    }
                    if (Files.isRegularFile(path)) {
                        if (PathUtils.isScannable(path, currentScanningFolder)
                            && !scanFile(path, currentDirName)) {
                            failure = true;
                            return false;
                        }
                    } else if (Files.isDirectory(path)) {
                        if (PathUtils.isScannable(path, currentScanningFolder)
                            && !scanDir(path)) {
                            failure = true;
                            return false;
                        }
                    } else {
                        boolean deviceDisconnected = currentScanningFolder
                            .checkIfDeviceDisconnected();
                        logWarning("Unable to scan file: "
                            + path.toAbsolutePath()
                            + ". Folder device disconnected? " + deviceDisconnected);
                        if (deviceDisconnected) {
                            // hardware failure
                            failure = true;
                            return false;
                        }
                        unableToScanFiles.add(path);
                    }
                }

                return true;
            }
            catch (IOException ioe) {
                boolean deviceDisconnected = currentScanningFolder
                    .checkIfDeviceDisconnected();
                logWarning("Unable to scan dir: " + dirToScan.toAbsolutePath()
                    + ". Folder device disconnected? " + deviceDisconnected);
                if (deviceDisconnected) {
                    // hardware failure
                    failure = true;
                    return false;
                }
                unableToScanFiles.add(dirToScan);
                return true;
            }
        }
    }
}