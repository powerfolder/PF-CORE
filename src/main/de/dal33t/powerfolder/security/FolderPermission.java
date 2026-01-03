/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: FolderAdminPermission.java 5581 2008-11-03 03:26:24Z tot $
 */
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.PermissionTypeProto;
import de.dal33t.powerfolder.util.Reject;

import java.util.logging.Logger;

/**
 * A generic subclass for all permissions that are related to a certain folder.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class FolderPermission
  implements Permission, D2DObject
{
    private static final Logger LOG = Logger.getLogger(FolderPermission.class.getName());
    private static final long serialVersionUID = 100L;

    public static final String ID_SEPARATOR = "_FP_";
    public static final String PROPERTYNAME_FOLDER = "folder";

    protected FolderInfo folder;

    public FolderPermission() {
    }
    
    protected FolderPermission(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folderinfo is null");
        folder = foInfo;
    }

    public abstract String getName();

    public abstract AccessMode getMode();

    public final FolderInfo getFolder() {
        return folder;
    }

    @Override
    public String getId() {
        return folder.id + ID_SEPARATOR + getClass().getSimpleName();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((folder == null) ? 0 : folder.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FolderPermission))
            return false;
        final FolderPermission other = (FolderPermission) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }

    public static FolderPermission get(FolderInfo foInfo, AccessMode mode) {
        if (AccessMode.READ.equals(mode)) {
            return read(foInfo);
        } else if (AccessMode.READ_WRITE.equals(mode)) {
            return readWrite(foInfo);
        } else if (AccessMode.ADMIN.equals(mode)) {
            return admin(foInfo);
        } else if (AccessMode.OWNER.equals(mode)) {
            return owner(foInfo);
        } else {
            // No access / null.
            return null;
        }
    }
    
    public static FolderDeletePermission delete(FolderInfo foInfo) {
        return new FolderDeletePermission(foInfo);
    }

    public static FolderReadPermission read(FolderInfo foInfo) {
        return new FolderReadPermission(foInfo);
    }

    public static FolderReadWritePermission readWrite(FolderInfo foInfo) {
        return new FolderReadWritePermission(foInfo);
    }

    public static FolderAdminPermission admin(FolderInfo foInfo) {
        return new FolderAdminPermission(foInfo);
    }

    public static FolderOwnerPermission owner(FolderInfo foInfo) {
        return new FolderOwnerPermission(foInfo);
    }

    /**
     * Returns the additional {@link FolderPermission} types that are implied by
     * this permission.
     * <p>
     * The permission's own class is <b>always</b> implied implicitly and must
     * therefore NOT be returned here.
     * <p>
     * Returning {@code null} or an empty array means that no additional
     * folder permissions are implied.
     *
     * @return an array of additional implied folder permission types,
     *         or {@code null} if none are implied
     */
    protected Class<? extends FolderPermission>[] getImpliedFolderPermissionTypes() {
        return null; // default: no additional implied folder permissions
    }

    /**
     * Checks whether the given folder permission type is allowed to be implied
     * by this permission.
     * <p>
     * Rules:
     * <ul>
     *   <li>The permission's own class is always allowed (self-implication).</li>
     *   <li>Additional allowed permission types are defined by
     *       {@link #getImpliedFolderPermissionTypes()}.</li>
     * </ul>
     *
     * @param otherPermission the target permission to check
     * @return {@code true} if the permission type is allowed to be implied
     */
    private boolean isAllowedFolderPermission(FolderPermission otherPermission) {

        // Self-implication is always allowed
        if (this.getClass().equals(otherPermission.getClass())) {
            return true;
        }

        Class<? extends FolderPermission>[] impliedTypes =
                getImpliedFolderPermissionTypes();

        // No additional implied permissions defined
        if (impliedTypes == null || impliedTypes.length == 0) {
            return false;
        }

        for (Class<? extends FolderPermission> clazz : impliedTypes) {
            if (clazz.isInstance(otherPermission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether this permission implies another permission.
     * <p>
     * A folder permission implies another folder permission if:
     * <ul>
     *   <li>The target permission type is allowed
     *       (see {@link #getImpliedFolderPermissionTypes()}).</li>
     *   <li>Both permissions refer to the same top-level folder.</li>
     *   <li>The target folder is the same as, or located below, the source folder
     *       in the folder hierarchy.</li>
     *   <li>The target folder does not explicitly opt out of permission inheritance.</li>
     * </ul>
     *
     * @param impliedPermission the permission to check
     * @return {@code true} if this permission implies the given permission
     */
    @Override
    public boolean implies(Permission impliedPermission) {

        // Only folder permissions can be implied
        if (!(impliedPermission instanceof FolderPermission)) {
            return false;
        }

        FolderPermission otherPermission = (FolderPermission) impliedPermission;

        // Check whether the permission type is allowed
        if (!isAllowedFolderPermission(otherPermission)) {
            return false;
        }

        FolderInfo thisFolder = getFolder();
        FolderInfo otherFolder = otherPermission.getFolder();

        // Respect explicit opt-out on target subfolders
        if (otherFolder.isSubFolder() && !otherFolder.inheritsPermissions()) {
            return false;
        }

        return isSameOrBelow(thisFolder, otherFolder);
    }

    /**
     * Checks whether the target folder is the same as, or hierarchically below,
     * the source folder.
     * <p>
     * Folder hierarchy rules:
     * <ul>
     *   <li>Permissions never cross top-level folders.</li>
     *   <li>A top-level folder applies to all its subfolders.</li>
     *   <li>Subfolder permissions never apply upwards.</li>
     *   <li>Hierarchy comparison is based on {@code parentPath + folder name}.</li>
     * </ul>
     *
     * @param thisFolder  the source folder of the permission
     * @param otherFolder the target folder of the implied permission
     * @return {@code true} if {@code otherFolder} is the same as or below {@code thisFolder}
     */
    private boolean isSameOrBelow(FolderInfo thisFolder, FolderInfo otherFolder) {
        // Determine top-level folders
        FolderInfo thisTopFolder = thisFolder.isSubFolder() ? thisFolder.getParentFolder() : thisFolder;
        FolderInfo otherTopFolder = otherFolder.isSubFolder() ? otherFolder.getParentFolder() : otherFolder;

        // Different top-level folders -> no inheritance
        if (!thisTopFolder.equals(otherTopFolder)) {
            return false;
        }

        // Top-level folder applies to all subfolders
        if (thisFolder.isTopFolder()) {
            return true;
        }

        // Subfolder permissions never apply upwards
        if (otherFolder.isTopFolder()) {
            return false;
        }

        // Compare full paths (parentPath + name)
        String thisFullPath = buildFullPath(thisFolder);
        String otherFullPath = buildFullPath(otherFolder);

        // Same folder or descendant folder
        return otherFullPath.equals(thisFullPath) || otherFullPath.startsWith(thisFullPath + '/');
    }

    /**
     * Builds the logical full path of a folder based on its parent path and name.
     * <p>
     * The returned path is used exclusively for hierarchical comparison and does
     * not represent a filesystem path.
     *
     * @param folderInfo the folder to build the path for
     * @return the logical full path of the folder
     */
    private static String buildFullPath(FolderInfo folderInfo) {
        String parentPath = folderInfo.getParentPath();
        String name = folderInfo.getName();

        if (parentPath == null || parentPath.isEmpty()) {
            return name;
        }
        return parentPath + '/' + name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " on " + folder;
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof PermissionInfoProto.PermissionInfo) {
            PermissionInfoProto.PermissionInfo proto = (PermissionInfoProto.PermissionInfo)mesg;
            if (proto.getObjectsList().size() == 1) {
                try {
                    // Objects can be any message so they need to be unpacked from com.google.protobuf.Any
                    com.google.protobuf.Any object = proto.getObjects(0);
                    String clazzName = object.getTypeUrl().split("/")[1];
                    if (clazzName.equals("FolderInfo")) {
                        FolderInfoProto.FolderInfo folderInfo = object.unpack(FolderInfoProto.FolderInfo.class);
                        this.folder = new FolderInfo(folderInfo);
                    }
                } catch (InvalidProtocolBufferException | NullPointerException e) {
                    LOG.severe("Cannot unpack message: " + e);
                }
            }
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        PermissionInfoProto.PermissionInfo.Builder builder = PermissionInfoProto.PermissionInfo.newBuilder();
        builder.setClazzName("PermissionInfo");
        // Objects can be any message so they need to be packed to com.google.protobuf.Any
        builder.addObjects(com.google.protobuf.Any.pack(this.folder.toD2D()));
        // Set permission enum
        if (this instanceof FolderAdminPermission) {
            builder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_ADMIN);
        }
        else if (this instanceof FolderOwnerPermission) {
            builder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_OWNER);
        }
        else if (this instanceof FolderReadPermission) {
            builder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_READ);
        }
        else if (this instanceof FolderReadWritePermission) {
            builder.setPermissionType(PermissionTypeProto.PermissionType.FOLDER_READ_WRITE);
        }
        return builder.build();
    }

}
