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
package de.dal33t.powerfolder.clientserver;

import java.util.Collection;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;

/**
 * Contins all methods to modify/alter, create or notify Accounts.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface AccountService {
    /**
     * Tries to register a new account.
     * 
     * @param username
     *            the username
     * @param password
     *            the password
     * @param newsLetter
     *            true if the users wants to subscribe to the newsletter.
     * @return the Account if registration was successfully. null if not
     *         possible or already taken even if password match.
     */
    Account register(String username, String password, boolean newsLetter);

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

    /**
     * @param filterModel
     *            the filter to apply
     * @return the filtered list of accounts.
     */
    Collection<AccountDetails> getAccounts(AccountFilterModel filterModel);

    /**
     * @param username
     * @return the with the given username or null if not found
     */
    Account findByUsername(String username);

    /**
     * Saves or updates the given Account.
     * 
     * @param user
     */
    void store(Account user);

    /**
     * Enables the selected account:
     * <p>
     * The Online Storage subscription
     * <P>
     * Sets all folders to SyncProfile.BACKUP_TARGET.
     * 
     * @see Account#enable(de.dal33t.powerfolder.Controller)
     * @param username
     *            the username of the account to enable
     */
    void enable(String username);

    /**
     * Removes a user with the given username from the database
     * 
     * @param username
     * @return true if the user existed and is now removed. false if the
     *         username could not be found.
     */
    boolean delete(String username);

    /**
     * Checks the givens accounts for excess usage
     * 
     * @param usernames
     *            the username of the users to check.
     */
    void checkAccounts(String... usernames);

    /**
     * @return all license key content for this account. or null if no key was
     *         found.
     */
    String[] getLicenseKeyContents();

    /**
     * @return the folder containing the license keys.
     */
    FolderInfo getLicenseKeyFolder();
}
