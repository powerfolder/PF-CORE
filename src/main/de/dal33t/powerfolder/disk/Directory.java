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

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Represents a directory of files. No actual disk access from this file, build
 * from list of FileInfos Holds the SubDirectories (may contain Files and
 * Subdirectories themselfs) and Files (FileInfos)
 * 
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom </a>
 * @version $Revision: 1.43 $
 */
public class Directory implements Comparable<Directory>, DiskItem {
    /**
     * The files (FileInfoHolder s) in this Directory key = fileInfo value =
     * FileInfoHolder
     */
    // TODO This map comsumes a LOT memory.
    private Map<FileInfo, FileInfoHolder> fileInfoHolderMap = new ConcurrentHashMap<FileInfo, FileInfoHolder>(
        2, 0.75f, 4);
    /** key = dir name, value = Directory* */
    private Map<String, Directory> subDirectoriesMap = new ConcurrentHashMap<String, Directory>(
        2, 0.75f, 4);
    /**
     * the path to this directory (including its name, excluding the localbase
     * (see Folder)
     */
    private String path;
    /** the name of this directory */
    private String name;
    /** The root Folder which this Directory belongs to */
    private Folder rootFolder;
    /** The parent Directory (may be null, if no parent!) */
    private Directory parent;
    private static final Logger log = Logger.getLogger(Directory.class.getName());

    /**
     * @param name
     *            The name of this folder
     * @param path
     *            The path to this folder
     */
    public Directory(Directory parent, String name, String path, Folder rootFolder) {
        this.parent = parent;
        this.name = name;
        this.path = path;
        this.rootFolder = rootFolder;
    }

    private static DataFlavor dataFlavor;

