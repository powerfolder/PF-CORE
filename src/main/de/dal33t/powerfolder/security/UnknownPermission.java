/*
 * Copyright 2004 - 2016 Christian Sprajc. All rights reserved.
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
 * $Id: Constants.java 11478 2010-02-01 15:25:42Z tot $
 */
package de.dal33t.powerfolder.security;

/**
 * PFC-2905: Generic permission for unknown entry in database. This may happen on a
 * downgrade.
 * 
 * @author sprajc
 */
public class UnknownPermission implements Permission {
    private static final long serialVersionUID = 1L;
    private String id;

    public UnknownPermission(String id) {
        super();
        this.id = id;
    }

    @Override
    public boolean implies(Permission impliedPermision) {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "UnknownPermission [id=" + id + "]";
    }
}
