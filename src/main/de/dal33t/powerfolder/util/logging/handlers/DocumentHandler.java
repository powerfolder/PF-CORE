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
package de.dal33t.powerfolder.util.logging.handlers;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.dal33t.powerfolder.util.UIThemeState;
import de.dal33t.powerfolder.util.logging.LoggingFormatter;

/**
 * Document handler class. This formats log records and appends them to
 * a styled document for display in the DebugPanel.
 */
public class DocumentHandler extends Handler {

    private static int numberOfLogCharacters = 50000;

    /** Delay between batched appends to the log buffer. */
    static final int FLUSH_DELAY_MS = 250;

    /** Upper bound of records waiting for the next flush; the oldest are dropped beyond it. */
    static final int MAX_PENDING_RECORDS = 2000;

    private final StyledDocument logBuffer = new DefaultStyledDocument();

    private final Queue<Pending> pending = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingCount = new AtomicInteger();
    private final AtomicBoolean flushScheduled = new AtomicBoolean();
    /** Created and used on the EDT only. */
    private Timer flushTimer;

    // PFC-3643: per-level log colours for the debug panel, theme-aware. The old
    // single set used Color.BLACK for INFO (invisible on the dark theme) and dark
    // blue/green that were unreadable on a dark background. We keep one set tuned for
    // the light theme and a brighter set for dark, and pick at render time.
    private static final Map<String, SimpleAttributeSet> logColorsLight =
            new HashMap<String, SimpleAttributeSet>();
    private static final Map<String, SimpleAttributeSet> logColorsDark =
            new HashMap<String, SimpleAttributeSet>();

    private static ThreadLocal<LoggingFormatter> formatterThreadLocal =
            new ThreadLocal<LoggingFormatter>() {
                protected LoggingFormatter initialValue() {
                    return new LoggingFormatter();
                }
            };

    private static void putColor(Map<String, SimpleAttributeSet> map,
        Level level, Color color)
    {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);
        map.put(level.getName(), set);
    }

    static {
        // Light theme: all log text black, regardless of level.
        putColor(logColorsLight, Level.SEVERE, Color.BLACK);
        putColor(logColorsLight, Level.WARNING, Color.BLACK);
        putColor(logColorsLight, Level.INFO, Color.BLACK);
        putColor(logColorsLight, Level.FINE, Color.BLACK);
        putColor(logColorsLight, Level.FINER, Color.BLACK);

        // Dark theme: all log text white (matches the client's text foreground),
        // regardless of level.
        Color white = UIThemeState.DARK_FOREGROUND;
        putColor(logColorsDark, Level.SEVERE, white);
        putColor(logColorsDark, Level.WARNING, white);
        putColor(logColorsDark, Level.INFO, white);
        putColor(logColorsDark, Level.FINE, white);
        putColor(logColorsDark, Level.FINER, white);
    }

    public void close() throws SecurityException {
    }

    public void flush() {
    }

    /**
     * Queue a log record for the log buffer. Records are formatted on the calling thread and appended in batches,
     * at most once per {@link #FLUSH_DELAY_MS} on the EDT, so busy logging does not flood the event queue.
     *
     * @param record
     */
    public void publish(final LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String formattedMessage;
        try {
            formattedMessage = formatterThreadLocal.get().format(record);
        } catch (RuntimeException e) {
            return;
        }
        pending.add(new Pending(record.getLevel().getName(), formattedMessage));
        // Bounded: older records would be trimmed from the document anyway.
        if (pendingCount.incrementAndGet() > MAX_PENDING_RECORDS && pending.poll() != null) {
            pendingCount.decrementAndGet();
        }
        if (flushScheduled.compareAndSet(false, true)) {
            EventQueue.invokeLater(this::startFlushTimer);
        }
    }

    /** Runs on the EDT. */
    private void startFlushTimer() {
        if (flushTimer == null) {
            flushTimer = new Timer(FLUSH_DELAY_MS, e -> flushPending());
            flushTimer.setRepeats(false);
        }
        flushTimer.start();
    }

    /** Appends all queued records to the log buffer. Runs on the EDT. */
    private void flushPending() {
        // Reset first: a record queued while draining schedules the next flush.
        flushScheduled.set(false);
        Map<String, SimpleAttributeSet> colors = UIThemeState.isDark() ? logColorsDark : logColorsLight;
        StringBuilder run = new StringBuilder();
        MutableAttributeSet runSet = null;
        try {
            synchronized (logBuffer) {
                Pending p;
                while ((p = pending.poll()) != null) {
                    pendingCount.decrementAndGet();
                    MutableAttributeSet set = colors.get(p.level);
                    if (run.length() > 0 && set != runSet) {
                        logBuffer.insertString(logBuffer.getLength(), run.toString(), runSet);
                        run.setLength(0);
                    }
                    runSet = set;
                    run.append(p.message);
                }
                if (run.length() > 0) {
                    logBuffer.insertString(logBuffer.getLength(), run.toString(), runSet);
                }
                int rem = logBuffer.getLength() - numberOfLogCharacters;
                if (rem > 0) {
                    logBuffer.remove(0, rem);
                }
            }
        } catch (RuntimeException e) {
            // Ignore
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    /** A formatted record waiting for the next flush. */
    private static final class Pending {
        private final String level;
        private final String message;

        private Pending(String level, String message) {
            this.level = level;
            this.message = message;
        }
    }

    /**
     * Resets the logbuffer with a max number of buffers characters
     *
     * @param characters
     */
    public static void setLogBuffer(int characters) {
        if (characters < 20) {
            throw new IllegalArgumentException(
                "Number of logbuffer characters must be at least 20");
        }
        numberOfLogCharacters = characters;
    }

    /**
     * @return the log buffer, for the debug panel.
     */
    public StyledDocument getLogBuffer() {
        return logBuffer;
    }

}
