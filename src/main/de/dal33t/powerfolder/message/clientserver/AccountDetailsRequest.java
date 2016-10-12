package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountDetailsRequestProto;
import de.dal33t.powerfolder.protocol.MemberInfoProto;

public class AccountDetailsRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private MemberInfo memberInfo;

    /**
     * Serialization constructor
     */
    public AccountDetailsRequest() {
    }

    public AccountDetailsRequest(MemberInfo memberInfo) {
        this.setMemberInfo(memberInfo);
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public AccountDetailsRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public MemberInfo getMemberInfo() {
        return memberInfo;
    }

    public void setMemberInfo(MemberInfo memberInfo) {
        this.memberInfo = memberInfo;
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof AccountDetailsRequestProto.AccountDetailsRequest) {
            AccountDetailsRequestProto.AccountDetailsRequest proto = (AccountDetailsRequestProto.AccountDetailsRequest)mesg;
            this.memberInfo = new MemberInfo(proto.getMemberInfo());
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        AccountDetailsRequestProto.AccountDetailsRequest.Builder builder = AccountDetailsRequestProto.AccountDetailsRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setMemberInfo((MemberInfoProto.MemberInfo)this.memberInfo.toD2D());
        return builder.build();
    }
}
