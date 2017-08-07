package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.protocol.CreateAccountRequestProto;

public class CreateAccountRequest extends LoginRequest {

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof CreateAccountRequestProto.CreateAccountRequest) {
            CreateAccountRequestProto.CreateAccountRequest proto = (CreateAccountRequestProto.CreateAccountRequest) message;
            this.requestCode = proto.getRequestCode();
            this.username = proto.getUsername();
            this.password = proto.getPassword();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        CreateAccountRequestProto.CreateAccountRequest.Builder builder = CreateAccountRequestProto.CreateAccountRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.username != null) builder.setUsername(this.getUsername());
        if (this.password != null) builder.setPassword(this.getPassword());
        return builder.build();
    }

}
