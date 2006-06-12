/* $Id: FolderFilesChanged.java,v 1.2 2006/03/12 23:13:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message which contains only the deltas of the folders list
 * 
 * @see de.dal33t.powerfolder.message.FileList
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class FolderFilesChanged extends FolderRelatedMessage {
    private static final long serialVersionUID = 100L;
    
    /** A list of files added to the folder */
    public FileInfo[] added;
    /** A list of files modified files in the folder */
    public FileInfo[] modified;
    /** A list of files removed from the folder */
    public FileInfo[] removed;

    public FolderFilesChanged(FolderInfo folder) {
        this.folder = folder;
    }

    /**
     * Contructs a new filelist with added files from argument
     * 
     * @param added
     */
    FolderFilesChanged(FolderInfo aFolder, FileInfo[] addedFiles) {
        Reject.ifNull(aFolder, "Folder is null");
        Reject.ifNull(addedFiles, "Added files is null");

        folder = aFolder;
        added = addedFiles;
    }

    /**
     * Build a filelist marking the one file as added/updated to the DB.
     * 
     * @param fileInfo
     *            the file to broadcast
     */
    public FolderFilesChanged(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "Fileinfo is null");

        folder = fileInfo.getFolderInfo();
        added = new FileInfo[]{fileInfo};
    }

    public String toString() {
        return "FolderFilesChanged '" + folder.name + "': "
            + (added != null ? added.length : 0) + " added, "
            + (modified != null ? modified.length : 0) + " modified, "
            + (removed != null ? removed.length : 0) + " removed";
    }
}