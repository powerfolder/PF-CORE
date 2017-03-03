package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.*;
import de.dal33t.powerfolder.security.*;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class PermissionRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private static final Logger LOG = Logger.getLogger(PermissionRequest.class.getName());

    private int requestCode;
    /*
    The Permission protocol object stores subjects and objects for a single permission type.
    The Permission message cannot be changed easily,
    so the subjects and objects (as Permission objects) are stored directly in the PermissionRequest message.
    This is an EXCEPTION to the definition that a message object has to store the same data as the protocol object.
     */
    private Collection<Permission> permissions;
    private Collection<D2DObject> subjects;

    /**
     * Serialization constructor
     */
    public PermissionRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public PermissionRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int replyCode) {
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
        if (mesg instanceof PermissionRequestProto.PermissionRequest) {
            PermissionRequestProto.PermissionRequest proto = (PermissionRequestProto.PermissionRequest) mesg;
            this.requestCode = proto.getRequestCode();
            this.permissions = new CopyOnWriteArrayList<Permission>();
            PermissionProto.Permission permissionProto = proto.getPermission();
            switch(permissionProto.getPermissionType()) {
                case ADMIN :
                    this.permissions.add(new AdminPermission(permissionProto));
                    break;
                case CHANGE_PREFERENCES :
                    this.permissions.add(new ChangePreferencesPermission(permissionProto));
                    break;
                case CHANGE_TRANSFER_MODE :
                    this.permissions.add(new ChangeTransferModePermission(permissionProto));
                    break;
                case COMPUTERS_APP :
                    this.permissions.add(new ComputersAppPermission(permissionProto));
                    break;
                case CONFIG_APP :
                    this.permissions.add(new ConfigAppPermission(permissionProto));
                    break;
                case FOLDER_ADMIN :
                    this.permissions.add(new FolderAdminPermission(permissionProto));
                    break;
                case FOLDER_CREATE :
                    this.permissions.add(new FolderCreatePermission(permissionProto));
                    break;
                case FOLDER_OWNER :
                    this.permissions.add(new FolderOwnerPermission(permissionProto));
                    break;
                case FOLDER_READ :
                    this.permissions.add(new FolderReadPermission(permissionProto));
                    break;
                case FOLDER_READ_WRITE :
                    this.permissions.add(new FolderReadWritePermission(permissionProto));
                    break;
                case FOLDER_REMOVE :
                    this.permissions.add(new FolderRemovePermission(permissionProto));
                    break;
                case GROUP_ADMIN :
                    this.permissions.add(new GroupAdminPermission(permissionProto));
                    break;
                case ORGANIZATION_ADMIN :
                    this.permissions.add(new OrganizationAdminPermission(permissionProto));
                    break;
                case ORGANIZATION_CREATE :
                    this.permissions.add(new OrganizationCreatePermission(permissionProto));
                    break;
                case SYSTEM_SETTINGS :
                    this.permissions.add(new SystemSettingsPermission(permissionProto));
                    break;
                case UNRECOGNIZED :
                    break;
                default :
                    break;
            }
            // The Permission message cannot store subjects, hence they are stored in the PermissionRequest message
            if (permissionProto.getSubjectsCount() == 1) {
                this.subjects = new CopyOnWriteArrayList<D2DObject>();
                try {
                    // Subjects can be any message so they need to be unpacked from com.google.protobuf.Any
                    com.google.protobuf.Any subject = permissionProto.getSubjects(0);
                    String clazzName = subject.getTypeUrl().split("/")[1];
                    if (clazzName.equals("AccountInfo")) {
                        AccountInfoProto.AccountInfo accountInfo = subject.unpack(AccountInfoProto.AccountInfo.class);
                        this.subjects.add(new AccountInfo(accountInfo));
                    }
                    else if (clazzName.equals("NodeInfo")) {
                        NodeInfoProto.NodeInfo nodeInfo = subject.unpack(NodeInfoProto.NodeInfo.class);
                        this.subjects.add(new MemberInfo(nodeInfo));
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
        PermissionRequestProto.PermissionRequest.Builder builder = PermissionRequestProto.PermissionRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setRequestCode(this.requestCode);
        if (this.permissions != null && this.permissions.size() == 1) {
            Permission permission = this.permissions.iterator().next();
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
