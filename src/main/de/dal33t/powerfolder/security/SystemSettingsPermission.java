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
 * $Id: FolderAdminPermission.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.security;

/**
 * Permission that allows to change low level system settings of the
 * server/cloud.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class SystemSettingsPermission extends SingletonPermission {
    private static final long serialVersionUID = 100L;

    public static final SystemSettingsPermission INSTANCE = new SystemSettingsPermission();

    private SystemSettingsPermission() {
        super();
    }

    public boolean implies(Permission impliedPermision) {
        return false;
    }

    public String toString() {
        return getClass().getSimpleName();
    }
}
