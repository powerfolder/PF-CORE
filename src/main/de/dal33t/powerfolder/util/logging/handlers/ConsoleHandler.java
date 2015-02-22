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
 * $Id: ConsoleHandler.java 4734 2008-07-28 03:14:24Z harry $
 */
package de.dal33t.powerfolder.util.logging.handlers;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import de.dal33t.powerfolder.util.logging.LoggingFormatter;

/**
 * Handler for logging to the console.
 */
public class ConsoleHandler extends Handler {

    private static ThreadLocal<LoggingFormatter> formatterThreadLocal = new ThreadLocal<LoggingFormatter>()
    {
        protected LoggingFormatter initialValue() {
            return new LoggingFormatter();
        }
    };

    public void close() throws SecurityException {
    }

    public void flush() {
        System.out.flush();
        System.err.flush();
    }

    /**
     * Publish a log record to System.out, or System.err if a warning or severe.
     *
     * @param record
     */
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        String formattedMessage = formatterThreadLocal.get().format(record);
        if (record.getParameters() != null
            && record.getParameters().length >= 1
            && record.getParameters()[0] instanceof String)
        {
            String prefix = (String) record.getParameters()[0];
            while (prefix.length() < 6) {
                prefix += " ";
            }
            formattedMessage = prefix + " | " + formattedMessage;
        }
        Level level = record.getLevel();
        if (level.equals(Level.WARNING) || level.equals(Level.SEVERE)) {
            System.err.print(formattedMessage);
        } else {
            System.out.print(formattedMessage);
        }
    }
}
