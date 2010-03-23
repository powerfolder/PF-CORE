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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.skin.Snowland;
import de.dal33t.powerfolder.skin.SnowlandBasic;
import de.dal33t.powerfolder.util.update.Updater.UpdateSetting;

public class PowerFolderPro extends AbstractDistribution {

    public String getName() {
        return "PowerFolder Pro";
    }

    public String getBinaryName() {
        return "PowerFolder";
    }

    public void init(Controller controller) {
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
        // Switch to non-basic skin
        String skinName = PreferencesEntry.SKIN_NAME.getValueString(controller);
        if (skinName.equals(SnowlandBasic.NAME)) {
            PreferencesEntry.SKIN_NAME.setValue(controller, Snowland.NAME);
        }
    }

    public UpdateSetting createUpdateSettings() {
        // Pro URLs
        UpdateSetting settings = new UpdateSetting();
        settings.versionCheckURL = "http://checkversion.powerfolder.com/PowerFolderPro_LatestVersion.txt";
        settings.downloadLinkInfoURL = "http://checkversion.powerfolder.com/PowerFolderPro_DownloadLocation.txt";
        settings.releaseExeURL = "http://download.powerfolder.com/pro/win/PowerFolder_Latest_Win32_Installer.exe";
        return settings;
    }

    public boolean isRelay(Member node) {
        // Our public network strategy. Not very smart.
        return node.getId().contains("RELAY");
    }

    public boolean allowSkinChange() {
        return true;
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