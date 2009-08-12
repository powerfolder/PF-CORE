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
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.clientserver.AccountStateChanged;

/**
 * A security manager handles the access control to a powerfolder security
 * realm.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface SecurityManager {

    // Authentication *********************************************************

    /**
     * Authenticates the user.
     * 
     * @param username
     *            the username of the login
     * @param password
     *            the password of the login
     * @return the account if acces is possible, null if user could not be
     *         logged in.
     */
    Account authenticate(String username, String password);

    // Core callbacks *********************************************************

    /**
     * Called when the account status on the given node is changed. e.g. logout
     * through disconnect.
     * <P>
     * TODO Listen for {@link AccountStateChanged} messages and update
     * accordingly.
     * 
     * @param node
     */
    void nodeAccountStateChanged(Member node);

    // Security stuff *********************************************************

    /**
     * @param node
     *            the node to get the account info for.
     * @return the account info for the given member.
     */
    AccountInfo getAccountInfo(Member node);

    /**
     * @param node
     *            the node to check the permission.
     * @param permission
     * @return true if this account has the given permission.
     */
    boolean hasPermission(Member node, Permission permission);

    /**
     * Takes also {@link FolderSecuritySettings} and default permission into
     * consideration when checking the permission.
     * 
     * @param member
     * @param permission
     * @return if the member has the given permission on the folder.
     */
    boolean hasFolderPermission(Member member, FolderPermission permission);

    // Event handling *********************************************************

    void addListener(SecurityManagerListener listner);

    void removeListener(SecurityManagerListener listner);

}