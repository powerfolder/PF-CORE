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
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.D2DRequestToServer;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FolderCreateRequestProto;
import de.dal33t.powerfolder.protocol.FolderInfoProto;

public class FolderCreateRequest extends D2DRequestMessage implements D2DRequestToServer {

    protected FolderInfo folderInfo;

    public FolderCreateRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FolderCreateRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderCreateRequestProto.FolderCreateRequest) {
            FolderCreateRequestProto.FolderCreateRequest proto = (FolderCreateRequestProto.FolderCreateRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderInfo = new FolderInfo(proto.getFolderInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderCreateRequestProto.FolderCreateRequest.Builder builder = FolderCreateRequestProto.FolderCreateRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.folderInfo != null) builder.setFolderInfo((FolderInfoProto.FolderInfo) this.folderInfo.toD2D());
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.folderInfo != null;
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.FOLDER_CREATE_REQUEST;
    }

}
