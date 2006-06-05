package de.dal33t.powerfolder.test.util;

import java.lang.reflect.Field;

import de.dal33t.powerfolder.util.TransferCounter;
import junit.framework.TestCase;

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
        
        // Starts out with counter2 active
        for (int i = 0; i < period / 1000; i++) {
            tc.calculateCurrentCPS();
            assertEquals("Counter2 run " + i, false, ac.getBoolean(tc));
            Thread.sleep(1000);
        }
        // Now the switch should happen
        tc.calculateCurrentCPS();
        Thread.sleep(1000);
        tc.calculateCurrentCPS();
        for (int i = 1; i < period / 1000; i++) {
            tc.calculateCurrentCPS();
            assertEquals("Counter1 run " + i, true, ac.getBoolean(tc));
            Thread.sleep(1000);
        }
    }
}
