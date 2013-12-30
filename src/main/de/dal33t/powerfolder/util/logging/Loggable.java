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
 * $Id: Loggable.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.util.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/**
 * This class provides generic logging functionality. Extend from this class if
 * possible, for simple logging methods. If you cannot extend from this, add a
 * static final Logger to your class.
 */
public abstract class Loggable {

    private static boolean logNickPrefix;

    /** The class logger. */
    private transient Logger log;

    /**
     * @return true if the nick gets added as prefix to the log message if a
     *         {@link Controller} could be retrieved from this object.
     */
    public static boolean isLogNickPrefix() {
        return logNickPrefix;
    }

    /**
     * @param logPrefix
     *            if the nick should be added as prefix to the log message if a
     *            {@link Controller} could be retrieved from this object.
     */
    public static void setLogNickPrefix(boolean logPrefix) {
        Loggable.logNickPrefix = logPrefix;
    }

    /**
     * @return the name of the logger. by default uses the classname
     */
    protected final String getLoggerName() {
        return getClass().getName();
    }

    /**
     * Answers whether the logger level for this class is severe. Use to check
     * whether it is worth assebling a message to log.
     * 
     * @return
     */
    protected boolean isSevere() {
        return isLog(Level.SEVERE);
    }

    /**
     * Answers whether the logger level for this class is warning. Use to check
     * whether it is worth assebling a message to log.
     * 
     * @return
     */
    protected boolean isWarning() {
        return isLog(Level.WARNING);
    }

    /**
     * Answers whether the logger level for this class is info. Use to check
     * whether it is worth assebling a message to log.
     * 
     * @return
     */
    protected boolean isInfo() {
        return isLog(Level.INFO);
    }

    /**
     * Answers whether the logger level for this class is fine. Use to check
     * whether it is worth assebling a message to log.
     * 
     * @return
     */
    protected boolean isFine() {
        return isLog(Level.FINE);
    }

    /**
     * Answers whether the logger level for this class is finer. Use to check
     * whether it is worth assebling a message to log.
     * 
     * @return
     */
    protected boolean isFiner() {
        return isLog(Level.FINER);
    }

    /**
     * Log a message if logging level is severe.
     * 
     * @param message
     */
    protected void logSevere(String message) {
        logIt(Level.SEVERE, message, null);
    }

    /**
     * Log a message if logging level is warning.
     * 
     * @param message
     */
    protected void logWarning(String message) {
        logIt(Level.WARNING, message, null);
    }

    /**
     * Log a message if logging level is info.
     * 
     * @param message
     */
    protected void logInfo(String message) {
        logIt(Level.INFO, message, null);
    }

    /**
     * Log a message if logging level is fine.
     * 
     * @param message
     */
    protected void logFine(String message) {
        logIt(Level.FINE, message, null);
    }

    /**
     * Log a message if logging level is finer.
     * 
     * @param message
     */
    protected void logFiner(String message) {
        logIt(Level.FINER, message, null);
    }

    /**
     * Log a message and throwable if logging level is severe.
     * 
     * @param message
     * @param t
     */
    protected void logSevere(String message, Throwable t) {
        logIt(Level.SEVERE, message, t);
    }

    /**
     * Log a message and throwable if logging level is warning.
     * 
     * @param message
     * @param t
     */
    protected void logWarning(String message, Throwable t) {
        logIt(Level.WARNING, message, t);
    }

    /**
     * Log a message and throwable if logging level is info.
     * 
     * @param message
     * @param t
     */
    protected void logInfo(String message, Throwable t) {
        logIt(Level.INFO, message, t);
    }

    /**
     * Log a message and throwable if logging level is fine.
     * 
     * @param message
     * @param t
     */
    protected void logFine(String message, Throwable t) {
        logIt(Level.FINE, message, t);
    }

    /**
     * Log a message and throwable if logging level is finer.
     * 
     * @param message
     * @param t
     */
    protected void logFiner(String message, Throwable t) {
        logIt(Level.FINER, message, t);
    }

    /**
     * Log a throwable if logging level is severe.
     * 
     * @param t
     */
    protected void logSevere(Throwable t) {
        logIt(Level.SEVERE, t.getMessage(), t);
    }

    /**
     * Log a throwable if logging level is warning.
     * 
     * @param t
     */
    protected void logWarning(Throwable t) {
        logIt(Level.WARNING, t.getMessage(), t);
    }

    /**
     * Log a throwable if logging level is info.
     * 
     * @param t
     */
    protected void logInfo(Throwable t) {
        logIt(Level.INFO, t.getMessage(), t);
    }

    /**
     * Log a throwable if logging level is fine.
     * 
     * @param t
     */
    protected void logFine(Throwable t) {
        logIt(Level.FINE, t.getMessage(), t);
    }

    /**
     * Log a throwable if logging level is finer.
     * 
     * @param t
     */
    protected void logFiner(Throwable t) {
        logIt(Level.FINER, t.getMessage(), t);
    }

    /**
     * Actually check if a logger is at or above a specific logging level.
     * 
     * @param level
     * @return
     */
    protected boolean isLog(Level level) {
        // Performance: Don't create a Logger if not necessary
        if (level.intValue() < LoggingManager.getMinimumLoggingLevel()
            .intValue())
        {
            return false;
        }
        if (log == null) {
            log = Logger.getLogger(getLoggerName());
        }
        return log.isLoggable(level);
    }

    /**
     * Actually log a message and optionally a Throwable.
     * 
     * @param level
     * @param message
     * @param t
     */
    protected void logIt(Level level, String message, Throwable t) {
        if (log == null) {
            log = Logger.getLogger(getLoggerName());
        }
        String prefix = null;

        if (logNickPrefix && this instanceof PFComponent) {
            Controller controller = ((PFComponent) this).getController();
            if (controller != null && controller.getMySelf() != null) {
                prefix = controller.getMySelf().getNick();
            }
        }

        if (prefix != null) {
            LogRecord lr = new LogRecord(level, message);
            lr.setThrown(t);
            lr.setParameters(new Object[]{prefix});
            lr.setLoggerName(log.getName());
            log.log(lr);
        } else {
            log.log(level, message, t);
        }
    }
}
