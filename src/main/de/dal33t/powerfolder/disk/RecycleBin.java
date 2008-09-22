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
package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.RecycleBinConfirmEvent;
import de.dal33t.powerfolder.event.RecycleBinConfirmationHandler;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.ProgressListener;
import de.dal33t.powerfolder.util.os.RecycleDelete;

/**
 * Recycle Bin, implements a restorable delete on all platforms, by moving files
 * to the "RECYCLE_BIN_FOLDER", until this RecycleBin is emptied.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.12 $
 */
public class RecycleBin extends PFComponent {
    /** The recycle bin folder name */
    private static final String RECYCLE_BIN_FOLDER = ".recycle";
    /** all recycled files */
    // TODO: Check if this can be removed. Large datastructure. uses loza mem.
    private List<FileInfo> allRecycledFiles = new ArrayList<FileInfo>();
    /** all listeners to this recycle bin */
    private RecycleBinListener listeners = (RecycleBinListener) ListenerSupportFactory
        .createListenerSupport(RecycleBinListener.class);

    private RecycleBinConfirmationHandler recycleBinConfirmationHandler;

    /** create a recycle bin with its associated controller */
    public RecycleBin(Controller controller) {
        super(controller);
        moveFolders();
        allRecycledFiles.addAll(readRecyledFiles());
        logInfo("Recycle bin initialized, " + allRecycledFiles.size());
        logFine(allRecycledFiles.size() + " files in recycle bin");
    }

    /** Move the old recycle bin to the new location */
    private void moveFolders() {
        FolderRepository folderRepo = getController().getFolderRepository();
        Folder[] folders = folderRepo.getFolders();
        for (Folder folder : folders) {
            File oldRecycleBinDir = getOldRecycleBinDirectory(folder);
            File recycleBinDir = getRecycleBinDirectory(folder);
            if (oldRecycleBinDir.exists() && !recycleBinDir.exists()) {
                if (oldRecycleBinDir.renameTo(recycleBinDir)) {
                    logFine(oldRecycleBinDir + " renamed to " + recycleBinDir);
                } else {
                    logFine("failed to rename " + oldRecycleBinDir + " to "
                        + recycleBinDir);
                }
            }
        }
    }

