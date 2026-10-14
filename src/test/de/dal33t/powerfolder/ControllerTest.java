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
 *
 */
package de.dal33t.powerfolder;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.security.AdminPermission;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;
import static org.junit.jupiter.api.Assertions.*;

public final class ControllerTest extends ControllerTestCase {
    private volatile boolean run;

    @Test
    public void testActionMemoryLeak() {
        BaseAction action;
        ConfigurationEntry.SECURITY_PERMISSIONS_STRICT.setValue(
            getController(), true);
        for (int i = 0; i < 200; i++) {
            action = new MyAction(getController());
            action.allowWith(AdminPermission.INSTANCE);
        }

        // System.gc();
        // TestHelper.waitMilliSeconds(10000);
        getController().getOSClient().login("xxx", "dd".toCharArray());
        // TestHelper.waitMilliSeconds(60000);
        //
        // ------------------
        // Test with Profiler comes here:
        // No BoundPermission objects should exists here after forcing
        // GC ---------------
    }

    @SuppressWarnings("serial")
    private static final class MyAction extends BaseAction {

        protected MyAction(Controller controller) {
            super("Name", null, controller);
        }

        public void actionPerformed(ActionEvent e) {

        }

    }

    @Test
    public void testRestart() {
        getController().shutdown();
        Debug.dumpThreadStacks();
    }

    @Test
    public void testDistrubution() {
        assertEquals("PowerFolder.jar", getController().getJARName());
        assertEquals("PowerFolder.l4j.ini", getController().getL4JININame());
    }

    @Test
    public void testThreadPool() throws InterruptedException,
        ExecutionException
    {
        getController().getThreadPool().schedule(new Runnable() {
            public void run() {
                throw new NullPointerException("Intentionally broken code for test throwing exception");
            }
        }, 0, TimeUnit.MILLISECONDS);

        getController().getThreadPool().scheduleWithFixedDelay(new Runnable() {
            public void run() {
                throw new NullPointerException("Intentionally broken code for test throwing exception");
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        run = false;
        ScheduledFuture<?> f = getController().getThreadPool().schedule(
            new Runnable() {
                public void run() {
                    run = true;
                }
            }, 100, TimeUnit.MILLISECONDS);
        f.get();
        assertTrue( f.isDone(),"Future is not done yet");
        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return run;
            }
            
            @Override
            public String message() {
                return "Not run yet!";
            }
        });
        assertTrue( run,"Not run yet");
    }

