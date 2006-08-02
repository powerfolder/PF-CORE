package de.dal33t.powerfolder.web;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

import de.dal33t.powerfolder.PFComponent;

/** Maps velocity logging to our own logging system */
public class VelocityLogger extends PFComponent implements LogSystem {

    public VelocityLogger() {
    }

    /**
     * This init() will be invoked once by the LogManager to give you current
     * RuntimeServices intance
     */
    public void init(RuntimeServices rsvc) {
        // do nothing
    }

    /**
     * This is the method that you implement for Velocity to call with log
     * messages.
     */
    public void logVelocityMessage(int level, String message) {
        switch (level) {
            case DEBUG_ID : {
                log().debug(message);
                break;
            }

            case INFO_ID : { //info is the spam level in Velocity ;-)                 
                log().verbose(message);
                break;
            }

            case WARN_ID : {
                log().warn(message);
                break;
            }

            case ERROR_ID : {
                log().error(message);
                break;
            }
            default : {
                log().error("invalid log level");
            }
        }
    }
}