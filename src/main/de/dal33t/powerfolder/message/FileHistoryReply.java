/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: $
 */
package de.dal33t.powerfolder.message;

import java.io.IOException;

import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Message to reply to a FileHistoryRequest message.
 *
 * @author "Dennis Waldherr"
 */
public class FileHistoryReply extends Message {
    private static final long serialVersionUID = 100L;

    private final FileHistory fileHistory;
    private final FileInfo requestFileInfo;

    public FileHistoryReply(FileHistory fileHistory, FileInfo requestFileInfo) {
        Reject.notNull(requestFileInfo, "requestFileInfo");
        this.fileHistory = fileHistory;
        this.requestFileInfo = requestFileInfo;
    }

    /**
     * Returns the FileHistory for a FileInfo
     *
     * @return a FileHistory or null if the FileInfo was unknown
     */
    public FileHistory getFileHistory() {
        return fileHistory;
    }

    /**
     * Returns the FileInfo the request was made with. This is for
     * identification purposes only, it doesn't mean the sender really has a
     * FileHistory or the file at all.
     *
     * @return
     */
    public FileInfo getRequestFileInfo() {
        return requestFileInfo;
    }

    @Override
    public String toString() {
        return "FileHistoryReply for " + requestFileInfo + " is " + fileHistory;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();

        Reject.ifNull(requestFileInfo, "fileInfo is null!");
    }
}
