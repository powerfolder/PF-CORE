package de.dal33t.powerfolder.test.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import junit.framework.TestCase;

public class LocalIPTests extends TestCase {
	public void testIsLoopBackAddress() throws SocketException {
		for (Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces(); ni.hasMoreElements(); ) {
			for (Enumeration<InetAddress> ii = ni.nextElement().getInetAddresses(); ii.hasMoreElements();) {
				InetAddress addr = ii.nextElement();
				assertTrue("" + addr + " is not detected as local address!", addr.isLoopbackAddress());
			}
		}
	}
}
