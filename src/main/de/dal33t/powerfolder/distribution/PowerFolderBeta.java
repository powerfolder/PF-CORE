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

public class PowerFolderBeta extends AbstractDistribution {
    static final String BETA_SERVER_HOST = "relay001.node.powerfolder.com";

    public String getName() {
        return "4.0 Beta";
    }

    public String getBinaryName() {
        return "PowerFolder";
    }

    public void init(Controller controller) {
        super.init(controller);
        // Reset network ID to default in default distribution.
        // Separating networks should only be available with Server/Client
        // distribution
        resetNetworkID(controller);

        // Reset Provider URLs to PowerFolder.com in default distribution
        resetProviderURLs(controller);

        // Reset primary server if not PowerFolder server
        // if (!isPowerFolderServer(controller)) {
        resetToBetaServer(controller);
        // }
    }

    public static boolean isBetaServer(Controller c) {
        String host = ConfigurationEntry.SERVER_HOST.getValue(c);
        return host.contains(BETA_SERVER_HOST);
    }

    public boolean allowSkinChange() {
        return true;
    }

    // Internal ***************************************************************

    private void resetToBetaServer(Controller c) {
        logInfo("Setting beta server connect");
        ConfigurationEntry.SERVER_HOST.setValue(c,
            "relay001.node.powerfolder.com");
        ConfigurationEntry.SERVER_NODEID.setValue(c, "RELAY001");
        ConfigurationEntry.SERVER_NAME.setValue(c, "Online Storage Beta");
        ConfigurationEntry.SERVER_WEB_URL.setValue(c,
            "https://access.powerfolder.com/node/beta");
    }

}