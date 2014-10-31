/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Date;

import de.dal33t.powerfolder.util.Format;

public class TestDynDnsResolve {
    public static void main(String[] args) throws InterruptedException {
        Security.setProperty("networkaddress.cache.ttl", "0");
        Security.setProperty("networkaddress.cache.ttl", "0");
        Security.setProperty("networkaddress.cache.negative.ttl", "0");
        System.setProperty("sun.net.inetaddr.ttl", "0");
        System.out.println("Cache is confirmed: "
            + Security.getProperty("networkaddress.cache.ttl"));
        for (int i = 0; i < 25000; i++) {
            try {
                System.out.println(Format.formatDateShort(new Date())
                    + ": "
                    + InetAddress.getByName("tot-notebook.dyndns.org")
                        .getHostAddress());
            } catch (UnknownHostException uhe) {
                System.out.println("UHE");
            }
            Thread.sleep(1000);
        }
    }
}
