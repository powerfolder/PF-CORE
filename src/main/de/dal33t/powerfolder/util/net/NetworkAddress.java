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

/**
 * This class represents an IP address within a network (determined by a subnet mask).
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public final class NetworkAddress {
	private Inet4Address address;
	private SubnetMask mask;

	public NetworkAddress(Inet4Address ip, Inet4Address subnet) {
		this(ip, new SubnetMask(subnet));
	}
	public NetworkAddress(Inet4Address ip, SubnetMask subnet) {
		address = ip;
		mask = subnet;
	}

	public Inet4Address getAddress() {
		return address;
	}
	public SubnetMask getMask() {
		return mask;
	}
	@Override
	public boolean equals(Object arg) {
		if (arg == null || arg.getClass() != NetworkAddress.class)
			return false;
		NetworkAddress na = (NetworkAddress) arg;
		return address.equals(na.address) && mask.equals(na.mask);
	}
	@Override
	public int hashCode() {
		return address.hashCode() ^ mask.hashCode();
	}
	@Override
	public String toString() {
		return address + "/" + mask;
	}
    /**
     * Returns true if the address specified by this object is valid.
     * If it's not valid it cannot be used to test subnets etc. with other addresses.
     * @return
     */
    public boolean isValid() {
        return !address.getHostAddress().equals("0.0.0.0") &&
            !address.getHostAddress().equals("255.255.255.255");
    }


}
