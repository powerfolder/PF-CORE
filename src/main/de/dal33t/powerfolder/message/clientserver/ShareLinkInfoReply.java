package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;
import de.dal33t.powerfolder.protocol.ShareLinkInfoReplyProto;

public class ShareLinkInfoReply extends D2DReplyMessage {

    private ShareLinkInfo shareLinkInfo;

    public ShareLinkInfoReply() {
    }

    public ShareLinkInfoReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public ShareLinkInfoReply(String replyCode, ReplyStatusCode replyStatusCode, ShareLinkInfo shareLinkInfo) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.shareLinkInfo = shareLinkInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ShareLinkInfoReply(AbstractMessage message) {
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
        if (message instanceof ShareLinkInfoReplyProto.ShareLinkInfoReply) {
            ShareLinkInfoReplyProto.ShareLinkInfoReply proto = (ShareLinkInfoReplyProto.ShareLinkInfoReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
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
        ShareLinkInfoReplyProto.ShareLinkInfoReply.Builder builder = ShareLinkInfoReplyProto.ShareLinkInfoReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.shareLinkInfo != null) builder.setShareLinkInfo((ShareLinkInfoProto.ShareLinkInfo) this.shareLinkInfo.toD2D());
        return builder.build();
    }

}
