package de.dal33t.powerfolder.ui.information.folder.members;

import javax.swing.JPanel;

import de.dal33t.powerfolder.light.FolderInfo;

public interface MembersTab {

    /**
     * Set the tab with details for a folder.
     * 
     * @param folderInfo
     */
    public abstract void setFolderInfo(FolderInfo folderInfo);

    /**
     * Gets the ui component
     * 
     * @return
     */
    public abstract JPanel getUIComponent();

}