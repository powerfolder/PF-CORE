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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

public class ControllerTest extends ControllerTestCase {
    private volatile boolean run;

    public void testRestart() {
        getController().shutdown();
        Debug.dumpThreadStacks();
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
}
