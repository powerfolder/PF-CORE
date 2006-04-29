package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;

/** 
 * A chat message about folders
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * 
 */
public class FolderChatMessage extends FolderRelatedMessage {
	// FIXME: Add correct serialversion id
	
    public String text;
    //public String style; //eg: "Color=123,234,111;bold"
    
    public FolderChatMessage(FolderInfo folderInfo, String text) {       
        this.folder = folderInfo;
        this.text = text;
    }
}
