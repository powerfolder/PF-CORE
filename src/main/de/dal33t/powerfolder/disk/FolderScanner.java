package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;

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

    private ScanResult result;
    private int totalFilesCount = 0;

    public FolderScanner(Controller controller) {
        super(controller);
    }

    public void start() {
        // start directoryCrawlers
        for (int i = 0; i < MAX_CRAWLERS; ++i) {
            DirectoryCrawler directoryCrawler = new DirectoryCrawler();
            (new Thread(directoryCrawler, "DirectoryCrawler #" + i)).start();
            directoryCrawlersPool.add(directoryCrawler);
        }
    }

    public void shutdown() {
        // TODO: shutdown crawlers
    }

    public ScanResult scanFolder(Folder folder) {
        Reject.ifNull(folder, "folder cannot be null");
        log().info(
            getController().getMySelf().getNick() + " scan folder: "
                + folder.getName() + " start");
        long started = System.currentTimeMillis();

        currentScanningFolder = folder;

        File base = currentScanningFolder.getLocalBase();
        remaining = currentScanningFolder.getKnownFiles();
        scan(base);

        List<FileInfo> moved = tryFindMovements();
        Map<FileInfo, List<String>> problemFiles = tryFindProblems();
        result = new ScanResult();
        result.setChangedFiles(changedFiles);
        result.setNewFiles(newFiles);
        result.setDeletedFiles(new ArrayList(remaining.keySet()));
        result.setMovedFiles(moved);
        result.setProblemFiles(problemFiles);
        result.setRestoredFiles(restoredFiles);
        result.setTotalFilesCount(totalFilesCount);
        changedFiles.clear();
        newFiles.clear();       
        restoredFiles.clear();
        currentScanningFolder = null;
        totalFilesCount = 0;
        log().info(
            getController().getMySelf().getNick() + " scan folder "
                + folder.getName() + " done in "
                + (System.currentTimeMillis() - started));

        return result;
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

    /** root */
    private void scan(File folderBase) {
        File[] filelist = folderBase.listFiles();
        // add root to directories

        for (File file : filelist) {
            if (currentScanningFolder.isSystemSubDir(file)) {
                continue;
            }
            if (file.isDirectory()) {
                while (directoryCrawlersPool.isEmpty()) {
                    synchronized (this) {
                        try {
                            wait();
                            log().debug("wakeup 2");
                        } catch (InterruptedException e) {

                        }
                    }
                }
                synchronized (directoryCrawlersPool) {
                    DirectoryCrawler crawler = directoryCrawlersPool.remove(0);
                    activeDirectoryCrawlers.add(crawler);
                    crawler.scan(file);
                }

            } else { // the files in the root
                // ignore incomplete (downloading) files
                if (!FileUtils.isTempDownloadFile(file)) {
                    scanFile(file, "");
                }
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

    private final void scanFile(File fileToScan, String currentDirName) {
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

        private void scan(File root) {
            this.root = root;
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
                        } catch (InterruptedException e) {

                        }
                    }
                }
                scanDir(root);
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

        private void scanDir(File dirToScan) {
            log().verbose("dirCrawler scandir:" + dirToScan);
            if (dirToScan != null) {
                String currentDirName = getCurrentDirName(
                    currentScanningFolder, dirToScan);
                for (File subFile : dirToScan.listFiles()) {
                    if (subFile.isDirectory()) {
                        scanDir(subFile);
                    } else if (subFile.isFile()) {
                        if (!FileUtils.isTempDownloadFile(subFile)) {
                            scanFile(subFile, currentDirName);
                        }
                    }
                }
            }
        }
    }
}
