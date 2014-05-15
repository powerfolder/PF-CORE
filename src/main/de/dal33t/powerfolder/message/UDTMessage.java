/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
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

	@Override
	public String toString() {
		return "{UDTMessage from: " + getSource() + ", to: " + getDestination() + ", type: " + getType()
			+ ", port: " + getPort() + "}";
	}
}
