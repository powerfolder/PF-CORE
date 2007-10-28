package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.ui.UIPanel;

public interface FolderTab extends UIPanel {
    public String getTitle();
    public void setFolder(Folder folder);     
}
