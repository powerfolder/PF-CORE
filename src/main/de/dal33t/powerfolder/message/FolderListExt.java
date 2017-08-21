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
 * $Id: FolderList.java 13255 2010-08-04 14:35:17Z tot $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;

import java.io.*;
import java.util.Collection;

/**
 * EXT version of: List of available folders
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderListExt extends FolderList implements Externalizable {
    private static final long serialVersionUID = -3861676003458215175L;
    private static final long extVersionUID = 101L;

    private final long writeExtVersionUID;

    public FolderListExt() {
        super();
        writeExtVersionUID = 100L;
    }

    public FolderListExt(Collection<FolderInfo> allFolders, String remoteMagicId)
    {
        super(allFolders, remoteMagicId);
        writeExtVersionUID = 100L;
    }

    public FolderListExt(Collection<FolderInfo> allFolders)
    {
        super(allFolders);
        writeExtVersionUID = extVersionUID;
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersionUID && extUID != 100) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", supported: " + extVersionUID + " and 100");
        }
        joinedMetaFolders = in.readBoolean();
        if (in.readBoolean()) {
            int len = in.readInt();
            secretFolders = new FolderInfo[len];
            for (int i = 0; i < secretFolders.length; i++) {
                // Dummy objects. Name must never be used.
                secretFolders[i] = new FolderInfo(null, in.readUTF());
            }
        }

        if (extUID == extVersionUID) {
            if (in.readBoolean()) {
                int len = in.readInt();
                folders = new FolderInfo[len];
                for (int i = 0; i < folders.length; i++) {
                    folders[i] = FolderInfo.readExt(in);
                }
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(writeExtVersionUID);
        out.writeBoolean(joinedMetaFolders);
        out.writeBoolean(secretFolders != null);
        if (secretFolders != null) {
            out.writeInt(secretFolders.length);
            for (FolderInfo foInfo : secretFolders) {
                out.writeUTF(foInfo.id);
            }
        }

        if (writeExtVersionUID <= 100) {
            return;
        }

        out.writeBoolean(folders != null);
        if (folders != null) {
            out.writeInt(folders.length);
            for (FolderInfo foInfo : folders) {
                foInfo.writeExternal(out);
            }
        }
    }
}