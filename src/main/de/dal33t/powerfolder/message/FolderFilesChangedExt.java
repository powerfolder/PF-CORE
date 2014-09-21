/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: FolderFilesChanged.java 10689 2009-11-30 15:34:00Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ExternalizableUtil;

/**
 * A message which contains only the deltas of the folders list
 *
 * @see de.dal33t.powerfolder.message.FileList
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class FolderFilesChangedExt extends FolderFilesChanged implements
    Externalizable
{
    private static final long serialVersionUID = -2047091337743391978L;
    private static final long extVersionUID = 100L;
    private static final Logger LOG = Logger
        .getLogger(FolderFilesChangedExt.class.getName());

    public FolderFilesChangedExt() {
        // Serialization
        super();
    }

    FolderFilesChangedExt(FileInfo fileInfo) {
        super(fileInfo, true);
    }

    FolderFilesChangedExt(FolderInfo aFolder, FileInfo[] addedFiles) {
        super(aFolder, addedFiles);
    }

    FolderFilesChangedExt(FolderInfo folder) {
        super(folder);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", expected: " + extVersionUID);
        }
        folder = ExternalizableUtil.readFolderInfo(in);
        if (in.readBoolean()) {
            int len = in.readInt();
            added = new FileInfo[len];
            for (int i = 0; i < added.length; i++) {
                added[i] = FileInfoFactory.readExt(in);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);
        ExternalizableUtil.writeFolderInfo(out, folder);
        out.writeBoolean(added != null);
        if (added != null) {
            out.writeInt(added.length);
            for (int i = 0; i < added.length; i++) {
                added[i].writeExternal(out);
            }
        }
        if (removed != null && removed.length > 0
            && LOG.isLoggable(Level.SEVERE))
        {
            LOG.severe("Field removed-files is not empty! "
                + "This should not happen");
        }

    }
}