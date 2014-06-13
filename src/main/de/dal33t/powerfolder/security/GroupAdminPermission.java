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

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class GroupAdminPermission implements Permission {

    private static final long serialVersionUID = 100L;
    public static final String ID_SEPARATOR = "_GP_";
    private Group group;

    public GroupAdminPermission(Group group) {
        Reject.ifNull(group, "Group is null");
        this.group = group;
    }

    public boolean implies(Permission impliedPermision) {
        return false;
    }

    public String getId() {
        return group.getOID() + ID_SEPARATOR + getClass().getSimpleName();
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
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
}
