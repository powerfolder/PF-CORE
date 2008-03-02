package de.dal33t.powerfolder.test.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.net.UDTSocket;

public class UDTTest extends TestCase {
	private class ThreadHelper<T> {
		volatile T ref;
	}
	public void testSocket() throws IOException, InterruptedException {
		final ThreadHelper<Boolean> tmp = new ThreadHelper<Boolean>();
		tmp.ref = false;
		final UDTSocket serv = new UDTSocket();
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					serv.bind(new InetSocketAddress(10000));
					serv.listen(10);
					UDTSocket cl = serv.accept();
					PrintWriter w = new PrintWriter(cl.getOutputStream());
					w.println("Hello World!");
					w.close();
					cl.close();
					serv.close();
					tmp.ref = true;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.start();
		UDTSocket other = new UDTSocket();
		other.connect(new InetSocketAddress("127.0.0.1", 10000));
		BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()));
		assertEquals("Hello World!", in.readLine());
		in.close();
		other.close();
		t.join(1000);
		assertTrue(tmp.ref);
	}
	
	/**
	 * Potential NAT traversal test.
	 */
	public void testRendezvous() throws IOException {
		final UDTSocket c1 = new UDTSocket();
		UDTSocket c2 = new UDTSocket();
		c1.bind(new InetSocketAddress(10001));
		c2.bind(new InetSocketAddress(10002));
		c1.setSoRendezvous(true);
		c2.setSoRendezvous(true);
		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					c1.connect(new InetSocketAddress("127.0.0.1", 10002));
					PrintWriter w = new PrintWriter(c1.getOutputStream());
					w.println("Hello World!");
					w.close();
					c1.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		t.start();
		c2.connect(new InetSocketAddress("127.0.0.1", 10001));
		BufferedReader in = new BufferedReader(new InputStreamReader(c2.getInputStream()));
		assertEquals("Hello World!", in.readLine());
		in.close();
		c2.close();
	}
}
