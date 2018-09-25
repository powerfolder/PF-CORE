package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DReplyFromServer;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.protocol.LoginReplyProto;
import de.dal33t.powerfolder.protocol.ServerInfoProto;

public class LoginReply extends D2DReplyMessage implements D2DReplyFromServer {

    protected String replyCode;
    private ServerInfo redirectServerInfo;

    public LoginReply() {
    }

    public LoginReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public LoginReply(String replyCode, StatusCode replyStatusCode, ServerInfo redirectServerInfo) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.redirectServerInfo = redirectServerInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public LoginReply(AbstractMessage message) {
        initFromD2D(message);
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
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
            this.redirectServerInfo = new ServerInfo(proto.getRedirectServerInfo());
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
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.redirectServerInfo != null) builder.setRedirectServerInfo((ServerInfoProto.ServerInfo) this.redirectServerInfo.toD2D());
        return builder.build();
    }

}
