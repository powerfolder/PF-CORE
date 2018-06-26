package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.protocol.ShareLinkChangeReplyProto;

public class ShareLinkChangeReply extends ShareLinkRemoveReply {

    public ShareLinkChangeReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ShareLinkChangeReplyProto.ShareLinkChangeReply) {
            ShareLinkChangeReplyProto.ShareLinkChangeReply proto = (ShareLinkChangeReplyProto.ShareLinkChangeReply) message;
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
        ShareLinkChangeReplyProto.ShareLinkChangeReply.Builder builder = ShareLinkChangeReplyProto.ShareLinkChangeReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        return builder.build();
    }

}
