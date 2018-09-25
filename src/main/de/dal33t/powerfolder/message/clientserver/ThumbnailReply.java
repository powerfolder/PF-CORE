package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DReplyFromServer;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.ThumbnailReplyProto;

public class ThumbnailReply extends D2DReplyMessage implements D2DReplyFromServer {

    public byte[] data;

    public ThumbnailReply() {
    }

    public ThumbnailReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public ThumbnailReply(String replyCode, StatusCode replyStatusCode, byte[] data) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.data = data;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ThumbnailReply(AbstractMessage message) {
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
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ThumbnailReplyProto.ThumbnailReply) {
            ThumbnailReplyProto.ThumbnailReply proto = (ThumbnailReplyProto.ThumbnailReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
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
        ThumbnailReplyProto.ThumbnailReply.Builder builder = ThumbnailReplyProto.ThumbnailReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.data != null) builder.setData(ByteString.copyFrom(this.data));
        return builder.build();
    }

}
