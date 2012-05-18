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
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.net.RelayFinder;
import de.dal33t.powerfolder.skin.Origin;
import de.dal33t.powerfolder.ui.LookAndFeelSupport;
import de.dal33t.powerfolder.ui.dialog.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.util.ConfigurationLoader;

public class PowerFolderPro extends AbstractDistribution {

    public String getName() {
        return "PowerFolder Pro";
    }

    public String getBinaryName() {
        return "PowerFolder";
    }

    public void init(Controller controller) {
        super.init(controller);

        loadPreConfigFromClasspath(getController(), null);

        // #2467: Get server URL from the installer
        ConfigurationLoader.loadAndMergeFromInstaller(controller);

        boolean prompt = ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM
            .getValueBoolean(getController());
        if (prompt && isPowerFolderServer(controller)
            && controller.isUIEnabled())
        {
            try {
                LookAndFeelSupport
                    .setLookAndFeel(new Origin().getLookAndFeel());
            } catch (Exception e) {
                logSevere("Failed to set look and feel", e);
            }
            // Configuration required
            new ConfigurationLoaderDialog(controller).openAndWait();
        }
    }

    public RelayFinder createRelayFinder() {
        return new PublicRelayFinder();
    }

    public boolean allowSkinChange() {
        return true;
    }

    private class PublicRelayFinder implements RelayFinder {
        private static final String RELAY_1ST_CHOICE_ID = "WEBSERVICE005";
        private static final String RELAY_2ST_CHOICE_ID = "WEBSERVICE006";

        public Member findRelay(NodeManager nodeManager) {
            Member relay = nodeManager.getNode(RELAY_1ST_CHOICE_ID);
            Member server = getController().getOSClient().getServer();
            if (relay == null || server.equals(relay)) {
                relay = nodeManager.getNode(RELAY_2ST_CHOICE_ID);
            }
            if (relay == null) {
                relay = server;
                logFine("Using default server as relay: " + relay);
            } else {
                logFiner("Using relay: " + relay);
            }
            return relay;
        }
    }
}