    @Test
    public void testAvoidPeaks() {
        ConfigurationEntry.FOLDER_WATCHER_ENABLED.setValue(getController(), false);
        int nFolders = 500;
        for (int i = 0; i < nFolders; i++) {
            FolderInfo foInfo = FolderInfoFactory.newTopFolderForTest("Test-Folder" + i);
            joinFolder(foInfo,
                getController().getFolderRepository().getFoldersBasedir()
                    .resolve(foInfo.getName()),
                SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        }
        Collection<Folder> testFolders = new ArrayList<>();
        for (Folder folder : getController().getFolderRepository().getFolders(false)) {
            if (folder.getName().startsWith("Test-Folder")) {
                testFolders.add(folder);
            }
        }
        assertEquals(nFolders, testFolders.size());
        StatisticCalculationCounter calculated = new StatisticCalculationCounter(testFolders);

        List<Long> delays = new ArrayList<>();
        for (int round = 0; round < 3; round++) {
            Map<Folder, Integer> before = calculated.snapshot();
            Map<Folder, Long> scheduled = scheduleStatisticCalculationOf(testFolders);
            delays.addAll(scheduled.values());
            calculated.waitForCalculationsSince(before, scheduled.keySet());
        }

        assertTrue(delays.size() >= nFolders,
            "Only " + delays.size() + " calculations were scheduled for " + nFolders + " folders");
        int slotMillis = 100;
        Map<Long, Integer> dueInSlot = new HashMap<>();
        for (Long delay : delays) {
            dueInSlot.merge(delay / slotMillis, 1, Integer::sum);
        }
        int peak = Collections.max(dueInSlot.values());
        assertTrue(peak <= delays.size() / 6,
            "Statistic calculations are not spread: " + peak + " of " + delays.size()
                + " are due within the same " + slotMillis + "ms slot: " + dueInSlot);
    }

    private Map<Folder, Long> scheduleStatisticCalculationOf(Collection<Folder> folders) {
        Map<Folder, Long> scheduled = new HashMap<>();
        for (Folder folder : folders) {
            if (!folder.isStarted()) {
                continue;
            }
            long delay = folder.getStatistic().scheduleCalculate();
            if (delay >= 0) {
                scheduled.put(folder, delay);
            }
        }
        return scheduled;
    }

    private static final class StatisticCalculationCounter extends FolderAdapter {
        private final Map<Folder, AtomicInteger> calculations = new ConcurrentHashMap<>();

        StatisticCalculationCounter(Collection<Folder> folders) {
            for (Folder folder : folders) {
                calculations.put(folder, new AtomicInteger());
                folder.addFolderListener(this);
            }
        }

        @Override
        public void statisticsCalculated(FolderEvent folderEvent) {
            calculations.get(folderEvent.getFolder()).incrementAndGet();
        }

        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        Map<Folder, Integer> snapshot() {
            Map<Folder, Integer> counts = new HashMap<>();
            for (Map.Entry<Folder, AtomicInteger> entry : calculations.entrySet()) {
                counts.put(entry.getKey(), entry.getValue().get());
            }
            return counts;
        }

        void waitForCalculationsSince(Map<Folder, Integer> before, Collection<Folder> folders) {
            TestHelper.waitForCondition(30, new ConditionWithMessage() {
                @Override
                public boolean reached() {
                    return pending(before, folders).isEmpty();
                }

                @Override
                public String message() {
                    return "Statistics not calculated for " + pending(before, folders);
                }
            });
        }

        private List<Folder> pending(Map<Folder, Integer> before, Collection<Folder> folders) {
            List<Folder> pending = new ArrayList<>();
            for (Folder folder : folders) {
                if (calculations.get(folder).get() <= before.get(folder)) {
                    pending.add(folder);
                }
            }
            return pending;
        }
    }

    private static final class ConcurrencyTracker {
        private final AtomicInteger running = new AtomicInteger();
        private final AtomicInteger peak = new AtomicInteger();

        void enter() {
            peak.accumulateAndGet(running.incrementAndGet(), Math::max);
        }

        void exit() {
            running.decrementAndGet();
        }

        int peak() {
            return peak.get();
        }

        int running() {
            return running.get();
        }
    }

    @Test
    public void testScheduledTasks() {
        int nTasks = Constants.CONTROLLER_MIN_THREADS_IN_THREADPOOL * 10;
        int waitMS = 1000;
        ConcurrencyTracker tracker = new ConcurrencyTracker();
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean overlapped = new AtomicBoolean();
        List<AtomicInteger> runsPerTask = new ArrayList<>();
        List<ScheduledFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < nTasks; i++) {
            AtomicBoolean busy = new AtomicBoolean();
            AtomicInteger runs = new AtomicInteger();
            runsPerTask.add(runs);
            futures.add(getController().getThreadPool().scheduleWithFixedDelay(() -> {
                if (!busy.compareAndSet(false, true)) {
                    overlapped.set(true);
                    return;
                }
                tracker.enter();
                try {
                    TestHelper.waitMilliSeconds(waitMS);
                    runs.incrementAndGet();
                } catch (RuntimeException e) {
                    interrupted.set(true);
                } finally {
                    tracker.exit();
                    busy.set(false);
                }
            }, 1, 1, TimeUnit.MILLISECONDS));
            TestHelper.waitMilliSeconds(1);
        }
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return runsPerTask.stream().allMatch(runs -> runs.get() >= 2);
            }

            @Override
            public String message() {
                return "Runs per task: " + runsPerTask;
            }
        });
        for (ScheduledFuture<?> future : futures) {
            future.cancel(false);
        }
        TestHelper.waitForCondition(10, () -> tracker.running() == 0);

        assertFalse(overlapped.get(), "A fixed delay task was run concurrently with itself");
        assertTrue(tracker.peak() <= nTasks,
            "Saw a too big peak in concurrently running tasks: " + tracker.peak());
        assertFalse(interrupted.get(), "Tasks were cancelled");
    }

    @Test
    public void testManyThreadPoolTasks() throws InterruptedException {
        int nTasks = Constants.CONTROLLER_MIN_THREADS_IN_THREADPOOL * 100;
        int waitMS = 1000;
        ConcurrencyTracker tracker = new ConcurrencyTracker();
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicInteger completed = new AtomicInteger();
        for (int i = 0; i < nTasks; i++) {
            getController().getThreadPool().schedule(() -> {
                tracker.enter();
                try {
                    TestHelper.waitMilliSeconds(waitMS);
                    completed.incrementAndGet();
                } catch (RuntimeException e) {
                    interrupted.set(true);
                } finally {
                    tracker.exit();
                }
            }, 1, TimeUnit.MILLISECONDS);
            TestHelper.waitMilliSeconds(1);
        }
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return completed.get() == nTasks;
            }

            @Override
            public String message() {
                return "Completed " + completed.get() + " of " + nTasks + " tasks";
            }
        });
        assertFalse(interrupted.get(), "Tasks were cancelled. Threadpool was likely exhausted");
        assertTrue(tracker.peak() > Constants.CONTROLLER_MIN_THREADS_IN_THREADPOOL,
            "Threadpool did not grow beyond its core size, peak was " + tracker.peak());
        assertTrue(tracker.peak() <= nTasks,
            "Saw a too big peak in concurrently running tasks: " + tracker.peak());

        getController().getThreadPool().shutdown();
        getController().getThreadPool().awaitTermination(waitMS * 2,
            TimeUnit.MILLISECONDS);
        List<Runnable> remainingTasks = getController().getThreadPool()
            .shutdownNow();
        assertTrue(remainingTasks.size() <= 5,
            "Too many tasks remaining. Got " + remainingTasks.size()
                + " tasks remaining: " + remainingTasks);
    }
}
