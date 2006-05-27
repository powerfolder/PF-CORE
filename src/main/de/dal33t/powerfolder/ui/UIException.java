/* $Id: UIException.java,v 1.2 2004/09/24 03:37:45 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui;

/**
 * General exception for UI problems
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class UIException extends Exception {

    /**
     * 
     */
    public UIException() {
        super();
    }

    /**
     * @param message
     */
    public UIException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public UIException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public UIException(String message, Throwable cause) {
        super(message, cause);
    }

}
