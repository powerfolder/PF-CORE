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
package de.dal33t.powerfolder.test.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.UDTSocket;

public class UDTTest extends TestCase {
    private class ThreadHelper<T> {
        volatile T value;
    }

    public void testUDTSO() throws IOException {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }
        UDTSocket sock = new UDTSocket();

        sock.setSoSenderBufferLimit(1000 * 1024);
        assertTrue(Math.abs(1000 * 1024 - sock.getSoSenderBufferLimit()) < 1000);
        sock.setSoReceiverBufferLimit(1000 * 1024);
        assertTrue(Math.abs(1000 * 1024 - sock.getSoReceiverBufferLimit()) < 1000);
        sock.setSoUDPSenderBufferSize(1000 * 1024);
        assertTrue(Math.abs(1000 * 1024 - sock.getSoUDPSenderBufferSize()) < 1000);
        sock.setSoUDPReceiverBufferSize(1000 * 1024);
        assertTrue(Math.abs(1000 * 1024 - sock.getSoUDPReceiverBufferSize()) < 1000);
    }

    public void testClosing() {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }
        final UDTSocket c1 = new UDTSocket();
        final UDTSocket c2 = new UDTSocket();
        connectRendezvous(c1, new Runnable() {

            public void run() {
                try {
                    Thread.sleep(1000);
                    c1.close();
                    c1.close();
                } catch (IOException e) {
                    throw new Error(e);
                } catch (InterruptedException e) {
                    throw new Error(e);
                }
            }

        }, c2, new Runnable() {

            public void run() {
                try {
                    InputStream in = c2.getInputStream();
                    in.close();
                    c2.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }

        });
    }

    public void testSocket() throws IOException, InterruptedException {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }
        final ThreadHelper<Boolean> tmp = new ThreadHelper<Boolean>();
        tmp.value = false;
        final UDTSocket serv = new UDTSocket();
        int prt = bindSocket(serv);
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    serv.listen(10);
                    UDTSocket cl = serv.accept();
                    PrintWriter w = new PrintWriter(cl.getOutputStream());
                    w.println("Hello World!");
                    w.close();
                    cl.close();
                    serv.close();
                    tmp.value = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        assertFalse(serv.isConnected());
        assertFalse(serv.isClosed());
        t.start();
        UDTSocket other = new UDTSocket();
        assertFalse(other.isConnected());
        assertFalse(other.isClosed());

        other.connect(new InetSocketAddress("127.0.0.1", prt));

        assertTrue(other.isConnected());
        assertFalse(serv.isClosed());
        assertFalse(other.isClosed());
        BufferedReader in = new BufferedReader(new InputStreamReader(
            other.getInputStream()));
        assertEquals("Hello World!", in.readLine());
        in.close();

        other.close();
        assertFalse(other.isConnected());
        assertTrue(other.isClosed());

        t.join(1000);
        assertTrue(tmp.value);
        assertFalse(serv.isConnected());
        assertTrue(serv.isClosed());
    }

    public void xtestRendezvousOverhead() throws IOException {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }
        UDTSocket s = new UDTSocket();
        s.setSoRendezvous(true);
        int tmp = bindSocket(s);
        for (int i = 0; i < 10; i++) {
            try {
                s.connect(new InetSocketAddress("localhost", tmp + 1));
            } catch (IOException e) {
                if (i < 10) {
                    System.err.println("Next connect.");
                }
            }
        }
        s.close();
    }

    /**
     * Potential NAT traversal test.
     */
    public void testRendezvous() throws IOException {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }
        final UDTSocket c1 = new UDTSocket();
        final UDTSocket c2 = new UDTSocket();
        connectRendezvous(c1, new Runnable() {

            public void run() {
                try {
                    PrintWriter w = new PrintWriter(c1.getOutputStream());
                    w.println("Hello World!");
                    w.close();
                    c1.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }

        }, c2, new Runnable() {

            public void run() {
                try {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(c2.getInputStream()));
                    assertEquals("Hello World!", in.readLine());
                    in.close();
                    c2.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }

        });
    }

    /**
     * Adjust for the 2 manual tests below
     */
    private final InetSocketAddress addrA = new InetSocketAddress(
        "192.168.0.1", 1111),
        addrB = new InetSocketAddress("192.168.0.1", 1234);

    public void xtestOverheadA() throws InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        System.out.println("Starting 50 parallel connection requests to B");
        for (int i = 0; i < 2; i++) {
            service.execute(new Runnable() {
                public void run() {
                    UDTSocket socket = new UDTSocket();
                    socket.setSoRendezvous(true);
                    try {
                        socket.setSoSenderBufferLimit(100);
                        socket.setSoReceiverBufferLimit(100);
                        socket.setSoUDPReceiverBufferSize(100);
                        socket.setSoUDPSenderBufferSize(100);
                        bindSocket(socket);
                        System.out.println("Connecting...");
                        socket.connect(addrB);
                    } catch (IOException e) {
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

    public void testLargeTransfer() {
        if (!NetworkUtil.isUDTSupported()) {
            return;
        }
        final UDTSocket c1 = new UDTSocket();
        final UDTSocket c2 = new UDTSocket();
        connectRendezvous(c1, new Runnable() {

            public void run() {
                try {
                    byte b[] = new byte[32768];
                    OutputStream out = c1.getOutputStream();
                    System.err.println("Sending");
                    long time = System.currentTimeMillis();
                    for (int i = 0; i < 10000; i++) {
                        out.write(b);
                    }
                    c1.close();
                    System.err.println("Done sending with "
                        + (b.length * 10000000L / (System.currentTimeMillis() - time))
                        + " bytes/sec");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }, c2, new Runnable() {

            public void run() {
                try {
                    byte b[] = new byte[32768];
                    InputStream in = c2.getInputStream();
                    System.err.println("Receiving");
                    long count = 0;
                    int read = 0;
                    long time = System.currentTimeMillis();
                    while ((read = in.read(b)) >= 0) {
                        count += read;
                    }
                    c2.close();
                    assertEquals(b.length * 10000, count);
                    System.err.println("Done receiving with "
                        + (b.length * 10000000L / (System.currentTimeMillis() - time))
                        + " bytes/sec");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }

    private int bindSocket(UDTSocket a) {
        for (int i = 10000; i < 10000 + 2000; i++) {
            try {
                a.bind(new InetSocketAddress(i));
            } catch (IOException e) {
                // e.printStackTrace();
                // System.err.println("Trying next port...");
                continue;
            }
            return i;
        }
        return -1;
    }

    private void connectRendezvous(final UDTSocket a, final Runnable workerA,
        final UDTSocket b, final Runnable workerB)
    {
        a.setSoRendezvous(true);
        b.setSoRendezvous(true);

        final int pa = bindSocket(a);
        final int pb = bindSocket(b);
        System.err.println(pa + " " + pb);
        assertTrue(pa > 0);
        assertTrue(pb > 0);
        assertTrue(a.getSoRendezvous());
        assertTrue(b.getSoRendezvous());

        final ThreadHelper<Error> rethrower = new ThreadHelper<Error>();
        Thread tA, tB;
        tA = new Thread(new Runnable() {
            public void run() {
                try {
                    a.connect(new InetSocketAddress("localhost", pb));
                    workerA.run();
                } catch (Error t) {
                    rethrower.value = t;
                } catch (IOException e) {
                    rethrower.value = new Error(e);
                }
            }
        }, "Worker A");
        tB = new Thread(new Runnable() {
            public void run() {
                try {
                    b.connect(new InetSocketAddress("localhost", pa));
                    workerB.run();
                } catch (Error t) {
                    rethrower.value = t;
                } catch (IOException e) {
                    rethrower.value = new Error(e);
                }
            }
        }, "Worker B");
        tB.start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        tA.start();
        try {
            tA.join();
            tB.join();
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        if (rethrower.value != null) {
            throw rethrower.value;
        }
    }
}
