package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.CreateAccountReplyProto;

public class CreateAccountReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private CreateAccountReplyProto.CreateAccountReply.StatusCode statusCode;

    /**
     * Serialization constructor
     */
    public CreateAccountReply() {
    }

    public CreateAccountReply(CreateAccountReplyProto.CreateAccountReply.StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/

    public CreateAccountReply(AbstractMessage mesg) {
        initFromD2D(mesg);
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
        CreateAccountReplyProto.CreateAccountReply.Builder builder = CreateAccountReplyProto.CreateAccountReply.newBuilder();

        builder.setClazzName(this.getClass().getSimpleName());
        builder.setStatusCode(this.statusCode);

        return builder.build();
    }
}
