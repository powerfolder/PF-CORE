/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: LoggingFormatter.java 4734 2008-07-28 03:14:24Z harry $
 */
package de.dal33t.powerfolder.util.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;

/**
 * Class to format a LogRecord in a readable form. Displays as [hh:mm:ss] level
 * [logger name] message.
 */
public class LoggingFormatter extends Formatter {
    private static final String POWERFOLDER_LCASE = "powerfolder";
    private static final String POWERFOLDER = "PowerFolder";
    private static final String DAL33T = ".dal33t";

    private boolean showDate;
    private boolean replacePF;

    public LoggingFormatter(boolean showDate) {
        super();
        this.showDate = showDate;
        this.replacePF = !POWERFOLDER.equals(Constants.MISC_DIR_NAME);
    }

    public LoggingFormatter() {
        this(false);
    }

    /**
     * Format a log record nicely.
     * 
     * @param record
     * @return
     */
    public String format(LogRecord record) {
        String levelDescription;
        if (record.getLevel().equals(Level.OFF)) {
            levelDescription = "OFF   ";
        } else if (record.getLevel().equals(Level.WARNING)) {
            levelDescription = "WARN  ";
        } else if (record.getLevel().equals(Level.SEVERE)) {
            levelDescription = "SEVERE";
        } else if (record.getLevel().equals(Level.FINE)) {
            levelDescription = "FINE  ";
        } else if (record.getLevel().equals(Level.FINER)) {
            levelDescription = "FINER ";
        } else if (record.getLevel().equals(Level.INFO)) {
            levelDescription = "INFO  ";
        } else {
            levelDescription = record.getLevel().getName();
        }
        StringBuilder buf = new StringBuilder(300);
        buf.append('[');
        SimpleDateFormat sdf;
        if (showDate) {
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } else {
            sdf = new SimpleDateFormat("HH:mm:ss");
        }
        buf.append(sdf.format(new Date(record.getMillis())));
        buf.append("] ");
        buf.append(levelDescription);
        buf.append(" [");
        String loggerName = record.getLoggerName();
        int pos = loggerName.lastIndexOf('.');
        if (pos >= 0) {
            buf.append(loggerName.substring(pos + 1, loggerName.length()));
        } else {
            buf.append(loggerName);
        }
        buf.append("]: ");
        buf.append(replacePFString(record.getMessage()));
        buf.append(Util.getLineFeed());
        if (record.getThrown() != null) {
            Throwable throwable = record.getThrown();
            StringWriter sw = new StringWriter();
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(sw, true);
                throwable.printStackTrace(pw);
            } finally {
                if (pw != null) {
                    pw.flush();
                    pw.close();
                }
            }
            sw.flush();
            String trace = replacePFString(sw.toString());
            buf.append(trace).append(Util.getLineFeed());
        }
        return buf.toString();
    }

    private String replacePFString(String input) {
        if (!replacePF) {
            return input;
        }
        if (StringUtils.isBlank(input)) {
            return input;
        }
        return input.replace(POWERFOLDER, Constants.MISC_DIR_NAME)
            .replace(POWERFOLDER_LCASE, Constants.MISC_DIR_NAME)
            .replace(DAL33T, "");
    }
}
