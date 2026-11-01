/*
* Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
* Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
*/
package de.dal33t.powerfolder.util.net;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NetworkUtil}, and for {@link AddressRange} which only this class uses.
 * <p>
 * Replaces the former {@code de.dal33t.powerfolder.util.NetUtilTest}, which sat in the package above
 * the class it tested - two test classes for one class, and neither name found the other.
 */
public class NetworkUtilTest {

    // isLoopbackAddress ******************************************************

    /*
     * This one decides whether an address a proxy header claims is the loopback address, which is what
     * keeps a caller from passing itself off as a local client. The notation is the sender's choice, so
     * the cases below are the spellings of one address rather than a sample of them.
     */

    @Test
    public void testIPv4Loopback() {
        assertTrue(NetworkUtil.isLoopbackAddress("127.0.0.1"));
        // The whole 127.0.0.0/8 block is loopback, not just the one address.
        assertTrue(NetworkUtil.isLoopbackAddress("127.0.0.53"));
        assertTrue(NetworkUtil.isLoopbackAddress("127.1.2.3"));
        assertTrue(NetworkUtil.isLoopbackAddress("127.255.255.254"));
    }

    @Test
    public void testIPv6Loopback() {
        assertTrue(NetworkUtil.isLoopbackAddress("::1"));
        assertTrue(NetworkUtil.isLoopbackAddress("0:0:0:0:0:0:0:1"));
        assertTrue(NetworkUtil.isLoopbackAddress("0::1"));
        assertTrue(NetworkUtil.isLoopbackAddress("::ffff:127.0.0.1"));
    }

    @Test
    public void testLoopbackNotationIsStripped() {
        assertTrue(NetworkUtil.isLoopbackAddress("  127.0.0.1  "));
        assertTrue(NetworkUtil.isLoopbackAddress("127.0.0.1:8080"));
        assertTrue(NetworkUtil.isLoopbackAddress("[::1]"));
        assertTrue(NetworkUtil.isLoopbackAddress("[::1]:8080"));
        assertTrue(NetworkUtil.isLoopbackAddress("::1%lo0"));
        assertTrue(NetworkUtil.isLoopbackAddress("LOCALHOST"));
        assertTrue(NetworkUtil.isLoopbackAddress("localhost"));
    }

    @Test
    public void testRoutableAddressesAreNotLoopback() {
        assertFalse(NetworkUtil.isLoopbackAddress("10.0.0.1"));
        assertFalse(NetworkUtil.isLoopbackAddress("192.168.1.1"));
        assertFalse(NetworkUtil.isLoopbackAddress("203.0.113.7"));
        // Close to ::1 in writing, a different address: dropping zeroes textually would misread it.
        assertFalse(NetworkUtil.isLoopbackAddress("::10"));
        assertFalse(NetworkUtil.isLoopbackAddress("128.0.0.1"));
        assertFalse(NetworkUtil.isLoopbackAddress("27.0.0.1"));
    }

    /**
     * A host name is never resolved: that would put a name server lookup inside a request, and a name
     * pointing at the loopback address is not a claim the caller gets to make.
     */
    @Test
    public void testHostNamesAreNotResolved() {
        assertFalse(NetworkUtil.isLoopbackAddress("localhost.example.com"));
        assertFalse(NetworkUtil.isLoopbackAddress("evil.example.com"));
        assertFalse(NetworkUtil.isLoopbackAddress("not-an-address"));
    }

    @Test
    public void testLoopbackEmptyAndGarbage() {
        assertFalse(NetworkUtil.isLoopbackAddress(""));
        assertFalse(NetworkUtil.isLoopbackAddress("   "));
        assertFalse(NetworkUtil.isLoopbackAddress(":"));
        assertFalse(NetworkUtil.isLoopbackAddress("[]"));
        assertFalse(NetworkUtil.isLoopbackAddress("%"));
    }

    // portOf *****************************************************************

    @Test
    public void testPortOf() {
        assertEquals(8080, NetworkUtil.portOf("1.2.3.4:8080"));
        assertEquals(8080, NetworkUtil.portOf("  1.2.3.4:8080  "));
        assertEquals(8080, NetworkUtil.portOf("[::1]:8080"));
        assertEquals(1, NetworkUtil.portOf("1.2.3.4:1"));
        assertEquals(65535, NetworkUtil.portOf("1.2.3.4:65535"));
    }

