package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;

/**
 * Request to login to a server.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class LoginRequest extends Request {
    private static final long serialVersionUID = 100L;

    private String username;
    private Serializable credentials;

    public LoginRequest(String username, Serializable credentials) {
        super();
        this.username = username;
        this.credentials = credentials;
    }

    public String getUsername() {
        return username;
    }

    public Serializable getCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return "LoginRequest (username=" + username + ")";
    }
}
