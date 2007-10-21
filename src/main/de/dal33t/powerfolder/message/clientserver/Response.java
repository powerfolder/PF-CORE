package de.dal33t.powerfolder.message.clientserver;

import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.Reject;

/**
 * Represents the reponse for the request - response logic.
 * <p>
 * Subclass this for concrete responses.
 * 
 * @see Request
 * @see de.dal33t.powerfolder.clientserver.RequestExecutor
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Response extends Message {
    private static final long serialVersionUID = 100L;

    /**
     * The corresponding id of the request.
     */
    public String requestId;

    /**
     * Associate the response with the given request.
     * 
     * @param request
     */
    public void associate(Request request) {
        Reject.ifNull(request, "Request is null");
        Reject.ifBlank(request.requestId, "Request id is blank");
        requestId = request.requestId;
    }
}
