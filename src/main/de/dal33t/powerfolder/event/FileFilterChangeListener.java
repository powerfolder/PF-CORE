package de.dal33t.powerfolder.event;

public interface FileFilterChangeListener {
    public void filterChanged(FilterChangedEvent event); 
    public void countChanged(FilterChangedEvent event); 
}
