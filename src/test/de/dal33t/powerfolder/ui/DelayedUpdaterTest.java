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
        waitForUpdates(1);
        assertEquals(1, updates.size());
        long took = updates.get(0).getTime() - start;
        assertTrue( took >= updater.getDelay(),"Update took " + took + "ms");
    }

    @Test
    public void testTwoEvents() {
        long start = System.currentTimeMillis();
        updater.schedule(new Update());
        TestHelper.waitMilliSeconds(500);
        // Override old
        updater.schedule(new Update());
        waitForUpdates(1);
        assertEquals(1, updates.size());
        long took = updates.get(0).getTime() - start;
        assertTrue( took >= updater.getDelay(),"Updates took " + took + "ms");
        assertTrue( took <= 1500,"Updates took " + took
                + "ms, should not really take longer than single");
    }

    @Test
    public void testMultipleEvents() {
        int nEvents = 100;
        int interval = 107;
        long start = System.currentTimeMillis();
        for (int i = 0; i < nEvents; i++) {
            updater.schedule(new Update());
            TestHelper.waitMilliSeconds(interval);
        }
        long scheduling = System.currentTimeMillis() - start;
        int minExpectedUpdates = (int) (scheduling / (updater.getDelay() + interval));
        int maxExpectedUpdates = (int) (scheduling / updater.getDelay()) + 1;

        waitForUpdates(minExpectedUpdates);
        assertTrue(
                updates.size() >= minExpectedUpdates
                    && updates.size() <= maxExpectedUpdates,"Got wrong number of updates: " + updates.size() + " not within ["
                    + minExpectedUpdates + "," + maxExpectedUpdates + "]: " + updates);
    }


    private void waitForUpdates(final int expected) {
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return updates.size() >= expected;
            }

            public String message() {
                return "Got only " + updates.size() + " of " + expected
                    + " updates: " + updates;
            }
        });
    }

    private class Update implements Runnable {
        public void run() {
            updates.add(new Date() { public String toString() { return getTime() + ""; } });
        }
    }
}
