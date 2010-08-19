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
 * $Id: FilteredDirectoryModel.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.files;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.disk.Folder;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightwight model of a filtered directory. Use in reference to the original
 * Directory to get the real detail.
 */
public class FilteredDirectoryModel {

    private final Folder rootFolder;
    private final DirectoryInfo directoryInfo;
    private final List<FileInfo> fileInfos;
    private final List<FilteredDirectoryModel> subdirectories;
    private boolean newFiles;

    /**
     * Constructor
     */
    public FilteredDirectoryModel(Folder rootFolder) {
        this.rootFolder = rootFolder;
        fileInfos = new CopyOnWriteArrayList<FileInfo>();
        subdirectories = new CopyOnWriteArrayList<FilteredDirectoryModel>();
        directoryInfo = rootFolder.getBaseDirectoryInfo();
    }

    public FilteredDirectoryModel(Folder rootFolder, DirectoryInfo directoryInfo)
    {
        this.rootFolder = rootFolder;
        fileInfos = new CopyOnWriteArrayList<FileInfo>();
        subdirectories = new CopyOnWriteArrayList<FilteredDirectoryModel>();
        this.directoryInfo = directoryInfo;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    /**
     * Gets a readonly copy of the directories files.
     * 
     * @return
     */
    public List<FileInfo> getFileInfos() {
        return fileInfos;
    }

    public DirectoryInfo getDirectoryInfo() {
        return directoryInfo;
    }

    /**
     * Returns a list of subdirectory names and sub-FilteredDirectoryModels
     */
    public List<FilteredDirectoryModel> getSubdirectories() {
        return subdirectories;
    }

    /**
     * Answers if this or any of its children (or any of its children's
     * children...) have any files.
     * 
     * @return
     */
    public boolean hasFilesDeep() {
        if (!fileInfos.isEmpty()) {
            return true;
        }
        for (FilteredDirectoryModel subdirectory : subdirectories) {
            if (subdirectory.hasFilesDeep()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set true if this directory has ne files.
     * 
     * @param newFiles
     */
    public void setNewFiles(boolean newFiles) {
        this.newFiles = newFiles;
    }

    /**
     * Answers if this or any of its children (or any of its children's
     * children...) have any new files.
     * 
     * @return
     */
    public boolean hasDescendantNewFiles() {
        if (newFiles) {
            return true;
        }

        for (FilteredDirectoryModel subdirectory : subdirectories) {
            if (subdirectory.hasDescendantNewFiles()) {
                return true;
            }
        }

        return false;
    }

    public List<FileInfo> getFilesRecursive() {
        List<FileInfo> list = new ArrayList<FileInfo>();
        list.addAll(fileInfos);
        for (FilteredDirectoryModel subdirectory : subdirectories) {
            list.addAll(subdirectory.getFilesRecursive());
        }
        return list;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        FilteredDirectoryModel that = (FilteredDirectoryModel) obj;

        if (newFiles != that.newFiles) {
            return false;
        }
        if (directoryInfo != null
            ? !directoryInfo.equals(that.directoryInfo)
            : that.directoryInfo != null)
        {
            return false;
        }
        if (rootFolder != null
            ? !rootFolder.equals(that.rootFolder)
            : that.rootFolder != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = rootFolder != null ? rootFolder.hashCode() : 0;
        result = 31 * result
            + (directoryInfo != null ? directoryInfo.hashCode() : 0);
        result = 31 * result + (newFiles ? 1 : 0);
        return result;
    }
}
