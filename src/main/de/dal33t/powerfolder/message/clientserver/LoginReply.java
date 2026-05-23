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
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.protocol.LoginReplyProto;
import de.dal33t.powerfolder.protocol.ServerInfoProto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class LoginReply extends D2DReplyMessage implements D2DReplyFromServer {

    protected String replyCode;
    private ServerInfo redirectServerInfo;
    private Map<String, String> tokens;

    public LoginReply() {
    }

    public LoginReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public LoginReply(String replyCode, StatusCode replyStatusCode, ServerInfo redirectServerInfo) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.redirectServerInfo = redirectServerInfo;
    }

    public LoginReply(String replyCode, StatusCode replyStatusCode, Map<String, String> tokens) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.tokens = tokens;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public LoginReply(AbstractMessage message) {
        initFromD2D(message);
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof LoginReplyProto.LoginReply) {
            LoginReplyProto.LoginReply proto = (LoginReplyProto.LoginReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
            this.redirectServerInfo = new ServerInfo(proto.getRedirectServerInfo());
            this.tokens = proto.getTokensMap();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        LoginReplyProto.LoginReply.Builder builder = LoginReplyProto.LoginReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.redirectServerInfo != null) builder.setRedirectServerInfo((ServerInfoProto.ServerInfo) this.redirectServerInfo.toD2D());
        if (this.tokens != null) builder.putAllTokens(this.tokens);
        return builder.build();
    }

}
