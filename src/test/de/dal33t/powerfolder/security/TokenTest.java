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

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.util.test.TestHelper;
import junit.framework.TestCase;

public class TokenTest extends TestCase {

    public void testGenerateToken() {
        Token token = Token.newAccessToken(150000L, testAccount());
        assertFalse(token.hasSecret());
        assertFalse(token.isValid());
        assertFalse(token.isExpired());
        assertFalse(token.isRevoked());

        String secret = token.generateSecret();
        String id = Token.extractId(secret);

        assertNotNull(id);
        assertEquals(token.getId(), id);
        assertFalse(Token.isExpired(secret));
        assertNotNull(secret);
        assertTrue(token.hasSecret());
        assertTrue(token.isValid());
        assertFalse(token.isExpired());
        assertFalse(token.isRevoked());
        assertTrue(token.matches(secret));
        assertTrue(token.validate(secret));
    }

    public void testExpireToken() {
        Token token = Token.newAccessToken(500L, testAccount());
        String secret = token.generateSecret();

        assertTrue(token.isValid());
        assertFalse(token.isExpired());

        TestHelper.waitMilliSeconds(1000);

        assertFalse(token.isValid());
        assertTrue(token.isExpired());
        assertTrue(Token.isExpired(secret));
        assertFalse(token.validate(secret));
    }

    public void testRevokeToken() {
        Token token = Token.newAccessToken(150000L, testAccount());
        String secret = token.generateSecret();

        assertTrue(token.isValid());
        assertFalse(token.isRevoked());

        token.revoke();

        assertFalse(token.isValid());
        assertTrue(token.isRevoked());
        assertFalse(token.validate(secret));
    }

    public void testInvalidTokens() {
        Token token = Token.newAccessToken(150000L, testAccount());
        String secret = token.generateSecret();
        assertTrue(token.isValid());
        assertTrue(token.validate(secret));

        assertFalse(token.validate(null));
        assertFalse(token.validate(""));
        assertFalse(token.validate("  "));
        assertFalse(token.validate("dsflfl//754358xx.."));

        assertNull(Token.extractId(null));
        assertNull(Token.extractId(""));
        assertNull(Token.extractId("  "));
        assertNull(Token.extractId("xx..f.df.d45458d9fg8d89f789dsfdsju//"));

        Token anotherToken = Token.newAccessToken(150000L, testAccount());
        String anotherSecret = anotherToken.generateSecret();
        assertFalse(token.validate(anotherSecret));

        try {
            assertNull(token.generateSecret());
            fail("Token must not generated another secret");
        } catch (Exception e) {
        }

        try {
            Token.newAccessToken(-150000L, testAccount());
            fail("Expired token must not be generated");
        } catch (Exception e) {
        }
    }

    private static AccountInfo testAccount() {
        return new Account().createInfo();
    }
}
