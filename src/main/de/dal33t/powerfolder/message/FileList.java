/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
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

    private static final Logger log = Logger
        .getLogger(FileList.class.getName());

    private static final long serialVersionUID = 100L;

    public FileInfo[] files;
    /**
     * The number of following delta filelist to expect.
     *
     * @see FolderFilesChanged
     */
    public int nFollowingDeltas;

    protected FileList() {
        // Serialization
    }

    protected FileList(FolderInfo folderInfo, FileInfo[] files,
        int nDetlas2Follow)
    {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(files, "Files is null");
        Reject.ifTrue(nDetlas2Follow < 0,
            "Invalid number for following detla messages");

        this.files = files;
        this.folder = folderInfo;
        this.nFollowingDeltas = nDetlas2Follow;
    }

    protected FileList(FolderInfo folderInfo) {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        this.files = null;
        this.folder = folderInfo;
        this.nFollowingDeltas = 0;
    }

    /**
     * Just to inform that we won't or can't send any information about the
     * files.
     *
     * @param foInfo
     * @param useExt
     *            if use {@link Externalizable}s
     * @return a list that contains null information about the files in a
     *         folder.
     */
    public static FileList createEmpty(FolderInfo foInfo, boolean useExt) {
        if (useExt) {
            return new FileListExt(foInfo);
        }
        return new FileList(foInfo);
    }

    /**
     * Creates the message for the filelist. Filelist gets splitted into smaller
     * ones if required.
     *
     * @param folder
     * @param useExt
     *            if use {@link Externalizable}s
     * @return the splitted filelist messages.
     */
    public static Message[] create(Folder folder, boolean useExt) {
        return createFileListMessages(folder.getInfo(), folder.getKnownFiles(),
            folder.getKnownDirectories(), folder.getDiskItemFilter(), useExt);
    }

    /**
     * Splits the filelist into smaller ones. Splits into one
     * <code>FileList</code> and (if required) multiple
     * <code>FolderFilesChanged</code> messages
     *
     * @param foInfo
     *            the folder for the message
     * @param files
     *            the fileinfos to include.
     * @param diskItemFilter
     *            the item filter to appy
     * @return the splitted list
     */
    public static Message[] create4Test(FolderInfo foInfo,
        Collection<FileInfo> files, DiskItemFilter diskItemFilter)
    {
        Collection<DirectoryInfo> dirInfos = Collections.emptyList();
        return createFileListMessages(foInfo, files, dirInfos, diskItemFilter,
            true);
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
     *            the blacklist to apply * @param onlyChanges if the messages
     *            should contain only {@link FolderFilesChanged} or contain a
     *            {@link FileList} as first message.
     * @return the splitted list
     */
    private static Message[] createFileListMessages(FolderInfo foInfo,
        Collection<FileInfo> files, Collection<DirectoryInfo> dirs,
        DiskItemFilter diskItemFilter, boolean useExt)
    {
        Reject.ifNull(foInfo, "Folder info is null");
        Reject.ifNull(files, "Files is null");
        Reject.ifNull(diskItemFilter, "DiskItemFilter is null");
        Reject.ifTrue(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE <= 0,
            "Unable to split filelist. nFilesPerMessage: "
                + Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);

        if (files.isEmpty() && dirs.isEmpty()) {
            if (useExt) {
                return new Message[]{new FileListExt(foInfo, new FileInfo[0], 0)};
            } else {
                return new Message[]{new FileList(foInfo, new FileInfo[0], 0)};
            }
        }

        List<Message> messages = new ArrayList<Message>(files.size()
            / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
        int nDeltas = 0;
        boolean firstMessage = true;
        int curMsgIndex = 0;
        FileInfo[] messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
        for (FileInfo fileInfo : files) {
            if (diskItemFilter.isExcluded(fileInfo)) {
                continue;
            }
            messageFiles[curMsgIndex] = fileInfo;
            curMsgIndex++;
            if (curMsgIndex >= Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                if (firstMessage) {
                    if (useExt) {
                        messages.add(new FileListExt(foInfo, messageFiles, 0));
                    } else {
                        messages.add(new FileList(foInfo, messageFiles, 0));
                    }
                    firstMessage = false;
                } else {
                    nDeltas++;
                    if (useExt) {
                        messages.add(new FolderFilesChangedExt(foInfo,
                            messageFiles));
                    } else {
                        messages.add(new FolderFilesChanged(foInfo,
                            messageFiles));
                    }
                }
                messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
                curMsgIndex = 0;
            }
        }
        for (DirectoryInfo dirInfo : dirs) {
            if (diskItemFilter.isExcluded(dirInfo)) {
                continue;
            }
            messageFiles[curMsgIndex] = dirInfo;
            curMsgIndex++;
            if (curMsgIndex >= Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                if (firstMessage) {
                    if (useExt) {
                        messages.add(new FileListExt(foInfo, messageFiles, 0));
                    } else {
                        messages.add(new FileList(foInfo, messageFiles, 0));
                    }
                    firstMessage = false;
                } else {
                    nDeltas++;
                    if (useExt) {
                        messages.add(new FolderFilesChangedExt(foInfo,
                            messageFiles));
                    } else {
                        messages.add(new FolderFilesChanged(foInfo,
                            messageFiles));
                    }
                }
                messageFiles = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE];
                curMsgIndex = 0;
            }
        }

        if (firstMessage && curMsgIndex == 0) {
            // Only ignored files
            if (useExt) {
                return new Message[]{new FileListExt(foInfo, new FileInfo[0], 0)};
            } else {
                return new Message[]{new FileList(foInfo, new FileInfo[0], 0)};
            }
        }
        if (curMsgIndex != 0 && curMsgIndex < messageFiles.length) {
            // Last message
            FileInfo[] lastFiles = new FileInfo[curMsgIndex];
            System.arraycopy(messageFiles, 0, lastFiles, 0, lastFiles.length);
            if (firstMessage) {
                if (useExt) {
                    messages.add(new FileListExt(foInfo, lastFiles, 0));
                } else {
                    messages.add(new FileList(foInfo, lastFiles, 0));
                }
                firstMessage = false;
            } else {
                nDeltas++;
                if (useExt) {
                    messages.add(new FolderFilesChangedExt(foInfo, lastFiles));
                } else {
                    messages.add(new FolderFilesChanged(foInfo, lastFiles));
                }
            }
        }

        // Set the actual number of deltas
        if (!messages.isEmpty() && messages.get(0) instanceof FileList) {
            ((FileList) messages.get(0)).nFollowingDeltas = nDeltas;
        }

        if (log.isLoggable(Level.FINER)) {
            log.finer("Splitted filelist into " + messages.size()
                + ", deltas: " + nDeltas + ", folder: " + foInfo + ", files: "
                + files.size() + ", dirs: " + dirs.size() + "\nSplitted msgs: "
                + messages);
        }

        return messages.toArray(new Message[messages.size()]);
    }

    /**
     * A filelist that does contains any information of the remote folder.
     *
     * @return if this filelist contains null / nothing / nada.
     */
    public boolean isNull() {
        return files == null;
    }

    public String toString() {
        return "FileList of "
            + folder
            + ": "
            + (files != null
                ? files.length + " file(s)"
                : "No information about files.");
    }
}
