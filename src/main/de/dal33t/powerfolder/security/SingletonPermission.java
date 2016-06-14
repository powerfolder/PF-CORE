/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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

import java.lang.reflect.Field;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.PermissionProto;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Permission for simple yes/no rights on actions without resources such as
 * folders.
 *
 * @author sprajc
 */
public class SingletonPermission implements Permission, D2DObject {

    public static final String PERMISSION_PACKAGE_PREFIX = "de.dal33t.powerfolder.security.";
    private static final long serialVersionUID = 100L;

    public static SingletonPermission getByID(String permissionID) {
        if (StringUtils.isBlank(permissionID)) {
            return null;
        }
        // get class name
        String clazzName = PERMISSION_PACKAGE_PREFIX + permissionID;
        if (!clazzName.endsWith("Permission")) {
            clazzName += "Permission";
        }
        // choose/create the right permission implementation
        try {
            Class<?> permClass = Class.forName(clazzName);
            Object pObj;
            try {
                Field iField = permClass.getField("INSTANCE");
                pObj = iField.get(null);
            } catch (Exception e) {
                // Try to construct
                pObj = permClass.newInstance();
            }
            return (SingletonPermission) pObj;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean implies(Permission impliedPermision) {
        return false;
    }

    public String getId() {
        return getClass().getSimpleName();
    }

    @Override
    public final int hashCode() {
        return 31;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return getClass().isInstance(obj);
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
    }

    @Override
    public AbstractMessage toD2D() {
        PermissionProto.Permission.Builder builder = PermissionProto.Permission.newBuilder();
        builder.setClazzName("Permission");
        // Set permission enum
        if (this instanceof AdminPermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.ADMIN);
        }
        else if (this instanceof ChangePreferencesPermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.CHANGE_PREFERENCES);
        }
        else if (this instanceof ChangeTransferModePermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.CHANGE_TRANSFER_MODE);
        }
        else if (this instanceof ComputersAppPermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.COMPUTERS_APP);
        }
        else if (this instanceof ConfigAppPermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.CONFIG_APP);
        }
        else if (this instanceof FolderCreatePermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.FOLDER_CREATE);
        }
        else if (this instanceof FolderRemovePermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.FOLDER_REMOVE);
        }
        else if (this instanceof OrganizationCreatePermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.ORGANIZATION_CREATE);
        }
        else if (this instanceof SystemSettingsPermission) {
            builder.setPermissionType(PermissionProto.Permission.PermissionType.SYSTEM_SETTINGS);
        }
        return builder.build();
    }
}
