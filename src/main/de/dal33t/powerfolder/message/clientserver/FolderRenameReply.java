package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.protocol.FolderRenameReplyProto;

public class FolderRenameReply extends FolderCreateReply {

    public FolderRenameReply(String replyCode, StatusCode replyStatusCode) {
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
        FolderRenameReplyProto.FolderRenameReply.Builder builder = FolderRenameReplyProto.FolderRenameReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        return builder.build();
    }

}
