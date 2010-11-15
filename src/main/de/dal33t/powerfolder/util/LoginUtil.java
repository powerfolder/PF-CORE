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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;

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

                // Old fashioned MD5 encoded login support for old servers.
                String salt = IdGenerator.makeId() + IdGenerator.makeId();
                String mix = salt + new String(password).trim() + salt;
                byte[] passwordMD5 = Util.md5(mix.getBytes(Convert.UTF8));
                url += "&";
                url += Constants.LOGIN_PARAM_PASSWORD_MD5;
                url += "=";
                url += Util.endcodeForURL(Base64.encodeBytes(passwordMD5));
                url += "&";
                url += Constants.LOGIN_PARAM_SALT;
                url += "=";
                url += Util.endcodeForURL(Base64.encodeString(salt));
            }
        }
        return url;
    }
}
