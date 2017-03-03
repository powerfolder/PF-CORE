package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class ReplyStatusCode extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private ReplyStatusCodeProto.ReplyStatusCode.StatusCode statusCode;

    /**
     * Serialization constructor
     */
    public ReplyStatusCode() {
    }

    public ReplyStatusCode(ReplyStatusCodeProto.ReplyStatusCode.StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/

    public ReplyStatusCode(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public ReplyStatusCodeProto.ReplyStatusCode.StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(ReplyStatusCodeProto.ReplyStatusCode.StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @param  mesg  Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof ReplyStatusCodeProto.ReplyStatusCode) {
            ReplyStatusCodeProto.ReplyStatusCode proto = (ReplyStatusCodeProto.ReplyStatusCode)mesg;
            this.statusCode = proto.getStatusCode();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ReplyStatusCodeProto.ReplyStatusCode.Builder builder = ReplyStatusCodeProto.ReplyStatusCode.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setStatusCode(this.statusCode);
        return builder.build();
    }
}
