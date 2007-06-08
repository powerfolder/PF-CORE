package de.dal33t.powerfolder.test.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Adler32;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.delta.PartInfo;
import de.dal33t.powerfolder.util.delta.PartInfoMaker;
import de.dal33t.powerfolder.util.delta.RollingAdler32;
import de.dal33t.powerfolder.util.delta.RollingChecksum;

/**
 * Testcase for "delta encoding". 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 */
public class DeltaTest extends TestCase {
	private final static int ADLER_RS = 10;
	
	public void testAdler() {
		// Reference implementation from SUN, too bad it doesn't support rolling
		Adler32 ref = new Adler32();
		
		RollingChecksum ch = new RollingAdler32(ADLER_RS);
		ch.update("Wikipedia".getBytes(Charset.forName("ASCII")));
		// Taken straight from the wikipedia site
		assertEquals(ch.getValue(), 0x11E60398);
		ch.reset();
		// Test timing on large input
		long millis = System.currentTimeMillis();
		for (int i = 0; i < 5 * 1024 * 1024; i++) {
			ch.update((byte) i);
		}
		// On my system the time difference is ~80ms so this should work for slower machines too
		// Of course this test is kind of stupid, maybe someone else has a good idea for
		// performance testing ?
		assertTrue(System.currentTimeMillis() - millis < 1000);
		ch.reset();
		byte[] data = new byte[2048];
		for (int i = 0; i < data.length; i++ ) {
			data[i] = (byte) (Math.random() * 256);
		}
		ch.update(data, 0, 2048);
		ref.update(data, data.length - ADLER_RS, ADLER_RS);
		long cs1 = ch.getValue();
		ch.reset();
		ch.update(data, 1024, 1024);
		assertEquals(ref.getValue(), cs1);
		assertEquals(cs1, ch.getValue());
		ch.reset();
		ch.update(data, 10, 5);
		cs1 = ch.getValue();
	}
	
	public void testPartInfos() throws NoSuchAlgorithmException, IOException {
		PartInfoMaker pim = new PartInfoMaker(new Adler32(), 
				MessageDigest.getInstance("SHA-256"));
		byte[] data = new byte[1024 * 1024];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) (Math.random() * 256);
		}
		final PartInfo[] pi = pim.createPartInfos(new ByteArrayInputStream(data), 128);
		Adler32 ref = new Adler32();
		for (int i = 0; i < data.length / 128; i++) {
			ref.reset();
			ref.update(data, i * 128, 128);
			assertTrue("Failed at index " + i + ", expected " + ref.getValue() + " but got " + pi[i].getChecksum(), ref.getValue() == pi[i].getChecksum());
		}

		// Block searching test
		Map<Long, PartInfo> m = new HashMap<Long, PartInfo>();
		for (PartInfo p: pi) {
			m.put(p.getChecksum(), p);
		}
		RollingAdler32 ra = new RollingAdler32(128);
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		ra.update(data, 0, 127);
		int matches = 0;
		for (int i = 127; i < data.length; i++) {
			ra.update(data[i]);
			long sum = ra.getValue();
			PartInfo p = m.get(sum);
			if (p != null) {
				// Potential match
				sha256.update(data, i - 127, 128);
				if (Arrays.equals(sha256.digest(), p.getDigest())) {
					// This SHOULD be a match
					for (int j = 0; j < 128; j++) {
						assertEquals(data[i - 127 + j], data[(int) (128 * p.getIndex() + j)]);
					}
					matches++;
				}
			}
		}
		// Make sure we found all frames (Maybe even more due to randomness)
		assertTrue(matches >= pi.length);
	}
}
