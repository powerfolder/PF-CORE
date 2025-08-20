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

import de.dal33t.powerfolder.disk.dao.FolderInfoDAO;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.security.*;
import de.dal33t.powerfolder.util.StackDump;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.logging.Loggable;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class PermissionUserType extends Loggable implements UserType {

    private static final int[] sqlTypes = {Types.VARCHAR};
    private static List<FolderInfoDAO> FOLDER_INFO_DAOS = new LinkedList<>();

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
        Exception outE = null;

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

            if (FOLDER_INFO_DAOS.isEmpty()) {
                throw new IllegalStateException("FolderInfoDAO not set! "
                    + "Maybe server is already shut down or not started?");
            }

            // get the associated FolderInfo
            FolderInfo fdInfo = null;
            for (FolderInfoDAO dao: FOLDER_INFO_DAOS) {
                fdInfo = dao.findByID(fiId);
                if (fdInfo != null) {
                    break;
                }
            }

            if (fdInfo == null) {
                logWarning("FolderInfo with ID " + fiId + " not found", new StackDump());
                fdInfo = FolderInfoFactory.lookupInstance(fiId);
            } else if (!fdInfo.isLookupInstance()) {
                // /PF-1790: Remove all this later...
                if (fdInfo.intern().getVersion() > fdInfo.getVersion()) {
                    logInfo(fdInfo.intern() + ": Found newer version is memory. in DB " + fdInfo);
                    fdInfo = fdInfo.intern();
                }
            } else {
                logInfo(fdInfo + ": Found lookup instance in DB.");
            }

            if (StringUtils.isBlank(fdInfo.getName()) && isFine()) {
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
                outE = e;
                if (isFine()) {
                    /*
                    Prevent LazyInitializationException  illegal access to loading collection
                    at org.hibernate.collection.PersistentBag.size(PersistentBag.java:248)
                    at de.dal33t.powerfolder.security.Account.toString(Unknown Source)
                    at de.dal33t.powerfolder.util.db.PermissionUserType.nullSafeGet(Unknown Source)
                     */
                    String who;
                    if (owner instanceof Account) {
                        who = ((Account) owner).getUsername();
                    } else {
                        who = owner.toString();
                    }
                    logFine(who + ": Unable to resolve permission: " + permissionID + ". " + e, e);
                }
            }
        }

        if (p == null) {
            // This may happen on a downgrade.
            if (isWarning()) {
                 /*
                    Prevent LazyInitializationException  illegal access to loading collection
                    at org.hibernate.collection.PersistentBag.size(PersistentBag.java:248)
                    at de.dal33t.powerfolder.security.Account.toString(Unknown Source)
                    at de.dal33t.powerfolder.util.db.PermissionUserType.nullSafeGet(Unknown Source)
                     */
                String who;
                if (owner instanceof Account) {
                    who = ((Account) owner).getUsername();
                } else {
                    who = owner.toString();
                }
                logWarning(who + ": Unknown permission with ID: " + permissionID + ". " + outE);
            }
            return new UnknownPermission(permissionID);
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
        if (fdao == null) {
            FOLDER_INFO_DAOS.clear();
            return;
        }
        FOLDER_INFO_DAOS.add(0, fdao);
        if (FOLDER_INFO_DAOS.size() > 1) {
            Logger log = Logger.getLogger(PermissionUserType.class.getName());
            log.warning("FolderInfoDAO was already set! The reset should not happen!");
        }
    }
}

