package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.Set;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;

public class AskForFriendshipHandlerEvent extends EventObject {

    private Set<FolderInfo> joinedFolders;

    public AskForFriendshipHandlerEvent(Member source,
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
