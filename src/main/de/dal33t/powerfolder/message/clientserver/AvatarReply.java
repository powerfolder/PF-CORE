package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.AvatarReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class AvatarReply extends D2DReplyMessage {

    public byte[] data;

    public AvatarReply() {
    }

    public AvatarReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public AvatarReply(String replyCode, ReplyStatusCode replyStatusCode, byte[] data) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.data = data;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public AvatarReply(AbstractMessage message) {
        initFromD2D(message);
    }

    public byte[] getData() {
        return data;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof AvatarReplyProto.AvatarReply) {
            AvatarReplyProto.AvatarReply proto = (AvatarReplyProto.AvatarReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.data = proto.getData().toByteArray();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AvatarReplyProto.AvatarReply.Builder builder = AvatarReplyProto.AvatarReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.data != null) builder.setData(ByteString.copyFrom(this.data));
        return builder.build();
    }

}
