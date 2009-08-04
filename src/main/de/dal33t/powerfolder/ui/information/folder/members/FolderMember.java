package de.dal33t.powerfolder.ui.information.folder.members;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.security.FolderPermission;

/**
 * Temporary UI object to display the member of a folder.
 * 
 * @author sprajc
 */
public class FolderMember {
    private Folder folder;
    private Member member;
    private FolderPermission permission;

    public FolderMember(Folder folder, Member member,
        FolderPermission permission)
    {
        super();
        this.folder = folder;
        this.member = member;
        this.permission = permission;
    }

    public Folder getFolder() {
        return folder;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public FolderPermission getPermission() {
        return permission;
    }

    public void setPermission(FolderPermission permission) {
        this.permission = permission;
    }

}
