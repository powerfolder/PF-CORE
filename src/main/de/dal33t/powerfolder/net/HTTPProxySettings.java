/*
 * Copyright 2004 - 2008 Christian Sprajc All rights reserved.
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
 * $Id: AddressRange.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.net;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;

/**
 * Helper to set/load the general HTTP proxy settings of this VM.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public class HTTPProxySettings {
    private static final Logger LOG = Logger.getLogger(HTTPProxySettings.class
        .getName());

    private HTTPProxySettings() {
    }

    public static void setProxyProperties(String proxyHost, int proxyPort) {
        if (!StringUtils.isBlank(proxyHost)) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", "" + proxyPort);
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", "" + proxyPort);
        } else {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
        }
    }

    public static void setCredentials(final String proxyUsername,
        final String proxyPassword)
    {
        if (StringUtils.isBlank(proxyUsername)) {
            Authenticator.setDefault(null);
        } else {
            Reject.ifBlank(proxyPassword, "Password is blank");
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUsername,
                        proxyPassword.toCharArray());
                }
            });
        }
    }

    public static void loadFromConfig(Controller controller) {
        Reject.ifNull(controller, "Controller is null");

        if (ConfigurationEntry.HTTP_PROXY_SYSTEMPROXY
            .getValueBoolean(controller))
        {
            LOG.fine("Use system proxy settings");
            System.setProperty("java.net.useSystemProxies", "true");
            return;
        }

        String proxyHost = ConfigurationEntry.HTTP_PROXY_HOST
            .getValue(controller);
        if (StringUtils.isBlank(proxyHost)) {
            LOG.finer("No proxy");
            System.setProperty("java.net.useSystemProxies", "false");
            return;
        }
        int proxyPort = ConfigurationEntry.HTTP_PROXY_PORT
            .getValueInt(controller);
        setProxyProperties(proxyHost, proxyPort);

        // Username / Password
        String proxyUsername = ConfigurationEntry.HTTP_PROXY_USERNAME
            .getValue(controller);
        String proxyPassword = Util.toString(LoginUtil.deobfuscate(
            ConfigurationEntry.HTTP_PROXY_PASSWORD.getValue(controller)));
        try {
            setCredentials(proxyUsername, proxyPassword);
        } catch (IllegalArgumentException iae) {
            LOG.info("Could not set credentials for http proxy: " + iae.getMessage());
        }

        System.setProperty("java.net.useSystemProxies", "false");

        if (LOG.isLoggable(Level.WARNING)) {
            String auth = StringUtils.isBlank(proxyUsername)
                ? ""
                : "(" + proxyUsername + "/"
                    + (proxyPassword != null ? proxyPassword.length() : " n/a")
                    + " chars)";
            LOG.fine("Loaded HTTP proxy settings: " + proxyHost + ":"
                + proxyPort + " " + auth);
        }
    }

    public static void saveToConfig(Controller controller, String proxyHost,
        int proxyPort, String proxyUsername, String proxyPassword)
    {
        Reject.ifNull(controller, "Controller is null");
        if (!StringUtils.isBlank(proxyHost)) {
            ConfigurationEntry.HTTP_PROXY_HOST.setValue(controller, proxyHost);
        } else {
            ConfigurationEntry.HTTP_PROXY_HOST.removeValue(controller);
        }
        if (proxyPort > 0) {
            ConfigurationEntry.HTTP_PROXY_PORT.setValue(controller,
                String.valueOf(proxyPort));
        } else {
            ConfigurationEntry.HTTP_PROXY_PORT.removeValue(controller);
        }
        if (!StringUtils.isBlank(proxyUsername)) {
            ConfigurationEntry.HTTP_PROXY_USERNAME.setValue(controller,
                proxyUsername);
        } else {
            ConfigurationEntry.HTTP_PROXY_USERNAME.removeValue(controller);
        }
        if (!StringUtils.isBlank(proxyPassword)) {
            ConfigurationEntry.HTTP_PROXY_PASSWORD.setValue(controller,
                LoginUtil.obfuscate(Util.toCharArray(proxyPassword)));
        } else {
            ConfigurationEntry.HTTP_PROXY_PASSWORD.removeValue(controller);
        }
        setProxyProperties(proxyHost, proxyPort);
        setCredentials(proxyUsername, proxyPassword);

        if (StringUtils.isBlank(proxyHost)) {
            LOG.fine("Removed proxy settings");
        } else {
            String auth = StringUtils.isBlank(proxyUsername) ? "" : "("
                + proxyUsername + "/" + proxyPassword.length() + " chars)";
            LOG.fine("Saved HTTP proxy settings: " + proxyHost + ":"
                + proxyPort + " " + auth);
        }
    }

    public static final boolean useProxy() {
        return StringUtils.isNotBlank(System.getProperty("http.proxyHost"))
            || "true".equalsIgnoreCase(
                System.getProperty("java.net.useSystemProxies"));
    }
}
