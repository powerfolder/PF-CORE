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
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;
import de.dal33t.powerfolder.protocol.ShareLinkListReplyProto;

import java.util.Collection;

public class ShareLinkListReply extends D2DReplyMessage implements D2DReplyFromServer {

    private Collection<ShareLinkInfo> shareLinkInfos;

    public ShareLinkListReply() {
    }

    public ShareLinkListReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public ShareLinkListReply(String replyCode, StatusCode replyStatusCode, Collection<ShareLinkInfo> shareLinkInfos) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.shareLinkInfos = shareLinkInfos;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkListReply(AbstractMessage message) {
        initFromD2D(message);
    }

    public Collection<ShareLinkInfo> getShareLinkInfos() {
        return shareLinkInfos;
    }

    public void setShareLinkInfos(Collection<ShareLinkInfo> shareLinkInfos) {
        this.shareLinkInfos = shareLinkInfos;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkListReplyProto.ShareLinkListReply) {
            ShareLinkListReplyProto.ShareLinkListReply proto = (ShareLinkListReplyProto.ShareLinkListReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
            for (ShareLinkInfoProto.ShareLinkInfo shareLinkInfoProto : proto.getShareLinkInfosList()) {
                this.shareLinkInfos.add(new ShareLinkInfo(shareLinkInfoProto));
            }
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkListReplyProto.ShareLinkListReply.Builder builder = ShareLinkListReplyProto.ShareLinkListReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.shareLinkInfos != null) {
            for (ShareLinkInfo shareLinkInfo : this.shareLinkInfos) {
                builder.addShareLinkInfos((ShareLinkInfoProto.ShareLinkInfo) shareLinkInfo.toD2D());
            }
        }
        return builder.build();
    }

}
