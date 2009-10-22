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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.util.update.Updater.UpdateSetting;

public class PowerFolderBeta extends AbstractDistribution {
    static final String BETA_SERVER_HOST = "relay001.node.powerfolder.com";

    public String getName() {
        return "4.0 Beta";
    }

    public void init(Controller controller) {
        // Reset network ID to default in default distribution.
        // Separating networks should only be available with Server/Client
        // distribution
        resetNetworkID(controller);

        // Reset Provider URLs to PowerFolder.com in default distribution
        resetProviderURLs(controller);

        // Reset primary server if not PowerFolder server
        // if (!isPowerFolderServer(controller)) {
        resetServer(controller);
        // }
    }

    public UpdateSetting createUpdateSettings() {
        return null;
    }

    public boolean isRelay(Member node) {
        // Our public network strategy. Not very smart.
        return node.getId().contains("RELAY");
    }

    public static boolean isBetaServer(Controller c) {
        String host = ConfigurationEntry.SERVER_HOST.getValue(c);
        return host.contains(BETA_SERVER_HOST);
    }

    // Internal ***************************************************************

    private void resetServer(Controller c) {
        logInfo("Setting beta server connect");
        ConfigurationEntry.SERVER_HOST.setValue(c,
            "relay001.node.powerfolder.com");
        ConfigurationEntry.SERVER_NODEID.setValue(c, "RELAY001");
        ConfigurationEntry.SERVER_NAME.setValue(c, "Online Storage Beta");
        ConfigurationEntry.SERVER_WEB_URL.setValue(c,
            "https://access.powerfolder.com/node/beta");
    }

    private static void resetNetworkID(Controller c) {
        removeValue(c, ConfigurationEntry.NETWORK_ID);
    }

    private static void resetProviderURLs(Controller c) {
        removeValue(c, ConfigurationEntry.PROVIDER_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_ABOUT_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_QUICKSTART_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_SUPPORT_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_BUY_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_CONTACT_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_WIKI_URL);
        removeValue(c, ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL);
    }

    private static void removeValue(Controller c, ConfigurationEntry entry) {
        if (!entry.getValue(c).equals(entry.getDefaultValue())) {
            // Change back to default
            entry.removeValue(c);
        }
    }
}