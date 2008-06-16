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
package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.List;

import de.dal33t.powerfolder.ui.FilterModel;
import de.dal33t.powerfolder.DiskItem;

public class FileFilterChangedEvent extends EventObject {

    private List<DiskItem> filteredList;
    private int localFiles;
    private int incomingFiles;
    private int deletedFiles;
    private int recycledFiles;

    public FileFilterChangedEvent(FilterModel source, List<DiskItem> filteredList,
            int localFiles, int incomingFiles, int deletedFiles,
            int recycledFiles) {
        super(source);
        this.filteredList = filteredList;
        this.localFiles = localFiles;
        this.incomingFiles = incomingFiles;
        this.deletedFiles = deletedFiles;
        this.recycledFiles = recycledFiles;
    }

    public List<DiskItem> getFilteredList() {
        return filteredList;
    }

    public int getDeletedFiles() {
        return deletedFiles;
    }

    public int getIncomingFiles() {
        return incomingFiles;
    }

    public int getLocalFiles() {
        return localFiles;
    }

    public int getRecycledFiles() {
        return recycledFiles;
    }
}