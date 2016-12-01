/*
 * Copyright 2004 - 2016 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link ScheduledThreadPoolExecutor} that wraps all {@link Runnable}s to log their exceptions and errors. Also delegates all
 * actual execution into an unlimited threadpool. Periodically scheduled task may run concurrently!
 *
 * @author sprajc
 */
public class WrappedScheduledThreadPoolExecutor
    extends ScheduledThreadPoolExecutor
{
    private static final Logger LOG = Logger
        .getLogger(WrappedScheduledThreadPoolExecutor.class.getName());

    /**
     * The threadpool actually executing the scheduled tasks.
     */
    private WrapperExecutorService executingThreadPool;

    public WrappedScheduledThreadPoolExecutor(int corePoolSize,
        ThreadFactory threadFactory)
    {
        super(corePoolSize, threadFactory);
        executingThreadPool = new WrapperExecutorService(
            Executors.newCachedThreadPool(threadFactory));
    }

    // Overriding ************************************************************

    // Not overriden because super class calls schedule(..)

    // @Override
    // public void execute(Runnable command) {
    // super.execute(new WrappedRunnable(command));
    // }
    //
    // @Override
    // public Future<?> submit(Runnable task) {
    // return super.submit(new WrappedRunnable(task));
    // }

    // @Override
    // public <T> Future<T> submit(Callable<T> task) {
    // return super.submit(new WrappedCallable<T>(task));
    // }
    //
    // @Override
    // public <T> Future<T> submit(Runnable task, T result) {
    // return super.submit(new WrappedRunnable(task), result);
    // }

    @Override
    public void shutdown() {
        try {
            super.shutdown();            
        } finally {
            executingThreadPool.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks = new LinkedList<Runnable>();
        try {
            tasks.addAll(super.shutdownNow());
        } finally {
            tasks.addAll(executingThreadPool.shutdownNow());
        }
        return tasks;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
        TimeUnit unit)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay,
        TimeUnit unit)
    {
        checkBusyness();
        return super.schedule(new SchedueledRunnable(command), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
        long initialDelay, long period, TimeUnit unit)
    {
        checkBusyness();
        return super.scheduleAtFixedRate(new SchedueledRunnable(command),
            initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
        long initialDelay, long delay, TimeUnit unit)
    {
        checkBusyness();
        return super.scheduleWithFixedDelay(new SchedueledRunnable(command),
            initialDelay, delay, unit);
    }
    
    // Internal helper ********************************************************

    private void checkBusyness() {
        if (getActiveCount() >= getCorePoolSize()) {
            int queueSize = getQueue().size();
            Level l = Level.WARNING;
            if (queueSize > getCorePoolSize() * 10) {
                l = Level.SEVERE;
            }
            LOG.log(l,
                "Scheduled threadpool is exhausted. Got " + getQueue().size()
                    + " tasks in queue. Currently active threads: "
                    + getActiveCount() + "/" + getCorePoolSize());
        }
        // TODO: Check busyness of executingThreadPool
    }
    
    private class SchedueledRunnable implements Runnable {
        private Runnable toBeExecuted;

        public SchedueledRunnable(Runnable toBeExecuted) {
            super();
            Reject.ifNull(toBeExecuted, "Runnable to be execute is null");
            this.toBeExecuted = toBeExecuted;
        }

        @Override
        public void run() {
            executingThreadPool.submit(toBeExecuted);
        }
    }
}
