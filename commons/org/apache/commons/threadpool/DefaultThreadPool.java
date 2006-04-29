/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.threadpool;

/**
 * A default implementation of a ThreadPool which is constructed with a given
 * number of threads.
 * 
 * @author <a href="mailto:jstrachan@apache.org">James Strachan </a>
 * @version $Revision: 1.1 $
 */
public class DefaultThreadPool implements Runnable, ThreadPool {

    private MTQueue queue = new MTQueue();
    private boolean stopped = false;
    private final ThreadPoolMonitor monitor;
    private ThreadGroup threadGroup;
    private static int THREAD_NO = 1;

    public DefaultThreadPool(ThreadPoolMonitor monitor, int numberOfThreads,
        int threadPriority)
    {
        this.monitor = monitor;
        for (int i = 0; i < numberOfThreads; i++) {
            startThread(threadPriority);
        }
    }

    public DefaultThreadPool(ThreadPoolMonitor monitor, int numberOfThreads) {
        this.monitor = monitor;
        for (int i = 0; i < numberOfThreads; i++) {
            startThread();
        }
    }

    public DefaultThreadPool() {
        this.monitor = new CommonsLoggingThreadPoolMonitor();
        // typically a thread pool should have at least 1 thread
        startThread();
    }

    public DefaultThreadPool(int numberOfThreads) {
        this(new CommonsLoggingThreadPoolMonitor(), numberOfThreads);
    }

    public DefaultThreadPool(int numberOfThreads, int threadPriority) {
        this(new CommonsLoggingThreadPoolMonitor(), numberOfThreads,
            threadPriority);
    }

    public void setThreadGroup(ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    /** Start a new thread running */
    public Thread startThread() {
        Thread thread = createThread();
        thread.start();
        return thread;
    }

    public Thread startThread(int priority) {
        Thread thread = createThread();
        thread.setPriority(priority);
        thread.start();
        return thread;
    }

    private Thread createThread() {
        String threadName = "PoolThread " + THREAD_NO++;
        if (this.threadGroup != null) {
            return new Thread(this.threadGroup, this, threadName);
        }
        return new Thread(this, threadName);
    }

    public void stop() {
        stopped = true;
    }

    /**
     * Returns number of runnable object in the queue.
     */
    public int getRunnableCount() {
        return queue.size();
    }

    // ThreadPool interface
    //-------------------------------------------------------------------------

    /**
     * Dispatch a new task onto this pool to be invoked asynchronously later
     */
    public void invokeLater(Runnable task) {
        queue.add(task);
    }

    // Runnable interface
    //-------------------------------------------------------------------------

    /**
     * The method ran by the pool of background threads
     */
    public void run() {
        while (!stopped) {
            Runnable task = (Runnable) queue.remove();
            if (task != null) {
                try {
                    task.run();
                } catch (Throwable t) {
                    monitor.handleThrowable(this.getClass(), task, t);
                }
            }
        }
    }
}