    @Test
    public void testPortOfWithoutPort() {
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4"));
        assertEquals(-1, NetworkUtil.portOf("[::1]"));
        // A bare IPv6 address is all colons and names no port.
        assertEquals(-1, NetworkUtil.portOf("::1"));
        assertEquals(-1, NetworkUtil.portOf("0:0:0:0:0:0:0:1"));
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4:"));
        assertEquals(-1, NetworkUtil.portOf(""));
        assertEquals(-1, NetworkUtil.portOf("   "));
        assertEquals(-1, NetworkUtil.portOf(null));
    }

    @Test
    public void testPortOfRejectsOutOfRangeAndGarbage() {
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4:0"));
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4:65536"));
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4:-5"));
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4:99999999999"));
        assertEquals(-1, NetworkUtil.portOf("1.2.3.4:abc"));
    }

    // isNullIP ***************************************************************

    @Test
    public void testNullIP() throws UnknownHostException {
        assertTrue(NetworkUtil.isNullIP(InetAddress.getByName("0.0.0.0")));
        assertTrue(NetworkUtil.isNullIP(
            InetAddress.getByAddress(new byte[]{0, 0, 0, 0})));
        // A host name on the same bytes changes nothing: only the address counts.
        assertTrue(NetworkUtil.isNullIP(
            InetAddress.getByAddress("anything", new byte[]{0, 0, 0, 0})));

        assertFalse(NetworkUtil.isNullIP(InetAddress.getByName("127.0.0.1")));
        assertFalse(NetworkUtil.isNullIP(InetAddress.getByName("0.0.0.1")));
        assertFalse(NetworkUtil.isNullIP(InetAddress.getByName("1.0.0.0")));
        // The high bit set must not read as zero through a signed byte.
        assertFalse(NetworkUtil.isNullIP(InetAddress.getByName("128.0.0.0")));
        assertFalse(NetworkUtil.isNullIP(InetAddress.getByName("255.255.255.255")));
    }

    @Test
    public void testNullIPRejectsNull() {
        try {
            NetworkUtil.isNullIP(null);
            fail("A null address has to be rejected, not answered");
        } catch (NullPointerException expected) {
            // Reject.ifNull
        }
    }

    // getHostAddressNoResolve ************************************************

    /**
     * The point of the method is in its name: it reports what the address already knows and never asks a
     * name server. Both branches are checked without touching the network - an address built from bytes
     * alone has no host name, one built with a name carries it.
     */
    @Test
    public void testHostAddressNoResolve() throws UnknownHostException {
        InetAddress numeric = InetAddress.getByName("88.198.85.81");
        assertEquals("88.198.85.81", NetworkUtil.getHostAddressNoResolve(numeric));

        InetAddress named = InetAddress.getByAddress("os007.powerfolder.com",
            new byte[]{(byte) 88, (byte) 198, (byte) 85, (byte) 81});
        assertEquals("os007.powerfolder.com", NetworkUtil.getHostAddressNoResolve(named));

        assertEquals("127.0.0.1",
            NetworkUtil.getHostAddressNoResolve(InetAddress.getByName("127.0.0.1")));
    }

    // isPortAvailable ********************************************************

