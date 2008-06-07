package de.dal33t.powerfolder.security;

/**
 * Exception thrown when something when wrong with the security. e.g. the
 * current user has not sufficent permission to perfom a action.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SecurityException extends RuntimeException {

    public SecurityException() {
        super();
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }
}