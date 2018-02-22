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

import java.lang.reflect.Field;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.TransferCounter;

public class TransferCounterTest extends TestCase {
    public void testTransferCounter() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException {
        System.out.println("testTransferCounter test");
        TransferCounter tc = new TransferCounter();
        Field ac;
        ac = TransferCounter.class.getDeclaredField("CURRENT_CPS_CALCULATION_PERIOD");
        ac.setAccessible(true);
        long period = ac.getLong(tc);

        ac = TransferCounter.class.getDeclaredField("counter1Active");

        ac.setAccessible(true);
        tc.startedTransfer();

        // Starts out with counter2 active
        for (int i = 0; i < period / 1000; i++) {
            tc.calculateCurrentCPS();
            assertEquals("Counter2 run " + i, false, ac.getBoolean(tc));
            Thread.sleep(1000);
        }
        // Now the switch should happen
        tc.calculateCurrentCPS();
        Thread.sleep(900);
        tc.calculateCurrentCPS();
        for (int i = 1; i < period / 1000; i++) {
            tc.calculateCurrentCPS();
            assertEquals("Counter1 run " + i, true, ac.getBoolean(tc));
            Thread.sleep(1000);
        }
        tc.stoppedTransfer();
    }
}
