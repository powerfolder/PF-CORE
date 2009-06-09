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

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Administration permission on one folder.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderOwnerPermission extends FolderPermission {
    private static final long serialVersionUID = 100L;

    public FolderOwnerPermission(FolderInfo foInfo) {
        super(foInfo);
    }

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

}
