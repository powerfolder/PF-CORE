package de.dal33t.powerfolder.test.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.net.AddressRange;

public class NetUtilTest extends TestCase {
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
