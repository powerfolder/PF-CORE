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
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.util.net.AddressRange;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import java.net.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NetUtilTest {

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
                assertTrue(
                    NetworkUtil.isOnLanOrLoopback(address),"Address should be on lan: " + address);
                if (NetworkUtil.isOnInterfaceSubnet(ia, address)) {
                    it.remove();
                }
            }
            for (InetAddress address : inetAddresses) {
                if (NetworkUtil.isOnInterfaceSubnet(ia, address)) {
                    fail("Internet address " + address
                        + " should not be on LAN!" + ia);
                }
                assertFalse(
                    NetworkUtil.isOnLanOrLoopback(address),"Address should NOT be on lan: " + address);
            }
        }
        assertTrue( lanAddresses.isEmpty(),"LAN address not found on local adapter subnet: "
            + lanAddresses);

    }

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

    public void xtestNoResovleInetAddress() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 1337);
        assertFalse(addr.isUnresolved());

        addr = new InetSocketAddress("88.198.85.81", 1337);
        assertEquals("/88.198.85.81", addr.getAddress().toString());
        assertEquals("88.198.85.81",
            NetworkUtil.getHostAddressNoResolve(addr.getAddress()));
        // Do reverse lookup
        assertEquals( addr.getAddress().getHostName(),"addr.getAddress().getHostName()", "os007.powerfolder.com");
        assertEquals("os007.powerfolder.com/88.198.85.81", addr.getAddress()
            .toString());
        assertEquals(
            NetworkUtil.getHostAddressNoResolve(addr.getAddress()),"NetworkUtil.getHostAddressNoResolve", "os007.powerfolder.com");
        assertFalse(addr.isUnresolved());
        assertEquals( addr.getHostName(),"addr.getHostName()", "os007.powerfolder.com");
        assertEquals( addr.getAddress().getHostName(),"addr.getAddress().getHostName()", "os007.powerfolder.com");
        assertEquals("88.198.85.81", addr.getAddress().getHostAddress());
        assertEquals( addr.getAddress()
            .getCanonicalHostName(),"addr.getAddress() .getCanonicalHostName()", "os007.powerfolder.com");

        addr = new InetSocketAddress("195.201.181.138", 1337);
        assertEquals("/195.201.181.138", addr.getAddress().toString());
        assertEquals("195.201.181.138",
                NetworkUtil.getHostAddressNoResolve(addr.getAddress()));
        // Do reverse lookup
        assertEquals( addr.getAddress().getHostName(),"addr.getAddress().getHostName()", "my.powerfolder.com");
        assertEquals("my.powerfolder.com/195.201.181.138", addr.getAddress()
                .toString());
        assertEquals(
                NetworkUtil.getHostAddressNoResolve(addr.getAddress()),"NetworkUtil.getHostAddressNoResolve", "my.powerfolder.com");
        assertFalse(addr.isUnresolved());
        assertEquals( addr.getHostName(),"addr.getHostName()", "my.powerfolder.com");
        assertEquals( addr.getAddress().getHostName(),"addr.getAddress().getHostName()", "my.powerfolder.com");
        assertEquals("195.201.181.138", addr.getAddress().getHostAddress());
        assertEquals( addr.getAddress()
                .getCanonicalHostName(),"addr.getAddress() .getCanonicalHostName()", "my.powerfolder.com");
    }
}
