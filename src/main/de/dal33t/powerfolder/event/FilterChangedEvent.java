package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.List;

import de.dal33t.powerfolder.ui.FilterModel;
import de.dal33t.powerfolder.DiskItem;

public class FilterChangedEvent extends EventObject {
    private List<DiskItem> filteredList;
    
    public FilterChangedEvent(FilterModel source, List<DiskItem> filteredList) {
        super(source);        
        this.filteredList = filteredList;
    }
    
    public List<DiskItem> getFilteredList() {
        return filteredList;
    }

}
