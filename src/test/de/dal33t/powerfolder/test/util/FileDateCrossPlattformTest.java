package de.dal33t.powerfolder.test.util;

import java.util.Date;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.Util;

public class FileDateCrossPlattformTest extends TestCase {

    public void testDefault() {
        assertTrue(Util.equalsFileDateCrossPlattform(new Date(1000), new Date(3000)));
        assertTrue(Util.equalsFileDateCrossPlattform(new Date(11111111), new Date(11111000)));
        assertFalse(Util.equalsFileDateCrossPlattform(new Date(222222222), new Date(222220000)));
        
        // 2000 milliseconds we asssume the same 
        assertFalse(Util.isNewerFileDateCrossPlattform(new Date(3000),new Date(1000)));
        // 2001 milliseconds we asssume different
        assertTrue(Util.isNewerFileDateCrossPlattform(new Date(3001),new Date(1000)));
        // other is newer
        assertFalse(Util.isNewerFileDateCrossPlattform(new Date(1000),new Date(3001)));
    }
    
    public void testSpecial() {
        assertTrue(Util.equalsFileDateCrossPlattform(new Date(1146605870000L), new Date(1146605868805L)));
    }
}
