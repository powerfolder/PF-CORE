/* $Id: FileList.java,v 1.5 2006/03/12 23:13:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Blacklist;
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

    public final FileInfo[] files;
    /**
     * The number of following delta filelist to expect.
     * 
     * @see FolderFilesChanged
     */
    public final int nFollowingDeltas;

    private FileList(FolderInfo folderInfo, FileInfo[] files, int nDetlas2Follow)
    {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(files, "Files is null");
        Reject.ifTrue(nDetlas2Follow < 0,
            "Invalid number for following detla messages");

        this.files = files;
        this.folder = folderInfo;
        this.nFollowingDeltas = nDetlas2Follow;
    }

    /**
     * Creates the message for the filelist. Filelist gets splitted into smaller
     * ones if required.
     * 
     * @param folder
     * @return the splitted filelist messages.
     */
    public static Message[] createFileListMessages(Folder folder) {
        // Create filelist with blacklist
        return createFileListMessages(folder.getInfo(), folder.getKnownFiles(),
            folder.getBlacklist());
    }

    /**
     * Splits the filelist into smaller ones. Always splits into one
     * <code>FileList</code> and (if required) multiple
     * <code>FolderFilesChanged</code> messages
     * <P>
     * Public only because of JUNIT test
     * 
     * @param foInfo
     *            the folder for the message
     * @param files
     *            the array of fileinfos to include.
     * @return the splitted list
     * @private
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
            return new Message[]{new FileList(foInfo, files, 0)};
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
                messages[i] = new FileList(foInfo, messageFiles, nLists);
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

        LOG.verbose("Splitted filelist into " + arrSize + " folder: " + foInfo
            + "\nSplitted msgs: " + messages);

        return messages;
    }

    /**
     * Splits the filelist into smaller ones. Always splits into one
     * <code>FileList</code> and (if required) multiple
     * <code>FolderFilesChanged</code> messages
     * 
     * @param foInfo
     *            the folder for the message
     * @param files
     *            the fileinfos to include.
     * @param blacklist
     *            the blacklist to apply
     * @return the splitted list
     */
    public static Message[] createFileListMessages(FolderInfo foInfo,
        Collection<FileInfo> files, Blacklist blacklist)
    {
        Reject.ifTrue(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE <= 0,
            "Unable to split filelist. nFilesPerMessage: "
                + Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);

        if (files.isEmpty()) {
            return new Message[]{new FileList(foInfo, new FileInfo[0], 0)};
        }

        // Split list
        // FIXME: nLists is inaccurate. Sometimes a bit higher, because of
        // ingore pattersn.
        // However does not harm unless exact number is absolutely required
        // Keep an eye on side effects on Member/Handshake when waiting for
        // deltas.
        int nLists = (files.size() / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) + 1;
        List<Message> messages = new ArrayList<Message>(nLists);

        boolean firstMessage = true;
        int curMsgIndex = 0;
        FileInfo[] messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
        for (FileInfo file : files) {
            if (blacklist.isIgnored(file)) {
                continue;
            }
            messageFiles[curMsgIndex] = file;
            curMsgIndex++;
            if (curMsgIndex >= Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                if (firstMessage) {
                    messages.add(new FileList(foInfo, messageFiles, nLists));
                    firstMessage = false;
                } else {
                    messages.add(new FolderFilesChanged(foInfo, messageFiles));
                }
                messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
                curMsgIndex = 0;
            }
        }

        if (curMsgIndex != 0 && curMsgIndex < messageFiles.length) {
            // Last message
            FileInfo[] lastFiles = new FileInfo[curMsgIndex];
            System.arraycopy(messageFiles, 0, lastFiles, 0, lastFiles.length);
            if (firstMessage) {
                messages.add(new FileList(foInfo, lastFiles, nLists));
                firstMessage = false;
            } else {
                messages.add(new FolderFilesChanged(foInfo, lastFiles));
            }
        }

        LOG.warn("Splitted filelist into " + messages.size() + ", folder: "
            + foInfo + "\nSplitted msgs: " + messages);

        return messages.toArray(new Message[0]);
    }

    public String toString() {
        return "FileList of " + folder + ": " + files.length + " file(s)";
    }
}
