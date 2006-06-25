package de.dal33t.powerfolder.util.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Representation of a subnet mask.
 * Can be used to check if certain IPs are in the same subnet.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class SubnetMask {
	private Inet4Address mask;

	public SubnetMask(Inet4Address addr) {
		mask = addr;
	}
	
	public Inet4Address mask(Inet4Address addr) {
		byte b[] = new byte[4];
		for (int i = 0; i < b.length; i++)
			b[i] = (byte) (mask.getAddress()[i] & addr.getAddress()[i]); 
		try {
			return (Inet4Address) InetAddress.getByAddress(b);
		} catch (UnknownHostException e) {
			// This should never happen!!
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns true if all the given addresses are in the same subnet.
	 * @param addr a list of Inet4Addresses to test 
	 * @return
	 */
	public boolean sameSubnet(Inet4Address... addr) {
		if (addr.length == 0)
			return true;
		
		Inet4Address m = mask(addr[0]);
		for (Inet4Address a: addr)
			if (!m.equals(mask(a)))
				return false;
		return true;
	}

	@Override
	public boolean equals(Object arg0) {
		return mask.equals(arg0);
	}

	@Override
	public int hashCode() {
		return mask.hashCode();
	}

	@Override
	public String toString() {
		return mask.toString();
	}
}
