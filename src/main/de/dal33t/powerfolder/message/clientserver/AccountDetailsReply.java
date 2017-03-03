package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountDetailsReplyProto;
import de.dal33t.powerfolder.protocol.AccountProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.security.Account;

public class AccountDetailsReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    private Account user;
    private long spaceUsed;
    private Boolean needsToAgreeToS;
    private long recycleBinSize;

    /**
     * Serialization constructor
     */
    public AccountDetailsReply() {
    }

    public AccountDetailsReply(String replyCode, ReplyStatusCode replyStatusCode, AccountDetails accountDetails) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.user = accountDetails.getAccount();
        this.spaceUsed = accountDetails.getSpaceUsed();
        this.needsToAgreeToS = accountDetails.needsToAgreeToS();
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public AccountDetailsReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(String replyCode) {
        this.replyCode = replyCode;
    }

    public ReplyStatusCode getReplyStatusCode() {
        return replyStatusCode;
    }

    public void setReplyStatusCode(ReplyStatusCode replyStatusCode) {
        this.replyStatusCode = replyStatusCode;
    }

    public Account getUser() {
        return user;
    }

    public void setUser(Account user) {
        this.user = user;
    }

    public long getSpaceUsed() {
        return spaceUsed;
    }

    public void setSpaceUsed(long spaceUsed) {
        this.spaceUsed = spaceUsed;
    }

    public Boolean getNeedsToAgreeToS() {
        return needsToAgreeToS;
    }

    public void setNeedsToAgreeToS(Boolean needsToAgreeToS) {
        this.needsToAgreeToS = needsToAgreeToS;
    }

    public long getRecycleBinSize() {
        return recycleBinSize;
    }

    public void setRecycleBinSize(long recycleBinSize) {
        this.recycleBinSize = recycleBinSize;
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
        if (mesg instanceof AccountDetailsReplyProto.AccountDetailsReply) {
            AccountDetailsReplyProto.AccountDetailsReply proto = (AccountDetailsReplyProto.AccountDetailsReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.user            = new Account(proto.getAccount());
            this.spaceUsed       = proto.getSpaceUsed();
            this.needsToAgreeToS = proto.getNeedsToAgreeToS();
            this.recycleBinSize  = proto.getRecycleBinSize();
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
        AccountDetailsReplyProto.AccountDetailsReply.Builder builder = AccountDetailsReplyProto.AccountDetailsReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.user != null) builder.setAccount((AccountProto.Account)this.user.toD2D());
        builder.setSpaceUsed(this.spaceUsed);
        if (this.needsToAgreeToS != null) builder.setNeedsToAgreeToS(this.needsToAgreeToS);
        builder.setRecycleBinSize(this.recycleBinSize);
        return builder.build();
    }
}
