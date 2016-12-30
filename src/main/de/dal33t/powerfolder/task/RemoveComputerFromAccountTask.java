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

import java.util.logging.Logger;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Task to remote a computer from the computers list of a account.
 *
 * @author sprajc
 */
public final class RemoveComputerFromAccountTask extends ServerRemoteCallTask {
    private static final long serialVersionUID = 100L;
    private static final Logger LOG = Logger
        .getLogger(RemoveComputerFromAccountTask.class.getName());

    private final MemberInfo node;

    public RemoveComputerFromAccountTask(AccountInfo aInfo, MemberInfo node) {
        super(aInfo, DEFAULT_DAYS_TO_EXIPRE);
        Reject.ifNull(node, "Node");
        this.node = node;
    }

    @Override
    public boolean executeRemoteCall(ServerClient client) {
        LOG.fine("Removing computer from account: " + node);
        client.getAccountService().removeComputer(node);
        // Remove task
        remove();
        return true;
    }
}