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
package de.dal33t.powerfolder.ui.information.folder.members;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.security.FolderPermission;

/**
 * Temporary UI object to display the member of a folder.
 *
 * @author sprajc
 */
public class FolderMember {

    private Folder folder;
    private Member member;
    private AccountInfo accountInfo;
    private GroupInfo groupInfo;
    private FolderPermission permission;
    private boolean isPermissionChangeable;

    public FolderMember(Folder folder, Member member, AccountInfo accountInfo,
        GroupInfo groupInfo, FolderPermission permission, boolean isPermissionChangeable)
    {
        super();
        this.folder = folder;
        this.member = member;
        this.accountInfo = accountInfo;
        this.groupInfo = groupInfo;
        this.permission = permission;
        this.isPermissionChangeable = isPermissionChangeable;
    }

    public Folder getFolder() {
        return folder;
    }

    public Member getMember() {
        return member;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    public FolderPermission getPermission() {
        return permission;
    }

    public boolean isPermissionChangeable() {
        return isPermissionChangeable;
    }
}
