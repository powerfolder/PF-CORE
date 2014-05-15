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
 * $Id: Permission.java 10353 2009-11-06 13:53:33Z tot $
 */
package de.dal33t.powerfolder.security;

/**
 * Different access level for resources (Folders, Groups, Users, etc.pp)
 *
 * @author sprajc
 */
public enum AccessMode {
    NO_ACCESS, READ, READ_WRITE, ADMIN, OWNER
}
