package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountInfoProto;
import de.dal33t.powerfolder.protocol.GroupInfoProto;
import de.dal33t.powerfolder.protocol.InvitationCreateRequestProto;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.security.*;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class InvitationCreateRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private static final Logger LOG = Logger.getLogger(InvitationCreateRequest.class.getName());

    private String requestCode;
    /*
    The Permission protocol object stores subjects and objects for a single permission type.
    The Permission message cannot be changed easily,
    so the subjects and objects (as Permission objects) are stored directly in the InvitationCreateRequest message.
    This is an EXCEPTION to the definition that a message object has to store the same data as the protocol object.
     */
    private Collection<Permission> permissions;
    private Collection<D2DObject> subjects;

    /**
     * Serialization constructor
     */
    public InvitationCreateRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public InvitationCreateRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String replyCode) {
        this.requestCode = replyCode;
    }

    public Collection<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Collection<Permission> permissions) {
        this.permissions = permissions;
    }

    public Collection<D2DObject> getSubjects() {
        return subjects;
    }

    public void setSubjects(Collection<D2DObject> subjects) {
        this.subjects = subjects;
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
        if (mesg instanceof InvitationCreateRequestProto.InvitationCreateRequest) {
            InvitationCreateRequestProto.InvitationCreateRequest proto = (InvitationCreateRequestProto.InvitationCreateRequest) mesg;
            this.requestCode = proto.getRequestCode();
            this.permissions = new CopyOnWriteArrayList<Permission>();
            PermissionInfoProto.PermissionInfo permissionInfoProto = proto.getPermissionInfo();
            switch (permissionInfoProto.getPermissionType()) {
                case ADMIN:
                    this.permissions.add(new AdminPermission(permissionInfoProto));
                    break;
                case CHANGE_PREFERENCES:
                    this.permissions.add(new ChangePreferencesPermission(permissionInfoProto));
                    break;
                case CHANGE_TRANSFER_MODE:
                    this.permissions.add(new ChangeTransferModePermission(permissionInfoProto));
                    break;
                case COMPUTERS_APP:
                    this.permissions.add(new ComputersAppPermission(permissionInfoProto));
                    break;
                case CONFIG_APP:
                    this.permissions.add(new ConfigAppPermission(permissionInfoProto));
                    break;
                case FOLDER_ADMIN:
                    this.permissions.add(new FolderAdminPermission(permissionInfoProto));
                    break;
                case FOLDER_CREATE:
                    this.permissions.add(new FolderCreatePermission(permissionInfoProto));
                    break;
                case FOLDER_OWNER:
                    this.permissions.add(new FolderOwnerPermission(permissionInfoProto));
                    break;
                case FOLDER_READ:
                    this.permissions.add(new FolderReadPermission(permissionInfoProto));
                    break;
                case FOLDER_READ_WRITE:
                    this.permissions.add(new FolderReadWritePermission(permissionInfoProto));
                    break;
                case FOLDER_REMOVE:
                    this.permissions.add(new FolderRemovePermission(permissionInfoProto));
                    break;
                case GROUP_ADMIN:
                    this.permissions.add(new GroupAdminPermission(permissionInfoProto));
                    break;
                case ORGANIZATION_ADMIN:
                    this.permissions.add(new OrganizationAdminPermission(permissionInfoProto));
                    break;
                case ORGANIZATION_CREATE:
                    this.permissions.add(new OrganizationCreatePermission(permissionInfoProto));
                    break;
                case SYSTEM_SETTINGS:
                    this.permissions.add(new SystemSettingsPermission(permissionInfoProto));
                    break;
                case UNRECOGNIZED:
                    break;
                default:
                    break;
            }
            // The Permission message cannot store subjects, hence they are stored in the InvitationCreateRequest message
            if (permissionInfoProto.getSubjectsCount() == 1) {
                this.subjects = new CopyOnWriteArrayList<D2DObject>();
                try {
                    // Subjects can be any message so they need to be unpacked from com.google.protobuf.Any
                    com.google.protobuf.Any subject = permissionInfoProto.getSubjects(0);
                    String clazzName = subject.getTypeUrl().split("/")[1];
                    if (clazzName.equals("AccountInfo")) {
                        AccountInfoProto.AccountInfo accountInfo = subject.unpack(AccountInfoProto.AccountInfo.class);
                        this.subjects.add(new AccountInfo(accountInfo));
                    } else if (clazzName.equals("GroupInfo")) {
                        GroupInfoProto.GroupInfo groupInfo = subject.unpack(GroupInfoProto.GroupInfo.class);
                        this.subjects.add(new GroupInfo(groupInfo));
                    }
                } catch (InvalidProtocolBufferException | NullPointerException e) {
                    LOG.severe("Cannot unpack message: " + e);
                }
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
        InvitationCreateRequestProto.InvitationCreateRequest.Builder builder = InvitationCreateRequestProto.InvitationCreateRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setRequestCode(this.requestCode);
        if (this.permissions != null && this.permissions.size() == 1) {
            Permission permission = this.permissions.iterator().next();
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
