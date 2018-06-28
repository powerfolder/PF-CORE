package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.PermissionListReplyProto;
import de.dal33t.powerfolder.protocol.PermissionTypeProto;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;

import java.io.Serializable;
import java.util.Map;

public class PermissionListReply extends D2DReplyMessage {

    private Map<Serializable, FolderPermission> permissions;
    private Map<AccountInfo, FolderPermission> invitations;

    public PermissionListReply() {
    }

    public PermissionListReply(String replyCode, StatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public PermissionListReply(String replyCode, StatusCode replyStatusCode, Account account) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public PermissionListReply(String replyCode, StatusCode replyStatusCode, Map<Serializable, FolderPermission> permissions, Map<AccountInfo, FolderPermission> invitations) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.permissions = permissions;
        this.invitations = invitations;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public PermissionListReply(AbstractMessage message) {
        initFromD2D(message);
    }


    public Map<Serializable, FolderPermission> getPermissions() {
        return permissions;
    }

    public Map<AccountInfo, FolderPermission> getInvitations() {
        return invitations;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof PermissionListReplyProto.PermissionListReply) {
            PermissionListReplyProto.PermissionListReply proto = (PermissionListReplyProto.PermissionListReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        PermissionListReplyProto.PermissionListReply.Builder builder = PermissionListReplyProto.PermissionListReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
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
            folderAdminPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_ADMIN);
            folderOwnerPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_OWNER);
            folderReadWritePermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_READ_WRITE);
            folderReadPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_READ);
            // Iterate over permission map and sort each permission into the correct permission info object
            for (Map.Entry<Serializable, FolderPermission> entry : this.permissions.entrySet()) {
                switch (entry.getValue().getClass().getSimpleName()) {
                    case "FolderAdminPermission":
                        folderAdminPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(((D2DObject) entry.getKey()).toD2D()));
                        break;
                    case "FolderOwnerPermission":
                        folderOwnerPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(((D2DObject) entry.getKey()).toD2D()));
                        break;
                    case "FolderReadWritePermission":
                        folderReadWritePermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(((D2DObject) entry.getKey()).toD2D()));
                        break;
                    case "FolderReadPermission":
                        folderReadPermissionInfoBuilder.addSubjects(com.google.protobuf.Any.pack(((D2DObject) entry.getKey()).toD2D()));
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
            folderAdminInvitationPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_ADMIN);
            folderOwnerInvitationPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_OWNER);
            folderReadWriteInvitationPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_READ_WRITE);
            folderReadInvitationPermissionInfoBuilder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_READ);
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
