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
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "OAuth2AuthCode", indexes = {
    @Index(name = "IDX_OA2CODE_VALID_TO", columnList = OAuth2AuthCode.PROPERTYNAME_VALID_TO)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OAuth2AuthCode implements Serializable {

    private static final long serialVersionUID = 100L;

    public static final String CHALLENGE_METHOD_S256 = "S256";

    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_CODE_HASH = "codeHash";
    public static final String PROPERTYNAME_CLIENT_ID = "clientId";
    public static final String PROPERTYNAME_REDIRECT_URI = "redirectURI";
    public static final String PROPERTYNAME_CODE_CHALLENGE = "codeChallenge";
    public static final String PROPERTYNAME_RESOURCE = "resource";
    public static final String PROPERTYNAME_SCOPE = "scope";
    public static final String PROPERTYNAME_ACCOUNT_OID = "accountOID";
    public static final String PROPERTYNAME_VALID_TO = "validTo";

    @Id
    private String id;

    private String codeHash;

    private String clientId;

    @Column(length = 1024)
    private String redirectURI;

    @Column(length = 255)
    private String codeChallenge;

    @Column(length = 512)
    private String resource;

    private String scope;

    private String accountOID;

    private Date validTo;

    private OAuth2AuthCode() {
        // For hibernate
    }

    public static OAuth2AuthCode newAuthCode(long validMS, String clientId, String redirectURI,
        String codeChallenge, String resource, String scope, String accountOID) {
        return new OAuth2AuthCode(new Date(System.currentTimeMillis() + validMS), clientId, redirectURI,
            codeChallenge, resource, scope, accountOID);
    }

    private OAuth2AuthCode(Date validTo, String clientId, String redirectURI, String codeChallenge,
        String resource, String scope, String accountOID) {
        Reject.ifNull(validTo, "ValidTo is null");
        Reject.ifBlank(clientId, "Client ID");
        Reject.ifBlank(redirectURI, "Redirect URI");
        Reject.ifBlank(codeChallenge, "Code challenge");
        Reject.ifBlank(accountOID, "Account OID");
        this.id = IdGenerator.makeId();
        this.validTo = validTo;
        this.clientId = clientId;
        this.redirectURI = redirectURI;
        this.codeChallenge = codeChallenge;
        this.resource = resource;
        this.scope = scope;
        this.accountOID = accountOID;
        Reject.ifTrue(isExpired(), "Unable to create expired auth code");
    }

    public String generateCode() {
        Reject.ifTrue(hasCode(), "Auth code already generated");
        String code = id + '-' + Token.generate(validTo);
        this.codeHash = LoginUtil.hashAndSalt(code);
        return code;
    }

    public boolean matches(String codeCandidate) {
        if (StringUtils.isBlank(codeCandidate)) {
            return false;
        }
        if (!hasCode()) {
            return false;
        }
        if (!id.equals(extractId(codeCandidate))) {
            return false;
        }
        return LoginUtil.matches(codeCandidate.toCharArray(), codeHash);
    }

    public boolean validate(String codeCandidate) {
        return !isExpired() && matches(codeCandidate);
    }

    public boolean hasCode() {
        return StringUtils.isNotBlank(codeHash);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > validTo.getTime();
    }

    public static String extractId(String code) {
        return Token.extractId(code);
    }

    public String getId() {
        return id;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getResource() {
        return resource;
    }

    public String getScope() {
        return scope;
    }

    public String getAccountOID() {
        return accountOID;
    }

    public Date getValidTo() {
        return validTo;
    }

    @Override
    public String toString() {
        return "OAuth2AuthCode[" + id + ", client " + clientId + "]";
    }
}
