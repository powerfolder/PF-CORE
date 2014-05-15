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
