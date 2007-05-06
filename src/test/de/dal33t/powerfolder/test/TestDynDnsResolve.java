package de.dal33t.powerfolder.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;

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
                System.out.println(InetAddress.getByName(
                    "tot-notebook.dyndns.org").getHostAddress());
            } catch (UnknownHostException uhe) {
                System.out.println("UHE");
            }
            Thread.sleep(1000);
        }
    }
}
