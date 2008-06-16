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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;

/**
 * Abstract superclass which has logger included. Used to easily handle debug
 * output
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public abstract class Loggable {
    private transient Logger log;

    protected transient boolean logVerbose;
    protected transient boolean logEnabled;
    protected transient boolean logError;
    protected transient boolean logDebug;
    protected transient boolean logInfo;
    protected transient boolean logWarn;
    
    /**
     * Plain initalizer
     */
    protected Loggable() {
        super();
        log = Logger.getLogger(this);
        logEnabled = Logger.isEnabled() && !log.isExcluded();
        logVerbose = Logger.isVerboseLevelEnabled() && !log.isExcluded();        
        logError = Logger.isErrorLevelEnabled() && !log.isExcluded();
        logDebug = Logger.isDebugLevelEnabled() && !log.isExcluded();
        logInfo = Logger.isInfoLevelEnabled() && !log.isExcluded();
        logWarn = Logger.isWarnLevelEnabled() && !log.isExcluded();
    }

    /**
     * Initalizes with a given logger
     * 
     * @param log
     */
    protected Loggable(Logger log) {
        setLogger(log);
    }

    /**
     * Returns the logger for this class
     * 
     * @return
     */
    protected final Logger log() {
        if (log.prefix == null) {
            if (this instanceof PFComponent) {
                PFComponent pfComponent = (PFComponent) this;
                Controller controller = pfComponent.getController();
                if (controller != null) {
                    Member myself = controller.getMySelf();
                    if (myself != null) {
                        log.setPrefix(pfComponent.getController().getMySelf()
                            .getNick());
                    }
                }
            }
        }
        return log;
    }

    /**
     * Returns the logger. To access it from other classed. e.g. EventSupport
     * classes Only deligates to <code>log</code>
     * 
     * @see #log()
     * @return
     */
    public final Logger getLogger() {
        return log();
    }

    /**
     * Sets the logger manually
     * 
     * @param log
     */
    public final void setLogger(Logger log) {
        this.log = log;
    }

    /**
     * Answers the default log name for this element. overwrite if you want
     * other name than classname
     * 
     * @return
     */
    public String getLoggerName() {
        return null;
    }
}