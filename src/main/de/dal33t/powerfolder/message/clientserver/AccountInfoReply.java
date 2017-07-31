package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountInfoProto;
import de.dal33t.powerfolder.protocol.AccountInfoReplyProto;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;

import java.util.Collection;

public class AccountInfoReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    private Account account;
    private Collection<FolderPermission> invitations;
    private long avatarLastModifiedDate;
    private AccountInfo accountInfo;

    /**
     * Serialization constructor
     */
    public AccountInfoReply() {
    }

    public AccountInfoReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public AccountInfoReply(String replyCode, ReplyStatusCode replyStatusCode, Account account, Collection<FolderPermission> invitations, long avatarLastModifiedDate) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.account = account;
        this.invitations = invitations;
        this.avatarLastModifiedDate = avatarLastModifiedDate;
    }

    public AccountInfoReply(String replyCode, ReplyStatusCode replyStatusCode, AccountInfo accountInfo, long avatarLastModifiedDate) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.accountInfo = accountInfo;
        this.avatarLastModifiedDate = avatarLastModifiedDate;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public AccountInfoReply(AbstractMessage mesg) {
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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Collection<FolderPermission> getInvitations() {
        return invitations;
    }

    public void setInvitations(Collection<FolderPermission> invitations) {
        this.invitations = invitations;
    }

    public long getAvatarLastModifiedDate() {
        return avatarLastModifiedDate;
    }

    public void setAvatarLastModifiedDate(long avatarLastModifiedDate) {
        this.avatarLastModifiedDate = avatarLastModifiedDate;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
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
        if (mesg instanceof AccountInfoReplyProto.AccountInfoReply) {
            AccountInfoReplyProto.AccountInfoReply proto = (AccountInfoReplyProto.AccountInfoReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.accountInfo = new AccountInfo(proto.getAccountInfo());
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
        AccountInfoReplyProto.AccountInfoReply.Builder builder = AccountInfoReplyProto.AccountInfoReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null)
            builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        // Send account as account info
        // Create AccountInfo message from Account
        AccountInfoProto.AccountInfo accountInfo = (AccountInfoProto.AccountInfo) this.account.toD2D();
        // Inject invitations into account info object
        AccountInfoProto.AccountInfo.Builder accountInfoBuilder = accountInfo.toBuilder();
        for (FolderPermission folderPermission : this.invitations) {
            PermissionInfoProto.PermissionInfo.Builder permissionInfoBuilder = ((PermissionInfoProto.PermissionInfo) folderPermission.toD2D()).toBuilder();
            permissionInfoBuilder.setIsInvitation(true);
            accountInfoBuilder.addPermissionInfos(permissionInfoBuilder.build());
        }
        // Inject avatarLastModifiedDate into account info object
        accountInfoBuilder.setAvatarLastModifiedDate(this.avatarLastModifiedDate);
        accountInfo = accountInfoBuilder.build();
        if (this.account != null) builder.setAccountInfo(accountInfo);
        else if (this.accountInfo != null)
            builder.setAccountInfo((AccountInfoProto.AccountInfo) this.accountInfo.toD2D());
        return builder.build();
    }
}
