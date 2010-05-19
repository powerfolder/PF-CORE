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
import java.util.Properties;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.skin.SnowlandBasic;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.update.Updater.UpdateSetting;

public class PowerFolderBasic extends AbstractDistribution {

    public String getName() {
        return "PowerFolder Basic";
    }

    public String getBinaryName() {
        return "PowerFolder";
    }

    public void init(Controller controller) {
        super.init(controller);
        
        // #2005: Rollback
        Feature.NET_USE_POWERFOLDER_RELAY.enable();

        // Reset network ID to default in default distribution.
        // Separating networks should only be available with Server/Client
        // distribution
        resetNetworkID(controller);

        // Reset Provider URLs to PowerFolder.com in default distribution
        resetProviderURLs(controller);

        // Reset primary server if not PowerFolder server
        if (!isPowerFolderServer(controller)
            || PowerFolderBeta.isBetaServer(controller))
        {
            resetServer(controller);
        }

        // Switch to basic skin
        // String skinName =
        // PreferencesEntry.SKIN_NAME.getValueString(controller);
        // if (skinName.equals(PreferencesEntry.SKIN_NAME.getDefaultValue())) {
        PreferencesEntry.SKIN_NAME.setValue(controller, SnowlandBasic.NAME);
        // }

        // Load different Provider URLs
        try {
            Properties preConfig = ConfigurationLoader
                .loadPreConfigFromClasspath("config/Basic.config");
            ConfigurationLoader.mergeConfigs(preConfig, controller.getConfig(),
                true);
            logInfo("Loaded preconfiguration file config/Basic.config from jar file");
        } catch (IOException e) {
            logSevere("Error while loading config/Basic.config from jar file",
                e);
        }
    }

    public UpdateSetting createUpdateSettings() {
        // Use standard URLs
        UpdateSetting settings = new UpdateSetting();
        settings.versionCheckURL = "http://checkversion.powerfolder.com/PowerFolder_LatestVersion.txt";
        settings.downloadLinkInfoURL = "http://checkversion.powerfolder.com/PowerFolder_DownloadLocation.txt";
        settings.releaseExeURL = "http://download.powerfolder.com/free/PowerFolder_Latest_Win32_Installer.exe";
        return settings;
    }

    public boolean allowSkinChange() {
        return false;
    }

    @Override
    public boolean showCredentials() {
        return true;
    }

    @Override
    public boolean showClientPromo() {
        return true;
    }

}