/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ScheduledThreadPoolExecutor} that wraps all {@link Runnable}s and
 * {@link Callable}s to log their exceptions and errors
 *
 * @author sprajc
 */
public class WrappedScheduledThreadPoolExecutor extends
    ScheduledThreadPoolExecutor
{
    public WrappedScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public WrappedScheduledThreadPoolExecutor(int corePoolSize,
        ThreadFactory threadFactory)
    {
        super(corePoolSize, threadFactory);
    }

    public WrappedScheduledThreadPoolExecutor(int corePoolSize,
        RejectedExecutionHandler handler)
    {
        super(corePoolSize, handler);
    }

    public WrappedScheduledThreadPoolExecutor(int corePoolSize,
        ThreadFactory threadFactory, RejectedExecutionHandler handler)
    {
        super(corePoolSize, threadFactory, handler);
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
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
        TimeUnit unit)
    {
        return super.schedule(new WrappedCallable<V>(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay,
        TimeUnit unit)
    {
        return super.schedule(new WrappedRunnable(command), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
        long initialDelay, long period, TimeUnit unit)
    {
        return super.scheduleAtFixedRate(new WrappedRunnable(command),
            initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
        long initialDelay, long delay, TimeUnit unit)
    {
        return super.scheduleWithFixedDelay(new WrappedRunnable(command),
            initialDelay, delay, unit);
    }

}
