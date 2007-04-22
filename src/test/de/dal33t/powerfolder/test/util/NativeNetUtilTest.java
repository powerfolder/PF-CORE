package de.dal33t.powerfolder.test.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.net.AddressRange;
import de.dal33t.powerfolder.util.net.NetworkAddress;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.SubnetMask;
import de.dal33t.powerfolder.util.os.NetworkHelper;

public class NativeNetUtilTest extends TestCase {
	public void testInterfaceAddresses() {
        if (!NetworkHelper.isSupported()) {
            System.err.println("Unable to test network utility. only supported on windows systems");
            return;
        }
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
        
        SubnetMask secondMask = new SubnetMask((Inet4Address) InetAddress.getByName("255.255.255.128"));
        Inet4Address ip1 = (Inet4Address) InetAddress.getByName("195.145.13.115");
        Inet4Address ip2 = (Inet4Address) InetAddress.getByName("195.145.13.79");
        assertTrue(secondMask.sameSubnet(ip1, ip2));
	}
	
	public void testNetworkAddresses() throws UnknownHostException {
        if (!NetworkHelper.isSupported()) {
            System.err.println("Unable to test network utility. only supported on windows systems");
            return;
        }
		NetworkHelper nu = NetworkHelper.getInstance();
		assertNotNull(nu);
		// TODO: Cheap, needs change
		assertEquals(nu.getInterfaceAddresses().size(), nu.getNetworkAddresses().size());
        // Test a non LAN address:
        assertFalse(NetworkUtil.isOnAnySubnet(
            (Inet4Address) InetAddress.getByName("217.23.244.121")));
        NetworkAddress nw = nu.getNetworkAddresses().iterator().next();
        assertTrue(NetworkUtil.isOnAnySubnet(nw.getAddress()));
        Inet4Address a = (Inet4Address) InetAddress.getByName("0.0.0.0");
        Inet4Address b = (Inet4Address) InetAddress.getByName("255.255.255.255");
        for (NetworkAddress na: nu.getNetworkAddresses()) {
            assertFalse(na.isValid() && na.getMask().sameSubnet(a, b));
        }
	}
	
	public void testAddressRanges() throws UnknownHostException {
		AddressRange ar = new AddressRange(
				(Inet4Address) InetAddress.getByName("0.0.0.110"), 
				(Inet4Address) InetAddress.getByName("127.127.127.127"));
		assertTrue(ar.contains(
				(Inet4Address) InetAddress.getByName("127.127.127.127")));
		assertTrue(ar.contains(
				(Inet4Address) InetAddress.getByName("0.0.0.110")));
		assertTrue(ar.contains(
				(Inet4Address) InetAddress.getByName("127.127.127.126")));
		assertFalse(ar.contains(
				(Inet4Address) InetAddress.getByName("127.127.127.128")));
		assertFalse(ar.contains(
				(Inet4Address) InetAddress.getByName("128.127.127.127")));
		assertFalse(ar.contains(
				(Inet4Address) InetAddress.getByName("0.0.0.1")));
	}
}
