package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.PermissionRemoveRequestProto;

public class PermissionRemoveRequest extends InvitationCreateRequest {

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof PermissionRemoveRequestProto.PermissionRemoveRequest) {
            PermissionRemoveRequestProto.PermissionRemoveRequest proto = (PermissionRemoveRequestProto.PermissionRemoveRequest) message;
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
        PermissionRemoveRequestProto.PermissionRemoveRequest.Builder builder = PermissionRemoveRequestProto.PermissionRemoveRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.permissions != null) builder.setPermissionInfo(this.getPermissionInfo());
        return builder.build();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.PERMISSION_REMOVE_REQUEST;
    }

}
