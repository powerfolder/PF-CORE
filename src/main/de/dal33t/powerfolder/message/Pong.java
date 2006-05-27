/* $Id: Pong.java,v 1.5 2005/10/28 21:20:22 schaatser Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.util.Format;

/**
 * Answer to a ping message
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class Pong extends Message {
    private static final long serialVersionUID = 100L;

    public String id;
    private long receiveTime;
    byte[] payload;

    public Pong() {
        // Serialisation constructor
    }

    public Pong(Ping ping) {
        payload = ping.payload;
        id = ping.id;
    }

    /**
     * Calculates the response time of ping pong. Ping has to match Ping !
     * 
     * @param ping
     * @return the response time in ms
     */
    public long took(Ping ping) {
        if (ping == null) {
            throw new NullPointerException("Ping is null");
        }
        if (!ping.id.equals(id)) {
            throw new IllegalArgumentException("Pong (ID: " + id
                + ") does not match Ping (ID: " + ping.id + ")");
        }
        if (receiveTime == 0) {
            receiveTime = System.currentTimeMillis();
        }
        if (ping.sendTime == 0) {
            throw new IllegalStateException("Ping has not been flagged as sent");
        }
        return receiveTime - ping.sendTime;
    }

    public String toString() {
        return "Pong"
            + ((payload != null) ? " " + Format.formatBytes(payload.length)
                + " bytes payload" : "");
    }
}