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
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Helper class around configuration
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class ConfigurationLoader {
    private static Logger LOG = Logger.getLogger(ConfigurationLoader.class
        .getName());

    private ConfigurationLoader() {
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
        InputStream in = from.openStream();
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
    public static int mergeConfigs(Properties preConfig,
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
                n++;
                LOG.finer("Preconfigured " + key + "=" + value);
            }
        }
        LOG.fine("Preconfigs found " + preConfig.size());
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
