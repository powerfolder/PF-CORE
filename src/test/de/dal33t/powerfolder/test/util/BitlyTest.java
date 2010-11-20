package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.util.Bitly;
import junit.framework.TestCase;

public class BitlyTest extends TestCase {

    public void testShorten() {
        assertEquals("http://bit.ly/biSd0y", Bitly.shorten("http://www.betaworks.com"));
        assertEquals("http://bit.ly/dA8mXG", Bitly.shorten("http://www.powerfolder.com"));
    
    }
}
