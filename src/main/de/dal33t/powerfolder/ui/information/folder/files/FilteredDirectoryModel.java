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

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.DiskItem;

import java.util.List;
import java.util.ArrayList;

/**
 * Lightwight model of a filtered directory. The contains
 * a) a tree directory structure from the root folder, and
 * b) a list of either the files and directories in the currently accessed directory,
 * or all files from here down if in flat mode.
 */
public class FilteredDirectoryModel {

    private final Folder rootFolder;
    private final List<DiskItem> diskItems = new ArrayList<DiskItem>();
    private final FilteredDirectory filteredDirectory;
    private final String directoryRelativeName;

    public FilteredDirectoryModel(Folder rootFolder, String directoryRelativeName) {
        this.rootFolder = rootFolder;
        this.directoryRelativeName = directoryRelativeName;
        filteredDirectory = new FilteredDirectory(rootFolder.getName(), "");
    }

    public Folder getRootFolder() {
        return rootFolder;
    }

    public String getDirectoryRelativeName() {
        return directoryRelativeName;
    }

    public List<DiskItem> getDiskItems() {
        return diskItems;
    }

    public FilteredDirectory getFilteredDirectory() {
        return filteredDirectory;
    }

    public boolean hasFiles() {
        return filteredDirectory.hasFiles();
    }

    public boolean hasFilesDeep() {
        return filteredDirectory.hasFilesDeep();
    }

    public boolean hasNewFiles() {
        return filteredDirectory.hasNewFiles();
    }

    public boolean hasNewFilesDeep() {
        return filteredDirectory.hasNewFilesDeep();
    }
}
