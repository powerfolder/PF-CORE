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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * EXT version of: List of available folders
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderListExt extends FolderList implements Externalizable {
    private static final long serialVersionUID = -3861676003458215175L;
    private static final long extVersionUID = 100L;

    public FolderListExt() {
        super();
    }

    public FolderListExt(Collection<FolderInfo> allFolders, String remoteMagicId)
    {
        super(allFolders, remoteMagicId);
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
        joinedMetaFolders = in.readBoolean();
        if (in.readBoolean()) {
            int len = in.readInt();
            secretFolders = new FolderInfo[len];
            for (int i = 0; i < secretFolders.length; i++) {
                // Dummy objects. Name must never be used.
                secretFolders[i] = new FolderInfo(null, in.readUTF());
            }
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);
        out.writeBoolean(joinedMetaFolders);
        out.writeBoolean(secretFolders != null);
        if (secretFolders != null) {
            out.writeInt(secretFolders.length);
            for (FolderInfo foInfo : secretFolders) {
                out.writeUTF(foInfo.id);
            }
        }
    }

}