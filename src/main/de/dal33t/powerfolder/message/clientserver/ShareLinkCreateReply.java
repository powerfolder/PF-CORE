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
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DReplyFromServer;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.ShareLinkCreateReplyProto;
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;

public class ShareLinkCreateReply extends D2DReplyMessage implements D2DReplyFromServer {

    private ShareLinkInfo shareLinkInfo;

    public ShareLinkCreateReply() {
    }

    public ShareLinkCreateReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public ShareLinkCreateReply(String replyCode, StatusCode replyStatusCode, ShareLinkInfo shareLinkInfo) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.shareLinkInfo = shareLinkInfo;
    }

    public ShareLinkInfo getShareLinkInfo() {
        return shareLinkInfo;
    }

    public void setShareLinkInfo(ShareLinkInfo shareLinkInfo) {
        this.shareLinkInfo = shareLinkInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkCreateReply(AbstractMessage message) {
        initFromD2D(message);
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkCreateReplyProto.ShareLinkCreateReply) {
            ShareLinkCreateReplyProto.ShareLinkCreateReply proto = (ShareLinkCreateReplyProto.ShareLinkCreateReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
            this.shareLinkInfo = new ShareLinkInfo(proto.getShareLinkInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkCreateReplyProto.ShareLinkCreateReply.Builder builder = ShareLinkCreateReplyProto.ShareLinkCreateReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.shareLinkInfo != null)
            builder.setShareLinkInfo((ShareLinkInfoProto.ShareLinkInfo) this.shareLinkInfo.toD2D());
        return builder.build();
    }

}
