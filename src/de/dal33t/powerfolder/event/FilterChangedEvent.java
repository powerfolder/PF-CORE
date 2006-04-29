package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.List;

import de.dal33t.powerfolder.ui.FilterModel;

public class FilterChangedEvent extends EventObject {
    private List filteredList;
    
    public FilterChangedEvent(FilterModel source, List filteredList) {
        super(source);        
        this.filteredList = filteredList;
    }
    
    public List getFilteredList() {
        return filteredList;
    }

}
