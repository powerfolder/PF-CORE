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
 * $Id: DocumentHandler.java 4734 2008-07-28 03:14:24Z harry $
 */
package de.dal33t.powerfolder.util.logging.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.LoggingFormatter;

/**
 * A handler that buffers a given amount of {@link LogRecord}s in memory. Older
 * entries get discarded.
 * 
 * @author sprajc
 */
public class BufferedHandler extends Handler {

    private static ThreadLocal<LoggingFormatter> formatterThreadLocal = new ThreadLocal<LoggingFormatter>()
    {
        protected LoggingFormatter initialValue() {
            return new LoggingFormatter();
        }
    };

    private List<LogRecord> logRecords;
    // private Level level;
    private int size;

    public BufferedHandler(int size) {
        super();
        Reject.ifTrue(size <= 0 || size >= 10000, "Illegal size " + size);
        logRecords = Collections.synchronizedList(new LinkedList<LogRecord>());
        this.size = size;
        // INFO by default
        // level = Level.INFO;
    }

    @Override
    public void close() throws SecurityException {
        logRecords.clear();
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        synchronized (logRecords) {
            logRecords.add(0, record);
            if (logRecords.size() > size) {
                // Discard the oldest log record
                logRecords.remove(logRecords.size() - 1);
            }
        }
    }

    // API ********************************************************************

    public List<String> getFormattedLogLines() {
        synchronized (logRecords) {
            List<String> lines = new ArrayList<String>(logRecords.size());
            for (int i = 0; i < logRecords.size(); i++) {
                LogRecord record = logRecords.get(logRecords.size() - i - 1);
                String formattedMessage = formatterThreadLocal.get().format(
                    record);
                lines.add(formattedMessage);
            }
            return lines;
        }
    }

}
