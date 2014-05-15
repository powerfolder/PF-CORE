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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link ScheduledThreadPoolExecutor} that wraps all {@link Runnable}s and
 * {@link Callable}s to log their exceptions and errors
 *
 * @author sprajc
 */
public class WrapperExecutorService implements ExecutorService {
    private ExecutorService deligate;

    public WrapperExecutorService(ExecutorService deligate) {
        Reject.ifNull(deligate, "Deligate is null");
        this.deligate = deligate;
    }

    // Overriding ************************************************************

    public void execute(Runnable command) {
        deligate.execute(new WrappedRunnable(command));
    }

    public <T> Future<T> submit(Callable<T> task) {
        return deligate.submit(new WrappedCallable<T>(task));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return deligate.submit(new WrappedRunnable(task), result);
    }

    public Future<?> submit(Runnable task) {
        return deligate.submit(new WrappedRunnable(task));
    }

    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        return deligate.awaitTermination(timeout, unit);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException
    {
        return deligate.invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException
    {
        return deligate.invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException
    {
        return deligate.invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException
    {
        return deligate.invokeAny(tasks, timeout, unit);
    }

    public boolean isShutdown() {
        return deligate.isShutdown();
    }

    public boolean isTerminated() {
        return deligate.isTerminated();
    }

    public void shutdown() {
        deligate.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return deligate.shutdownNow();
    }

}
