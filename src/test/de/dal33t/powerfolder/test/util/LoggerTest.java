package de.dal33t.powerfolder.test.util;

import java.io.NotActiveException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.TestCase;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.logging.PFFormatter;

public class LoggerTest extends TestCase {
    
    int logCount = 0; 
    

    public void testLogger() {
        // only enable console logging
        Logger.setEnabledConsoleLogging(true);
        Logger.setEnabledTextPanelLogging(false);
        Logger.setEnabledToFileLogging(false);
        new TestLogger();

        assertEquals(1, logCount);
        // all logging disabled
        Logger.setEnabledConsoleLogging(false);
        new TestLogger();
        // no loggin should happen
        assertEquals(1, logCount);
        //enable loggin
        Logger.setEnabledConsoleLogging(true);
        // exclude class
        Logger.addExcludedConsoleClasses(ExcludedTestLogger.class);
        new ExcludedTestLogger();
        //no loggn should have happend
        assertEquals(1, logCount);
        
        //enable logging and exclude ERROR and INFO Levels
        Logger.setEnabledConsoleLogging(true);
        Logger.addExcludeConsoleLogLevel(Logger.ERROR);
        Logger.addExcludeConsoleLogLevel(Logger.INFO);
        
        new ExcludedLevelsTestLogger();
        // 1 + log enabled + warn + debug + verbose
        assertEquals(4, logCount);
    }

    public void testConsoleLogging() throws Exception {
        Logger.setEnabledConsoleLogging(true);
        Logger.removeExcludeConsoleLogLevel(Logger.ERROR);
        Logger.removeExcludeConsoleLogLevel(Logger.INFO);
        Logger.removeExcludeConsoleLogLevel(Logger.WARN);
        Logger logger = Logger.getLogger(new TestLogger());
        logger.info("Test Console (info)");
        logger.warn("Test Console (warn)");
        logger.error("Test Console (error)");
        logger.debug("Test Console (debug)");
    }
    
    public void testFormatter() throws Exception {
        PFFormatter lFormatter = new PFFormatter();
        
        LogRecord lrec = new LogRecord(Level.INFO, "LogRecord");
        lrec.setLoggerName("MyLogger");
        
        //System.out.println(lFormatter.formatMessage( lrec ) );
        
    }
    
    
    public class TestLogger extends PFComponent {
        public TestLogger() {
            // log().debug("Test");
            if (logEnabled) {                
                logCount++;
            }
        }
    }

    public class ExcludedTestLogger extends PFComponent {
        public ExcludedTestLogger() {
            if (logEnabled) {
                logCount++;
            }
        }
    }
    
    public class ExcludedLevelsTestLogger extends PFComponent {
        public ExcludedLevelsTestLogger() {
            if (logEnabled) {
                logCount++;
            }
            if (logError) {
                logCount++;
            }
            if (logVerbose) {
                logCount++;
            }
            if (logInfo) {
                logCount++;
            }
            if (logWarn) {
                logCount++;
            }
            if (logDebug) {
                logCount++;
            }
            
        }
    }
    
    
}
