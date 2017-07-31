package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AvatarReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class AvatarReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    public byte[] data;

    /**
     * Serialization constructor
     */
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
     * @param mesg Message to use data from
     **/
    public AvatarReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(String replyCode) {
        this.replyCode = replyCode;
    }

    public ReplyStatusCode getReplyStatusCode() {
        return replyStatusCode;
    }

    public void setReplyStatusCode(ReplyStatusCode replyStatusCode) {
        this.replyStatusCode = replyStatusCode;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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
        if (mesg instanceof AvatarReplyProto.AvatarReply) {
            AvatarReplyProto.AvatarReply proto = (AvatarReplyProto.AvatarReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.data = proto.getData().toByteArray();
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
        AvatarReplyProto.AvatarReply.Builder builder = AvatarReplyProto.AvatarReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null)
            builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.data != null) builder.setData(ByteString.copyFrom(this.data));
        return builder.build();
    }
}
