package de.dal33t.powerfolder.message.clientserver;

import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Represents the request for the request - response logic.
 * <p>
 * Subclass this for concrete requests.
 * 
 * @see Response
 * @see de.dal33t.powerfolder.clientserver.RequestExecutor
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class Request extends Message {
    private static final long serialVersionUID = 100L;

    private String requestId;

    public Request() {
        requestId = IdGenerator.makeId();
    }
    
    public String getRequestId() {
        return requestId;
    }
}
