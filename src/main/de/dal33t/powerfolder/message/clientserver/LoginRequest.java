package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.LoginRequestProto;

public class LoginRequest extends D2DRequestMessage {

    protected String username;
    protected String password;
    protected String token;

    public LoginRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public LoginRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof LoginRequestProto.LoginRequest) {
            LoginRequestProto.LoginRequest proto = (LoginRequestProto.LoginRequest) message;
            this.requestCode = proto.getRequestCode();
            this.username = proto.getUsername();
            this.password = proto.getPassword();
            this.token = proto.getToken();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        LoginRequestProto.LoginRequest.Builder builder = LoginRequestProto.LoginRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.username != null) builder.setUsername(this.getUsername());
        if (this.password != null) builder.setPassword(this.getPassword());
        if (this.token != null) builder.setToken(this.getToken());
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.username != null && this.password != null;
    }

}
