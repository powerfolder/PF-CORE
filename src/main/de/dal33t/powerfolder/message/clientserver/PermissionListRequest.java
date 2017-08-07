package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.PermissionListRequestProto;

public class PermissionListRequest extends D2DRequestMessage {

    private String folderId;

    public PermissionListRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public PermissionListRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getFolderId() {
        return folderId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof PermissionListRequestProto.PermissionListRequest) {
            PermissionListRequestProto.PermissionListRequest proto = (PermissionListRequestProto.PermissionListRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderId = proto.getFolderId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        PermissionListRequestProto.PermissionListRequest.Builder builder = PermissionListRequestProto.PermissionListRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.folderId != null) builder.setFolderId(this.folderId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.folderId != null;
    }

}
