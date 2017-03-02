package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountDetailsRequestProto;
import de.dal33t.powerfolder.protocol.MemberInfoProto;

public class AccountDetailsRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private int requestCode;

    /**
     * Serialization constructor
     */
    public AccountDetailsRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public AccountDetailsRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    /**
     * initFromD2DMessage
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof AccountDetailsRequestProto.AccountDetailsRequest) {
            AccountDetailsRequestProto.AccountDetailsRequest proto = (AccountDetailsRequestProto.AccountDetailsRequest) mesg;
            this.requestCode = proto.getRequestCode();
        }
    }

    /**
     * toD2D
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public AbstractMessage toD2D() {
        AccountDetailsRequestProto.AccountDetailsRequest.Builder builder = AccountDetailsRequestProto.AccountDetailsRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setRequestCode(this.requestCode);
        return builder.build();
    }
}
