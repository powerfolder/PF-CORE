package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountInfoRequestProto;

public class AccountInfoRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String requestCode;
    private String accountId;
    private String nodeId;

    /**
     * Serialization constructor
     */
    public AccountInfoRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public AccountInfoRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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
        if (mesg instanceof AccountInfoRequestProto.AccountInfoRequest) {
            AccountInfoRequestProto.AccountInfoRequest proto = (AccountInfoRequestProto.AccountInfoRequest) mesg;
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
        AccountInfoRequestProto.AccountInfoRequest.Builder builder = AccountInfoRequestProto.AccountInfoRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setRequestCode(this.requestCode);
        return builder.build();
    }
}
