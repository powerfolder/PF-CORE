package de.dal33t.powerfolder.ui.information.folder.members;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.AccountInfo;
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
    private FolderPermission permission;
    private boolean defaultPermission;

    public FolderMember(Folder folder, Member member, AccountInfo accountInfo,
        FolderPermission permission, boolean defaultPermission)
    {
        super();
        this.folder = folder;
        this.member = member;
        this.accountInfo = accountInfo;
        this.permission = permission;
        this.defaultPermission = defaultPermission;
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

    public boolean isDefaultPermission() {
        return defaultPermission;
    }

    public FolderPermission getPermission() {
        return permission;
    }
}
