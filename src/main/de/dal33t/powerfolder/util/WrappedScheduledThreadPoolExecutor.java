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
package de.dal33t.powerfolder.util;

import java.util.*;
import java.util.concurrent.*;
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

    public static int WARN_NUMBER_WORKERS;
    public static int SEVERE_NUMBER_WORKERS;
    static {
        setWarningLevel(500);
    }
    
    /**
     * The threadpool actually executing the scheduled tasks.
     */
    private WrapperExecutorService wrappingThreadPool;
    private ThreadPoolExecutor executingThreadPool;

    // Stats
    private Map<Class, Integer> classCountRunning;

    public WrappedScheduledThreadPoolExecutor(int corePoolSize,
        ThreadFactory threadFactory)
    {
        super(corePoolSize, threadFactory);
        executingThreadPool = (ThreadPoolExecutor) Executors
            .newCachedThreadPool(threadFactory);
        wrappingThreadPool = new WrapperExecutorService(executingThreadPool);
        Comparator<Class> classComparator = (Class o1, Class o2) -> o1.getName().compareTo(o2.getName());
        this.classCountRunning = Collections.synchronizedMap(new TreeMap<>(classComparator));
    }

    public static void setWarningLevel(int nThreads){
        WARN_NUMBER_WORKERS = Math.max(500, nThreads);
        SEVERE_NUMBER_WORKERS = 3 * WARN_NUMBER_WORKERS;
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

    private Date lastDump = new Date(System.currentTimeMillis() - 1000L * 30);

    private void checkBusyness() {
        int activeCount = getActiveCount();
        if (activeCount == 0) {
            return;
        }
        if (lastDump.after(new Date(System.currentTimeMillis() - 1000L * 30))) {
            return;
        }
        Level l = Level.FINER;
        if (activeCount > SEVERE_NUMBER_WORKERS) {
            l = Level.SEVERE;
        } else if (activeCount > WARN_NUMBER_WORKERS) {
            l = Level.WARNING;
        }
        if (LOG.isLoggable(l)) {
            StringBuffer b = new StringBuffer("Active tasks:");
            synchronized (classCountRunning) {
                for (Class clazz : classCountRunning.keySet()) {
                    b.append("\n");
                    b.append(clazz.getName());
                    b.append("\t");
                    b.append(classCountRunning.get(clazz));
                }
            }
            LOG.log(l, "Scheduled threadpool status: Currently active threads: "
                            + activeCount + "/" + getPoolSize() + "\n" + b);
            lastDump = new Date();
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
                    ProfilingEntry pe = null;
                    try {
                        Integer count = classCountRunning.get(task.getClass());
                        classCountRunning.put(task.getClass(), count == null ? 1 : count + 1);
                        pe = Profiling.start(task.getClass(), "run");
                        task.run();
                    } finally {
                        running.set(false);
                        Profiling.end(pe);
                        // PF-1791: Remove synchronized blocks. Trade off. Causes lots of locks otherwise
                        Integer count = classCountRunning.get(task.getClass());
                        count = count == null ? 1 : count;
                        count--;
                        if (count == 0) {
                            classCountRunning.remove(task.getClass());
                        } else {
                            classCountRunning.put(task.getClass(), count);
                        }
                    }
                } else {
                    //LOG.warning("Skipping execution of " + task);
                }
            });
        }
    }
}
