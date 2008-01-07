package de.dal33t.powerfolder.clientserver;

/**
 * Exception that occoured while invoking the remove service.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RemoteCallException extends RuntimeException {

    public RemoteCallException() {
        super();
    }

    public RemoteCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteCallException(String message) {
        super(message);

    }

    public RemoteCallException(Throwable cause) {
        super(cause);
    }
}
