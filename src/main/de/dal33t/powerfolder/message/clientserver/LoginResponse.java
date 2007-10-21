package de.dal33t.powerfolder.message.clientserver;

import de.dal33t.powerfolder.security.Identity;
import de.dal33t.powerfolder.util.Reject;

/**
 * The response to login to a server.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class LoginResponse extends Response {
    private static final long serialVersionUID = 100L;

    private Result result;
    private Identity identity;

    public LoginResponse(Result result, LoginRequest req) {
        super(req);
        Reject.ifNull(result, "Result is null");
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public enum Result {
        LOGIN_OK, LOGIN_FAILURE;
    }
}
