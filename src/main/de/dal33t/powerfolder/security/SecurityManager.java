/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * A security manager handles the access control to a powerfolder security
 * realm.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface SecurityManager {

    // Authentication *********************************************************

    /**
     * Authenticates the user.
     *
     * @param username
     *            the username of the login
     * @param credentials
     *            the password of the login
     * @return the account if access is possible, null if user could not be
     *         logged in.
     */
    Account authenticate(String username, Object credentials);

    /**
     * PFC-2548: Token based authentication.
     * 
     * @param tokenSecret
     *            the token secret.
     * @return the account if access is possible, null if no user could not be
     *         logged in by the token.
     * @see Token
     */
    Account authenticate(String tokenSecret);

    /**
     * PFS-1965: Post process setting for the passed account e.g. last login date.<br />
     * <br />
     * This method is meant for accounts already authenticated via Shibboleth.
     * 
     * @param shibbolethAccount
     *            The account already authenticated via Shibboleth.
     */
    void processAfterShibbolethLogin(Account shibbolethAccount);

    /**
     * Logs out and clears the current session.
     */
    void logout();

    // Core callbacks *********************************************************

    /**
     * Called when the account status on the given node is changed. e.g.
     * disconnect.
     *
     * @param node
     * @param refreshFolderMemberships
     *            of memberships of the folders should be re-synced
     */
    void nodeAccountStateChanged(Member node, boolean refreshFolderMemberships);

    // Security stuff *********************************************************

    /**
     * @param node
     *            the node to get the account info for.
     * @return the account info for the given member.
     */
    AccountInfo getAccountInfo(Member node);

    /**
     * Central method to check if a given computer/member has the permission.
     * <p>
     * This takes default permissions for folders into consideration. Also
     * accepts null {@link AccountInfo} as parameter - then applies default
     * permission of folder only.
     *
     * @param accountInfo
     * @param permission
     * @return true if the account has the permission. false if not
     */
    boolean hasPermission(MemberInfo memberInfo, Permission permission);

    /**
     * Central method to check if a given account has the permission.
     * <p>
     * This takes default permissions for folders into consideration. Also
     * accepts null {@link AccountInfo} as parameter - then applies default
     * permission of folder only.
     *
     * @param accountInfo
     * @param permission
     * @return true if the account has the permission. false if not
     */
    boolean hasPermission(AccountInfo accountInfo, Permission permission);

    /**
     * Central method to check if a given account has the permission.
     * <p>
     * This takes default permissions for folders into consideration. Also
     * accepts null {@link AccountInfo} as parameter - then applies default
     * permission of folder only.
     *
     * @param account
     * @param permission
     * @return true if the account has the permission. false if not
     */
    boolean hasPermission(Account account, Permission permission);

    /**
     * Central method to check read permission on a folder. Meta-folders are
     * checked against their content folder. Implementations may cache
     * authoritative answers.
     *
     * @param member
     * @param foInfo
     * @return true if the member has read permission on the folder.
     */
    default boolean hasReadPermission(Member member, FolderInfo foInfo) {
        if (foInfo.isMetaFolder()) {
            foInfo = foInfo.lookupContentFolderInfo();
        }
        return hasPermission(member.getInfo(), FolderPermission.read(foInfo));
    }

    /**
     * Central method to check write permission on a folder. Meta-folders are
     * checked against their content folder. Implementations may cache
     * authoritative answers.
     *
     * @param member
     * @param foInfo
     * @return true if the member has write permission on the folder.
     */
    default boolean hasWritePermission(Member member, FolderInfo foInfo) {
        if (foInfo.isMetaFolder()) {
            foInfo = foInfo.lookupContentFolderInfo();
        }
        return hasPermission(member.getInfo(),
            FolderPermission.readWrite(foInfo));
    }

    /**
     * Central method to check admin permission on a folder. Meta-folders are
     * checked against their content folder.
     *
     * @param member
     * @param foInfo
     * @return true if the member has admin permission on the folder.
     */
    default boolean hasAdminPermission(Member member, FolderInfo foInfo) {
        if (foInfo.isMetaFolder()) {
            foInfo = foInfo.lookupContentFolderInfo();
        }
        return hasPermission(member.getInfo(), FolderPermission.admin(foInfo));
    }

    /**
     * Central method to check owner permission on a folder. Meta-folders are
     * checked against their content folder.
     *
     * @param member
     * @param foInfo
     * @return true if the member has owner permission on the folder.
     */
    default boolean hasOwnerPermission(Member member, FolderInfo foInfo) {
        if (foInfo.isMetaFolder()) {
            foInfo = foInfo.lookupContentFolderInfo();
        }
        return hasPermission(member.getInfo(), FolderPermission.owner(foInfo));
    }

    /**
     * Invalidates cached folder permission answers of the given node.
     *
     * @param node
     */
    default void clearPermissionCache(Member node) {
    }

    /**
     * Invalidates all cached folder permission answers.
     */
    default void clearPermissionCache() {
    }

    // Event handling *********************************************************

    void addListener(SecurityManagerListener listner);

    void removeListener(SecurityManagerListener listner);

}
