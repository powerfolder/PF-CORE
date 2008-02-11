package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message that gets router through a third party node
 * <p>
 * TRAC #597.
 * <p>
 * A connection follows this workwork:
 * <p>
 * SYN ->
 * <p> <- ACK
 * <p>
 * DATA -> and <- DATA
 * <p>
 * EOF -> or <- EOF
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RelayedMessage extends Message {
    private static final long serialVersionUID = 100L;

    private long connectionId;
    private Type type;
    private MemberInfo source;
    private MemberInfo destination;
    private byte[] payload;

    public RelayedMessage(Type type, MemberInfo source, MemberInfo destination,
        long connectionId, byte[] payload)
    {
        super();
        Reject.ifNull(type, "Type is null");
        Reject.ifNull(source, "Source is null");
        Reject.ifNull(destination, "Destination is null");
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.connectionId = connectionId;
        this.payload = payload;
    }

    // Accessing **************************************************************

    public Type getType() {
        return type;
    }

    public MemberInfo getSource() {
        return source;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public MemberInfo getDestination() {
        return destination;
    }

    // Classes ****************************************************************

    public enum Type {
        /**
         * Contains the serialized message (zipped).
         */
        DATA_ZIPPED,

        /**
         * Indicates a request to open a new relayed connection to the
         * destination.
         * <p>
         * payload is null
         */
        SYN,

        /**
         * Accepts a new relayed connection from the source.
         * <p>
         * payload is null
         */
        ACK,

        /**
         * Rejects a new relayed connection from the source.
         * <p>
         * payload is null
         */
        NACK,

        /**
         * Indicates to shut down the connection.
         * <p>
         * payload is null
         */
        EOF;
    }

    // General ****************************************************************

    public String toString() {
        return "RelMsg {conId=" + connectionId + ", type=" + type + ", src="
            + source.nick + ", des=" + destination.nick + ", data="
            + (payload != null ? payload.length : "n/a") + "}";
    }
}
