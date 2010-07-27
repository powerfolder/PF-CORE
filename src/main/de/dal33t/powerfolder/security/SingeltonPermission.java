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


/**
 * Permission for simple yes/no rights on actions without resources such as
 * folders.
 * 
 * @author sprajc
 */
public class SingeltonPermission implements Permission {

    private static final long serialVersionUID = 100L;

    public boolean implies(Permission impliedPermision) {
        return false;
    }

    public String getId() {
        return getClass().getSimpleName();
    }

    @Override
    public final int hashCode() {
        return 31;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return getClass() == obj.getClass();
    }

    public String toString() {
        return getClass().getSimpleName();
    }
}
