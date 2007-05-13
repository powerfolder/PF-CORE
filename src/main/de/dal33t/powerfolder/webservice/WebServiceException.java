package de.dal33t.powerfolder.webservice;

/**
 * A exception around the webservice.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceException extends Exception {

    public WebServiceException() {
        super();
    }

    public WebServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebServiceException(String message) {
        super(message);
    }

    public WebServiceException(Throwable cause) {
        super(cause);
    }
}
