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
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import de.dal33t.powerfolder.util.Reject;

/**
 * A handler that buffers a given amount of {@link LogRecord}s in memory. Older
 * entries get discarded.
 *
 * @author sprajc
 */
public class BufferedHandler extends Handler {

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

    public void clear() {
        logRecords.clear();
    }

    public List<String> getFormattedLogLines(int nSize, Formatter formatter,
        Level level)
    {
        synchronized (logRecords) {
            List<String> lines = new ArrayList<String>(logRecords.size());
            int nLines = Math.min(nSize, logRecords.size());
            for (int i = 0; i < nLines; i++) {
                LogRecord record = logRecords.get(i);
                if (level != null
                    && record.getLevel().intValue() < level.intValue())
                {
                    continue;
                }
                String formattedMessage = formatter.format(record);
                formattedMessage = buildLinks(formattedMessage);
                formattedMessage = formattedMessage.replace(
                    "de.dal33t.powerfolder.", "");
                lines.add(formattedMessage);
            }
            return lines;
        }
    }

    private String buildLinks(String formattedMessage) {
        String orig = formattedMessage;
        int x = 0;
        int t = 0;
        while (x >= 0) {
            x = formattedMessage.toLowerCase().indexOf("http", x + 1);
            t++;
            if (t > 100) {
                // Something is wrong here.
                return orig;
            }
            if (x >= 0) {
                if (formattedMessage.charAt(x - 1) == '\''
                    || formattedMessage.charAt(x - 1) == '"')
                {
                    x++;
                    continue;
                }
                int sx = formattedMessage.indexOf(" ", x);
                if (sx < x) {
                    sx = formattedMessage.indexOf(",", x);
                }
                if (sx < x) {
                    sx = formattedMessage.indexOf("'", x);
                }
                if (sx < x) {
                    sx = formattedMessage.indexOf("\n", x);
                }
                if (sx < 0) {
                    sx = formattedMessage.length() + 1;
                }
                if (x > 0 && sx > x) {
                    sx = sx - 1;
                    String url = formattedMessage.substring(x, sx);
                    int len = formattedMessage.length();
                    formattedMessage = formattedMessage.substring(0, x)
                        + "<a target='_blank' href='"
                        + url
                        + "'>"
                        + url
                        + "</a>"
                        + formattedMessage.substring(sx,
                            formattedMessage.length());

                    len = formattedMessage.length() - len;
                    x += len;
                }
            }
        }
        return formattedMessage;
    }
}
