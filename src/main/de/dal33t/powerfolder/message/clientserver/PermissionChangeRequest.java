package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
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

}
