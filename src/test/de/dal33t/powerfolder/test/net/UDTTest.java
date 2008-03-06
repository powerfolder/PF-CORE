package de.dal33t.powerfolder.test.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.net.UDTSocket;

public class UDTTest extends TestCase {
	private class ThreadHelper<T> {
		volatile T value;
	}
	public void testSocket() throws IOException, InterruptedException {
		final ThreadHelper<Boolean> tmp = new ThreadHelper<Boolean>();
		tmp.value = false;
		final UDTSocket serv = new UDTSocket();
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					bindSocket(serv);
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
		
		other.connect(new InetSocketAddress("127.0.0.1", 10000));
		
		assertTrue(other.isConnected());
		assertFalse(serv.isClosed());
		assertFalse(other.isClosed());
		BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()));
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
	
	/**
	 * Potential NAT traversal test.
	 */
	public void testRendezvous() throws IOException {
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
					BufferedReader in = new BufferedReader(new InputStreamReader(c2.getInputStream()));
					assertEquals("Hello World!", in.readLine());
					in.close();
					c2.close();
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			
		});
	}
	
	public void testLargeTransfer() {
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
					System.err.println("Done sending with " + (b.length * 10000000L / (System.currentTimeMillis() - time)) + " bytes/sec");
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
					System.err.println("Done receiving with " + (b.length * 10000000L / (System.currentTimeMillis() - time)) + " bytes/sec");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
		});
	}
	
	private int bindSocket(UDTSocket a) {
		for (int i = 10000; i < 10000 + 50; i++)  {
			try {
				a.bind(new InetSocketAddress(i));
			} catch (IOException e) {
//				e.printStackTrace();
//				System.err.println("Trying next port...");
				continue;
			}
			return i;
		}
		return -1;
	}
	
	private void connectRendezvous(final UDTSocket a, final Runnable workerA, final UDTSocket b, final Runnable workerB) {
		final int pa = bindSocket(a);
		final int pb = bindSocket(b);
		assertTrue(pa > 0);
		assertTrue(pb > 0);
		a.setSoRendezvous(true);
		b.setSoRendezvous(true);
		assertTrue(a.getSoRendezvous());
		assertTrue(b.getSoRendezvous());
		
		final ThreadHelper<Error> rethrower = new ThreadHelper<Error>();
		Thread tA, tB;
		tA = new Thread(new Runnable() {
			@SuppressWarnings("deprecation")
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
			@SuppressWarnings("deprecation")
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
