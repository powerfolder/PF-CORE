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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private WrapperExecutorService wrappingThreadPool;
    private ThreadPoolExecutor executingThreadPool;

    public WrappedScheduledThreadPoolExecutor(int corePoolSize,
        ThreadFactory threadFactory)
    {
        super(corePoolSize, threadFactory);
        executingThreadPool = (ThreadPoolExecutor) Executors
            .newCachedThreadPool(threadFactory);
        wrappingThreadPool = new WrapperExecutorService(executingThreadPool);
    }

    // Overriding ************************************************************

    @Override
    public void execute(Runnable command) {
        if (command instanceof WrappedRunnable
            || command instanceof ScheduledRunnable)
        {
            super.execute(command);
            return;
        }
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Decorating exec: " + command);            
        }
        super.execute(new ScheduledRunnable(command, true));
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable,
        RunnableScheduledFuture<V> task)
    {
        if (runnable instanceof WrappedRunnable
            || runnable instanceof ScheduledRunnable)
        {
            return super.decorateTask(runnable, task);
        }
        LOG.warning("Decorating task: " + runnable);
        return super.decorateTask(new ScheduledRunnable(runnable, true), task);
    }
    
    // Not overriden because super class calls schedule(..)
    
    // @Override
    // public Future<?> submit(Runnable task) {
    //
    // LOG.warning("Submitt: " + task);
    // return super.submit(new WrappedRunnable(task));
    // }
    //
    // @Override
    // public <T> Future<T> submit(Callable<T> task) {
    // LOG.warning("Submitt: " + task);
    // return super.submit(new WrappedCallable<T>(task));
    // }
    // //
    // @Override
    // public <T> Future<T> submit(Runnable task, T result) {
    // LOG.warning("Submitt: " + task);
    // return super.submit(new WrappedRunnable(task), result);
    // }

    @Override
    public void shutdown() {
        try {
            super.shutdown();            
        } finally {
            wrappingThreadPool.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks = new LinkedList<Runnable>();
        try {
            tasks.addAll(super.shutdownNow());
        } finally {
            tasks.addAll(wrappingThreadPool.shutdownNow());
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
        return super.schedule(new ScheduledRunnable(command, true), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
        long initialDelay, long period, TimeUnit unit)
    {
        checkBusyness();
        return super.scheduleAtFixedRate(new ScheduledRunnable(command, true),
            initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
        long initialDelay, long delay, TimeUnit unit)
    {
        checkBusyness();
        return super.scheduleWithFixedDelay(new ScheduledRunnable(command, false),
            initialDelay, delay, unit);
    }
    
    // Internal helper ********************************************************

    
    private void checkBusyness() {
        if (getActiveCount() >= getPoolSize()) {
            Level l = Level.FINER;
            if (getActiveCount() > 250) {
                 l = Level.WARNING;
            } else if (getActiveCount() > 1000) {
                l = Level.SEVERE;
            }
            if (LOG.isLoggable(l)) {
                LOG.log(l,
                    "Scheduled threadpool status: Currently active threads: "
                        + getActiveCount() + "/" + getPoolSize());
            }
        }
    }
    
    @Override
    public int getCorePoolSize() {
        return super.getCorePoolSize() + executingThreadPool.getCorePoolSize();
    }

    @Override
    public int getPoolSize() {
        return super.getPoolSize() + executingThreadPool.getPoolSize();
    }

    @Override
    public int getActiveCount() {
        return super.getActiveCount() + executingThreadPool.getActiveCount();
    }

    private class ScheduledRunnable implements Runnable {
        private Runnable task;
        private boolean concurrentExecutionAllowed;
        private AtomicBoolean running = new AtomicBoolean(false);

        public ScheduledRunnable(Runnable task,
            boolean concurrentExecutionAllowed)
        {
            super();
            Reject.ifNull(task, "Runnable to be execute is null");
            this.task = task;
            this.concurrentExecutionAllowed = concurrentExecutionAllowed;
        }

        @Override
        public void run() {
            checkBusyness();
            wrappingThreadPool.submit(() -> {
                if (concurrentExecutionAllowed
                    || running.compareAndSet(false, true))
                {
                    try {
                        task.run();
                    } finally {
                        running.set(false);
                    }
                } else {
                    //LOG.warning("Skipping execution of " + task);
                }
            });
        }
    }
}
