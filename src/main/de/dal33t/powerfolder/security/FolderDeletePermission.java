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
 * $Id: Permission.java 10353 2009-11-06 13:53:33Z tot $
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * PFS-2119: Permission to delete a folder.
 * 
 * @author sprajc
 */
public class FolderDeletePermission extends FolderPermission {
    private static final long serialVersionUID = 100L;

    /**
     * Construct externally with {@link FolderPermission#delete(FolderInfo)}
     *
     * @param foInfo
     */
    FolderDeletePermission(FolderInfo foInfo) {
        super(foInfo);
    }
    @Override
    public boolean implies(Permission impliedPermision) {
        return false;
    }

    @Override
    public String getName() {
        return Translation.get("permissions.folder.delete");
    }

    @Override
    public AccessMode getMode() {
        return null;
    }

}
