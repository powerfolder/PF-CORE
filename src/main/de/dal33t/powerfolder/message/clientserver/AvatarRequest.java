package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.AvatarRequestProto;

public class AvatarRequest extends D2DRequestMessage {

    private String accountId;

    public AvatarRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public AvatarRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getAccountId() {
        return accountId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof AvatarRequestProto.AvatarRequest) {
            AvatarRequestProto.AvatarRequest proto = (AvatarRequestProto.AvatarRequest) message;
            this.requestCode = proto.getRequestCode();
            this.accountId = proto.getAccountId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AvatarRequestProto.AvatarRequest.Builder builder = AvatarRequestProto.AvatarRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.accountId != null) builder.setAccountId(this.accountId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.accountId != null;
    }

}
