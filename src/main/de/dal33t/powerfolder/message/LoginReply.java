package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.LoginReplyProto;

public class LoginReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private LoginReplyProto.LoginReply.StatusCode statusCode;

    /**
     * Serialization constructor
     */
    public LoginReply() {
    }

    public LoginReply(LoginReplyProto.LoginReply.StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/

    public LoginReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof LoginReplyProto.LoginReply) {
            LoginReplyProto.LoginReply proto = (LoginReplyProto.LoginReply)mesg;
            
            this.statusCode = proto.getStatusCode();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        LoginReplyProto.LoginReply.Builder builder = LoginReplyProto.LoginReply.newBuilder();

        builder.setClazzName(this.getClass().getSimpleName());
        builder.setStatusCode(this.statusCode);

        return builder.build();
    }
}
