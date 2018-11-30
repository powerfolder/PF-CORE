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

import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains all methods to modify/alter, create or notify Accounts.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface AccountService {

    /**
     * For internal use. Empty password may never login
     */
    static final String EMPTY_PASSWORD = "$BL4NK.P4SSW0RD$";

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
     * @param username
     *            The username of the user to register.
     * @param password
     *            The password. If null a random password will be generated and
     *            sent by email.
     * @param newsLetter
     *            If the user wants to subscribe to the newsletter.
     * @param referredBy
     *            The account oid of the referring user.
     * @return The account, if registration was successful.
     * @throws RegisterFailedException
     *             If registration failed.
     */
    Account register(String username, String password, boolean newsLetter,
        String referredBy) throws RegisterFailedException;

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
     * @deprecated since 14.0
     */
    @Deprecated
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
    
    
    /**
     * Returns the current skin of an account
     *
     * @param account The account
     * @return The current skin of the account
     */
    String getClientSkinName(AccountInfo account);

    /**
     * Merge one or more accounts into {@code account}.
     * {@code account} will be stored on success.
     * {@code mergeAccounts} are being deleted.
     * <p>
     * HINT: Server Administrators are always allowed to merge accounts!
     * <p>
     * Only certain combinations of accounts are allowed to be merged.
     * <p>
     * DB Users are allowed to only merge DB Users
     * LDAP Users are allowed to merge DB Users and LDAP Users
     * Shib Users are allowed to only merge DB Users
     * <p>
     * _column_ user can import _row_ user
     * <p>
     * | DB | LDAP | Shib
     * -----+----+------+------
     * DB   | T  | T    | T
     * LDAP | F  | T    | F
     * Shib | F  | F    | F
     *
     * @param account       Surviving account.
     * @param mergeAccounts Accounts that are merged into {@code account} and deleted afterwards.
     * @return An empty list, if all accounts were merged correctly, otherwise the
     * list of Account IDs of the accounts which are not allowed to be merged.
     * If any one account of {@code mergeAccounts} cannot be merged, no account
     * will be merged.
     */
    List<String> mergeAccounts(Account account, Account... mergeAccounts);

    /**
     * Update the {@code account's} Email addresses to {@code emails}.
     * <p>
     *     This method prioritizes the actions to be done.
     * </p>
     * <ol>
     *     <li>Initiate a merge</li>
     *     <li>Send verification Emails</li>
     *     <li>Remove Emails</li>
     * </ol>
     * <p>
     *     I.e. if an Email address is added that initiates a merge of two accounts,
     *     no lesser action is performed, if verification Emails are sent, no lesser
     *     action is performed.
     * </p>
     *
     * @param account
     *     The account to update the Email addresses
     * @param emails
     *     The new list of Email addresses
     * @return An {@link UpdateEmail} to indicate what happened and if the user
     * has to get active to verify an Email address or to merge two accounts.
     */
    UpdateEmail updateEmails(@NotNull Account account, @NotNull String[] emails);

    /**
     * Status and information about updating Emails of an {@link Account}
     *
     * {@code StatusCode StatusCodes} have a special meaning:
     * <table>
     *     <thead>
     *         <tr>
     *             <td>StatusCode</td>
     *             <td>Meaning</td>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>CONTINUE(100)</td>
     *             <td>Email verification needed</td>
     *         </tr>
     *         <tr>
     *             <td>PROCESSING(102)</td>
     *             <td>Merge verification needed</td>
     *         </tr>
     *         <tr>
     *             <td>OK(200)</td>
     *             <td>Only removed Emails</td>
     *         </tr>
     *         <tr>
     *             <td>NO_CONTENT(204)</td>
     *             <td>Nothing changed</td>
     *         </tr>
     *         <tr>
     *             <td>FORBIDDEN(403)</td>
     *             <td>Merge not allowed on this server</td>
     *         </tr>
     *     </tbody>
     * </table>
     */
    class UpdateEmail {
        @NotNull
        final StatusCode  status;
        @Nullable
        final Set<String> emails;
        @Nullable
        final String      type;


        // Creation ---
        private UpdateEmail(@NotNull StatusCode status) {
            this.status = status;
            this.emails = null;
            this.type   = null;
        }

        private UpdateEmail(@NotNull StatusCode status, @NotNull String email) {
            this.status = status;
            this.emails = new HashSet<>(1);
            this.emails.add(email);
            this.type   = null;
        }

        private UpdateEmail(@NotNull StatusCode status, @NotNull String email, @NotNull String type) {
            this.status = status;
            this.emails = new HashSet<>(1);
            this.emails.add(email);
            this.type   = type;
        }

        private UpdateEmail(@NotNull StatusCode status, @NotNull Set<String> emails) {
            this.status = status;
            this.emails = emails;
            this.type   = null;
        }

        /**
         * Create an {@link UpdateEmail} with {@link StatusCode#OK}
         *
         * @return {@code UpdateEmail} indicating that Emails were removed. May
         * contain a list of removed addresses.
         */
        public static UpdateEmail createRemovedEmails(@NotNull Set<String> emails) {
            return new UpdateEmail(StatusCode.OK, emails);
        }

        /**
         * Create an {@link UpdateEmail} with {@link StatusCode#NO_CONTENT}
         *
         * @return {@code UpdateEmail} indicating that nothing changed. Does not
         * contain any further information.
         */
        public static UpdateEmail createNothingChanged() {
            return new UpdateEmail(StatusCode.NO_CONTENT);
        }

        /**
         * Create an {@link UpdateEmail} with {@link StatusCode#FORBIDDEN}
         *
         * @return {@code UpdateEmail} indicating that the operation was not
         * allowed. Does not contain any further information.
         */
        public static UpdateEmail createNotAllowed(String email, String type) {
            return new UpdateEmail(StatusCode.FORBIDDEN, email, type);
        }

        /**
         * Create an {@link UpdateEmail} with {@link StatusCode#CONTINUE}
         *
         * @return {@code UpdateEmail} indicating that an Email was sent to
         * those Email addresses. The user has to verify that he/she has access
         * to those Email accounts. Contains a list of all affected Emails.
         */
        public static UpdateEmail createEmailVerificationNeeded() {
            return new UpdateEmail(StatusCode.CONTINUE);
        }

        /**
         * Create an {@link UpdateEmail} with {@link StatusCode#PROCESSING}
         *
         * @return {@code UpdateEmail} indicating that the user has to verify to
         * merge two accounts. Contains the Email of the account to merge.
         */
        public static UpdateEmail createMergeVerificationNeeded(@NotNull String email) {
            return new UpdateEmail(StatusCode.PROCESSING, email);
        }
        // ---

        // Access ---
        @NotNull
        public StatusCode getStatus() {
            return status;
        }

        @Nullable
        public Set<String> getEmails() {
            return emails;
        }

        @Nullable
        public String getType() {
            return type;
        }
        // ---
    }
}
