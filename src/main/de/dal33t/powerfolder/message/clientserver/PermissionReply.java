package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.PermissionProto;
import de.dal33t.powerfolder.protocol.PermissionReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;
import de.dal33t.powerfolder.security.*;

public class PermissionReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    private Permission permission;

    /**
     * Serialization constructor
     */
    public PermissionReply() {
    }

    public PermissionReply(String replyCode, ReplyStatusCode replyStatusCode, Permission permission) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.permission = permission;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public PermissionReply(AbstractMessage mesg) {
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
        if (mesg instanceof PermissionReplyProto.PermissionReply) {
            PermissionReplyProto.PermissionReply proto = (PermissionReplyProto.PermissionReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            PermissionProto.Permission permissionProto = proto.getPermission();
            switch (permissionProto.getPermissionType()) {
                case ADMIN:
                    this.permission = new AdminPermission(permissionProto);
                    break;
                case CHANGE_PREFERENCES:
                    this.permission = new ChangePreferencesPermission(permissionProto);
                    break;
                case CHANGE_TRANSFER_MODE:
                    this.permission = new ChangeTransferModePermission(permissionProto);
                    break;
                case COMPUTERS_APP:
                    this.permission = new ComputersAppPermission(permissionProto);
                    break;
                case CONFIG_APP:
                    this.permission = new ConfigAppPermission(permissionProto);
                    break;
                case FOLDER_ADMIN:
                    this.permission = new FolderAdminPermission(permissionProto);
                    break;
                case FOLDER_CREATE:
                    this.permission = new FolderCreatePermission(permissionProto);
                    break;
                case FOLDER_OWNER:
                    this.permission = new FolderOwnerPermission(permissionProto);
                    break;
                case FOLDER_READ:
                    this.permission = new FolderReadPermission(permissionProto);
                    break;
                case FOLDER_READ_WRITE:
                    this.permission = new FolderReadWritePermission(permissionProto);
                    break;
                case FOLDER_REMOVE:
                    this.permission = new FolderRemovePermission(permissionProto);
                    break;
                case GROUP_ADMIN:
                    this.permission = new GroupAdminPermission(permissionProto);
                    break;
                case ORGANIZATION_ADMIN:
                    this.permission = new OrganizationAdminPermission(permissionProto);
                    break;
                case ORGANIZATION_CREATE:
                    this.permission = new OrganizationCreatePermission(permissionProto);
                    break;
                case SYSTEM_SETTINGS:
                    this.permission = new SystemSettingsPermission(permissionProto);
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
        PermissionReplyProto.PermissionReply.Builder builder = PermissionReplyProto.PermissionReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode)this.replyStatusCode.toD2D());
        if (this.permission != null) {
            Permission permission = this.permission;
            // Since the different permission classes do not have one common superclass we have to decide for each class separately
            if (permission instanceof FolderPermission) {
                builder.setPermission((PermissionProto.Permission) ((FolderPermission) permission).toD2D());
            } else if (permission instanceof GroupAdminPermission) {
                builder.setPermission((PermissionProto.Permission) ((GroupAdminPermission) permission).toD2D());
            } else if (permission instanceof OrganizationAdminPermission) {
                builder.setPermission((PermissionProto.Permission) ((OrganizationAdminPermission) permission).toD2D());
            } else if (permission instanceof SingletonPermission) {
                builder.setPermission((PermissionProto.Permission) ((SingletonPermission) permission).toD2D());
            }
        }
        return builder.build();
    }
}
