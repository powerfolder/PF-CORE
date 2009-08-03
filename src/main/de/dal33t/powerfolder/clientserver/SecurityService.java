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
 * $Id: FolderService.java 4655 2008-07-19 15:32:32Z bytekeeper $
 */
package de.dal33t.powerfolder.clientserver;

import java.util.Collection;
import java.util.Map;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.Permission;

/**
 * Service for client authentication and permission checks.
 * 
 * @author sprajc
 */
public interface SecurityService {

    // Login stuff ************************************************************

    /**
     * Logs in from a remote location.
     * 
     * @param username
     * @param passwordMD5
     *            the password mixed with the salt as MD5
     * @param salt
     *            the salt - a random string.
     * @return the Account with this username or null if login failed.
     */
    boolean login(String username, String passwordMD5, String salt);

    /**
     * @return Account details about the currently logged in user.
     */
    AccountDetails getAccountDetails();

    // Security / Permission stuff ********************************************

    /**
     * Resulting map may not contain all nodes only those connected to the
     * server.
     * 
     * @param nodes
     * @return the {@link AccountInfo} for the nodes.
     */
    Map<MemberInfo, AccountInfo> getAccountInfos(Collection<MemberInfo> nodes);

    /**
     * @param node
     * @param permission
     * @return true if the account with the given node has that permission.
     */
    boolean hasPermission(MemberInfo node, Permission permission);

    /**
     * @param foInfo
     * @return the default permission for the given folder.
     */
    FolderPermission getDefaultPermission(FolderInfo foInfo);

    /**
     * Sets the default permission for the given folder.
     * 
     * @param foInfo
     * @param permission
     */
    void setDefaultPermission(FolderInfo foInfo, FolderPermission permission);

    // /**
    // * Grants the currently logged in user access to folder. the folder is NOT
    // * setup on the remote server.
    // *
    // * @param foInfos
    // * @see #createFolder(FolderInfo, SyncProfile)
    // */
    // void grantAdmin(FolderInfo... foInfos);

    /**
     * Revokes the currently logged in user access to folder. the folder is NOT
     * setup on the remote server.
     * 
     * @param foInfos
     */
    void revokeAdmin(FolderInfo... foInfos);

    // Misc *******************************************************************

    /**
     * TRAC #1566
     * 
     * @param pattern
     * @return the nodes
     */
    Map<MemberInfo, AccountInfo> searchNodes(String pattern);

}
