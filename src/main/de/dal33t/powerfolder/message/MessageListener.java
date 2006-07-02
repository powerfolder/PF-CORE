/* $Id: MessageListener.java,v 1.1 2004/12/15 11:31:36 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Member;

/**
 * A message listener, which can by added to a member.
 * <p>
 * TODO Make this extend CoreListener
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 */
public interface MessageListener {
    /**
     * Handles the received message.
     * 
     * @param source
     *            the node where it came from
     * @param message
     *            the message received
     */
    public void handleMessage(Member source, Message message);
}
