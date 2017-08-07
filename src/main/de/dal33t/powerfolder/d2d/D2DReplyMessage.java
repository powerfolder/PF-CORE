package de.dal33t.powerfolder.d2d;

import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.clientserver.ReplyStatusCode;

public abstract class D2DReplyMessage extends Message implements D2DObject {

    protected String replyCode;
    protected ReplyStatusCode replyStatusCode;

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

}