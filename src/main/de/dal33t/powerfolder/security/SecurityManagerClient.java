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
    private Map<Member, Session> sessions;
    private Map<Member, PermissionsCacheSegment> permissionsCache;
    private Map<FolderInfo, FolderPermission> defaultPermissionsCache;

    private static final boolean CACHE_ENABLED = true;

    public SecurityManagerClient(Controller controller, ServerClient client) {
        super(controller);
        Reject.ifNull(client, "Client is null");
        this.client = client;
        this.sessions = new ConcurrentHashMap<Member, Session>();
        this.permissionsCache = new ConcurrentHashMap<Member, PermissionsCacheSegment>();
        this.defaultPermissionsCache = new ConcurrentHashMap<FolderInfo, FolderPermission>();
    }

    public Account authenticate(String username, String password) {
        return client.login(username, password);
    }

    public boolean hasPermission(Member node, Permission permission) {
        Reject.ifNull(node, "Node is null");
        Reject.ifNull(permission, "Permission info is null");
        if (!Feature.SECURITY_CHECKS.isEnabled()) {
            return true;
        }
        try {
            Boolean hasPermission;
            PermissionsCacheSegment cache = permissionsCache.get(node);
            if (cache != null) {
                hasPermission = cache.hasPermission(permission);
            } else {
                // Create cache
                hasPermission = null;
                cache = new PermissionsCacheSegment();
                permissionsCache.put(node, cache);
            }
            boolean cacheHit;
            if (!CACHE_ENABLED) {
                hasPermission = null;
            }
            if (hasPermission == null) {
                hasPermission = Boolean.valueOf(client.getSecurityService()
                    .hasPermission(node.getInfo(), permission));
                cache.set(permission, hasPermission);
                cacheHit = false;
            } else {
                cacheHit = true;
            }
            logWarning((cacheHit ? "(cachd) " : "(retvd) ") + node + " has "
                + (hasPermission ? "" : "NOT ") + permission);

            return hasPermission;
        } catch (RemoteCallException e) {
            logWarning("Unable to check permission for " + node + ". " + e);
            logFiner(e);
            return false;
        }
    }

    public AccountInfo getAccountInfo(Member node) {
        Session session = sessions.get(node);
        // Cache hit
        if (session != null && CACHE_ENABLED) {
            return session.getAccountInfo();
        }

        AccountInfo aInfo;
        try {
            Map<MemberInfo, AccountInfo> res = client.getSecurityService()
                .getAccountInfos(Collections.singleton(node.getInfo()));
            aInfo = res.get(node.getInfo());
        } catch (RemoteCallException e) {
            logSevere("Unable to retrieve account info for " + node + ". " + e);
            logFiner(e);
            aInfo = null;
        }
        logWarning("Retrieved account " + aInfo + " for " + node);
        if (aInfo != null && CACHE_ENABLED) {
            sessions.put(node, new Session(aInfo));
        }
        return aInfo;
    }

    public void nodeAccountStateChanged(final Member node) {
        Runnable refresher = null;
        if (node.isMySelf()) {
            refresher = new Runnable() {
                public void run() {
                    client.refreshAccountDetails();
                    // Make sure nothing is left in cache.
                    permissionsCache.remove(node);
                    sessions.remove(node);
                    node.synchronizeFolderMemberships();
                }
            };
        } else if (!node.isCompleteyConnected()) {
            sessions.remove(node);
        } else {
            refresher = new Runnable() {
                public void run() {
                    permissionsCache.remove(node);
                    // Refresh account info on that node.
                    sessions.remove(node);
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
            return getController().getOSClient().getSecurityService()
                .getDefaultPermission(foInfo);
        } catch (Exception e) {
            logWarning("Unable to retrieve default permission from server. Using admin as fallback. "
                + e);
            logFiner(e);
            return new FolderAdminPermission(foInfo);
        }
    }

    private class Session {
        private AccountInfo info;

        public Session(AccountInfo info) {
            super();
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
