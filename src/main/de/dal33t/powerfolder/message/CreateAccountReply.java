package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.message.clientserver.LoginReply;
import de.dal33t.powerfolder.message.clientserver.ReplyStatusCode;
import de.dal33t.powerfolder.protocol.CreateAccountReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class CreateAccountReply extends LoginReply {

    public CreateAccountReply(String replyCode, ReplyStatusCode replyStatusCode) {
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
        if (message instanceof CreateAccountReplyProto.CreateAccountReply) {
            CreateAccountReplyProto.CreateAccountReply proto = (CreateAccountReplyProto.CreateAccountReply) message;
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
        CreateAccountReplyProto.CreateAccountReply.Builder builder = CreateAccountReplyProto.CreateAccountReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        return builder.build();
    }

}
