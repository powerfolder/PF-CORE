package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.LoginRequestProto;

public class LoginRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String requestCode;
    private String username;
    private String password;

    /**
     * Serialization constructor
     */
    public LoginRequest() {
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public LoginRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        if(mesg instanceof LoginRequestProto.LoginRequest) {
            LoginRequestProto.LoginRequest proto = (LoginRequestProto.LoginRequest)mesg;
            this.requestCode = proto.getRequestCode();
            this.username = proto.getUsername();
            this.password = proto.getPassword();
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        LoginRequestProto.LoginRequest.Builder builder = LoginRequestProto.LoginRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.username != null) builder.setUsername(this.getUsername());
        if (this.password != null) builder.setPassword(this.getPassword());
        if (this.requestCode != null) builder.setPassword(this.getRequestCode());
        return builder.build();
    }
}
