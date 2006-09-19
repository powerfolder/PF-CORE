package de.dal33t.powerfolder.test.util;

import junit.framework.TestCase;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Logger;

public class LoggerTest extends TestCase {
    boolean logEnabledFlag = false;
    int logCount = 0; 
    boolean logEnabledFlagExcludedClass = false;

    public void testLogger() {
        // only enable console logging
        Logger.setEnabledConsoleLogging(true);
        Logger.setEnabledTextPanelLogging(false);
        Logger.setEnabledToFileLogging(false);
        new TestLogger();
        assertTrue(logEnabledFlag);
        assertEquals(1, logCount);
        // all logging disabled
        Logger.setEnabledConsoleLogging(false);
        new TestLogger();
        // no loggin should happen
        assertEquals(1, logCount);
        //enable
        Logger.setEnabledConsoleLogging(true);        
        Logger.addExcludedConsoleClasses(ExcludedTestLogger.class);
        new ExcludedTestLogger();
        assertFalse(logEnabledFlagExcludedClass);
    }

    public class TestLogger extends PFComponent {
        public TestLogger() {
            // log().debug("Test");
            if (logEnabled) {
                logEnabledFlag = true;
                logCount++;
            }
        }
    }

    public class ExcludedTestLogger extends PFComponent {
        public ExcludedTestLogger() {
            if (logEnabled) {
                logEnabledFlagExcludedClass = true;
            }
        }
    }
}
