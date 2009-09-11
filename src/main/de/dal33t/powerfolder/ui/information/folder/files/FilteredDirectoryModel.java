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
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightwight model of a filtered directory. Use in reference to the original
 * Directory to get the real detail.
 */
public class FilteredDirectoryModel {

    private final Directory parentDirectory;
    private final Folder rootFolder;
    private final String name;
    private final File relativeFile;
    private final List<FileInfo> fileInfos;
    private final List<FilteredDirectoryModel> subdirectories;
    private boolean newFiles;

    /**
     * Constructor
     */
    public FilteredDirectoryModel(Directory parentDirectory,
                                  Folder rootFolder, String name,
                                  File relativeFile) {
        this.parentDirectory = parentDirectory;
        this.rootFolder = rootFolder;
        this.name = name;
        this.relativeFile = relativeFile;
        fileInfos = new CopyOnWriteArrayList<FileInfo>();
        subdirectories = new CopyOnWriteArrayList<FilteredDirectoryModel>();
    }

    /**
     * Parent of this model, may be null.
     * @return
     */
    public Directory getParentDirectory() {
        return parentDirectory;
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    /**
     * Returns the display name for the node.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the file.
     *
     * @return
     */
    public File getRelativeFile() {
        return relativeFile;
    }

    /**
     * Gets a readonly copy of the directories files.
     *
     * @return
     */
    public List<FileInfo> getFileInfos() {
        return fileInfos;
    }

    /**
     * Returns a list of subdirectory names and sub-FilteredDirectoryModels
     */
    public List<FilteredDirectoryModel> getSubdirectories() {
        return subdirectories;
    }

    public List<Directory> getSubdirectoryDirectories() {
        List<Directory> list = new ArrayList<Directory>();
        for (FilteredDirectoryModel fdm : subdirectories) {
            Directory d = new Directory(rootFolder, fdm.parentDirectory,
                    fdm.name);
                d.addAll(rootFolder.getController().getMySelf(),
                        fdm.fileInfos.toArray(new FileInfo[fdm.fileInfos.size()]));
            list.add(d);
        }
        return list;
    }

    /**
     * Answers if this or any of its children (or any of its children's
     * children...) have any files.
     *
     * @return
     */
    public boolean hasDescendantFiles() {
        if (!fileInfos.isEmpty()) {
            return true;
        }
        for (FilteredDirectoryModel subdirectory : subdirectories) {
            if (subdirectory.hasDescendantFiles()) {
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
}
