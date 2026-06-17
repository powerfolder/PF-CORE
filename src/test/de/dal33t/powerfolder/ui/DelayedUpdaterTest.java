/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class DelayedUpdaterTest extends ControllerTestCase {
    private DelayedUpdater updater;
    private List<Date> updates;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        updater = new DelayedUpdater(getController());
        updater.setDelay(1000);
        updates = new CopyOnWriteArrayList<Date>();
    }

    @Test
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
        assertTrue( took >= updater.getDelay(),"Update took " + took + "ms");
    }

    @Test
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
        assertTrue( took >= updater.getDelay(),"Updates took " + took + "ms");
        assertTrue( took <= 1500,"Updates took " + took
                + "ms, should not really take longer than single");
        assertTrue(
            sinceLastEvent < 200,"Should not have passed more that 200ms after last event");
    }

    @Test
    public void testMultipleEvents() {
        // About to discard about 90% of the events
        for (int i = 0; i < 100; i++) {
            updater.schedule(new Update());
            TestHelper.waitMilliSeconds(107);
        }

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return updates.size() >= 10;
            }

            public String message() {
                return "Got only " + updates.size() + " updates";
            }
        });
        assertTrue(
                updates.size() >= 10 && updates.size() <= 20,"Got wrong number of updates: " + updates.size() + ": " + updates);
    }

    private class Update implements Runnable {
        public void run() {
            updates.add(new Date() { public String toString() { return getTime() + ""; } });
        }
    }
}
