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