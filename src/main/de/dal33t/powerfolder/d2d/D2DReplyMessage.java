package de.dal33t.powerfolder.d2d;

import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.message.Message;

public abstract class D2DReplyMessage extends Message implements D2DObject, D2DEvent {

    protected String replyCode;
    protected StatusCode replyStatusCode;

    public String getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(String replyCode) {
        this.replyCode = replyCode;
    }

    public StatusCode getReplyStatusCode() {
        return replyStatusCode;
    }

    public void setReplyStatusCode(StatusCode replyStatusCode) {
        this.replyStatusCode = replyStatusCode;
    }

}