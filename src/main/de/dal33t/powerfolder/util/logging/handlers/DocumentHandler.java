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
* $Id: DocumentHandler.java 4734 2008-07-28 03:14:24Z harry $
*/
package de.dal33t.powerfolder.util.logging.handlers;

import de.dal33t.powerfolder.util.logging.LoggingFormatter;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Document handler class. This formats log records and appends them to
 * a styled document for display in the DebugPanel.
 */
public class DocumentHandler extends Handler {

    private static int numberOfLogCharacters = 50000;

    private final StyledDocument logBuffer = new DefaultStyledDocument();

    private static final Map<String, SimpleAttributeSet> logColors =
            new HashMap<String, SimpleAttributeSet>();

    private static ThreadLocal<LoggingFormatter> formatterThreadLocal =
            new ThreadLocal<LoggingFormatter>() {
                protected LoggingFormatter initialValue() {
                    return new LoggingFormatter();
                }
            };

    static {
        // Initialize logging colors.
        SimpleAttributeSet severe = new SimpleAttributeSet();
        StyleConstants.setForeground(severe, Color.RED);
        logColors.put(Level.SEVERE.getName(), severe);

        SimpleAttributeSet warn = new SimpleAttributeSet();
        StyleConstants.setForeground(warn, Color.BLUE);
        logColors.put(Level.WARNING.getName(), warn);

        SimpleAttributeSet info = new SimpleAttributeSet();
        StyleConstants.setForeground(info, Color.BLACK);
        logColors.put(Level.INFO.getName(), info);

        SimpleAttributeSet fine = new SimpleAttributeSet();
        StyleConstants.setForeground(fine, Color.GREEN.darker());
        logColors.put(Level.FINE.getName(), fine);

        SimpleAttributeSet finer = new SimpleAttributeSet();
        StyleConstants.setForeground(finer, Color.GRAY);
        logColors.put(Level.FINER.getName(), finer);
    }

    public void close() throws SecurityException {
    }

    public void flush() {
    }

    /**
     * Publish a log record to the log buffer.
     *
     * @param record
     */
    public void publish(final LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MutableAttributeSet set = logColors.get(record.getLevel()
                        .getName());
                    String formattedMessage = formatterThreadLocal.get()
                        .format(record);
                    synchronized (logBuffer) {
                        logBuffer.insertString(logBuffer.getLength(),
                            formattedMessage, set);
                        if (logBuffer.getLength() > numberOfLogCharacters) {
                            int rem = (logBuffer.getLength() - numberOfLogCharacters);
                            if (rem > 0) {
                                logBuffer.remove(0, rem);
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    // Ignore
                } catch (BadLocationException e) {
                    // Ignore
                }
            }
        });
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
