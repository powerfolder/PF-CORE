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
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;

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
    public FolderFilesChangedExt() {
        // Serialization
    }

    public FolderFilesChangedExt(FileInfo fileInfo) {
        super(fileInfo);
    }

    public FolderFilesChangedExt(FolderInfo aFolder, FileInfo[] addedFiles) {
        super(aFolder, addedFiles);
    }

    public FolderFilesChangedExt(FolderInfo folder) {
        super(folder);
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        if (in.readBoolean()) {
            folder = FolderInfo.readExt(in);
        } else {
            folder = null;
        }
        if (in.readBoolean()) {
            int len = in.readInt();
            added = new FileInfo[len];
            for (int i = 0; i < added.length; i++) {
                added[i] = FileInfoFactory.readExt(in);
            }
        }
        if (in.readBoolean()) {
            int len = in.readInt();
            removed = new FileInfo[len];
            for (int i = 0; i < removed.length; i++) {
                removed[i] = FileInfoFactory.readExt(in);
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(folder != null);
        folder.writeExternal(out);
        out.writeBoolean(added != null);
        if (added != null) {
            out.writeInt(added.length);
            for (int i = 0; i < added.length; i++) {
                added[i].writeExternal(out);
            }
        }
        out.writeBoolean(removed != null);
        if (removed != null) {
            out.writeInt(removed.length);
            for (int i = 0; i < removed.length; i++) {
                removed[i].writeExternal(out);
            }
        }
    }
}