package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.util.os.NetworkUtil;
import junit.framework.TestCase;

public class NativeNetUtil extends TestCase {
	public void testNetUtil() {
		NetworkUtil nu = NetworkUtil.getInstance();
		assertNotNull(nu);
		for (String[] s: nu.getInterfaceAddresses()) {
			assertEquals(s.length, 2);
		}
	}
}
