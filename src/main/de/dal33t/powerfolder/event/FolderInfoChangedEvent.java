package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.List;

import de.dal33t.powerfolder.ui.FilterModel;
import de.dal33t.powerfolder.light.FolderInfo;

public class FolderInfoChangedEvent extends EventObject {
    private List<FolderInfo> filteredList;
    
    public FolderInfoChangedEvent(FilterModel source, List<FolderInfo> filteredList) {
        super(source);        
        this.filteredList = filteredList;
    }
    
    public List<FolderInfo> getFilteredList() {
        return filteredList;
    }

}
