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

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.util.Reject;

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
    /**
     * The issuer of the remote call.
     */
    private final AccountInfo issuer;

    /**
     * @param issuer
     *            the issuer of the task. The same user must be logged in to the
     *            server for a successful executing. Otherwise task will be
     *            scheduled until the original issuer re-logs in.
     * @param daysToExpire
     *            day to expire/remove if not successfully executed.
     */
    protected ServerRemoteCallTask(AccountInfo issuer, int daysToExpire) {
        super(daysToExpire);
        Reject.ifNull(issuer, "Account issuer");
        this.issuer = issuer;
    }

    /**
     * It does not matter what user/account executes this remote call.
     *
     * @param daysToExpire
     *            day to expire/remove if not successfully executed.
     */
    protected ServerRemoteCallTask(int daysToExpire) {
        super(daysToExpire);
        this.issuer = null;
    }

    /**
     * @return the issuer of this task. if set task will only execute if the
     *         client is logged in with the same account at the server.
     */
    protected AccountInfo getIssuer() {
        return issuer;
    }

    @Override
    public void initialize() {
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
     * guranteed that the server is connected. The call is responsible for
     * removing the task via {@link #remove()}.
     *
     * @param client
     * @throws Exception
     *             if something went wrong. Task will be KEPT for later
     *             execution re-try.
     */
    protected abstract void executeRemoteCall(ServerClient client)
        throws Exception;

    // Internal ***************************************************************

    private boolean checkAndExecute(ServerClient client) {
        if (!client.isConnected()) {
            return false;
        }
        if (issuer != null) {
            // Check if issuer matches the currently logged in user.
            AccountInfo currentLogin = client.getAccountInfo();
            if (currentLogin == null) {
                return false;
            }
            if (!issuer.equals(currentLogin)) {
                // Mismatch, don't do it.
                return false;
            }
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

        public void nodeServerStatusChanged(ServerClientEvent event) {
            checkAndExecute(event.getClient());
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

}
