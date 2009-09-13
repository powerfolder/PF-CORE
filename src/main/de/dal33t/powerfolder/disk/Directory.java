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

import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents a directory of files. No actual disk access from this file, build
 * from list of FileInfos Holds the SubDirectories (may contain Files and
 * Subdirectories themselfs) and Files (FileInfos)
 *
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom </a>
 * @version $Revision: 1.43 $
 */
public class Directory implements Comparable<Directory>, DiskItem {

    private static final Logger log = Logger.getLogger(Directory.class
            .getName());

    /**
     * The files (FileInfoHolder s) in this Directory key = fileInfo value =
     * FileInfoHolder
     */
    private final Map<FileInfo, FileInfoHolder> fileInfoHolderMap
            = new ConcurrentHashMap<FileInfo, FileInfoHolder>(2, 0.75f, 4);

    /**
     * key = dir name, value = Directory
     */
    private final Map<String, Directory> subDirectoriesMap
            = new ConcurrentHashMap<String, Directory>(2, 0.75f, 4);

    /**
     * The name of this directory (no path elements)
     */
    private final String name;

    /**
     * The root Folder which this Directory belongs to
     */
    private final Folder rootFolder;

    /**
     * The parent Directory (may be null, if no parent)
     */
    private final Directory parent;

    /**
     * Constructor
     *
     * @param rootFolder
     * @param parent
     * @param name
     */
    public Directory(Folder rootFolder, Directory parent, String name) {
        Reject.ifNull(rootFolder, "Need a root folder");
        Reject.ifNull(name, "Need a name");
        this.rootFolder = rootFolder;
        this.parent = parent;
        this.name = name;
    }

    /**
     * returns file relative to the root folder base directory.
     */
    public File getRelativeFile() {
        if (parent == null) {
            return new File(name);
        } else {
            return new File(parent.getRelativeFile(), name);
        }
    }

    /**
     * returns the absolute file for this directory.
     */
    public File getAbsoluteFile() {
        if (parent == null) {
            return new File(rootFolder.getLocalBase(), name);
        } else {
            return new File(parent.getAbsoluteFile(), name);
        }
    }

    /**
     * returns the Directory with this name, creates it if not exists yet
     */
    public Directory getSubDirectory(String nameArg) {
        return subDirectoriesMap.get(nameArg);
    }
    
    /**
     * returns the Directory with this name, creates it if not exists yet
     */
    public Directory getCreateSubDirectory(String nameArg) {
        if (subDirectoriesMap.containsKey(nameArg)) {
            return subDirectoriesMap.get(nameArg);
        }
        Directory sub = new Directory(rootFolder, parent, nameArg);

        File newFileName = new File(getAbsoluteFile(), nameArg);
        if (!newFileName.exists()) {
            if (!newFileName.mkdir()) {
                log.info("Failed to create " + newFileName.getAbsolutePath());
            }
        }
        return sub;
    }

    /**
     * Answers if all files in this dir and in subdirs are expected.
     *
     * @param folderRepository
     * @return if the directory is expected
     */
    public boolean isExpected(FolderRepository folderRepository) {
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            FileInfo fileInfo = holder.getFileInfo();
            if (fileInfo.isDeleted()) {
                // Don't consider deleted
                continue;
            }
            if (!fileInfo.isExpected(folderRepository)) {
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
     * Removes a fileinfo from this directory or subdirectories. Also erases
     * directory objects if the got empty.
     *
     * @param fileInfo
     */
    public void removeFileInfo(FileInfo fileInfo) {
        if (fileInfoHolderMap.remove(fileInfo) != null) {
            return;
        }
        for (String key : subDirectoriesMap.keySet()) {
            Directory dir = subDirectoriesMap.get(key);
            dir.removeFileInfo(fileInfo);
            if (dir.fileInfoHolderMap.isEmpty()) {
                subDirectoriesMap.remove(key);
            }
        }
    }

    public boolean removeFilesOfMember(Member member) {
        // @todo duh, member parameter is not used ???
        boolean removed = false;
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (!holder.isAnyVersionAvailable()) {
                removed = true;
                fileInfoHolderMap.remove(holder.getFileInfo());
            }
        }
        for (String key : subDirectoriesMap.keySet()) {
            Directory dir = subDirectoriesMap.get(key);
            boolean dirRemoved = dir.removeFilesOfMember(member);
            if (dir.fileInfoHolderMap.isEmpty()) {
                subDirectoriesMap.remove(key);
            }
            removed = removed || dirRemoved;
        }
        return removed;
    }

    /**
     * get the files in this dir (not the files in the subs)
     *
     * @return the list of files
     */
    public List<FileInfo> getFileInfos() {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfo fileInfo : fileInfoHolderMap.keySet()) {
            files.add(fileInfo);
        }
        return files;
    }

    /**
     * Get the files in this dir and all subdirectories, recursively.
     *
     * @return the list of files
     */
    public List<FileInfo> getFileInfosRecursive() {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfo fileInfo : fileInfoHolderMap.keySet()) {
            files.add(fileInfo);
        }
        for (Directory directory : subDirectoriesMap.values()) {
            files.addAll(directory.getFileInfosRecursive());
        }
        return files;
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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Directory directory = (Directory) obj;

        if (!name.equals(directory.name)) {
            return false;
        }
        if (parent != null ? !parent.equals(directory.parent) : directory.parent != null) {
            return false;
        }
        if (!rootFolder.equals(directory.rootFolder)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + rootFolder.hashCode();
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public int compareTo(Directory o) {
        if (name.equals(o.name)) {
            if (rootFolder.getInfo().equals(o.rootFolder.getInfo())) {
                if (parent == null) {
                    return o.parent == null ? 0 : -1;
                } else {
                    return o.parent == null ? 1 : parent.compareTo(o.parent);
                }
            } else {
                return rootFolder.getInfo().compareTo(o.rootFolder.getInfo());
            }
        } else {
            return name.compareTo(o.name);
        }
    }

    /**
     * Adds a FileInfo to this Directory
     *
     * @param fileInfo the file to add to this Directory
     */
    private void addFile(Member member, FileInfo fileInfo) {
        if (fileInfo.isDiretory()) {
            // Not supported yet.
            return;
        }
        // Keep synchronization here.
        synchronized (fileInfoHolderMap) {
            if (fileInfoHolderMap.containsKey(fileInfo)) { // already there
                FileInfoHolder fileInfoHolder = fileInfoHolderMap.get(fileInfo);
                if (member.isMySelf()) {
                    // replace, this may be a converted meta FileInfo that is
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

    public String getName() {
        return getRelativeFile().getPath();
    }

    public String getFilenameOnly() {
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

    /**
     * add a file recursive to this or correct sub Directory
     */
    void add(Member member, FileInfo file) {
        if (file.isDiretory()) {
            // Not supported yet.
            return;
        }
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
                } else {
                    Directory dir = new Directory(rootFolder, this, dirName);
                    // rootFolder.addDirectory(dir);
                    subDirectoriesMap.put(dirName, dir);
                    dir.add(member, file, rest);
                }
            }
        }
    }

    private void add(Member member, FileInfo file, String restPath) {
        if (file.isDiretory()) {
            // Not supported yet.
            return;
        }
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
                } else {
                    Directory dir = new Directory(rootFolder, this, dirName);
                    subDirectoriesMap.put(dirName, dir);
                    dir.add(member, file, rest);
                }
            }
        }
    }

    public Collection<Directory> getSubdirectories() {
        return subDirectoriesMap.values();
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

    public boolean isDiretory() {
        return true;
    }

    public boolean isFile() {
        return false;
    }
}