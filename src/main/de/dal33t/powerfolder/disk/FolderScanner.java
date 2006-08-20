package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;

/**
 * Scanner for a folder. Uses MAX_CRAWLERS number of threads that are crawling
 * in the direct subfolders of the root.
 */
public class FolderScanner extends PFComponent {
    private Folder currentScanningFolder;
    private HashMap<FileInfo, FileInfo> remaining;

    /** DirectoryCrawler threads that are idle */
    private List<DirectoryCrawler> directoryCrawlersPool = Collections
        .synchronizedList(new LinkedList<DirectoryCrawler>());
    /** Where crawling DirectoryCrawlers are */
    private List<DirectoryCrawler> activeDirectoryCrawlers = Collections
        .synchronizedList(new LinkedList<DirectoryCrawler>());
    private final static int MAX_CRAWLERS = 3;

    private List<FileInfo> changedFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());
    private List<FileInfo> newFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());
    private List<FileInfo> restoredFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    /** total files in the current scanning folder */
    private int totalFilesCount = 0;
    /**
     * because of multi threading we use a flag to indicate a failed besisdes
     * returnrng false
     */
    private boolean failure = false;

    public FolderScanner(Controller controller) {
        super(controller);
    }

    /** starts the folder scanner, creates MAX_CRAWLERS number of crawlers */
    public void start() {
        // start directoryCrawlers
        for (int i = 0; i < MAX_CRAWLERS; ++i) {
            DirectoryCrawler directoryCrawler = new DirectoryCrawler();
            (new Thread(directoryCrawler, "DirectoryCrawler #" + i)).start();
            directoryCrawlersPool.add(directoryCrawler);
        }
    }

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

    private boolean abort = false;

    public void setAborted(boolean flag) {
        abort = flag;
    }

    public ScanResult scanFolder(Folder folder) {
        Reject.ifNull(folder, "folder cannot be null");
        if (currentScanningFolder != null) {
            throw new IllegalStateException(
                "Not allowed to start another scan while scanning is in process");
        }
        log().info(
            getController().getMySelf().getNick() + " scan folder: "
                + folder.getName() + " start");
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

        List<FileInfo> moved = tryFindMovements();
        Map<FileInfo, List<String>> problemFiles = tryFindProblems();
        ScanResult result = new ScanResult();
        result.setChangedFiles(changedFiles);
        result.setNewFiles(newFiles);
        result.setDeletedFiles(new ArrayList(remaining.keySet()));
        result.setMovedFiles(moved);
        result.setProblemFiles(problemFiles);
        result.setRestoredFiles(restoredFiles);
        result.setTotalFilesCount(totalFilesCount);
        result.setResultState(ScanResult.ResultState.SCANNED);
        reset();
        log().info(
            getController().getMySelf().getNick() + " scan folder "
                + folder.getName() + " done in "
                + (System.currentTimeMillis() - started));

        return result;
    }

    private void reset() {
        abort = false;
        failure = false;
        changedFiles.clear();
        newFiles.clear();
        restoredFiles.clear();
        currentScanningFolder = null;
        totalFilesCount = 0;
    }

    private Map<FileInfo, List<String>> tryFindProblems() {
        Map<FileInfo, List<String>> returnValue = new HashMap<FileInfo, List<String>>();
        for (FileInfo newFile : newFiles) {
            if (FilenameProblem.hasProblems(newFile.getFilenameOnly())) {
                returnValue.put(newFile, FilenameProblem
                    .describeProblems(newFile.getFilenameOnly()));
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

    private boolean isReady() {
        boolean ready;
        synchronized (directoryCrawlersPool) {
            ready = activeDirectoryCrawlers.size() == 0
                && directoryCrawlersPool.size() == MAX_CRAWLERS;
        }
        return ready;
    }

    private List<FileInfo> tryFindMovements() {
        List<FileInfo> returnValue = new ArrayList<FileInfo>(1);
        for (FileInfo deletedFile : remaining.keySet()) {
            long size = deletedFile.getSize();
            long modificationDate = deletedFile.getModifiedDate().getTime();
            for (FileInfo newFile : newFiles) {
                if (newFile.getSize() == size
                    && newFile.getModifiedDate().getTime() == modificationDate)
                {
                    // posible movement detected
                    log().debug(
                        "Movement from: " + deletedFile + " to: " + newFile);
                    returnValue.add(newFile);
                }
            }
        }
        return returnValue;
    }

    private final boolean scanFile(File fileToScan, String currentDirName) {
        if (!fileToScan.exists()) {
            // hardware no longer available
            return false;
        }
        // this is a incomplete fileinfo just find one fast in the remaining
        // list
        log().verbose(
            "scanFile: " + fileToScan + " curdirname: " + currentDirName);
        totalFilesCount++;
        String filename;
        if (currentDirName.length() == 0) {
            filename = fileToScan.getName();
        } else {
            filename = currentDirName + "/" + fileToScan.getName();
        }
        FileInfo fInfo = new FileInfo(currentScanningFolder.getInfo(), filename);

        FileInfo exists;
        boolean changed = false;
        synchronized (remaining) {
            exists = remaining.remove(fInfo);
        }
        if (exists != null) {// file was known
            if (exists.isDeleted()) {
                // file restored
                synchronized (restoredFiles) {
                    restoredFiles.add(exists);
                }
            } else {
                long modification = fileToScan.lastModified();
                if (exists.getModifiedDate().getTime() < modification) {
                    // disk file = newer

                    changed = true;
                }
                long size = fileToScan.length();
                if (exists.getSize() != size) {
                    // size changed
                    log().debug(
                        getController().getMySelf().getNick()
                            + " size change!: from " + exists.getSize()
                            + " to: " + size);
                    changed = true;
                }
                if (changed) {
                    synchronized (changedFiles) {
                        changedFiles.add(exists);
                    }
                }
            }
        } else {// file is new
            log().verbose(
                "NEW file found: " + fInfo.getName() + " hash: "
                    + fInfo.hashCode());
            FileInfo info = new FileInfo(currentScanningFolder, fileToScan);
            info.setFolder(currentScanningFolder);
            info.setSize(fileToScan.length());
            info.setModifiedInfo(getController().getMySelf().getInfo(),
                new Date(fileToScan.lastModified()));
            synchronized (newFiles) {
                newFiles.add(info);
            }
        }
        return true;
    }

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

    private class DirectoryCrawler implements Runnable {
        private File root;
        private boolean shutdown = false;

        private void scan(File root) {
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

                synchronized (FolderScanner.this) {
                    FolderScanner.this.notify();
                }
            }
        }

        private boolean scanDir(File dirToScan) {
            log().verbose("dirCrawler scandir:" + dirToScan);

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
