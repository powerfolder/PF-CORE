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
import de.dal33t.powerfolder.protocol.ShareLinkListRequestProto;
import de.dal33t.powerfolder.util.StringUtils;

public class ShareLinkListRequest extends D2DRequestMessage implements D2DRequestToServer {

    private String folderId;

    public ShareLinkListRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkListRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getFolderId() {
        return folderId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkListRequestProto.ShareLinkListRequest) {
            ShareLinkListRequestProto.ShareLinkListRequest proto = (ShareLinkListRequestProto.ShareLinkListRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderId = proto.getFolderId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkListRequestProto.ShareLinkListRequest.Builder builder = ShareLinkListRequestProto.ShareLinkListRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.folderId != null) builder.setFolderId(this.folderId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && StringUtils.isNotBlank(this.folderId);
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.SHARE_LINK_LIST_REQUEST;
    }

}
