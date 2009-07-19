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

import de.dal33t.powerfolder.light.AccountInfo;

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

    // Permission questions on Member level ***********************************

    /**
     * @param aInfo
     * @param permission
     * @return true if this account has the given permission.
     */
    boolean hasPermission(AccountInfo aInfo, Permission permission);
}