package de.dal33t.powerfolder.security;

import java.util.Date;

import de.dal33t.powerfolder.util.test.TestHelper;
import junit.framework.TestCase;

public class TokenTest extends TestCase {

    public void testGenerateToken() {
        Date validTo = new Date(System.currentTimeMillis() + 150000L);
        Token token = new Token(validTo, null, null);
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
        Date validTo = new Date(System.currentTimeMillis() + 500L);
        Token token = new Token(validTo, null, null);
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
        Date validTo = new Date(System.currentTimeMillis() + 150000L);
        Token token = new Token(validTo, null, null);
        String secret = token.generateSecret();

        assertTrue(token.isValid());
        assertFalse(token.isRevoked());

        token.revoke();

        assertFalse(token.isValid());
        assertTrue(token.isRevoked());
        assertFalse(token.validate(secret));
    }

    public void testInvalidTokens() {
        Date validTo = new Date(System.currentTimeMillis() + 150000L);
        Token token = new Token(validTo, null, null);
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

        Token anotherToken = new Token(validTo, null, null);
        String anotherSecret = anotherToken.generateSecret();
        assertFalse(token.validate(anotherSecret));

        try {
            assertNull(token.generateSecret());
            fail("Token must not generated another secret");
        } catch (Exception e) {
        }

        validTo = new Date(System.currentTimeMillis() - 150000L);
        try {
            new Token(validTo, null, null);
            fail("Expired token must not be generated");
        } catch (Exception e) {
        }
    }
}
