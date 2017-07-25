package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountInfoRequestProto;
import de.dal33t.powerfolder.protocol.PermissionListRequestProto;

public class PermissionListRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String requestCode;
    private String folderId;

    /**
     * Serialization constructor
     */
    public PermissionListRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public PermissionListRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    /**
     * initFromD2DMessage
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof PermissionListRequestProto.PermissionListRequest) {
            PermissionListRequestProto.PermissionListRequest proto = (PermissionListRequestProto.PermissionListRequest) mesg;
            this.requestCode = proto.getRequestCode();
            this.folderId = proto.getFolderId();
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
        PermissionListRequestProto.PermissionListRequest.Builder builder = PermissionListRequestProto.PermissionListRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.folderId != null) builder.setFolderId(this.folderId);
        return builder.build();
    }
}
