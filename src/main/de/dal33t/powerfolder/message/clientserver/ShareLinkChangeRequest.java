package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.protocol.ShareLinkChangeRequestProto;
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;

public class ShareLinkChangeRequest extends ShareLinkCreateRequest {

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkChangeRequestProto.ShareLinkChangeRequest) {
            ShareLinkChangeRequestProto.ShareLinkChangeRequest proto = (ShareLinkChangeRequestProto.ShareLinkChangeRequest) message;
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
        ShareLinkChangeRequestProto.ShareLinkChangeRequest.Builder builder = ShareLinkChangeRequestProto.ShareLinkChangeRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.shareLinkInfo != null) builder.setShareLinkInfo((ShareLinkInfoProto.ShareLinkInfo) this.shareLinkInfo.toD2D());
        return builder.build();
    }

}
