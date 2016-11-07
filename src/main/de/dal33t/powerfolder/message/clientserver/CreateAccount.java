package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.CreateAccountProto;

public class CreateAccount extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private String username;
    private String password;

    /**
     * Serialization constructor
     */
    public CreateAccount() {
    }

    public CreateAccount(String username, String password) {
        this.setUsername(username);
        this.setPassword(password);
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public CreateAccount(AbstractMessage mesg) {
        initFromD2D(mesg);
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

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof CreateAccountProto.CreateAccount) {
            CreateAccountProto.CreateAccount proto = (CreateAccountProto.CreateAccount)mesg;
            this.setUsername(proto.getUsername());
            this.setPassword(proto.getPassword());
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        CreateAccountProto.CreateAccount.Builder builder = CreateAccountProto.CreateAccount.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setUsername(this.getUsername());
        builder.setPassword(this.getPassword());
        return builder.build();
    }
}
