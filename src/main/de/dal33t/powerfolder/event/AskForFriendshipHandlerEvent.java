package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.HashSet;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;

public class AskForFriendshipHandlerEvent extends EventObject {
    
    private HashSet<FolderInfo> joinedFolders;

    public AskForFriendshipHandlerEvent(Member source,
        HashSet<FolderInfo> joinedFolders)
    {
        super(source);
        
        this.joinedFolders = joinedFolders;
    }

    public HashSet<FolderInfo> getJoinedFolders() {
        return joinedFolders;
    }

    public Member getMember() {
        return (Member) getSource();
    }

}
