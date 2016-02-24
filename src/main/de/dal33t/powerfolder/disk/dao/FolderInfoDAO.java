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
 * $Id$
 */
package de.dal33t.powerfolder.disk.dao;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderOwnerPermission;
import de.dal33t.powerfolder.util.db.GenericDAO;

/**
 * Data Access Object for FolderInfo objects.
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public interface FolderInfoDAO extends GenericDAO<FolderInfo> {
    /**
     * PFS-809
     *
     * @param folder
     * @return the number of potential entities (accounts or groups) which can
     *         access this folder by permissions. Does not count admins.
     */
    int countMembers(FolderInfo folder);

    /**
     * PFS-1786
     * 
     * @param folder
     * @return {@code True} if there is more then one account with an
     *         {@link FolderOwnerPermission} associated with the {@link Folder}.
     */
    boolean hasMultipleOwner(FolderInfo folder);
}
