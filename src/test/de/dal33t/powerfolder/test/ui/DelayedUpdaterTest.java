package de.dal33t.powerfolder.test.ui;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class DelayedUpdaterTest extends ControllerTestCase {
    private DelayedUpdater updater;
    private List<Date> updates;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        updater = new DelayedUpdater(getController());
        updater.setDelay(1000);
        updates = new CopyOnWriteArrayList<Date>();
    }

    public void testSingleEvent() {
        long start = System.currentTimeMillis();
        updater.schedule(new Update());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return updates.size() == 1;
            }
        });
        assertEquals(1, updates.size());
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
                return updates.size() == 1;
            }
        });
        assertEquals(1, updates.size());
        long took = System.currentTimeMillis() - start;
        long sinceLastEvent = System.currentTimeMillis()
            - updates.get(0).getTime();
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
                return updates.size() >= 10;
            }

            public String message() {
                return "Got only " + updates.size() + " updates";
            }
        });
        assertTrue("Got wrong number of updates: " + updates.size(),
                updates.size() >= 10 && updates.size() <= 12);
    }

    private class Update implements Runnable {
        public void run() {
            updates.add(new Date());
        }
    }
}