    /**
     * creates a list of all files that are in the recycle bin. iterates over
     * each folder and each file that is in the powerfolder data base and if
     * that file is marked deleted and exsits in the recycle bin it is added to
     * this list.
     */
    private List<FileInfo> readRecyledFiles() {
        List<FileInfo> recycledFiles = new ArrayList<FileInfo>();
        FolderRepository folderRepo = getController().getFolderRepository();
        for (Folder folder : folderRepo.getFoldersAsCollection()) {
            Collection<FileInfo> fileInfos = folder.getKnownFiles();
            for (FileInfo fileInfo : fileInfos) {
                if (isInRecycleBin(fileInfo)) {
                    recycledFiles.add(fileInfo);
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
                    logSevere("cannot remove file from recycle bin: "
                        + fileToDelete);
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
            if (!directory.delete()) {
                logSevere("Failed to delete: " + directory.getAbsolutePath());
            }
        }
    }

    /**
     * @return the File object with an absolute path to the recycle bin
     *         directory in the file system for this fileInfo
     * @param fileInfo
     *            the fileInfo to get the recyclebin dir for
     */
    private File getRecycleBinDirectory(FileInfo fileInfo) {
        FolderRepository repo = getController().getFolderRepository();
        Folder folder = repo.getFolder(fileInfo.getFolderInfo());
        if (folder == null) {
            return null;
        }
        return getRecycleBinDirectory(folder);
    }

    /**
     * @return the File object with an abosulute path to the recycle bin
     *         directory in the file system for this folder
     * @param folder
     *            the folder to get the recyclebin dir for
     */
    private static File getRecycleBinDirectory(Folder folder) {
        File folderBaseDir = folder.getSystemSubDir();
        return new File(folderBaseDir, RECYCLE_BIN_FOLDER);
    }

    /**
     * @return the File object with an absolute path to the recycle bin
     *         directory in the file system for this folder
     * @param folder
     *            the folder to get the recyclebin dir for
     */
    private static File getOldRecycleBinDirectory(Folder folder) {
        File folderBaseDir = folder.getLocalBase();
        return new File(folderBaseDir, RECYCLE_BIN_FOLDER);
    }

    /** @retrun is this fileInfo in the powerfolder recycle bin */
    public boolean isInRecycleBin(FileInfo fileInfo) {
        File recycleBinDir = getRecycleBinDirectory(fileInfo);
        if (recycleBinDir == null) {
            // no longer on folder
            return false;
        }
        File target = new File(recycleBinDir, fileInfo.getName());
        return target.exists();
    }

    /** @return a copy of the list of all files in the powerfolder recycle bin */
    public List<FileInfo> getAllRecycledFiles() {
        return new ArrayList<FileInfo>(allRecycledFiles);
    }

    /** @return the number of files in the powerfolder recycle bin */
    public int countAllRecycledFiles() {
        return allRecycledFiles.size();
    }

    /**
     * adds a file to the list of recycled files and fires fileAdded event, if
     * file with that name is alredy tere it will fire a fileUpdate event.
     */
    private void addFile(FileInfo file) {
        if (allRecycledFiles.contains(file)) {
            fileUpdated(file);
        } else {
            allRecycledFiles.add(file);
            fireFileAdded(file);
        }
    }

    /**
     * removes a file from the list of recycled files and fires fileRemoved
     * event
     */
    private void removeFile(FileInfo file) {
        allRecycledFiles.remove(file);
        fireFileRemoved(file);
    }

    /** removes a list of FileInfos from the list of recycled files and fires */
    private void removeFiles(List<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            removeFile(fileInfo);
        }
    }

    /**
     * called from Folder package protected
     * 
     * @return true if this file if the powerfolder recyclebin folder for this
     *         Folder
     */
    public static boolean isRecycleBin(Folder folder, File file) {
        return file.equals(getRecycleBinDirectory(folder));
    }

    /**
     * removes all files from the powerfolder recyclebin. Only files that are
     * marked deleted in the powerfolder data base of a folder and if the file
     * is in the PowerFolder Recycle bin are deleted. If we support the OS
     * recycle bin like on windows the files are moved there.
     * 
     * @param progressListener
     *            the optional progress listener.
     */
    public void emptyRecycleBin(ProgressListener progressListener) {
        FolderRepository repo = getController().getFolderRepository();
        Folder[] folders = repo.getFolders();
        int numberOfFolder = folders.length;
        int folderIndex = 0;
        for (Folder folder : folders) {

            // First of four stages for this folder.
            // Find files to delete.
            if (progressListener != null) {
                progressListener
                    .progressReached((int) (100.0 * folderIndex / numberOfFolder));
            }

            File recycleBinDir = getRecycleBinDirectory(folder);
            List<FileInfo> toRemove = new ArrayList<FileInfo>();
            Collection<FileInfo> fileInfos = folder.getKnownFiles();
            for (FileInfo fileInfo : fileInfos) {
                File fileToRemove = new File(recycleBinDir, fileInfo.getName());
                if (fileToRemove.exists()) {
                    if (RecycleDelete.isSupported()) {
                        RecycleDelete.delete(fileToRemove.getAbsolutePath());
                    } else {
                        if (!fileToRemove.delete()) {
                            logSevere("cannot remove file from recycle bin: "
                                + fileToRemove);
                        }
                    }
                }
                if (!fileToRemove.exists()) {
                    toRemove.add(fileInfo);
                }
            }

            // Second of four stages for this folder.
            // Remove files.
            if (progressListener != null) {
                progressListener
                    .progressReached((int) (100.0 * ((double) folderIndex + 0.25) / numberOfFolder));
            }

            if (!toRemove.isEmpty()) {
                removeFiles(toRemove);
            }

            // Third of four stages for this folder.
            // Delete from recycle bin.
            if (progressListener != null) {
                progressListener
                    .progressReached((int) (100.0 * ((double) folderIndex + 0.5) / numberOfFolder));
            }
            if (recycleBinDir.exists()) {
                if (!recycleBinDir.isDirectory()) {
                    logSevere("recycle bin is not a directory!");
                    continue;
                }
                File[] files = recycleBinDir.listFiles();
                for (File fileToRemove : files) {
                    if (RecycleDelete.isSupported()) {
                        RecycleDelete.delete(fileToRemove.getAbsolutePath());
                    } else {
                        if (!fileToRemove.delete()) {
                            logSevere("cannot remove file from recycle bin: "
                                + fileToRemove);
                        }
                    }
                }
            }

            // Fourth of four stages for this folder.
            // Remove empty directories.
            if (progressListener != null) {
                progressListener
                    .progressReached((int) (100.0 * ((double) folderIndex + 0.75) / numberOfFolder));
            }
            removeEmptyDirs(recycleBinDir);

            folderIndex++;
        }

    }

    /**
     * Moves the file to the PowerFolder Recycle Bin.
     * 
     * @return true if succeded
     */
    public boolean moveToRecycleBin(FileInfo fileInfo, File file) {
        if (!file.exists()) {
            logSevere("moveToRecycleBin: source file does not exists: " + file);
        }

        File recycleBinDir = getRecycleBinDirectory(fileInfo);
        if (!recycleBinDir.exists()) {
            if (!recycleBinDir.mkdir()) {
                logSevere("moveToRecycleBin: cannot create recycle bin: "
                    + recycleBinDir);
                return false;
            }
            // Make recycle bin system/hidden
            FileUtils.setAttributesOnWindows(recycleBinDir, true, true);
        }

        File target = new File(recycleBinDir, fileInfo.getName());
        File parent = new File(target.getParent());
        if (!parent.equals(recycleBinDir)) {
            if (!parent.exists() && !parent.mkdirs()) {
                logSevere("moveToRecycleBin: cannot create recycle bin directorty structure for: "
                    + target);
                return false;
            }
        }
        if (target.exists()) {
            // file allready in recycle bin
            if (RecycleDelete.isSupported()) {
                RecycleDelete.delete(target.getAbsolutePath());
            }
            if (target.exists()) {
                if (!target.delete()) {
                    logSevere("Failed to delete: " + target.getAbsolutePath());
                }
            }
        }
        if (!file.renameTo(target)) {
            logWarning("moveToRecycleBin: cannot rename file to recycle bin: "
                + target);
            try {
                FileUtils.copyFile(file, target);
            } catch (IOException ioe) {
                logSevere("moveToRecycleBin: cannot copy to recycle bin: "
                    + target + '\n' + ioe.getMessage());
                return false;
            }
            if (!file.delete()) {
                logSevere("moveToRecycleBin: cannot delete file after copy to recycle bin: "
                    + file);
                return false;
            }
        }
        // checks to validate code
        if (file.exists()) {
            logSevere("moveToRecycleBin: source not deleted?: " + file);
            return false;
        }
        if (!target.exists()) {
            logSevere("moveToRecycleBin: target not created?: " + target);
            return false;
        }
        addFile(fileInfo);
        return true;
    }

    /**
     * returns a File object pointing to the fysical file on disk in the recycle
     * bin
     */
    public File getDiskFile(FileInfo fileInfo) {
        if (!isInRecycleBin(fileInfo)) {
            throw new IllegalArgumentException(
                "getDiskFile: fileInfo should be in recyclebin: " + fileInfo);
        }
        File recycleBinDir = getRecycleBinDirectory(fileInfo);
        return new File(recycleBinDir, fileInfo.getName());
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
            if (!isOverWriteAllowed(source, target)) {
                // not allowed to overwrite skip
                return false;
            }
            // else we are allowed to overwrite
            if (RecycleDelete.isSupported()) {
                RecycleDelete.delete(target.getAbsolutePath());
            }
            if (target.exists()) {
                if (!target.delete()) {
                    logSevere("Failed to delete: " + target.getAbsolutePath());
                }
            }
        }
        File parent = new File(target.getParent());
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                logSevere("restoreFromRecycleBin: cannot create directorty structure for: "
                    + target);
                return false;
            }
        }

        if (!source.renameTo(target)) {
            logWarning("restoreFromRecycleBin: cannot rename file from recycle bin to: "
                + target);
            try {
                FileUtils.copyFile(source, target);
            } catch (IOException ioe) {
                logSevere("restoreFromRecycleBin: cannot copy from recycle bin to: "
                    + target + '\n' + ioe.getMessage());
                return false;
            }
            if (!source.delete()) {
                logSevere("restoreFromRecycleBin: cannot delete file after copy from recycle bin: "
                    + source);
                return false;
            }
        }
        // checks to validate code
        if (source.exists()) {
            logSevere("restoreFromRecycleBin: source not deleted?: " + source);
            return false;
        }
        if (!target.exists()) {
            logSevere("restoreFromRecycleBin: target not created?: " + target);
            return false;
        }
        // Let folder scan the restored file
        // This updated internal version numbers and broadcasts changes to
        // remote users
        folder.scanRestoredFile(new FileInfo(folder, target));
        removeFile(fileInfo);
        removeEmptyDirs(recycleBinDir);
        return true;
    }

    private boolean isOverWriteAllowed(File source, File target) {
        if (recycleBinConfirmationHandler == null) {
            throw new IllegalStateException(
                "recycleBinConfirmationHandler should be set");
        }
        return recycleBinConfirmationHandler
            .confirmOverwriteOnRestore(new RecycleBinConfirmEvent(this, source,
                target));
    }

    // confrim handler
    public void setRecycleBinConfirmationHandler(
        RecycleBinConfirmationHandler recycleBinConfirmationHandler)
    {
        this.recycleBinConfirmationHandler = recycleBinConfirmationHandler;
    }

    // ***********************events

    /** fires fireFileAdded to all listeners */
    private void fireFileAdded(FileInfo file) {
        listeners.fileAdded(new RecycleBinEvent(this, file));
    }

    /** fires fireFileRemoved to all listeners */
    private void fireFileRemoved(FileInfo file) {
        listeners.fileRemoved(new RecycleBinEvent(this, file));
    }

    /** fires fireFileAdded to all listeners */
    private void fileUpdated(FileInfo file) {
        listeners.fileUpdated(new RecycleBinEvent(this, file));
    }

    /** register to receive recycle bin events */
    public void addRecycleBinListener(RecycleBinListener listener) {
        ListenerSupportFactory.addListener(listeners, listener);
    }

    /** remove listener */
    public void removeRecycleBinListener(RecycleBinListener listener) {
        ListenerSupportFactory.removeListener(listeners, listener);
    }
}