    /**
     * Data flavor for Drag and Drop. We use this to find the source of the
     * drag. If drag and drop is within the same folder we MOVE files, else COPY
     * of files. Do not use for getting files Use DataFlavor.javaFileListFlavor
     * for that.
     */
    public static DataFlavor getDataFlavor() {
        if (dataFlavor == null) {
            try {
                dataFlavor = new DataFlavor(
                    DataFlavor.javaJVMLocalObjectMimeType + ";class="
                        + Directory.class.getName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException();
            }
        }
        return dataFlavor;
    }

    public boolean isRetained() {
        return rootFolder.getDiskItemFilter().isRetained(this);
    }

    public boolean isFolderWhitelist() {
        return rootFolder.isWhitelist();
    }

    /** returns a File object to the diretory in the filesystem */
    public File getFile() {
        return new File(rootFolder.getLocalBase(), path);
    }

    /** returns the Directory with this name, creates it if not exists yet */
    public Directory getCreateSubDirectory(String nameArg) {
        if (subDirectoriesMap.containsKey(nameArg)) {
            return subDirectoriesMap.get(nameArg);
        }
        Directory sub = new Directory(this, nameArg, path + '/' + nameArg,
            rootFolder);

        File newFileName = new File(getFile(), nameArg);
        if (!newFileName.exists()) {
            if (!newFileName.mkdir()) {
                log.info("Failed to create " + newFileName.getAbsolutePath());
            }
        }
        return sub;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Directory getParentDirectory() {
        return parent;
    }

    /**
     * notify this Directory that a file is added
     * 
     * @param file
     */
    public void add(File file) {
        FileInfo fileInfo = new FileInfo(rootFolder, file);
        rootFolder.scanNewFile(fileInfo);
    }

    /**
     * move a file from this source to this Directory, overwrites target if
     * exisits!
     * 
     * @param file
     * @return the file has been moved
     */
    public boolean moveFileFrom(File file) {
        Reject.ifNull(file, "file cannot be null");
        if (!file.exists()) {
            throw new IllegalStateException("File must exists");
        }

        File newFile = new File(getFile(), file.getName());
        try {
            if (file.getCanonicalPath().equals(newFile.getCanonicalPath())) {
                throw new IllegalStateException("cannot copy onto itself");
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "IOException", e);
            return false;
        }
        File tmpFile = null;
        if (newFile.exists()) {
            // target exists, rename it so we backup
            tmpFile = new File(newFile + ".tmp");
            if (!newFile.renameTo(tmpFile)) {
                log.severe("Couldn't rename " + newFile.getAbsolutePath()
                    + " to " + tmpFile.getAbsolutePath());
            }
        }
        if (file.renameTo(newFile)) {
            // success!
            if (tmpFile != null) {
                if (!tmpFile.delete()) {
                    log.severe("Couldn't delete " + tmpFile.getAbsolutePath());
                }
            }
        } else {
            // rename failed restore if possible
            if (tmpFile != null) {
                if (!tmpFile.renameTo(newFile)) {
                    log.severe("Couldn't rename " + newFile.getAbsolutePath()
                            + " to " + tmpFile.getAbsolutePath());
                }
            }
        }
        return newFile.exists() && !file.exists();
    }

    /** copy a file from this source to this Directory */
    public boolean copyFileFrom(File file, final FileCopier fileCopier) {
        if (file.exists() && file.canRead()) {
            File newFile = new File(getFile(), file.getName());
            try {
                if (file.getCanonicalPath().equals(newFile.getCanonicalPath()))
                {
                    // cannot copy file onto itself
                    throw new IllegalStateException("cannot copy onto itself");
                }
            } catch (IOException e) {
                return false;
            }
            fileCopier.add(file, newFile, this);

            if (!fileCopier.isStarted()) {
                Runnable runner = new Runnable() {
                    public void run() {
                        fileCopier.start();
                    }
                };
                Thread thread = new Thread(runner);
                thread.start();
                // wait for start else more are started if more files are
                // added using this method
                while (!fileCopier.isStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {

                    }
                }
            }
            return true;
        }
        return false;

    }

    /**
     * Removes a fileinfo from this directory or subdirectories. Also erases
     * directory objects if the got empty.
     * 
     * @param fInfo
     */
    public void removeFileInfo(FileInfo fInfo) {
        if (fileInfoHolderMap.remove(fInfo) != null) {
            return;
        }
        for (Directory dir : subDirectoriesMap.values()) {
            dir.removeFileInfo(fInfo);
            if (dir.fileInfoHolderMap.isEmpty()) {
                subDirectoriesMap.remove(dir);
            }
        }
    }

    public boolean removeFilesOfMember(Member member) {
        boolean removed = false;
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (!holder.isAnyVersionAvailable()) {
                removed = true;
                fileInfoHolderMap.remove(holder.getFileInfo());
            }
        }
        for (Directory dir : subDirectoriesMap.values()) {
            boolean dirRemoved = dir.removeFilesOfMember(member);
            if (dir.fileInfoHolderMap.isEmpty()) {
                subDirectoriesMap.remove(dir);
            }
            removed = removed || dirRemoved;
        }
        return removed;
    }

    /**
     * @param fileInfo
     *            the fileinfo
     * @return the holder of the fileinfo
     */
    public FileInfoHolder getFileInfoHolder(FileInfo fileInfo) {
        FileInfoHolder holder = fileInfoHolderMap.get(fileInfo);
        if (holder != null) {
            return holder;
        }
        String thePath = fileInfo.getLocationInFolder();
        String dirName;
        String rest;
        int index = thePath.indexOf('/');
        if (index == -1) {
            dirName = thePath;
            rest = "";
        } else {
            dirName = thePath.substring(0, index);
            rest = thePath.substring(index + 1, thePath.length());
        }
        if (dirName.length() == 0) {
            return null;
        }
        Directory dir = subDirectoriesMap.get(dirName);
        if (dir == null) {
            // should not happen but this saves a crash (white screen)
            // throw new IllegalStateException("dir not found: " + dirName + " |
            // "
            // + fileInfo.getName());
            return null;
        }
        if (rest.length() == 0) {
            return dir.getFileInfoHolder(fileInfo);
        }
        return dir.getFileInfoHolder(fileInfo, rest);
    }

    private FileInfoHolder getFileInfoHolder(FileInfo fileInfo, String restPath)
    {
        String dirName;
        String rest;
        int index = restPath.indexOf('/');
        if (index == -1) {
            dirName = restPath;
            rest = "";
        } else {
            dirName = restPath.substring(0, index);
            rest = restPath.substring(index + 1, restPath.length());
        }
        Directory dir = subDirectoriesMap.get(dirName);
        if (dir == null) {
            throw new IllegalStateException("dir not found: " + dirName + " | "
                + fileInfo.getName());
        }
        if (rest.length() == 0) {
            return dir.getFileInfoHolder(fileInfo);
        }
        return dir.getFileInfoHolder(fileInfo, rest);
    }

    public Directory getSubDirectory(String dirName) {
        String tmpDirName;
        String rest;
        int index = dirName.indexOf('/');
        if (index == -1) {
            tmpDirName = dirName;
            rest = "";
        } else {
            tmpDirName = dirName.substring(0, index);
            rest = dirName.substring(index + 1, dirName.length());
        }
        Directory dir = subDirectoriesMap.get(tmpDirName);
        if (dir == null) {
            throw new IllegalStateException("dir '" + dirName + "' not found");
        }
        if (rest.length() == 0) {
            return dir;
        }
        return dir.getSubDirectory(rest);
    }

    /**
     * get the files in this dir (not the files in the subs)
     * 
     * @return the list of files
     * @see #getFilesRecursive()
     */
    public List<FileInfo> getFiles() {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfo fileInfo : fileInfoHolderMap.keySet()) {
            files.add(fileInfo);
        }
        return files;
    }

