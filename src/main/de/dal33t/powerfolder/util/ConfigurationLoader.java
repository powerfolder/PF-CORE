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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
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
     * Loads a pre-configuration from the URL and sets/replaces the values in
     * the given config.
     * 
     * @param from
     *            the URL to load from
     * @param config
     *            the config file to set the pre-configuration values into.
     * @return true if succeeded, otherwise false.
     */
    public static boolean loadPreConfiguration(URL from, Properties config) {
        try {
            loadPreConfiguration(from.openStream(), config, true);
            return true;
        } catch (IOException e) {
            LOG.warning("Unable to load pre configuration from " + from + ": "
                + e);
            LOG.log(Level.FINER, e.toString(), e);
            return false;
        }
    }

    /**
     * Loads a configuration file from the given input stream and sets these in
     * the given config. Closed the given input stream after reading.
     * 
     * @param in
     *            the input stream to read the pre-config from
     * @param config
     *            the config file to set the pre-configuration values into.
     * @param replaceExisting
     *            if existing keys in the config should be replaced by values of
     *            the pre-config.
     * @return true if succeeded, otherwise false.
     */
    public static boolean loadPreConfiguration(InputStream in,
        Properties config, boolean replaceExisting)
    {
        Reject.ifNull(in,
            "Unable to load Preconfiguration. Input stream is null");
        try {
            Properties preConfig = new Properties();
            preConfig.load(in);
            for (Object obj : preConfig.keySet()) {
                String key = (String) obj;
                String value = preConfig.getProperty(key);
                if (!config.containsKey(key) || replaceExisting) {
                    config.setProperty(key, value);
                    LOG.warning("Preconfigured " + key + "=" + value);
                }
            }
            LOG.fine("Preconfigs found " + preConfig.size());
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to load Preconfiguration", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        return false;
    }
}
