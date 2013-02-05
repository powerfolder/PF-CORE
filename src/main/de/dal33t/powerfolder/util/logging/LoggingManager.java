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
 * $Id: LoggingManager.java 4734 2008-07-28 03:14:24Z harry $
 */
package de.dal33t.powerfolder.util.logging;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.text.StyledDocument;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.handlers.BufferedHandler;
import de.dal33t.powerfolder.util.logging.handlers.ConsoleHandler;
import de.dal33t.powerfolder.util.logging.handlers.DocumentHandler;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Class to manage logging handler. This maintains up to three handlers;
 * document, file and console. The file handler is only constructed when
 * required. root logging is adjusted to the minimum required by the handlers.
 * This allows Logger.isLoggable() to optimize based on root logging level. Root
 * logging level is never set above SEVERE, so that runtime exceptions get
 * handled.
 */
public class LoggingManager {

    /** Default debug directory */
    private static final String DEBUG_DIR = "logs";

    /** File Logging file prefix */
    private static String LOGFILE_PREFIX = Constants.MISC_DIR_NAME; // "PowerFolder"
    private static String LOGFILE_SUFFIX = "-log.txt";

    /** The document handler for the DebugPanel */
    private static final DocumentHandler documentHandler;

    /** The console handler */
    private static final ConsoleHandler consoleHandler;

    /** The buffer handler */
    private static final BufferedHandler bufferedHandler;

    /** The file handler */
    private static FileHandler fileHandler;

    /** Lock object when creating file handler */
    private static final Object fileHandlerLock = new Object();

    /** The document logging level */
    private static Level documentLoggingLevel;

    /** The console logging level */
    private static Level consoleLoggingLevel;

    /** The file logging level */
    private static Level fileLoggingLevel;

    /** The name of the file logging file */
    private static String fileLoggingFileName;

    /** #2585 */
    private static boolean fileRotate;

    /** The buffered logging level */
    private static Level bufferedLoggingLevel;
    /**
     * The default filter for the handlers
     */
    private static Filter DEFAULT_FILTER = new Filter() {
        public boolean isLoggable(LogRecord record) {
            String loggerName = record.getLoggerName();
            if (loggerName == null) {
                return false;
            }
            // Special HACK for EhCache
            if (loggerName.contains("EhCacheProvider")
                && record.getMessage() != null
                && record.getMessage().contains("Could not find configuration"))
            {
                return false;
            }
            if ((loggerName.startsWith("org.hibernate") || loggerName
                .startsWith("com.mchange"))
                && record.getLevel().intValue() > Level.FINE.intValue())
            {
                if (record.getLevel() == Level.INFO) {
                    record.setLevel(Level.FINE);
                }
                return true;
            }
            return loggerName.startsWith("de.dal33t")
                || loggerName.startsWith("net.sf.webdav");
        }
    };

    static {
        Logger rootLogger = getRootLogger();

        // Switch logging nearly off until one of the handlers is configured.
        rootLogger.setLevel(Level.SEVERE);

        // Remove any default log handlers; we do our own logging as required.
        for (Handler handler : rootLogger.getHandlers()) {
            handler.flush();
            handler.close();
            rootLogger.removeHandler(handler);
        }

        // Create loggers, thread-safe in the static initializer.
        consoleHandler = new ConsoleHandler();
        documentHandler = new DocumentHandler();
        bufferedHandler = new BufferedHandler(200);

        rootLogger.setFilter(DEFAULT_FILTER);
        consoleHandler.setFilter(DEFAULT_FILTER);
        documentHandler.setFilter(DEFAULT_FILTER);
        bufferedHandler.setFilter(DEFAULT_FILTER);
    }

    /**
     * Set the console handler level. Add handler to root logger if this is the
     * first time.
     * 
     * @param level
     */
    public static void setConsoleLogging(Level level) {
        if (consoleLoggingLevel == null) {
            getRootLogger().addHandler(consoleHandler);
        }
        consoleLoggingLevel = level;
        consoleHandler.setLevel(level);

        setMinimumBaseLoggingLevel();
    }

