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
package de.dal33t.powerfolder.test;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.security.AdminPermission;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class ControllerTest extends ControllerTestCase {
    private volatile boolean run;

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

    public void testRestart() {
        getController().shutdown();
        Debug.dumpThreadStacks();
    }

    public void testDistrubution() {
        assertEquals("PowerFolder.jar", getController().getJARName());
        assertEquals("PowerFolder.l4j.ini", getController().getL4JININame());
    }

    public void testThreadPool() throws InterruptedException,
        ExecutionException
    {
        getController().getThreadPool().schedule(new Runnable() {
            public void run() {
                throw new NullPointerException("Broken code");
            }
        }, 0, TimeUnit.MILLISECONDS);

        getController().getThreadPool().scheduleWithFixedDelay(new Runnable() {
            public void run() {
                throw new NullPointerException("Broken code");
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        run = false;
        ScheduledFuture<?> f = getController().getThreadPool().schedule(
            new Runnable() {
                public void run() {
                    System.out.println("Completed");
                    run = true;
                }
            }, 0, TimeUnit.MILLISECONDS);
        f.get();
        assertEquals(true, f.isDone());
        assertTrue(run);
    }

    /**
     * PFS-2232 / PFC-2941
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void testManyThreadPoolTasks()
        throws InterruptedException, ExecutionException
    {
        final AtomicBoolean interrupted = new AtomicBoolean();
        // 1) Schedule tasks and wait for execution
        int nTasks = Constants.CONTROLLER_MIN_THREADS_IN_THREADPOOL * 10;
        int waitMS = 1000;
        for (int i = 0; i < nTasks; i++) {
            getController().getThreadPool().schedule(new Runnable() {
                public void run() {
                    try {
                        TestHelper.waitMilliSeconds(waitMS);
                        System.out.print(".");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        interrupted.set(true);
                    }
                }
            }, 0, TimeUnit.MILLISECONDS);
            TestHelper.waitMilliSeconds(1);
        }
        TestHelper.waitMilliSeconds(500);

        // 2) Terminate
        getController().getThreadPool().shutdown();
        getController().getThreadPool().awaitTermination(waitMS * 2,
            TimeUnit.MILLISECONDS);
        List<Runnable> remainingTasks = getController().getThreadPool()
            .shutdownNow();
        TestHelper.waitMilliSeconds(1000);

        // 3) Check empty threadpool
        // Two tasks may remain:
        // LimitedConnectivityChecker
        // Controller#performHousekeeping
        assertTrue(
            "Not two tasks remaining. Got " + remainingTasks.size()
                + " tasks remaining: " + remainingTasks,
            remainingTasks.size() <= 2);
        assertFalse(
            "Tasks were not completed, but cancelled. Threadpool was likely exhausted", interrupted.get());
    }
}
