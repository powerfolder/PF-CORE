package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.PermissionListReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;

import java.util.Map;

public class PermissionListReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    private Map<AccountInfo, FolderPermission> permissions;
    private Map<AccountInfo, FolderPermission> invitations;

    /**
     * Serialization constructor
     */
    public PermissionListReply() {
    }

    public PermissionListReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public PermissionListReply(String replyCode, ReplyStatusCode replyStatusCode, Account account) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public PermissionListReply(String replyCode, ReplyStatusCode replyStatusCode, Map<AccountInfo, FolderPermission> permissions, Map<AccountInfo, FolderPermission> invitations) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.permissions = permissions;
        this.invitations = invitations;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public PermissionListReply(AbstractMessage mesg) {
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

    /**
     * initFromD2DMessage
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof PermissionListReplyProto.PermissionListReply) {
            PermissionListReplyProto.PermissionListReply proto = (PermissionListReplyProto.PermissionListReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
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
        PermissionListReplyProto.PermissionListReply.Builder builder = PermissionListReplyProto.PermissionListReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null)
            builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        // Create one permission info object for each permission type
        if (this.permissions != null) {
            PermissionInfoProto.PermissionInfo.Builder folderAdminPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            PermissionInfoProto.PermissionInfo.Builder folderOwnerPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            PermissionInfoProto.PermissionInfo.Builder folderReadWritePermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            PermissionInfoProto.PermissionInfo.Builder folderReadPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            folderAdminPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderOwnerPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderReadWritePermissionInfoBuilder.setClazzName("PermissionInfo");
            folderReadPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderAdminPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_ADMIN);
            folderOwnerPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_OWNER);
            folderReadWritePermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_READ_WRITE);
            folderReadPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_READ);
            // Iterate over permission map and sort each permission into the correct permission info object
            for (Map.Entry<AccountInfo, FolderPermission> entry : this.permissions.entrySet()) {
                switch (entry.getValue().getClass().getSimpleName()) {
                    case "FolderAdminPermission":
                        folderAdminPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                    case "FolderOwnerPermission":
                        folderOwnerPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                    case "FolderReadWritePermission":
                        folderReadWritePermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                    case "FolderReadPermission":
                        folderReadPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                }
            }
            // Add permission info objects
            if (folderAdminPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderAdminPermissionInfoBuilder);
            if (folderOwnerPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderOwnerPermissionInfoBuilder);
            if (folderReadWritePermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderReadWritePermissionInfoBuilder);
            if (folderReadPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderReadPermissionInfoBuilder);
        }
        // Create one permission info object for each invitation permission type
        if (this.invitations != null) {
            PermissionInfoProto.PermissionInfo.Builder folderAdminInvitationPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            PermissionInfoProto.PermissionInfo.Builder folderOwnerInvitationPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            PermissionInfoProto.PermissionInfo.Builder folderReadWriteInvitationPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            PermissionInfoProto.PermissionInfo.Builder folderReadInvitationPermissionInfoBuilder = PermissionInfoProto.PermissionInfo.newBuilder();
            folderAdminInvitationPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderOwnerInvitationPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderReadWriteInvitationPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderReadInvitationPermissionInfoBuilder.setClazzName("PermissionInfo");
            folderAdminInvitationPermissionInfoBuilder.setIsInvitation(true);
            folderOwnerInvitationPermissionInfoBuilder.setIsInvitation(true);
            folderReadWriteInvitationPermissionInfoBuilder.setIsInvitation(true);
            folderReadInvitationPermissionInfoBuilder.setIsInvitation(true);
            folderAdminInvitationPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_ADMIN);
            folderOwnerInvitationPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_OWNER);
            folderReadWriteInvitationPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_READ_WRITE);
            folderReadInvitationPermissionInfoBuilder.setPermissionType(PermissionInfoProto.PermissionInfo.PermissionType.FOLDER_READ);
            // Iterate over invitation map and sort each invitation into the correct permission info object
            for (Map.Entry<AccountInfo, FolderPermission> entry : this.invitations.entrySet()) {
                switch (entry.getValue().getClass().getSimpleName()) {
                    case "FolderAdminPermission":
                        folderAdminInvitationPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                    case "FolderOwnerPermission":
                        folderOwnerInvitationPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                    case "FolderReadWritePermission":
                        folderReadWriteInvitationPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                    case "FolderReadPermission":
                        folderReadInvitationPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(entry.getKey().toD2D()));
                        break;
                }
            }
            // Add invitation permission info objects
            if (folderAdminInvitationPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderAdminInvitationPermissionInfoBuilder);
            if (folderOwnerInvitationPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderOwnerInvitationPermissionInfoBuilder);
            if (folderReadWriteInvitationPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderReadWriteInvitationPermissionInfoBuilder);
            if (folderReadInvitationPermissionInfoBuilder.getSubjectsCount() > 0)
                builder.addPermissionInfos(folderReadInvitationPermissionInfoBuilder);
        }
        return builder.build();
    }
}
