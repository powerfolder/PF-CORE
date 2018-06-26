package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.ShareLinkRemoveReplyProto;

public class ShareLinkRemoveReply extends D2DReplyMessage {

    public ShareLinkRemoveReply() {
    }

    public ShareLinkRemoveReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkRemoveReply(AbstractMessage message) {
        initFromD2D(message);
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkRemoveReplyProto.ShareLinkRemoveReply) {
            ShareLinkRemoveReplyProto.ShareLinkRemoveReply proto = (ShareLinkRemoveReplyProto.ShareLinkRemoveReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ShareLinkRemoveReplyProto.ShareLinkRemoveReply.Builder builder = ShareLinkRemoveReplyProto.ShareLinkRemoveReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        return builder.build();
    }

}
