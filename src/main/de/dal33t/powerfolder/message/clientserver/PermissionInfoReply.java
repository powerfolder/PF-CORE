package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.PermissionInfoReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.security.*;

public class PermissionInfoReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    private Permission permission;

    /**
     * Serialization constructor
     */
    public PermissionInfoReply() {
    }

    public PermissionInfoReply(String replyCode, ReplyStatusCode replyStatusCode, Permission permission) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.permission = permission;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public PermissionInfoReply(AbstractMessage mesg) {
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

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
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
        if (mesg instanceof PermissionInfoReplyProto.PermissionInfoReply) {
            PermissionInfoReplyProto.PermissionInfoReply proto = (PermissionInfoReplyProto.PermissionInfoReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            PermissionInfoProto.PermissionInfo permissionInfoProto = proto.getPermissionInfo();
            switch (permissionInfoProto.getPermissionType()) {
                case ADMIN:
                    this.permission = new AdminPermission(permissionInfoProto);
                    break;
                case CHANGE_PREFERENCES:
                    this.permission = new ChangePreferencesPermission(permissionInfoProto);
                    break;
                case CHANGE_TRANSFER_MODE:
                    this.permission = new ChangeTransferModePermission(permissionInfoProto);
                    break;
                case COMPUTERS_APP:
                    this.permission = new ComputersAppPermission(permissionInfoProto);
                    break;
                case CONFIG_APP:
                    this.permission = new ConfigAppPermission(permissionInfoProto);
                    break;
                case FOLDER_ADMIN:
                    this.permission = new FolderAdminPermission(permissionInfoProto);
                    break;
                case FOLDER_CREATE:
                    this.permission = new FolderCreatePermission(permissionInfoProto);
                    break;
                case FOLDER_OWNER:
                    this.permission = new FolderOwnerPermission(permissionInfoProto);
                    break;
                case FOLDER_READ:
                    this.permission = new FolderReadPermission(permissionInfoProto);
                    break;
                case FOLDER_READ_WRITE:
                    this.permission = new FolderReadWritePermission(permissionInfoProto);
                    break;
                case FOLDER_REMOVE:
                    this.permission = new FolderRemovePermission(permissionInfoProto);
                    break;
                case GROUP_ADMIN:
                    this.permission = new GroupAdminPermission(permissionInfoProto);
                    break;
                case ORGANIZATION_ADMIN:
                    this.permission = new OrganizationAdminPermission(permissionInfoProto);
                    break;
                case ORGANIZATION_CREATE:
                    this.permission = new OrganizationCreatePermission(permissionInfoProto);
                    break;
                case SYSTEM_SETTINGS:
                    this.permission = new SystemSettingsPermission(permissionInfoProto);
                    break;
                case UNRECOGNIZED:
                    break;
                default:
                    break;
            }
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
        PermissionInfoReplyProto.PermissionInfoReply.Builder builder = PermissionInfoReplyProto.PermissionInfoReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode)this.replyStatusCode.toD2D());
        if (this.permission != null) {
            Permission permission = this.permission;
            // Since the different permission classes do not have one common superclass we have to decide for each class separately
            if (permission instanceof FolderPermission) {
                builder.setPermissionInfo((PermissionInfoProto.PermissionInfo) ((FolderPermission) permission).toD2D());
            } else if (permission instanceof GroupAdminPermission) {
                builder.setPermissionInfo((PermissionInfoProto.PermissionInfo) ((GroupAdminPermission) permission).toD2D());
            } else if (permission instanceof OrganizationAdminPermission) {
                builder.setPermissionInfo((PermissionInfoProto.PermissionInfo) ((OrganizationAdminPermission) permission).toD2D());
            } else if (permission instanceof SingletonPermission) {
                builder.setPermissionInfo((PermissionInfoProto.PermissionInfo) ((SingletonPermission) permission).toD2D());
            }
        }
        return builder.build();
    }
}
