/* $Id: Ping.java,v 1.4 2005/10/28 21:20:22 schaatser Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Format;

/**
 * A simple ping
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class Ping extends Message {
    private static final long serialVersionUID = 100L;
    private static final int DEFAULT_PAYLOAD_SIZE = 1000;

    long sendTime;
    byte[] payload;
    public String id;

    public Ping() {
        this(DEFAULT_PAYLOAD_SIZE);
    }

    public Ping(int payloadSize) {
        // generates a unique id, to identify pong response
        id = IdGenerator.makeId();
        if (payloadSize > 0) {
            payload = new byte[payloadSize];
            // fill payload
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (Math.random() * 256);
            }
        } else {
            throw new IllegalArgumentException("Payload size illegal: "
                + payloadSize);
        }
    }

    public void sent() {
        sendTime = System.currentTimeMillis();
    }

    /*
     * General
     */

    public String toString() {
        return "Ping"
            + ((payload != null) ? " " + Format.formatBytes(payload.length)
                + " bytes payload" : "");
    }
}