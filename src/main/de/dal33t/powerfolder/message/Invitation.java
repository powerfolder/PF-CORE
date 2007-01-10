/* $Id: Invitation.java,v 1.5 2005/04/21 11:53:43 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.crypto.spec.IvParameterSpec;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * A Invitation to a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class Invitation extends FolderRelatedMessage {
    private static final long serialVersionUID = 101L;
    // The prefix for pf links
    private static final String POWERFOLDER_LINK_PREFIX = "powerfolder://";
    private static final Logger LOG = Logger.getLogger(Invitation.class);

    // Added invitor to invitation
    public MemberInfo invitor;
    
    public String invitationText;

    public Invitation(FolderInfo folder, MemberInfo invitor) {
        this.folder = folder;
        this.invitor = invitor;
    }
    

    public Invitation(FolderInfo folder, MemberInfo invitor, String invitationText) {
		this(folder, invitor);
		this.invitationText = invitationText;
	}


	/**
     * @see de.dal33t.powerfolder.RConManager
     * @return the invitation as powerfolder link. FIXME: Replace characters in
     *         name and id with escape chars (%20)
     */
    public String toPowerFolderLink() {
        return "PowerFolder://|folder|" + Util.endcodeForURL(folder.name) + "|"
            + (folder.secret ? "S" : "P") + "|" + Util.endcodeForURL(folder.id)
            + "|" + folder.bytesTotal + "|" + folder.filesCount;
    }

    /**
     * Creates a powerfolder invitation from a powerfolder link.
     * 
     * @param link
     *            the link as string.
     * @return the invitation parsed from the link. or null if link was not
     *         parsable.
     */
    public static Invitation fromPowerFolderLink(String link) {
        Reject.ifBlank(link, "Link is empty");
        String plainLink = link.substring(POWERFOLDER_LINK_PREFIX.length());

        // Chop off ending /
        if (plainLink.endsWith("/")) {
            plainLink = plainLink.substring(1, plainLink.length() - 1);
        }

        try {
            // Parse link
            StringTokenizer nizer = new StringTokenizer(plainLink, "|");
            // Get type
            String type = nizer.nextToken();

            if (!"folder".equalsIgnoreCase(type)) {
                // Illegal, not supported
                LOG.error("Link not supported: " + link);
                return null;
            }

            // Decode the url form
            String name = Util.decodeFromURL(nizer.nextToken());
            boolean secret = nizer.nextToken().equalsIgnoreCase("s");
            String id = Util.decodeFromURL(nizer.nextToken());
            FolderInfo folder = new FolderInfo(name, id, secret);

            // Parse optional folder infos
            if (nizer.hasMoreElements()) {
                try {
                    folder.bytesTotal = Long.parseLong(nizer.nextToken());
                    if (nizer.hasMoreElements()) {
                        folder.filesCount = Integer.parseInt(nizer.nextToken());
                    }
                } catch (NumberFormatException e) {
                    LOG
                        .error("Unable to parse additonal folder info from link. "
                            + link);
                }
            }

            return new Invitation(folder, null);
        } catch (NoSuchElementException e) {
            LOG.error("Illegal link '" + link + "'");
        }
        return null;

    }

    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
    }
}