/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;

/**
 * Utility class for login helpers
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.29 $
 */
public class LoginUtil {
    private LoginUtil() {
    }

    private final static int OBF_BYTE = 0xAA;
    public final static String HASH_DIGEST = "MD5";

    /**
     * Obfuscates a password into String. This does NOT mean the password is
     * Encrypted or secure, but it prevents accidentally reveal.
     * <p>
     * Passwords can easily deobfuscated by calling {@link #deobfuscate(String)}
     * 
     * @param password
     *            the password to obfuscate
     * @return the obfuscated password
     */
    public static String obfuscate(char[] password) {
        if (password == null) {
            return null;
        }
        CharBuffer cBuf = CharBuffer.wrap(password);
        byte[] buf = new byte[password.length * 3];
        ByteBuffer bBuf = ByteBuffer.wrap(buf);
        CharsetEncoder enc = Convert.UTF8.newEncoder();
        enc.encode(cBuf, bBuf, true);
        int len = bBuf.position();
        if (len != buf.length) {
            buf = Arrays.copyOf(buf, len);
        }
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) (buf[i] ^ OBF_BYTE);
            buf[i] = (byte) (buf[i] + 127);
        }
        return Base64.encodeBytes(buf, Base64.DONT_BREAK_LINES);
    }

    /**
     * Deobfuscates a obfuscated password by {@link #obfuscate(char[])}
     * 
     * @param passwordOBF
     *            the obfuscated password
     * @return the original password
     */
    public static char[] deobfuscate(String passwordOBF) {
        if (passwordOBF == null) {
            return null;
        }
        try {
            byte[] buf = Base64.decode(passwordOBF, Base64.DONT_BREAK_LINES);
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (buf[i] - 127);
                buf[i] = (byte) (buf[i] ^ OBF_BYTE);
            }
            ByteBuffer bBuf = ByteBuffer.wrap(buf);
            char[] ca = new char[buf.length];
            CharBuffer cBuf = CharBuffer.wrap(ca);
            CharsetDecoder dec = Convert.UTF8.newDecoder();
            dec.decode(bBuf, cBuf, true);
            int len = cBuf.position();
            if (len != ca.length) {
                ca = Arrays.copyOf(ca, len);
            }
            return ca;
        } catch (Exception e) {
            Logger.getLogger(LoginUtil.class.getName()).log(
                Level.SEVERE,
                "Unable to decode obfuscated password: " + passwordOBF + ". "
                    + e, e);
            return null;
        }

    }

    /**
     * Decorates the login URL with credentials if given.
     * 
     * @param loginURL
     *            the login URL, e.g. http://localhost/login
     * @param username
     * @param password
     * @return the login URL with encoded credentials as parameters.
     */
    public static String decorateURL(String loginURL, String username,
        char[] password)
    {
        String url = loginURL;
        if (StringUtils.isNotBlank(username)) {
            url += "?";
            url += Constants.LOGIN_PARAM_USERNAME;
            url += "=";
            url += Util.endcodeForURL(username);
            if (password != null && password.length > 0) {
                url += "&";
                url += Constants.LOGIN_PARAM_PASSWORD_OBF;
                url += "=";
                url += Util.endcodeForURL(obfuscate(password));
            }
        }
        return url;
    }

    /**
     * Decorates the login URL with credentials if given.
     * 
     * @param loginURL
     *            the login URL, e.g. http://localhost/login
     * @param username
     * @param passwordObf
     *            the obfuscated password
     * @return the login URL with encoded credentials as parameters.
     */
    public static String decorateURL(String loginURL, String username,
        String passwordObf)
    {
        String url = loginURL;
        if (StringUtils.isNotBlank(username)) {
            url += "?";
            url += Constants.LOGIN_PARAM_USERNAME;
            url += "=";
            url += Util.endcodeForURL(username);
            if (StringUtils.isNotBlank(passwordObf)) {
                url += "&";
                url += Constants.LOGIN_PARAM_PASSWORD_OBF;
                url += "=";
                url += Util.endcodeForURL(passwordObf);
            }
        }
        return url;
    }

    public static boolean matches(char[] pwCandidate, String hashedPW) {
        String[] parts = hashedPW.split(":");
        if (parts.length != 3) {
            // Legacy for clear text passwords
            return hashedPW != null && !hashedPW.startsWith(HASH_DIGEST)
                && Arrays.equals(pwCandidate, Util.toCharArray(hashedPW));
        }
        if (!parts[0].equalsIgnoreCase(HASH_DIGEST)) {
            return false;
        }
        String salt = parts[1];
        String expectedHash = parts[2];
        String actualHash = hash(Util.toString(pwCandidate), salt);
        return expectedHash.equals(actualHash);
    }

    /**
     * @param password
     *            the password to process
     * @return the hashed password and salt.
     */
    public static String hashAndSalt(String password) {
        String salt = IdGenerator.makeId();
        return getDigest().getAlgorithm() + ":" + salt + ":"
            + hash(password, salt);
    }

    /**
     * @param password
     *            the password to process
     * @return the hashed password and salt.
     */
    public static String hash(String password, String salt) {
        String input = password + salt;
        byte[] in = input.getBytes(Convert.UTF8);
        for (int i = 0; i < 1597; i++) {
            in = digest(in);
        }
        return Base64.encodeBytes(in);
    }

    /**
     * Clears a password array to avoid keeping the password in plain text in
     * memory.
     * 
     * @param password
     *            the password array to clear. Array is destroyed and unusable
     *            after.
     */
    public static void clear(char[] password) {
        if (password == null || password.length == 0) {
            return;
        }
        for (int i = 0; i < password.length; i++) {
            password[i] = (char) (Math.random() * 256);
        }
    }

    /**
     * PFS-569: Hack alert!
     * 
     * @param controller
     * @return
     */
    public static String getInviteUsernameLabel(Controller controller) {
        if (isUsernameShibboleth(controller)) {
            return Translation.getTranslation("general.email") + ":";
        }
        return getUsernameLabel(controller);
    }

    /**
     * #2401: Texts: "Email" should not be shown if using AD username, e.g. on
     * login
     * 
     * @param controller
     * @return
     */
    public static String getUsernameLabel(Controller controller) {
        return getUsernameText(controller) + ":";
    }

    /**
     * #2401: Texts: "Email" should not be shown if using AD username, e.g. on
     * login
     * 
     * @param controller
     * @return
     */
    public static String getUsernameText(Controller controller) {
        Reject.ifNull(controller, "Controller");
        if (isUsernameEmailOnly(controller)) {
            return Translation.getTranslation("general.email");
        } else if (isUsernameAny(controller)) {
            return Translation.getTranslation("general.username") + "/"
                + Translation.getTranslation("general.email");
        } else {
            if (isBoolConfValue(controller)) {
                return Translation.getTranslation("general.username");
            }
            // Otherwise return text of config entry.
            return ConfigurationEntry.SERVER_USERNAME_IS_EMAIL
                .getValue(controller);
        }
    }

    public static boolean isValidUsername(Controller controller, String username)
    {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        if (isUsernameAny(controller)) {
            return true;
        }
        // PFS-569
        if (isUsernameShibboleth(controller)) {
            return username.contains("!") || username.contains("@");
        }
        if (ConfigurationEntry.SERVER_USERNAME_IS_EMAIL
            .getValueBoolean(controller))
        {
            return Util.isValidEmail(username);
        }
        return true;
    }

    private static boolean isUsernameShibboleth(Controller controller) {
        String v = ConfigurationEntry.SERVER_USERNAME_IS_EMAIL
            .getValue(controller);
        if (v == null) {
            return false;
        }
        return v.toLowerCase().contains("shibboleth");
    }

    private static boolean isUsernameEmailOnly(Controller controller) {
        if (isUsernameAny(controller)) {
            return false;
        }
        if (!isBoolConfValue(controller)) {
            return false;
        }
        return ConfigurationEntry.SERVER_USERNAME_IS_EMAIL
            .getValueBoolean(controller);
    }

    private static boolean isUsernameAny(Controller controller) {
        String v = ConfigurationEntry.SERVER_USERNAME_IS_EMAIL
            .getValue(controller);
        return "both".equalsIgnoreCase(v);
    }

    private static boolean isBoolConfValue(Controller controller) {
        String value = ConfigurationEntry.SERVER_USERNAME_IS_EMAIL
            .getValue(controller);
        if (value == null) {
            return false;
        }
        return value.equalsIgnoreCase("true")
            || value.equalsIgnoreCase("false");
    }

    /**
     * Calculates the SHA digest and returns the value as a 16 element
     * <code>byte[]</code>.
     * 
     * @param data
     *            Data to digest
     * @return digest
     */
    private static byte[] digest(byte[] data) {
        return getDigest().digest(data);
    }

    /**
     * Returns a MessageDigest for the given <code>algorithm</code>.
     * 
     * @param algorithm
     *            The MessageDigest algorithm name.
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Returns an MD5 MessageDigest.
     * 
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getDigest() {
        return getDigest(HASH_DIGEST);
    }
}
