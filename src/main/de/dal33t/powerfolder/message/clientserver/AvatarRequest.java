package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AvatarRequestProto;

public class AvatarRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String requestCode;
    private String accountId;

    /**
     * Serialization constructor
     */
    public AvatarRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public AvatarRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
        if (mesg instanceof AvatarRequestProto.AvatarRequest) {
            AvatarRequestProto.AvatarRequest proto = (AvatarRequestProto.AvatarRequest) mesg;
            this.requestCode = proto.getRequestCode();
            this.accountId = proto.getAccountId();
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
        AvatarRequestProto.AvatarRequest.Builder builder = AvatarRequestProto.AvatarRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setRequestCode(this.requestCode);
        if (this.accountId != null) builder.setAccountId(this.accountId);
        return builder.build();
    }
}
