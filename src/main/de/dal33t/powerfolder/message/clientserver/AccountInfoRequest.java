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
import de.dal33t.powerfolder.protocol.AccountInfoRequestProto;

public class AccountInfoRequest extends D2DRequestMessage implements D2DRequestToServer {

    private String accountId;
    private String nodeId;

    public AccountInfoRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public AccountInfoRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getNodeId() {
        return nodeId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof AccountInfoRequestProto.AccountInfoRequest) {
            AccountInfoRequestProto.AccountInfoRequest proto = (AccountInfoRequestProto.AccountInfoRequest) message;
            this.requestCode = proto.getRequestCode();
            this.accountId = proto.getAccountId();
            this.nodeId = proto.getNodeId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AccountInfoRequestProto.AccountInfoRequest.Builder builder = AccountInfoRequestProto.AccountInfoRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.accountId != null) builder.setAccountId(this.accountId);
        if (this.nodeId != null) builder.setNodeId(this.nodeId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.ACCOUNT_INFO_REQUEST;
    }

}
