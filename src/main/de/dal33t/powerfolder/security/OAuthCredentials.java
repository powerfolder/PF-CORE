package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * PFS-1645: Pojo that holds a OAuth2 client-ID and secret.
 */

@Entity(name = "OAuth")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OAuthCredentials {

    public static final String PROPERTYNAME_ID = "id";

    public static final String PROPERTYNAME_CLIENT_ID = "client_id";
    public static final String PROPERTYNAME_CLIENT_SECRET = "client_secrect";

    @Id
    private String id;

    private String clientID;
    private String clientSecret;

    private OAuthCredentials() {
        // For hibernate
    }

    public static OAuthCredentials newCredentials(String clientID, String clientSecret) {
        return new OAuthCredentials(clientID, clientSecret);
    }

    public String getId() {
        return id;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientID() {
        return clientID;
    }

    private OAuthCredentials(String clientID, String clientSecret) {

        Reject.ifNull(clientID, "clientID");
        Reject.ifNull(clientSecret, "clientSecret");

        this.id = IdGenerator.makeId();
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }
}