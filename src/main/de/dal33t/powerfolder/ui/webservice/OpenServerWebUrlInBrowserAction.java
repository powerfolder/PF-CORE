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
package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.BrowserLauncher;


public class OpenServerWebUrlInBrowserAction extends BaseAction {

    protected OpenServerWebUrlInBrowserAction(Controller controller) {
        super("openwebservice", controller);
        updateState();
        getController().getOSClient().addListener(new MyServerClientListener());
    }

    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher.openURL(getController().getOSClient().getWebURL());
        } catch (IOException e1) {
            Logger.getAnonymousLogger().log(Level.SEVERE, e.toString(), e);
        }
    }

    private void updateState() {
        setEnabled(getController().getOSClient().hasWebURL()
            && Feature.SERVER_INTERNAL_FUNCTIONS.isEnabled());
    }

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            updateState();
        }

        public void login(ServerClientEvent event) {
            updateState();
        }

        public void serverConnected(ServerClientEvent event) {
            updateState();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateState();
        }

    }
}
