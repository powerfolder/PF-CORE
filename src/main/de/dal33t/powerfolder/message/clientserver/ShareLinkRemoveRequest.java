package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.ShareLinkRemoveRequestProto;

public class ShareLinkRemoveRequest extends D2DRequestMessage {

    private String shareLinkId;

    public ShareLinkRemoveRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkRemoveRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getShareLinkId() {
        return shareLinkId;
    }

    public void setShareLinkId(String shareLinkId) {
        this.shareLinkId = shareLinkId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkRemoveRequestProto.ShareLinkRemoveRequest) {
            ShareLinkRemoveRequestProto.ShareLinkRemoveRequest proto = (ShareLinkRemoveRequestProto.ShareLinkRemoveRequest) message;
            this.requestCode = proto.getRequestCode();
            this.shareLinkId = proto.getShareLinkId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkRemoveRequestProto.ShareLinkRemoveRequest.Builder builder = ShareLinkRemoveRequestProto.ShareLinkRemoveRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.shareLinkId != null) builder.setShareLinkId(this.shareLinkId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.shareLinkId != null;
    }

}
