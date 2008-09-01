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
 * $Id: LogDispatch.java 4282 2008-06-16 03:25:09Z harry $
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.UIController;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang.StringUtils;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import java.awt.Color;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

/**
 * Class to log messages. Ideally, classes should extend Loggable and log
 * messages using that, which calles this class. LogDispatch can be used
 * directly where a class cannot extend Loggable and in static situations. <p/>
 * Loggable maintains a Map of Loggers for each class. <p/> Supported levels
 * are:
 * <ul>
 * <li>SEVERE</li>
 * <li>WARNING</li>
 * <li>INFO</li>
 * <li>FINE</li>
 * <li>FINER</li>
 * </ul>
 */
public class LogDispatch {

    /**
     * Map of loggers for logging.
     */
    private static final String DEBUG_DIR = "debug";

    private static boolean logToTextPanelEnabled;
    private static boolean logToFileEnabled;
    private static String logFileName;
    private static boolean loggingConfigured;
    private static final StyledDocument logBuffer = new DefaultStyledDocument();
    private static final Map<String, SimpleAttributeSet> logColors = new HashMap<String, SimpleAttributeSet>();
    private static int nLogLines = 1000;
    private static Level loggingLevel;

    // For adding prefixes to the log messages. For Multi-controller tests.
    private static boolean logPrefixes;
    private static ThreadLocal<String> LOG_PREFIX = new ThreadLocal<String>();

    private static final AtomicBoolean shownOutOfMemoryErrorWarning = new AtomicBoolean();
    private static UIController uiController;

