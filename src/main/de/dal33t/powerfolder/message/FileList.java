/* $Id: FileList.java,v 1.5 2006/03/12 23:13:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Files of a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileList extends FolderRelatedMessage {
    private static final Logger LOG = Logger.getLogger(FileList.class);

    private static final long serialVersionUID = 100L;

    public FileInfo[] files;

    public FileList() {
        // Serialisation constructor
    }

    private FileList(FolderInfo folderInfo, FileInfo[] files) {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(files, "Files is null");

        this.files = files;
        this.folder = folderInfo;
    }

    private FileList(Folder folder) {
        this(folder.getInfo(), folder.getFiles());
    }

    /**
     * Creates the message for the filelist. Filelist gets splitted into smaller
     * ones if required
     * 
     * @param folder
     * @return
     */
    public static Message[] createFileListMessages(Folder folder) {
        // TODO: Omit creation of inital filelist. directly create splitted
        // messages
        return new FileList(folder)
            .split(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
    }

    /**
     * Splits the filelist into smaller ones. Always splits into one
     * <code>FileList</code> and (if required) multiple
     * <code>FolderFilesChanged</code> messages
     * 
     * @param nFilesPerMessage
     *            the number of maximum files in one list
     * @return the splitted list
     */
    private Message[] split(int nFilesPerMessage) {
        Reject.ifTrue(nFilesPerMessage <= 0,
            "Unable to split filelist. nFilesPerMessage: " + nFilesPerMessage);

        if (nFilesPerMessage >= files.length || files.length == 0) {
            // No need to split
            return new Message[]{this};
        }

        // Split list
        int nLists = files.length / nFilesPerMessage;
        int lastListSize = this.files.length - nFilesPerMessage * nLists;
        int arrSize = nLists;
        if (lastListSize > 0) {
            arrSize++;
        }

        Message[] messages = new Message[arrSize];
        for (int i = 0; i < nLists; i++) {
            FileInfo[] messageFiles = new FileInfo[nFilesPerMessage];
            System.arraycopy(this.files, i * nFilesPerMessage, messageFiles, 0,
                messageFiles.length);
            if (i == 0) {
                messages[i] = new FileList(this.folder, messageFiles);
            } else {
                messages[i] = new FolderFilesChanged(this.folder, messageFiles);
            }

        }

        // Add last list
        if (lastListSize > 0) {
            FileInfo[] messageFiles = new FileInfo[lastListSize];
            System.arraycopy(this.files, nFilesPerMessage * nLists,
                messageFiles, 0, messageFiles.length);
            messages[arrSize - 1] = new FolderFilesChanged(this.folder, messageFiles);
        }

        LOG.warn("Splitted filelist into " + arrSize + " lists: " + this
            + "\nSplitted msgs: " + messages);

        return messages;
    }

    public String toString() {
        return "FileList of " + folder + ": " + files.length + " file(s)";
    }
}
