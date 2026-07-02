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
package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.FolderRenameRequestProto;

public class FolderRenameRequest extends FolderCreateRequest {

    private String name;

    public String getName() {
        return name;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderRenameRequestProto.FolderRenameRequest) {
            FolderRenameRequestProto.FolderRenameRequest proto = (FolderRenameRequestProto.FolderRenameRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderInfo = new FolderInfo(proto.getFolderInfo());
            this.name = proto.getName();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderRenameRequestProto.FolderRenameRequest.Builder builder = FolderRenameRequestProto.FolderRenameRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        if (this.name != null) builder.setName(this.name);
        return builder.build();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.FOLDER_RENAME_REQUEST;
    }

}
