package de.dal33t.powerfolder.test.ui;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;

public class DelayedUpdaterTest extends TestCase {
    private DelayedUpdater updater;
    private List<Date> udates;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        updater = new DelayedUpdater();
        updater.setDelay(1000);
        udates = new CopyOnWriteArrayList<Date>();
    }

    public void testSingleEvent() {
        long start = System.currentTimeMillis();
        updater.schedule(new Update());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return udates.size() == 1;
            }
        });
        assertEquals(1, udates.size());
        long took = System.currentTimeMillis() - start;
        assertTrue("Update took " + took + "ms", took >= updater.getDelay());
    }

    public void testTwoEvents() {
        long start = System.currentTimeMillis();
        updater.schedule(new Update());
        TestHelper.waitMilliSeconds(500);
        // Override old
        updater.schedule(new Update());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return udates.size() == 1;
            }
        });
        assertEquals(1, udates.size());
        long took = System.currentTimeMillis() - start;
        long sinceLastEvent = System.currentTimeMillis()
            - udates.get(0).getTime();
        assertTrue("Updates took " + took + "ms", took >= updater.getDelay());
        assertTrue("Updates took " + took
            + "ms, should not really take longer than single", took <= 1100);
        assertTrue("Should not have passed more that 100ms after last event",
            sinceLastEvent < 100);
    }

    public void testMultipleEvents() {
        // About to discard about 90% of the events
        for (int i = 0; i < 100; i++) {
            updater.schedule(new Update());
            TestHelper.waitMilliSeconds(101);
        }

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return udates.size() >= 10;
            }

            public String message() {
                return "Got only " + udates.size() + " updates";
            }
        });

        assertEquals(10, udates.size());
    }

    private class Update implements Runnable {
        public void run() {
            udates.add(new Date());
        }
    }
}
