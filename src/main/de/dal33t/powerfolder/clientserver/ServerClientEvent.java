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

import java.util.EventObject;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;

@SuppressWarnings("serial")
public class ServerClientEvent extends EventObject {
    private Member node;
    private AccountDetails accountDetails;
    private boolean loginSuccess;

    public ServerClientEvent(ServerClient source) {
        super(source);
    }

    public ServerClientEvent(ServerClient source, AccountDetails details) {
        this(source, details, true);
    }

    public ServerClientEvent(ServerClient source, AccountDetails details,
        boolean loginSuccess)
    {
        super(source);
        this.accountDetails = details;
        this.loginSuccess = loginSuccess;
    }

    public ServerClientEvent(ServerClient source, Member node) {
        super(source);
        this.node = node;
    }

    public ServerClient getServerClient() {
        return (ServerClient) getSource();
    }

    public AccountDetails getAccountDetails() {
        return accountDetails;
    }

    public Member getServerNode() {
        return node;
    }

    public boolean isLoginSuccess() {
        return loginSuccess;
    }

    public ServerClient getClient() {
        return (ServerClient) getSource();
    }
}
