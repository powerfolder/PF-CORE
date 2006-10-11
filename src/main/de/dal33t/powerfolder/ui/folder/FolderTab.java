package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.ui.HasUIPanel;

public interface FolderTab extends HasUIPanel {
    public String getTitle();
    public void setFolder(Folder folder);     
}
