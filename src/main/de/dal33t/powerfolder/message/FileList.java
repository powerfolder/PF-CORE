/* $Id: FileList.java,v 1.5 2006/03/12 23:13:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Files of a folder.
 * <p>
 * TODO Improve splitting. Should act upon a List<FileInfo> instead of array
 * 
 * @see de.dal33t.powerfolder.message.FolderFilesChanged
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

    /**
     * Creates the message for the filelist. Filelist gets splitted into smaller
     * ones if required.
     * 
     * @param folder
     * @return
     */
    /*
     * public static Message[] createFileListMessages(Folder folder) {
     * FileInfo[] files = folder.getFiles(); return
     * createFileListMessages(folder.getInfo(), files); }
     */

    public static Message[] createFileListMessages(Folder folder) {
        List<FileInfo> infos = folder.getFilesAsList();
        // filter files that are ignored
        LOG.warn("Removed " + folder.getBlacklist().applyIgnore(infos)
            + " files because of blacklisting");
        FileInfo[] infosArray = new FileInfo[infos.size()];
        return createFileListMessages(folder.getInfo(), infos
            .toArray(infosArray));
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
    public static Message[] createFileListMessages(FolderInfo foInfo,
        FileInfo[] files)
    {
        Reject.ifTrue(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE <= 0,
            "Unable to split filelist. nFilesPerMessage: "
                + Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);

        if (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE >= files.length
            || files.length == 0)
        {
            // No need to split
            return new Message[]{new FileList(foInfo, files)};
        }

        // Split list
        int nLists = files.length / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE;
        int lastListSize = files.length
            - Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * nLists;
        int arrSize = nLists;
        if (lastListSize > 0) {
            arrSize++;
        }

        Message[] messages = new Message[arrSize];
        for (int i = 0; i < nLists; i++) {
            FileInfo[] messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
            System.arraycopy(files, i
                * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE, messageFiles, 0,
                messageFiles.length);
            if (i == 0) {
                messages[i] = new FileList(foInfo, messageFiles);
            } else {
                messages[i] = new FolderFilesChanged(foInfo, messageFiles);
            }

        }

        // Add last list
        if (lastListSize > 0) {
            FileInfo[] messageFiles = new FileInfo[lastListSize];
            System.arraycopy(files, Constants.FILE_LIST_MAX_FILES_PER_MESSAGE
                * nLists, messageFiles, 0, messageFiles.length);
            messages[arrSize - 1] = new FolderFilesChanged(foInfo, messageFiles);
        }

        LOG.warn("Splitted filelist into " + arrSize + " folder: " + foInfo
            + "\nSplitted msgs: " + messages);

        return messages;
    }

    public String toString() {
        return "FileList of " + folder + ": " + files.length + " file(s)";
    }
}