    /**
     * Set the document handler level. Add handler to root logger if this is the
     * first time.
     * 
     * @param level
     * @param controller
     */
    public static void setDocumentLogging(Level level, Controller controller) {
        if (documentLoggingLevel == null) {
            getRootLogger().addHandler(documentHandler);
        }
        documentLoggingLevel = level;
        documentHandler.setLevel(level);

        PreferencesEntry.DOCUMENT_LOGGING.setValue(controller, level.getName());

        setMinimumBaseLoggingLevel();
    }

    /**
     * Set the file handler level. Add handler to root logger if this is the
     * first time. Create the file handler inside a synchronized block to stop
     * other threads trying to access it during construction.
     * 
     * @param level
     * @param rotate
     *            to rotate the file every day
     */
    public static void setFileLogging(Level level, boolean rotate) {

        fileLoggingLevel = level;
        fileRotate = rotate;

        if (fileHandler == null) {
            createFileHandler();
        }

        if (fileHandler != null) {
            fileHandler.setLevel(fileLoggingLevel);
        }

        setMinimumBaseLoggingLevel();
    }

    /**
     * Set the console handler level. Add handler to root logger if this is the
     * first time.
     * 
     * @param level
     */
    public static void setBufferedLogging(Level level) {
        if (bufferedLoggingLevel == null) {
            getRootLogger().addHandler(bufferedHandler);
        }
        bufferedLoggingLevel = level;
        bufferedHandler.setLevel(level);

        setMinimumBaseLoggingLevel();
    }

    public static void clearBuffer() {
        if (bufferedHandler != null) {
            bufferedHandler.clear();
        }
    }

