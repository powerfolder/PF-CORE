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
 * $Id: FileInfo.java 5858 2008-11-24 02:30:33Z tot $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import de.dal33t.powerfolder.util.ImmutableList;
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
public class DefaultFileHistory extends FileHistory implements Serializable {

    private static final Logger log = Logger.getLogger(DefaultFileHistory.class
        .getName());
    private static final long serialVersionUID = 100L;

    private final ImmutableList<VersionedFile> history;

    private DefaultFileHistory(VersionedFile file) {
        super();
        Reject.ifNull(file, "file is null!");
        history = new ImmutableList<VersionedFile>(file);
    }

    private DefaultFileHistory(ImmutableList<VersionedFile> history) {
        this.history = history;
    }

    /*
     * (non-Javadoc)
     * @see de.dal33t.powerfolder.light.FileHistory#getFile()
     */
    public VersionedFile getFile() {
        return history.getHead();
    }

    /*
     * (non-Javadoc)
     * @see
     * de.dal33t.powerfolder.light.FileHistory#addVersion(de.dal33t.powerfolder
     * .light.VersionedFile)
     */
    public FileHistory addVersion(VersionedFile newFileInfo) {
        Reject.ifNull(newFileInfo, "newFileInfo is null");

        if (history.getTail() != null) {
            VersionedFile lastFileInfo = history.getTail().getHead();
            if (lastFileInfo.getFileInfo().getVersion() >= newFileInfo
                .getFileInfo().getVersion())
            {
                // Only merged histories are allowed to have FileInfos with the
                // same version in it!
                throw new IllegalStateException(
                    "Strange history add. Last file: "
                        + lastFileInfo.getFileInfo().toDetailString()
                        + ", added: "
                        + newFileInfo.getFileInfo().toDetailString());
            }
        }
        return new DefaultFileHistory(history.add(newFileInfo));
    }

    /*
     * (non-Javadoc)
     * @see
     * de.dal33t.powerfolder.light.FileHistory#hasConflictWith(de.dal33t.powerfolder
     * .light.DefaultFileHistory)
     */

    /*
     * (non-Javadoc)
     * @seede.dal33t.powerfolder.light.FileHistory#getCommonAncestor(de.dal33t.
     * powerfolder.light.DefaultFileHistory)
     */
    public VersionedFile getCommonAncestor(FileHistory other) {
        Set<VersionedFile> tmp = new HashSet<VersionedFile>();
        for (VersionedFile f : history) {
            tmp.add(f);
        }
        for (VersionedFile f : other) {
            if (tmp.contains(f)) {
                return f;
            }
        }
        return null;
    }

    public Iterator<VersionedFile> iterator() {
        return history.iterator();
    }
}
