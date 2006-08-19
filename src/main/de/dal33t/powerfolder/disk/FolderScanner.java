package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.FileUtils;

public class FolderScanner extends PFComponent implements Runnable {
    private List<Folder> foldersToScan = Collections
        .synchronizedList(new LinkedList<Folder>());
    private Folder currentScanningFolder;
    private HashMap<FileInfo, FileInfo> remaining;
    private boolean stop;
    private boolean scanning;
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

    private ScanResult result;
    private int totalFilesCount = 0;

    public FolderScanner(Controller controller) {
        super(controller);
        // start directoryCrawlers
        for (int i = 0; i < MAX_CRAWLERS; ++i) {
            DirectoryCrawler directoryCrawler = new DirectoryCrawler();
            (new Thread(directoryCrawler, "DirectoryCrawler #" + i)).start();
            directoryCrawlersPool.add(directoryCrawler);
        }
    }

    public ScanResult getResult() {
        return result;
    }

    public void scan(Folder folder, boolean manual) {
        // TODO / Ideas:
        // FolderScanner should only have ONE folder to be scanned at a time,
        // no queue. If this method gets called while scanning process is
        // running throw a IllegalStateException. A new scan should only be
        // startable swhen
        // 1. the former scan was finished or 2. the former scan was canceled

        synchronized (foldersToScan) {
            if (currentScanningFolder == folder) {
                // weare already scanning this folder ->skipp
                return;
            }
            if (foldersToScan.contains(folder)) {
                if (manual) { // move to front of the queue if manual/forced
                    foldersToScan.remove(folder);
                    foldersToScan.add(0, folder);
                } else {
                    // skipp
                    return;
                }
            }
            // folder not in queue yet:
            if (manual) {
                foldersToScan.add(0, folder);
            } else {
                foldersToScan.add(folder);
            }
        }
        synchronized (this) {
            notify();
        }
    }

    private void startScan() {
        log().info(
            getController().getMySelf().getNick()
                + "-------FolderScanner start-----------");
        long started = System.currentTimeMillis();
        if (currentScanningFolder != null) {
            throw new IllegalStateException(
                "don't want 2 scan 2 Folders concurrent");
        }

        synchronized (foldersToScan) {
            if (foldersToScan.size() > 0) {
                currentScanningFolder = foldersToScan.remove(0);
                remaining = currentScanningFolder.getKnownFiles();
            }
        }
        if (currentScanningFolder != null) {
            File base = currentScanningFolder.getLocalBase();
            scan(base);
        }

        log().verbose("Scan took: " + (System.currentTimeMillis() - started));
        log().verbose("new files:" + newFiles.size());
        List<FileInfo> moved = tryFindMovements();
        Map<FileInfo, List<String>> problemFiles = tryFindProblems();
        result = new ScanResult();
        result.setChangedFiles(changedFiles);
        result.setNewFiles(newFiles);
        result.setDeletedFiles(new ArrayList(remaining.keySet()));
        result.setMovedFiles(moved);
        result.setProblemFiles(problemFiles);
        result.setTotalFilesCount(totalFilesCount);
        currentScanningFolder.scanned(result);
        newFiles.clear();
        changedFiles.clear();        
        currentScanningFolder = null;
        scanning = false;

        synchronized (this) {
            notify(); // wake up to find a new folder in the queue for
                        // scanning
        }

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

    public void run() {
        while (true) {
            if (foldersToScan.size() == 0) {
                try {
                    synchronized (this) {
                        wait();
                        if (scanning) { // not for this part
                            notify();
                        }
                    }
                    if (stop) {
                        break;
                    }
                } catch (InterruptedException e) {
                    /* should not happen */
                    continue;
                }
            }
            if (!scanning && currentScanningFolder == null) { // new scan
                // scanning one folder at the time
                startScan();
            }

        }
    }

    public boolean isScanning() {
        return scanning;
    }

    public void shutdown() {
        stop = true;
        synchronized (this) {
            notify();
        }
        // TODO: shutdown crawlers
    }

    /** root */
    void scan(File folderBase) {
        scanning = true;
        File[] filelist = folderBase.listFiles();

        for (File file : filelist) {
            if (currentScanningFolder.isSystemSubDir(file)) {
                continue;
            }
            if (file.isDirectory()) {
                while (directoryCrawlersPool.isEmpty()) {
                    synchronized (this) {
                        try {
                            wait();
                            if (!scanning) { // not for us
                                notify();
                            }
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
            // wait for completion
            synchronized (this) {
                notify();
            }
            try {
                synchronized (this) {
                    // wait(100);
                    wait();

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log().info(
            getController().getMySelf().getNick()
                + "----- FolderScanner ready--------");
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
                
                exists.setModifiedInfo(getController().getMySelf().getInfo(),
                    new Date(fileToScan.lastModified()));
                exists.setSize(fileToScan.length());
                exists.setDeleted(false);
                changed = true;
            } else {
                long modification = fileToScan.lastModified();
                if (exists.getModifiedDate().getTime() < modification) {
                    // disk file = newer
                    MemberInfo myself = getController().getMySelf().getInfo();
                    exists.setModifiedInfo(myself, new Date(modification));
                    changed = true;
                }
                long size = fileToScan.length();
                if (exists.getSize() != size) {
                    // size changed
                    log().debug(getController().getMySelf().getNick() + " size change!: from " + exists.getSize() + " to: " + size);
                    exists.setSize(size);
                    changed = true;
                    
                }
                if (changed) {
                    exists.setVersion(exists.getVersion() + 1);
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
