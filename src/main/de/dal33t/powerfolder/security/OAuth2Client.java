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
 *
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "OAuth2Client")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OAuth2Client implements Serializable {

    private static final long serialVersionUID = 100L;

    public static final String AUTH_METHOD_NONE = "none";
    public static final String SCOPE_MCP = "mcp";

    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_CLIENT_NAME = "clientName";
    public static final String PROPERTYNAME_REDIRECT_URIS = "redirectURIs";
    public static final String PROPERTYNAME_TOKEN_ENDPOINT_AUTH_METHOD = "tokenEndpointAuthMethod";
    public static final String PROPERTYNAME_SCOPE = "scope";
    public static final String PROPERTYNAME_CREATED = "created";
    public static final String PROPERTYNAME_LAST_USED = "lastUsed";
    public static final String PROPERTYNAME_NOTES = "notes";

    @Id
    private String id;

    @Column(length = 255)
    private String clientName;

    @Column(length = 2048)
    private String redirectURIs;

    private String tokenEndpointAuthMethod;

    private String scope;

    private Date created;

    private Date lastUsed;

    @Column(length = 512)
    private String notes;

    private OAuth2Client() {
        // For hibernate
    }

    public static OAuth2Client newClient(String clientName, String redirectURIs, String scope) {
        return new OAuth2Client(clientName, redirectURIs, scope);
    }

    private OAuth2Client(String clientName, String redirectURIs, String scope) {
        Reject.ifBlank(redirectURIs, "Redirect URIs");
        this.id = IdGenerator.makeId();
        this.clientName = clientName;
        this.redirectURIs = redirectURIs;
        this.tokenEndpointAuthMethod = AUTH_METHOD_NONE;
        this.scope = scope;
        this.created = new Date();
    }

    public String getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public String getRedirectURIs() {
        return redirectURIs;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public String getScope() {
        return scope;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void touch() {
        this.lastUsed = new Date();
    }

    public boolean isPublicClient() {
        return AUTH_METHOD_NONE.equals(tokenEndpointAuthMethod);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "OAuth2Client[" + id + ", " + clientName + "]";
    }
}
