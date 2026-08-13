/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;

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
    // PFC-3543: 103 adds the FolderInfo inheritsPermissions flag on top of 102 (parent).
    // PFS-5306: 104 adds the FolderInfo tags on top of 103.
    private static final long extVersionUID = 104L;

    /**
     * The wire version written for this message. It alone expresses which
     * FolderInfo fields follow, so there is a single source of truth:
     * <ul>
     *   <li>100: no folder list</li>
     *   <li>101: folder list, legacy FolderInfo protocol</li>
     *   <li>102: + parent/subfolder information</li>
     *   <li>103: + inheritsPermissions (interruption of permission inheritance)</li>
     *   <li>104: + tags (PFS-5306)</li>
     * </ul>
     */
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

    /**
     * @param remoteProtocolVersion the protocol version the remote peer negotiated
     *                              (see {@link Identity}). Determines which FolderInfo
     *                              fields are written: &gt;= 116 includes folder
     *                              tags (PFS-5306), &gt;= 115 includes
     *                              inheritsPermissions (PFC-3543), &gt;= 114 includes
     *                              parent/subfolder information, otherwise none.
     */
    public FolderListExt(Collection<FolderInfo> allFolders, int remoteProtocolVersion)
    {
        super(allFolders);
        if (remoteProtocolVersion >= Identity.PROTOCOL_VERSION_116) {
            writeExtVersionUID = 104L;
        } else if (remoteProtocolVersion >= Identity.PROTOCOL_VERSION_115) {
            writeExtVersionUID = 103L;
        } else if (remoteProtocolVersion >= Identity.PROTOCOL_VERSION_114) {
            writeExtVersionUID = 102L;
        } else {
            writeExtVersionUID = 101L;
        }
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID < 100L || extUID > extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", supported: 100.." + extVersionUID);
        }
        joinedMetaFolders = in.readBoolean();
        if (in.readBoolean()) {
            int len = in.readInt();
            secretFolders = new FolderInfo[len];
            for (int i = 0; i < secretFolders.length; i++) {
                // Dummy objects. Name must never be used.
                secretFolders[i] = FolderInfoFactory.lookupInstance(in.readUTF());
            }
        }

        if (extUID >= 101L) {
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
                foInfo.writeExternal(out, writeExtVersionUID >= 102L, writeExtVersionUID >= 103L,
                    writeExtVersionUID >= 104L);
            }
        }
    }
}
