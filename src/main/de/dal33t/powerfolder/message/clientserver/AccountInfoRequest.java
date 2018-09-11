package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.D2DRequestToServer;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.AccountInfoRequestProto;

public class AccountInfoRequest extends D2DRequestMessage implements D2DRequestToServer {

    private String accountId;
    private String nodeId;

    public AccountInfoRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public AccountInfoRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getNodeId() {
        return nodeId;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof AccountInfoRequestProto.AccountInfoRequest) {
            AccountInfoRequestProto.AccountInfoRequest proto = (AccountInfoRequestProto.AccountInfoRequest) message;
            this.requestCode = proto.getRequestCode();
            this.accountId = proto.getAccountId();
            this.nodeId = proto.getNodeId();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AccountInfoRequestProto.AccountInfoRequest.Builder builder = AccountInfoRequestProto.AccountInfoRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.accountId != null) builder.setAccountId(this.accountId);
        if (this.nodeId != null) builder.setNodeId(this.nodeId);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.ACCOUNT_INFO_REQUEST;
    }

}
