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
import de.dal33t.powerfolder.util.Updater.UpdateSetting;

public class DefaultDistribution extends AbstractDistribution {

    public String getName() {
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
        resetServer(controller);
    }

    public boolean supportsWebRegistration() {
        return true;
    }

    public boolean allowUserToSelectServer() {
        // Don't allow the user to change the server.
        return false;
    }

    public UpdateSetting createUpdateSettings() {
        return null;
    }

    // Internal ***************************************************************

    private void resetServer(Controller c) {
        if (isPowerFolderServer(c)) {
            return;
        }
        logInfo("Resetting server connection to "
            + ConfigurationEntry.SERVER_HOST.getDefaultValue());
        setDefaultValue(c, ConfigurationEntry.SERVER_NAME);
        setDefaultValue(c, ConfigurationEntry.SERVER_WEB_URL);
        setDefaultValue(c, ConfigurationEntry.SERVER_NODEID);
        setDefaultValue(c, ConfigurationEntry.SERVER_HOST);
    }

    private static boolean isPowerFolderServer(Controller c) {
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

    private static void resetNetworkID(Controller c) {
        setDefaultValue(c, ConfigurationEntry.NETWORK_ID);
    }

    private static void resetProviderURLs(Controller c) {
        setDefaultValue(c, ConfigurationEntry.PROVIDER_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_ABOUT_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_QUICKSTART_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_SUPPORT_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_BUY_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_CONTACT_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_WIKI_URL);
        setDefaultValue(c, ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL);
    }

    private static void setDefaultValue(Controller c, ConfigurationEntry entry)
    {
        entry.setValue(c, entry.getDefaultValue());
    }
}