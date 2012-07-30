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
import java.util.List;

import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;

/**
 * Contains all methods to modify/alter, create or notify Accounts.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface AccountService {
    /**
     * Tries to register a new account.
     * 
     * @param username
     *            the username
     * @param password
     *            the password. if NULL a random password will be generated and
     *            send by email.
     * @param newsLetter
     *            true if the users wants to subscribe to the newsletter.
     * @param serverInfo
     *            The server to host the account on or null for default
     * @param referredBy
     *            the account OID this user was referred by.
     * @param recommendWelcomeEmail
     *            if a welcome mail is recommend to be sent
     * @return the Account if registration was successfully. null if not
     *         possible or already taken even if password match.
     */
    Account register(String username, String password, boolean newsLetter,
        ServerInfo serverInfo, String referredBy, boolean recommendWelcomeEmail);

    /**
     * Logs in from a remote location.
     * 
     * @param username
     * @param passwordMD5
     *            the password mixed with the salt as MD5
     * @param salt
     *            the salt - a random string.
     * @return the Account with this username or null if login failed.
     * @deprecated use {@link SecurityService#login(String, char[])}
     */
    @Deprecated
    boolean login(String username, String passwordMD5, String salt);

    /**
     * @return Account details about the currently logged in user.
     * @deprecated use {@link SecurityService#getAccountDetails()}
     */
    @Deprecated
    AccountDetails getAccountDetails();

    /**
     * TRAC #1567, #1042
     * 
     * @param emails
     * @param personalMessage
     * @return true if all messages was successfully delivered
     */
    boolean tellFriend(Collection<String> emails, String personalMessage);

    /**
     * @return all license key content for this account. or null if no key was
     *         found.
     */
    List<String> getLicenseKeyContents();

    /**
     * Removes a computer from the own list of computers.
     * 
     * @param node
     */
    void removeComputer(MemberInfo node);

    /**
     * Performs all checks on the given online storage user accounts.
     * 
     * @param accounts
     */
    void checkAccounts(Collection<Account> accounts);

}
