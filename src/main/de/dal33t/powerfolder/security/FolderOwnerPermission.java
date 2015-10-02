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
 * $Id: FolderAdminPermission.java 6582 2009-01-26 17:02:32Z tot $
 */
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FolderPermissionProto;
import de.dal33t.powerfolder.util.Translation;

/**
 * Administration permission on one folder.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderOwnerPermission extends FolderPermission {
    private static final long serialVersionUID = 100L;

    /**
     * Construct externally with {@link FolderPermission#owner(FolderInfo)}
     *
     * @param foInfo
     */
    FolderOwnerPermission(FolderInfo foInfo) {
        super(foInfo);
    }

    @Override
    public String getName() {
        return Translation.get("permissions.folder.owner");
    }

    @Override
    public AccessMode getMode() {
        return AccessMode.OWNER;
    }

    @Override
    public boolean implies(Permission impliedPermision) {
        if (impliedPermision instanceof FolderReadPermission) {
            FolderReadPermission rp = (FolderReadPermission) impliedPermision;
            return rp.getFolder().equals(getFolder());
        } else if (impliedPermision instanceof FolderReadWritePermission) {
            FolderReadWritePermission rwp = (FolderReadWritePermission) impliedPermision;
            return rwp.getFolder().equals(getFolder());
        } else if (impliedPermision instanceof FolderAdminPermission) {
            FolderAdminPermission p = (FolderAdminPermission) impliedPermision;
            return p.getFolder().equals(getFolder());
        }
        return false;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FolderOwnerPermission))
            return false;
        final FolderOwnerPermission other = (FolderOwnerPermission) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof FolderPermissionProto.FolderPermission)
        {
          FolderPermissionProto.FolderPermission proto =
            (FolderPermissionProto.FolderPermission)mesg;

          this.folder = new FolderInfo(proto.getFolder());
        }
    }
}
