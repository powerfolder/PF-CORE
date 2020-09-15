package de.dal33t.powerfolder.util.logging.handlers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * PFS-3596: A handler that counts the number of log messages for each level.
 *
 * @author sprajc
 */
public class CountingHandler extends Handler {
    private AtomicInteger severe = new AtomicInteger(0);
    private AtomicInteger warning = new AtomicInteger(0);

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        if (Level.SEVERE.equals(record.getLevel())) {
            severe.incrementAndGet();
        } else if (Level.WARNING.equals(record.getLevel())) {
            warning.incrementAndGet();
        }
    }

    public int countSevere() {
        return severe.get();
    }

    public int countWarnings() {
        return warning.get();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
