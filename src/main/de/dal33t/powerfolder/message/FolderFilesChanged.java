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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.d2d.D2DMessage;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.FolderFilesChangedProto;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message which contains only the deltas of the folders list
 *
 * @see de.dal33t.powerfolder.message.FileList
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class FolderFilesChanged extends FolderRelatedMessage
  implements D2DMessage
{

    private static final Logger log = Logger.getLogger(FolderFilesChanged.class
        .getName());
    private static final long serialVersionUID = 100L;

    /** A list of files added to the folder */
    protected FileInfo[] added;

    /**
     * @Deprecated use {@link #added}
     */
    @Deprecated
    protected FileInfo[] removed;

    protected FolderFilesChanged() {
        // Serialization
    }

    FolderFilesChanged(FolderInfo folder) {
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
    protected FolderFilesChanged(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "Fileinfo is null");

        folder = fileInfo.getFolderInfo();
        if (fileInfo.isDeleted()) {
            removed = new FileInfo[]{fileInfo};
        } else {
            added = new FileInfo[]{fileInfo};
        }
    }

    /**
     * Build a filelist marking the one file as added/updated to the DB.
     *
     * @param fileInfo
     *            the file to broadcast
     */
    protected FolderFilesChanged(FileInfo fileInfo, boolean forExt) {
        Reject.ifNull(fileInfo, "Fileinfo is null");

        folder = fileInfo.getFolderInfo();
        added = new FileInfo[]{fileInfo};
    }

    /**
     * @param fileInfo
     * @param useExt
     *            if {@link Externalizable}s should be used.
     * @return a changed message containing the {@link FileInfo} only.
     */
    public static FolderFilesChanged create(FileInfo fileInfo, boolean useExt) {
        if (useExt) {
            return new FolderFilesChangedExt(fileInfo);
        }
        return new FolderFilesChanged(fileInfo);
    }

    /**
     * Splits the filelist into small delta message. Splits into multiple
     * <code>FolderFilesChanged</code> messages
     *
     * @param foInfo
     *            the folder for the message
     * @param files
     *            the new fileinfos/dirinfos to include.
     * @param fileInfoFilter
     *            the filter to apply
     * @param useExt
     *            if {@link Externalizable}s should be used.
     * @return the messages
     */
    public static FolderFilesChanged[] create(FolderInfo foInfo,
        Collection<FileInfo> files, DiskItemFilter fileInfoFilter,
        boolean useExt)
    {
        Reject.ifNull(foInfo, "Folder info is null");
        Reject.ifNull(files, "Files is null");
        Reject.ifNull(fileInfoFilter, "FileInfoFilter is null");
        Reject.ifTrue(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE <= 0,
            "Unable to split filelist. nFilesPerMessage: "
                + Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);

        if (files.isEmpty()) {
            return null;
        }

        List<FolderFilesChanged> messages = new ArrayList<FolderFilesChanged>(
            files.size() / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
        int nDeltas = 0;
        int curMsgIndex = 0;
        int nDirs = 0;
        FileInfo[] messageFiles = new FileInfo[Math.min(
            Constants.FILE_LIST_MAX_FILES_PER_MESSAGE, files.size())];
        for (FileInfo fileInfo : files) {
            if (fileInfoFilter.isExcluded(fileInfo)) {
                continue;
            }
            if (fileInfo.isDiretory()) {
                nDirs++;
            }
            messageFiles[curMsgIndex] = fileInfo;
            curMsgIndex++;
            if (curMsgIndex >= messageFiles.length) {
                nDeltas++;
                FolderFilesChanged msg;

                if (useExt) {
                    msg = new FolderFilesChangedExt(foInfo);
                    msg.added = messageFiles;
                } else {
                    // Backward compatibility
                    msg = new FolderFilesChanged(foInfo);
                    if (messageFiles.length > 0 && messageFiles[0].isDeleted())
                    {
                        msg.removed = messageFiles;
                        if (log.isLoggable(Level.FINE)) {
                            log.log(Level.FINE, "Legacy/Removed: " + msg);
                        }
                    } else {
                        msg.added = messageFiles;
                    }
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

            FolderFilesChanged msg;
            if (useExt) {
                msg = new FolderFilesChangedExt(foInfo);
                msg.added = lastFiles;
            } else {
                // Backward compatibility
                msg = new FolderFilesChanged(foInfo);
                if (messageFiles.length > 0 && messageFiles[0].isDeleted()) {
                    msg.removed = lastFiles;
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Legacy/Removed: " + msg);
                    }
                } else {
                    msg.added = lastFiles;
                }
            }
            messages.add(msg);
        }

        if (log.isLoggable(Level.FINER)) {
            log.finer("Splitted folder files change into " + messages.size()
                + ", deltas: " + nDeltas + ", folder: " + foInfo + ", files: "
                + files.size() + ", dirs: " + nDirs + "\nSplitted msgs: "
                + messages);
        }

        return messages.toArray(new FolderFilesChanged[messages.size()]);
    }

    @Deprecated
    public FileInfo[] getRemoved() {
        return removed;
    }

    public FileInfo[] getFiles() {
        return added;
    }

    @Override
    public String toString() {
        if (removed != null) {
            return "FolderFilesChanged '" + folder.getLocalizedName() + "': "
                + (removed != null ? removed.length : 0)
                + " (removed/legacy) files";
        }
        return "FolderFilesChanged '" + folder.getLocalizedName() + "': "
            + (added != null ? added.length : 0) + " files";
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2DMessage(AbstractMessage mesg)
    {
      if(mesg instanceof FolderFilesChangedProto.FolderFilesChanged)
        {
          FolderFilesChangedProto.FolderFilesChanged proto =
            (FolderFilesChangedProto.FolderFilesChanged)mesg;

          /* Convert list back to array */
          int i = 0;

          this.added = new FileInfo[proto.getAddedCount()];

          for(FileInfoProto.FileInfo finfo : proto.getAddedList())
            {
              this.added[i++] = new FileInfo(finfo);
            }

          this.folder = new FolderInfo(proto.getFolder());
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2DMessage()
    {
      FolderFilesChangedProto.FolderFilesChanged.Builder builder =
        FolderFilesChangedProto.FolderFilesChanged.newBuilder();

      builder.setClassName("FolderFilesChanged");

      /* Convert array to list */
      for(FileInfo finfo : this.added)
        {
          builder.addAdded((FileInfoProto.FileInfo)finfo.toD2DMessage());
        }

      builder.setFolder((FolderInfoProto.FolderInfo)this.folder.toD2DMessage());

      return builder.build();
    }
}