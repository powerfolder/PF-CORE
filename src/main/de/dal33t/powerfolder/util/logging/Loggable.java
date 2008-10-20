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

import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class Loggable {

    private Logger log;

    protected void logSevere(String message) {
        logIt(Level.SEVERE, message, null);
    }

    protected void logWarning(String message) {
        logIt(Level.WARNING, message, null);
    }

    protected void logInfo(String message) {
        logIt(Level.INFO, message, null);
    }

    protected void logFine(String message) {
        logIt(Level.FINE, message, null);
    }

    protected void logFiner(String message) {
        logIt(Level.FINER, message, null);
    }

    protected void logSevere(String message, Throwable t) {
        logIt(Level.SEVERE, message, t);
    }

    protected void logWarning(String message, Throwable t) {
        logIt(Level.WARNING, message, t);
    }

    protected void logInfo(String message, Throwable t) {
        logIt(Level.INFO, message, t);
    }

    protected void logFine(String message, Throwable t) {
        logIt(Level.FINE, message, t);
    }

    protected void logFiner(String message, Throwable t) {
        logIt(Level.FINER, message, t);
    }

    protected void logSevere(Throwable t) {
        logIt(Level.SEVERE, t.getMessage(), t);
    }

    protected void logWarning(Throwable t) {
        logIt(Level.WARNING, t.getMessage(), t);
    }

    protected void logInfo(Throwable t) {
        logIt(Level.INFO, t.getMessage(), t);
    }

    protected void logFine(Throwable t) {
        logIt(Level.FINE, t.getMessage(), t);
    }

    protected void logFiner(Throwable t) {
        logIt(Level.FINER, t.getMessage(), t);
    }

    private void logIt(Level level, String message, Throwable t) {
        if (log == null) {
            log = Logger.getLogger(getClass().getName());
        }
        if (t == null) {
            log.log(level, message);
        } else {
            log.log(level, message, t);
        }
    }
}
