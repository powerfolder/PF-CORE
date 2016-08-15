package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.AccountProto;
import de.dal33t.powerfolder.protocol.LoginReplyProto;
import de.dal33t.powerfolder.security.Account;

public class LoginReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private boolean returnValue;
    private int statusCode;
    private Account account;

    /**
     * Serialization constructor
     */
    public LoginReply() {
    }

    public LoginReply(boolean returnValue, int statusCode, Account account) {
        this.returnValue = returnValue;
        this.statusCode = statusCode;
        this.account = account;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public LoginReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof LoginReplyProto.LoginReply) {
            LoginReplyProto.LoginReply proto = (LoginReplyProto.LoginReply)mesg;
            this.returnValue = proto.getReturnValue();
            this.statusCode = proto.getStatusCode();
            this.account = new Account(proto.getAccount());
        }
    }

    @Override
    public AbstractMessage toD2D() {
        LoginReplyProto.LoginReply.Builder builder = LoginReplyProto.LoginReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setReturnValue(this.returnValue);
        builder.setStatusCode(this.statusCode);
        if (this.account != null) {
            builder.setAccount((AccountProto.Account)this.account.toD2D());
        }
        return builder.build();
    }
}
