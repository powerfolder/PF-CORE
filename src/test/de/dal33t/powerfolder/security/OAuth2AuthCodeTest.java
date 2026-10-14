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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OAuth2AuthCodeTest {

    private static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String REDIRECT = "https://claude.ai/api/mcp/auth_callback";

    @Test
    public void testGenerateCode() {
        OAuth2AuthCode authCode = newAuthCode(300000L);

        assertFalse(authCode.hasCode());
        assertFalse(authCode.isExpired());

        String code = authCode.generateCode();

        assertNotNull(code);
        assertTrue(authCode.hasCode());
        assertEquals(authCode.getId(), OAuth2AuthCode.extractId(code));
        assertTrue(authCode.matches(code));
        assertTrue(authCode.validate(code));
        assertNotEquals(code, authCode.getCodeHash());
    }

    @Test
    public void testBoundParameters() {
        OAuth2AuthCode authCode = newAuthCode(300000L);

        assertEquals("client-abc", authCode.getClientId());
        assertEquals(REDIRECT, authCode.getRedirectURI());
        assertEquals(CHALLENGE, authCode.getCodeChallenge());
        assertEquals("https://pf.example.com/mcp", authCode.getResource());
        assertEquals(OAuth2Client.SCOPE_MCP, authCode.getScope());
        assertEquals("account-oid-1", authCode.getAccountOID());
        assertNotNull(authCode.getValidTo());
    }

    @Test
    public void testInvalidCodes() {
        OAuth2AuthCode authCode = newAuthCode(300000L);
        String code = authCode.generateCode();

        assertTrue(authCode.validate(code));

        assertFalse(authCode.validate(null));
        assertFalse(authCode.validate(""));
        assertFalse(authCode.validate("   "));
        assertFalse(authCode.validate("not-a-code"));

        OAuth2AuthCode other = newAuthCode(300000L);
        String otherCode = other.generateCode();
        assertFalse(authCode.validate(otherCode));
        assertFalse(other.validate(code));

        try {
            authCode.generateCode();
            fail("Auth code must not generate another code");
        } catch (Exception e) {
        }
    }

    @Test
    public void testNoCodeGeneratedDoesNotMatch() {
        OAuth2AuthCode authCode = newAuthCode(300000L);

        assertFalse(authCode.hasCode());
        assertFalse(authCode.matches("anything"));
        assertFalse(authCode.validate("anything"));
    }

    @Test
    public void testMandatoryParameters() {
        try {
            OAuth2AuthCode.newAuthCode(300000L, null, REDIRECT, CHALLENGE, null, OAuth2Client.SCOPE_MCP, "oid");
            fail("Auth code without client id must not be created");
        } catch (Exception e) {
        }

        try {
            OAuth2AuthCode.newAuthCode(300000L, "client-abc", null, CHALLENGE, null, OAuth2Client.SCOPE_MCP, "oid");
            fail("Auth code without redirect URI must not be created");
        } catch (Exception e) {
        }

        try {
            OAuth2AuthCode.newAuthCode(300000L, "client-abc", REDIRECT, null, null, OAuth2Client.SCOPE_MCP, "oid");
            fail("Auth code without code challenge must not be created");
        } catch (Exception e) {
        }

        try {
            OAuth2AuthCode.newAuthCode(300000L, "client-abc", REDIRECT, CHALLENGE, null, OAuth2Client.SCOPE_MCP, null);
            fail("Auth code without account OID must not be created");
        } catch (Exception e) {
        }

        try {
            OAuth2AuthCode.newAuthCode(-300000L, "client-abc", REDIRECT, CHALLENGE, null, OAuth2Client.SCOPE_MCP,
                "oid");
            fail("Expired auth code must not be created");
        } catch (Exception e) {
        }
    }

    private static OAuth2AuthCode newAuthCode(long validMS) {
        return OAuth2AuthCode.newAuthCode(validMS, "client-abc", REDIRECT, CHALLENGE,
            "https://pf.example.com/mcp", OAuth2Client.SCOPE_MCP, "account-oid-1");
    }
}
