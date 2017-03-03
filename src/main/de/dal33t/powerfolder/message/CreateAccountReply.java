package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.clientserver.ReplyStatusCode;
import de.dal33t.powerfolder.protocol.CreateAccountReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class CreateAccountReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    /**
     * Serialization constructor
     */
    public CreateAccountReply() {
    }

    public CreateAccountReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public CreateAccountReply(AbstractMessage mesg) {
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

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof CreateAccountReplyProto.CreateAccountReply) {
            CreateAccountReplyProto.CreateAccountReply proto = (CreateAccountReplyProto.CreateAccountReply)mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        CreateAccountReplyProto.CreateAccountReply.Builder builder = CreateAccountReplyProto.CreateAccountReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode)this.replyStatusCode.toD2D());
        return builder.build();
    }
}
