package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.LoginReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class LoginReply extends D2DReplyMessage {

    protected String replyCode;
    private String redirectUrl;

    public LoginReply() {
    }

    public LoginReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public LoginReply(String replyCode, ReplyStatusCode replyStatusCode, String redirectUrl) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.redirectUrl = redirectUrl;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public LoginReply(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof LoginReplyProto.LoginReply) {
            LoginReplyProto.LoginReply proto = (LoginReplyProto.LoginReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.redirectUrl = proto.getRedirectUrl();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        LoginReplyProto.LoginReply.Builder builder = LoginReplyProto.LoginReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.redirectUrl != null) builder.setRedirectUrl(this.redirectUrl);
        return builder.build();
    }

}
