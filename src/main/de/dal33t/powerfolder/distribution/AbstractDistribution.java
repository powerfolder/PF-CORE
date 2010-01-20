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
import de.dal33t.powerfolder.Constants;
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
     * @return true if the credentials in the about box should be shown.
     */
    public boolean showCredentials() {
        return true;
    }

    /**
     * @param c
     * @return true if the set server is part of the public PowerFolder network
     *         (non inhouse server).
     */
    public static boolean isPowerFolderServer(Controller c) {
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

    protected boolean loadTranslation(String customTranslationId,
        String language)
    {
        // Load texts
        String translationFile = "Translation_" + language + "_"
            + customTranslationId + ".properties";
        if (Thread.currentThread().getContextClassLoader().getResourceAsStream(
            translationFile) != null)
        {
            Locale l = new Locale(language, customTranslationId);
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
                .loadPreConfigFromClasspath("Client.config");
            ConfigurationLoader.mergeConfigs(preConfig, config, true);
            logInfo("Loaded preconfiguration file Client.config from jar file");
            return true;
        } catch (IOException e) {
            logSevere("Error while loading Client.config from jar file", e);
            return false;
        }
    }

    protected static final void removeValue(Controller c,
        ConfigurationEntry entry)
    {
        // Change back to default
        entry.removeValue(c);
    }

    protected static final void resetServer(Controller c) {
        removeValue(c, ConfigurationEntry.SERVER_NAME);
        removeValue(c, ConfigurationEntry.SERVER_WEB_URL);
        removeValue(c, ConfigurationEntry.SERVER_NODEID);
        removeValue(c, ConfigurationEntry.SERVER_HOST);
        removeValue(c, ConfigurationEntry.SERVER_HTTP_TUNNEL_RPC_URL);
    }

    protected static final void resetNetworkID(Controller c) {
        removeValue(c, ConfigurationEntry.NETWORK_ID);
    }

    protected static final void resetProviderURLs(Controller c) {
        removeValue(c, ConfigurationEntry.PROVIDER_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_ABOUT_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_QUICKSTART_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_SUPPORT_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_BUY_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_CONTACT_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_WIKI_URL);
    }

    protected static final void setAppName(String name) {
        Translation.setPlaceHolder("APPNAME", name);
    }

    protected static final void setAppDescription(String description) {
        Translation.setPlaceHolder("APPDESCRIPTION", description);
    }

    /**
     * Sets the folder basedir subdir name if not already set in config. e.g.
     * C:\Users\sprajc\myDir.
     * 
     * @param c
     * @param replacement
     */
    protected static final void setFoldersBaseDirName(Controller c, String myDir)
    {
        if (!ConfigurationEntry.FOLDER_BASEDIR.hasValue(c)) {
            String folderBaseDir = ConfigurationEntry.FOLDER_BASEDIR
                .getDefaultValue().replace(
                    Constants.FOLDERS_BASE_DIR_SUBDIR_NAME, myDir);
            ConfigurationEntry.FOLDER_BASEDIR.setValue(c, folderBaseDir);
        }
    }
}
