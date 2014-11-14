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
 * $Id: FilteredDirectoryEvent.java 5514 2008-10-25 15:23:59Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.files;

/**
 * Event encapsulating filtering results.
 */
public class FilteredDirectoryEvent {

    private FilteredDirectoryModel model;
    private long deletedFiles;
    private long incomingFiles;
    private long localFiles;
    private boolean folderChanged;
    private int fileFilterMode;
    private boolean defaultFilter;

    public FilteredDirectoryEvent(long deletedFiles, long incomingFiles,
        long localFiles, FilteredDirectoryModel model, boolean folderChanged,
        int fileFilterMode, boolean defaultFilter) {
        this.deletedFiles = deletedFiles;
        this.incomingFiles = incomingFiles;
        this.localFiles = localFiles;
        this.model = model;
        this.folderChanged = folderChanged;
        this.fileFilterMode = fileFilterMode;
        this.defaultFilter = defaultFilter;
    }

    public long getDeletedFiles() {
        return deletedFiles;
    }

    public long getIncomingFiles() {
        return incomingFiles;
    }

    public long getLocalFiles() {
        return localFiles;
    }

    public FilteredDirectoryModel getModel() {
        return model;
    }

    public boolean isFolderChanged() {
        return folderChanged;
    }

    public int getFileFilterMode() {
        return fileFilterMode;
    }

    public boolean isDefaultFilter() {
        return defaultFilter;
    }
}
