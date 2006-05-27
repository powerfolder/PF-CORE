/* $Id: Invitation.java,v 1.5 2005/04/21 11:53:43 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Util;

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

    public Invitation(FolderInfo folder, MemberInfo invitor) {
        this.folder = folder;
        this.invitor = invitor;
    }

    /**
     * Returns the invitation as powerfolder link. FIXME: Replace characters in
     * name and id with escape chars (%20)
     * 
     * @see de.dal33t.powerfolder.RConManager
     * @return
     */
    public String toPowerFolderLink() {
        return "PowerFolder://|folder|" + Util.endcodeForURL(folder.name) + "|"
            + (folder.secret ? "S" : "P") + "|" + Util.endcodeForURL(folder.id)
            + "|" + folder.bytesTotal + "|" + folder.filesCount;
    }

    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
    }
}