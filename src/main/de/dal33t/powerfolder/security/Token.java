/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
 * $Id: Constants.java 11478 2010-02-01 15:25:42Z tot $
 */
package de.dal33t.powerfolder.security;

import java.nio.ByteBuffer;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.util.Base58;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * PFC-2548: Device token/keys for authentication.
 * 
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
@Entity
@org.hibernate.annotations.Table(appliesTo = "Token", indexes = {@Index(name = "IDX_TOKEN_AOID", columnNames = {AccountInfo.PROPERTYNAME_OID})})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Token {
    public static final int TOKEN_VERSION = 1;
    private static final char SEPARATOR = '-';

    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_SECRET = "secrect";

    public static final String PROPERTYNAME_REVOKED = "revoked";
    public static final String PROPERTYNAME_VALID_TO = "validTo";
    public static final String PROPERTYNAME_NODE_INFO = "nodeInfo";
    public static final String PROPERTYNAME_ACCOUNT_INFO = "accountInfo";
    public static final String PROPERTYNAME_SERVICE_INFO = "serviceInfo";
    
    // PFC-2455:
    private static final long REQUEST_TOKEN_TIMEOUT = 60 * 1000L;
    // 1337 Years valid if not removed/revoked
    private static final long SERVICE_TOKEN_TIMEOUT = 1000L * 60 * 60 * 24 * 365 * 1337;
    // PFS-2008:
    private static final long MERGE_TOKEN_TIMEOUT = 10 * 60 * 1000L;
    private static final long ADD_EMAIL_TOKEN_TIMEOUT = 10 * 60 * 1000L;

    @Id
    private String id;
    private String secrect;

    private boolean revoked;
    @Index(name="IDX_TOKEN_VALID_TO")
    private Date validTo;

    @ManyToOne
    @JoinColumn(name = "nodeInfo_id")
    private MemberInfo nodeInfo;

    @Embedded
    @Fetch(FetchMode.JOIN)
    private AccountInfo accountInfo;
    
    @ManyToOne
    @JoinColumn(name = "serviceInfo_id")
    private ServerInfo serviceInfo;

    @Column(length = 1024)
    private String notes;

    private Token() {
        // For hibernate
    }

    public static Token newRequestToken(ServerInfo fedService) {
        Reject.ifNull(fedService, "Service null");
        Reject.ifFalse(fedService.isFederatedService(),
            "Not a federated service");
        Date validTo = new Date(System.currentTimeMillis() + REQUEST_TOKEN_TIMEOUT);
        return new Token(validTo, fedService, null, null);
    }
    
    public static Token newAccessToken(long validMS, AccountInfo aInfo)
    {
        return newAccessToken(validMS, aInfo, (MemberInfo) null);
    }

    public static Token newAccessToken(long validMS, AccountInfo aInfo,
        MemberInfo mInfo)
    {
        Reject.ifFalse(validMS > 0, "Invalid time");
        Reject.ifNull(aInfo, "Account info null");
        Date validTo = new Date(System.currentTimeMillis() + validMS);
        return new Token(validTo, null, aInfo, mInfo);
    }

    public static Token newAccessToken(AccountInfo aInfo,
        ServerInfo fedService)
    {
        Reject.ifNull(aInfo, "Account info null");
        Reject.ifNull(fedService, "Service null");
        Reject.ifFalse(fedService.isFederatedService(),
            "Not a federated service");
        Date validTo = new Date(
            System.currentTimeMillis() + SERVICE_TOKEN_TIMEOUT);
        return new Token(validTo, fedService, aInfo, null);
    }

    public static Token newMergeToken(AccountInfo aInfo,
        String usernameToMerge)
    {
        Token token = newAccessToken(MERGE_TOKEN_TIMEOUT, aInfo);
        token.addNotesWithDate(
            aInfo.getUsername() + " merging with " + usernameToMerge);
        return token;
    }

    public static Token newAddEmailToken(AccountInfo aInfo,
        String eMailToAdd)
    {
        Token token = newAccessToken(ADD_EMAIL_TOKEN_TIMEOUT, aInfo);
        token.addNotesWithDate(
            aInfo.getUsername() + " adding email " + eMailToAdd);
        return token;
    }

    /**
     * Constructs and prepares a new token.
     * 
     * @param validTo
     *            mandatory expiration date.
     * @param sInfo
     *            optional federated service information
     * @param aInfo
     *            optional account information
     * @param mInfo
     *            optional device information
     */
    private Token(Date validTo, ServerInfo sInfo, AccountInfo aInfo, MemberInfo mInfo) {
        Reject.ifNull(validTo, "Validto is null");
        this.serviceInfo = sInfo;
        this.accountInfo = aInfo;
        this.nodeInfo = mInfo;
        this.id = TOKEN_VERSION + IdGenerator.makeId();
        this.validTo = validTo;
        Reject.ifTrue(isExpired(), "Unable to create expired token");
    }

    /**
     * @return the client secret (token), which can be used for authentication.
     */
    public String generateSecret() {
        Reject.ifTrue(hasSecret(), "Token secret already generated");
        Reject.ifTrue(isRevoked(), "Token already revoked");
        String token = generate(validTo);
        Reject.ifTrue(isExpired(token), "Token already expired");
        String clientSecret = id + SEPARATOR + token;
        String hasedSalted = LoginUtil.hashAndSalt(clientSecret);
        // Permanently known to the server:
        secrect = hasedSalted;
        return clientSecret;
    }

    /**
     * Tests if the secret token candidate matches this token. Does NOT check
     * validity or revoked status.
     * 
     * @param secretCandiate
     * @return true if the given candidate secret matches this token.
     */
    public boolean matches(String secretCandiate) {
        if (StringUtils.isBlank(secretCandiate)) {
            return false;
        }
        if (!hasSecret()) {
            return false;
        }
        String id = extractId(secretCandiate);
        if (!getId().equals(id)) {
            return false;
        }
        return LoginUtil.matches(secretCandiate.toCharArray(), secrect);
    }

    /**
     * The final and full check if the secret can be used for authentication.
     * Checks: expiration date, revoke state, matching with db secret.
     * 
     * @param secretCandiate
     * @return true if
     */
    public boolean validate(String secretCandiate) {
        return !isExpired(secretCandiate) && isValid()
            && matches(secretCandiate);
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(secrect) && !revoked && !isExpired();
    }

    public boolean hasSecret() {
        return StringUtils.isNotBlank(secrect);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > validTo.getTime();
    }

    public String getId() {
        return id;
    }

    public String getSecrect() {
        return secrect;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke() {
        this.revoked = true;
    }

    public Date getValidTo() {
        return validTo;
    }

    public MemberInfo getNodeInfo() {
        return nodeInfo;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }
    
    public ServerInfo getServiceInfo() {
        return serviceInfo;
    }
    
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Adds a line of info with the current date to the notes.
     *
     * @param infoText
     */
    public void addNotesWithDate(String infoText) {
        if (StringUtils.isBlank(infoText)) {
            return;
        }
        String infoLine = Format.formatDateCanonical(new Date());
        infoLine += ": ";
        infoLine += infoText;
        if (StringUtils.isBlank(notes)) {
            setNotes(infoLine);
        } else {
            setNotes(notes + "\n" + infoLine);
        }
    }

    // Static helper **********************************************************



    public static String extractId(String secret) {
        if (StringUtils.isBlank(secret)) {
            return null;
        }
        int i = secret.lastIndexOf(SEPARATOR);
        if (i >= 0) {
            return secret.substring(0, i);
        }
        return null;
    }

    /**
     * @param validTo
     * @return an unique token string with encoded expiration time.
     */
    public static String generate(Date validTo) {
        Reject.ifNull(validTo, "ValidTo");
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(validTo.getTime());
        byte[] vArr = buffer.array();

        byte[] b1 = IdGenerator.makeIdBytes();
        byte[] b2 = IdGenerator.makeIdBytes();

        byte[] otpArr = new byte[vArr.length + b1.length + b2.length];

        System.arraycopy(vArr, 0, otpArr, 0, vArr.length);
        System.arraycopy(b1, 0, otpArr, vArr.length, b1.length);
        System.arraycopy(b2, 0, otpArr, vArr.length + b1.length, b2.length);

        String otp = Base58.encode(otpArr);
        return otp;
    }

    /**
     * Validates a token string against expiration time. Note: Does NOT validate
     * a token secret / full token!
     * 
     * @param tokenString
     * @return if the token string is (still) valid
     */
    public static boolean isExpired(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            return true;
        }
        int i = tokenString.lastIndexOf(SEPARATOR);
        if (i >= 0) {
            tokenString = tokenString.substring(i + 1);
        }
        byte[] otpArr;
        try {
            otpArr = Base58.decode(tokenString);
        } catch (Exception e) {
            // Illegal OTP
            return true;
        }
        if (otpArr.length < 8) {
            return true;
        }
        long validTo;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.put(otpArr, 0, 8);
            buffer.flip();// need flip
            validTo = buffer.getLong();
        } catch (Exception e) {
            return true;
        }
        return System.currentTimeMillis() > validTo;
    }

    @Override
    public String toString() {
        return "Token [id=" + id + ", secrect=" + secrect + ", revoked="
            + revoked + ", validTo=" + validTo + ", nodeInfo=" + nodeInfo
            + ", accountInfo=" + accountInfo + "]";
    }
}
