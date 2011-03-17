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
 * $Id: CleanupTranslationFiles.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;

/**
 * Helper class around configuration
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class ConfigurationLoader {
    // TODO Sync with MaintenanceFolder.DEFAULT_CONFIG_FILENAME
    // TODO #2025
    public static final String DEFAULT_CONFIG_FILENAME = "Default.config";
    private static final String DEFAULT_PROPERTIES_URI = "/client_deployment/"
        + DEFAULT_CONFIG_FILENAME;
    private static final String PREFERENCES_PREFIX = "pref.";
    private static final int URL_CONNECT_TIMEOUT_SECONDS = 15;

    private static Logger LOG = Logger.getLogger(ConfigurationLoader.class
        .getName());

    private ConfigurationLoader() {
    }

    /**
     * #2179 Loads the the config from the server and merges it with the
     * controllers config.
     * 
     * @param controller
     * @return if a config was successfully loaded
     */
    public static boolean loadAndMergeConfigURL(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        String configURL = ConfigurationEntry.CONFIG_URL.getValue(controller);
        if (StringUtils.isBlank(configURL)) {
            return false;
        }
        try {
            Properties serverConfig = loadPreConfiguration(configURL);
            boolean overWrite = Boolean
                .valueOf(ConfigurationEntry.CONFIG_OVERWRITE_VALUES
                    .getDefaultValue());
            String owStr = serverConfig
                .getProperty(ConfigurationEntry.CONFIG_OVERWRITE_VALUES
                    .getConfigKey());
            try {
                overWrite = Boolean.parseBoolean(owStr);
            } catch (Exception e) {
                LOG.warning("Unable to parse config from server: " + configURL
                    + ". Overwrite str: " + owStr + ". " + e);
            }
            int i = merge(serverConfig, controller.getConfig(),
                controller.getPreferences(), overWrite);
            LOG.warning("Loaded " + i + " settings (overwrite? " + overWrite
                + ") from server: " + configURL);
            if (i > 0) {
                controller.saveConfig();
            }
            return true;
        } catch (Exception e) {
            LOG.warning("Unable to load config from server: " + configURL
                + ". " + e);
            return false;
        }
    }

    /**
     * Loads a pre-configuration from a server. Automatically adds HTTP:// and
     * url suffix.
     * 
     * @param server
     * @return the loaded config.
     * @throws IOException
     */
    public static Properties loadPreConfiguration(String server)
        throws IOException
    {
        String finalURL = Util.removeLastSlashFromURI(server);
        if (!finalURL.startsWith("http")) {
            finalURL = "http://" + finalURL;
        }
        if (!finalURL.endsWith(".config")
            && !finalURL.contains(DEFAULT_PROPERTIES_URI))
        {
            finalURL += DEFAULT_PROPERTIES_URI;
        }
        return loadPreConfiguration(new URL(finalURL));
    }

    /**
     * Loads a pre-configuration from the URL
     * 
     * @param from
     *            the URL to load from
     * @return the loaded properties WITHOUT those in config.
     * @throws IOException
     */
    public static Properties loadPreConfiguration(URL from) throws IOException {
        Reject.ifNull(from, "URL is null");
        URLConnection con = from.openConnection();
        con.setConnectTimeout(1000 * URL_CONNECT_TIMEOUT_SECONDS);
        con.connect();
        InputStream in = con.getInputStream();
        try {
            return loadPreConfiguration(in);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the pre configuration from file from the classpath.
     * 
     * @param filename
     *            the filename to load
     * @return the loaded properties
     * @throws IOException
     */
    public static Properties loadPreConfigFromClasspath(String filename)
        throws IOException
    {
        InputStream in = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(filename);
        if (in == null) {
            throw new FileNotFoundException("File '" + filename
                + "' not found in classpath");
        }
        try {
            return loadPreConfiguration(in);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Convenient method to combine
     * {@link #mergeConfigs(Properties, Properties, boolean)} and
     * {@link #mergePreferences(Properties, Preferences, boolean)}
     * 
     * @param preConfig
     *            the pre config
     * @param targetConfig
     *            the config file to set the pre-configuration values into.
     * @param targetPreferences
     *            the preferences to set the pre-configuration values into.
     * @param replaceExisting
     *            if existing key/value pairs will be overwritten by pairs of
     *            pre config.
     * @return the sum of merged entries.
     */
    public static int merge(Properties preConfig, Properties targetConfig,
        Preferences targetPreferences, boolean replaceExisting)
    {
        return mergeConfigs(preConfig, targetConfig, replaceExisting)
            + mergePreferences(preConfig, targetPreferences, replaceExisting);
    }

    /**
     * Merges the give pre configuration properties into the target config
     * properties. It can be choosen if existing keys in the target properties
     * should be replaced or not.
     * 
     * @param preConfig
     *            the pre config
     * @param targetConfig
     *            the config file to set the pre-configuration values into.
     * @param replaceExisting
     *            if existing key/value pairs will be overwritten by pairs of
     *            pre config.
     * @return the number of merged entries.
     */
    private static int mergeConfigs(Properties preConfig,
        Properties targetConfig, boolean replaceExisting)
    {
        Reject.ifNull(preConfig, "PreConfig is null");
        Reject.ifNull(targetConfig, "TargetConfig is null");
        int n = 0;
        for (Object obj : preConfig.keySet()) {
            String key = (String) obj;
            String value = preConfig.getProperty(key);
            if (!targetConfig.containsKey(key) || replaceExisting) {
                targetConfig.setProperty(key, value);
                if (!key.startsWith(PREFERENCES_PREFIX)) {
                    n++;
                }
                LOG.finer("Preconfigured " + key + "=" + value);
            }
        }
        if (n > 0) {
            LOG.fine(n + " default configurations set");
        } else {
            LOG.finer("No additional default configurations set");
        }
        return n;
    }

    /**
     * PUBLIC because of tests. DO NOT USE. Use
     * {@link #merge(Properties, Properties, Preferences, boolean)} instead.
     * <p>
     * Merges the give pre configuration properties into the target preferences.
     * It can be choosen if existing keys in the target preferences should be
     * replaced or not. Will only set those values from preConfig where the key
     * begins with "pref." and cut it off. "pref.xxx=true" will be set to
     * "xxx=true" in preferences.
     * 
     * @param preConfig
     *            the pre config
     * @param targetPreferences
     *            the preferences to set the pre-configuration values into.
     * @param replaceExisting
     *            if existing key/value pairs will be overwritten by pairs of
     *            pre config.
     * @return the number of merged entries.
     */
    public static int mergePreferences(Properties preConfig,
        Preferences targetPreferences, boolean replaceExisting)
    {
        Reject.ifNull(preConfig, "PreConfig is null");
        Reject.ifNull(targetPreferences, "TargetPreferences is null");
        int n = 0;
        for (Object obj : preConfig.keySet()) {
            String key = (String) obj;
            String value = preConfig.getProperty(key);
            if (!key.startsWith(PREFERENCES_PREFIX)) {
                continue;
            } else {
                key = key.substring(PREFERENCES_PREFIX.length(), key.length());
            }
            boolean entryMissing = "-XXWEIRED-DEFAULT-VALUE"
                .equals(targetPreferences.get(key, "-XXWEIRED-DEFAULT-VALUE"));
            if (entryMissing || replaceExisting) {
                targetPreferences.put(key, value);
                n++;
                LOG.finer("Preconfigured " + key + "=" + value);
            }
        }
        if (n > 0) {
            LOG.fine(n + " default preferences set");
        } else {
            LOG.finer("No additional default preferences set");
        }
        return n;
    }

    /**
     * Loads a configuration file from the given input stream.
     * 
     * @param in
     *            the input stream to read the pre-config from
     * @return the loaded properties.
     * @throws IOException
     */
    private static Properties loadPreConfiguration(InputStream in)
        throws IOException
    {
        Reject.ifNull(in,
            "Unable to load Preconfiguration. Input stream is null");
        Properties preConfig = new Properties();
        preConfig.load(in);
        return preConfig;
    }
}
