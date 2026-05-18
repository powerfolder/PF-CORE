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
package de.dal33t.powerfolder.util.test;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class LoginUtilTest extends TestCase {

    public void testDecorateUrlNullPass() {
        String url = "http://localhost:8080";
        String username = "TestUser";
        char[] password = null;

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + username, LoginUtil.decorateURL(url, username, password));

        String usernameWithSpecial = "Test%23User!";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.encodeForURL(usernameWithSpecial), LoginUtil.decorateURL(url, usernameWithSpecial, password));

    }

    public void testDecorateUrlNullUsername() {
        String url = "http://localhost:8080";
        String username = null;
        char[] password = null;

        assertEquals(url, LoginUtil.decorateURL(url, username, password));

        String usernameEmpty = "";
        assertEquals(url, LoginUtil.decorateURL(url, username, password));
    }


    public void testDecorateUrlOk() {
        String url = "http://localhost:8080";
        String username = "TestUser";
        char[] password = {'p','a','s','s','A','1','!','w', 'o', 'R', 'd'};

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + username + "&" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.encodeForURL(LoginUtil.obfuscate(password)),
                LoginUtil.decorateURL(url, username, password));

    }

    public void testDecorateUrlException() {
        String nullString = null;
        char[] nullChars = null;
        assertNull(LoginUtil.decorateURL(nullString, nullString, nullChars));
    }

    public void testDecorateUrlObfuscatedEmptyUser() {
        String url = "http://localhost:8080";
        String username = null;
        String password = "TestPassA1!";

        assertEquals(url + "?" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.encodeForURL(password), LoginUtil.decorateURL(url, username, password));

        String usernameEmpty = "";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.encodeForURL(password), LoginUtil.decorateURL(url, usernameEmpty, password));

        String usernameSpaces = "   ";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.encodeForURL(password), LoginUtil.decorateURL(url, usernameSpaces, password));

    }

    public void testDecorateUrlObfuscatedEmptyPassword() {
        String url = "http://localhost:8080";
        String username = "TestUsername";
        String password = null;

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.encodeForURL(username), LoginUtil.decorateURL(url, username, password));

        String passwordEmpty = "";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.encodeForURL(username), LoginUtil.decorateURL(url, username, passwordEmpty));

        String passwordSpaces = "   ";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.encodeForURL(username), LoginUtil.decorateURL(url, username, passwordSpaces));
    }

    public void testDecorateUrlObfuscatedOk() {
        String url = "http://localhost:8080";
        String username = "TestUsername";
        String password = "PassA1!Word$%2";

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.encodeForURL(username) + "&" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.encodeForURL(password)
                , LoginUtil.decorateURL(url, username, password));

        String usernameSpecialChars = "TestUsernameA1!@#$";
        String passwordSpaces = "   PassA1!Word$%2   ";

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.encodeForURL(usernameSpecialChars) + "&" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.encodeForURL(passwordSpaces)
                , LoginUtil.decorateURL(url, usernameSpecialChars, passwordSpaces));
    }

    public void testMatchesHashedPwBlank() {
        assertFalse(LoginUtil.matches(new char[] {'t','s','t'}, ""));
        assertFalse(LoginUtil.matches(new char[] {'t','s','t'}, null));
        assertFalse(LoginUtil.matches(new char[] {'t','s','t'}, "   "));
    }

    public void testHasAndSaltBlankPass() {
        assertNull(LoginUtil.hashAndSalt(new char[0]));
        assertNull(LoginUtil.hashAndSalt((char[]) null));
    }

    public void testClearNullAndEmpty() {
        char[] password = null;
        LoginUtil.clear(password);
        assertNull(password);
        char[] emptyPassword = {};
        LoginUtil.clear(emptyPassword);
        assertTrue(emptyPassword.length == 0);
    }

    public void testClearOk() {

        char[] password = {'p','a','s','s','A','1','!'};
        char[] samePassword = {'p','a','s','s','A','1','!'};
        int passwordLength = password.length;
        LoginUtil.clear(password);
        assertTrue(password.length == passwordLength);
        for (int index = 0; index < passwordLength; index++) {
            assertTrue(password[index] != samePassword[index]);
        }

    }

    public void testIsUsernameAny() throws IOException {
        Controller controller = new Controller();
        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");

        controller.getConfig().put("server.username.isemail","No");

        assertFalse(LoginUtil.isUsernameAny(controller));

        controller.getConfig().put("server.username.isemail","Yes");
        assertFalse(LoginUtil.isUsernameAny(controller));

        controller.getConfig().put("server.username.isemail","both");
        assertTrue(LoginUtil.isUsernameAny(controller));

        controller.getConfig().put("server.username.isemail","BOTH");
        assertTrue(LoginUtil.isUsernameAny(controller));

        controller.getConfig().put("server.username.isemail","BoTh");
        assertTrue(LoginUtil.isUsernameAny(controller));

        FileUtils.forceDelete(file);
    }

    public void testIsBooleanConfValueNull() throws IOException {
        Controller controller = new Controller();
        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");

        controller.getConfig().put("disableui","true");
        controller.getConfig().setProperty("server.username.isemail", "null");

        assertFalse(LoginUtil.isBoolConfValue(controller));

        controller.getConfig().setProperty("server.username.isemail", "both");
        assertFalse(LoginUtil.isBoolConfValue(controller));

        controller.getConfig().setProperty("server.username.isemail", "true");
        assertTrue(LoginUtil.isBoolConfValue(controller));
        controller.getConfig().setProperty("server.username.isemail", "false");
        assertTrue(LoginUtil.isBoolConfValue(controller));

        controller.getConfig().setProperty("server.username.isemail", "TRUE");
        assertTrue(LoginUtil.isBoolConfValue(controller));
        controller.getConfig().setProperty("server.username.isemail", "FALSE");
        assertTrue(LoginUtil.isBoolConfValue(controller));

        controller.getConfig().setProperty("server.username.isemail", "TrUe");
        assertTrue(LoginUtil.isBoolConfValue(controller));
        controller.getConfig().setProperty("server.username.isemail", "FaLsE");
        assertTrue(LoginUtil.isBoolConfValue(controller));

        controller.shutdown();
        FileUtils.forceDelete(file);
    }

    public void testGetInviteUsernameShibboleth() throws IOException {
        Controller controller = new Controller();

        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");

        controller.getConfig().put("server.username.isemail","shibbolethTesting");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","Testbwidm");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","nutzerkennung");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","TTTshiBBolEtH");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","BWIDM");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","nutZerKeNNuNg");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.shutdown();
        FileUtils.forceDelete(file);
    }

    public void testGetInviteUsername() throws IOException {
        Controller controller = new Controller();
        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");

        //If it's not Shibboleth then same logic as getUsernameText

        controller.getConfig().put("server.username.isemail","true");
        assertEquals(Translation.get("general.email") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","both");
        assertEquals(Translation.get("general.username") + '/' + Translation.get("general.email") + ":",
                LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","false");
        assertEquals(Translation.get("general.username") + ":", LoginUtil.getInviteUsernameLabel(controller));

        controller.getConfig().put("server.username.isemail","TestingString");
        assertEquals("TestingString:", LoginUtil.getInviteUsernameLabel(controller));

        //If it is removed, the default value will be considered, which is both
        controller.getConfig().remove("server.username.isemail");
        assertEquals(Translation.get("general.username") + '/' + Translation.get("general.email") + ":",
                LoginUtil.getInviteUsernameLabel(controller));

        FileUtils.forceDelete(file);

    }

    public void testGetUsernameTextNull() {
        Controller controller = null;

        try {
            LoginUtil.getUsernameText(controller);
            fail("Did not reject controller when controller was null");
        } catch (NullPointerException e){
            //OK since controller was null
        }
    }

    public void testGetUsernameTestOk() throws IOException {
        Controller controller = new Controller();

        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");

        controller.getConfig().put("server.username.isemail","true");
        assertEquals(Translation.get("general.email"), LoginUtil.getUsernameText(controller));

        controller.getConfig().put("server.username.isemail","both");
        assertEquals(Translation.get("general.username") + '/' + Translation.get("general.email"),
                LoginUtil.getUsernameText(controller));

        controller.getConfig().put("server.username.isemail","false");
        assertEquals(Translation.get("general.username"), LoginUtil.getUsernameText(controller));

        controller.getConfig().put("server.username.isemail","ThisIsATest");
        assertEquals("ThisIsATest", LoginUtil.getUsernameText(controller));

        //If it is removed, the default value will be considered, which is both
        controller.getConfig().remove("server.username.isemail");
        assertEquals(Translation.get("general.username") + '/' + Translation.get("general.email"),
                LoginUtil.getUsernameText(controller));

        FileUtils.forceDelete(file);
    }

    public void testIsUsernameEmailOnlyTest() throws IOException {
        Controller controller = new Controller();
        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");

        controller.getConfig().put("server.username.isemail","both");
        assertFalse(LoginUtil.isUsernameEmailOnly(controller));

        controller.getConfig().put("server.username.isemail","Testing");
        assertFalse(LoginUtil.isUsernameEmailOnly(controller));

        controller.getConfig().put("server.username.isemail","true");
        assertTrue(LoginUtil.isUsernameEmailOnly(controller));

        controller.getConfig().put("server.username.isemail","false");
        assertFalse(LoginUtil.isUsernameEmailOnly(controller));

        FileUtils.forceDelete(file);
    }

    public void testIsValidUsername() throws IOException {
        Controller controller = new Controller();
        File file = new File("build/test/testConfig.config");
        file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("disableui=true");
        fileWriter.close();
        controller.startConfig("build/test/testConfig.config");
        assertFalse(LoginUtil.isValidUsername(controller,""));
        assertFalse(LoginUtil.isValidUsername(controller,"    "));

        controller.getConfig().put("server.username.isemail","both");
        assertTrue(LoginUtil.isValidUsername(controller,"TestUser"));

        controller.getConfig().put("server.username.isemail","true");
        assertTrue(LoginUtil.isValidUsername(controller, "test@testing.com"));
        assertFalse(LoginUtil.isValidUsername(controller, "TestEmailWrong"));
        assertFalse(LoginUtil.isValidUsername(controller, ""));

        controller.getConfig().put("server.username.isemail","false");
        assertTrue(LoginUtil.isValidUsername(controller, "UserOne"));
        assertTrue(LoginUtil.isValidUsername(controller,"Test@testing.com"));
        assertFalse(LoginUtil.isValidUsername(controller, "   "));

        FileUtils.forceDelete(file);
    }

    public void testSatisfiesUnixPoicyNull() {
        assertFalse(LoginUtil.satisfiesUnixPolicy(null));
        String pass = null;
        assertFalse(LoginUtil.satisfiesUnixPolicy(pass));
    }

    public void testDeobfuscateIllegalArgument() {
        assertNull(LoginUtil.deobfuscate("!"));
        assertNull(LoginUtil.deobfuscate("\""));
    }


    public void testObfuscate() {
        String password = "xC33öcn$k3444o$$44";
        String obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, Util.toString(LoginUtil.deobfuscate(obf)));
        for (int i = 0; i < 200; i++) {
            password = IdGenerator.makeId();
            obf = LoginUtil.obfuscate(password.toCharArray());
            String deObf = Util.toString(LoginUtil.deobfuscate(obf));
            assertEquals(deObf, password.length(), deObf.length());
            assertEquals(password, deObf);
        }
        assertNull(LoginUtil.obfuscate(null));
        assertNull(LoginUtil.deobfuscate(null));
        assertTrue(Arrays.equals("".toCharArray(),
                LoginUtil.deobfuscate(LoginUtil.obfuscate("".toCharArray()))));;
        assertTrue(Arrays.equals("  ".toCharArray(),
                LoginUtil.deobfuscate(LoginUtil.obfuscate("  ".toCharArray()))));;

        password = "%$§\"&/(09€";
        obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, Util.toString(LoginUtil.deobfuscate(obf)));

        password = "EsJs3XngawbCkMurIibtzQD23+OVPFjh2+uB4A8LaEA=";
        obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, Util.toString(LoginUtil.deobfuscate(obf)));
    }

    public void testHash() {
        String password = IdGenerator.makeId();
        String hasedSalted = LoginUtil.hashAndSalt(password.toCharArray());
        assertTrue(LoginUtil.matches(password.toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches("test".toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches(null, hasedSalted));
        // Legacy support.
        assertTrue(LoginUtil.matches("XXX".toCharArray(), "XXX"));
    }

    public void testHashFormat() {
        String hashed = LoginUtil.hashAndSalt("testPassword123".toCharArray());
        assertNotNull(hashed);
        String[] parts = hashed.split(":");
        assertEquals(3, parts.length);
        assertEquals("ARGON2ID", parts[0]);
        assertFalse(parts[1].isEmpty());
        assertFalse(parts[2].isEmpty());
    }

    public void testHashUniqueSalts() {
        String hash1 = LoginUtil.hashAndSalt("samePassword".toCharArray());
        String hash2 = LoginUtil.hashAndSalt("samePassword".toCharArray());
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertFalse("Same password must produce different hashes due to salt",
                hash1.equals(hash2));
        assertTrue(LoginUtil.matches("samePassword".toCharArray(), hash1));
        assertTrue(LoginUtil.matches("samePassword".toCharArray(), hash2));
    }

    public void testHashDifferentPasswords() {
        String hash1 = LoginUtil.hashAndSalt("password1".toCharArray());
        String hash2 = LoginUtil.hashAndSalt("password2".toCharArray());
        assertTrue(LoginUtil.matches("password1".toCharArray(), hash1));
        assertTrue(LoginUtil.matches("password2".toCharArray(), hash2));
        assertFalse(LoginUtil.matches("password1".toCharArray(), hash2));
        assertFalse(LoginUtil.matches("password2".toCharArray(), hash1));
    }

    public void testMatchesWrongPassword() {
        String hashed = LoginUtil.hashAndSalt("correctPassword".toCharArray());
        assertFalse(LoginUtil.matches("wrongPassword".toCharArray(), hashed));
        assertFalse(LoginUtil.matches("CORRECTPASSWORD".toCharArray(), hashed));
        assertFalse(LoginUtil.matches("correctPassword ".toCharArray(), hashed));
        assertFalse(LoginUtil.matches(" correctPassword".toCharArray(), hashed));
        assertFalse(LoginUtil.matches("".toCharArray(), hashed));
    }

    public void testMatchesNullCandidate() {
        String hashed = LoginUtil.hashAndSalt("test".toCharArray());
        assertFalse(LoginUtil.matches(null, hashed));
    }

    public void testMatchesInvalidFormat() {
        assertFalse(LoginUtil.matches("test".toCharArray(), "SHA-256:onlytwocolons"));
        assertFalse(LoginUtil.matches("test".toCharArray(), "UNKNOWN:salt:hash"));
        assertFalse(LoginUtil.matches("test".toCharArray(), "MD5:"));
        assertFalse(LoginUtil.matches("test".toCharArray(), "SHA-256:"));
    }

    public void testMatchesLegacyCleartext() {
        assertTrue(LoginUtil.matches("plaintext".toCharArray(), "plaintext"));
        assertFalse(LoginUtil.matches("wrong".toCharArray(), "plaintext"));
        assertFalse(LoginUtil.matches("MD5password".toCharArray(), "MD5password"));
        assertFalse(LoginUtil.matches("SHA-256pw".toCharArray(), "SHA-256pw"));
    }

    public void testMatchesLegacySHA256() {
        String password = "TestPassword123!";
        String salt = "legacySalt";
        String legacyHash = "SHA-256:" + salt + ":" + LoginUtil.hash("SHA-256", password, salt);

        assertTrue(LoginUtil.matches(password.toCharArray(), legacyHash));
        assertFalse(LoginUtil.matches("WrongPassword".toCharArray(), legacyHash));
        assertTrue(LoginUtil.needsRehash(legacyHash));
        assertTrue(LoginUtil.isHashed(legacyHash));
    }

    public void testMatchesLegacyMD5() {
        String password = "TestPassword123!";
        String salt = "legacySalt";
        String legacyHash = "MD5:" + salt + ":" + LoginUtil.hash("MD5", password, salt);

        assertTrue(LoginUtil.matches(password.toCharArray(), legacyHash));
        assertFalse(LoginUtil.matches("WrongPassword".toCharArray(), legacyHash));
        assertTrue(LoginUtil.needsRehash(legacyHash));
        assertTrue(LoginUtil.isHashed(legacyHash));
    }

    public void testNeedsRehash() {
        String argon2Hash = LoginUtil.hashAndSalt("test".toCharArray());
        assertFalse(LoginUtil.needsRehash(argon2Hash));

        String sha256Hash = "SHA-256:salt:hash";
        assertTrue(LoginUtil.needsRehash(sha256Hash));

        String md5Hash = "MD5:salt:hash";
        assertTrue(LoginUtil.needsRehash(md5Hash));

        assertTrue(LoginUtil.needsRehash("cleartext"));
        assertFalse(LoginUtil.needsRehash(null));
    }

    public void testIsHashed() {
        assertTrue(LoginUtil.isHashed("SHA-256:salt:hash"));
        assertTrue(LoginUtil.isHashed("MD5:salt:hash"));
        assertTrue(LoginUtil.isHashed("ARGON2ID:salt:hash"));
        assertFalse(LoginUtil.isHashed("plaintext"));
        assertFalse(LoginUtil.isHashed(null));
        assertFalse(LoginUtil.isHashed(""));
    }

    public void testHashSpecialCharacters() {
        String[] passwords = {
                "päss€wörd!",
                "pass word with spaces",
                "!@#$%^&*()_+-=[]{}|;':\",./<>?",
                "パスワード",
                "a",
                "x".repeat(1000)
        };
        for (String pw : passwords) {
            String hashed = LoginUtil.hashAndSalt(pw.toCharArray());
            assertNotNull("hashAndSalt returned null for: " + pw, hashed);
            assertTrue("matches failed for: " + pw,
                    LoginUtil.matches(pw.toCharArray(), hashed));
        }
    }

    public void testHashConsistency() {
        String password = "consistencyTest";
        String hashed = LoginUtil.hashAndSalt(password.toCharArray());
        for (int i = 0; i < 50; i++) {
            assertTrue(LoginUtil.matches(password.toCharArray(), hashed));
        }
    }

    public void testHashDirectMethod() {
        String digest = "SHA-256";
        String salt = "testSalt";
        String hash1 = LoginUtil.hash(digest, "password", salt);
        String hash2 = LoginUtil.hash(digest, "password", salt);
        assertEquals("Same input must produce same hash", hash1, hash2);

        String hash3 = LoginUtil.hash(digest, "password", "differentSalt");
        assertFalse("Different salt must produce different hash", hash1.equals(hash3));

        String hash4 = LoginUtil.hash(digest, "differentPassword", salt);
        assertFalse("Different password must produce different hash", hash1.equals(hash4));
    }

    public void testPasswordPolicyMinLength() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("aA1!"));
        assertFalse(LoginUtil.satisfiesUnixPolicy("aA1!567"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("aA1!5678"));
    }

    public void testPasswordPolicyRequiresDigit() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("aAbBcCd!"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("aAbBcC1!"));
    }

    public void testPasswordPolicyRequiresLowercase() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("ABCDEF1!"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("ABCDEf1!"));
    }

    public void testPasswordPolicyRequiresUppercase() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("abcdef1!"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("abcdeF1!"));
    }

    public void testPasswordPolicyRequiresSpecialChar() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("abcdeF12"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("abcdeF1!"));
    }

    public void testPasswordPolicyNoWhitespace() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("abc deF1!"));
        assertFalse(LoginUtil.satisfiesUnixPolicy(" abcdeF1!"));
    }

    /**
     * Brute-force attack simulation: measures how long it takes to check
     * passwords against a hash. Demonstrates that the 1597-iteration
     * SHA-256 approach is relatively fast to crack compared to BCrypt/Argon2.
     */
    public void testBruteForceAttackResistance() {
        String secretPassword = "Secr3t!X";
        String hashed = LoginUtil.hashAndSalt(secretPassword.toCharArray());

        // Dictionary of common passwords to try
        String[] dictionary = {
                "password", "123456", "admin", "letmein", "welcome",
                "monkey", "dragon", "master", "qwerty", "login",
                "password1", "Password1!", "Passw0rd!", "Admin123!",
                "Secr3t!X" // the actual password
        };

        long startTime = System.nanoTime();
        int attempts = 0;
        boolean cracked = false;

        for (String candidate : dictionary) {
            attempts++;
            if (LoginUtil.matches(candidate.toCharArray(), hashed)) {
                cracked = true;
                break;
            }
        }

        long dictionaryTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        assertTrue("Dictionary attack should find the password", cracked);
        System.out.println("[Dictionary Attack] Cracked in " + attempts
                + " attempts, " + dictionaryTimeMs + " ms");

        // Brute-force: measure throughput (how many hashes per second)
        int bruteForceAttempts = 20;
        startTime = System.nanoTime();
        for (int i = 0; i < bruteForceAttempts; i++) {
            LoginUtil.matches(("attempt" + i).toCharArray(), hashed);
        }
        long bruteForceTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        long hashesPerSecond = bruteForceTimeMs > 0
                ? (bruteForceAttempts * 1000L) / bruteForceTimeMs : 0;

        System.out.println("[Brute Force] " + bruteForceAttempts + " attempts in "
                + bruteForceTimeMs + " ms (" + hashesPerSecond + " hashes/sec)");
        System.out.println("[Brute Force] At this rate, a 8-char password "
                + "(uppercase+lowercase+digits+special, ~70^8 = 576 billion combos) "
                + "would take ~" + (576_000_000_000L / Math.max(hashesPerSecond, 1) / 3600 / 24 / 365)
                + " years on a single thread");

        assertTrue("Argon2id should be slow enough to resist brute force",
                hashesPerSecond < 100);
    }

    /**
     * Timing attack resistance: verifies that matching a correct vs incorrect
     * password takes roughly the same time (no early-exit information leak).
     */
    public void testTimingAttackResistance() {
        String password = "TimingT3st!";
        String hashed = LoginUtil.hashAndSalt(password.toCharArray());

        // Warm up JIT
        for (int i = 0; i < 3; i++) {
            LoginUtil.matches(password.toCharArray(), hashed);
            LoginUtil.matches("WrongPass1!".toCharArray(), hashed);
        }

        int rounds = 10;

        // Measure correct password
        long startCorrect = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            LoginUtil.matches(password.toCharArray(), hashed);
        }
        long correctTimeNs = System.nanoTime() - startCorrect;

        // Measure wrong password (same length)
        long startWrong = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            LoginUtil.matches("WrongPass1!".toCharArray(), hashed);
        }
        long wrongTimeNs = System.nanoTime() - startWrong;

        // Measure completely different length
        long startShort = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            LoginUtil.matches("x".toCharArray(), hashed);
        }
        long shortTimeNs = System.nanoTime() - startShort;

        double correctMs = correctTimeNs / 1_000_000.0;
        double wrongMs = wrongTimeNs / 1_000_000.0;
        double shortMs = shortTimeNs / 1_000_000.0;

        System.out.println("[Timing Attack] Correct password: " + String.format("%.2f", correctMs) + " ms");
        System.out.println("[Timing Attack] Wrong password (same len): " + String.format("%.2f", wrongMs) + " ms");
        System.out.println("[Timing Attack] Wrong password (short): " + String.format("%.2f", shortMs) + " ms");

        // Times should be within 50% of each other — hashing dominates, not comparison
        double ratio = Math.max(correctMs, wrongMs) / Math.max(Math.min(correctMs, wrongMs), 0.01);
        System.out.println("[Timing Attack] Correct/Wrong ratio: " + String.format("%.2f", ratio)
                + " (should be close to 1.0)");
        assertTrue("Timing difference between correct and wrong password should be < 2x "
                        + "(ratio=" + String.format("%.2f", ratio) + ")",
                ratio < 2.0);
    }

    public void testOTP() {
        // Valid
        for (int i = 0; i < 10000; i++) {
            String otp = LoginUtil.generateOTP(1000L);
            // 11BrLcYZedRqKqHhdy2sWhT2WCrNrxDEdSvDGgYDzCsFs58BRxYWG
            assertTrue(otp.length() >= 53);
            assertTrue(LoginUtil.isOTPValid(otp));
        }

        // Expired
        String otp = LoginUtil.generateOTP(500L);
        assertTrue(LoginUtil.isOTPValid(otp));
        TestHelper.waitMilliSeconds(600);
        assertFalse(LoginUtil.isOTPValid(otp));

        // Illegal stuff
        assertFalse(LoginUtil.isOTPValid(null));
        assertFalse(LoginUtil.isOTPValid("HACK"));
        assertFalse(LoginUtil
                .isOTPValid("30957s0cuxpcfeärl43#r3ä2ö43täö4eäföedäfgsdägösdägösäfdglsd08g7sa0g7w098470387"));
    }

    public void testPasswordPolicy() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("12"));
        assertFalse(LoginUtil.satisfiesUnixPolicy("12345678"));
        assertFalse(LoginUtil.satisfiesUnixPolicy("ksjfdfgdgkjsrägklöjwerägjrägö100%&sdfsjföklsdj"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("aaa$56AAAA"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("aaa$56AA"));

        assertTrue(LoginUtil.satisfiesUnixPolicy("aaZZa44@"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("!2e4567B"));

        assertFalse(LoginUtil.satisfiesUnixPolicy("@!xxxx332445"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("@!xxXx332445"));

        assertFalse(LoginUtil.satisfiesUnixPolicy("abc123"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("ABC123abc!"));
    }

    public void testGetDigestException() {
        try {
            LoginUtil.hash("asdasd", "testing", "zxc");
            fail("Did not throw runtime exception when digest was not available");
        } catch (RuntimeException e){
            //OK
        }
    }

    public void testControllerNotStarter() {
        Controller controller = new Controller();
        try {
            LoginUtil.getUsernameText(controller);
            fail("Did not throw NullPointerException but controller config was null");
        } catch (NullPointerException e){
            //OK
        }

        try {
            LoginUtil.isValidUsername(controller, "someUser");
            fail("Did not throw NullPointerException but controller config was null");
        } catch (NullPointerException e){
            //OK
        }


        try {
            LoginUtil.isUsernameEmailOnly(controller);
            fail("Did not throw NullPointerException but controller config was null");
        } catch (NullPointerException e){
            //OK
        }

        try {
            LoginUtil.isUsernameAny(controller);
            fail("Did not throw NullPointerException but controller config was null");
        } catch (NullPointerException e){
            //OK
        }

        try {
            LoginUtil.isBoolConfValue(controller);
            fail("Did not throw NullPointerException but controller config was null");
        } catch (NullPointerException e){
            //OK
        }

        try {
            LoginUtil.getInviteUsernameLabel(controller);
            fail("Did not throw NullPointerException but controller config was null");
        } catch (NullPointerException e){
            //OK
        }
    }


}