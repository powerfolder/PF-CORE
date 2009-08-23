/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: SendMessageTask.java 9008 2009-08-13 12:56:12Z harry $
 */
package de.dal33t.powerfolder.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;

/**
 * Task that get executed when the server is connected.
 * 
 * @author sprajc
 */
public abstract class ServerRemoteCallTask extends PersistentTask {

    private static final Logger LOG = Logger
        .getLogger(ServerRemoteCallTask.class.getName());

    private static final long serialVersionUID = 100L;
    private transient ServerClientListener listener;

    public ServerRemoteCallTask(int daysToExpire) {
        super(daysToExpire);
    }

    @Override
    public void initialize() {
        if (!PreferencesEntry.USE_ONLINE_STORAGE
            .getValueBoolean(getController()))
        {
            // Skip. We don't use the online storage.
            remove();
            return;
        }
        if (isExpired()) {
            // Expired
            remove();
            return;
        }
        ServerClient client = getController().getOSClient();
        if (!checkAndExecute(client)) {
            // Queue for later executing
            listener = new MyServerClientListener();
            client.addListener(listener);
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (listener != null) {
            getController().getOSClient().removeListener(listener);
        }
    }

    /**
     * Execute a remote call to the service. When this method is called it is
     * guranteed that the server is connected
     * 
     * @param client
     * @throws Exception
     *             if something went wrong. Task will be KEPT for later
     *             execution re-try.
     */
    public abstract void executeRemoteCall(ServerClient client)
        throws Exception;

    // Internal ***************************************************************

    private boolean checkAndExecute(ServerClient client) {
        if (!client.isConnected()) {
            return false;
        }
        try {
            executeRemoteCall(client);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                "Exception while executing remote call. " + e, e);
            return false;
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
            checkAndExecute(event.getClient());
        }

        public void login(ServerClientEvent event) {
            checkAndExecute(event.getClient());
        }

        public void serverConnected(ServerClientEvent event) {
            checkAndExecute(event.getClient());
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }

}