    static {
        // Initially turn logging off logs.
        // Controller will set real level as required.
        Logger rootLogger = getRootLogger();
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                // We do console logging ourselves.
                rootLogger.removeHandler(handler);
            }
            handler.setLevel(Level.OFF);
            handler.setFormatter(new LogFormatter());
        }
        rootLogger.setLevel(Level.OFF);
        loggingLevel = Level.OFF;

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

    /**
     * Set the uiController for showing OutOfMemoryErrors.
     * 
     * @param uiController
     */
    public static void setUIController(UIController uiController) {
        LogDispatch.uiController = uiController;
    }

    /**
     * @param prefix
     *            the prefix in log message.
     */
    public static void setCurrentPrefix(String prefix) {
        LOG_PREFIX.set(prefix);
    }

    public static String getCurrentPrefix() {
        return LOG_PREFIX.get();
    }

    /**
     * Log a message at FINE level.
     * 
     * @param clazz
     * @param message
     */
    public static void logFiner(String className, String message) {
        log(Level.FINER, className, message, null);
    }

    /**
     * Log a message at FINER level.
     * 
     * @param className
     * @param message
     */
    public static void logFine(String className, String message) {
        log(Level.FINE, className, message, null);
    }

    /**
     * Log a message at INFO level.
     * 
     * @param className
     * @param message
     */
    public static void logInfo(String className, String message) {
        log(Level.INFO, className, message, null);
    }

    /**
     * Log a message at WARNING level.
     * 
     * @param className
     * @param message
     */
    public static void logWarning(String className, String message) {
        log(Level.WARNING, className, message, null);
    }

    /**
     * Log a message at SEVERE level.
     * 
     * @param className
     * @param message
     */
    public static void logSevere(String className, String message) {
        log(Level.SEVERE, className, message, null);
    }

    /**
     * Log a message and Throwable at FINER level.
     * 
     * @param className
     * @param message
     * @param t
     */
    public static void logFiner(String className, String message, Throwable t) {
        log(Level.FINER, className, message, t);
    }

    /**
     * Log a message and Throwable at FINE level.
     * 
     * @param className
     * @param message
     * @param t
     */
    public static void logFine(String className, String message, Throwable t) {
        log(Level.FINE, className, message, t);
    }

    /**
     * Log a message and Throwable at INFO level.
     * 
     * @param className
     * @param message
     * @param t
     */
    public static void logInfo(String className, String message, Throwable t) {
        log(Level.INFO, className, message, t);
    }

    /**
     * Log a message and Throwable at WARNING level.
     * 
     * @param className
     * @param message
     * @param t
     */
    public static void logWarning(String className, String message, Throwable t)
    {
        log(Level.WARNING, className, message, t);
    }

    /**
     * Log a message and Throwable at SEVERE level.
     * 
     * @param className
     * @param message
     * @param t
     */
    public static void logSevere(String className, String message, Throwable t)
    {
        log(Level.SEVERE, className, message, t);
    }

    /**
     * Logs a Throwable at FINER level.
     * 
     * @param className
     * @param t
     */
    public static void logFiner(String className, Throwable t) {
        log(Level.FINER, className, null, t);
    }

    /**
     * Logs a Throwable at FINE level.
     * 
     * @param className
     * @param t
     */
    public static void logFine(String className, Throwable t) {
        log(Level.FINE, className, null, t);
    }

    /**
     * Logs a Throwable at INFO level.
     * 
     * @param className
     * @param t
     */
    public static void logInfo(String className, Throwable t) {
        log(Level.INFO, className, null, t);
    }

    /**
     * Logs a Throwable at WARNING level.
     * 
     * @param className
     * @param t
     */
    public static void logWarning(String className, Throwable t) {
        log(Level.WARNING, className, null, t);
    }

    /**
     * Logs a Throwable at SEVERE level.
     * 
     * @param className
     * @param t
     */
    public static void logSevere(String className, Throwable t) {
        log(Level.SEVERE, className, null, t);
    }

    /**
     * Resets the logbuffer with a max number of buffers lines
     * 
     * @param lines
     */
    public static void setLogBuffer(int lines) {
        if (lines < 2) {
            throw new IllegalArgumentException(
                "Number of logbuffer lines must be at least 2");
        }
        nLogLines = lines;
    }

    /**
     * Convenience method for getting the root logger.
     * 
     * @return
     */
    private static Logger getRootLogger() {
        return Logger.getLogger("");
    }

    private static void log(Level level, String className, String message,
        Throwable t)
    {

        Reject.ifNull(level, "Level null");
        Reject.ifNull(className, "Class name null");
        Reject.ifTrue(message == null && t == null,
            "Message and throwable both null");

        Logger logger = Logger.getLogger(className);

        if (message == null) {
            logger.log(level, "", t);
        } else {
            if (t == null) {
                logger.log(level, message);
            } else {
                logger.log(level, message, t);
            }
        }

        if (level.intValue() >= loggingLevel.intValue()) {

            // Console logging.
            logToConsole(level, className, message, t);

            // Log to the debug panel
            if (logToTextPanelEnabled) {
                logToTextPanel(level, className, message, t);
            }
        }

        // Handle OutOfMemoryError (once only)
        if (t != null && t instanceof OutOfMemoryError
            && !shownOutOfMemoryErrorWarning.get() && uiController != null)
        {
            shownOutOfMemoryErrorWarning.set(true);
            uiController.showOutOfMemoryError((OutOfMemoryError) t);
        }
    }

    /**
     * Console logging. Warnings and Severe to System.err, the rest to
     * System.out.
     * 
     * @param level
     * @param className
     * @param message
     * @param t
     */
    private static void logToConsole(Level level, String className,
        String message, Throwable t)
    {
        String formattedMessage = formatMessage(level, className, message, t);
        if (level.equals(Level.WARNING) || level.equals(Level.SEVERE)) {
            System.err.print(formattedMessage);
        } else {
            System.out.print(formattedMessage);
        }
    }

    /**
     * Log details to the text panel for debug panel.
     * 
     * @param level
     * @param className
     * @param message
     * @param t
     */
    private static void logToTextPanel(Level level, String className,
        String message, Throwable t)
    {

        final MutableAttributeSet set = logColors.get(level.getName());
        final String formattedMessage = formatMessage(level, className,
            message, t);

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    synchronized (logBuffer) {
                        logBuffer.insertString(logBuffer.getLength(),
                            formattedMessage, set);
                        if (logBuffer.getLength() > nLogLines) {
                            logBuffer.remove(0, formattedMessage.length());
                        }
                    }
                } catch (RuntimeException e) {
                    // e.printStackTrace();
                } catch (BadLocationException e) {
                    // e.printStackTrace();
                }
            }
        });
    }

    private static String formatMessage(Level level, String className,
        String message, Throwable t)
    {
        StringBuilder sb = new StringBuilder(1000);
        if (logPrefixes) {
            if (getCurrentPrefix() != null) {
                sb.append(getCurrentPrefix());
            }
        }
        sb.append('[');
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        sb.append(sdf.format(new Date()));
        sb.append("] ");
        sb.append(level);
        sb.append(" [");
        int pos = className.lastIndexOf('.');
        if (pos >= 0) {
            sb.append(className.substring(pos + 1, className.length()));
        } else {
            sb.append(className);
        }
        sb.append("]: ");
        sb.append(message);
        sb.append('\n');
        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(sw, true);
                t.printStackTrace(pw);
            } finally {
                if (pw != null) {
                    pw.flush();
                    pw.close();
                }
            }
            sw.flush();
            String trace = sw.toString();
            sb.append(trace).append('\n');
        }

        return sb.toString();
    }

    /**
     * Gets the logging level for a class.
     * 
     * @param className
     * @return
     */
    public static Level getLevel(String className) {
        return Logger.getLogger(className).getLevel();
    }

    /**
     * Is logging to file enabled?
     * 
     * @return
     */
    public static boolean isLogToFileEnabled() {
        return logToFileEnabled && logFileName != null
            && !logFileName.equals("");
    }

    /**
     * Enables logging to file.
     * 
     * @param selected
     */
    public static void setLogFileEnabled(boolean selected) {
        logToFileEnabled = selected;
        configureLogFileHandler();
    }

    /**
     * Sets the logfile for all logging output (verbose included)
     * 
     * @param logFilename
     */
    public static void setLogFile(String logFileNameArg) {
        logFileName = logFileNameArg;
        configureLogFileHandler();
    }

    /**
     * Clears existing file handlers and adds a new one if required.
     */
    private static void configureLogFileHandler() {

        Logger rootLogger = getRootLogger();
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                FileHandler fileHandler = (FileHandler) handler;
                try {
                    fileHandler.flush();
                    fileHandler.close();
                    rootLogger.removeHandler(handler);
                    logInfo(LogDispatch.class.getName(),
                        "Removed FileHandler from logging handlers.");
                } catch (Exception e) {
                    logSevere(LogDispatch.class.getName(), e);
                }
            }
        }

        if (isLogToFileEnabled()) {
            try {
                String f = Util.removeInvalidFilenameChars(logFileName);
                int i = f.lastIndexOf('.');
                if (i > 0) {
                    // Insert number before extenstion.
                    f = f.substring(0, i) + ".%g" + f.substring(i, f.length());
                } else {
                    f += ".%g";
                }
                FileHandler fileHandeler = new FileHandler(getDebugDir()
                    .getAbsolutePath()
                    + '/' + f, 100000, 10, true);

                fileHandeler.setFormatter(new LogFormatter());
                fileHandeler.setLevel(rootLogger.getLevel());
                rootLogger.addHandler(fileHandeler);
                logInfo(LogDispatch.class.getName(), "Added FileHandler ("
                    + logFileName + ") to logging handlers.");
            } catch (SecurityException e) {
                logSevere(LogDispatch.class.getName(), e);
            } catch (IOException e) {
                logSevere(LogDispatch.class.getName(), e);
            }
        }
    }

    /**
     * Sets the logging level. Sets root, handler and internal logging level.
     * 
     * @param level
     */
    public static void setLevel(Level level) {
        Reject.ifNull(level, "Level is null");
        Logger rootLogger = getRootLogger();
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(level);
        }
        // Root logger.
        rootLogger.setLevel(level);
        loggingConfigured = true;
        loggingLevel = level;
        logInfo(LogDispatch.class.getName(), "Logging level set to " + level);
    }

    /**
     * @return the directory for debug output.
     */
    public static File getDebugDir() {
        File canidate = new File(DEBUG_DIR);
        if (canidate.exists() && canidate.isDirectory()) {
            return canidate;
        }
        canidate.mkdirs();
        if (!canidate.exists() || !canidate.isDirectory()) {
            // Fallback! TRAC #1087
            canidate = new File(Controller.getMiscFilesLocation(), DEBUG_DIR);
            canidate.mkdirs();
        }
        if (!canidate.exists() || !canidate.isDirectory()) {
            // Completely fail
        }
        return canidate;
    }

    /**
     * Any handlers enabled?
     * 
     * @return
     */
    public static boolean isEnabled() {
        return logToFileEnabled || logToTextPanelEnabled;
    }

    /**
     * Has logging been configued, or is it still the default?
     * 
     * @return
     */
    public static boolean isLoggingConfigured() {
        return loggingConfigured;
    }

    /**
     * Enables/Disables the TextPanel loggin. <p/> Clears the logging document
     * if log disabled
     * 
     * @param enabled
     */
    public static void setEnabledTextPanelLogging(boolean enabled) {
        logToTextPanelEnabled = enabled;
    }

    /**
     * @return true if printing of nick of the current controller in front of
     *         the log messages is enabled.
     */
    public static boolean isNickPrefix() {
        return logPrefixes;
    }

    /**
     * Enables/Disables printing the nick of the current controller in front of
     * the log messages.
     * 
     * @param nickPrefix
     */
    public static void setNickPrefix(boolean nickPrefix) {
        LogDispatch.logPrefixes = nickPrefix;
    }

    /**
     * @return the log buffer, for the debug panel.
     */
    public static StyledDocument getLogBuffer() {
        return logBuffer;
    }

    public static Level getLoggingLevel() {
        return loggingLevel;
    }

    /**
     * LogRecord formatter.
     */
    private static class LogFormatter extends Formatter {
        public String format(LogRecord record) {
            StringBuilder buf = new StringBuilder(1000);
            if (logPrefixes) {
                if (getCurrentPrefix() != null) {
                    buf.append(getCurrentPrefix());
                }
            }
            buf.append('[');
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
            buf.append(sdf.format(new Date(record.getMillis())));
            buf.append("] ");
            buf.append(record.getLevel());
            buf.append(" [");
            String loggerName = record.getLoggerName();
            int pos = loggerName.lastIndexOf('.');
            if (pos >= 0) {
                buf.append(loggerName.substring(pos + 1, loggerName.length()));
            } else {
                buf.append(loggerName);
            }
            buf.append("]: ");
            buf.append(record.getMessage());
            buf.append('\n');
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
                String trace = sw.toString();
                buf.append(trace).append('\n');
            }
            return buf.toString();
        }
    }
}
