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
    private static List<DirectoryCrawler> directoryCrawlersPool = Collections
        .synchronizedList(new LinkedList<DirectoryCrawler>());
    /** Where crawling DirectoryCrawlers are */
    private static List<DirectoryCrawler> activeDirectoryCrawlers = Collections
        .synchronizedList(new LinkedList<DirectoryCrawler>());
    private final static int MAX_CRAWLERS = 3;

    private List<FileInfo> changedFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());
    private List<FileInfo> newFiles = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    public FolderScanner(Controller controller) {
        super(controller);
        // start directoryCrawlers
        for (int i = 0; i < MAX_CRAWLERS; ++i) {
            DirectoryCrawler directoryCrawler = new DirectoryCrawler();
            (new Thread(directoryCrawler, "DirectoryCrawler #" + i)).start();
            directoryCrawlersPool.add(directoryCrawler);
        }
    }

    public void scan(Folder folder) {
        synchronized (foldersToScan) {
            foldersToScan.add(folder);
        }
        synchronized (this) {
            notify();
        }
    }

    private void startScan() {
        System.out.println("Scan start");
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
        System.out.println("Scan took: "
            + (System.currentTimeMillis() - started));
        tryFindMovements();
        ScanResult result = new ScanResult();
        result.setChangedFiles(changedFiles);
        result.setNewFiles(newFiles);
        result.setDeletedFiles(new ArrayList(remaining.keySet()));
        scanning = false;
        System.exit(0);
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
            if (file.isDirectory()
                && !getController().getRecycleBin().isRecycleBin(
                    currentScanningFolder, file))
            {
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
                // ignore our database file or incomplete (downloading)
                // files
                if (!file.getName().equals(Folder.DB_FILENAME)
                    && !file.getName().equals(Folder.DB_BACKUP_FILENAME)
                    && !FileUtils.isTempDownloadFile(file))
                {
                    scanFile(file, "");
                }
            }
        }
        
    }

    private void tryFindMovements() {
        
        System.out.println("deleted: " + remaining.size());
        System.out.println(remaining);
        System.out.println("newFiles: " + newFiles.size());
        System.out.println(newFiles);
        
        for (FileInfo deletedFile : remaining.keySet()) {
            long size = deletedFile.getSize();
            long modificationDate = deletedFile.getModifiedDate().getTime();
            for (FileInfo newFile : newFiles) {
                if (newFile.getSize() == size
                    && newFile.getModifiedDate().getTime() == modificationDate)
                {
                    //posible movement detected
                    log().debug("From: " +  deletedFile + " to: " + newFile);
                }
            }
        }
    }

    private final void scanFile(File fileToScan, String currentDirName) {
        // this is a incomplete fileinfo just find one fast in the remaining
        // list
        String filename;
        if (currentDirName.length() == 0){
            filename = fileToScan.getName();
        } else {
            filename = currentDirName + "/" + fileToScan.getName();
        }
        FileInfo fInfo = new FileInfo(currentScanningFolder.getInfo(),
            filename);
        
        FileInfo exists;
        boolean changed = false;
        synchronized (remaining) {            
            exists = remaining.remove(fInfo);
        }
        if (exists != null) {// file was known            
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
                exists.setSize(size);
                changed = true;
            }
            if (changed) {
                synchronized (changedFiles) {
                    changedFiles.add(exists);
                }
            }
        } else {// file is new
            //System.out.println("NEW " + fInfo.getName());
            FileInfo info = new FileInfo(currentScanningFolder, fileToScan);
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
            // System.out.println("dirCrawler scandir:" + dirToScan);
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
