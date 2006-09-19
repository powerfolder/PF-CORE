/* $Id: Loggable.java,v 1.11 2006/02/23 10:24:58 schaatser Exp $
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

    /**
     * Plain initalizer
     */
    protected Loggable() {
        super();
        log = Logger.getLogger(this);
        logVerbose = Logger.isEnabled() && log.isVerbose();
        logEnabled = Logger.isEnabled() && !log.isExcluded();        
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