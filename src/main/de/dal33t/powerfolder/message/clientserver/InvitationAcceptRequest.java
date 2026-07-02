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
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountInfoProto;
import de.dal33t.powerfolder.protocol.InvitationAcceptRequestProto;
import de.dal33t.powerfolder.protocol.NodeInfoProto;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.security.*;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class InvitationAcceptRequest extends InvitationCreateRequest {

    private boolean accept;

    public boolean isAccept() {
        return accept;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof InvitationAcceptRequestProto.InvitationAcceptRequest) {
            InvitationAcceptRequestProto.InvitationAcceptRequest proto = (InvitationAcceptRequestProto.InvitationAcceptRequest) message;
            this.requestCode = proto.getRequestCode();
            this.setPermissionInfo(proto.getPermissionInfo());
            this.accept = proto.getAccept();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        InvitationAcceptRequestProto.InvitationAcceptRequest.Builder builder = InvitationAcceptRequestProto.InvitationAcceptRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.permissions != null) builder.setPermissionInfo(this.getPermissionInfo());
        builder.setAccept(this.accept);
        return builder.build();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.INVITATION_ACCEPT_REQUEST;
    }

}
