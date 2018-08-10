package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.AccountChangeRequestProto;

import java.util.Collection;

public class AccountChangeRequest extends FolderCreateRequest {

    private String firstname;
    private String surname;
    private String telephone;
    private Collection<String> emails;

    public String getFirstname() {
        return firstname;
    }

    public String getSurname() {
        return surname;
    }

    public String getTelephone() {
        return telephone;
    }

    public Collection<String> getEmails() {
        return emails;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof AccountChangeRequestProto.AccountChangeRequest) {
            AccountChangeRequestProto.AccountChangeRequest proto = (AccountChangeRequestProto.AccountChangeRequest) message;
            this.requestCode = proto.getRequestCode();
            this.firstname = proto.getFirstname();
            this.surname = proto.getSurname();
            this.telephone = proto.getTelephone();

        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        AccountChangeRequestProto.AccountChangeRequest.Builder builder = AccountChangeRequestProto.AccountChangeRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.firstname != null) builder.setFirstname(this.firstname);
        if (this.surname != null) builder.setSurname(this.surname);
        if (this.firstname != null) builder.setFirstname(this.firstname);

        return builder.build();
    }

}
