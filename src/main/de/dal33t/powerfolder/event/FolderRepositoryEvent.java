package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderInfo;

/** Event fired by the FolderRepository */
public class FolderRepositoryEvent extends EventObject {
    private Folder folder;
    private FolderInfo folderInfo;

    /** create a FolderRepositoryEvent */
    public FolderRepositoryEvent(FolderRepository source) {
        super(source);
    }

    /**
     * create a FolderRepositoryEvent about a Folder
     * 
     * @param folder
     *            the folder this event is about
     */
    public FolderRepositoryEvent(FolderRepository source, Folder folder) {
        super(source);
        this.folder = folder;
        this.folderInfo = folder.getInfo();
    }

    /**
     * create a FolderRepositoryEvent about a FolderInfo
     * 
     * @param folderInfo
     *            the folder this event is about
     */
    public FolderRepositoryEvent(FolderRepository source, FolderInfo folderInfo)
    {
        super(source);
        this.folderInfo = folderInfo;
    }

    /**
     * @return Returns the folder, maybe null (then use getFolderInfo)
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * @return Returns the folderInfo, maybe null (then use getFolder)
     */
    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

}
