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
 * $Id$
 */
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Reject;

/**
 * UI Model for the Online Storage client.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClientModel extends PFUIComponent {
    private ServerClient client;

    public ServerClientModel(Controller controller, ServerClient client) {
        super(controller);

        Reject.ifNull(client, "Client is null");
        this.client = client;
    }

    public ServerClient getClient() {
        return client;
    }

    /**
     * Checks the current webservice account and opens the login wizard if
     * problem occurs.
     */
    public void checkAndSetupAccount() {
        if (Boolean.FALSE.equals(
                PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(
                        getController()))) {
            return;
        }
        if (client.isLoggedIn()) {
            return;
        }
        if (!client.isLastLoginKnown()) {
            PFWizard.openLoginWizard(getController(), client);
            return;
        }

        SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() {
                return client.loginWithLastKnown().isValid();
            }

            @Override
            public void finished() {
                if (get() == null || !(Boolean) get()) {
                    PFWizard.openLoginWizard(getController(), client);
                }
            }
        };
        worker.start();
    }
}
