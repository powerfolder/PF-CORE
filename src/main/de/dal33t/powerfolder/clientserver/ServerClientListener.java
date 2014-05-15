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
 * $Id: ServerClient.java 4375 2008-06-23 01:12:51Z tot $
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.CoreListener;

public interface ServerClientListener extends CoreListener {
    /**
     * When a new login attempt has been performed. Covers failed and successful
     * logins.
     *
     * @param event
     */
    void login(ServerClientEvent event);

    /**
     * When the account has changed on the server.
     * <p>
     * TODO Implement
     *
     * @param event
     */
    void accountUpdated(ServerClientEvent event);

    /**
     * When the server is fully connected
     *
     * @param event
     */
    void serverConnected(ServerClientEvent event);

    /**
     * When the server disconnects.
     *
     * @param event
     */
    void serverDisconnected(ServerClientEvent event);

    /**
     * When a Member becomes a server.
     *
     * @param event
     * @see Member#setServer(boolean)
     */
    void nodeServerStatusChanged(ServerClientEvent event);
}
