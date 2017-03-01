/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
 * $Id: FolderRepository.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.PermissionProto;
import de.dal33t.powerfolder.protocol.StringMessageProto;
import de.dal33t.powerfolder.util.Util;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class OrganizationAdminPermission implements Permission, D2DObject {

    private static final Logger LOG = Logger.getLogger(OrganizationAdminPermission.class.getName());
    public static final String ID_SEPARATOR = "_OP_";
    public static OrganizationPermissionHelper ORGANIZATION_PERMISSION_HELPER;
    private static final long serialVersionUID = 100L;
    private String organizationOID;

    public OrganizationAdminPermission(String organizationOID) {
        this.organizationOID = organizationOID;
    }
    
    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public OrganizationAdminPermission(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public boolean implies(Permission impliedPermision) {
        if (ORGANIZATION_PERMISSION_HELPER != null) {
            return ORGANIZATION_PERMISSION_HELPER.implies(this,
                impliedPermision);
        }
        return false;
    }

    public String getId() {
        return organizationOID + ID_SEPARATOR + getClass().getSimpleName();
    }

    public String getOrganizationOID() {
        return organizationOID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((organizationOID == null) ? 0 : organizationOID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Permission))
            return false;
        Permission other = (Permission) obj;
        return Util.equals(getId(), other.getId());
    }

    public static interface OrganizationPermissionHelper {
        boolean implies(OrganizationAdminPermission organizationPermission,
            Permission impliedPermision);
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof PermissionProto.Permission) {
            PermissionProto.Permission proto = (PermissionProto.Permission)mesg;
            if (proto.getObjectsList().size() == 1) {
                try {
                    // Objects can be any message so they need to be unpacked from com.google.protobuf.Any
                    com.google.protobuf.Any object = proto.getObjects(0);
                    String clazzName = object.getTypeUrl().split("/")[1];
                    if (clazzName.equals("StringMessage")) {
                        StringMessageProto.StringMessage stringMessage = object.unpack(StringMessageProto.StringMessage.class);
                        this.organizationOID = stringMessage.getValue();
                    }
                } catch (InvalidProtocolBufferException | NullPointerException e) {
                    LOG.severe("Cannot unpack message: " + e);
                }
            }
        }
    }

    @Override
    public AbstractMessage toD2D() {
        PermissionProto.Permission.Builder builder = PermissionProto.Permission.newBuilder();
        builder.setClazzName("Permission");
        StringMessageProto.StringMessage.Builder stringMessageBuilder = StringMessageProto.StringMessage.newBuilder();
        stringMessageBuilder.setClazzName("StringMessage");
        stringMessageBuilder.setValue(this.organizationOID);
        // Objects can be any message so they need to be packed to com.google.protobuf.Any
        builder.setObjects(0, com.google.protobuf.Any.pack(stringMessageBuilder.build()));
        // Set permission enum
        builder.setPermissionType(PermissionProto.Permission.PermissionType.ORGANIZATION_ADMIN);
        return builder.build();
    }
}
