package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.RecycleDelete;

/**
 * Recycle Bin, enables a restorable delete on all platforms, by moving files to
 * the "RECYCLE_BIN_FOLDER", until this RecycleBin is emptied.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.12 $
 */
public class RecycleBin extends PFComponent {
    private final static String RECYCLE_BIN_FOLDER = ".recycle";
    private List<FileInfo> allRecycledFiles = new ArrayList<FileInfo>();
    private Set<RecycleBinListener> listeners = new HashSet<RecycleBinListener>();

    public RecycleBin(Controller controller) {
        super(controller);
        allRecycledFiles.addAll(readRecyledFiles());
        log().debug("Created");
    }

    private List<FileInfo> readRecyledFiles() {
        List<FileInfo> recycledFiles = new ArrayList<FileInfo>();
        FolderRepository folderRepo = getController().getFolderRepository();
        Folder[] folders = folderRepo.getFolders();
        for (Folder folder : folders) {
            FileInfo[] fileInfos = folder.getFiles();
            for (FileInfo fileInfo : fileInfos) {
                if (fileInfo.isDeleted()) {
                    if (isInRecycleBin(fileInfo)) {
                        recycledFiles.add(fileInfo);
                    }
                }
            }
        }
        return recycledFiles;
    }

    /**
     * permanently delete file from Recycle Bin (if possible will move to OS
     * Recycle Bin)
     */
    public void delete(FileInfo fileInfo) {
        File recycleDir = getRecycleBinDirectory(fileInfo);
        File fileToDelete = new File(recycleDir, fileInfo.getName());
        if (fileToDelete.exists()) {
            if (RecycleDelete.isSupported()) {
                RecycleDelete.delete(fileToDelete.getAbsolutePath());
            } else {
                if (!fileToDelete.delete()) {
                    log().error(
                        "cannot remove file from recycle bin: " + fileToDelete);
                }
            }
            if (!fileToDelete.exists()) {
                removeFile(fileInfo);
            }
        }
        removeEmptyDirs(recycleDir);
    }

