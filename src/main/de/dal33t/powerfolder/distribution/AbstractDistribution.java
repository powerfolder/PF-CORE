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
import de.dal33t.powerfolder.net.RelayFinder;
import de.dal33t.powerfolder.net.RelayedConnectionManager.ServerIsRelayFinder;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.Reject;
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
    private static final String[] POWERFOLDER_DISTRIBUTIONS = {
        PowerFolderPro.class.getName(), PowerFolderBasic.class.getName(),
        "de.dal33t.powerfolder.distribution.PowerFolderGeneric"};
    private static final String DEFAULT_CONFIG_FILENAME = "Default.config";

    private Controller controller;

    public boolean showCredentials() {
        return false;
    }

    public void init(Controller controller) {
        this.controller = controller;
    }

    public RelayFinder createRelayFinder() {
        return new ServerIsRelayFinder();
    }

    protected Controller getController() {
        return controller;
    }

    protected boolean addTranslation(String language) {
        // Load texts
        String translationFile = "Translation_" + language + ".properties";
        if (Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(translationFile) != null)
        {
            Locale l = new Locale(language);
            Translation.addSupportedLocales(l);
            // Do not automatically set the locale:
            // Translation.saveLocalSetting(l);
            Translation.resetResourceBundle();
            logInfo("Translation file loaded: " + translationFile);
            return true;
        }
        return false;
    }

    /**
     * @return true if this client is a branded client (non PowerFolder)
     */
    public boolean isBrandedClient() {
        for (String className : POWERFOLDER_DISTRIBUTIONS) {
            if (className.equals(getClass().getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Loads and merges the default config file (Default.config) from classpath
     * and merges it with the existing config and preferences.
     * 
     * @param controller
     * @param replaceExisting
     *            true to replace all values, false to preserve all values of
     *            the existing config/prefs, null check the
     *            {@link ConfigurationEntry#CONFIG_OVERWRITE_VALUES} in the
     *            loaded config.
     * @return
     */
    protected boolean loadPreConfigFromClasspath(Controller controller,
        Boolean replaceExisting)
    {
        Reject.ifNull(controller, "Controller");
        try {
            Properties preConfig = ConfigurationLoader
                .loadPreConfigFromClasspath(DEFAULT_CONFIG_FILENAME);

            boolean overWrite;
            if (replaceExisting != null) {
                overWrite = replaceExisting;
            } else {
                overWrite = ConfigurationLoader
                    .overwriteConfigEntries(preConfig);
            }
            int n = ConfigurationLoader.merge(preConfig,
                controller.getConfig(), controller.getPreferences(), overWrite);
            logFine("Loaded " + n + " preconfiguration file "
                + DEFAULT_CONFIG_FILENAME + " from jar file");
            return true;
        } catch (IOException e) {
            logSevere("Error while loading " + DEFAULT_CONFIG_FILENAME
                + " from jar file", e);
            return false;
        }
    }

    protected boolean loadPreConfigFromClasspath(Controller controller) {
        return loadPreConfigFromClasspath(controller, true);
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
     * @param myDir
     *            e.g. "MySierraCloud"
     */
    protected static final void setFoldersBaseDirName(Controller c, String myDir)
    {
        if (!ConfigurationEntry.FOLDER_BASEDIR.hasValue(c)) {
            String folderBaseDir = ConfigurationEntry.FOLDER_BASEDIR
                .getDefaultValue().replace(
                    Constants.FOLDERS_BASE_DIR_SUBDIR_NAME, myDir);
            ConfigurationEntry.FOLDER_BASEDIR.setValue(c, folderBaseDir);
        } else {
            Constants.FOLDERS_BASE_DIR_SUBDIR_NAME = myDir;
        }
    }
}
