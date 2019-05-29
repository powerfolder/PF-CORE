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
        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.endcodeForURL(usernameWithSpecial), LoginUtil.decorateURL(url, usernameWithSpecial, password));

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
        System.out.println(LoginUtil.decorateURL(url, username, password));

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + username + "&" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.endcodeForURL(LoginUtil.obfuscate(password)),
                LoginUtil.decorateURL(url, username, password));

    }

    public void testDecorateUrlObfuscatedEmptyUser() {
        String url = "http://localhost:8080";
        String username = null;
        String password = "TestPassA1!";

        assertEquals(url + "?" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.endcodeForURL(password), LoginUtil.decorateURL(url, username, password));

        String usernameEmpty = "";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.endcodeForURL(password), LoginUtil.decorateURL(url, usernameEmpty, password));

        String usernameSpaces = "   ";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.endcodeForURL(password), LoginUtil.decorateURL(url, usernameSpaces, password));

    }

    public void testDecorateUrlObfuscatedEmptyPassword() {
        String url = "http://localhost:8080";
        String username = "TestUsername";
        String password = null;

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.endcodeForURL(username), LoginUtil.decorateURL(url, username, password));

        String passwordEmpty = "";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.endcodeForURL(username), LoginUtil.decorateURL(url, username, passwordEmpty));

        String passwordSpaces = "   ";
        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.endcodeForURL(username), LoginUtil.decorateURL(url, username, passwordSpaces));
    }

    public void testDecorateUrlObfuscatedOk() {
        String url = "http://localhost:8080";
        String username = "TestUsername";
        String password = "PassA1!Word$%2";

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.endcodeForURL(username) + "&" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.endcodeForURL(password)
                , LoginUtil.decorateURL(url, username, password));

        String usernameSpecialChars = "TestUsernameA1!@#$";
        String passwordSpaces = "   PassA1!Word$%2   ";

        assertEquals(url + "?" + Constants.LOGIN_PARAM_USERNAME + "=" + Util.endcodeForURL(usernameSpecialChars) + "&" + Constants.LOGIN_PARAM_PASSWORD_OBF + "=" + Util.endcodeForURL(passwordSpaces)
                , LoginUtil.decorateURL(url, usernameSpecialChars, passwordSpaces));
    }

    public void testMatchesHashedPwBlank() {
        assertFalse(LoginUtil.matches(new char[] {'t','s','t'}, ""));
        assertFalse(LoginUtil.matches(new char[] {'t','s','t'}, null));
        assertFalse(LoginUtil.matches(new char[] {'t','s','t'}, "   "));
    }

    public void testHasAndSaltBlankPass() {
        assertNull(LoginUtil.hashAndSalt(""));
        assertNull(LoginUtil.hashAndSalt("   "));
        assertNull(LoginUtil.hashAndSalt(null));
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
        String hasedSalted = LoginUtil.hashAndSalt(password);
        assertTrue(LoginUtil.matches(password.toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches("test".toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches(null, hasedSalted));
        // Legacy support.
        assertTrue(LoginUtil.matches("XXX".toCharArray(), "XXX"));
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
}