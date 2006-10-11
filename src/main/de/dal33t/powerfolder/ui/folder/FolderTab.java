package de.dal33t.powerfolder.ui.folder;

import javax.swing.JComponent;

import de.dal33t.powerfolder.disk.Folder;

public interface FolderTab {
    public String getTitle();
    public void setFolder(Folder folder);
    public JComponent getUIComponent(); 
}
