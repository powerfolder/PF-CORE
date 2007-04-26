/* $Id: Ping.java,v 1.4 2005/10/28 21:20:22 schaatser Exp $
 */
package de.dal33t.powerfolder.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * A simple ping
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class Ping extends Message {
    // #462: implements Externalizable {
    private static final long serialVersionUID = 100L;
    private static final int MESSAGE_VERSION = 100;

    long sendTime;
    byte[] payload;
    public String id;

    /**
     * Constructs a null/empty ping. Also called by serialization process.
     */
    public Ping() {
        this(-1);
    }

    /**
     * @param payloadSize
     *            the size of the random payload. values <0 initalize a complete
     *            empty ping, even without id
     */
    public Ping(int payloadSize) {
        if (payloadSize >= 0) {
            // generates a unique id, to identify pong response
            id = IdGenerator.makeId();
            payload = new byte[payloadSize];
            // fill payload
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (Math.random() * 256);
            }
        } else {
            id = null;
            payload = null;
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

    // Serialization protoype code: #462 **************************************

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        // Version check
        int serializatedMessageVersion = in.readInt();
        if (serializatedMessageVersion != MESSAGE_VERSION) {
            throw new ClassNotFoundException("Incompatible class versions for "
                + getClass().getName() + ". remote version: "
                + serializatedMessageVersion + ", own version: "
                + MESSAGE_VERSION);
        }
        int utfSize = in.readInt();
        if (utfSize >= 0) {
            id = in.readUTF();
        } else {
            id = null;
        }
        int payloadSize = in.readInt();
        if (payloadSize == -1) {
            payload = null;
        } else {
            payload = new byte[payloadSize];
            in.read(payload);
        }
        payload = (byte[]) in.readObject();
        sendTime = in.readLong();
        in.close();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(MESSAGE_VERSION);

        if (id != null) {
            out.write(id.length());
            out.writeUTF(id);
        } else {
            out.write(-1);
        }

        if (payload != null) {
            out.writeInt(payload.length);
            out.write(payload);
        } else {
            out.writeInt(-1);
        }

        out.writeLong(sendTime);
        out.close();
    }
}