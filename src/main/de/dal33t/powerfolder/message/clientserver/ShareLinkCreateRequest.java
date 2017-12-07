package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.ShareLinkCreateRequestProto;
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;

public class ShareLinkCreateRequest extends D2DRequestMessage {

    protected ShareLinkInfo shareLinkInfo;

    public ShareLinkCreateRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkCreateRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public ShareLinkInfo getShareLinkInfo() {
        return shareLinkInfo;
    }

    public void setShareLinkInfo(ShareLinkInfo shareLinkInfo) {
        this.shareLinkInfo = shareLinkInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkCreateRequestProto.ShareLinkCreateRequest) {
            ShareLinkCreateRequestProto.ShareLinkCreateRequest proto = (ShareLinkCreateRequestProto.ShareLinkCreateRequest) message;
            this.requestCode = proto.getRequestCode();
            this.shareLinkInfo = new ShareLinkInfo(proto.getShareLinkInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkCreateRequestProto.ShareLinkCreateRequest.Builder builder = ShareLinkCreateRequestProto.ShareLinkCreateRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.shareLinkInfo != null) builder.setShareLinkInfo((ShareLinkInfoProto.ShareLinkInfo) this.shareLinkInfo.toD2D());
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.shareLinkInfo != null;
    }

}
