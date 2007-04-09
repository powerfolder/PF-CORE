package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.Folder;

public class FolderEvent extends EventObject {

    public FolderEvent(Folder source) {
        super(source);
    }

    public Folder getFolder() {
        return (Folder) getSource();
    }
}
