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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.RemoteCallException;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * The security manager for the client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SecurityManagerClient extends AbstractSecurityManager {

    private static final boolean CACHE_ENABLED = true;

    private static final AccountInfo NULL_ACCOUNT = new AccountInfo(null, null)
    {
        public String toString() {
            return "Anonymous";
        }
    };

    private ServerClient client;
    private Map<Member, Session> sessions;
    private Map<AccountInfo, PermissionsCacheSegment> permissionsCacheAccounts;

    public SecurityManagerClient(Controller controller, ServerClient client) {
        super(controller);
        Reject.ifNull(client, "Client is null");
        this.client = client;
        this.sessions = new ConcurrentHashMap<Member, Session>();
        this.permissionsCacheAccounts = new ConcurrentHashMap<AccountInfo, PermissionsCacheSegment>();
    }

    public Account authenticate(String username, String password) {
        return client.login(username, password);
    }

    public boolean hasPermission(AccountInfo accountInfo, Permission permission)
    {
        if (!Feature.SECURITY_CHECKS.isEnabled()) {
            return true;
        }
        try {
            Boolean hasPermission;
            PermissionsCacheSegment cache = permissionsCacheAccounts
                .get(nullSafeGet(accountInfo));
            if (cache != null) {
                hasPermission = cache.hasPermission(permission);
            } else {
                // Create cache
                hasPermission = null;
                cache = new PermissionsCacheSegment();
                permissionsCacheAccounts.put(nullSafeGet(accountInfo), cache);
            }
            String source;
            if (!CACHE_ENABLED) {
                hasPermission = null;
            }
            if (hasPermission == null) {
                if (client.isConnected()) {
                    hasPermission = Boolean.valueOf(client.getSecurityService()
                        .hasPermission(accountInfo, permission));
                    cache.set(permission, hasPermission);
                    source = "recvd";
                } else {
                    // TODO How to handle server disconnect?
                    hasPermission = true;
                    source = "nocon";
                }
            } else {
                source = "cache";
            }
            if (isFine()) {
                logFine("(" + source + ") " + nullSafeGet(accountInfo)
                    + " has " + (hasPermission ? "" : "NOT ") + permission);
            }
            return hasPermission;
        } catch (RemoteCallException e) {
            logWarning("Unable to check permission for "
                + nullSafeGet(accountInfo) + ". " + e);
            logFiner(e);
            return false;
        }
    }

    /**
     * Gets the {@link AccountInfo} for the given node. Retrieves it from server
     * if necessary. NEVER refreshes from server when running in EDT thread.
     * 
     * @see de.dal33t.powerfolder.security.SecurityManager#getAccountInfo(de.dal33t.powerfolder.Member)
     */
    public AccountInfo getAccountInfo(Member node) {
        Session session = sessions.get(node);
        // Cache hit
        if (session != null && CACHE_ENABLED) {
            if (isFiner()) {
                logFiner("Retured cached account for " + node + " : "
                    + session.getAccountInfo());
            }
            return session.getAccountInfo();
        }
        // Smells like hack
        if (UIUtil.isAWTAvailable() && EventQueue.isDispatchThread()) {
            if (isFiner()) {
                logFiner("Not trying to refresh account of " + node
                    + ". Running in EDT thread");
            }
            return null;
        }
        if (!client.isConnected()) {
            // Not available yet.
            return null;
        }
        AccountInfo aInfo;
        try {
            Map<MemberInfo, AccountInfo> res = client.getSecurityService()
                .getAccountInfos(Collections.singleton(node.getInfo()));
            aInfo = res.get(node.getInfo());
            if (isFiner()) {
                logFiner("Retrieved account " + aInfo + " for " + node);
            }
            if (CACHE_ENABLED) {
                sessions.put(node, new Session(aInfo));
            }
        } catch (RemoteCallException e) {
            logSevere("Unable to retrieve account info for " + node + ". " + e);
            logFiner(e);
            aInfo = null;
        }
        return aInfo;
    }

    public void nodeAccountStateChanged(final Member node) {
        Runnable refresher = null;
        if (node.isMySelf()) {
            refresher = new MySelfRefrehser(node);
        } else if (!node.isCompletelyConnected()) {
            refresher = new DisconnectRefresher(node);
        } else {
            refresher = new DefaultRefresher(node);
        }
        if (refresher != null && getController().isStarted()) {
            getController().getThreadPool().schedule(refresher, 0,
                TimeUnit.SECONDS);
        }
    }

    public void fetchAccountInfos(Collection<Member> nodes, boolean forceRefresh)
    {
        Reject.ifNull(nodes, "Nodes is null");
        try {
            if (!client.isConnected()) {
                return;
            }
            Collection<MemberInfo> reqNodes = new ArrayList<MemberInfo>(nodes
                .size());
            for (Member node : nodes) {
                if (forceRefresh || !sessions.containsKey(node)) {
                    reqNodes.add(node.getInfo());
                }
            }
            if (reqNodes.isEmpty()) {
                return;
            }
            Map<MemberInfo, AccountInfo> res = client.getSecurityService()
                .getAccountInfos(reqNodes);
            if (isWarning()) {
                logWarning("Retrieved " + res.size() + " AccountInfos for "
                    + reqNodes.size() + " requested of " + nodes.size()
                    + " nodes: " + res);
            }
            for (Entry<MemberInfo, AccountInfo> entry : res.entrySet()) {
                Member node = entry.getKey().getNode(getController(), false);
                if (node == null) {
                    continue;
                }
                AccountInfo aInfo = res.get(node.getInfo());
                if (CACHE_ENABLED) {
                    sessions.put(node, new Session(aInfo));
                }
                // logWarning("Fire account state change on " + node + " - "
                // + aInfo);
                fireNodeAccountStateChanged(node);
            }
        } catch (RemoteCallException e) {
            logSevere("Unable to retrieve account info for " + nodes.size()
                + " nodes. " + e);
            logFiner(e);
        }
    }

    // Internal helper ********************************************************

    private AccountInfo nullSafeGet(AccountInfo aInfo) {
        if (aInfo == null) {
            return NULL_ACCOUNT;
        }
        return aInfo;
    }

    private void clearNodeCache(Member node) {
        Session s = sessions.remove(node);
        logWarning("Clearing permissions cache on " + node + ": " + s);
        permissionsCacheAccounts.remove(nullSafeGet(s != null ? s
            .getAccountInfo() : null));
    }

    /**
     * Handle server connect: Refresh/Precache AccountInfos of friends and
     * members on our folders.
     */
    private void prefetchAccountInfos() {
        Collection<Member> nodesToRefresh = new ArrayList<Member>();
        for (Member node : getController().getNodeManager()
            .getNodesAsCollection())
        {
            if (shouldAutoRefresh(node)) {
                nodesToRefresh.add(node);
            }
        }
        fetchAccountInfos(nodesToRefresh, true);
    }

    /**
     * Refreshes a AccountInfo for the given node if it should be pre-fetched.
     * 
     * @param node
     */
    private void refresh(Member node) {
        if (shouldAutoRefresh(node)) {
            getAccountInfo(node);
        }
        fireNodeAccountStateChanged(node);
    }

    private boolean shouldAutoRefresh(Member node) {
        if (node.isMySelf()) {
            return true;
        }
        return node.isCompletelyConnected()
            && (node.isFriend() || node.hasJoinedAnyFolder() || node.isOnLAN());
    }

    private final class DefaultRefresher implements Runnable {
        private final Member node;

        private DefaultRefresher(Member node) {
            this.node = node;
        }

        public void run() {
            if (client.isServer(node) && node.isCompletelyConnected()) {
                prefetchAccountInfos();
            }

            clearNodeCache(node);
            refresh(node);

            // This is required because of probably changed access
            // permissions to folder.
            node.synchronizeFolderMemberships();
        }
    }

    private final class MySelfRefrehser implements Runnable {
        private final Member node;

        private MySelfRefrehser(Member node) {
            this.node = node;
        }

        public void run() {
            client.refreshAccountDetails();
            clearNodeCache(node);
            refresh(node);
            getController().getFolderRepository()
                .triggerSynchronizeAllFolderMemberships();
        }
    }

    private final class DisconnectRefresher implements Runnable {
        private final Member node;

        private DisconnectRefresher(Member node) {
            this.node = node;
        }

        public void run() {
            clearNodeCache(node);
            refresh(node);
        }
    }

    private class Session {
        private AccountInfo info;

        public Session(AccountInfo info) {
            super();
            // Info CAN be null! Means no login
            this.info = info;
        }

        public AccountInfo getAccountInfo() {
            return info;
        }
    }

    private class PermissionsCacheSegment {
        Map<Permission, Boolean> permissions = new ConcurrentHashMap<Permission, Boolean>();

        void set(Permission permission, Boolean hasPermission) {
            Reject.ifNull(permission, "Permission is null");
            permissions.put(permission, hasPermission);
        }

        Boolean hasPermission(Permission permission) {
            Reject.ifNull(permission, "Permission is null");
            return permissions.get(permission);
        }
    }

}
