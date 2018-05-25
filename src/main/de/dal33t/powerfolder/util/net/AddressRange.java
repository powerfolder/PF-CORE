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
package de.dal33t.powerfolder.util.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class represents an IP range in a subnet.
 * Instances are immutable.
 *
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public final class AddressRange {
	private InetAddress start;
	private InetAddress end;

	public AddressRange(InetAddress start, InetAddress end) {
        if (!((start instanceof Inet4Address && end instanceof Inet4Address) ||
            (start instanceof Inet6Address && end instanceof Inet6Address)))
        {
   	        throw new IllegalArgumentException(String.format("Start and End Addresses are not of the same type: start is %s end is %s", start.getClass().getName(), end.getClass().getName()));
        }

		this.start = start;
		this.end = end;
	}

	public static AddressRange parseRange(String s)
        throws UnknownHostException
    {
	    String[] ips = s.split("-");
	    if (s.length() > 2) {
	        return null;
        }
        String startIP = ips[0];
	    String endIP = startIP;
	    if (s.length() == 1) {
	        endIP = ips[1];
        }
        return new AddressRange(InetAddress.getByName(startIP), InetAddress.getByName(endIP));
	}

	public InetAddress getEnd() {
		return end;
	}

	/**
	 * Checks if the given address is in range of the interval [start, end].
	 * @param addr the address to check
	 * @return true if the address is contained in this range
	 */
	public boolean contains(Inet4Address addr) {
		byte[] s = start.getAddress(), e = end.getAddress(), a = addr.getAddress();
		// Lexicographic compare
		for (int i = 0; i < s.length; i++) {
			int av = a[i] & 0xff, sv = s[i] & 0xff, ev = e[i] & 0xff;
			if (av > ev || av < sv) {
				return false;
			}
			if (av > sv && av < ev) {
				return true;
			} // else av == ev or av == sv=> continue
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((end == null) ? 0 : end.hashCode());
		result = PRIME * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AddressRange other = (AddressRange) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	public InetAddress getStart() {
		return start;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		String t = start.toString();
		b.append(t.substring(t.indexOf('/') + 1));
		if (!end.equals(start)) {
			b.append('-');
			t = end.toString();
			b.append(t.substring(t.indexOf('/') + 1));
		}
		return b.toString();
	}
}
