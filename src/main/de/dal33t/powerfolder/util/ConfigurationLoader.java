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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.message.ConfigurationLoadRequest;

/**
 * Helper class around configuration
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class ConfigurationLoader {
    private static final String INITIAL_STARTUP_CONFIG_FILENAME = "initial_startup.config";
    public static final String DEFAULT_CONFIG_FILENAME = "Default.config";
    public static final String RADIUS_CONFIG_FILENAME = "RADIUS.config";
    private static final String DEFAULT_PROPERTIES_URI = "/client_deployment/"
        + DEFAULT_CONFIG_FILENAME;
    private static final String PREFERENCES_PREFIX = "pref.";
    private static final int URL_CONNECT_TIMEOUT_SECONDS = 10;

    private static Logger LOG = Logger.getLogger(ConfigurationLoader.class
        .getName());

    private ConfigurationLoader() {
    }

    public static boolean overwriteConfigEntries(Properties p) {
        boolean overWrite = Boolean
            .valueOf(ConfigurationEntry.CONFIG_OVERWRITE_VALUES
                .getDefaultValue());
        String owStr = p.getProperty(ConfigurationEntry.CONFIG_OVERWRITE_VALUES
            .getConfigKey());
        try {
            overWrite = Boolean.parseBoolean(owStr);
        } catch (Exception e) {
            LOG.warning("Unable to parse pre-config overwrite value. Problem value: "
                + owStr + ". Now using: " + overWrite + ". " + e);
        }
        return overWrite;
    }

    private static boolean dropFolderSettings(Properties p) {
        boolean drop = Boolean
            .valueOf(ConfigurationEntry.CONFIG_DROP_FOLDER_SETTINGS
                .getDefaultValue());
        String owStr = p
            .getProperty(ConfigurationEntry.CONFIG_DROP_FOLDER_SETTINGS
                .getConfigKey());
        try {
            drop = Boolean.parseBoolean(owStr);
        } catch (Exception e) {
            LOG.warning("Unable to parse pre-config drop folders settings value. Problem value: "
                + owStr + ". Now using: " + drop + ". " + e);
        }
        return drop;
    }

    /**
     * Processes/Handles a configuration (re-) load request.
     *
     * @param controller
     * @param clr
     */
    public static void processMessage(final Controller controller,
        ConfigurationLoadRequest clr)
    {
        Reject.ifNull(controller, "Controller");
        Reject.ifNull(clr, "Message");
        try {
            LOG.info("Processing message: " + clr);
            if (StringUtils.isBlank(clr.getConfigURL())) {
                // Single Key=value option
                if (clr.isKeyValue()) {
                    boolean hasValue = controller.getConfig().containsKey(
                        clr.getKey());
                    if (clr.isReplaceExisting() == null
                        || clr.isReplaceExisting()
                        || (!hasValue && !clr.isReplaceExisting()))
                    {
                        if (clr.getValue() == null) {
                            controller.getConfig().remove(clr.getKey());
                        } else {
                            controller.getConfig().put(clr.getKey(),
                                clr.getValue());
                        }
                        // Seems to be valid, store.
                        controller.saveConfig();
                        LOG.log(
                            Level.INFO,
                            "Update configuration " + clr.getKey() + "="
                                + clr.getValue());
                    }
                }
            } else {
                Properties preConfig = ConfigurationLoader
                    .loadPreConfiguration(clr.getConfigURL());
                if (preConfig != null) {
                    boolean overwrite;
                    if (clr.isReplaceExisting() != null) {
                        overwrite = clr.isReplaceExisting();
                    } else {
                        overwrite = overwriteConfigEntries(preConfig);
                    }
                    if (dropFolderSettings(preConfig)) {
                        Set<String> entryIds = FolderSettings
                            .loadEntryIds(controller.getConfig());
                        for (String entryId : entryIds) {
                            FolderSettings.removeEntries(
                                controller.getConfig(), entryId);
                        }
                    }
                    int i = ConfigurationLoader.merge(preConfig,
                        controller.getConfig(), controller.getPreferences(),
                        overwrite);
                    LOG.log(Level.FINE, "Loaded/Merged " + i
                        + " config/prefs entries from: " + clr.getConfigURL());
                    ConfigurationEntry.CONFIG_URL.setValue(controller,
                        clr.getConfigURL());
                    // Seems to be valid, store.
                    controller.saveConfig();
                } else {
                    LOG.log(Level.WARNING,
                        "Unable to load config from " + clr.getConfigURL());
                }
            }
            if (clr.isRestartRequired() && controller.isStarted()) {
                // PFC-2827:
                controller.schedule(new Runnable() {
                    public void run() {
                        controller.shutdownAndRequestRestart();
                    }
                }, 10000L);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to reload configuration: " + clr
                + ". " + e, e);
        }
    }

    /**
     * #2467: Set server URL via command line option in installer
     *
     * @param controller
     * @return
     */
    public static boolean loadAndMergeFromInstaller(Controller controller) {
        Path initFile = null;
        String windir = System.getenv("WINDIR");
        if (StringUtils.isNotBlank(windir)) {
            Path tempDir = Paths.get(windir).resolve("TEMP");
            initFile = tempDir.resolve(INITIAL_STARTUP_CONFIG_FILENAME);
        }
        if (initFile == null || Files.notExists(initFile)) {
            String tempStr = System.getProperty("java.io.tmpdir");
            initFile = Paths.get(tempStr).resolve(
                INITIAL_STARTUP_CONFIG_FILENAME);
            if (Files.notExists(initFile)) {
                return false;
            }
        }
        String url = "";
        boolean delete = false;
        try (InputStream in = Files.newInputStream(initFile)) {
            Properties props = new Properties();
            props.load(in);

            url = props.getProperty(ConfigurationEntry.CONFIG_URL
                .getConfigKey());
            if (StringUtils.isBlank(url)) {
                String fn = props
                    .getProperty(ConfigurationEntry.INSTALLER_FILENAME
                        .getConfigKey());
                if (StringUtils.isNotBlank(fn)) {
                    url = PathUtils.decodeURLFromFilename(fn);
                }
            }
            if (StringUtils.isBlank(url)) {
                return false;
            }
            Properties preConfig = loadPreConfiguration(url);
            if (preConfig == null) {
                return false;
            }
            int i = merge(preConfig, controller);
            LOG.info("Startup " + i + " with server " + url);
            if (i > 0) {
                ConfigurationEntry.CONFIG_URL.setValue(controller, url);
                controller.saveConfig();
            }
            delete = true;
            return true;
        } catch (Exception e) {
            LOG.warning("Unable to read configuration " + initFile + " / "
                + url + ". " + e);
        } finally {
            if (delete) {
                try {
                    Files.delete(initFile);
                } catch (IOException ioe) {
                    LOG.fine("Unable to deleted file " + initFile + ". " + ioe);
                }
            }
        }

        return false;
    }

    /**
     * #2179 Loads the the config URL from the command line interface and merges
     * it with the controllers config.
     *
     * @param controller
     * @return if a config was successfully loaded
     */
    public static boolean loadAndMergeCLI(Controller controller) {
        CommandLine cli = controller.getCommandLine();
        if (cli == null) {
            return false;
        }
        String configName = cli.getOptionValue("c");
        if (StringUtils.isNotBlank(configName)
            && (configName.startsWith("http:") || configName
                .startsWith("https:")))
        {
            return loadAndMergeURL(controller, configName);
        }
        return false;
    }

    /**
     * #2179 Loads the the config from the server and merges it with the
     * controllers config.
     *
     * @param controller
     * @return if a config was successfully loaded
     */
    public static boolean loadAndMergeConfigURL(Controller controller) {
        return loadAndMergeURL(controller, ConfigurationEntry.CONFIG_URL.getValue(controller));
    }

    /**
     * #2179 Loads the the config from the URL and merges it with the
     * controllers config.
     *
     * @param controller
     * @return if a config was successfully loaded
     */
    public static boolean loadAndMergeURL(Controller controller,
        String configURL)
    {
        Reject.ifNull(controller, "Controller is null");
        if (StringUtils.isBlank(configURL)) {
            return false;
        }
        String un = controller.getCLIUsername();
        char[] pw = Util.toCharArray(controller.getCLIPassword());
        try {
            Properties serverConfig = loadPreConfiguration(configURL, un, pw);
            boolean overWrite = overwriteConfigEntries(serverConfig);
            if (dropFolderSettings(serverConfig)) {
                Set<String> entryIds = FolderSettings.loadEntryIds(controller
                    .getConfig());
                for (String entryId : entryIds) {
                    FolderSettings.removeEntries(controller.getConfig(),
                        entryId);
                }
            }
            int i = merge(serverConfig, controller.getConfig(),
                controller.getPreferences(), overWrite);

            LOG.info("Loaded " + i + " profile settings (overwrite? "
                + overWrite + ") from: " + configURL);

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
        return loadPreConfiguration(server, null, null);
    }

    /**
     * Loads a pre-configuration from a server. Automatically adds HTTP:// and
     * url suffix.
     *
     * @param server
     * @return the loaded config.
     * @throws IOException
     */
    public static Properties loadPreConfiguration(String server, String un,
        char[] pw) throws IOException
    {
        Reject.ifBlank(server, "Server URL is blank");
        String finalURL = Util.removeLastSlashFromURI(server.replace(
            "download_client", ""));
        if (!finalURL.startsWith("http")) {
            finalURL = "http://" + finalURL;
        }
        if (!finalURL.endsWith(".config")
            && !finalURL.contains(DEFAULT_PROPERTIES_URI))
        {
            finalURL += DEFAULT_PROPERTIES_URI;
        }
        return loadPreConfiguration(new URL(finalURL.replace(" ", "%20")), un, pw);
    }

    /**
     * Loads a pre-configuration from the URL
     *
     * @param from
     *            the URL to load from
     * @return the loaded properties WITHOUT those in config.
     * @throws IOException
     */
    private static Properties loadPreConfiguration(URL from, String un,
        char[] pw) throws IOException
    {
        Reject.ifNull(from, "URL is null");
        URLConnection con = from.openConnection();
        if (StringUtils.isNotBlank(un)) {
            String s = un + ":" + Util.toString(pw);
            LoginUtil.clear(pw);
            String base64 = "Basic " + Base64.encodeBytes(s.getBytes("UTF-8"));
            con.setRequestProperty("Authorization", base64);
        }
        con.setConnectTimeout(1000 * URL_CONNECT_TIMEOUT_SECONDS);
        con.setReadTimeout(1000 * URL_CONNECT_TIMEOUT_SECONDS);
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

    public static int merge(Properties preConfig, Controller controller) {
        boolean overWrite = overwriteConfigEntries(preConfig);
        if (dropFolderSettings(preConfig)) {
            Set<String> entryIds = FolderSettings.loadEntryIds(controller
                .getConfig());
            for (String entryId : entryIds) {
                FolderSettings.removeEntries(controller.getConfig(), entryId);
            }
        }
        return merge(preConfig, controller.getConfig(),
            controller.getPreferences(), overWrite);
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
                Object oldValue = targetConfig.setProperty(key, value);
                if (!key.startsWith(PREFERENCES_PREFIX)
                    && !value.equals(oldValue))
                {
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
