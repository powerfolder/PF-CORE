package de.dal33t.powerfolder.test.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.net.NetworkAddress;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.SubnetMask;
import de.dal33t.powerfolder.util.os.NetworkHelper;

public class NativeNetUtil extends TestCase {
	public void testInterfaceAddresses() {
		NetworkHelper nu = NetworkHelper.getInstance();
		assertNotNull(nu);
		for (String[] s: nu.getInterfaceAddresses()) {
			assertEquals(s.length, 2);
		}
	}
	
	public void testSubnetMasks() throws UnknownHostException {
		SubnetMask mask = new SubnetMask((Inet4Address) InetAddress.getByName("255.255.253.0"));
		assertEquals(mask.mask((Inet4Address) InetAddress.getByName("192.168.0.1")),
				mask.mask((Inet4Address) InetAddress.getByName("192.168.2.8")));
		assertNotSame(mask.mask((Inet4Address) InetAddress.getByName("192.168.0.1")),
				mask.mask((Inet4Address) InetAddress.getByName("192.168.3.1")));
		assertTrue(mask.sameSubnet((Inet4Address) InetAddress.getByName("192.168.0.1"),
				(Inet4Address) InetAddress.getByName("192.168.2.8")));
	}
	
	public void testNetworkAddresses() throws UnknownHostException {
		NetworkHelper nu = NetworkHelper.getInstance();
		assertNotNull(nu);
		// TODO: Cheap, needs change
		assertEquals(nu.getInterfaceAddresses().size(), nu.getNetworkAddresses().size());
        // Test a non LAN address:
        assertFalse(NetworkUtil.isOnAnySubnet(
            (Inet4Address) InetAddress.getByName("217.23.244.121")));
        NetworkAddress nw = nu.getNetworkAddresses().iterator().next();
        assertTrue(NetworkUtil.isOnAnySubnet(nw.getAddress()));
	}
}
