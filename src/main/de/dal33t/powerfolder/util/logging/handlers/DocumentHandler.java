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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.dal33t.powerfolder.util.logging.LoggingFormatter;

/**
 * Document handler class. This formats log records and appends them to
 * a styled document for display in the DebugPanel.
 */
public class DocumentHandler extends Handler {

    private static int numberOfLogCharacters = 50000;

    private final StyledDocument logBuffer = new DefaultStyledDocument();

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
        Color white = new Color(0xF5F5F5);
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
                    Map<String, SimpleAttributeSet> colors =
                        de.dal33t.powerfolder.ui.LookAndFeelSupport.isDarkMode()
                            ? logColorsDark : logColorsLight;
                    MutableAttributeSet set = colors.get(record.getLevel()
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