    /**
     * Only the occupied case is asserted. That a port is free cannot be stated without a race - and
     * after closing a socket the port may sit in TIME_WAIT, which the method reports as taken.
     */
    @Test
    public void testPortAvailable() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            assertFalse(NetworkUtil.isPortAvailable(socket.getLocalPort()), "A bound port must not be reported as available");
        }
    }

    // isFromThisComputer *****************************************************

    @Test
    public void testFromThisComputer() throws SocketException, UnknownHostException {
        assertTrue(NetworkUtil.isFromThisComputer(InetAddress.getByName("127.0.0.1")));
        assertTrue(NetworkUtil.isFromThisComputer(InetAddress.getByName("::1")));
        // Documented behaviour: a null address is answered, not rejected.
        assertFalse(NetworkUtil.isFromThisComputer(null));
        assertFalse(NetworkUtil.isFromThisComputer(InetAddress.getByName("203.0.113.7")));

        for (InterfaceAddress ia : NetworkUtil.getAllLocalNetworkAddressesCached().keySet()) {
            if (ia != null && ia.getAddress() != null) {
                assertTrue(NetworkUtil.isFromThisComputer(ia.getAddress()), "An address of a local interface is from this computer: " + ia.getAddress());
            }
        }
    }

    @Test
    public void testLocalNetworkAddressesAreCached() throws SocketException {
        assertNotNull(NetworkUtil.getAllLocalNetworkAddressesCached());
        // The cache has to answer the same set, not rebuild a different one.
        assertEquals(NetworkUtil.getAllLocalNetworkAddressesCached().keySet(),
            NetworkUtil.getAllLocalNetworkAddressesCached().keySet());
    }

    // isOnLanOrLoopback / isOnInterfaceSubnet ********************************

    /**
     * #1403
     *
     * @throws UnknownHostException
     * @throws SocketException
     */
    @Test
    public void testSubnet() throws SocketException, UnknownHostException {
        Set<InetAddress> lanAddresses = new HashSet<InetAddress>();
        lanAddresses.add(Inet4Address.getByName("127.0.0.1"));
        for (InterfaceAddress ia : NetworkUtil
            .getAllLocalNetworkAddressesCached().keySet())
        {
            if (!(ia.getAddress() instanceof Inet4Address)) {
                continue;
            }
            if (ia.getAddress().isSiteLocalAddress()) {
                byte[] bAddrs = ia.getAddress().getAddress();
                if (bAddrs[3] != -88) {
                    bAddrs[3] = -88;
                } else {
                    bAddrs[3] = -99;
                }
                lanAddresses.add(Inet4Address.getByAddress(bAddrs));

                bAddrs = ia.getAddress().getAddress();
                if (bAddrs[3] != 44) {
                    bAddrs[3] = 44;
                } else {
                    bAddrs[3] = 45;
                }
                lanAddresses.add(Inet4Address.getByAddress(bAddrs));
            } else if (ia.getAddress().isLinkLocalAddress()) {
                byte[] bAddrs = ia.getAddress().getAddress();
                if (bAddrs[3] != -88) {
                    bAddrs[3] = -88;
                } else {
                    bAddrs[3] = -99;
                }
                if (bAddrs[2] != -66) {
                    bAddrs[2] = -66;
                } else {
                    bAddrs[2] = -55;
                }
                lanAddresses.add(Inet4Address.getByAddress(bAddrs));
            }
        }

        Set<InetAddress> inetAddresses = new HashSet<InetAddress>();
        inetAddresses.add(Inet4Address.getByName("188.40.205.177"));
        inetAddresses.add(Inet4Address.getByName("184.72.127.2"));
        inetAddresses.add(Inet4Address.getByName("192.168.255.1"));

        // Now we should have at least 2 test LAN addresses and 2 inet
        // addresses.

        for (InterfaceAddress ia : NetworkUtil
            .getAllLocalNetworkAddressesCached().keySet())
        {
            if (!(ia.getAddress() instanceof Inet4Address)) {
                continue;
            }
            for (Iterator<InetAddress> it = lanAddresses.iterator(); it
                .hasNext();)
            {
                InetAddress address = it.next();
                assertTrue(NetworkUtil.isOnLanOrLoopback(address), "Address should be on lan: " + address);
                if (NetworkUtil.isOnInterfaceSubnet(ia, address)) {
                    it.remove();
                }
            }
            for (InetAddress address : inetAddresses) {
                if (NetworkUtil.isOnInterfaceSubnet(ia, address)) {
                    fail("Internet address " + address
                        + " should not be on LAN!" + ia);
                }
                assertFalse(NetworkUtil.isOnLanOrLoopback(address), "Address should NOT be on lan: " + address);
            }
        }
        assertTrue(lanAddresses.isEmpty(), "LAN address not found on local adapter subnet: "
            + lanAddresses);

    }

    // AddressRange ***********************************************************

    @Test
    public void testAddressRanges() throws UnknownHostException {
        AddressRange ar = new AddressRange(
            (Inet4Address) InetAddress.getByName("0.0.0.110"),
            (Inet4Address) InetAddress.getByName("127.127.127.127"));
        assertTrue(ar.contains((Inet4Address) InetAddress
            .getByName("127.127.127.127")));
        assertTrue(ar.contains((Inet4Address) InetAddress
            .getByName("0.0.0.110")));
        assertTrue(ar.contains((Inet4Address) InetAddress
            .getByName("127.127.127.126")));
        assertFalse(ar.contains((Inet4Address) InetAddress
            .getByName("127.127.127.128")));
        assertFalse(ar.contains((Inet4Address) InetAddress
            .getByName("128.127.127.127")));
        assertFalse(ar
            .contains((Inet4Address) InetAddress.getByName("0.0.0.1")));
    }

    @Test
    public void testExamples() throws ParseException, UnknownHostException {
        AddressRange r = AddressRange.parseRange("195.145.13.0-195.145.13.255");
        assertTrue(r.contains((Inet4Address) Inet4Address
            .getByName("195.145.13.84")));
        for (int j = 1; j < 255; j++) {
            for (int i = 1; i < 255; i++) {
                if (j == 13) {
                    assertTrue(r.contains((Inet4Address) Inet4Address
                        .getByName("195.145." + j + "." + i)));
                } else {
                    assertFalse(r.contains((Inet4Address) Inet4Address
                        .getByName("195.145." + j + "." + i)));
                }
            }
        }
    }

    @Test
    public void testPrivateAdrressRange() throws ParseException,
        UnknownHostException
    {
        AddressRange ar = AddressRange.parseRange("10.51.32.1-10.51.64.254");
        assertFalse(ar.contains((Inet4Address) InetAddress
            .getByName("10.51.31.1")));
        assertTrue(ar.contains((Inet4Address) InetAddress
            .getByName("10.51.32.1")));
        assertTrue(ar.contains((Inet4Address) InetAddress
            .getByName("10.51.64.254")));
        assertFalse(ar.contains((Inet4Address) InetAddress
            .getByName("10.51.65.254")));
    }

    @Test
    public void testRangeEdges() throws ParseException, UnknownHostException {
        AddressRange single = AddressRange.parseRange("10.0.0.5-10.0.0.5");
        assertTrue(single.contains((Inet4Address) InetAddress.getByName("10.0.0.5")));
        assertFalse(single.contains((Inet4Address) InetAddress.getByName("10.0.0.4")));
        assertFalse(single.contains((Inet4Address) InetAddress.getByName("10.0.0.6")));

        // Comparison must be unsigned: every octet above 127 has the high bit set.
        AddressRange high = AddressRange.parseRange("200.0.0.0-250.255.255.255");
        assertTrue(high.contains((Inet4Address) InetAddress.getByName("240.128.200.1")));
        assertFalse(high.contains((Inet4Address) InetAddress.getByName("199.255.255.255")));
        assertFalse(high.contains((Inet4Address) InetAddress.getByName("251.0.0.0")));

        AddressRange all = AddressRange.parseRange("0.0.0.0-255.255.255.255");
        assertTrue(all.contains((Inet4Address) InetAddress.getByName("0.0.0.0")));
        assertTrue(all.contains((Inet4Address) InetAddress.getByName("255.255.255.255")));
        assertTrue(all.contains((Inet4Address) InetAddress.getByName("128.64.32.16")));
    }

    /**
     * Disabled since it came over from {@code NetUtilTest}: it does reverse lookups against
     * powerfolder.com hosts, so it needs a name server and the records to stay as they are.
     */
    public void xtestNoResovleInetAddress() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 1337);
        assertFalse(addr.isUnresolved());

        addr = new InetSocketAddress("88.198.85.81", 1337);
        assertEquals("/88.198.85.81", addr.getAddress().toString());
        assertEquals("88.198.85.81",
            NetworkUtil.getHostAddressNoResolve(addr.getAddress()));
        // Do reverse lookup
        assertEquals("os007.powerfolder.com", addr.getAddress().getHostName(), "addr.getAddress().getHostName()");
        assertEquals("os007.powerfolder.com/88.198.85.81", addr.getAddress()
            .toString());
        assertEquals("os007.powerfolder.com", NetworkUtil.getHostAddressNoResolve(addr.getAddress()), "NetworkUtil.getHostAddressNoResolve");
        assertFalse(addr.isUnresolved());
        assertEquals("os007.powerfolder.com", addr.getHostName(), "addr.getHostName()");
        assertEquals("os007.powerfolder.com", addr.getAddress().getHostName(), "addr.getAddress().getHostName()");
        assertEquals("88.198.85.81", addr.getAddress().getHostAddress());
        assertEquals("os007.powerfolder.com", addr.getAddress()
            .getCanonicalHostName(), "addr.getAddress() .getCanonicalHostName()");

        addr = new InetSocketAddress("195.201.181.138", 1337);
        assertEquals("/195.201.181.138", addr.getAddress().toString());
        assertEquals("195.201.181.138",
                NetworkUtil.getHostAddressNoResolve(addr.getAddress()));
        // Do reverse lookup
        assertEquals("my.powerfolder.com", addr.getAddress().getHostName(), "addr.getAddress().getHostName()");
        assertEquals("my.powerfolder.com/195.201.181.138", addr.getAddress()
                .toString());
        assertEquals("my.powerfolder.com", NetworkUtil.getHostAddressNoResolve(addr.getAddress()), "NetworkUtil.getHostAddressNoResolve");
        assertFalse(addr.isUnresolved());
        assertEquals("my.powerfolder.com", addr.getHostName(), "addr.getHostName()");
        assertEquals("my.powerfolder.com", addr.getAddress().getHostName(), "addr.getAddress().getHostName()");
        assertEquals("195.201.181.138", addr.getAddress().getHostAddress());
        assertEquals("my.powerfolder.com", addr.getAddress()
                .getCanonicalHostName(), "addr.getAddress() .getCanonicalHostName()");
    }
}
