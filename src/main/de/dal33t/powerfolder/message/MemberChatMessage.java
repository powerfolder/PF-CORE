package de.dal33t.powerfolder.message;

public class MemberChatMessage extends Message  {
	// FIXME: Add correct serialversion id
	
    public String text; 
    public MemberChatMessage(String text) {
        this.text = text;
    }
    
}
