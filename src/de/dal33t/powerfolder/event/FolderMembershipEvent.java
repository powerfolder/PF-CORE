package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;

public class FolderMembershipEvent extends EventObject {
    private Member member;
    
    public FolderMembershipEvent(Folder source, Member member) {
        super(source);
        this.member = member;       
    }
    
    public Member getMember() {
        return member;
    }

}