    /** removes empty directories, recurse into subs. */
    private void removeEmptyDirs(File directory) {
        if (directory == null) {
            throw new NullPointerException("removeEmptyDirs: directory is null");
        }
        // no recycle bin dir for this folder
        if (!directory.exists()) {
            return;
        }
        if (!directory.isDirectory()) {
            throw new IllegalStateException("File (" + directory
                + ") should be a directory");
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // recurse into directory
                removeEmptyDirs(file);
            }
        }
        if (directory.exists() && directory.isDirectory()
            && directory.listFiles().length == 0)
        {
            directory.delete();
        }
    }

    private File getRecycleBinDirectory(FileInfo fileInfo) {
        FolderRepository repo = getController().getFolderRepository();
        Folder folder = repo.getFolder(fileInfo.getFolderInfo());
        return getRecycleBinDirectory(folder);
    }

    private File getRecycleBinDirectory(Folder folder) {
        File folderBaseDir = folder.getLocalBase();
        return new File(folderBaseDir, RECYCLE_BIN_FOLDER);
    }

    public boolean isInRecycleBin(FileInfo fileInfo) {
        if (!fileInfo.isDeleted()) {
            throw new IllegalArgumentException(
                "isInRecycleBin: fileInfo should be deleted: " + fileInfo);
        }
        File recycleBinDir = getRecycleBinDirectory(fileInfo);
        File target = new File(recycleBinDir, fileInfo.getName());
        return target.exists();
    }

    public List<FileInfo> getAllRecycledFiles() {
        return new ArrayList<FileInfo>(allRecycledFiles);
    }
    
    public int countAllRecycledFiles() {
        return allRecycledFiles.size();
    }

    public int getSize() {
        return allRecycledFiles.size();
    }

    private void addFile(FileInfo file) {
        allRecycledFiles.add(file);
        fireFileAdded(file);
    }

    private void removeFile(FileInfo file) {
        allRecycledFiles.remove(file);
        fireFileRemoved(file);
    }

    private void removeFiles(List<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            removeFile(fileInfo);
        }
    }

    boolean isRecycleBin(Folder folder, File file) {
        return file.equals(getRecycleBinDirectory(folder));
    }

    public void emptyRecycleBin() {
        FolderRepository repo = getController().getFolderRepository();
        Folder[] folders = repo.getFolders();
        for (Folder folder : folders) {
            File recycleBinDir = getRecycleBinDirectory(folder);
            List<FileInfo> toRemove = new ArrayList<FileInfo>();
            FileInfo[] fileInfos = folder.getFiles();
            for (FileInfo fileInfo : fileInfos) {
                if (fileInfo.isDeleted()) {
                    File fileToRemove = new File(recycleBinDir, fileInfo
                        .getName());
                    if (fileToRemove.exists()) {
                        if (RecycleDelete.isSupported()) {
                            RecycleDelete
                                .delete(fileToRemove.getAbsolutePath());
                        } else {
                            if (!fileToRemove.delete()) {
                                log().error(
                                    "cannot remove file from recycle bin: "
                                        + fileToRemove);
                            }
                        }
                    }
                    if (!fileToRemove.exists()) {
                        toRemove.add(fileInfo);
                    }
                }
            }
            if (toRemove.size() > 0) {
                removeFiles(toRemove);
            }

            if (recycleBinDir.exists()) {
                if (!recycleBinDir.isDirectory()) {
                    log().error("recycle bin is not a directory!");
                    continue;
                }
                File[] files = recycleBinDir.listFiles();
                for (File fileToRemove : files) {
                    if (RecycleDelete.isSupported()) {
                        RecycleDelete.delete(fileToRemove.getAbsolutePath());
                    } else {
                        if (!fileToRemove.delete()) {
                            log().error(
                                "cannot remove file from recycle bin: "
                                    + fileToRemove);
                        }
                    }
                }
            }
            removeEmptyDirs(recycleBinDir);
        }

    }

    /** @return true if succeded */
    public boolean moveToRecycleBin(FileInfo fileInfo, File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(
                "moveToRecycleBin: file does not exists: " + file);
        }

        File recycleBinDir = getRecycleBinDirectory(fileInfo);
        if (!recycleBinDir.exists()) {
            if (!recycleBinDir.mkdir()) {
                log().error(
                    "moveToRecycleBin: cannot create recycle bin: "
                        + recycleBinDir);
                return false;
            }
            // Make recycle bin system/hidden
            Util.setAttributesOnWindows(recycleBinDir, true, true);
        }

        File target = new File(recycleBinDir, fileInfo.getName());
        File parent = new File(target.getParent());
        if (!parent.equals(recycleBinDir)) {
            if (!parent.exists() && !parent.mkdirs()) {
                log().error(
                    "moveToRecycleBin: cannot create recycle bin directorty structure for: "
                        + target);
                return false;
            }
        }
        if (!file.renameTo(target)) {
            log().warn(
                "moveToRecycleBin: cannot rename file to recycle bin: "
                    + target);
            try {
                Util.copyFile(file, target);
            } catch (IOException ioe) {
                log().error(
                    "moveToRecycleBin: cannot copy to recycle bin: " + target
                        + "\n" + ioe.getMessage());
                return false;
            }
            if (!file.delete()) {
                log().error(
                    "moveToRecycleBin: cannot delete file after copy to recycle bin: "
                        + file);
                return false;
            }
        }
        // checks to validate code
        if (file.exists()) {
            log().error("moveToRecycleBin: source not deleted?: " + file);
            return false;
        }
        if (!target.exists()) {
            log().error("moveToRecycleBin: target not created?: " + target);
            return false;
        }
        addFile(fileInfo);
        return true;
    }

    /**
     * restore this file to the Folder from the PowerFolder Recycle Bin
     * 
     * @return succes (true) or failure (false)
     */
    public boolean restoreFromRecycleBin(FileInfo fileInfo) {
        if (!isInRecycleBin(fileInfo)) {
            throw new IllegalArgumentException(
                "restoreFromRecycleBin: fileInfo should be in recyclebin: "
                    + fileInfo);
        }
        File recycleBinDir = getRecycleBinDirectory(fileInfo);
        FolderRepository repo = getController().getFolderRepository();
        Folder folder = repo.getFolder(fileInfo.getFolderInfo());
        File folderBaseDir = folder.getLocalBase();

        File source = new File(recycleBinDir, fileInfo.getName());
        File target = new File(folderBaseDir, fileInfo.getName());

        if (target.exists()) {
            throw new IllegalArgumentException(
                "restoreFromRecycleBin: target should not exists in folder: "
                    + target);
        }
        File parent = new File(target.getParent());
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                log().error(
                    "restoreFromRecycleBin: cannot create directorty structure for: "
                        + target);
                return false;
            }
        }

        if (!source.renameTo(target)) {
            log().warn(
                "restoreFromRecycleBin: cannot rename file from recycle bin to: "
                    + target);
            try {
                Util.copyFile(source, target);
            } catch (IOException ioe) {
                log().error(
                    "restoreFromRecycleBin: cannot copy from recycle bin to: "
                        + target + "\n" + ioe.getMessage());
                return false;
            }
            if (!source.delete()) {
                log().error(
                    "restoreFromRecycleBin: cannot delete file after copy from recycle bin: "
                        + source);
                return false;
            }
        }
        // checks to validate code
        if (source.exists()) {
            log()
                .error("restoreFromRecycleBin: source not deleted?: " + source);
            return false;
        }
        if (!target.exists()) {
            log()
                .error("restoreFromRecycleBin: target not created?: " + target);
            return false;
        }
        // Let folder scan the restored file
        // This updated internal version numbers and broadcasts changes to
        // remote users
        folder.scanRestoredFile(new FileInfo(folder, target));
        // fileInfo.setDeleted(true);
        removeFile(fileInfo);
        removeEmptyDirs(recycleBinDir);
        return true;
    }

    // ***********************events

    private void fireFileAdded(FileInfo file) {
        for (RecycleBinListener listener : listeners) {
            listener.fileAdded(new RecycleBinEvent(this, file));
        }
    }

    private void fireFileRemoved(FileInfo file) {
        for (RecycleBinListener listener : listeners) {
            listener.fileRemoved(new RecycleBinEvent(this, file));
        }
    }

    public void addRecycleBinListener(RecycleBinListener listener) {
        listeners.add(listener);
    }

    public void removeRecycleBinListener(RecycleBinListener listener) {
        listeners.remove(listener);
    }

}
