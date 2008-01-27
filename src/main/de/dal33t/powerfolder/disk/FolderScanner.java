package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;

/**
 * Disk Scanner for a folder. It compares the curent database of files agains
 * the ones availeble on disk and produces a ScanResult. MultiThreading is used,
 * for each subfolder of the root a DirectoryCrawler is used with a maximum of
 * MAX_CRAWLERS.<BR>
 * On succes the resultState of ScanResult is ScanResult.ResultState.SCANNED.<BR>
 * If the user aborted the scan (by selecting silent mode) the resultState =
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

    /**
     * This is the list of knownfiles, if a file is found on disk the file is
     * removed from this list. The files that are left in this list after
     * scanning are deleted from disk.
     */
    private HashMap<FileInfo, FileInfo> remaining;

    /** DirectoryCrawler threads that are idle */
    private List<DirectoryCrawler> directoryCrawlersPool = Collections
        .synchronizedList(new LinkedList<DirectoryCrawler>());
    /** Where crawling DirectoryCrawlers are */
    private List<DirectoryCrawler> activeDirectoryCrawlers = Collections
        .synchronizedList(new LinkedList<DirectoryCrawler>());
    /**
     * Maximum number of DirectoryCrawlers after test of a big folder this seams
     * the optimum number.
     */
    private final static int MAX_CRAWLERS = 3;

    /**
     * Files that have their size or modification date changed are collected
     * here.
     */
    private List<FileInfo> changedFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());
    /** Files not in the database (remaining) and are NEW are collected here. */
    private List<FileInfo> newFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());
    /**
     * Files that where marked deleted in the database but are available on disk
     * are collected here.
     */
    private List<FileInfo> restoredFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    /**
     * The files which could not be scanned
     */
    private List<File> unableToScanFiles = Collections
        .synchronizedList(new ArrayList<File>());

    private List<FileInfo> allFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    /** Total number of files in the current scanning folder */
    private int totalFilesCount = 0;

    /**
     * Because of multi threading we use a flag to indicate a failed besides
     * returning false
     */
    private boolean failure = false;

    /**
     * when set to true the scanning process will be aborted and the resultState
     * of the scan will be ScanResult.ResultState.USER_ABORT
     */
    private boolean abort = false;

    /**
     * The semaphore to aquire = means this thread got the folder scan now.
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
    }

    /**
     * Starts the folder scanner, creates MAX_CRAWLERS number of
     * DirectoryCrawler
     */
    public void start() {
        // start directoryCrawlers
        for (int i = 0; i < MAX_CRAWLERS; ++i) {
            DirectoryCrawler directoryCrawler = new DirectoryCrawler();
            Thread thread = new Thread(directoryCrawler,
                "FolderScanner.DirectoryCrawler #" + i);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
            directoryCrawlersPool.add(directoryCrawler);
        }
    }

    /**
     * sets aborted to true (user probably closed the program), and shutsdown
     * the DirectoryCrawlers
     */
    public void shutdown() {
        setAborted(true);
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
     * Abort scanning. when set to true the scanning process will be aborted and
     * the resultState of the scan will be ScanResult.ResultState.USER_ABORT
     * 
     * @param flag
     */
    public void setAborted(boolean flag) {
        abort = flag;
    }

    /**
     * Scans a folder. If the folder scanner is busy the method waits until the
     * scanner is available. See class description for more information.
     * 
     * @param folder
     *            The folder to scan.
     * @return a ScanResult the scan result. result state will never be
     *         <code>ScanResult.ResultState.BUSY</code>
     */
    public ScanResult scanFolderWaitIfBusy(Folder folder) {
        // Aquire the folder wait
        ScanResult result;
        boolean scannerBusy;
        do {
            result = scanFolder(folder);
            scannerBusy = ScanResult.ResultState.BUSY.equals(result
                .getResultState());
            if (scannerBusy) {
                log().debug("Folder scanner is busy, waiting...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log().verbose(e);
                    break;
                }
            }
        } while (scannerBusy);
        return result;
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
            ScanResult result = new ScanResult();
            result.setResultState(ScanResult.ResultState.BUSY);
            return result;
        }

        try {
            currentScanningFolder = folder;
            if (logDebug) {
                log().debug("Scan of folder: " + folder.getName() + " start");
            }
            long started = System.currentTimeMillis();
            // Debug.dumpThreadStacks();

            File base = currentScanningFolder.getLocalBase();
            remaining = new HashMap<FileInfo, FileInfo>(currentScanningFolder
                .getKnownFilesMap());
            if (!scan(base) || failure) {
                // if false there was an IOError
                reset();
                ScanResult result = new ScanResult();
                result.setResultState(ScanResult.ResultState.HARDWARE_FAILURE);
                return result;
            }
            if (abort) {
                reset();
                ScanResult result = new ScanResult();
                result.setResultState(ScanResult.ResultState.USER_ABORT);
                return result;
            }
            // from , to
            Map<FileInfo, FileInfo> moved = tryFindMovements(remaining,
                newFiles);
            Map<FileInfo, List<FilenameProblem>> problemFiles = tryFindProblems(allFiles);

            // Remove the files that where unable to read.

            int n = unableToScanFiles.size();
            for (int i = 0; i < n; i++) {
                File file = unableToScanFiles.get(i);
                FileInfo fInfo = new FileInfo(currentScanningFolder, file);
                remaining.remove(fInfo);
                // TRAC #523
                if (file.isDirectory()) {
                    String dirPath = file.getAbsolutePath().replace(
                        File.separatorChar, '/');
                    // Is a directory. Remove all from remaining that are in
                    // that
                    // dir.
                    log().verbose(
                        "Checking unreadable folder for files that were not scanned: "
                            + dirPath);
                    for (Iterator<FileInfo> it = remaining.keySet().iterator(); it
                        .hasNext();)
                    {
                        FileInfo fInfo2 = it.next();
                        String locationInFolder = fInfo2.getLowerCaseName();
                        if (dirPath.endsWith(locationInFolder)) {
                            log().warn(
                                "Found file in unreadable folder. Unable to scan: "
                                    + fInfo2);
                            it.remove();
                            unableToScanFiles.add(fInfo2
                                .getDiskFile(getController()
                                    .getFolderRepository()));
                        }
                    }
                }
            }

            log().verbose(
                "Unable to scan " + unableToScanFiles.size() + " file(s)");

            // Remaining files = deleted! But only if they are not already
            // flagged
            // as deleted or if the could not be scanned
            for (Iterator<FileInfo> it = remaining.keySet().iterator(); it
                .hasNext();)
            {
                FileInfo fInfo = it.next();
                if (fInfo.isDeleted()) {
                    // This file was already flagged as deleted,
                    // = not a freshly deleted file
                    it.remove();
                }
            }

            // Build scanresult
            ScanResult result = new ScanResult();
            result.setChangedFiles(changedFiles);
            result.setNewFiles(newFiles);
            // FIX for Mac OS X. empty keyset causes problems.
            synchronized (remaining) {
                result.setDeletedFiles(!remaining.keySet().isEmpty() ? remaining
                    .keySet() : Collections.EMPTY_LIST);
            }
            result.setMovedFiles(moved);
            result.setProblemFiles(problemFiles);
            result.setRestoredFiles(restoredFiles);
            result.setTotalFilesCount(totalFilesCount);
            result.setResultState(ScanResult.ResultState.SCANNED);

            // prepare for next scan
            reset();
            if (logEnabled) {
                log().debug(
                    "Scan of folder " + folder.getName() + " done in "
                        + (System.currentTimeMillis() - started)
                        + "ms. Result: " + result.getResultState());
            }
            return result;
        } finally {
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
        changedFiles.clear();
        newFiles.clear();
        allFiles.clear();
        restoredFiles.clear();
        unableToScanFiles.clear();
        currentScanningFolder = null;
        totalFilesCount = 0;
    }

    private void waitForCrawlersToStop() {
        while (!activeDirectoryCrawlers.isEmpty()) {
            log().debug(
                "Waiting for " + activeDirectoryCrawlers.size()
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
     * @return the list of problems
     */
    public static Map<FileInfo, List<FilenameProblem>> tryFindProblems(
        List<FileInfo> files)
    {
        Map<String, FileInfo> lowerCaseNames = new HashMap<String, FileInfo>();
        Map<FileInfo, List<FilenameProblem>> returnValue = new HashMap<FileInfo, List<FilenameProblem>>();
        for (FileInfo fileInfo : files) {
            List<FilenameProblem> problemList = null;
            if (lowerCaseNames.containsKey(fileInfo.getLowerCaseName())) {
                // possible dupe because of same filename but with different
                // case
                FilenameProblem problem = new FilenameProblem(fileInfo,
                    lowerCaseNames.get(fileInfo.getLowerCaseName()));
                problemList = new ArrayList<FilenameProblem>(1);
                problemList.add(problem);
            } else {
                lowerCaseNames.put(fileInfo.getLowerCaseName(), fileInfo);
            }
            if (FilenameProblem.hasProblems(fileInfo.getFilenameOnly())) {
                if (problemList == null) {
                    problemList = new ArrayList<FilenameProblem>(1);
                }
                problemList.addAll(FilenameProblem.getProblems(fileInfo));

            }
            if (problemList != null) {
                returnValue.put(fileInfo, problemList);
            }
        }
        return returnValue;
    }

    /**
     * Scans folder from the local base folder as root
     * 
     * @param folderBase
     *            The file root of the folder to scan from.
     * @returns true on success, false on failure (hardware not found?)
     */
    private boolean scan(File folderBase) {
        File[] filelist = folderBase.listFiles();
        if (filelist == null) { // if filelist is null there is probable an
            // hardware failure
            return false;
        }
        for (File file : filelist) {
            if (failure) {
                return false;
            }
            if (abort) {
                break;
            }
            if (currentScanningFolder.isSystemSubDir(file)) {
                continue;
            }
            if (file.isDirectory()) {
                while (directoryCrawlersPool.isEmpty()) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {

                        }
                    }
                }
                synchronized (directoryCrawlersPool) {
                    DirectoryCrawler crawler = directoryCrawlersPool.remove(0);
                    activeDirectoryCrawlers.add(crawler);
                    crawler.scan(file);
                }

            } else if (file.isFile()) { // the files in the root
                // ignore incomplete (downloading) files
                if (allowFile(file)) {
                    if (!scanFile(file, "")) {
                        failure = true;
                        return false;
                    }
                }
            } else {
                log().warn(
                    "Unable to scan file: " + file.getAbsolutePath()
                        + ". Folder device disconnected? "
                        + currentScanningFolder.isDeviceDisconnected());
                if (currentScanningFolder.isDeviceDisconnected()) {
                    // Hardware not longer available? BREAK scan!
                    failure = true;
                    return false;
                }
                unableToScanFiles.add(file);
            }
        }

        while (!isReady()) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * allow all files but temp download, temp copy file and "powerfolder.db"
     * 
     * @return true if file is allowed
     */
    private static boolean allowFile(File file) {
        return !(FileUtils.isTempDownloadFile(file)
            || FileCopier.isTempBackup(file)
            || file.getName().equalsIgnoreCase(Folder.DB_FILENAME) || file
            .getName().equalsIgnoreCase(Folder.DB_BACKUP_FILENAME));
    }

    /** @return true if all directory Crawler are idle. */
    private boolean isReady() {
        boolean ready;
        synchronized (directoryCrawlersPool) {
            ready = activeDirectoryCrawlers.size() == 0
                && directoryCrawlersPool.size() == MAX_CRAWLERS;
        }
        return ready;
    }

    /**
     * if a file is in the knownFilesNotOnDisk list and in the newlyFoundFiles
     * list with the same size and modification date the file is for 99% sure
     * moved. Map<from , to>
     */
    private Map<FileInfo, FileInfo> tryFindMovements(
        Map<FileInfo, FileInfo> knownFilesNotOnDisk,
        List<FileInfo> newlyFoundFiles)
    {
        Map<FileInfo, FileInfo> returnValue = new HashMap<FileInfo, FileInfo>(1);
        for (FileInfo deletedFile : knownFilesNotOnDisk.keySet()) {
            long size = deletedFile.getSize();
            long modificationDate = deletedFile.getModifiedDate().getTime();
            for (FileInfo newFile : newlyFoundFiles) {
                if (newFile.getSize() == size
                    && newFile.getModifiedDate().getTime() == modificationDate)
                {
                    // possible movement detected
                    if (logEnabled) {
                        log()
                            .debug(
                                "Movement from: " + deletedFile + " to: "
                                    + newFile);
                    }
                    returnValue.put(deletedFile, newFile);
                }
            }
        }
        return returnValue;
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
     * @return true on succes and false on IOError (disk failure or file removed
     *         in the meantime)
     */
    private final boolean scanFile(File fileToScan, String currentDirName) {
        Reject.ifNull(currentScanningFolder,
            "currentScanningFolder must not be null");

        // log().warn("Scanning " + fileToScan.getAbsolutePath());
        if (!fileToScan.exists()) {
            // hardware no longer available
            return false;
        }

        // log().verbose(
        // "scanFile: " + fileToScan + " curdirname: " + currentDirName);
        totalFilesCount++;
        String filename;
        if (currentDirName.length() == 0) {
            filename = fileToScan.getName();
        } else {
            filename = currentDirName + "/" + fileToScan.getName();
        }

        // this is a incomplete fileinfo just find one fast in the remaining
        // list
        FileInfo fInfo = new FileInfo(currentScanningFolder.getInfo(), filename);

        // scannedFiles++;
        // if (scannedFiles % 100 == 0) {
        // System.err.println("(" + scannedFiles + ") Scanning: "
        // + fInfo.getName());
        // }

        FileInfo exists = remaining.remove(fInfo);

        if (exists != null) {// file was known
            allFiles.add(exists);
            if (exists.isDeleted()) {
                // file restored
                if (!exists.inSyncWithDisk(fileToScan)) {
                    restoredFiles.add(exists);
                }

            } else {
                boolean changed = !exists.inSyncWithDisk(fileToScan);
                if (changed) {
                    log().warn("Changed file detected: " + exists.toDetailString() + ". On disk: size: " + fileToScan.length() + ", lastMod: " + fileToScan.lastModified());
                    changedFiles.add(exists);
                }
            }
        } else {// file is new
            if (logVerbose) {
                log().verbose(
                    "NEW file found: " + fInfo.getName() + " hash: "
                        + fInfo.hashCode());
            }
            FileInfo info = new FileInfo(currentScanningFolder, fileToScan);

            // Not necessary. gets set in constructor.
            info.setFolderInfo(currentScanningFolder.getInfo());
            info.setSize(fileToScan.length());
            info.setModifiedInfo(getController().getMySelf().getInfo(),
                new Date(fileToScan.lastModified()));
            newFiles.add(info);
            allFiles.add(info);
        }
        return true;
    }

    /** calculates the subdir of this file relative to the location of the folder */
    private static final String getCurrentDirName(Folder folder, File subFile) {
        String fileName = subFile.getName();
        File parent = subFile.getParentFile();
        File folderBase = folder.getLocalBase();
        while (!folderBase.equals(parent)) {
            if (parent == null) {
                throw new NullPointerException(
                    "Local file seems not to be in a subdir of the local powerfolder copy");
            }
            fileName = parent.getName() + "/" + fileName;
            parent = parent.getParentFile();
        }
        return fileName;
    }

    /** A Thread that scans a directory */
    private class DirectoryCrawler implements Runnable {
        private File root;
        private boolean shutdown = false;

        private void scan(File aRoot) {
            if (this.root != null) {
                throw new IllegalStateException(
                    "cannot scan 2 directories at once");
            }
            synchronized (this) {
                this.root = aRoot;
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
                            log().verbose(e.getMessage());
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

                // scan of this directory is ready, notify FolderScanner we are
                // ready for the next folder.
                synchronized (FolderScanner.this) {
                    FolderScanner.this.notify();
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
        private boolean scanDir(File dirToScan) {
            Reject.ifNull(currentScanningFolder,
                "current scanning folder must not be null");
            String currentDirName = getCurrentDirName(currentScanningFolder,
                dirToScan);
            File[] files = dirToScan.listFiles();
            if (files == null) { // hardware failure
                log().warn(
                    "Unable to scan dir: " + dirToScan.getAbsolutePath()
                        + ". Folder device disconnected? "
                        + currentScanningFolder.isDeviceDisconnected());
                unableToScanFiles.add(dirToScan);
                if (!currentScanningFolder.isDeviceDisconnected()) {
                    return true;
                }
                // hardware failure
                failure = true;
                return false;
            }
            if (files.length == 0) {
                // HACK alert # 593
                if (ConfigurationEntry.DELETE_EMPTY_DIRECTORIES
                    .getValueBoolean(getController()))
                {
                    log().warn(
                        "Found EMPTY DIR, deleting it: "
                            + dirToScan.getAbsolutePath());
                    if (!dirToScan.delete()) {
                        log().error(
                            "Failed to delete: " + dirToScan.getAbsolutePath());
                    }
                }
                return true;
            }
            for (File subFile : files) {
                if (failure) {
                    return false;
                }
                if (abort) {
                    break;
                }
                if (subFile.isDirectory()) {
                    if (!scanDir(subFile)) {
                        // hardware failure
                        failure = true;
                        return false;
                    }
                } else if (subFile.isFile()) {
                    if (allowFile(subFile)) {
                        if (!scanFile(subFile, currentDirName)) {
                            // hardware failure
                            failure = true;
                            return false;
                        }
                    }
                } else {
                    log().warn(
                        "Unable to scan file: " + subFile.getAbsolutePath()
                            + ". Folder device disconnected? "
                            + currentScanningFolder.isDeviceDisconnected());
                    if (currentScanningFolder.isDeviceDisconnected()) {
                        // hardware failure
                        failure = true;
                        return false;
                    }
                    unableToScanFiles.add(subFile);
                }
            }
            return true;
        }
    }
}