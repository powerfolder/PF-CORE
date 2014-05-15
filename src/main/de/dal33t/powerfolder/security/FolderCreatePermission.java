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

import de.dal33t.powerfolder.ConfigurationEntry;

/**
 * If the user is allowed to create new folders. Only considered if
 * {@link ConfigurationEntry#SECURITY_PERMISSIONS_STRICT}
 * <p>
 * Included since #1979 (4.2.3)
 *
 * @author sprajc
 */
public class FolderCreatePermission extends SingletonPermission {
    private static final long serialVersionUID = 100L;
    public final static Permission INSTANCE = new FolderCreatePermission();
}
