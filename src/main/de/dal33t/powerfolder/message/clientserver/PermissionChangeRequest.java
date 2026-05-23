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
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.PermissionChangeRequestProto;

public class PermissionChangeRequest extends InvitationCreateRequest {

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof PermissionChangeRequestProto.PermissionChangeRequest) {
            PermissionChangeRequestProto.PermissionChangeRequest proto = (PermissionChangeRequestProto.PermissionChangeRequest) message;
            this.requestCode = proto.getRequestCode();
            this.setPermissionInfo(proto.getPermissionInfo());
        }
    }

    /**
     * toD2D
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public AbstractMessage toD2D() {
        PermissionChangeRequestProto.PermissionChangeRequest.Builder builder = PermissionChangeRequestProto.PermissionChangeRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.permissions != null) builder.setPermissionInfo(this.getPermissionInfo());
        return builder.build();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.PERMISSION_CHANGE_REQUEST;
    }

}
