/* $Id: FolderFilesChanged.java,v 1.2 2006/03/12 23:13:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Blacklist;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message which contains only the deltas of the folders list
 * 
 * @see de.dal33t.powerfolder.message.FileList
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class FolderFilesChanged extends FolderRelatedMessage {
    private static final Logger LOG = Logger.getLogger(FileList.class);
    private static final long serialVersionUID = 100L;

    /** A list of files added to the folder */
    public FileInfo[] added;
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

    /**
     * Splits the filelist into small delta message. Splits into multiple
     * <code>FolderFilesChanged</code> messages
     * 
     * @param foInfo
     *            the folder for the message
     * @param files
     *            the new fileinfos to include.
     * @param blacklist
     *            the blacklist to apply
     * @param added
     *            true if the the files are put into the "added" field,
     *            otherwise into "removed"
     * @return the splitted list or NULL if nothing to send.
     */
    public static FolderFilesChanged[] createFolderFilesChangedMessages(
        FolderInfo foInfo, Collection<FileInfo> files, Blacklist blacklist,
        boolean added)
    {
        Reject.ifNull(foInfo, "Folder info is null");
        Reject.ifNull(files, "Files is null");
        Reject.ifNull(blacklist, "Blacklist is null");
        Reject.ifTrue(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE <= 0,
            "Unable to split filelist. nFilesPerMessage: "
                + Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);

        if (files.isEmpty()) {
            return null;
        }

        List<FolderFilesChanged> messages = new ArrayList<FolderFilesChanged>();
        int nDeltas = 0;
        int curMsgIndex = 0;
        FileInfo[] messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
        for (FileInfo fileInfo : files) {
            if (blacklist.isIgnored(fileInfo)) {
                continue;
            }
            messageFiles[curMsgIndex] = fileInfo;
            curMsgIndex++;
            if (curMsgIndex >= Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                nDeltas++;
                FolderFilesChanged msg = new FolderFilesChanged(foInfo);
                if (added) {
                    msg.added = messageFiles;
                } else {
                    msg.removed = messageFiles;
                }
                messages.add(msg);
                messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
                curMsgIndex = 0;
            }
        }
        if (curMsgIndex == 0 && messages.isEmpty()) {
            // Only ignored files
            return null;
        }
        if (curMsgIndex != 0 && curMsgIndex < messageFiles.length) {
            // Last message
            FileInfo[] lastFiles = new FileInfo[curMsgIndex];
            System.arraycopy(messageFiles, 0, lastFiles, 0, lastFiles.length);
            nDeltas++;
            FolderFilesChanged msg = new FolderFilesChanged(foInfo);
            if (added) {
                msg.added = lastFiles;
            } else {
                msg.removed = lastFiles;
            }
            messages.add(msg);
        }

        if (LOG.isVerbose()) {
            LOG.verbose("Splitted folder files delta into " + messages.size()
                + " messages, folder: " + foInfo + "\nSplitted msgs: "
                + messages);
        }

        return messages.toArray(new FolderFilesChanged[0]);
    }

    public String toString() {
        return "FolderFilesChanged '" + folder.name + "': "
            + (added != null ? added.length : 0) + " files, "
            + (removed != null ? removed.length : 0) + " removed";
    }
}