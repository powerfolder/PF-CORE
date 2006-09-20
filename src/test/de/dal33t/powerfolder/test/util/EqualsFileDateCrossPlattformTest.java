package de.dal33t.powerfolder.test.util;

import java.util.Date;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Util;

public class EqualsFileDateCrossPlattformTest extends TestCase {

    public void testDefault() {
        assertTrue(Util.equalsFileDateCrossPlattform(new Date(11111111), new Date(11111000)));
        assertFalse(Util.equalsFileDateCrossPlattform(new Date(222222222), new Date(222220000)));
    }
    
    public void testSpecial() {
        assertTrue(Util.equalsFileDateCrossPlattform(new Date(1146605870000L), new Date(1146605868805L)));
    }
}
