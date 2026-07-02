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
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;

/**
 * Permission to allow a user to create organizations using the Organizations
 * App.<br />
 * <br />
 * This user is allowed to create {@link Organization Organizations} and
 * {@link Account Users} of those Organizations, and grant
 * {@link OrganizationAdminPermission} to users of the organization he/she is
 * member of.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class OrganizationCreatePermission extends SingletonPermission {

    private static final long serialVersionUID = 100L;
    public static final OrganizationCreatePermission INSTANCE = new OrganizationCreatePermission();

    private OrganizationCreatePermission() {
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public OrganizationCreatePermission(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public boolean implies(Permission impliedPermision) {
        return false;
    }
}
