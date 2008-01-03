package de.dal33t.powerfolder.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default thread factory. Creates threads with a name prefix.
 */
public class NamedThreadFactory implements ThreadFactory {
    private static final Logger LOG = Logger
        .getLogger(NamedThreadFactory.class);

    private final UncaughtExceptionHandler exceptionHandler = new ExceptionHandler();
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;

    public NamedThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        t.setUncaughtExceptionHandler(exceptionHandler);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

    private final class ExceptionHandler implements UncaughtExceptionHandler {
        public void uncaughtException(Thread t1, Throwable e) {
            e.printStackTrace();
            LOG.error("Exception in " + t1 + ": " + e.toString(), e);
        }
    }
}