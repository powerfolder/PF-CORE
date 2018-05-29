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

import de.dal33t.powerfolder.util.Reject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class represents an IP range in a subnet.
 * Instances are immutable.
 *
 * @author Dennis "Dante" Waldherr
 */
public final class AddressRange {
	private final InetAddress start;
	private final InetAddress end;

	private final int version;

    /**
     * Create a range of IP addresses. {@code start} and {@code end} can only be
     * either both IPv4 or IPv6 addresses
     *
     * @param start
     *     Start of address range
     * @param end
     *     End of address range
     *
     * @throws IllegalArgumentException
     *     If {@code start} and {@code end} are not both either IPv4 or IPv6
     *     addresses
     */
	AddressRange(@NotNull InetAddress start, @NotNull InetAddress end) {
        Reject.ifNull(start, "Start is null");
        Reject.ifNull(end, "End is null");

        if (!NetworkUtil.checkIfSameVersion(start, end)) {
            throw new IllegalArgumentException(String.format("Start and End Addresses are not of the same type: start is %s end is %s", start.getClass().getName(), end.getClass().getName()));
        }

        if (start instanceof Inet4Address) {
            version = 4;
        } else {
            version = 6;
        }

		this.start = start;
		this.end = end;
	}

    /**
     * Parses an IP range in the form of <ip address>-<ip address>.
     *
     * @param rangeAsString
     *     The string to parse
     *
     * @return an AddressRange
     *
     * @throws UnknownHostException
     *     If {@link InetAddress#getByName(String)} fails
     * @throws IllegalArgumentException
     *     if there are more then two IP Addresses specified in {@code
     *     rangeAsString}
     */
	public static @NotNull AddressRange parseRange(@NotNull String rangeAsString)
        throws UnknownHostException, IllegalArgumentException
    {
	    String[] ips = rangeAsString.split("-");
	    if (ips.length > 2) {
	        throw new IllegalArgumentException("To many addresses, should be 2 at max but are " + ips.length);
        }
        String startIP = ips[0];
	    // default endIP to startIP
	    String endIP = startIP;
	    if (ips.length == 2) {
	        endIP = ips[1];
        }
        return new AddressRange(InetAddress.getByName(startIP), InetAddress.getByName(endIP));
	}

    @Contract(pure = true)
    public @NotNull InetAddress getStart() {
        return start;
    }

    @Contract(pure = true)
    public @NotNull InetAddress getEnd() {
		return end;
	}

    @Contract(pure = true)
    public int getProtocolVersion() {
	    return version;
	}

    /**
     * Checks if the given address is in range of the interval [start, end].
     *
     * @param address
     *     the address to check, can be {@code null}
     *
     * @return {@code True} if the address is contained in this range, {@code
     * false} otherwise. If {@code address} is {@code null}, {@code false}
     *
     * @throws IllegalArgumentException
     *     If an {@link Inet4Address} is passed for an IPv6 AddressRange or an
     *     {@link Inet6Address} is passed for an IPv4 AddressRange
     */
	public boolean contains(@Nullable InetAddress address) {
		if (address == null) {
			return false;
		}

        checkProtocolVersion(address);

        // Lexicographic compare
        return NetworkUtil.isAddressInRange(start.getAddress(), end.getAddress(),
            address.getAddress());
	}

    /**
     * Check if {@code address} is the same IP version as this {@link
     * AddressRange}.
     *
     * @param address
     *     The {@link InetAddress} to compare
     */
    private void checkProtocolVersion(@Nullable InetAddress address) {
        if (version == 4 && address instanceof Inet6Address) {
            throw new IllegalArgumentException(
                "IPv6 Address is not within IPv4 Address range");
        } else if (version == 6 && address instanceof Inet4Address) {
            throw new IllegalArgumentException(
                "IPv4 Address is not within IPv6 Address range");
        }
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
		if (this == obj) {
            return true;
        }
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		final AddressRange other = (AddressRange) obj;

		if (end == null) {
			if (other.end != null) {
                return false;
            }
		} else if (!end.equals(other.end)) {
            return false;
        }

		if (start == null) {
            return other.start == null;
		} else {
            return start.equals(other.start);
        }
    }

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
        b.append(start.getHostAddress());
		if (!end.equals(start)) {
			b.append('-');
            b.append(end.getHostAddress());
		}
		return b.toString();
	}
}
