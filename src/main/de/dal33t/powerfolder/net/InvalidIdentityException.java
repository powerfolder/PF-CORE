/* $Id: InvalidIdentityException.java,v 1.4 2005/11/26 02:26:40 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

/**
 * Throws if a client did identify wrong, member should be invalidated
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
@SuppressWarnings("serial")
public class InvalidIdentityException extends ConnectionException {
    private ConnectionHandlerIntf from;

    /**
     * @param message
     */
    public InvalidIdentityException(String message, ConnectionHandlerIntf from) {
        super(message);
        if (from == null) {
            throw new NullPointerException("From is null");
        }
        this.from = from;
    }

    /**
     * Returns the connection handler from which the wrong identity was received
     * 
     * @return
     */
    public ConnectionHandlerIntf getFrom() {
        return from;
    }
}