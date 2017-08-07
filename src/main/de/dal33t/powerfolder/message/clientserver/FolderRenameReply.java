package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.protocol.FolderRenameReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class FolderRenameReply extends FolderCreateReply {

    public FolderRenameReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof FolderRenameReplyProto.FolderRenameReply) {
            FolderRenameReplyProto.FolderRenameReply proto = (FolderRenameReplyProto.FolderRenameReply) mesg;
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
        FolderRenameReplyProto.FolderRenameReply.Builder builder = FolderRenameReplyProto.FolderRenameReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        return builder.build();
    }

}
