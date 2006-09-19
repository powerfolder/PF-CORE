package de.dal33t.powerfolder.test.util;

import junit.framework.TestCase;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Logger;

public class LoggerTest extends TestCase {
    boolean logEnabledFlag = false;
    public void testLogger() {
        
        Logger.setEnabledConsoleLogging(true);
        new TestLogger();
        assertTrue(logEnabledFlag);
        
    }
    
    public class TestLogger extends PFComponent {
        public TestLogger() {
            //log().debug("Test");
            if (logEnabled) {
                logEnabledFlag = true;
            }
        }
    }
}
