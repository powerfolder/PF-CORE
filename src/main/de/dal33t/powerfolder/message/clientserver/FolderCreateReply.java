package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.FolderCreateReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class FolderCreateReply extends D2DReplyMessage {

    public FolderCreateReply() {
    }

    public FolderCreateReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FolderCreateReply(AbstractMessage message) {
        initFromD2D(message);
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FolderCreateReplyProto.FolderCreateReply) {
            FolderCreateReplyProto.FolderCreateReply proto = (FolderCreateReplyProto.FolderCreateReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FolderCreateReplyProto.FolderCreateReply.Builder builder = FolderCreateReplyProto.FolderCreateReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        return builder.build();
    }

}
