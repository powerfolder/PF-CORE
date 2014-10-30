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
 * $Id: FileList.java 13570 2010-08-28 15:57:15Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ExternalizableUtil;

/**
 * #2072: {@link Externalizable} version of {@link FileList}
 */
public class FileListExt extends FileList implements Externalizable {
    private static final long serialVersionUID = -299244748325976914L;
    private static final long extVersionUID = 100L;

    public FileListExt() {
        super();
    }

    FileListExt(FolderInfo folderInfo, FileInfo[] files, int nDetlas2Follow) {
        super(folderInfo, files, nDetlas2Follow);
    }

    FileListExt(FolderInfo folderInfo) {
        super(folderInfo);
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
        nFollowingDeltas = in.readInt();
        if (in.readBoolean()) {
            files = new FileInfo[in.readInt()];
            for (int i = 0; i < files.length; i++) {
                files[i] = FileInfoFactory.readExt(in);
            }
        } else {
            files = null;
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);
        ExternalizableUtil.writeFolderInfo(out, folder);
        out.writeInt(nFollowingDeltas);
        out.writeBoolean(files != null);
        if (files != null) {
            out.writeInt(files.length);
            for (FileInfo fInfo : files) {
                fInfo.writeExternal(out);
            }
        }
    }
}
