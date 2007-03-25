/* $Id: Invitation.java,v 1.5 2005/04/21 11:53:43 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * A Invitation to a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class Invitation extends FolderRelatedMessage {
    private static final long serialVersionUID = 101L;

    // Added invitor to invitation
    public MemberInfo invitor;
    public transient SyncProfile suggestedProfile;
    public transient String invitationText;

    public Invitation(FolderInfo folder, MemberInfo invitor) {
        this.folder = folder;
        this.invitor = invitor;
    }
    
    public Invitation(FolderInfo folder, MemberInfo invitor, String invitationText) {
		this(folder, invitor);
		this.invitationText = invitationText;
	}

    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
    }
}