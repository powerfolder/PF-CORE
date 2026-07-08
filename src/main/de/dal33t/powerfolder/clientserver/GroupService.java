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
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.security.AccessMode;
import de.dal33t.powerfolder.security.Group;

public interface GroupService {

    /**
     * Deletes a group including its avatar. Requires admin permission.
     *
     * @param group the group to delete
     */
    void deleteGroup(Group group);

    /**
     * Sets a folder permission for a group, replacing any existing folder permissions of the group on that
     * folder. {@link AccessMode#NO_ACCESS} revokes without granting a new permission. Requires folder-admin
     * permission.
     *
     * @param groupInfo  the group to hold the permission
     * @param folderInfo the folder
     * @param accessMode the access mode of the permission
     * @return {@link StatusCode#OK} or {@link StatusCode#NOT_FOUND} if the group does not exist
     */
    StatusCode setFolderPermission(GroupInfo groupInfo, FolderInfo folderInfo, AccessMode accessMode);
}