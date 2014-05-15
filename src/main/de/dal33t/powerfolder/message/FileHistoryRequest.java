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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Message to request the remote FileHistory of a given FileInfo. <br>
 * TODO Currently it's only requesting one FileInfo, although we talked about
 * multiple infos. I'm not sure it's worth the effort though as there's no
 * guarantee that the number of requests will "usually" be greater than 1. Also,
 * the requester would have added overhead in trying to build good requests
 * messages if multiple remote clients have a shared set of "newest version"
 * files.
 *
 * @author "Dennis Waldherr"
 */
public class FileHistoryRequest extends Message {
    private static final long serialVersionUID = 100L;

    private final FileInfo fileInfo;

    public FileHistoryRequest(FileInfo fileInfo) {
        Reject.notNull(fileInfo, "fileInfo");
        this.fileInfo = fileInfo;
    }

    /**
     * Returns the requested FileInfo.
     *
     * @return
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    @Override
    public String toString() {
        return "FileHistoryRequest for " + fileInfo;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();

        Reject.ifNull(fileInfo, "fileInfo is null!");
    }
}