    /**
     * Physically create the file handler.
     * 
     * @param level
     */
    private static void createFileHandler() {
        // Make sure nothing else tries to create the file handler concurrently.
        synchronized (fileHandlerLock) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String logFilename = LOGFILE_PREFIX;
                if (fileRotate) {
                    logFilename += '-';
                    logFilename += sdf.format(new Date());
                }
                logFilename += LOGFILE_SUFFIX;
                fileLoggingFileName = new File(getDebugDir(),
                    FileUtils.removeInvalidFilenameChars(logFilename))
                    .getAbsolutePath();
                fileHandler = new FileHandler(fileLoggingFileName, true);
                fileHandler.setFormatter(new LoggingFormatter(!fileRotate));
                getRootLogger().addHandler(fileHandler);
                fileHandler.setFilter(DEFAULT_FILTER);
            } catch (IOException e) {
                // Duh. No file logger.
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the document handler document for display in the debug panel.
     */
    public static StyledDocument getLogBuffer() {
        Reject
            .ifNull(documentHandler.getLogBuffer(), "DocumentHandler not set");
        return documentHandler.getLogBuffer();
    }

    /**
     * @return the buffered handler.
     */
    public static BufferedHandler getBufferedHandler() {
        return bufferedHandler;
    }

    /**
     * @return if file logging is enabled.
     */
    public static boolean isLogToFile() {
        return fileLoggingLevel != null;
    }

    /**
     * @return the document logging level.
     */
    public static Level getDocumentLoggingLevel() {
        if (documentLoggingLevel == null) {
            return Level.OFF;
        } else {
            return documentLoggingLevel;
        }
    }

    public static Level getMinimumLoggingLevel() {
        Level min = Level.OFF;
        if (documentLoggingLevel != null
            && documentLoggingLevel.intValue() < min.intValue())
        {
            min = documentLoggingLevel;
        }
        if (consoleLoggingLevel != null
            && consoleLoggingLevel.intValue() < min.intValue())
        {
            min = consoleLoggingLevel;
        }
        if (fileLoggingLevel != null
            && fileLoggingLevel.intValue() < min.intValue())
        {
            min = fileLoggingLevel;
        }
        if (bufferedLoggingLevel != null
            && bufferedLoggingLevel.intValue() < min.intValue())
        {
            min = bufferedLoggingLevel;
        }
        return min;
    }

    /**
     * Convenience method for getting the root logger.
     * 
     * @return
     */
    private static Logger getRootLogger() {
        return Logger.getLogger("");
    }

    /**
     * @return the directory that the file logging is written to.
     */
    public static File getDebugDir() {
        File canidate;
        if (!OSUtil.isWebStart() && OSUtil.isLinux()) {
            // Never for webstart or Windows systems
            canidate = new File(DEBUG_DIR);
            if (canidate.exists() && canidate.isDirectory()) {
                return canidate;
            }

            canidate.mkdirs();
            if (canidate.exists() && canidate.isDirectory()) {
                return canidate;
            }
        }

        // Fallback! TRAC #1087
        canidate = new File(Controller.getMiscFilesLocation(), DEBUG_DIR);
        if (canidate.exists() && canidate.isDirectory()) {
            return canidate;
        }

        canidate.mkdirs();
        if (canidate.exists() && canidate.isDirectory()) {
            return canidate;
        }

        return null;
    }

    /**
     * Sets the file logging file name prefix. Should be the config name.
     * 
     * @param prefix
     */
    public static void setPrefix(String prefix) {
        assert prefix != null;
        LoggingManager.LOGFILE_PREFIX = prefix;
    }

    /**
     * @return the file logging file name.
     */
    public static String getLoggingFileName() {
        synchronized (fileHandlerLock) {
            return fileLoggingFileName;
        }
    }

    /**
     * Set the root logging level to the highest possible, so that
     * Logger.isLoggable() has the desired effect in the code.
     */
    private static void setMinimumBaseLoggingLevel() {
        Level level = Level.SEVERE;
        if (documentLoggingLevel != null
            && documentLoggingLevel.intValue() < level.intValue())
        {
            level = documentLoggingLevel;
        }
        if (consoleLoggingLevel != null
            && consoleLoggingLevel.intValue() < level.intValue())
        {
            level = consoleLoggingLevel;
        }
        synchronized (fileHandlerLock) {
            if (fileLoggingLevel != null
                && fileLoggingLevel.intValue() < level.intValue())
            {
                level = fileLoggingLevel;
            }
        }
        if (bufferedLoggingLevel != null
            && bufferedLoggingLevel.intValue() < level.intValue())
        {
            level = bufferedLoggingLevel;
        }
        getRootLogger().setLevel(level);
    }

    public static Level levelForName(String levelName) {
        if (levelName == null) {
            return null;
        }
        if (levelName.equalsIgnoreCase(Level.ALL.getName())) {
            return Level.ALL;
        } else if (levelName.equalsIgnoreCase(Level.CONFIG.getName())) {
            return Level.CONFIG;
        } else if (levelName.equalsIgnoreCase(Level.FINE.getName())) {
            return Level.FINE;
        } else if (levelName.equalsIgnoreCase(Level.FINER.getName())) {
            return Level.FINER;
        } else if (levelName.equalsIgnoreCase(Level.FINEST.getName())) {
            return Level.FINEST;
        } else if (levelName.equalsIgnoreCase(Level.INFO.getName())) {
            return Level.INFO;
        } else if (levelName.equalsIgnoreCase(Level.OFF.getName())) {
            return Level.OFF;
        } else if (levelName.equalsIgnoreCase(Level.SEVERE.getName())) {
            return Level.SEVERE;
        } else if (levelName.equalsIgnoreCase(Level.WARNING.getName())
            || levelName.equalsIgnoreCase("WARN"))
        {
            return Level.WARNING;
        }
        return null;
    }

    /**
     * Re-set the file logging, to change the log file to a new date.
     */
    public static void resetFileLogging() {
        if (fileLoggingLevel != null && fileHandler != null) {

            // Close off the old one first.
            fileHandler.flush();
            fileHandler.close();

            createFileHandler();
        }
    }

    public static void closeFileLogging() {
        if (fileLoggingLevel != null && fileHandler != null) {

            // Close off the old one first.
            fileHandler.flush();
            fileHandler.close();
        }
    }

    /**
     * PFS-475
     * 
     * @param maxAgeDays
     */
    public static void removeOldLogs(final int maxAgeDays) {
        File[] oldFiles = getDebugDir().listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (!file.isFile()) {
                    return false;
                }
                if (!file.getName().startsWith(LOGFILE_PREFIX)) {
                    return false;
                }
                if (!file.getName().contains(LOGFILE_SUFFIX)) {
                    return false;
                }
                long msOld = System.currentTimeMillis() - file.lastModified();
                int daysOld = (int) (msOld / 1000 / 60 / 60 / 24);
                return daysOld >= maxAgeDays;
            }
        });
        for (File logFile : oldFiles) {
            logFile.delete();
        }
    }
}
