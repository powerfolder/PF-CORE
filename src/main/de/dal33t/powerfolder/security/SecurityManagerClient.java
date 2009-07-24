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
 * $Id: Folder.java 8681 2009-07-19 00:07:45Z tot $
 */
package de.dal33t.powerfolder.security;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.RemoteCallException;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * The security manager for the client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SecurityManagerClient extends AbstractSecurityManager {
    private ServerClient client;
    private Map<Member, AccountInfo> sessions;

    public SecurityManagerClient(Controller controller, ServerClient client) {
        super(controller);
        Reject.ifNull(client, "Client is null");
        this.client = client;
        this.sessions = new ConcurrentHashMap<Member, AccountInfo>();
    }

    public Account authenticate(String username, String password) {
        return client.login(username, password);
    }

    public boolean hasPermission(AccountInfo info, Permission permission) {
        Reject.ifNull(info, "Account info is null");
        Reject.ifNull(permission, "Permission info is null");
        if (!Feature.SECURITY_CHECKS.isEnabled()) {
            return true;
        }

        boolean hasPermission = client.getAccountService().hasPermission(
            info.getOID(), permission);
        // TODO Cache permissions
        logWarning(info + " has " + (hasPermission ? "" : "NOT ") + permission);
        return hasPermission;
    }

    public AccountInfo getAccountInfo(Member node) {
        // Use cache
        return getAccountInfo(node, false);
    }

    public AccountInfo getAccountInfo(Member node, boolean forceRefresh) {
        AccountInfo aInfo = sessions.get(node);
        // Cache
        if (aInfo == null || forceRefresh) {
            try {
                Map<MemberInfo, AccountInfo> res = client.getAccountService()
                    .getAccountInfos(Collections.singleton(node.getInfo()));
                aInfo = res.get(node.getInfo());
            } catch (RemoteCallException e) {
                logSevere("Unable to retrieve account info for " + node + ". "
                    + e);
                logFiner(e);
            }
            logWarning("Retrieved account " + aInfo + " for " + node);
            if (aInfo != null) {
                sessions.put(node, aInfo);
            }
        }
        return aInfo;
    }

    public void nodeAccountStateChanged(final Member node) {
        Runnable refresher = null;
        if (node.isMySelf()) {
            refresher = new Runnable() {
                public void run() {
                    client.refreshAccountDetails();
                    sessions.remove(node);
                    // Broadcast this information to all connected nodes.
                    // getController().getNodeManager().broadcastMessage(
                    // new AccountStateChanged(getController().getMySelf()
                    // .getInfo()));
                    // And now sync folder memberhips.
                    node.synchronizeFolderMemberships();
                }
            };
        } else if (!node.isCompleteyConnected()) {
            sessions.remove(node);
        } else {
            refresher = new Runnable() {
                public void run() {
                    // Refresh account info on that node.
                    getAccountInfo(node, true);
                    // This is required because of probably changed access
                    // permissions to folder.
                    node.synchronizeFolderMemberships();
                }
            };
        }
        if (refresher != null) {
            getController().getThreadPool().schedule(refresher, 0,
                TimeUnit.SECONDS);
        }
    }

    @Override
    protected FolderPermission retrieveDefaultPermission(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "FolderInfo is null");
        try {
            return getController().getOSClient().getFolderService()
                .getDefaultPermission(foInfo);
        } catch (Exception e) {
            logWarning(
                "Unable to retrieve default permission from server. Using admin as fallback. "
                    + e, e);
            return new FolderAdminPermission(foInfo);
        }
    }
}
