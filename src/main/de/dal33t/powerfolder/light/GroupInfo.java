/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.light;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.GroupInfoProto;
import de.dal33t.powerfolder.security.Group;

import java.io.Serializable;

/**
 * Leightweight reference/info object to an {@link Group}
 *
 * @author sprajc
 */
public class GroupInfo implements Serializable, D2DObject {

    /*
     * WARNING: Changing this value causes SIGNIFICANT problems and is virtually never the right
     * move: every serialized instance becomes unreadable (InvalidClassException) - stored folder
     * databases as well as wire messages from nodes still running the old value. Adding fields
     * is a serialization-COMPATIBLE change under the SAME UID; see the detailed note on
     * FolderInfo#serialVersionUID.
     */
    private static final long serialVersionUID = 100L;

    private String oid;

    private String displayName;

    /**
     * Serialization constructor
     */
    public GroupInfo() {
    }

    public GroupInfo(String oid, String displayName) {
        this.oid = oid;
        this.displayName = displayName;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public GroupInfo(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getOID() {
        return oid;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oid == null) ? 0 : oid.hashCode());
        return result;
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof GroupInfoProto.GroupInfo) {
            GroupInfoProto.GroupInfo proto  = (GroupInfoProto.GroupInfo)mesg;
            this.oid                = proto.getId();
            this.displayName        = proto.getName();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        GroupInfoProto.GroupInfo.Builder builder = GroupInfoProto.GroupInfo.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.oid != null) builder.setId(this.oid);
        if (this.getDisplayName() != null) builder.setName(this.getDisplayName());
        return builder.build();
    }
}
