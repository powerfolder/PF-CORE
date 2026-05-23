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
import de.dal33t.powerfolder.d2d.D2DEvent;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.D2DRequestToServer;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.LoginRequestProto;

import java.util.ArrayList;
import java.util.Collection;

public class LoginRequest extends D2DRequestMessage implements D2DRequestToServer {

    protected String username;
    protected String password;
    protected String token;
    private long tosVersion;
    private Collection<String> nodeIds;

    public LoginRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public LoginRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public long getTosVersion() {
        return tosVersion;
    }

    public Collection<String> getNodeIds() {
        return nodeIds;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof LoginRequestProto.LoginRequest) {
            LoginRequestProto.LoginRequest proto = (LoginRequestProto.LoginRequest) message;
            this.requestCode = proto.getRequestCode();
            this.username = proto.getUsername();
            this.password = proto.getPassword();
            this.token = proto.getToken();
            this.tosVersion = proto.getTosVersion();
            this.nodeIds = new ArrayList<>();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        LoginRequestProto.LoginRequest.Builder builder = LoginRequestProto.LoginRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.username != null) builder.setUsername(this.getUsername());
        if (this.password != null) builder.setPassword(this.getPassword());
        if (this.token != null) builder.setToken(this.getToken());
        builder.setTosVersion(this.tosVersion);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.username != null && this.password != null;
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.LOGIN_REQUEST;
    }

}
