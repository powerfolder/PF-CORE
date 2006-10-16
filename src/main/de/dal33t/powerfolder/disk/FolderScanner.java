package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
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
     * Create a folder scanner.
     * 
     * @param controller
     *            the controller that holds this folder.
     */
    public FolderScanner(Controller controller) {
        super(controller);
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
    }

    /**
     * Abort scanning. when set to true the scanning process will be aborted and
     * the resultState of the scan will be ScanResult.ResultState.USER_ABORT
     */
    public void setAborted(boolean flag) {
        abort = flag;
    }

    /**
     * Scan a folder. See class description for explaining.
     * 
     * @param folder
     *            The folder to scan.
     * @return a ScanResult
     */
    public ScanResult scanFolder(Folder folder) {
        Reject.ifNull(folder, "folder cannot be null");
        if (currentScanningFolder != null) {
            throw new IllegalStateException(
                "Not allowed to start another scan while scanning is in process");
        }
        if (logEnabled) {
            log().info("scan folder: " + folder.getName() + " start");
        }
        long started = System.currentTimeMillis();

        currentScanningFolder = folder;

        File base = currentScanningFolder.getLocalBase();
        remaining = currentScanningFolder.getKnownFiles();
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
        Map<FileInfo,FileInfo> moved = tryFindMovements(remaining, newFiles);
        // for testing purposes I add some problem files here
        // [test]
        //FileInfo testFile1 = new FileInfo(folder.getInfo(), "sub/AUX");
        //testFile1.setSize(1000);
        //testFile1.setModifiedInfo(getController().getNodeManager().getMySelf()
        //    .getInfo(), new Date());
        //FileInfo testFile2 = new FileInfo(folder.getInfo(), "?hhh");
        //testFile2.setSize(1000);
        //testFile2.setModifiedInfo(getController().getNodeManager().getMySelf()
        //    .getInfo(), new Date());

//        allFiles.add(testFile1);
  //      allFiles.add(testFile2);
        // [\test]
        Map<FileInfo, List<FilenameProblem>> problemFiles = tryFindProblems(
            allFiles);

        // Remaining files = deleted!
        // Set size to 0 of these remaining files, to keep backward
        // compatibility
        for (FileInfo info : remaining.keySet()) {
            info.setSize(0);
        }

        // Build scanresult
        ScanResult result = new ScanResult();
        result.setChangedFiles(changedFiles);
        result.setNewFiles(newFiles);
        result.setDeletedFiles(new ArrayList<FileInfo>(remaining.keySet()));
        result.setMovedFiles(moved);
        result.setProblemFiles(problemFiles);
        result.setRestoredFiles(restoredFiles);
        result.setTotalFilesCount(totalFilesCount);
        result.setResultState(ScanResult.ResultState.SCANNED);
        // here temporary as long as not enabled for testing, should be in
        // folder
        if (result.getResultState().equals(ScanResult.ResultState.SCANNED)) {
            if (result.getProblemFiles().size() > 0) {
                FileNameProblemHandler handler = getController()
                    .getFolderRepository().getFileNameProblemHandler();
                if (handler != null) {
                    handler.fileNameProblemsDetected(new FileNameProblemEvent(
                        currentScanningFolder, result));
                }
            }
        }
        // prepera for next scan
        reset();
        if (logEnabled) {
            log().info(
                "scan folder " + folder.getName() + " done in "
                    + (System.currentTimeMillis() - started));
        }
        return result;
    }

    /** after scanning the state of this scanning should be reset */
    private void reset() {
        abort = false;
        failure = false;
        allFiles.clear();
        changedFiles.clear();
        newFiles.clear();
        restoredFiles.clear();
        currentScanningFolder = null;
        totalFilesCount = 0;
    }

    /**
     * Produces a list of FilenameProblems per FileInfo that has problems.
     * Public for testing
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
                    lowerCaseNames.get(fileInfo));
                problemList = new ArrayList<FilenameProblem>(1);
                problemList.add(problem);
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
                if (!FileUtils.isTempDownloadFile(file)
                    && !FileCopier.isTempBackup(file))
                {
                    if (!scanFile(file, "")) {
                        failure = true;
                        return false;
                    }
                }
            } else { // Hardware not longer available? BREAK scan!
                failure = true;
                return false;
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
     * moved.
     * Map<from , to>
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

        FileInfo exists;
        synchronized (remaining) {
            exists = remaining.remove(fInfo);
        }
        if (exists != null) {// file was known
            synchronized (allFiles) {
                allFiles.add(exists);
            }
            if (exists.isDeleted()) {
                // file restored
                synchronized (restoredFiles) {
                    // Resync state with disk
                    exists.syncFromDiskIfRequired(getController(), fileToScan);
                    restoredFiles.add(exists);
                }
            } else {
                boolean changed = false;
                long modification = fileToScan.lastModified();
                if (exists.getModifiedDate().getTime() < modification) {
                    // disk file = newer
                    changed = true;
                }
                long size = fileToScan.length();
                if (exists.getSize() != size) {
                    // size changed
                    log().error(
                        "rare size change (modification date the same?!): from "
                            + exists.getSize() + " to: " + size);
                    changed = true;
                }
                if (changed) {
                    synchronized (changedFiles) {
                        changedFiles.add(exists);
                    }
                }
            }
        } else {// file is new
            if (logVerbose) {
                log().verbose(
                    "NEW file found: " + fInfo.getName() + " hash: "
                        + fInfo.hashCode());
            }
            FileInfo info = new FileInfo(currentScanningFolder, fileToScan);

            info.setFolder(currentScanningFolder);
            info.setSize(fileToScan.length());
            info.setModifiedInfo(getController().getMySelf().getInfo(),
                new Date(fileToScan.lastModified()));
            synchronized (newFiles) {
                newFiles.add(info);
            }
            synchronized (allFiles) {
                allFiles.add(info);
            }

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

        private void scan(File root) {
            if (this.root != null) {
                throw new IllegalStateException(
                    "cannot scan 2 directories at once");
            }
            this.root = root;
            synchronized (this) {
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
                        try {
                            wait();
                            if (shutdown) {
                                return;
                            }
                        } catch (InterruptedException e) {
                            log().verbose(e.getMessage());
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
                failure = true;
                return false;
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
                    if (!FileUtils.isTempDownloadFile(subFile)
                        && !FileCopier.isTempBackup(subFile))
                    {
                        if (!scanFile(subFile, currentDirName)) {
                            // hardware failure
                            failure = true;
                            return false;
                        }
                    }
                } else {// hardware failure
                    failure = true;
                    return false;
                }
            }
            return true;
        }
    }
}