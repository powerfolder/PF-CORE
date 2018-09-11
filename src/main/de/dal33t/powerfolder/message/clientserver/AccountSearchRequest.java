package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.D2DRequestToServer;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.AccountSearchRequestProto;

public class AccountSearchRequest extends D2DRequestMessage implements D2DRequestToServer {

    private String keyword;

    public AccountSearchRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public AccountSearchRequest(AbstractMessage message) {
        initFromD2D(message);
    }


    public String getKeyword() {
        return keyword;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof AccountSearchRequestProto.AccountSearchRequest) {
            AccountSearchRequestProto.AccountSearchRequest proto = (AccountSearchRequestProto.AccountSearchRequest) message;
            this.requestCode = proto.getRequestCode();
            this.keyword = proto.getKeyword();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AccountSearchRequestProto.AccountSearchRequest.Builder builder = AccountSearchRequestProto.AccountSearchRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.keyword != null) builder.setKeyword(this.keyword);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.keyword != null;
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.ACCOUNT_SEARCH_REQUEST;
    }

}