    /**
     * returns only valid files. (valid if at least one member has a not deleted
     * version or member with deleted version is myself)
     * 
     * @return the list of fileinfos
     */
    public List<FileInfo> getFilesRecursive() {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (holder.isValid()) {
                files.add(holder.getFileInfo());
            }
        }
        for (Directory dir : subDirectoriesMap.values()) {
            files.addAll(dir.getFilesRecursive());
        }
        return files;
    }

    /**
     * @return the list of subdirectories in this directory, that are NOT
     *         deleted.
     */
    public List<Directory> listSubDirectories() {
        List<Directory> list = new ArrayList<Directory>(subDirectoriesMap
            .values());
        for (Iterator<Directory> iterator = list.iterator(); iterator.hasNext();)
        {
            Directory directory = iterator.next();
            if (directory.isDeleted()) {
                iterator.remove();
            }
        }
        Collections.sort(list);
        return list;
    }

    /**
     * @return a references to the internal directory collection. Collection
     *         might change after return.
     */
    public Collection<Directory> getSubDirectoriesAsCollection() {
        return Collections.unmodifiableCollection(subDirectoriesMap.values());
    }

    /**
     * @return true if this is the same object or the paths are equal
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof Directory) {
            Directory otherDirectory = (Directory) obj;
            if (rootFolder != null) {
                if (rootFolder != otherDirectory.rootFolder) {
                    return false;
                }
            }
            return otherDirectory.path.equals(path);
        }
        return false;
    }

    /**
     * Added because equals is overridden. (See #692)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return rootFolder.hashCode() ^ path.hashCode();
    }

    /** used for sorting, ignores case * */
    public int compareTo(Directory other) {
        if (other == this) {
            return 0;
        }
        return path.compareToIgnoreCase(other.path);
    }

    /** does this Direcory allready has this exact file on disk */
    public boolean alreadyHasFileOnDisk(File file) {
        File toCheck = new File(getFile(), file.getName());
        return toCheck.exists();
    }

    /**
     * Adds a FileInfo to this Directory
     * 
     * @param fileInfo
     *            the file to add to this Directory
     */
    private void addFile(Member member, FileInfo fileInfo) {
        // Keep synchronization here.
        synchronized (fileInfoHolderMap) {
            if (fileInfoHolderMap.containsKey(fileInfo)) { // already there
                FileInfoHolder fileInfoHolder = fileInfoHolderMap.get(fileInfo);
                if (member.isMySelf()) {
                    // replace, this maybe a converted meta FileInfo that is
                    // re-added.
                    fileInfoHolderMap.put(fileInfo, fileInfoHolder);
                    fileInfoHolder.setFileInfo(fileInfo);
                }

                fileInfoHolder.put(member, fileInfo);
            } else { // new
                FileInfoHolder fileInfoHolder = new FileInfoHolder(rootFolder,
                    member, fileInfo);
                fileInfoHolderMap.put(fileInfo, fileInfoHolder);
            }
        }
    }

    public void addAll(Member member, FileInfo[] fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            add(member, fileInfo);
        }
    }

    /**
     * Answers if all files in this dir and in subdirs are expected.
     * 
     * @param folderRepository
     * @return if the directory is expected
     */
    public boolean isExpected(FolderRepository folderRepository) {
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            FileInfo fInfo = holder.getFileInfo();
            if (fInfo.isDeleted()) {
                // Don't consider deleted
                continue;
            }
            if (!fInfo.isExpected(folderRepository)) {
                return false;
            }
        }

        for (Directory dir : subDirectoriesMap.values()) {
            if (!dir.isExpected(folderRepository)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if all filesInfos and filesInfos in subdirectories are
     *         deleted
     */
    public boolean isDeleted() {
        if (fileInfoHolderMap.isEmpty() && subDirectoriesMap.isEmpty()) {
            return false;
        }
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (!holder.getFileInfo().isDeleted()) {
                return false; // one file not deleted
            }
        }
        for (Directory dir : subDirectoriesMap.values()) {
            if (!dir.isDeleted()) {
                return false;
            }
        }
        return true; // this dir is deleted
    }

    /**
     * @return true if at least one filesInfos in any subdirectories is found
     *         that is deleted.
     */
    public boolean containsDeleted() {
        if (fileInfoHolderMap.isEmpty() && subDirectoriesMap.isEmpty()) {
            return false;
        }
        for (FileInfoHolder fileInfoHolder : fileInfoHolderMap.values()) {
            if (fileInfoHolder.getFileInfo().isDeleted()) {
                return true;
            }
        }
        for (Directory directory : subDirectoriesMap.values()) {
            if (directory.containsDeleted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if this directory contains any local files.
     */
    public boolean containsLocalFiles() {
        if (fileInfoHolderMap.isEmpty() && subDirectoriesMap.isEmpty()) {
            return false;
        }
        for (FileInfoHolder fileInfoHolder : fileInfoHolderMap.values()) {
            FileInfo fileInfo = fileInfoHolder.getFileInfo();
            FileInfo newestVersion = null;
            if (fileInfo.getFolder(rootFolder.getController()
                .getFolderRepository()) != null)
            {
                newestVersion = fileInfo.getNewestNotDeletedVersion(rootFolder
                    .getController().getFolderRepository());
            }

            boolean isIncoming = fileInfo.isDownloading(rootFolder
                .getController())
                || fileInfo.isExpected(rootFolder.getController()
                    .getFolderRepository())
                || newestVersion != null
                && newestVersion.isNewerThan(fileInfo);
            if (!isIncoming && !fileInfo.isDeleted()) {
                return true;
            }
        }
        for (Directory directory : subDirectoriesMap.values()) {
            if (directory.containsLocalFiles()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the directory contains any incoming files.
     */
    public boolean containsIncomingFiles() {
        if (fileInfoHolderMap.isEmpty() && subDirectoriesMap.isEmpty()) {
            return false;
        }
        for (FileInfoHolder fileInfoHolder : fileInfoHolderMap.values()) {
            FileInfo fileInfo = fileInfoHolder.getFileInfo();
            FileInfo newestVersion = null;
            if (fileInfo.getFolder(rootFolder.getController()
                .getFolderRepository()) != null)
            {
                newestVersion = fileInfo.getNewestNotDeletedVersion(rootFolder
                    .getController().getFolderRepository());
            }

            boolean isIncoming = fileInfo.isDownloading(rootFolder
                .getController())
                || fileInfo.isExpected(rootFolder.getController()
                    .getFolderRepository())
                || newestVersion != null
                && newestVersion.isNewerThan(fileInfo);
            if (isIncoming) {
                return true;
            }
        }
        for (Directory directory : subDirectoriesMap.values()) {
            if (directory.containsIncomingFiles()) {
                return true;
            }
        }
        return false;
    }

    /**
     * TODO: Optimize: Run through all recently completed downloads instead of
     * all files. Afterwards remove
     * NavTreeCellRenderer.containsRecentlyCompleted()
     * 
     * @return true if this directory (or any subdirectory) contains a completed
     *         download.
     */
    public boolean containsCompletedDownloads() {
        if (fileInfoHolderMap.isEmpty() && subDirectoriesMap.isEmpty()) {
            return false;
        }
        for (FileInfoHolder fInfoHolder : fileInfoHolderMap.values()) {
            // TODO UARG ugly access to controller.
            if (rootFolder.getController().getTransferManager()
                .isCompletedDownload(fInfoHolder.getFileInfo()))
            {
                return true;
            }
        }
        for (Directory dir : subDirectoriesMap.values()) {
            if (dir.containsCompletedDownloads()) {
                return true;
            }
        }
        return false; // nothing found.
    }

    /**
     * returns a list of all valid FileInfo s, so not the remotely deleted
     * <p>
     * TODO Valid state of FileInfo is highly questionable.
     * 
     * @return the list of files
     */
    public List<FileInfo> getValidFiles() {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (holder.isValid()) {
                files.add(holder.getFileInfo());
            }
        }
        return files;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    /**
     * @return Returns the rootFolder.
     */
    public Folder getRootFolder() {
        return rootFolder;
    }

    public void copyListsFrom(Directory other) {
        fileInfoHolderMap = other.fileInfoHolderMap;
        subDirectoriesMap = other.subDirectoriesMap;
    }

    /**
     * @return Returns the path.
     */
    public String getPath() {
        return path;
    }

    /**
     * helper code to fill build the Directory with FileInfos in the correct sub
     * Directories
     * 
     * @param listOfFiles
     *            The files to add to this Diretory
     * @param folder
     *            The Folder that holds this directory
     */
    static Directory buildDirsRecursive(Member member,
        Collection<FileInfo> listOfFiles, Folder folder)
    {
        Directory root = new Directory(null, Translation
            .getTranslation("general.files"), "", folder);
        for (FileInfo info : listOfFiles) {
            root.add(member, info);
        }
        return root;
    }

    /** add a file recursive to this or correct sub Directory */
    void add(Member member, FileInfo file) {
        String thePath = file.getLocationInFolder();
        if (thePath.length() == 0) {
            addFile(member, file);
        } else {
            String dirName;
            String rest;
            int index = thePath.indexOf('/');
            if (index == -1) {
                dirName = thePath;
                rest = "";
            } else {
                dirName = thePath.substring(0, index);
                rest = thePath.substring(index + 1, thePath.length());
            }
            synchronized (subDirectoriesMap) {
                if (subDirectoriesMap.containsKey(dirName)) {
                    Directory dir = subDirectoriesMap.get(dirName);
                    dir.add(member, file, rest);
                    // TODO fire change ?
                } else {
                    Directory dir = new Directory(this, dirName, dirName,
                        rootFolder);
                    // rootFolder.addDirectory(dir);
                    subDirectoriesMap.put(dirName, dir);
                    dir.add(member, file, rest);
                    // TODO fire change ?
                }
            }
        }
    }

    private void add(Member member, FileInfo file, String restPath) {
        if (restPath.length() == 0) {
            addFile(member, file);
        } else {
            String dirName;
            String rest;
            int index = restPath.indexOf('/');
            if (index == -1) {
                dirName = restPath;
                rest = "";
            } else {
                dirName = restPath.substring(0, index);
                rest = restPath.substring(index + 1, restPath.length());
            }
            synchronized (subDirectoriesMap) {
                if (subDirectoriesMap.containsKey(dirName)) {
                    Directory dir = subDirectoriesMap.get(dirName);
                    dir.add(member, file, rest);
                    // TODO fire Change?
                } else {
                    Directory dir = new Directory(this, dirName, path + '/'
                        + dirName, rootFolder);
                    subDirectoriesMap.put(dirName, dir);
                    dir.add(member, file, rest);
                    // TODO fire change ?
                }
            }
        }
    }

    public String toAscii() {
        StringBuilder str = new StringBuilder();
        str.append(rootFolder.getName());
        str.append('\n');
        return toAscii(str, 0).toString();
    }

    private StringBuilder toAscii(StringBuilder str, int depth) {
        int newdepth = depth + 1;
        String tabs = getTabs(depth);
        for (Directory dir : subDirectoriesMap.values()) {
            str.append(tabs);
            str.append("<DIR>");
            str.append(dir.name);
            str.append('\n');
            dir.toAscii(str, newdepth);
        }
        tabs = getTabs(newdepth);
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            str.append(tabs);
            str.append("<FILE>");
            FileInfo file = holder.getFileInfo();
            str.append(file.getFilenameOnly());
            str.append('\n');
        }

        return str;
    }

    private static final String tabChars = "--";

    private static final String getTabs(int number) {
        String str = "";
        for (int i = 0; i < number; i++) {
            str += (tabChars);
        }
        return str;
    }

    // ////////////////////////////
    // DiskItem implementation. //
    // ////////////////////////////

    public String getExtension() {
        return "";
    }

    public String getLowerCaseName() {
        return name == null ? "" : name.toLowerCase();
    }

    public long getSize() {
        return 0;
    }

    public MemberInfo getModifiedBy() {
        return null;
    }

    public Date getModifiedDate() {
        return null;
    }

    public FolderInfo getFolderInfo() {
        return null;
    }
}