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
 * $Id: FileInfo.java 5858 2008-11-24 02:30:33Z tot $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import de.dal33t.powerfolder.util.Reject;

/**
 * First draft of the file archive metadata implementation.
 * <p>
 * http://dev.powerfolder.com/projects/powerfolder/wiki/Versioning
 * <P>
 * TRAC #388
 * <P>
 * TODO Add required methods.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FileHistory implements Serializable {

    private static final Logger log = Logger.getLogger(FileHistory.class
        .getName());
    private static final long serialVersionUID = 100L;

    private FileInfo file;
    private List<FileInfo> history;

    private FileHistory(FileInfo file) {
        super();
        this.file = file;
        this.history = new CopyOnWriteArrayList<FileInfo>();
    }

    /**
     * @return the FileInfo with the most recent version.
     */
    public FileInfo getFile() {
        return file;
    }

    /**
     * @return an unmodifiable reference to the internal file history list.
     *         Contents may change after get. Create a copy after get if you
     *         need a stable snapshot.
     */
    public List<FileInfo> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * Adds a new version to the history and replaces the most recent file.
     * 
     * @param newFileInfo
     *            the file version to add
     */
    public void addVersion(FileInfo newFileInfo) {
        Reject.ifNull(newFileInfo, "newFileInfo is null");
        if (!history.isEmpty()) {
            FileInfo lastFileInfo = history.get(history.size() - 1);
            if (lastFileInfo.getVersion() >= newFileInfo.getVersion()) {
                log.severe("Strange history add. Last file: "
                    + lastFileInfo.toDetailString() + ", added: "
                    + newFileInfo.toDetailString());
            }
        }
        history.add(newFileInfo);
        file = newFileInfo;
    }
}
