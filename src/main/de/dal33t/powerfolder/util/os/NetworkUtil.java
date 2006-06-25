package de.dal33t.powerfolder.util.os;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.net.NetworkAddress;
import de.dal33t.powerfolder.util.os.Win32.NetworkUtilImpl;

public abstract class NetworkUtil {
	private static NetworkUtil instance;
	
	/**
	 * Returns an instance of a subclass of this class.
	 * 
	 * @return a NetworkUtil instance or null if there is none for
	 * 		the underlying operating system
	 */
	public static NetworkUtil getInstance() {
		if (instance == null) {
			if (Util.isWindowsSystem()) {
				if (NetworkUtilImpl.loadLibrary())
					instance = new NetworkUtilImpl(); 
			}
		}
		return instance;
	}
	
	/**
	 * Returns a list of IP addresses and subnet masks.
	 * The returned list contains String-arrays of size 2 with
	 * index 0 containing an IP address and index 1 containing the
	 * associated subnet mask.
	 * @return
	 */
	public abstract Collection<String[]> getInterfaceAddresses();
	
	public Collection<NetworkAddress> getNetworkAddresses() {
		Collection<String[]> addr = getInterfaceAddresses();
		Collection<NetworkAddress> result = new ArrayList<NetworkAddress>(addr.size());
		
		for (String s[]: addr) {
			try {
				result.add(new NetworkAddress((Inet4Address) InetAddress.getByName(s[0]),
						(Inet4Address) InetAddress.getByName(s[1])));
			} catch (UnknownHostException e) {
				// Should never happen!
				throw new RuntimeException(e);
			}
		}
		return result;
	}
}
