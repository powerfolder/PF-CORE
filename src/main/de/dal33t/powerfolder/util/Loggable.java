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
 * $Id$
 */
package de.dal33t.powerfolder.util;

import java.util.logging.Level;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/**
 * Abstract superclass which has logger included. Used to easily handle debug
 * output. NOTE: Do not extend Serializable classes from Loggable as it breaks
 * class hierarchy. Use logStatic instead.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public abstract class Loggable {

    /**
     * Logs a message at FINER level.
     * 
     * @param message
     */
    protected void logFiner(String message) {
        retrieveAndSetCurrentController();
        LogDispatch.logFiner(getLoggerName(), message, null);
    }

    /**
     * Logs a message at FINE level.
     * 
     * @param message
     */
    protected void logFine(String message) {
        retrieveAndSetCurrentController();
        LogDispatch.logFine(getLoggerName(), message, null);
    }

    /**
     * Logs a message at INFO level.
     * 
     * @param message
     */
    protected void logInfo(String message) {
        retrieveAndSetCurrentController();
        LogDispatch.logInfo(getLoggerName(), message, null);
    }

    /**
     * Logs a message at WARNING level.
     * 
     * @param message
     */
    protected void logWarning(String message) {
        retrieveAndSetCurrentController();
        LogDispatch.logWarning(getLoggerName(), message, null);
    }

    /**
     * Logs a message at SEVERE level.
     * 
     * @param message
     */
    protected void logSevere(String message) {
        retrieveAndSetCurrentController();
        LogDispatch.logSevere(getLoggerName(), message, null);
    }

    /**
     * Logs a message and Throwable at FINER level.
     * 
     * @param message
     * @param t
     */
    protected void logFiner(String message, Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logFiner(getLoggerName(), message, t);
    }

    /**
     * Logs a message and Throwable at FINE level.
     * 
     * @param message
     * @param t
     */
    protected void logFine(String message, Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logFine(getLoggerName(), message, t);
    }

    /**
     * Logs a message and Throwable at INFO level.
     * 
     * @param message
     * @param t
     */
    protected void logInfo(String message, Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logInfo(getLoggerName(), message, t);
    }

    /**
     * Logs a message and Throwable at WARNING level.
     * 
     * @param message
     * @param t
     */
    protected void logWarning(String message, Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logWarning(getLoggerName(), message, t);
    }

    /**
     * Logs a message and Throwable at SEVERE level.
     * 
     * @param message
     * @param t
     */
    protected void logSevere(String message, Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logSevere(getLoggerName(), message, t);
    }

    /**
     * Logs a Throwable at FINER level.
     * 
     * @param t
     */
    protected void logFiner(Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logFiner(getLoggerName(), null, t);
    }

    /**
     * Logs a Throwable at FINE level.
     * 
     * @param t
     */
    protected void logFine(Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logFine(getLoggerName(), null, t);
    }

    /**
     * Logs a Throwable at INFO level.
     * 
     * @param t
     */
    protected void logInfo(Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logInfo(getLoggerName(), null, t);
    }

    /**
     * Logs a Throwable at WARNING level.
     * 
     * @param t
     */
    protected void logWarning(Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logWarning(getLoggerName(), null, t);
    }

    /**
     * Logs a Throwable at SEVERE level.
     * 
     * @param t
     */
    protected void logSevere(Throwable t) {
        retrieveAndSetCurrentController();
        LogDispatch.logSevere(getLoggerName(), null, t);
    }

    /**
     * Logs a message at FINER level.
     * 
     * @param message
     */
    public static void logFinerStatic(Class clazz, String message) {
        LogDispatch.logFiner(clazz.getName(), message, null);
    }

    /**
     * Logs a message at FINE level.
     * 
     * @param message
     */
    public static void logFineStatic(Class clazz, String message) {
        LogDispatch.logFine(clazz.getName(), message, null);
    }

    /**
     * Logs a message at INFO level.
     * 
     * @param message
     */
    public static void logInfoStatic(Class clazz, String message) {
        LogDispatch.logInfo(clazz.getName(), message, null);
    }

    /**
     * Logs a message at WARNING level.
     * 
     * @param message
     */
    public static void logWarningStatic(Class clazz, String message) {
        LogDispatch.logWarning(clazz.getName(), message, null);
    }

    /**
     * Logs a message at SEVERE level.
     * 
     * @param message
     */
    public static void logSevereStatic(Class clazz, String message) {
        LogDispatch.logSevere(clazz.getName(), message, null);
    }

    /**
     * Logs a message and Throwable at FINER level.
     * 
     * @param message
     * @param t
     */
    public static void logFinerStatic(Class clazz, String message, Throwable t)
    {
        LogDispatch.logFiner(clazz.getName(), message, t);
    }

    /**
     * Logs a message and Throwable at FINE level.
     * 
     * @param message
     * @param t
     */
    public static void logFineStatic(Class clazz, String message, Throwable t) {
        LogDispatch.logFine(clazz.getName(), message, t);
    }

    /**
     * Logs a message and Throwable at INFO level.
     * 
     * @param message
     * @param t
     */
    public static void logInfoStatic(Class clazz, String message, Throwable t) {
        LogDispatch.logInfo(clazz.getName(), message, t);
    }

    /**
     * Logs a message and Throwable at WARNING level.
     * 
     * @param message
     * @param t
     */
    public static void logWarningStatic(Class clazz, String message, Throwable t)
    {
        LogDispatch.logWarning(clazz.getName(), message, t);
    }

    /**
     * Logs a message and Throwable at SEVERE level.
     * 
     * @param message
     * @param t
     */
    public static void logSevereStatic(Class clazz, String message, Throwable t)
    {
        LogDispatch.logSevere(clazz.getName(), message, t);
    }

    /**
     * Logs a Throwable at FINER level.
     * 
     * @param t
     */
    public static void logFinerStatic(Class clazz, Throwable t) {
        LogDispatch.logFiner(clazz.getName(), null, t);
    }

    /**
     * Logs a Throwable at FINE level.
     * 
     * @param t
     */
    public static void logFineStatic(Class clazz, Throwable t) {
        LogDispatch.logFine(clazz.getName(), null, t);
    }

    /**
     * Logs a Throwable at INFO level.
     * 
     * @param t
     */
    public static void logInfoStatic(Class clazz, Throwable t) {
        LogDispatch.logInfo(clazz.getName(), null, t);
    }

    /**
     * Logs a Throwable at WARNING level.
     * 
     * @param t
     */
    public static void logWarningStatic(Class clazz, Throwable t) {
        LogDispatch.logWarning(clazz.getName(), null, t);
    }

    /**
     * Logs a Throwable at SEVERE level.
     * 
     * @param t
     */
    public static void logSevereStatic(Class clazz, Throwable t) {
        LogDispatch.logSevere(clazz.getName(), null, t);
    }

    /**
     * Answers whether this class should log FINER messages.
     * 
     * @return
     */
    protected boolean isLogFiner() {
        return testLevel(Level.FINER);
    }

    /**
     * Answers whether this class should log FINE messages.
     * 
     * @return
     */
    protected boolean isLogFine() {
        return testLevel(Level.FINE);
    }

    /**
     * Answers whether this class should log INFO messages.
     * 
     * @return
     */
    protected boolean isLogInfo() {
        return testLevel(Level.INFO);
    }

    /**
     * Answers whether this class should log WARNING messages.
     * 
     * @return
     */
    protected boolean isLogWarning() {
        return testLevel(Level.WARNING);
    }

    /**
     * Answers whether this class should log SEVERE messages.
     * 
     * @return
     */
    protected boolean isLogSevere() {
        return testLevel(Level.SEVERE);
    }

    /**
     * Answers whether this class should log FINER messages.
     * 
     * @return
     */
    public static boolean isLogFinerStatic(Class clazz) {
        return testLevelStatic(clazz.getName(), Level.FINER);
    }

    /**
     * Answers whether this class should log FINE messages.
     * 
     * @return
     */
    public static boolean isLogFineStatic(Class clazz) {
        return testLevelStatic(clazz.getName(), Level.FINE);
    }

    /**
     * Answers whether this class should log INFO messages.
     * 
     * @return
     */
    public static boolean isLogInfoStatic(Class clazz) {
        return testLevelStatic(clazz.getName(), Level.INFO);
    }

    /**
     * Answers whether this class should log WARNING messages.
     * 
     * @return
     */
    public static boolean isLogWarningStatic(Class clazz) {
        return testLevelStatic(clazz.getName(), Level.WARNING);
    }

    /**
     * Answers whether this class should log SEVERE messages.
     * 
     * @return
     */
    public static boolean isLogSevereStatic(Class clazz) {
        return testLevelStatic(clazz.getName(), Level.SEVERE);
    }

    /**
     * Should the descendant bother creating log messages at this level?
     * 
     * @param level
     * @return
     */
    private boolean testLevel(Level level) {
        return testLevelStatic(getLoggerName(), level);
    }

    /**
     * Should the descendant bother creating log messages at this level?
     * 
     * @param level
     * @return
     */
    private static boolean testLevelStatic(String className, Level level) {
        if (!LogDispatch.isEnabled()) {
            // No log handlers ==> no logging.
            return false;
        }

        Level loggingLevel = LogDispatch.getLevel(className);
        if (loggingLevel == null) {
            loggingLevel = Level.OFF;
        }
        return loggingLevel.intValue() >= level.intValue();
    }
    
    private void retrieveAndSetCurrentController() {
        LogDispatch.setCurrentController(getTheController());
    }

    private Controller getTheController() {
        if (this instanceof PFComponent) {
            return ((PFComponent) this).getController();
        }
        return null;
    }

    /**
     * The name of the logger. Overrideable for special cases.
     * 
     * @return
     */
    public String getLoggerName() {
        return getClass().getName();
    }
}