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
 * $Id$
 */
package de.dal33t.powerfolder.util.db;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import de.dal33t.powerfolder.disk.dao.FolderInfoDAO;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderAdminPermission;
import de.dal33t.powerfolder.security.FolderOwnerPermission;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.FolderReadPermission;
import de.dal33t.powerfolder.security.FolderReadWritePermission;
import de.dal33t.powerfolder.security.GroupAdminPermission;
import de.dal33t.powerfolder.security.OrganizationAdminPermission;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.security.SingletonPermission;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class PermissionUserType extends Loggable implements UserType {

    private static final int[] sqlTypes = {Types.VARCHAR};
    private static FolderInfoDAO FOLDER_INFO_DAO = null;

    public Object assemble(Serializable cached, Object owner)
        throws HibernateException
    {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        Permission p = null;
        String permissionID = rs.getString(names[0]);

        if (StringUtils.isBlank(permissionID)) {
            if (isFiner()) {
                logFiner("Permission ID is empty");
            }
            return null;
            // throw new IllegalStateException("Permission ID is empty");
        }
        // FolderPermissions (e. g. 4711_FP_FolderAdminPermission)
        else if (permissionID.contains(FolderPermission.ID_SEPARATOR)) {
            String[] idAndName = permissionID.split(FolderPermission.ID_SEPARATOR);
            String fiId = idAndName[0];
            String clazzName = idAndName[1];

            if (FOLDER_INFO_DAO == null) {
                throw new IllegalStateException("FolderInfoDAO not set! "
                    + "Maybe server is already shut down or not started?");
            }

            // get the associated FolderInfo
            FolderInfo fdInfo = FOLDER_INFO_DAO.findByID(fiId);

            if (fdInfo == null) {
                logSevere("FolderInfo with ID " + fiId + " not found!",
                    new RuntimeException());
                fdInfo = new FolderInfo(null, fiId);
            } else {
                fdInfo = fdInfo.intern();
            }

            if (StringUtils.isBlank(fdInfo.getName())) {
                logFine("Unknown folder with ID=" + fdInfo.getId());
            }

            // choose the right permission implementation
            if (clazzName.equals(FolderAdminPermission.class.getSimpleName())) {
                p = FolderPermission.admin(fdInfo);
            } else if (clazzName.equals(FolderOwnerPermission.class
                .getSimpleName()))
            {
                p = FolderPermission.owner(fdInfo);
            } else if (clazzName.equals(FolderReadPermission.class
                .getSimpleName()))
            {
                p = FolderPermission.read(fdInfo);
            } else if (clazzName.equals(FolderReadWritePermission.class
                .getSimpleName()))
            {
                p = FolderPermission.readWrite(fdInfo);
            }
        } else if (permissionID.contains(GroupAdminPermission.ID_SEPARATOR)) {
            String[] idAndName = permissionID.split(GroupAdminPermission.ID_SEPARATOR);
            String groupID = idAndName[0];
            String clazzName = idAndName[1];

            if (clazzName.equals(GroupAdminPermission.class.getSimpleName())) {
                p = new GroupAdminPermission(groupID);
            }
        } else if (permissionID
            .contains(OrganizationAdminPermission.ID_SEPARATOR))
        {
            String[] idAndName = permissionID
                .split(OrganizationAdminPermission.ID_SEPARATOR);
            String organizationID = idAndName[0];
            String clazzName = idAndName[1];

            if (clazzName.equals(OrganizationAdminPermission.class
                .getSimpleName()))
            {
                p = new OrganizationAdminPermission(organizationID);
            }
        }
        // SingletonPermissions (e. g. ChangePreferencesPermission)
        else {
            // get class name
            String clazzName = SingletonPermission.PERMISSION_PACKAGE_PREFIX
                + permissionID;

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
                p = (Permission) pObj;
            } catch (Exception e) {
                logSevere("Unable to resolve permission: " + permissionID, e);
            }
        }

        if (p == null) {
            // this should never happen
            logSevere("No permission could be created for ID " + permissionID);
            throw new IllegalStateException(
                "No permission could be created for ID " + permissionID);
        }

        return p;
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
        throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            Permission p = (Permission) value;
            st.setString(index, p.getId());

            // FolderInfos must not be stored here. Done in DAOs.
            // if (p instanceof FolderPermission) {
            // FolderPermission fp = (FolderPermission) p;
            // FOLDER_INFO_DAO.store(fp.getFolder());
            // }
        }
    }

    public Object replace(Object original, Object target, Object owner)
        throws HibernateException
    {
        return original;
    }

    public Class<?> returnedClass() {
        return Permission.class;
    }

    public int[] sqlTypes() {
        return sqlTypes;
    }

    public static void setFolderInfoDAO(FolderInfoDAO fdao) {
        if (FOLDER_INFO_DAO != null && fdao != null) {
            Logger log = Logger.getLogger(PermissionUserType.class.getName());
            log.severe("FolderInfoDAO was already set! The reset should not happen!");
        }
        FOLDER_INFO_DAO = fdao;
    }
}
