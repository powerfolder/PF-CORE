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
 */
package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FileListRequestProto;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message to re-request the file list from another member.
 *
 * @author Sprajc
 */
public class FileListRequest extends FolderRelatedMessage implements D2DObject
{
    private static final long serialVersionUID = 100L;

    public FileListRequest(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        folder = foInfo;
    }

    @Override
    public String toString() {
        return "FileListRequest [folder=" + folder.getLocalizedName() + "/" + folder.id + "]";
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FileListRequestProto.FileListRequest.Builder builder = FileListRequestProto.FileListRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.folder != null) builder.setFolderId(this.folder.id);
        builder.setRecursive(true);
        return builder.build();
    }

}
