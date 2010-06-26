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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;

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
    private final Map<String, FileInfoHolder> fileInfoHolderMap = Util
        .createConcurrentHashMap(2);

    private final Object subDirMapLock = new Object();
    /**
     * key = dir name, value = Directory
     */
    private Map<String, Directory> subDirectoriesMap;

    /**
     * The name of this directory (no path elements)
     */
    private final String filenameOnly;

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
     * @param filenameOnly
     */
    public Directory(Folder rootFolder, Directory parent, String filenameOnly) {
        Reject.ifNull(rootFolder, "Need a root folder");
        Reject.ifNull(filenameOnly, "Need a filenameOnly");
        this.rootFolder = rootFolder;
        this.parent = parent;
        this.filenameOnly = filenameOnly;
    }

    /**
     * returns the absolute file for this directory.
     */
    public File getAbsoluteFile() {
        return FileUtils.buildFileFromRelativeName(rootFolder.getLocalBase(),
            getRelativeName());
    }

    /**
     * returns the Directory with this relativeName (the name relative to root
     * folder).
     */
    public Directory getSubDirectory(String relativeName) {
        if (!hasSubDirectories()) {
            return null;
        }
        int i = relativeName.indexOf('/');
        if (i == -1) {
            return getSubdirectriesMap().get(relativeName);
        } else {
            String subDir = relativeName.substring(0, i);
            String restPath = relativeName.substring(i + 1);
            Directory dir = getSubdirectriesMap().get(subDir);
            return dir.getSubDirectory(restPath);
        }
    }

    /**
     * Answers if all files in this dir and in subdirs are expected.
     * 
     * @return if the directory is expected
     */
    public boolean isExpected() {
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            FileInfo fileInfo = holder.getFileInfo();
            if (fileInfo.isDeleted()) {
                // Don't consider deleted
                continue;
            }
            if (!fileInfo.isExpected(rootFolder.getController()
                .getFolderRepository()))
            {
                return false;
            }
        }

        if (hasSubDirectories()) {
            for (Directory dir : getSubdirectriesMap().values()) {
                if (!dir.isExpected()) {
                    return false;
                }
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
        if (fileInfoHolderMap.remove(fileInfo.getRelativeName()) != null) {
            return;
        }
        if (hasSubDirectories()) {
            for (String key : getSubdirectriesMap().keySet()) {
                Directory dir = getSubdirectriesMap().get(key);
                dir.removeFileInfo(fileInfo);
                if (dir.fileInfoHolderMap.isEmpty()) {
                    getSubdirectriesMap().remove(key);
                }
            }
        }
    }

    public boolean removeUnusedFileInfoHolders() {
        boolean removed = false;
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (!holder.isAnyVersionAvailable()) {
                removed = true;
                fileInfoHolderMap
                    .remove(holder.getFileInfo().getRelativeName());
            }
        }
        if (hasSubDirectories()) {
            for (Entry<String, Directory> entry : getSubdirectriesMap()
                .entrySet())
            {
                Directory dir = entry.getValue();
                boolean dirRemoved = dir.removeUnusedFileInfoHolders();
                if (dir.fileInfoHolderMap.isEmpty()) {
                    getSubdirectriesMap().remove(entry.getKey());
                }
                removed = removed || dirRemoved;
            }
        }
        return removed;
    }

    /**
     * get the files in this dir (not the files in the subs)
     * 
     * @return the list of files
     */
    public Collection<FileInfo> getFileInfos() {
        List<FileInfo> files = new ArrayList<FileInfo>(fileInfoHolderMap.size());
        for (FileInfoHolder fileInfoHolder : fileInfoHolderMap.values()) {
            files.add(fileInfoHolder.getFileInfo());
        }
        return files;
    }

    /**
     * @return the file info holders as unmodifiable collection.
     */
    public Collection<FileInfoHolder> getFileInfoHolders() {
        return Collections.unmodifiableCollection(fileInfoHolderMap.values());
    }

    /**
     * Get the files in this dir and all subdirectories, recursively.
     * 
     * @return the list of files
     */
    public List<FileInfo> getFileInfosRecursive() {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfoHolder fileInfoHolder : fileInfoHolderMap.values()) {
            files.add(fileInfoHolder.getFileInfo());
        }
        if (hasSubDirectories()) {
            for (Directory directory : getSubdirectriesMap().values()) {
                files.addAll(directory.getFileInfosRecursive());
            }
        }
        return files;
    }

    /**
     * @return true if all filesInfos and filesInfos in subdirectories are
     *         deleted
     */
    public boolean isDeleted() {
        if (fileInfoHolderMap.isEmpty() && !hasSubDirectories()) {
            return false;
        }
        for (FileInfoHolder holder : fileInfoHolderMap.values()) {
            if (!holder.getFileInfo().isDeleted()) {
                return false; // one file not deleted
            }
        }
        if (hasSubDirectories()) {
            for (Directory dir : getSubdirectriesMap().values()) {
                if (!dir.isDeleted()) {
                    return false;
                }
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

        if (!filenameOnly.equals(directory.filenameOnly)) {
            return false;
        }
        if (parent != null
            ? !parent.equals(directory.parent)
            : directory.parent != null)
        {
            return false;
        }
        if (!rootFolder.equals(directory.rootFolder)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = filenameOnly.hashCode();
        result = 31 * result + rootFolder.hashCode();
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    public int compareTo(Directory o) {
        if (filenameOnly.equals(o.filenameOnly)) {
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
            return filenameOnly.compareTo(o.filenameOnly);
        }
    }

    /**
     * Adds a FileInfo to this Directory
     * 
     * @param fileInfo
     *            the file to add to this Directory
     */
    private void addFile(Member member, FileInfo fileInfo) {
        if (fileInfo.isDiretory()) {
            // Not supported.
            return;
        }
        String relativeName = fileInfo.getRelativeName();
        // Keep synchronization here.
        synchronized (fileInfoHolderMap) {
            FileInfoHolder fileInfoHolder = fileInfoHolderMap.get(relativeName);
            if (fileInfoHolder != null) { // already there
                if (member.isMySelf()) {
                    // Update value.
                    fileInfoHolder.setFileInfo(fileInfo);
                }
                fileInfoHolder.put(member, fileInfo);
            } else { // new
                fileInfoHolder = new FileInfoHolder(rootFolder, member,
                    fileInfo);
                fileInfoHolderMap.put(relativeName, fileInfoHolder);
            }
        }
    }

    public void addAll(Member member, FileInfo... fileInfos) {
        addAll(member, Arrays.asList(fileInfos));
    }

    public void addAll(Member member, Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            add(member, fileInfo);
        }
    }

    /**
     * Build a path name relative to root.
     * 
     * @return
     */
    public String getRelativeName() {
        if (parent == null) {
            return filenameOnly;
        } else if (parent.getRelativeName().trim().length() == 0) {
            return filenameOnly;
        } else {
            return parent.getRelativeName().trim() + '/' + filenameOnly;
        }
    }

    public String getFilenameOnly() {
        return filenameOnly;
    }

    public String toString() {
        return filenameOnly;
    }

    /**
     * @return Returns the rootFolder.
     */
    public Folder getRootFolder() {
        return rootFolder;
    }

    /**
     * Add a file recursive to this or correct sub Directory
     * 
     * @param member
     *            member to add for
     * @param fileInfo
     *            FileInfo to add
     */
    void add(Member member, FileInfo fileInfo) {
        add0(member, fileInfo, fileInfo.getRelativeName());
    }

    /**
     * Add a file recursive from a point in the tree.
     * 
     * @param member
     *            member to add for
     * @param fileInfo
     *            FileInfo to add
     * @param relativePath
     *            relative path in the tree, relative to this directory
     */
    void add0(Member member, FileInfo fileInfo, String relativePath) {
        if (fileInfo.isDiretory()) {
            // Not supported.
            return;
        }

        int index = relativePath.indexOf('/');
        if (index == -1) {
            // Local. Put it here
            addFile(member, fileInfo);
            return;
        }

        String dirName = relativePath.substring(0, index);
        String theRest = relativePath.substring(index + 1);

        if (StringUtils.isBlank(dirName) || StringUtils.isBlank(theRest)) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Got empty directory " + this + ", dirname: "
                    + dirName + ", rest: " + theRest + ", relpath: "
                    + relativePath + " " + fileInfo.toDetailString());
            }
        }

        Directory dir;
        synchronized (subDirMapLock) {
            dir = getSubdirectriesMap().get(dirName);
            if (dir == null) {
                dir = new Directory(rootFolder, this, dirName);
                getSubdirectriesMap().put(dirName, dir);
            }
        }
        dir.add0(member, fileInfo, theRest);
    }

    public Collection<Directory> getSubdirectories() {
        if (!hasSubDirectories()) {
            return Collections.emptyList();
        }
        return getSubdirectriesMap().values();
    }

    private boolean hasSubDirectories() {
        return subDirectoriesMap != null && !subDirectoriesMap.isEmpty();
    }

    private synchronized Map<String, Directory> getSubdirectriesMap() {
        if (subDirectoriesMap != null) {
            return subDirectoriesMap;
        }
        synchronized (subDirMapLock) {
            subDirectoriesMap = Util.createConcurrentHashMap(2);
        }
        return subDirectoriesMap;
    }

    public FileInfo getDirectoryInfo() {
        return rootFolder.getFile(FileInfoFactory.lookupInstance(rootFolder
            .getInfo(), getRelativeName(), true));
    }

    // ////////////////////////////
    // DiskItem implementation. //
    // ////////////////////////////

    public String getExtension() {
        return "";
    }

    public String getLowerCaseFilenameOnly() {
        return filenameOnly == null ? "" : filenameOnly.toLowerCase();
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