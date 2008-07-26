/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.util;

import junit.framework.TestCase;
import de.dal33t.powerfolder.PFComponent;

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

    public class TestLogger extends PFComponent {
        public TestLogger() {
            // logFine("Test");
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
            if (isLogFiner()) {
                logCount++;
            }
            if (logInfo) {
                logCount++;
            }
            if (logWarn) {
                logCount++;
            }
            if (isLogFine()) {
                logCount++;
            }
            
        }
    }
    
    
}
