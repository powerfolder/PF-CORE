package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.MemberInfo;


/**
 * Message for requesting and accepting a connection attempt by UDT.
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class UDTMessage extends Message {
	private static final long serialVersionUID = 100L;
	private Type type;
	private int port;
	private MemberInfo source;
	private MemberInfo destination;

	public enum Type {
		/**
		 * Requests a UDT connection
		 */
		SYN, 
		
		/**
		 * Acknowledges a UDT connection attempt
		 */
		ACK, 
		
		/**
		 * Rejects a UDT connection attempt
		 */
		NACK;
	};
	
	public UDTMessage(Type type, MemberInfo source, MemberInfo dest, int port) {
		this.type = type;
		this.source = source;
		destination = dest;
		this.port = port;
	}
	
	public Type getType() {
		return type;
	}
	
	/**
	 * The sender of this message will open a UDT socket on this port once the connection should be established. 
	 * @return the port the receiver should try to connect to 
	 */
	public int getPort() {
		return port;
	}

	public MemberInfo getSource() {
		return source;
	}

	public MemberInfo getDestination() {
		return destination;
	}
}
