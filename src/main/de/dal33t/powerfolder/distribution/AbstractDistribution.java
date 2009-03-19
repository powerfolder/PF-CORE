/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: ServerClient.java 6435 2009-01-21 23:35:04Z tot $
 */
package de.dal33t.powerfolder.distribution;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Offer various helper methods for branding
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public abstract class AbstractDistribution extends Loggable implements
    Distribution
{
    /**
     * @param c
     * @return true if the set server is part of the public PowerFolder network
     *         (non inhouse server).
     */
    protected static boolean isPowerFolderServer(Controller c) {
        String host = ConfigurationEntry.SERVER_HOST.getValue(c);
        if (host != null) {
            if (host.toLowerCase().contains("powerfolder.com")) {
                return true;
            }
        }
        String nodeId = ConfigurationEntry.SERVER_NODEID.getValue(c);
        if (nodeId != null) {
            if (nodeId.toLowerCase().contains("WEBSERVICE")) {
                return true;
            }
        }
        return false;
    }

    protected boolean loadTranslation(String customTranslationId) {
        // Load texts
        String translationFile = "Translation_en_" + customTranslationId
            + ".properties";
        if (Thread.currentThread().getContextClassLoader().getResourceAsStream(
            translationFile) != null)
        {
            Locale l = new Locale("en", customTranslationId);
            Translation.saveLocalSetting(l);
            Translation.resetResourceBundle();
            logInfo("Translation file loaded: " + translationFile);
            return true;
        }
        return false;
    }

    protected boolean loadPreConfigFromClasspath(Properties config) {
        try {
            Properties preConfig = ConfigurationLoader
                .loadPreConfigFromClasspath("Client.properties");
            ConfigurationLoader.mergeConfigs(preConfig, config, true);
            logInfo(
                "Loaded preconfiguration file Client.config from jar file");
            return true;
        } catch (IOException e) {
            logSevere("Error while loading Client.config from jar file");
            return false;
        }
    }
}
