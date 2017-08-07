package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.protocol.PermissionInfoRequestProto;

public class PermissionInfoRequest extends InvitationCreateRequest {

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof PermissionInfoRequestProto.PermissionInfoRequest) {
            PermissionInfoRequestProto.PermissionInfoRequest proto = (PermissionInfoRequestProto.PermissionInfoRequest) message;
            this.requestCode = proto.getRequestCode();
            this.setPermissionInfo(proto.getPermissionInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        PermissionInfoRequestProto.PermissionInfoRequest.Builder builder = PermissionInfoRequestProto.PermissionInfoRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.permissions != null) builder.setPermissionInfo(this.getPermissionInfo());
        return builder.build();
    }

}
