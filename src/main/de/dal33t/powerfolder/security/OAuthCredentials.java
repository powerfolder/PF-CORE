/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
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

    public static final String PROPERTYNAME_CLIENT_ID = "clientId";
    public static final String PROPERTYNAME_CLIENT_SECRET = "clientSecret";

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