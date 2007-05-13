package de.dal33t.powerfolder.webservice;

import java.io.Serializable;

/**
 * A general response object of the webservice.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceResponse implements Serializable {
    private static final long serialVersionUID = 100L;
    public static final String CONTENT_TYPE = "application/octet-stream";

    private Serializable value;
    private boolean failure;

    public WebServiceResponse(boolean failure, Serializable value) {
        this.value = value;
        this.failure = failure;
    }

    public boolean isFailure() {
        return failure;
    }

    public Serializable getValue() {
        return value;
    }
}
