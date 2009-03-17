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
        ConfigurationEntry.NETWORK_ID.setValue(controller,
            ConfigurationEntry.NETWORK_ID.getDefaultValue());
        
        // Reset Provider URLs to PowerFolder.com in default distribution
        ConfigurationEntry.PROVIDER_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_ABOUT_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_ABOUT_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_QUICKSTART_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_QUICKSTART_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_SUPPORT_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_SUPPORT_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL.setValue(
            controller, ConfigurationEntry.PROVIDER_SUPPORT_FILE_TICKET_URL
                .getDefaultValue());
        ConfigurationEntry.PROVIDER_BUY_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_BUY_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_CONTACT_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_CONTACT_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_WIKI_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_WIKI_URL.getDefaultValue());
        ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL.setValue(controller,
            ConfigurationEntry.PROVIDER_HTTP_TUNNEL_RPC_URL.getDefaultValue());
    }

    public boolean supportsWebRegistration() {
        return true;
    }

    public UpdateSetting createUpdateSettings() {
        return null;
    }
}
