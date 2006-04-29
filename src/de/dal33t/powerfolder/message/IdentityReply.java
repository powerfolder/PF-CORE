/* $Id: IdentityReply.java,v 1.2 2004/09/24 03:37:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

/**
 * Indicated the accept of the identity, which was sent
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class IdentityReply extends Message {
    private static final long serialVersionUID = 100L;

    public boolean accepted;
    public String message;

	public IdentityReply() {
		// Serialisation constructor
	}
	
    /**
     * Builds a new identity reply
     * @param accepted
     * @param message
     */
    private IdentityReply(boolean accepted, String message) {
        this.accepted = accepted;
        this.message = message;
    }

    /**
     * Builds a identity reply rejecting the
     * identity. a cause should be declared
     * 
     * @param why
     * @return
     */
    public static IdentityReply reject(String why) {
        return new IdentityReply(false, why);
    }

    /**
     * Builds a identity reply, accpeting identity
     * @return
     */
    public static IdentityReply accept() {
        return new IdentityReply(true, null);
    }

    public String toString() {
        String reply = accepted ? "accepted" : "rejected";
        return "Identity " + reply + (message == null ? "" : ": " + message);
    }
}
