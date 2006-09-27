/* $Id: TransferException.java,v 1.2 2004/09/24 03:37:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.transfer;

/**
 * General exception while handling uploads / downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class TransferException extends RuntimeException {

    /**
     * 
     */
    public TransferException() {
        super();
    }

    /**
     * @param message
     */
    public TransferException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public TransferException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public TransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
