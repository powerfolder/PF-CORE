package de.dal33t.powerfolder.test.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

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
	
	public void testExamples() throws ParseException, UnknownHostException {
	    AddressRange r = AddressRange.parseRange("195.145.13.0-195.145.13.255");
	    assertTrue(r.contains((Inet4Address) Inet4Address.getByName("195.145.13.84")));
	    for (int j = 1; j < 255; j++) {
    	    for (int i = 1; i < 255; i++) {
    	        if (j == 13) {
    	            assertTrue(r.contains((Inet4Address) Inet4Address.getByName("195.145." + j + "." + i)));
    	        } else { 
    	            assertFalse(r.contains((Inet4Address) Inet4Address.getByName("195.145." + j + "." + i)));
    	        }
    	    }
	    }
	}
}
