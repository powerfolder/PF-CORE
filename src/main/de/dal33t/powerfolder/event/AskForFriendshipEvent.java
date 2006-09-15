package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.Set;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;

/** The event that tells which user joined which folders, used from NodeManager. */
public class AskForFriendshipEvent extends EventObject {

    private Set<FolderInfo> joinedFolders;

    /**
     * @param source
     *            the member that joined 1 or more folders
     * @param joinedFolders
     *            set of folders that the member joined
     */
    public AskForFriendshipEvent(Member source,
        Set<FolderInfo> joinedFolders)
    {
        super(source);

        this.joinedFolders = joinedFolders;
    }

    public Set<FolderInfo> getJoinedFolders() {
        return joinedFolders;
    }

    public Member getMember() {
        return (Member) getSource();
    }

}
